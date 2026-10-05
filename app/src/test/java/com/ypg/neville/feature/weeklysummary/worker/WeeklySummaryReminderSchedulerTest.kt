package com.ypg.neville.feature.weeklysummary.worker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class WeeklySummaryReminderSchedulerTest {

    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun initialDelay_targetsSelectedDayAtSix() {
        val now = ZonedDateTime.of(2026, 7, 28, 12, 0, 0, 0, zone)

        val delay = WeeklySummaryReminderScheduler.initialDelay(
            day = DayOfWeek.SUNDAY,
            zoneId = zone,
            now = now
        )

        assertEquals(
            java.time.Duration.between(
                now,
                ZonedDateTime.of(2026, 8, 2, 6, 0, 0, 0, zone)
            ).toMillis(),
            delay
        )
    }

    @Test
    fun initialDelay_movesToNextWeekAfterSixOnSelectedDay() {
        val now = ZonedDateTime.of(2026, 8, 2, 6, 1, 0, 0, zone)

        val delay = WeeklySummaryReminderScheduler.initialDelay(
            day = DayOfWeek.SUNDAY,
            zoneId = zone,
            now = now
        )

        assertEquals(
            java.time.Duration.between(
                now,
                ZonedDateTime.of(2026, 8, 9, 6, 0, 0, 0, zone)
            ).toMillis(),
            delay
        )
    }
}
