package com.ypg.neville.model.metas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ypg.neville.model.db.room.NevilleRoomDatabase

object GoalUnitNotificationScheduler {
    const val CHANNEL_ID = "goal_unit_channel"

    const val EXTRA_GOAL_ID = "goal_id"

    fun schedule(context: Context, db: NevilleRoomDatabase, goalId: String) {
        val goal = db.goalDao().getById(goalId)
        if (goal == null || !goal.isStarted || !goal.notifyOnUnitAvailable) {
            cancelPending(context, goalId)
            return
        }

        val next = db.goalUnitDao()
            .getByGoalId(goalId)
            .asSequence()
            .filter { UnitStatus.fromRaw(it.status) == UnitStatus.PENDING }
            .filter { it.unitIndex > goal.lastNotifiedUnitIndex }
            .minByOrNull { it.unitIndex }

        if (next == null) {
            cancelPending(context, goalId)
            return
        }

        val now = System.currentTimeMillis()
        val triggerAt = (next.startDate ?: now + 1000L).coerceAtLeast(now + 1000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, goalId)
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

    fun cancelPending(context: Context, goalId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, goalId))
    }

    private fun pendingIntent(context: Context, goalId: String): PendingIntent {
        val intent = Intent(context, GoalUnitNotificationReceiver::class.java).apply {
            putExtra(EXTRA_GOAL_ID, goalId)
        }
        return PendingIntent.getBroadcast(
            context,
            goalId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
