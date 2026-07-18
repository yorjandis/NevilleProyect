package com.ypg.neville.model.metas

import com.ypg.neville.model.db.room.GoalEntity
import com.ypg.neville.model.db.room.GoalUnitEntity
import com.ypg.neville.model.db.room.ArchivedGoalEntity
import com.ypg.neville.model.db.room.ArchivedUnitEntity
import java.util.Locale

enum class TimeUnitType(val raw: String, val priority: Int) {
    MINUTOS("minutos", 0),
    HORAS("horas", 1),
    DIAS("dias", 2),
    SEMANAS("semanas", 3),
    MESES("meses", 4),
    ANIOS("años", 5);

    fun descriptionFor(value: Int): String {
        return when (this) {
            MINUTOS -> if (value == 1) "minuto" else "minutos"
            HORAS -> if (value == 1) "hora" else "horas"
            DIAS -> if (value == 1) "día" else "días"
            SEMANAS -> if (value == 1) "semana" else "semanas"
            MESES -> if (value == 1) "mes" else "meses"
            ANIOS -> if (value == 1) "año" else "años"
        }
    }

    companion object {
        fun fromRaw(raw: String?): TimeUnitType {
            val normalized = (raw ?: "")
                .trim()
                .lowercase(Locale.getDefault())
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")

            return when (normalized) {
                "minuto", "minutos", "minute", "minutes" -> MINUTOS
                "hora", "horas", "hour", "hours" -> HORAS
                "dia", "dias", "day", "days" -> DIAS
                "semana", "semanas", "week", "weeks" -> SEMANAS
                "mes", "meses", "month", "months" -> MESES
                "ano", "anos", "year", "years" -> ANIOS
                else -> DIAS
            }
        }
    }
}

enum class GoalScheduleType(val raw: String, val label: String) {
    INTERVAL("interval", "Por intervalo"),
    WEEKLY("weekly", "Días por semana"),
    SPECIFIC_DATES("specificDates", "Fechas específicas");

    companion object {
        fun fromRaw(raw: String?): GoalScheduleType = entries.firstOrNull { it.raw == raw } ?: INTERVAL
    }
}

enum class GoalCompletionBasis(val raw: String, val label: String) {
    EXECUTIONS("executions", "Número de ejecuciones"),
    DURATION("duration", "Duración total");

    companion object {
        fun fromRaw(raw: String?): GoalCompletionBasis = entries.firstOrNull { it.raw == raw } ?: EXECUTIONS
    }
}

enum class GoalDayPeriod(val raw: String, val label: String) {
    ANYTIME("anytime", "Cualquier momento"),
    MORNING("morning", "Por la mañana"),
    AFTERNOON("afternoon", "Por la tarde"),
    NIGHT("night", "Por la noche");

    companion object {
        fun fromRaw(raw: String?): GoalDayPeriod = entries.firstOrNull { it.raw == raw } ?: ANYTIME
    }
}

enum class UnitStatus(val raw: String) {
    PENDING("pending"),
    COMPLETED("completed"),
    LOST("lost");

    companion object {
        fun fromRaw(raw: String?): UnitStatus {
            return entries.firstOrNull { it.raw == raw } ?: PENDING
        }
    }
}

data class UnitInfo(
    val name: String,
    val info: String
)

data class ProgramaPreestablecido(
    val fileBaseName: String,
    val title: String,
    val detalles: String,
    val description: String,
    val unidadesinfo: List<UnitInfo>,
    val noUnidades: Int,
    val tipoUnidad: String,
    val frecuencia: Int,
    val scheduleType: GoalScheduleType = GoalScheduleType.INTERVAL,
    val weeklyDaysPerWeek: Int = 3,
    val dayPeriod: GoalDayPeriod = GoalDayPeriod.ANYTIME,
    val customUnitLabel: String = ""
)

data class HabitPreset(
    val title: String,
    val description: String,
    val noUnidades: Int,
    val noFrecuencias: Int,
    val scheduleType: GoalScheduleType = GoalScheduleType.INTERVAL,
    val weeklyDaysPerWeek: Int = 3,
    val dayPeriod: GoalDayPeriod = GoalDayPeriod.ANYTIME,
    val customUnitLabel: String = ""
)

