package com.ypg.neville.model.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL_ID = "reminder_channel"
    private const val WORK_INPUT_REMINDER_ID = "reminder_id"

    private fun uniqueWorkName(id: String): String = "reminder_work_$id"

    fun schedule(context: Context, reminder: ReminderEntity) {
        if (!reminder.isStarted) return

        val now = System.currentTimeMillis()
        val frequency = ReminderFrequency.fromEntity(reminder) ?: return
        val delayMillis = computeDelayMillis(frequency, now) ?: return

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(Data.Builder().putString(WORK_INPUT_REMINDER_ID, reminder.id).build())
            .setInitialDelay(delayMillis.coerceAtLeast(1000L), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName(reminder.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelPending(context: Context, reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(reminderId))
    }

    fun stop(context: Context, repository: ReminderRepository, reminderId: String) {
        val current = repository.get(reminderId) ?: return
        cancelPending(context, reminderId)
        repository.update(
            current.copy(
                isStarted = false,
                startedAt = null
            )
        )
    }

    fun resume(context: Context, repository: ReminderRepository, reminderId: String) {
        val current = repository.get(reminderId) ?: return
        val updated = current.copy(
            isStarted = true,
            startedAt = System.currentTimeMillis()
        )
        repository.update(updated)
        schedule(context, updated)
    }

    fun cancel(context: Context, repository: ReminderRepository, reminderId: String) {
        cancelPending(context, reminderId)
        repository.deleteById(reminderId)
    }

    private fun computeDelayMillis(frequency: ReminderFrequency, now: Long): Long? {
        return when (frequency) {
            is ReminderFrequency.Interval -> frequency.intervalMillis()
            else -> {
                val next = frequency.nextFireAt(now) ?: return null
                (next - now).coerceAtLeast(1000L)
            }
        }
    }

    fun readReminderId(data: Data): String? = data.getString(WORK_INPUT_REMINDER_ID)
}
