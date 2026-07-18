package com.ypg.neville.model.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ypg.neville.MainActivity
import com.ypg.neville.R

class JournalDailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_DISABLE) {
            JournalDailyReminderManager.setEnabled(context, false)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }

        val config = JournalDailyReminderManager.readConfig(context)
        if (!config.enabled) {
            JournalDailyAlarmScheduler.cancel(context)
            return
        }

        showNotification(context, config.resolvedMessage)
        JournalDailyAlarmScheduler.schedule(context, config.hour, config.minute)
    }

    private fun showNotification(context: Context, message: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            "journal_daily_open".hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openDiarioIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_DIARIO, true)
        }
        val openDiarioPendingIntent = PendingIntent.getActivity(
            context,
            "journal_daily_open_diario".hashCode(),
            openDiarioIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disableIntent = Intent(context, JournalDailyReminderReceiver::class.java).apply {
            action = ACTION_DISABLE
        }
        val disablePendingIntent = PendingIntent.getBroadcast(
            context,
            "journal_daily_disable".hashCode(),
            disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .setContentTitle(context.getString(R.string.global_journal_reminder_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_note, context.getString(R.string.global_open_journal), openDiarioPendingIntent)
            .addAction(R.drawable.ic_delete, context.getString(R.string.global_disable), disablePendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            context.getString(R.string.global_reminders_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.global_reminders_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_DISABLE = "com.ypg.neville.reminder.JOURNAL_DAILY_DISABLE"
        private val NOTIFICATION_ID = "journal_daily".hashCode()
    }
}
