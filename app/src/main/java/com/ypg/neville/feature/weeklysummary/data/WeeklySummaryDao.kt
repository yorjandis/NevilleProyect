package com.ypg.neville.feature.weeklysummary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WeeklySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSummary(summary: WeeklySummaryEntity)

    @Query("SELECT * FROM weekly_summaries WHERE weekStartMillis = :weekStart LIMIT 1")
    fun getSummaryByWeekStart(weekStart: Long): WeeklySummaryEntity?

    @Query("SELECT * FROM weekly_summaries ORDER BY weekStartMillis DESC")
    fun getAllSummariesDesc(): List<WeeklySummaryEntity>

    @Query(
        "DELETE FROM weekly_summaries " +
            "WHERE weekStartMillis NOT IN (" +
            "SELECT weekStartMillis FROM weekly_summaries " +
            "ORDER BY weekStartMillis DESC LIMIT :keepLatest)"
    )
    fun deleteSummariesOlderThanLatest(keepLatest: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSectionOrder(items: List<WeeklySummarySectionOrderEntity>)

    @Query("SELECT * FROM weekly_summary_section_order ORDER BY position ASC")
    fun getSectionOrder(): List<WeeklySummarySectionOrderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEvent(event: WeeklySummaryEventEntity)

    @Query(
        "SELECT COUNT(*) FROM weekly_summary_events " +
            "WHERE eventType = :type AND timestamp >= :startMillis AND timestamp < :endMillis"
    )
    fun countEvents(type: String, startMillis: Long, endMillis: Long): Int

    @Query(
        "SELECT COUNT(DISTINCT targetKey) FROM weekly_summary_events " +
            "WHERE eventType = :type AND timestamp >= :startMillis AND timestamp < :endMillis"
    )
    fun countDistinctTargets(type: String, startMillis: Long, endMillis: Long): Int

    @Query(
        "DELETE FROM weekly_summary_events " +
            "WHERE timestamp < :olderThanMillis"
    )
    fun purgeOldEvents(olderThanMillis: Long)

    @Query(
        "SELECT COUNT(*) FROM morning_dialog_sessions " +
            "WHERE completed = 1 AND completedAtEpochMillis >= :startMillis AND completedAtEpochMillis < :endMillis"
    )
    fun countMorningRitualsCompleted(startMillis: Long, endMillis: Long): Int

    @Query(
        "SELECT COUNT(*) FROM evening_ritual_reviews " +
            "WHERE sessionDateEpochDay >= :startEpochDay AND sessionDateEpochDay < :endEpochDay"
    )
    fun countEveningRitualsCompleted(startEpochDay: Long, endEpochDay: Long): Int

    @Query(
        "SELECT COUNT(*) FROM evening_ritual_reviews AS evening " +
            "INNER JOIN morning_dialog_sessions AS morning " +
            "ON morning.sessionDateEpochDay = evening.sessionDateEpochDay " +
            "WHERE morning.completed = 1 " +
            "AND evening.sessionDateEpochDay >= :startEpochDay " +
            "AND evening.sessionDateEpochDay < :endEpochDay"
    )
    fun countCompletedRitualCycles(startEpochDay: Long, endEpochDay: Long): Int

    @Query(
        "SELECT COALESCE(CAST(ROUND(AVG(energy)) AS INTEGER), 0) FROM evening_ritual_reviews " +
            "WHERE sessionDateEpochDay >= :startEpochDay AND sessionDateEpochDay < :endEpochDay"
    )
    fun averageEveningEnergy(startEpochDay: Long, endEpochDay: Long): Int

    @Query(
        "SELECT COALESCE(CAST(ROUND(AVG(identityAlignment)) AS INTEGER), 0) FROM evening_ritual_reviews " +
            "WHERE sessionDateEpochDay >= :startEpochDay AND sessionDateEpochDay < :endEpochDay"
    )
    fun averageEveningIdentityAlignment(startEpochDay: Long, endEpochDay: Long): Int

    @Query(
        "SELECT COALESCE(SUM(presenceReturns), 0) FROM evening_ritual_reviews " +
            "WHERE sessionDateEpochDay >= :startEpochDay AND sessionDateEpochDay < :endEpochDay"
    )
    fun sumEveningPresenceReturns(startEpochDay: Long, endEpochDay: Long): Int

    @Query(
        "SELECT COALESCE(SUM(goalUnitsCompletedCount), 0) FROM evening_ritual_reviews " +
            "WHERE sessionDateEpochDay >= :startEpochDay AND sessionDateEpochDay < :endEpochDay"
    )
    fun sumEveningGoalUnitsCompleted(startEpochDay: Long, endEpochDay: Long): Int

    @Query(
        "SELECT COUNT(*) FROM cardio_coherence_records " +
            "WHERE dateEpochMillis >= :startMillis AND dateEpochMillis < :endMillis"
    )
    fun countCardioCoherenceSessions(startMillis: Long, endMillis: Long): Int

    @Query(
        "SELECT COALESCE(SUM(durationMinutes), 0) FROM cardio_coherence_records " +
            "WHERE dateEpochMillis >= :startMillis AND dateEpochMillis < :endMillis"
    )
    fun sumCardioCoherenceMinutes(startMillis: Long, endMillis: Long): Int

    @Query(
        "SELECT COALESCE(SUM(afterScore - beforeScore), 0) FROM cardio_coherence_records " +
            "WHERE dateEpochMillis >= :startMillis AND dateEpochMillis < :endMillis"
    )
    fun sumCardioCoherenceScoreDelta(startMillis: Long, endMillis: Long): Int

    @Query("SELECT MIN(timestamp) FROM weekly_summary_events")
    fun minEventTimestamp(): Long?

    @Query("SELECT MIN(dateEpochMillis) FROM cardio_coherence_records")
    fun minCardioCoherenceRecordTimestamp(): Long?

    @Query("SELECT MIN(sessionDateEpochDay) FROM evening_ritual_reviews")
    fun minEveningReviewEpochDay(): Long?

    @Query("SELECT MAX(weekEndMillis) FROM weekly_summaries")
    fun maxSummaryWeekEnd(): Long?

    @Transaction
    fun replaceSectionOrder(items: List<WeeklySummarySectionOrderEntity>) {
        upsertSectionOrder(items)
    }
}
