package com.ypg.neville.feature.weeklysummary.worker

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
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryRepository
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummarySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeeklySummaryReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val config = WeeklySummarySettings.readConfig(applicationContext)
        if (!config.notificationsEnabled) {
            return@withContext Result.success()
        }

        runCatching {
            WeeklySummaryRepository.createDefault().generatePendingSummaries()
        }
        showNotification()
        WeeklySummaryReminderScheduler.scheduleNext(applicationContext)
        Result.success()
    }

    private fun showNotification() {
        ensureChannel()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openSummaryIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_WEEKLY_SUMMARY, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            openSummaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .setContentTitle(applicationContext.getString(R.string.weekly_notification_title))
            .setContentText(applicationContext.getString(R.string.weekly_notification_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(applicationContext.getString(R.string.weekly_notification_body))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.weekly_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(
                    R.string.weekly_notification_channel_description
                )
            }
        )
    }

    companion object {
        private const val CHANNEL_ID = "weekly_summary_review_channel"
        private val NOTIFICATION_ID = "weekly_summary_review".hashCode()
    }
}
