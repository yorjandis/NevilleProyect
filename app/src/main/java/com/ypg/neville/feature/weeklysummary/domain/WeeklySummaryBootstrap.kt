package com.ypg.neville.feature.weeklysummary.domain

import android.content.Context
import com.ypg.neville.feature.weeklysummary.worker.WeeklySummaryScheduler

object WeeklySummaryBootstrap {

    @Volatile
    var appContext: Context? = null
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
        WeeklySummaryScheduler.sync(context.applicationContext)
    }
}
