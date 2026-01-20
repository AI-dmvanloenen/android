package com.odoo.fieldapp.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and manages background sync operations
 *
 * Handles:
 * - Periodic sync (every 15 minutes when connected)
 * - One-time sync when connectivity is restored
 * - Cancellation of sync work
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SyncScheduler"
        private const val PERIODIC_SYNC_INTERVAL_MINUTES = 15L
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Network constraints requiring internet connectivity
     */
    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Schedule periodic sync that runs every 15 minutes when network is available
     * Uses KEEP policy to avoid rescheduling if already scheduled
     */
    fun schedulePeriodicSync() {
        Log.d(TAG, "Scheduling periodic sync every $PERIODIC_SYNC_INTERVAL_MINUTES minutes")

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    /**
     * Trigger an immediate one-time sync
     * Useful when network connectivity is restored
     */
    fun triggerImmediateSync() {
        Log.d(TAG, "Triggering immediate sync")

        val oneTimeSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            oneTimeSyncRequest
        )
    }

    /**
     * Cancel all scheduled sync work
     */
    fun cancelAllSync() {
        Log.d(TAG, "Cancelling all sync work")
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME_PERIODIC)
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME_ONE_TIME)
    }
}
