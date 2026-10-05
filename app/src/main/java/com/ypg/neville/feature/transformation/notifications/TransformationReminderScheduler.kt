package com.ypg.neville.feature.transformation.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ypg.neville.feature.transformation.domain.TransformationConfiguration
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object TransformationReminderScheduler {
    fun sync(context: Context, configuration: TransformationConfiguration?) {
        val manager = WorkManager.getInstance(context.applicationContext)
        ReminderKind.entries.forEach { kind ->
            val workName = "transformation-${kind.name.lowercase()}"
            if (configuration?.remindersEnabled != true) {
                manager.cancelUniqueWork(workName)
            } else {
                val minute = when (kind) {
                    ReminderKind.Morning -> configuration.morningMinuteOfDay
                    ReminderKind.Pause -> configuration.pauseMinuteOfDay
                    ReminderKind.Evening -> configuration.eveningMinuteOfDay
                }.coerceIn(0, 1439)
                val request = PeriodicWorkRequestBuilder<TransformationReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayUntil(minute), TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder().putString(TransformationReminderWorker.KEY_KIND, kind.name).build())
                    .build()
                manager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.UPDATE, request)
            }
        }
    }

    internal fun delayUntil(minuteOfDay: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        val target = now.withHour(minuteOfDay / 60).withMinute(minuteOfDay % 60).withSecond(0).withNano(0)
        val next = if (target.isAfter(now)) target else target.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
    }
}

enum class ReminderKind { Morning, Pause, Evening }
