package com.ypg.neville.feature.weeklysummary.domain

import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryDao
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEntity
import com.ypg.neville.feature.weeklysummary.data.WeeklySummarySectionOrderEntity
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeeklySummaryRepository(
    private val dao: WeeklySummaryDao
) {

    fun generatePendingSummaries(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()) {
        ensureSectionOrderSeeded()

        val lastClosedBoundary = WeeklySummaryTime.previousMondayBoundary(nowMillis, zoneId)
        val firstBoundaryToGenerate = computeFirstBoundaryToGenerate(lastClosedBoundary, zoneId)

        var boundary = firstBoundaryToGenerate
        while (boundary <= lastClosedBoundary) {
            val weekStart = boundary - WEEK_MS
            if (dao.getSummaryByWeekStart(weekStart) == null) {
                dao.upsertSummary(buildSummary(weekStart, boundary))
            }
            boundary += WEEK_MS
        }

        dao.purgeOldEvents(nowMillis - (180L * DAY_MS))
    }

    fun getSummaryByWeekStart(weekStart: Long): WeeklySummaryEntity? {
        return dao.getSummaryByWeekStart(weekStart)
    }

    fun getAllSummariesDesc(): List<WeeklySummaryEntity> {
        return dao.getAllSummariesDesc()
    }

    fun getCurrentOrderedSectionKeys(): List<String> {
        val fromDb = dao.getSectionOrder().sortedBy { it.position }.map { it.sectionKey }
        if (fromDb.isEmpty()) return DEFAULT_SECTION_ORDER
        return fromDb
    }

    fun moveSection(sectionKey: String, moveUp: Boolean) {
        val current = getCurrentOrderedSectionKeys().toMutableList()
        val index = current.indexOf(sectionKey)
        if (index == -1) return

        val target = if (moveUp) index - 1 else index + 1
        if (target !in current.indices) return

        val item = current.removeAt(index)
        current.add(target, item)

        dao.replaceSectionOrder(current.mapIndexed { idx, key -> WeeklySummarySectionOrderEntity(key, idx) })
    }

    fun toViewData(summary: WeeklySummaryEntity): WeeklySummaryViewData {
        val sectionMap = linkedMapOf(
            SECTION_NOTES to WeeklySummarySectionData(
                key = SECTION_NOTES,
                title = "Notas",
                metrics = listOf(
                    "Creadas" to summary.notesCreated,
                    "Modificadas" to summary.notesModified,
                    "Eliminadas" to summary.notesDeleted
                )
            ),
            SECTION_JOURNAL to WeeklySummarySectionData(
                key = SECTION_JOURNAL,
                title = "Diario",
                metrics = listOf(
                    "Creadas" to summary.journalCreated,
                    "Modificadas" to summary.journalModified,
                    "Eliminadas" to summary.journalDeleted
                )
            ),
            SECTION_CONFERENCES to WeeklySummarySectionData(
                key = SECTION_CONFERENCES,
                title = "Conferencias",
                metrics = listOf("Leídas (>= 1/3 scroll)" to summary.conferencesRead)
            ),
            SECTION_GOALS to WeeklySummarySectionData(
                key = SECTION_GOALS,
                title = "Metas",
                metrics = listOf(
                    "Creadas" to summary.goalsCreated,
                    "Completadas" to summary.goalsCompleted,
                    "En progreso" to summary.goalsInProgress
                )
            ),
            SECTION_REMINDERS to WeeklySummarySectionData(
                key = SECTION_REMINDERS,
                title = "Recordatorios",
                metrics = listOf(
                    "Creados" to summary.remindersCreated,
                    "Modificados" to summary.remindersModified,
                    "Eliminados" to summary.remindersDeleted
                )
            ),
            SECTION_VOICE to WeeklySummarySectionData(
                key = SECTION_VOICE,
                title = "Notas de Voz",
                metrics = listOf(
                    "Creadas" to summary.voiceCreated,
                    "Eliminadas" to summary.voiceDeleted
                )
            ),
            SECTION_ANCHORS to WeeklySummarySectionData(
                key = SECTION_ANCHORS,
                title = "Anclas Emocionales",
                metrics = listOf(
                    "Creadas" to summary.emotionalAnchorsCreated,
                    "Utilizadas" to summary.emotionalAnchorsUsed
                )
            ),
            SECTION_MORNING to WeeklySummarySectionData(
                key = SECTION_MORNING,
                title = "Ritual Matutino",
                metrics = listOf("Completados" to summary.morningRitualsCompleted)
            ),
            SECTION_PHRASES to WeeklySummarySectionData(
                key = SECTION_PHRASES,
                title = "Frases Personales",
                metrics = listOf(
                    "Creadas" to summary.personalPhrasesCreated,
                    "Modificadas" to summary.personalPhrasesModified,
                    "Eliminadas" to summary.personalPhrasesDeleted
                )
            ),
            SECTION_ENCYCLOPEDIA to WeeklySummarySectionData(
                key = SECTION_ENCYCLOPEDIA,
                title = "Enciclopedia",
                metrics = listOf("Artículos accedidos" to summary.encyclopediaAccessed)
            )
        )

        val orderedKeys = getCurrentOrderedSectionKeys()
        val sections = orderedKeys.mapNotNull { sectionMap[it] }
        return WeeklySummaryViewData(entity = summary, sections = sections)
    }

    private fun ensureSectionOrderSeeded() {
        if (dao.getSectionOrder().isNotEmpty()) return
        dao.replaceSectionOrder(
            DEFAULT_SECTION_ORDER.mapIndexed { index, key ->
                WeeklySummarySectionOrderEntity(sectionKey = key, position = index)
            }
        )
    }

    private fun computeFirstBoundaryToGenerate(lastClosedBoundary: Long, zoneId: ZoneId): Long {
        val fromSummary = dao.maxSummaryWeekEnd()
        if (fromSummary != null) {
            return fromSummary
        }

        val minEventTs = dao.minEventTimestamp()
        if (minEventTs == null) {
            return lastClosedBoundary + WEEK_MS
        }

        val eventWeekBoundary = WeeklySummaryTime.previousMondayBoundary(minEventTs, zoneId) + WEEK_MS
        return eventWeekBoundary
    }

    private fun buildSummary(weekStartMillis: Long, weekEndMillis: Long): WeeklySummaryEntity {
        return WeeklySummaryEntity(
            weekStartMillis = weekStartMillis,
            weekEndMillis = weekEndMillis,
            notesCreated = dao.countEvents(WeeklySummaryEventType.NOTES_CREATED, weekStartMillis, weekEndMillis),
            notesModified = dao.countEvents(WeeklySummaryEventType.NOTES_MODIFIED, weekStartMillis, weekEndMillis),
            notesDeleted = dao.countEvents(WeeklySummaryEventType.NOTES_DELETED, weekStartMillis, weekEndMillis),
            journalCreated = dao.countEvents(WeeklySummaryEventType.JOURNAL_CREATED, weekStartMillis, weekEndMillis),
            journalModified = dao.countEvents(WeeklySummaryEventType.JOURNAL_MODIFIED, weekStartMillis, weekEndMillis),
            journalDeleted = dao.countEvents(WeeklySummaryEventType.JOURNAL_DELETED, weekStartMillis, weekEndMillis),
            conferencesRead = dao.countDistinctTargets(WeeklySummaryEventType.CONFERENCE_READ, weekStartMillis, weekEndMillis),
            goalsCreated = dao.countEvents(WeeklySummaryEventType.GOALS_CREATED, weekStartMillis, weekEndMillis),
            goalsCompleted = dao.countDistinctTargets(WeeklySummaryEventType.GOALS_COMPLETED, weekStartMillis, weekEndMillis),
            goalsInProgress = dao.countDistinctTargets(WeeklySummaryEventType.GOALS_IN_PROGRESS, weekStartMillis, weekEndMillis),
            remindersCreated = dao.countEvents(WeeklySummaryEventType.REMINDERS_CREATED, weekStartMillis, weekEndMillis),
            remindersModified = dao.countEvents(WeeklySummaryEventType.REMINDERS_MODIFIED, weekStartMillis, weekEndMillis),
            remindersDeleted = dao.countEvents(WeeklySummaryEventType.REMINDERS_DELETED, weekStartMillis, weekEndMillis),
            voiceCreated = dao.countEvents(WeeklySummaryEventType.VOICE_CREATED, weekStartMillis, weekEndMillis),
            voiceDeleted = dao.countEvents(WeeklySummaryEventType.VOICE_DELETED, weekStartMillis, weekEndMillis),
            emotionalAnchorsCreated = dao.countEvents(WeeklySummaryEventType.ANCHORS_CREATED, weekStartMillis, weekEndMillis),
            emotionalAnchorsUsed = dao.countEvents(WeeklySummaryEventType.ANCHORS_USED, weekStartMillis, weekEndMillis),
            morningRitualsCompleted = dao.countMorningRitualsCompleted(weekStartMillis, weekEndMillis),
            personalPhrasesCreated = dao.countEvents(WeeklySummaryEventType.PHRASES_CREATED, weekStartMillis, weekEndMillis),
            personalPhrasesModified = dao.countEvents(WeeklySummaryEventType.PHRASES_MODIFIED, weekStartMillis, weekEndMillis),
            personalPhrasesDeleted = dao.countEvents(WeeklySummaryEventType.PHRASES_DELETED, weekStartMillis, weekEndMillis),
            encyclopediaAccessed = dao.countEvents(WeeklySummaryEventType.ENCYCLOPEDIA_ACCESSED, weekStartMillis, weekEndMillis)
        )
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
        private const val WEEK_MS = 7L * DAY_MS

        const val SECTION_NOTES = "notes"
        const val SECTION_JOURNAL = "journal"
        const val SECTION_CONFERENCES = "conferences"
        const val SECTION_GOALS = "goals"
        const val SECTION_REMINDERS = "reminders"
        const val SECTION_VOICE = "voice"
        const val SECTION_ANCHORS = "anchors"
        const val SECTION_MORNING = "morning"
        const val SECTION_PHRASES = "phrases"
        const val SECTION_ENCYCLOPEDIA = "encyclopedia"

        val DEFAULT_SECTION_ORDER = listOf(
            SECTION_NOTES,
            SECTION_JOURNAL,
            SECTION_CONFERENCES,
            SECTION_GOALS,
            SECTION_REMINDERS,
            SECTION_VOICE,
            SECTION_ANCHORS,
            SECTION_MORNING,
            SECTION_PHRASES,
            SECTION_ENCYCLOPEDIA
        )

        fun createDefault(): WeeklySummaryRepository {
            val appContext = requireNotNull(WeeklySummaryBootstrap.appContext) {
                "WeeklySummaryBootstrap no inicializado"
            }
            val db = NevilleRoomDatabase.getInstance(appContext)
            return WeeklySummaryRepository(db.weeklySummaryDao())
        }

        fun weekStartsForMonth(month: LocalDate, zoneId: ZoneId): List<Long> {
            val firstDay = month.withDayOfMonth(1)
            val firstMonday = firstDay.with(DayOfWeek.MONDAY)
            val normalizedFirst = if (firstMonday.isAfter(firstDay)) firstMonday.minusWeeks(1) else firstMonday
            val weekStarts = mutableListOf<Long>()
            var cursor = normalizedFirst
            while (cursor.month == month.month || cursor.plusDays(6).month == month.month) {
                weekStarts += WeeklySummaryTime.weekBoundaryAtMonday3(cursor, zoneId)
                cursor = cursor.plusWeeks(1)
            }
            return weekStarts
        }
    }
}
