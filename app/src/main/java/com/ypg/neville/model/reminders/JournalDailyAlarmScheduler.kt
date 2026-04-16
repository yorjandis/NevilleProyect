package com.ypg.neville.model.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object JournalDailyAlarmScheduler {

    private const val REQUEST_CODE = 20260414

    fun schedule(context: Context, hour: Int, minute: Int) {
        val triggerAt = computeNextTrigger(hour, minute)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context)

        alarmManager.cancel(pendingIntent)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            else ->
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, JournalDailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextTrigger(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return calendar.timeInMillis
    }
}

