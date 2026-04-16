package com.ypg.neville.feature.weeklysummary.domain

import android.util.Log
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEventEntity
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.util.concurrent.Executors

object WeeklySummaryEventLogger {

    private val executor = Executors.newSingleThreadExecutor()

    fun log(eventType: String, targetKey: String = "", timestamp: Long = System.currentTimeMillis()) {
        val appContext = WeeklySummaryBootstrap.appContext ?: return
        executor.execute {
            runCatching {
                NevilleRoomDatabase.getInstance(appContext)
                    .weeklySummaryDao()
                    .insertEvent(
                        WeeklySummaryEventEntity(
                            eventType = eventType,
                            targetKey = targetKey,
                            timestamp = timestamp
                        )
                    )
            }.onFailure {
                Log.w("WeeklySummaryEventLogger", "No se pudo registrar evento $eventType", it)
            }
        }
    }
}