data class GoalCardState(
    val goal: GoalEntity,
    val units: List<GoalUnitEntity>
) {
    val unitType: TimeUnitType
        get() = TimeUnitType.fromRaw(goal.unitType)

    val scheduleType: GoalScheduleType
        get() = GoalScheduleType.fromRaw(goal.scheduleType)

    val dayPeriod: GoalDayPeriod
        get() = GoalDayPeriod.fromRaw(goal.dayPeriod)

    val completionBasis: GoalCompletionBasis
        get() = GoalCompletionBasis.fromRaw(goal.completionBasis)

    val completedCount: Int
        get() = units.count { UnitStatus.fromRaw(it.status) == UnitStatus.COMPLETED }

    val progressedCount: Int
        get() = units.count {
            val st = UnitStatus.fromRaw(it.status)
            st == UnitStatus.COMPLETED || st == UnitStatus.LOST
        }

    val lostIndexes: List<Int>
        get() = units.filter { UnitStatus.fromRaw(it.status) == UnitStatus.LOST }
            .map { (it.unitIndex - 1).coerceAtLeast(0) }
            .sorted()

    val progressRatio: Double
        get() = if (goal.totalUnits <= 0) 0.0 else progressedCount.toDouble() / goal.totalUnits.toDouble()

    val isCompleted: Boolean
        get() = units.isNotEmpty() && units.all { UnitStatus.fromRaw(it.status) != UnitStatus.PENDING }

    val executionTargetText: String
        get() {
            val value = goal.executionTargetValue.takeIf { it > 0 } ?: 1.0
            val number = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
            val label = goal.customUnitLabel.trim()
            return when {
                label.isNotEmpty() && value == 1.0 -> "$label por ejecución"
                label.isNotEmpty() -> "$number $label por ejecución"
                value == 1.0 -> "Una ejecución"
                else -> "$number por ejecución"
            }
        }

    val scheduleSummary: String
        get() {
            val cadence = when (scheduleType) {
                GoalScheduleType.INTERVAL -> "Cada ${if (goal.frequency == 1) "" else "${goal.frequency} "}${unitType.descriptionFor(goal.frequency)}"
                GoalScheduleType.WEEKLY -> "${goal.weeklyDaysPerWeek.coerceIn(1, 7)} días por semana"
                GoalScheduleType.SPECIFIC_DATES -> "En fechas específicas"
            }
            return if (dayPeriod == GoalDayPeriod.ANYTIME) cadence else "$cadence · ${dayPeriod.label}"
        }

    val planSummary: String
        get() {
            val ending = when (completionBasis) {
                GoalCompletionBasis.EXECUTIONS -> "${goal.totalUnits} ${if (goal.totalUnits == 1) "ejecución" else "ejecuciones"}"
                GoalCompletionBasis.DURATION -> {
                    val durationUnit = TimeUnitType.fromRaw(goal.durationUnit)
                    "durante ${goal.durationValue.coerceAtLeast(1)} ${durationUnit.descriptionFor(goal.durationValue.coerceAtLeast(1))}"
                }
            }
            return "$executionTargetText · $scheduleSummary · $ending"
        }

    fun titleMatches(query: String): Boolean {
        if (query.isBlank()) return true
        return goal.title.lowercase(Locale.getDefault()).contains(query.trim().lowercase(Locale.getDefault()))
    }
}

data class ArchivedGoalCardState(
    val goal: ArchivedGoalEntity,
    val units: List<ArchivedUnitEntity>
) {
    val completedCount: Int
        get() = units.count { UnitStatus.fromRaw(it.status) == UnitStatus.COMPLETED }

    val progressedCount: Int
        get() = units.count {
            val st = UnitStatus.fromRaw(it.status)
            st == UnitStatus.COMPLETED || st == UnitStatus.LOST
        }

    val progressRatio: Double
        get() = if (goal.totalUnits <= 0) 0.0 else progressedCount.toDouble() / goal.totalUnits.toDouble()

    val completionRate: Double
        get() = if (progressedCount == 0) 0.0 else completedCount.toDouble() / progressedCount.toDouble()

    val hasLostUnits: Boolean
        get() = units.any { UnitStatus.fromRaw(it.status) == UnitStatus.LOST }

    val lostIndexes: List<Int>
        get() = units.filter { UnitStatus.fromRaw(it.status) == UnitStatus.LOST }
            .map { (it.unitIndex - 1).coerceAtLeast(0) }
            .sorted()

    fun titleMatches(query: String): Boolean {
        if (query.isBlank()) return true
        return goal.title.lowercase(Locale.getDefault()).contains(query.trim().lowercase(Locale.getDefault()))
    }
}
