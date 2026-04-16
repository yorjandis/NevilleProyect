package com.ypg.neville.feature.morningdialog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettingsDataStore
import kotlinx.coroutines.runBlocking

class MorningDialogAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        val appContext = context.applicationContext
        val notificationHelper = MorningDialogNotificationHelper(appContext)

        if (action == MorningDialogNotificationConfig.ACTION_DAY_REMINDER_FIRE) {
            val sessionId = intent.getLongExtra(MorningDialogScheduler.EXTRA_SESSION_ID, -1L)
            if (sessionId > 0L) {
                val reminderIndex = intent.getIntExtra(MorningDialogScheduler.EXTRA_DAY_REMINDER_INDEX, 0)
                notificationHelper.showDayReminderNotification(sessionId, reminderIndex)
            }
            return
        }

        if (action != MorningDialogNotificationConfig.ACTION_FIRE) return

        val settingsStore = MorningDialogSettingsDataStore(appContext)
        val settings = runBlocking { settingsStore.getSettings() }

        if (!settings.enabled) {
            MorningDialogScheduler(appContext).cancel()
            return
        }

        notificationHelper.showMorningDialogNotification()
        MorningDialogScheduler(appContext).scheduleDaily(settings.hour, settings.minute)
    }
}
