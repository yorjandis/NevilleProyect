package com.ypg.neville.model.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class JournalDailyReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        JournalDailyReminderManager.sync(context)
    }
}

