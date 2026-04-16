package com.ypg.neville.feature.morningdialog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettingsDataStore
import kotlinx.coroutines.runBlocking

class MorningDialogBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val appContext = context.applicationContext
        val settings = runBlocking { MorningDialogSettingsDataStore(appContext).getSettings() }
        val scheduler = MorningDialogScheduler(appContext)

        if (settings.enabled) {
            scheduler.scheduleDaily(settings.hour, settings.minute)
        } else {
            scheduler.cancel()
        }
    }
}
