package com.ypg.neville.model.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CloudBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val manager = CloudBackupManager(applicationContext)
        if (manager.getProviderInfo() == null) {
            return Result.success()
        }
        if (manager.getFrequency() == CloudBackupManager.BackupFrequency.MANUAL) {
            return Result.success()
        }
        if (!manager.hasSavedPassphrase()) {
            return Result.success()
        }

        return when (manager.backupNow()) {
            is CloudBackupManager.BackupResult.Success -> Result.success()
            is CloudBackupManager.BackupResult.Error -> Result.retry()
        }
    }
}
