package com.ypg.neville.feature.weeklysummary.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklySummaryTimeTest {

    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun weeklyBoundaries_followLocalMondaysAcrossDaylightSavingTime() {
        val first = WeeklySummaryTime.weekBoundaryAtMonday3(
            LocalDate.of(2026, 3, 23),
            zone
        )
        val next = WeeklySummaryTime.weekBoundaryAtMonday3(
            LocalDate.of(2026, 3, 30),
            zone
        )

        val firstLocal = Instant.ofEpochMilli(first).atZone(zone)
        val nextLocal = Instant.ofEpochMilli(next).atZone(zone)

        assertEquals(LocalDate.of(2026, 3, 23), firstLocal.toLocalDate())
        assertEquals(LocalDate.of(2026, 3, 30), nextLocal.toLocalDate())
        assertEquals(LocalTime.of(3, 0), firstLocal.toLocalTime())
        assertEquals(LocalTime.of(3, 0), nextLocal.toLocalTime())
        assertEquals(167, Duration.between(firstLocal, nextLocal).toHours())
    }
}
