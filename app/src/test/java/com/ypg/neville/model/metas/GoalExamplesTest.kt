package com.ypg.neville.model.metas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalExamplesTest {

    @Test
    fun `catalog mirrors the thirty iOS examples with unique identifiers`() {
        assertEquals(30, GoalExamples.all.size)
        assertEquals(GoalExamples.all.size, GoalExamples.all.map { it.id }.toSet().size)
    }

    @Test
    fun `every example is a valid Android goal configuration`() {
        GoalExamples.all.forEach { example ->
            assertTrue(example.title.isNotBlank())
            assertTrue(example.details.isNotBlank())
            assertTrue(example.executionTargetValue > 0)
            assertTrue(example.amount > 0)
            assertTrue(example.durationValue > 0)
            assertTrue(example.frequency > 0)
            assertTrue(example.weeklyDaysPerWeek in 1..7)

            if (example.scheduleType == GoalScheduleType.SPECIFIC_DATES) {
                assertEquals(example.amount, example.specificDateOffsets.size)
            }
        }
    }

    @Test
    fun `weekly clock times map to an Android day period`() {
        GoalExamples.all
            .filter { it.weeklyTimeMinutes != null }
            .forEach { example ->
                assertTrue(example.androidDayPeriod != GoalDayPeriod.ANYTIME)
            }
    }
}
