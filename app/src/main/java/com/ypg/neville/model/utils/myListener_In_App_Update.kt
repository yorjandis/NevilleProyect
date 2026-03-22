package com.ypg.neville.model.utils

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import java.util.concurrent.Executors

class myListener_In_App_Update(private val context: Context) {

    private var listener: In_mylistener? = null

    interface In_mylistener {
        fun onUpdateAvailable(pUpdateAvailable: Boolean)
    }

    fun setMylistener(plistener: In_mylistener) {
        this.listener = plistener
        doTask()
    }

    private fun doTask() {
        if (this.listener != null) {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                val appUpdateManager = AppUpdateManagerFactory.create(context)
                appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        listener?.onUpdateAvailable(true)
                    } else {
                        listener?.onUpdateAvailable(false)
                    }
                }
            }
        }
    }
}
