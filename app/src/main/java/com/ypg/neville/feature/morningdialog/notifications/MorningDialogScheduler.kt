package com.ypg.neville.feature.morningdialog.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import java.time.LocalDate
import java.time.ZoneId

class MorningDialogScheduler(
    private val context: Context
) {

    private val appContext = context.applicationContext

    fun scheduleDaily(hour: Int, minute: Int) {
        val triggerAt = computeNextTrigger(hour, minute)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent()

        alarmManager.cancel(pendingIntent)

        // Para una hora diaria específica configurada por el usuario, AlarmManager es más adecuado
        // que WorkManager: WorkManager no garantiza exactitud al minuto y está orientado a trabajo
        // deferible. Aquí priorizamos disparo temporal consistente para UX de ritual matutino.
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            else -> {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun cancel() {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent())
    }

    fun scheduleSessionDayReminders(sessionId: Long, timesInMinutes: List<Int>) {
        cancelSessionDayReminders(sessionId)
        val today = LocalDate.now(ZoneId.systemDefault())
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        timesInMinutes
            .distinct()
            .sorted()
            .take(MAX_DAY_REMINDERS)
            .forEachIndexed { index, minuteOfDay ->
                val triggerAt = computeTodayTriggerIfFuture(today, minuteOfDay) ?: return@forEachIndexed
                val pendingIntent = dayReminderPendingIntent(
                    sessionId = sessionId,
                    reminderIndex = index
                )
                alarmManager.cancel(pendingIntent)
                setAlarm(alarmManager, triggerAt, pendingIntent)
            }
    }

    fun cancelSessionDayReminders(sessionId: Long) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        repeat(MAX_DAY_REMINDERS) { index ->
            alarmManager.cancel(dayReminderPendingIntent(sessionId, index))
        }
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(appContext, MorningDialogAlarmReceiver::class.java).apply {
            action = MorningDialogNotificationConfig.ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            appContext,
            MorningDialogNotificationConfig.REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dayReminderPendingIntent(sessionId: Long, reminderIndex: Int): PendingIntent {
        val intent = Intent(appContext, MorningDialogAlarmReceiver::class.java).apply {
            action = MorningDialogNotificationConfig.ACTION_DAY_REMINDER_FIRE
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_DAY_REMINDER_INDEX, reminderIndex)
        }
        return PendingIntent.getBroadcast(
            appContext,
            buildRequestCodeForDayReminder(sessionId, reminderIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAt: Long, pendingIntent: PendingIntent) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            else -> {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    private fun computeTodayTriggerIfFuture(day: LocalDate, minuteOfDay: Int): Long? {
        val now = System.currentTimeMillis()
        val hour = (minuteOfDay / 60).coerceIn(0, 23)
        val minute = (minuteOfDay % 60).coerceIn(0, 59)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, day.year)
            set(Calendar.MONTH, day.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, day.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis.takeIf { it > now }
    }

    private fun buildRequestCodeForDayReminder(sessionId: Long, reminderIndex: Int): Int {
        return "md_day_${sessionId}_$reminderIndex".hashCode()
    }

    private fun computeNextTrigger(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_DAY_REMINDER_INDEX = "extra_day_reminder_index"
        const val MAX_DAY_REMINDERS = 6
        val DEFAULT_DAY_REMINDER_TIMES: List<Int> = listOf(
            11 * 60,
            14 * 60,
            17 * 60,
            20 * 60,
            22 * 60
        )
    }
}
