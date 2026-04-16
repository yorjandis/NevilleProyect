package com.ypg.neville.feature.weeklysummary.domain

import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEntity

const val WEEKLY_SUMMARY_GENERATION_HOUR = 3

data class WeeklySummarySectionData(
    val key: String,
    val title: String,
    val metrics: List<Pair<String, Int>>
)

data class WeeklySummaryViewData(
    val entity: WeeklySummaryEntity,
    val sections: List<WeeklySummarySectionData>
)
