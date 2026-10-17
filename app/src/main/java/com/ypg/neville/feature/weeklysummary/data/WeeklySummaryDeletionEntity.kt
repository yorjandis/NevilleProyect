package com.ypg.neville.feature.weeklysummary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Prevents a summary explicitly removed by the user from being generated again.
 */
@Entity(tableName = "weekly_summary_deletions")
data class WeeklySummaryDeletionEntity(
    @PrimaryKey
    val weekStartMillis: Long,
    val deletedAtMillis: Long = System.currentTimeMillis()
)
