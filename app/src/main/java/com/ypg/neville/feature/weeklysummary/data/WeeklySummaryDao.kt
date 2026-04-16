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

    @Query("SELECT MIN(timestamp) FROM weekly_summary_events")
    fun minEventTimestamp(): Long?

    @Query("SELECT MAX(weekEndMillis) FROM weekly_summaries")
    fun maxSummaryWeekEnd(): Long?

    @Transaction
    fun replaceSectionOrder(items: List<WeeklySummarySectionOrderEntity>) {
        upsertSectionOrder(items)
    }
}
