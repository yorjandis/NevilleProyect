package com.ypg.neville.model.backup

import android.content.Context
import com.ypg.neville.model.preferences.DbPreferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CloudBackupScheduler {

    private const val UNIQUE_WORK_NAME = "cloud_backup_periodic"

    fun sync(context: Context) {
        val appContext = context.applicationContext
        val prefs = DbPreferences.default(appContext)
        val manager = CloudBackupManager(appContext)
        val hasProvider = !prefs.getString(CloudBackupManager.KEY_PROVIDER_URI, null).isNullOrBlank()
        val hasPassphrase = manager.hasSavedPassphrase()
        val frequency = CloudBackupManager.BackupFrequency.from(
            prefs.getString(
                CloudBackupManager.KEY_BACKUP_FREQUENCY,
                CloudBackupManager.BackupFrequency.MANUAL.name
            )
        )

        val wm = WorkManager.getInstance(appContext)
        if (!hasProvider || !hasPassphrase || frequency == CloudBackupManager.BackupFrequency.MANUAL) {
            wm.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val intervalDays = when (frequency) {
            CloudBackupManager.BackupFrequency.DAILY -> 1L
            CloudBackupManager.BackupFrequency.WEEKLY -> 7L
            CloudBackupManager.BackupFrequency.MANUAL -> 1L
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(intervalDays, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        wm.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
