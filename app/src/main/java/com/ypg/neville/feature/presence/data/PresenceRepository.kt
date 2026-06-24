package com.ypg.neville.feature.presence.data

import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.Flow

enum class PresenceEventType(val raw: String) {
    Present("presente"),
    Unconscious("inconsciente"),
    Mood("estadoAnimo")
}

data class PresenceMood(
    val id: String,
    val title: String,
    val countsAsUnconscious: Boolean
) {
    companion object {
        val common = listOf(
            PresenceMood("sientoMiFuturoAhora", "Siento mi futuro ahora", false),
            PresenceMood("pilotoAutomatico", "Piloto automático", true),
            PresenceMood("distraido", "Distraído", true),
            PresenceMood("sereno", "Sereno", false),
            PresenceMood("alegre", "Alegre", false),
            PresenceMood("ansioso", "Ansioso", false),
            PresenceMood("triste", "Triste", false),
            PresenceMood("enfadado", "Enfadado", false),
            PresenceMood("cansado", "Cansado", false),
            PresenceMood("agradecido", "Agradecido", false)
        )

        fun titleFor(id: String?): String = common.firstOrNull { it.id == id }?.title ?: id.orEmpty()
    }
}

data class PresenceDayStats(
    val dateMillis: Long,
    val presentes: Int,
    val inconscientes: Int,
    val estadosAnimo: Int,
    val futuroAhora: Int
) {
    val total: Int get() = presentes + inconscientes
    val ratioPresencia: Float get() = if (total == 0) 0f else presentes.toFloat() / total.toFloat()
}

data class PresenceMoodStats(
    val moodId: String,
    val count: Int
) {
    val title: String get() = PresenceMood.titleFor(moodId)
}

data class PresenceEventPoint(
    val id: String,
    val createdAtMillis: Long,
    val dayStartMillis: Long,
    val eventType: PresenceEventType?,
    val moodId: String?
) {
    val isPresentReturn: Boolean get() = eventType == PresenceEventType.Present
    val isAutomaticPilot: Boolean
        get() = moodId == "pilotoAutomatico" || moodId == "distraido"
}

data class PresenceStreakStats(
    val currentDays: Int,
    val bestDays: Int
)

class PresenceRepository(
    private val dao: PresenceEventDao,
    private val source: String = "Android"
) {
    fun observeChanges(): Flow<Int> = dao.observeTotalCount()

    suspend fun recordPresent(mood: PresenceMood? = null) {
        createEvent(PresenceEventType.Present, mood?.id)
    }

    suspend fun recordMood(mood: PresenceMood) {
        createEvent(
            type = if (mood.countsAsUnconscious) PresenceEventType.Unconscious else PresenceEventType.Mood,
            moodId = mood.id
        )
    }

    suspend fun todayPresentCount(): Int {
        val start = startOfDay(System.currentTimeMillis())
        return dao.countByTypeBetween(PresenceEventType.Present.raw, start, start + DAY_MILLIS)
    }

    suspend fun futureFeelingCount(days: Int = 30): Int {
        return dao.countMoodSince("sientoMiFuturoAhora", rangeStart(days))
    }

    suspend fun dayStats(days: Int = 14): List<PresenceDayStats> {
        val safeDays = days.coerceAtLeast(1)
        val start = rangeStart(safeDays)
        val buckets = linkedMapOf<Long, MutablePresenceDayStats>()
        repeat(safeDays) { offset ->
            val day = start + offset * DAY_MILLIS
            buckets[day] = MutablePresenceDayStats(day)
        }

        dao.eventsSince(start).forEach { event ->
            val day = startOfDay(event.dayStartMillis)
            val bucket = buckets.getOrPut(day) { MutablePresenceDayStats(day) }
            when (event.eventType) {
                PresenceEventType.Present.raw -> bucket.presentes += 1
                PresenceEventType.Unconscious.raw -> bucket.inconscientes += 1
                PresenceEventType.Mood.raw -> bucket.estadosAnimo += 1
            }
            if (event.moodId == "sientoMiFuturoAhora") {
                bucket.futuroAhora += 1
            }
        }

        return buckets.values.map { it.toStats() }.sortedBy { it.dateMillis }
    }

    suspend fun moodStats(days: Int = 30): List<PresenceMoodStats> {
        return dao.eventsSince(rangeStart(days))
            .mapNotNull { it.moodId?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
            .map { PresenceMoodStats(it.key, it.value) }
            .sortedByDescending { it.count }
    }

    suspend fun eventPoints(days: Int = 14): List<PresenceEventPoint> {
        return dao.eventsSince(rangeStart(days))
            .sortedBy { it.createdAtMillis }
            .map { event ->
                PresenceEventPoint(
                    id = event.id,
                    createdAtMillis = event.createdAtMillis,
                    dayStartMillis = event.dayStartMillis,
                    eventType = PresenceEventType.entries.firstOrNull { it.raw == event.eventType },
                    moodId = event.moodId
                )
            }
    }

    suspend fun todayEventPoints(): List<PresenceEventPoint> = eventPoints(1)

    suspend fun streakStats(days: Int = 90, threshold: Int = 10): PresenceStreakStats {
        val stats = dayStats(days)
        var best = 0
        var running = 0
        stats.forEach { day ->
            if (day.presentes >= threshold) {
                running += 1
                best = maxOf(best, running)
            } else {
                running = 0
            }
        }

        var current = 0
        for (day in stats.asReversed()) {
            if (day.presentes >= threshold) {
                current += 1
            } else {
                break
            }
        }
        return PresenceStreakStats(current, best)
    }

    suspend fun resetAll() {
        dao.deleteAll()
    }

    private suspend fun createEvent(type: PresenceEventType, moodId: String?) {
        val now = System.currentTimeMillis()
        dao.insert(
            PresenceEventEntity(
                id = UUID.randomUUID().toString(),
                createdAtMillis = now,
                dayStartMillis = startOfDay(now),
                eventType = type.raw,
                moodId = moodId,
                note = "",
                source = source
            )
        )
    }

    private fun rangeStart(days: Int): Long {
        return startOfDay(System.currentTimeMillis()) - (days.coerceAtLeast(1) - 1) * DAY_MILLIS
    }

    private class MutablePresenceDayStats(val dateMillis: Long) {
        var presentes = 0
        var inconscientes = 0
        var estadosAnimo = 0
        var futuroAhora = 0

        fun toStats() = PresenceDayStats(dateMillis, presentes, inconscientes, estadosAnimo, futuroAhora)
    }

    companion object {
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        fun startOfDay(millis: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}
