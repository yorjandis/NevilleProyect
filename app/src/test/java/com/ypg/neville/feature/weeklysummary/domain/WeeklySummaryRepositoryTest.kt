package com.ypg.neville.feature.weeklysummary.domain

import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryDao
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEntity
import com.ypg.neville.feature.weeklysummary.data.WeeklySummarySectionOrderEntity
import java.lang.reflect.Proxy
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklySummaryRepositoryTest {

    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun generatePendingSummaries_fillsEachMissingClosedWeekExactlyOnce() {
        val march23 = boundary(LocalDate.of(2026, 3, 23))
        val march30 = boundary(LocalDate.of(2026, 3, 30))
        val april6 = boundary(LocalDate.of(2026, 4, 6))
        val storedStarts = mutableSetOf(march23, april6)
        val inserted = mutableListOf<WeeklySummaryEntity>()
        val repository = WeeklySummaryRepository(fakeDao(storedStarts, inserted))

        repository.generatePendingSummaries(
            nowMillis = ZonedDateTime.of(2026, 4, 15, 12, 0, 0, 0, zone)
                .toInstant()
                .toEpochMilli(),
            zoneId = zone
        )

        assertEquals(listOf(march30), inserted.map { it.weekStartMillis })
        assertEquals(
            boundary(LocalDate.of(2026, 4, 6)),
            inserted.single().weekEndMillis
        )
    }

    @Test
    fun generatePendingSummaries_createsPreviousWeekWhenThereIsNoHistory() {
        val inserted = mutableListOf<WeeklySummaryEntity>()
        val repository = WeeklySummaryRepository(fakeDao(mutableSetOf(), inserted))

        repository.generatePendingSummaries(
            nowMillis = ZonedDateTime.of(2026, 4, 15, 12, 0, 0, 0, zone)
                .toInstant()
                .toEpochMilli(),
            zoneId = zone
        )

        assertEquals(
            listOf(boundary(LocalDate.of(2026, 4, 6))),
            inserted.map { it.weekStartMillis }
        )
    }

    private fun boundary(date: LocalDate): Long {
        return WeeklySummaryTime.weekBoundaryAtMonday3(date, zone)
    }

    private fun fakeDao(
        storedStarts: MutableSet<Long>,
        inserted: MutableList<WeeklySummaryEntity>
    ): WeeklySummaryDao {
        return Proxy.newProxyInstance(
            WeeklySummaryDao::class.java.classLoader,
            arrayOf(WeeklySummaryDao::class.java)
        ) { _, method, arguments ->
            when (method.name) {
                "getAllSummaryWeekStarts" -> storedStarts.toList()
                "getDeletedWeekStarts" -> emptyList<Long>()
                "getSectionOrder" -> WeeklySummaryRepository.DEFAULT_SECTION_ORDER.mapIndexed {
                        index, key ->
                    WeeklySummarySectionOrderEntity(key, index)
                }
                "upsertSummary" -> {
                    val summary = arguments.orEmpty().first() as WeeklySummaryEntity
                    inserted += summary
                    storedStarts += summary.weekStartMillis
                    Unit
                }
                "minEventTimestamp",
                "minCardioCoherenceRecordTimestamp",
                "minEveningReviewEpochDay",
                "maxSummaryWeekEnd" -> null
                else -> when (method.returnType) {
                    Integer.TYPE -> 0
                    Long::class.javaPrimitiveType -> 0L
                    Boolean::class.javaPrimitiveType -> false
                    Void.TYPE -> Unit
                    List::class.java -> emptyList<Any>()
                    else -> null
                }
            }
        } as WeeklySummaryDao
    }
}
