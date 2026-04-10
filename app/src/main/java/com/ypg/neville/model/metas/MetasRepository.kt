package com.ypg.neville.model.metas

import android.content.Context
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
        notifyOnUnitAvailable: Boolean = false
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || totalUnits <= 0) return

        val goalId = UUID.randomUUID().toString()
        val safeFrequency = frequency.coerceAtLeast(1)

        db.runInTransaction {
            goalDao.insert(
                GoalEntity(
                    id = goalId,
                    title = cleanTitle,
                    descriptionText = description,
                    totalUnits = totalUnits,
                    unitType = unitType.raw,
                    frequency = safeFrequency,
                    isStarted = false,
                    startDate = null,
                    notifyOnUnitAvailable = notifyOnUnitAvailable,
                    lastNotifiedUnitIndex = 0
                )
            )

            val units = (1..totalUnits).map { idx ->
                val info = unitsInfo.getOrNull(idx - 1)
                GoalUnitEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    unitIndex = idx,
                    status = UnitStatus.PENDING.raw,
                    unitType = unitType.raw,
                    name = info?.name ?: "Unidad $idx",
                    info = info?.info ?: "",
                    note = "",
                    startDate = null,
                    endDate = null,
                    completedDate = null
                )
            }
            unitDao.insertAll(units)
        }
    }

    fun createProgramGoal(programa: ProgramaPreestablecido) {
        createGoal(
            title = programa.title,
            description = programa.description,
            totalUnits = programa.noUnidades,
            unitType = TimeUnitType.fromRaw(programa.tipoUnidad),
            frequency = programa.frecuencia,
            unitsInfo = programa.unidadesinfo,
            notifyOnUnitAvailable = false
        )
    }

    fun startGoal(goalId: String) {
        val now = System.currentTimeMillis()
        var shouldSchedule = false
        db.runInTransaction {
            val goal = goalDao.getById(goalId) ?: return@runInTransaction
            if (goal.isStarted) return@runInTransaction

            val unitType = TimeUnitType.fromRaw(goal.unitType)
            val baseStart = alignedStart(now, unitType)
            val safeFreq = goal.frequency.coerceAtLeast(1)
            val currentUnits = unitDao.getByGoalId(goalId)

            val rescheduled = currentUnits.map { unit ->
                val indexOffset = (unit.unitIndex - 1).coerceAtLeast(0)
                val start = addTime(baseStart, unitType, indexOffset * safeFreq)
                val end = addTime(start, unitType, safeFreq)
                unit.copy(
                    startDate = start,
                    endDate = end,
                    completedDate = null,
                    status = UnitStatus.PENDING.raw
                )
            }.toMutableList()

            if (rescheduled.isNotEmpty()) {
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
        }
    }

    fun markUnitCompleted(unitId: String): Boolean {
        val now = System.currentTimeMillis()
        var changed = false
        var goalIdToRefresh: String? = null
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
        }
        goalIdToRefresh?.let { GoalUnitNotificationScheduler.schedule(context, db, it) }
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

    fun updateGoal(goalId: String, title: String, description: String) {
        val goal = goalDao.getById(goalId) ?: return
        goalDao.update(goal.copy(title = title.trim(), descriptionText = description))
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
                    isStarted = false,
                    startDate = System.currentTimeMillis(),
                    notifyOnUnitAvailable = false,
                    lastNotifiedUnitIndex = 0
                )
            )

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
                    startDate = null,
                    endDate = null,
                    completedDate = null
                )
            }
            unitDao.insertAll(newUnits)
            restored = true
        }
        return restored
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
        val start = unit.startDate ?: return null
        val step = state.goal.frequency.coerceAtLeast(1)
        return addTime(start, state.unitType, step)
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
                val json = context.assets.open("metas/programasPreestablecidos/$file")
                    .bufferedReader().use { it.readText() }
                runCatching { parsePrograma(JSONObject(json), file.removeSuffix(".json")) }.getOrNull()
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
            HabitPreset(
                title = item.optString("title"),
                description = item.optString("description"),
                noUnidades = item.optInt("noUnidades", 21),
                noFrecuencias = item.optInt("noFrecuencias", 1)
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
            frecuencia = obj.optInt("frecuencia", 1)
        )
    }
}
