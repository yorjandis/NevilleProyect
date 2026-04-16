package com.ypg.neville.feature.weeklysummary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_summary_section_order")
data class WeeklySummarySectionOrderEntity(
    @PrimaryKey
    val sectionKey: String,
    val position: Int
)
