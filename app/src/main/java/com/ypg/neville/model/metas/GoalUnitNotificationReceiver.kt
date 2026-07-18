package com.ypg.neville.model.metas

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
import com.ypg.neville.model.db.room.NevilleRoomDatabase

class GoalUnitNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val goalId = intent?.getStringExtra(GoalUnitNotificationScheduler.EXTRA_GOAL_ID) ?: return
        val db = NevilleRoomDatabase.getInstance(context)
        val goalDao = db.goalDao()
        val unitDao = db.goalUnitDao()
        val goal = goalDao.getById(goalId) ?: return

        if (!goal.isStarted || !goal.notifyOnUnitAvailable) {
            GoalUnitNotificationScheduler.cancelPending(context, goalId)
            return
        }

        val now = System.currentTimeMillis()
        val nextAvailable = unitDao.getByGoalId(goalId)
            .asSequence()
            .filter { UnitStatus.fromRaw(it.status) == UnitStatus.PENDING }
            .filter { it.unitIndex > goal.lastNotifiedUnitIndex }
            .filter { (it.startDate ?: Long.MAX_VALUE) <= now }
            .minByOrNull { it.unitIndex }

        if (nextAvailable != null) {
            showNotification(
                context = context,
                goalTitle = goal.title,
                goalDescription = goal.descriptionText,
                unitName = nextAvailable.name,
                goalId = goal.id,
                unitId = nextAvailable.id
            )
            goalDao.update(goal.copy(lastNotifiedUnitIndex = nextAvailable.unitIndex))
        }

        GoalUnitNotificationScheduler.schedule(context, db, goalId)
    }

    private fun showNotification(
        context: Context,
        goalTitle: String,
        goalDescription: String,
        unitName: String,
        goalId: String,
        unitId: String
    ) {
        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_METAS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (goalId + unitId).hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openMetasIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_METAS, true)
        }
        val openMetasPendingIntent = PendingIntent.getActivity(
            context,
            (goalId + unitId + "_metas").hashCode(),
            openMetasIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val descriptionText = goalDescription.trim().ifBlank {
            context.getString(R.string.global_no_description)
        }
        val message = context.getString(R.string.global_goal_unit_available, unitName, descriptionText)
        val notification = NotificationCompat.Builder(context, GoalUnitNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .setContentTitle(goalTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_show, context.getString(R.string.global_open_goals), openMetasPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        NotificationManagerCompat.from(context)
            .notify((goalId + unitId).hashCode(), notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            GoalUnitNotificationScheduler.CHANNEL_ID,
            context.getString(R.string.global_goals_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.global_goals_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
