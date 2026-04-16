package com.ypg.neville.model.metas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ypg.neville.model.db.room.NevilleRoomDatabase

class GoalUnitBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val db = NevilleRoomDatabase.getInstance(context)
        db.goalDao()
            .getAll()
            .filter { it.isStarted && it.notifyOnUnitAvailable }
            .forEach { goal ->
                GoalUnitNotificationScheduler.schedule(context, db, goal.id)
            }
    }
}

