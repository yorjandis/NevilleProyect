package com.ypg.neville.model.metas

import android.content.Context
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType
import com.ypg.neville.localization.GoalContentLocalization
import com.ypg.neville.model.db.room.ArchivedGoalEntity
import com.ypg.neville.model.db.room.ArchivedUnitEntity
import com.ypg.neville.model.db.room.GoalEntity
import com.ypg.neville.model.db.room.GoalUnitEntity
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class MetasRepository(
    private val context: Context,
    private val db: NevilleRoomDatabase
) {
    private val goalDao = db.goalDao()
    private val unitDao = db.goalUnitDao()
    private val archivedGoalDao = db.archivedGoalDao()
    private val archivedUnitDao = db.archivedUnitDao()

    fun loadGoals(): List<GoalCardState> {
        val goals = goalDao.getAll()
        val states = goals.map { goal ->
            GoalCardState(goal, unitDao.getByGoalId(goal.id))
        }
        return states.sortedWith(::urgencyComparator)
    }

    fun loadArchivedGoals(): List<ArchivedGoalCardState> {
        return archivedGoalDao.getAll().map { goal ->
            ArchivedGoalCardState(goal, archivedUnitDao.getByGoalId(goal.id))
        }
    }

    fun createGoal(
        title: String,
        description: String,
        totalUnits: Int,
        unitType: TimeUnitType,
        frequency: Int,
        unitsInfo: List<UnitInfo> = emptyList(),
        notifyOnUnitAvailable: Boolean = false,
        scheduleType: GoalScheduleType = GoalScheduleType.INTERVAL,
        weeklyDaysPerWeek: Int = 3,
        dayPeriod: GoalDayPeriod = GoalDayPeriod.ANYTIME,
        customUnitLabel: String = "",
        executionTargetValue: Double = 1.0,
        completionBasis: GoalCompletionBasis = GoalCompletionBasis.EXECUTIONS,
        durationValue: Int = 30,
        durationUnit: TimeUnitType = TimeUnitType.DIAS,
        specificDates: List<Long> = emptyList()
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || totalUnits <= 0) return

        val goalId = UUID.randomUUID().toString()
        val safeFrequency = frequency.coerceAtLeast(1)
        val safeExecutionTarget = executionTargetValue.takeIf { it > 0 } ?: 1.0
        val storedUnitType = when (scheduleType) {
            GoalScheduleType.INTERVAL -> unitType
            GoalScheduleType.WEEKLY -> TimeUnitType.SEMANAS
            GoalScheduleType.SPECIFIC_DATES -> TimeUnitType.DIAS
        }
        val plannedTotal = if (completionBasis == GoalCompletionBasis.DURATION) {
            plannedUnitCount(
                referenceDate = System.currentTimeMillis(),
                durationValue = durationValue,
                durationUnit = durationUnit,
                scheduleType = scheduleType,
                intervalUnit = unitType,
                frequency = safeFrequency,
                weeklyDays = weeklyDaysPerWeek
            )
        } else totalUnits

        db.runInTransaction {
            goalDao.insert(
                GoalEntity(
                    id = goalId,
                    title = cleanTitle,
                    descriptionText = description,
                    totalUnits = plannedTotal,
                    unitType = storedUnitType.raw,
                    frequency = safeFrequency,
                    scheduleType = scheduleType.raw,
                    weeklyDaysPerWeek = weeklyDaysPerWeek.coerceIn(1, 7),
                    dayPeriod = dayPeriod.raw,
                    customUnitLabel = customUnitLabel.trim(),
                    executionTargetValue = safeExecutionTarget,
                    completionBasis = completionBasis.raw,
                    durationValue = durationValue.coerceAtLeast(1),
                    durationUnit = durationUnit.raw,
                    isStarted = false,
                    startDate = null,
                    notifyOnUnitAvailable = notifyOnUnitAvailable,
                    lastNotifiedUnitIndex = 0
                )
            )

            val units = (1..plannedTotal).map { idx ->
                val info = unitsInfo.getOrNull(idx - 1)
                val scheduledDate = specificDates.getOrNull(idx - 1)
                val window = scheduledDate?.let { applyDayPeriodWindow(startOfDay(it), dayPeriod) }
                GoalUnitEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    unitIndex = idx,
                    status = UnitStatus.PENDING.raw,
                    unitType = storedUnitType.raw,
                    name = info?.name ?: defaultUnitName(idx, safeExecutionTarget, customUnitLabel),
                    info = info?.info ?: "",
                    note = "",
                    startDate = window?.first,
                    endDate = window?.second,
                    completedDate = null
                )
            }
            unitDao.insertAll(units)
        }
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.GOALS_CREATED, targetKey = goalId)
    }

    fun createProgramGoal(programa: ProgramaPreestablecido) {
        createGoal(
            title = programa.title,
            description = programa.description,
            totalUnits = programa.noUnidades,
            unitType = TimeUnitType.fromRaw(programa.tipoUnidad),
            frequency = programa.frecuencia,
            unitsInfo = programa.unidadesinfo,
            notifyOnUnitAvailable = false,
            scheduleType = programa.scheduleType,
            weeklyDaysPerWeek = programa.weeklyDaysPerWeek,
            dayPeriod = programa.dayPeriod,
            customUnitLabel = programa.customUnitLabel
        )
    }

    fun startGoal(goalId: String) {
        val now = System.currentTimeMillis()
        var shouldSchedule = false
        db.runInTransaction {
            val goal = goalDao.getById(goalId) ?: return@runInTransaction
            if (goal.isStarted) return@runInTransaction

            val unitType = TimeUnitType.fromRaw(goal.unitType)
            val scheduleType = GoalScheduleType.fromRaw(goal.scheduleType)
            val dayPeriod = GoalDayPeriod.fromRaw(goal.dayPeriod)
            val scheduleReference = nextScheduleReference(now, dayPeriod)
            val baseStart = alignedStart(scheduleReference, unitType)
            val safeFreq = goal.frequency.coerceAtLeast(1)
            val currentUnits = unitDao.getByGoalId(goalId)

            val rescheduled = currentUnits.map { unit ->
                val indexOffset = (unit.unitIndex - 1).coerceAtLeast(0)
                val (start, end) = when (scheduleType) {
                    GoalScheduleType.SPECIFIC_DATES -> {
                        val savedStart = unit.startDate ?: addTime(baseStart, TimeUnitType.DIAS, indexOffset)
                        applyDayPeriodWindow(startOfDay(savedStart), dayPeriod)
                    }
                    GoalScheduleType.WEEKLY -> {
                        val weeklyDays = goal.weeklyDaysPerWeek.coerceIn(1, 7)
                        val weekOffset = indexOffset / weeklyDays
                        val dayOffset = indexOffset % weeklyDays
                        val day = addTime(addTime(startOfDay(scheduleReference), TimeUnitType.SEMANAS, weekOffset), TimeUnitType.DIAS, dayOffset)
                        applyDayPeriodWindow(day, dayPeriod)
                    }
                    GoalScheduleType.INTERVAL -> {
                        val rawStart = addTime(baseStart, unitType, indexOffset * safeFreq)
                        val rawEnd = addTime(rawStart, unitType, safeFreq)
                        applyDayPeriodWindow(rawStart, dayPeriod, rawEnd)
                    }
                }
                unit.copy(
                    startDate = start,
                    endDate = end,
                    completedDate = null,
                    status = UnitStatus.PENDING.raw
                )
            }.toMutableList()

            if (rescheduled.isNotEmpty() &&
                scheduleType == GoalScheduleType.INTERVAL &&
                GoalCompletionBasis.fromRaw(goal.completionBasis) == GoalCompletionBasis.EXECUTIONS &&
                dayPeriod == GoalDayPeriod.ANYTIME &&
                unitType != TimeUnitType.SEMANAS
            ) {
                val first = rescheduled[0]
                rescheduled[0] = first.copy(
                    status = UnitStatus.COMPLETED.raw,
                    completedDate = now
                )
            }

            goalDao.update(goal.copy(isStarted = true, startDate = now, lastNotifiedUnitIndex = 0))
            unitDao.updateAll(rescheduled)
            shouldSchedule = true
        }

        if (shouldSchedule) {
            GoalUnitNotificationScheduler.schedule(context, db, goalId)
            WeeklySummaryEventLogger.log(WeeklySummaryEventType.GOALS_IN_PROGRESS, targetKey = goalId)
        }
    }

    fun markUnitCompleted(unitId: String): Boolean {
        val now = System.currentTimeMillis()
        var changed = false
        var goalIdToRefresh: String? = null
        var goalCompleted = false
        db.runInTransaction {
            val unit = unitDao.getById(unitId) ?: return@runInTransaction
            val goal = goalDao.getById(unit.goalId) ?: return@runInTransaction
            if (!goal.isStarted) return@runInTransaction
            if (!canBeCompleted(unit, now)) return@runInTransaction

            unitDao.update(
                unit.copy(
                    status = UnitStatus.COMPLETED.raw,
                    completedDate = now
                )
            )
            changed = true
            goalIdToRefresh = unit.goalId
            val allUnits = unitDao.getByGoalId(unit.goalId)
            goalCompleted = allUnits.isNotEmpty() && allUnits.all { UnitStatus.fromRaw(it.status) != UnitStatus.PENDING }
        }
        goalIdToRefresh?.let { GoalUnitNotificationScheduler.schedule(context, db, it) }
        if (goalCompleted && goalIdToRefresh != null) {
            WeeklySummaryEventLogger.log(WeeklySummaryEventType.GOALS_COMPLETED, targetKey = goalIdToRefresh.orEmpty())
        }
        return changed
    }

    fun refreshLostUnits(goalId: String): Boolean {
        val now = System.currentTimeMillis()
        var changed = false

        db.runInTransaction {
            val goal = goalDao.getById(goalId) ?: return@runInTransaction
            if (!goal.isStarted) return@runInTransaction

            val units = unitDao.getByGoalId(goalId)
            val updates = units.mapNotNull { unit ->
                val isPending = UnitStatus.fromRaw(unit.status) == UnitStatus.PENDING
                val expired = (unit.endDate ?: Long.MAX_VALUE) < now
                if (isPending && expired) {
                    changed = true
                    unit.copy(status = UnitStatus.LOST.raw)
                } else {
                    null
                }
            }
            if (updates.isNotEmpty()) {
                unitDao.updateAll(updates)
            }
        }
        if (changed) {
            GoalUnitNotificationScheduler.schedule(context, db, goalId)
        }
        return changed
    }

    fun updateGoal(
        goalId: String,
        title: String,
        description: String,
        customUnitLabel: String,
        dayPeriod: GoalDayPeriod
    ) {
        db.runInTransaction {
            val goal = goalDao.getById(goalId) ?: return@runInTransaction
            val oldLabel = goal.customUnitLabel.trim()
            val newLabel = customUnitLabel.trim()
            val schedule = GoalScheduleType.fromRaw(goal.scheduleType)
            val unitType = TimeUnitType.fromRaw(goal.unitType)
            val updatedUnits = unitDao.getByGoalId(goalId).map { unit ->
                val oldDefault = if (oldLabel.isBlank()) "Unidad ${unit.unitIndex}" else "${oldLabel.replaceFirstChar { it.uppercase() }} ${unit.unitIndex}"
                val updatedName = if (unit.name == oldDefault || unit.name == "Unidad ${unit.unitIndex}" || unit.name == "Unit ${unit.unitIndex}") {
                    defaultUnitName(unit.unitIndex, goal.executionTargetValue, newLabel)
                } else unit.name
                if (UnitStatus.fromRaw(unit.status) != UnitStatus.PENDING || unit.startDate == null) {
                    unit.copy(name = updatedName)
                } else {
                    val base = if (schedule == GoalScheduleType.INTERVAL) alignedStart(unit.startDate, unitType) else startOfDay(unit.startDate)
                    val defaultEnd = if (schedule == GoalScheduleType.INTERVAL) {
                        addTime(base, unitType, goal.frequency.coerceAtLeast(1))
                    } else addTime(base, TimeUnitType.DIAS, 1)
                    val window = applyDayPeriodWindow(base, dayPeriod, defaultEnd)
                    unit.copy(name = updatedName, startDate = window.first, endDate = window.second)
                }
            }
            goalDao.update(
                goal.copy(
                    title = title.trim(),
                    descriptionText = description,
                    customUnitLabel = newLabel,
                    dayPeriod = dayPeriod.raw
                )
            )
            unitDao.updateAll(updatedUnits)
        }
        GoalUnitNotificationScheduler.schedule(context, db, goalId)
    }

    fun updateGoalDescription(goalId: String, description: String) {
        val goal = goalDao.getById(goalId) ?: return
        goalDao.update(goal.copy(descriptionText = description))
    }

    fun updateUnitNote(unitId: String, note: String) {
        val unit = unitDao.getById(unitId) ?: return
        unitDao.update(unit.copy(note = note))
    }

    fun updateArchivedGoalDescription(goalId: String, description: String) {
        val goal = archivedGoalDao.getById(goalId) ?: return
        archivedGoalDao.update(goal.copy(descriptionText = description))
    }

    fun updateArchivedUnitNote(unitId: String, note: String) {
        val unit = archivedUnitDao.getById(unitId) ?: return
        archivedUnitDao.update(unit.copy(note = note))
    }

    fun deleteGoal(goalId: String) {
        GoalUnitNotificationScheduler.cancelPending(context, goalId)
        goalDao.deleteById(goalId)
    }

    fun deleteArchivedGoal(goalId: String) {
        archivedGoalDao.deleteById(goalId)
    }

    fun archiveGoal(goalId: String): Boolean {
        val now = System.currentTimeMillis()
        var archived = false

        db.runInTransaction {
            val goal = goalDao.getById(goalId) ?: return@runInTransaction
            val units = unitDao.getByGoalId(goalId)
            val isCompleted = units.isNotEmpty() && units.all { UnitStatus.fromRaw(it.status) != UnitStatus.PENDING }
            if (!isCompleted) return@runInTransaction

            if (archivedGoalDao.getById(goal.id) != null) {
                goalDao.deleteById(goal.id)
                archived = true
                return@runInTransaction
            }

            archivedGoalDao.insert(
                ArchivedGoalEntity(
                    id = goal.id,
                    title = goal.title,
                    descriptionText = goal.descriptionText,
                    totalUnits = goal.totalUnits,
                    unitType = goal.unitType,
                    frequency = goal.frequency,
                    scheduleType = goal.scheduleType,
                    weeklyDaysPerWeek = goal.weeklyDaysPerWeek,
                    dayPeriod = goal.dayPeriod,
                    customUnitLabel = goal.customUnitLabel,
                    executionTargetValue = goal.executionTargetValue,
                    completionBasis = goal.completionBasis,
                    durationValue = goal.durationValue,
                    durationUnit = goal.durationUnit,
                    completionDate = now
                )
            )

            val archivedUnits = units.map { unit ->
                ArchivedUnitEntity(
                    id = unit.id,
                    goalId = goal.id,
                    unitIndex = unit.unitIndex,
                    status = unit.status,
                    name = unit.name,
                    info = unit.info,
                    note = unit.note,
                    startDate = unit.startDate,
                    endDate = unit.endDate,
                    completedDate = unit.completedDate
                )
            }
            archivedUnitDao.insertAll(archivedUnits)
            GoalUnitNotificationScheduler.cancelPending(context, goal.id)
            goalDao.deleteById(goal.id)
            archived = true
        }

        return archived
    }

    fun restoreArchivedGoal(archivedGoalId: String): Boolean {
        var restored = false
        db.runInTransaction {
            val archived = archivedGoalDao.getById(archivedGoalId) ?: return@runInTransaction
            val archivedUnits = archivedUnitDao.getByGoalId(archivedGoalId)
            val newGoalId = UUID.randomUUID().toString()

            goalDao.insert(
                GoalEntity(
                    id = newGoalId,
                    title = archived.title,
                    descriptionText = archived.descriptionText,
                    totalUnits = archived.totalUnits,
                    unitType = archived.unitType,
                    frequency = archived.frequency,
                    scheduleType = archived.scheduleType,
                    weeklyDaysPerWeek = archived.weeklyDaysPerWeek,
                    dayPeriod = archived.dayPeriod,
                    customUnitLabel = archived.customUnitLabel,
                    executionTargetValue = archived.executionTargetValue,
                    completionBasis = archived.completionBasis,
                    durationValue = archived.durationValue,
                    durationUnit = archived.durationUnit,
                    isStarted = false,
                    startDate = System.currentTimeMillis(),
                    notifyOnUnitAvailable = false,
                    lastNotifiedUnitIndex = 0
                )
            )

            val keepSpecificDates = GoalScheduleType.fromRaw(archived.scheduleType) == GoalScheduleType.SPECIFIC_DATES
            val newUnits = archivedUnits.map { old ->
                GoalUnitEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = newGoalId,
                    unitIndex = old.unitIndex,
                    status = UnitStatus.PENDING.raw,
                    unitType = archived.unitType,
                    name = old.name.ifBlank { "Unidad ${old.unitIndex}" },
                    info = old.info,
                    note = "",
                    startDate = if (keepSpecificDates) old.startDate else null,
                    endDate = if (keepSpecificDates) old.endDate else null,
                    completedDate = null
                )
            }
            unitDao.insertAll(newUnits)
            restored = true
        }
        return restored
    }

    fun reactivateCompletedGoal(goalId: String): Boolean {
        val archived = archiveGoal(goalId)
        return archived && restoreArchivedGoal(goalId)
    }

    fun timeUntilNextUnit(state: GoalCardState, now: Long = System.currentTimeMillis()): String? {
        if (!state.goal.isStarted) return null
        val next = firstPendingUnit(state.units, now = null) ?: return null
        val start = next.startDate ?: return null

        if (now >= start) return "Listo"

        val secs = kotlin.math.ceil((start - now) / 1000.0).toInt()
        if (secs < 60) return "Próxima unidad en ${secs.coerceAtLeast(0)}s"

        return when (state.unitType) {
            TimeUnitType.MINUTOS -> {
                val minutes = kotlin.math.ceil((start - now) / 60000.0).toInt()
                "Próxima unidad en $minutes min"
            }

            TimeUnitType.HORAS -> {
                val minutes = kotlin.math.ceil((start - now) / 60000.0).toInt()
                "Próxima unidad en ${minutes}min"
            }

            TimeUnitType.DIAS -> {
                val totalMinutes = kotlin.math.ceil((start - now) / 60000.0).toInt().coerceAtLeast(0)
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                val h = if (hours > 0) "${hours}hr y " else ""
                val m = if (minutes > 0) "${minutes}min" else ""
                "Próxima unidad en $h$m".trim()
            }

            TimeUnitType.SEMANAS -> {
                val totalHours = kotlin.math.ceil((start - now) / 3_600_000.0).toLong().coerceAtLeast(0)
                val days = totalHours / 24
                val hours = totalHours % 24
                "Próxima unidad en ${days}d ${hours}h"
            }

            TimeUnitType.MESES -> {
                val cal = Calendar.getInstance()
                val diff = cal.run {
                    val startDate = java.util.Date(start)
                    val nowDate = java.util.Date(now)
                    setTime(nowDate)
                    val startCal = Calendar.getInstance().apply { time = startDate }
                    Pair(
                        startCal.get(Calendar.DAY_OF_YEAR) - get(Calendar.DAY_OF_YEAR) +
                            (startCal.get(Calendar.YEAR) - get(Calendar.YEAR)) * 365,
                        (((start - now) / (60L * 60L * 1000L)) % 24L).toInt()
                    )
                }
                val days = diff.first.coerceAtLeast(0)
                val hours = diff.second.coerceAtLeast(0)
                val d = if (days > 0) "${days}${if (days == 1) "día" else "días"} y " else ""
                val h = if (hours > 0) "${hours}hr" else ""
                "Próxima unidad en $d$h".trim()
            }

            TimeUnitType.ANIOS -> {
                val calNow = Calendar.getInstance().apply { timeInMillis = now }
                val calStart = Calendar.getInstance().apply { timeInMillis = start }
                val months = (calStart.get(Calendar.YEAR) - calNow.get(Calendar.YEAR)) * 12 +
                    (calStart.get(Calendar.MONTH) - calNow.get(Calendar.MONTH))
                val days = ((start - now) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0)
                val remHours = ((start - now) / (60L * 60L * 1000L)) % 24
                val m = if (months > 0) "${months}${if (months == 1) "mes" else "meses"} y " else ""
                val d = if (days > 0) "${days}${if (days == 1L) "día" else "días"} y " else ""
                val h = if (remHours > 0) "${remHours}hr" else ""
                "Próxima unidad en $m$d$h".trim()
            }
        }
    }

    fun canBeCompleted(unit: GoalUnitEntity, now: Long = System.currentTimeMillis()): Boolean {
        val status = UnitStatus.fromRaw(unit.status)
        val start = unit.startDate ?: return false
        val end = unit.endDate ?: return false
        return status == UnitStatus.PENDING && now >= start && now <= end
    }

    fun nextPendingUnit(state: GoalCardState, now: Long = System.currentTimeMillis()): GoalUnitEntity? {
        return firstPendingUnit(state.units, now)
    }

    fun nextExpirationDate(state: GoalCardState, now: Long = System.currentTimeMillis()): Long? {
        val unit = nextPendingUnit(state, now) ?: return null
        return unit.endDate
    }

    fun updateGoalUnitNotifications(goalId: String, enabled: Boolean) {
        val goal = goalDao.getById(goalId) ?: return
        if (goal.notifyOnUnitAvailable == enabled) {
            if (enabled) {
                GoalUnitNotificationScheduler.schedule(context, db, goalId)
            } else {
                GoalUnitNotificationScheduler.cancelPending(context, goalId)
            }
            return
        }

        goalDao.update(goal.copy(notifyOnUnitAvailable = enabled))
        if (enabled) {
            GoalUnitNotificationScheduler.schedule(context, db, goalId)
        } else {
            GoalUnitNotificationScheduler.cancelPending(context, goalId)
        }
    }

    private fun firstPendingUnit(units: List<GoalUnitEntity>, now: Long?): GoalUnitEntity? {
        return units.filter { unit ->
            if (UnitStatus.fromRaw(unit.status) != UnitStatus.PENDING) return@filter false
            if (now == null) return@filter true
            (unit.startDate ?: now) <= now
        }.minByOrNull { it.unitIndex }
    }

    private fun urgencyComparator(g1: GoalCardState, g2: GoalCardState): Int {
        if (g1.isCompleted && !g2.isCompleted) return 1
        if (!g1.isCompleted && g2.isCompleted) return -1

        val d1 = urgencyDate(g1) ?: Long.MAX_VALUE
        val d2 = urgencyDate(g2) ?: Long.MAX_VALUE
        if (d1 != d2) return d1.compareTo(d2)

        return g1.unitType.priority.compareTo(g2.unitType.priority)
    }

    private fun urgencyDate(state: GoalCardState): Long? {
        val now = System.currentTimeMillis()
        val nextAvailable = firstPendingUnit(state.units, now)
        if (nextAvailable != null) {
            return nextAvailable.endDate
        }

        return state.units
            .filter { UnitStatus.fromRaw(it.status) == UnitStatus.PENDING }
            .minByOrNull { it.unitIndex }
            ?.startDate
    }

    private fun alignedStart(now: Long, unit: TimeUnitType): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        when (unit) {
            TimeUnitType.MINUTOS -> {
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }

            TimeUnitType.HORAS -> {
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }

            TimeUnitType.DIAS -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }

            TimeUnitType.SEMANAS -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }

            TimeUnitType.MESES -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }

            TimeUnitType.ANIOS -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
        }
        return cal.timeInMillis
    }

    private fun addTime(base: Long, unit: TimeUnitType, value: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        when (unit) {
            TimeUnitType.MINUTOS -> cal.add(Calendar.MINUTE, value)
            TimeUnitType.HORAS -> cal.add(Calendar.HOUR_OF_DAY, value)
            TimeUnitType.DIAS -> cal.add(Calendar.DAY_OF_MONTH, value)
            TimeUnitType.SEMANAS -> cal.add(Calendar.WEEK_OF_YEAR, value)
            TimeUnitType.MESES -> cal.add(Calendar.MONTH, value)
            TimeUnitType.ANIOS -> cal.add(Calendar.YEAR, value)
        }
        return cal.timeInMillis
    }

    fun loadProgramasAgrupados(): List<Pair<String, List<ProgramaPreestablecido>>> {
        val files = (context.assets.list("metas/programasPreestablecidos") ?: emptyArray())
            .filter { it.endsWith(".json") }
            .sorted()

        val grouped = files.groupBy { file ->
            file.removeSuffix(".json").replace(Regex("_\\d+$"), "")
        }

        return grouped.entries.sortedBy { it.key }.map { entry ->
            val programas = entry.value.sorted().mapNotNull { file ->
                val fileBaseName = file.removeSuffix(".json")
                val json = context.assets.open("metas/programasPreestablecidos/$file")
                    .bufferedReader().use { it.readText() }
                runCatching {
                    val fallback = parsePrograma(JSONObject(json), fileBaseName)
                    GoalContentLocalization.localizedProgram(context, fileBaseName, fallback)
                }.getOrNull()
            }
            entry.key to programas
        }
    }

    fun loadHabitPresets(): List<HabitPreset> {
        val json = context.assets.open("metas/habitos_preestablecidos.json")
            .bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return (0 until arr.length()).map { idx ->
            val item = arr.getJSONObject(idx)
            val fallback = applyHabitScheduleMetadata(HabitPreset(
                title = item.optString("title"),
                description = item.optString("description"),
                noUnidades = item.optInt("noUnidades", 21),
                noFrecuencias = item.optInt("noFrecuencias", 1),
                scheduleType = GoalScheduleType.fromRaw(item.optString("scheduleType")),
                weeklyDaysPerWeek = item.optInt("weeklyDaysPerWeek", 3),
                dayPeriod = GoalDayPeriod.fromRaw(item.optString("dayPeriod")),
                customUnitLabel = item.optString("customUnitLabel")
            ))
            GoalContentLocalization.localizedHabit(
                context = context,
                spanishTitle = item.optString("title"),
                fallback = fallback
            )
        }.sortedBy { it.title.lowercase() }
    }

    private fun parsePrograma(obj: JSONObject, fileBaseName: String): ProgramaPreestablecido {
        val unidades = obj.getJSONArray("unidadesinfo")
        val list = mutableListOf<UnitInfo>()
        for (i in 0 until unidades.length()) {
            val item = unidades.getJSONObject(i)
            list.add(UnitInfo(item.optString("name"), item.optString("info")))
        }

        return ProgramaPreestablecido(
            fileBaseName = fileBaseName,
            title = obj.optString("title"),
            detalles = obj.optString("detalles"),
            description = obj.optString("description"),
            unidadesinfo = list,
            noUnidades = obj.optInt("noUnidades", list.size.coerceAtLeast(21)),
            tipoUnidad = obj.optString("tipoUnidad", TimeUnitType.DIAS.raw),
            frecuencia = obj.optInt("frecuencia", 1),
            scheduleType = GoalScheduleType.fromRaw(obj.optString("scheduleType")),
            weeklyDaysPerWeek = obj.optInt("weeklyDaysPerWeek", 3),
            dayPeriod = GoalDayPeriod.fromRaw(obj.optString("dayPeriod")),
            customUnitLabel = obj.optString("customUnitLabel")
        )
    }

    private fun applyHabitScheduleMetadata(preset: HabitPreset): HabitPreset {
        if (preset.scheduleType != GoalScheduleType.INTERVAL ||
            preset.dayPeriod != GoalDayPeriod.ANYTIME ||
            preset.customUnitLabel.isNotBlank()
        ) return preset

        return when (preset.title) {
            "Cena Temprana" -> preset.copy(dayPeriod = GoalDayPeriod.AFTERNOON, customUnitLabel = "cenas tempranas")
            "Exposicion Luz Matutina" -> preset.copy(dayPeriod = GoalDayPeriod.MORNING, customUnitLabel = "sesiones")
            "Entrenamiento En Zona2" -> preset.copy(scheduleType = GoalScheduleType.WEEKLY, weeklyDaysPerWeek = 3, customUnitLabel = "sesiones")
            "Detox Digital Nocturno" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "noches")
            "Espacios Ordenados Diarios" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "noches")
            "Hidratacion Diaria" -> preset.copy(customUnitLabel = "días hidratados")
            "Estiramientos Matutinos" -> preset.copy(dayPeriod = GoalDayPeriod.MORNING, customUnitLabel = "sesiones")
            "Dormir8Horas" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "noches")
            "Yoga Diario", "Meditacion Corta" -> preset.copy(customUnitLabel = "sesiones")
            "Sauna Semanal" -> preset.copy(scheduleType = GoalScheduleType.WEEKLY, weeklyDaysPerWeek = 1, customUnitLabel = "sesiones")
            "Ejercicio De Fuerza" -> preset.copy(scheduleType = GoalScheduleType.WEEKLY, weeklyDaysPerWeek = 3, customUnitLabel = "entrenamientos")
            "Lectura Diaria" -> preset.copy(customUnitLabel = "páginas")
            "Diario Gratitud" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "entradas")
            "Reescribir El Pasado" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "revisiones")
            "Imaginacion Creativa Diaria", "Meditacion Imaginativa" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "sesiones")
            "Revisar Sueños" -> preset.copy(dayPeriod = GoalDayPeriod.MORNING, customUnitLabel = "registros")
            "Revisar Metas Semanal" -> preset.copy(scheduleType = GoalScheduleType.WEEKLY, weeklyDaysPerWeek = 1, customUnitLabel = "revisiones")
            "Dormir Sin Pantallas" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "noches")
            "Reflexion Antes Dormir" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "reflexiones")
            "Yoga Antes Dormir" -> preset.copy(dayPeriod = GoalDayPeriod.NIGHT, customUnitLabel = "sesiones")
            "Planificacion Diaria" -> preset.copy(dayPeriod = GoalDayPeriod.MORNING, customUnitLabel = "planificaciones")
            else -> preset
        }
    }

    private fun defaultUnitName(index: Int, targetValue: Double, customLabel: String): String {
        val label = customLabel.trim()
        val number = if (targetValue % 1.0 == 0.0) targetValue.toInt().toString() else "%.2f".format(targetValue).trimEnd('0').trimEnd('.')
        return when {
            targetValue == 1.0 && label.isBlank() -> "Unidad $index"
            targetValue == 1.0 -> "${label.replaceFirstChar { it.uppercase() }} $index"
            label.isBlank() -> "$number · $index"
            else -> "$number $label · $index"
        }
    }

    private fun plannedUnitCount(
        referenceDate: Long,
        durationValue: Int,
        durationUnit: TimeUnitType,
        scheduleType: GoalScheduleType,
        intervalUnit: TimeUnitType,
        frequency: Int,
        weeklyDays: Int
    ): Int {
        val end = addTime(referenceDate, durationUnit, durationValue.coerceAtLeast(1))
        if (scheduleType == GoalScheduleType.WEEKLY) {
            val days = ((startOfDay(end) - startOfDay(referenceDate)) / 86_400_000L).toInt().coerceAtLeast(1)
            val safeDays = weeklyDays.coerceIn(1, 7)
            return ((days / 7) * safeDays + minOf(days % 7, safeDays)).coerceIn(1, 5_000)
        }
        var count = 0
        var cursor = referenceDate
        while (cursor < end && count < 5_000) {
            count++
            val next = addTime(cursor, intervalUnit, frequency.coerceAtLeast(1))
            if (next <= cursor) break
            cursor = next
        }
        return count.coerceAtLeast(1)
    }

    private fun startOfDay(epoch: Long): Long = Calendar.getInstance().apply {
        timeInMillis = epoch
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun nextScheduleReference(now: Long, period: GoalDayPeriod): Long {
        if (period == GoalDayPeriod.ANYTIME) return now
        val (_, end) = applyDayPeriodWindow(startOfDay(now), period)
        return if (now > end) addTime(now, TimeUnitType.DIAS, 1) else now
    }

    private fun applyDayPeriodWindow(
        start: Long,
        period: GoalDayPeriod,
        defaultEnd: Long = addTime(startOfDay(start), TimeUnitType.DIAS, 1)
    ): Pair<Long, Long> {
        if (period == GoalDayPeriod.ANYTIME) return start to defaultEnd
        val day = startOfDay(start)
        return when (period) {
            GoalDayPeriod.ANYTIME -> start to defaultEnd
            GoalDayPeriod.MORNING -> addTime(day, TimeUnitType.HORAS, 5) to addTime(day, TimeUnitType.HORAS, 12)
            GoalDayPeriod.AFTERNOON -> addTime(day, TimeUnitType.HORAS, 12) to addTime(day, TimeUnitType.HORAS, 20)
            GoalDayPeriod.NIGHT -> addTime(day, TimeUnitType.HORAS, 20) to addTime(day, TimeUnitType.HORAS, 29)
        }
    }
}
