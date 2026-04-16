package com.ypg.neville.feature.weeklysummary.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object WeeklySummaryScheduler {

    private const val UNIQUE_WORK_NAME = "weekly-summary-generator"

    fun sync(context: Context) {
        val delay = initialDelayToNextMonday3am(ZoneId.systemDefault())
        val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun initialDelayToNextMonday3am(zoneId: ZoneId): Long {
        val now = LocalDateTime.now(zoneId)
        val base = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            .withHour(3)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val next = if (base.isAfter(now)) base else base.plusWeeks(1)
        return java.time.Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
    }
}
