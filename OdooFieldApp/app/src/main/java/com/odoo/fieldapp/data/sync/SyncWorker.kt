package com.odoo.fieldapp.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.odoo.fieldapp.domain.connectivity.NetworkMonitor
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.repository.DashboardRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that performs background sync of all entities
 *
 * This worker is triggered:
 * - Periodically (every 15 minutes)
 * - When network connectivity is restored
 * - On app startup if configured
 *
 * It performs two tasks:
 * 1. Sync data from Odoo (pull)
 * 2. Process pending queue items (push)
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dashboardRepository: DashboardRepository,
    private val syncQueueManager: SyncQueueManager,
    private val networkMonitor: NetworkMonitor
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME_PERIODIC = "periodic_sync"
        const val WORK_NAME_ONE_TIME = "one_time_sync"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work")

        // Check network connectivity before attempting sync
        if (!networkMonitor.isCurrentlyOnline()) {
            Log.d(TAG, "No network connectivity, skipping sync")
            return Result.retry()
        }

        return try {
            // 1. Process pending queue items first (push local changes)
            Log.d(TAG, "Processing sync queue")
            val queueResult = syncQueueManager.processQueue()
            Log.d(TAG, "Queue processing: success=${queueResult.successCount}, failed=${queueResult.failCount}")

            // 2. Sync data from Odoo (pull remote changes)
            Log.d(TAG, "Syncing from Odoo")
            val syncResult = dashboardRepository.syncAll()

            when (syncResult) {
                is Resource.Success -> {
                    Log.d(TAG, "Sync completed successfully")
                    Result.success()
                }
                is Resource.Error -> {
                    Log.e(TAG, "Sync completed with errors: ${syncResult.message}")
                    // Return success even with errors to avoid infinite retries
                    // The errors are displayed in the UI
                    Result.success()
                }
                is Resource.Loading -> {
                    // This shouldn't happen as syncAll returns final result
                    Log.w(TAG, "Unexpected loading state")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with exception", e)
            // Retry on failure, WorkManager handles backoff
            Result.retry()
        }
    }
}
