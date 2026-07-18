package com.ypg.neville.feature.morningdialog.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ypg.neville.MainActivity
import com.ypg.neville.R

class MorningDialogNotificationHelper(
    private val context: Context
) {

    private val appContext = context.applicationContext

    fun showMorningDialogNotification() {
        ensureChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_MORNING_DIALOG, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            "morning_dialog_open".hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, MorningDialogNotificationConfig.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .setContentTitle(appContext.getString(R.string.global_ritual_morning_title))
            .setContentText(appContext.getString(R.string.global_ritual_morning_text))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    appContext.getString(R.string.global_ritual_morning_text)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(appContext)
            .notify(MorningDialogNotificationConfig.NOTIFICATION_ID, notification)
    }

    fun showDayReminderNotification(sessionId: Long, reminderIndex: Int) {
        ensureChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_MORNING_DIALOG_DETAIL_ID, sessionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            "morning_dialog_open_detail_$sessionId".hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = appContext.getString(R.string.global_ritual_day_text)
        val notification = NotificationCompat.Builder(appContext, MorningDialogNotificationConfig.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .setContentTitle(appContext.getString(R.string.global_ritual_day_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(
            MorningDialogNotificationConfig.NOTIFICATION_ID_DAY_BASE + reminderIndex,
            notification
        )
    }

    fun showEveningRitualNotification() {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_MY_DAY, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            "evening_ritual_open".hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = appContext.getString(R.string.global_ritual_evening_text)
        val notification = NotificationCompat.Builder(appContext, MorningDialogNotificationConfig.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .setContentTitle(appContext.getString(R.string.global_ritual_evening_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(appContext).notify(
            MorningDialogNotificationConfig.NOTIFICATION_ID_EVENING,
            notification
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            MorningDialogNotificationConfig.CHANNEL_ID,
            appContext.getString(R.string.global_ritual_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.global_ritual_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
