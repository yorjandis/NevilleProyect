package com.ypg.neville.feature.transformation.notifications

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ypg.neville.MainActivity
import com.ypg.neville.R

class TransformationReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val kind = runCatching { ReminderKind.valueOf(inputData.getString(KEY_KIND).orEmpty()) }.getOrNull()
            ?: return Result.failure()
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val (title, body) = when (kind) {
            ReminderKind.Morning -> R.string.transformation_notification_morning_title to R.string.transformation_notification_morning_body
            ReminderKind.Pause -> R.string.transformation_notification_pause_title to R.string.transformation_notification_pause_body
            ReminderKind.Evening -> R.string.transformation_notification_evening_title to R.string.transformation_notification_evening_body
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_TRANSFORMATION_PROTOCOL, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, NOTIFICATION_BASE_ID + kind.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tips)
            .setContentTitle(applicationContext.getString(title))
            .setContentText(applicationContext.getString(body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(applicationContext.getString(body)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_BASE_ID + kind.ordinal, notification)
        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.transformation_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    companion object {
        const val KEY_KIND = "kind"
        private const val CHANNEL_ID = "transformation_protocol"
        private const val NOTIFICATION_BASE_ID = 24100
    }
}
