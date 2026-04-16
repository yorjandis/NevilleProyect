package com.ypg.neville.feature.morningdialog.notifications

import android.content.Context
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object MorningDialogStartup {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun sync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val settings = MorningDialogSettingsDataStore(appContext).getSettings()
            val scheduler = MorningDialogScheduler(appContext)
            if (settings.enabled) {
                scheduler.scheduleDaily(settings.hour, settings.minute)
            } else {
                scheduler.cancel()
            }
        }
    }
}
