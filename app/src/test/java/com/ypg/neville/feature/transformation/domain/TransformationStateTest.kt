package com.ypg.neville.feature.transformation.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TransformationStateTest {
    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun currentDay_isOneOnStartDayAndClampedAtTwentyOne() {
        val configuration = configuration(Instant.parse("2026-01-01T09:00:00Z").toEpochMilli())
        val state = TransformationState(configuration = configuration)

        assertEquals(1, state.currentDay(Clock.fixed(Instant.parse("2026-01-01T22:00:00Z"), zone), zone))
        assertEquals(21, state.currentDay(Clock.fixed(Instant.parse("2026-02-15T10:00:00Z"), zone), zone))
    }

    @Test
    fun metrics_deriveProgressAndAverageFromCompletedEntries() {
        val state = TransformationState(
            entries = listOf(
                TransformationDayEntry(1, morningCompleted = true, actionCompleted = true, eveningCompleted = true, score = TransformationScore(2, 2, 2, 2, 2)),
                TransformationDayEntry(2, eveningCompleted = true, score = TransformationScore(1, 1, 1, 1, 1))
            )
        )

        assertEquals(1, state.completedDays)
        assertEquals(1f / 21f, state.progress)
        assertEquals(7.5, state.averageScore, 0.001)
    }

    private fun configuration(startedAt: Long) = TransformationConfiguration(
        "Patrón", "señal", "pensamiento", "emoción", "conducta", "consecuencia",
        "alternativa", "incomodidad", "actúa con calma", startedAt, false, 480, 840, 1260
    )
}
