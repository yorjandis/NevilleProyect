package com.ypg.neville.feature.weeklysummary.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklySummarySettingsTest {

    @Test
    fun normalizedRecordsToKeep_clampsAndUsesFiveRecordSteps() {
        assertEquals(5, WeeklySummarySettings.normalizedRecordsToKeep(1))
        assertEquals(5, WeeklySummarySettings.normalizedRecordsToKeep(9))
        assertEquals(30, WeeklySummarySettings.normalizedRecordsToKeep(33))
        assertEquals(100, WeeklySummarySettings.normalizedRecordsToKeep(104))
    }

    @Test
    fun reviewDay_usesSundayAsFallback() {
        assertEquals(
            WeeklySummaryReviewDay.Sunday,
            WeeklySummaryReviewDay.fromStoredValue(Int.MIN_VALUE)
        )
    }
}
