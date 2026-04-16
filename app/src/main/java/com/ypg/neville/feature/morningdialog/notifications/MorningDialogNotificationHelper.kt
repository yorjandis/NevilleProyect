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
            .setContentTitle(MorningDialogNotificationConfig.TITLE)
            .setContentText(MorningDialogNotificationConfig.TEXT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(MorningDialogNotificationConfig.TEXT))
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

        val text = "Vuelve a tu intención consciente de hoy."
        val notification = NotificationCompat.Builder(appContext, MorningDialogNotificationConfig.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .setContentTitle("Recordatorio de tu ritual")
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

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(MorningDialogNotificationConfig.CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            MorningDialogNotificationConfig.CHANNEL_ID,
            MorningDialogNotificationConfig.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = MorningDialogNotificationConfig.CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }
}
