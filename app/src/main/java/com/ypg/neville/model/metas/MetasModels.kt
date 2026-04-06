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
    MESES("meses", 3),
    ANIOS("años", 4);

    fun descriptionFor(value: Int): String {
        return when (this) {
            MINUTOS -> if (value == 1) "minuto" else "minutos"
            HORAS -> if (value == 1) "hora" else "horas"
            DIAS -> if (value == 1) "día" else "días"
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
                "mes", "meses", "month", "months" -> MESES
                "ano", "anos", "year", "years" -> ANIOS
                else -> DIAS
            }
        }
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
    val frecuencia: Int
)

data class HabitPreset(
    val title: String,
    val description: String,
    val noUnidades: Int,
    val noFrecuencias: Int
)

data class GoalCardState(
    val goal: GoalEntity,
    val units: List<GoalUnitEntity>
) {
    val unitType: TimeUnitType
        get() = TimeUnitType.fromRaw(goal.unitType)

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
