package com.ypg.neville.feature.weeklysummary.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummarySettings
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object WeeklySummaryReminderScheduler {

    private const val UNIQUE_WORK_NAME = "weekly-summary-review-reminder"

    fun sync(context: Context) {
        schedule(context, ExistingWorkPolicy.REPLACE)
    }

    fun scheduleNext(context: Context) {
        schedule(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun schedule(context: Context, policy: ExistingWorkPolicy) {
        val appContext = context.applicationContext
        val config = WeeklySummarySettings.readConfig(appContext)
        val workManager = WorkManager.getInstance(appContext)
        if (!config.notificationsEnabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val request = OneTimeWorkRequestBuilder<WeeklySummaryReminderWorker>()
            .setInitialDelay(
                initialDelay(
                    day = config.day.dayOfWeek,
                    zoneId = ZoneId.systemDefault()
                ),
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            policy,
            request
        )
    }

    internal fun initialDelay(
        day: java.time.DayOfWeek,
        zoneId: ZoneId,
        now: ZonedDateTime = ZonedDateTime.now(zoneId)
    ): Long {
        val candidate = now
            .with(TemporalAdjusters.nextOrSame(day))
            .withHour(WeeklySummarySettings.AVAILABLE_HOUR)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val next = if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
    }
}
