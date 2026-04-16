package com.ypg.neville.model.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ypg.neville.model.db.room.NevilleRoomDatabase

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_STOP) return

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val db = NevilleRoomDatabase.getInstance(context)
        val repository = ReminderRepository(db.reminderDao())
        ReminderScheduler.stop(context, repository, reminderId)
    }

    companion object {
        const val ACTION_STOP = "com.ypg.neville.reminder.ACTION_STOP"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
