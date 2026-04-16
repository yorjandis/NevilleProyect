package com.ypg.neville.feature.weeklysummary.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object WeeklySummaryTime {

    fun weekBoundaryAtMonday3(date: LocalDate, zoneId: ZoneId): Long {
        return LocalDateTime.of(date, LocalTime.of(WEEKLY_SUMMARY_GENERATION_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun previousMondayBoundary(nowMillis: Long, zoneId: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val mondayDate = now.toLocalDate().with(DayOfWeek.MONDAY)
        val currentMondayBoundary = weekBoundaryAtMonday3(mondayDate, zoneId)
        return if (nowMillis >= currentMondayBoundary) {
            currentMondayBoundary
        } else {
            weekBoundaryAtMonday3(mondayDate.minusWeeks(1), zoneId)
        }
    }

    fun weekLabel(weekStartMillis: Long, weekEndMillis: Long, zoneId: ZoneId): String {
        val start = Instant.ofEpochMilli(weekStartMillis).atZone(zoneId).toLocalDate()
        val end = Instant.ofEpochMilli(weekEndMillis - 1L).atZone(zoneId).toLocalDate()
        return "$start - $end"
    }
}
