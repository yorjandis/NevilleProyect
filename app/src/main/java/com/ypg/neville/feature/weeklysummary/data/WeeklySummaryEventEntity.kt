package com.ypg.neville.feature.weeklysummary.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_summary_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["eventType"]),
        Index(value = ["targetKey"])
    ]
)
data class WeeklySummaryEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String,
    val targetKey: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
