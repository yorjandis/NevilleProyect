package com.ypg.neville.model.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.room.NevilleRoomDatabase

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val reminderId = ReminderScheduler.readReminderId(inputData) ?: return Result.success()
        val db = NevilleRoomDatabase.getInstance(applicationContext)
        val repository = ReminderRepository(db.reminderDao())
        val reminder = repository.get(reminderId) ?: return Result.success()

        if (!reminder.isStarted) {
            return Result.success()
        }

        showNotification(reminder)

        val frequency = ReminderFrequency.fromEntity(reminder)
        if (frequency is ReminderFrequency.DateOnce) {
            repository.update(reminder.copy(isStarted = false, startedAt = null))
            ReminderScheduler.cancelPending(applicationContext, reminder.id)
            return Result.success()
        }

        val refreshed = repository.get(reminderId)
        if (refreshed?.isStarted == true) {
            ReminderScheduler.schedule(applicationContext, refreshed)
        }

        return Result.success()
    }

    private fun showNotification(reminder: ReminderEntity) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            applicationContext,
            reminder.id.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(applicationContext, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_STOP
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            reminder.id.hashCode() + 3000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .setContentTitle(reminder.title)
            .setContentText(reminder.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_delete, "Detener", stopPendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(reminder.id.hashCode(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(ReminderScheduler.CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            "Recordatorios",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de recordatorios"
        }
        manager.createNotificationChannel(channel)
    }
}
