package com.ypg.neville.feature.weeklysummary.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeeklySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            WeeklySummaryRepository.createDefault().generatePendingSummaries()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
