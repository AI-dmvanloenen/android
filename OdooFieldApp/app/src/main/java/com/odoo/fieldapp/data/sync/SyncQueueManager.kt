package com.odoo.fieldapp.data.sync

import android.util.Log
import com.google.gson.Gson
import com.odoo.fieldapp.data.local.dao.SyncQueueDao
import com.odoo.fieldapp.data.local.entity.SyncEntityType
import com.odoo.fieldapp.data.local.entity.SyncOperation
import com.odoo.fieldapp.data.local.entity.SyncQueueEntity
import com.odoo.fieldapp.data.local.entity.SyncQueueStatus
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.dto.CustomerRequest
import com.odoo.fieldapp.data.remote.dto.PaymentRequest
import com.odoo.fieldapp.data.remote.dto.SaleRequest
import com.odoo.fieldapp.data.repository.ApiKeyProvider
import com.odoo.fieldapp.domain.connectivity.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Manages the sync queue for failed create operations
 *
 * Processes queued items with exponential backoff retry logic.
 * Items are automatically retried when network becomes available.
 */
@Singleton
class SyncQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "SyncQueueManager"
        private const val BASE_BACKOFF_SECONDS = 30L
        private const val MAX_BACKOFF_SECONDS = 3600L // 1 hour max
    }

    /**
     * Get count of pending items in the queue
     */
    fun getPendingCount(): Flow<Int> = syncQueueDao.getPendingCount()

    /**
     * Get count of permanently failed items
     */
    fun getFailedCount(): Flow<Int> = syncQueueDao.getFailedCount()

    /**
     * Get all queue items for UI display
     */
    fun getAllItems(): Flow<List<SyncQueueEntity>> = syncQueueDao.getAllItems()

    /**
     * Queue a customer create operation for later sync
     */
    suspend fun queueCustomerCreate(mobileUid: String, request: CustomerRequest) {
        val payload = gson.toJson(request)
        val entity = SyncQueueEntity(
            entityType = SyncEntityType.CUSTOMER,
            operation = SyncOperation.CREATE,
            payload = payload,
            mobileUid = mobileUid
        )
        syncQueueDao.insert(entity)
        Log.d(TAG, "Queued customer create: mobileUid=$mobileUid")
    }

    /**
     * Queue a sale create operation for later sync
     */
    suspend fun queueSaleCreate(mobileUid: String, request: SaleRequest) {
        val payload = gson.toJson(request)
        val entity = SyncQueueEntity(
            entityType = SyncEntityType.SALE,
            operation = SyncOperation.CREATE,
            payload = payload,
            mobileUid = mobileUid
        )
        syncQueueDao.insert(entity)
        Log.d(TAG, "Queued sale create: mobileUid=$mobileUid")
    }

    /**
     * Queue a payment create operation for later sync
     */
    suspend fun queuePaymentCreate(mobileUid: String, request: PaymentRequest) {
        val payload = gson.toJson(request)
        val entity = SyncQueueEntity(
            entityType = SyncEntityType.PAYMENT,
            operation = SyncOperation.CREATE,
            payload = payload,
            mobileUid = mobileUid
        )
        syncQueueDao.insert(entity)
        Log.d(TAG, "Queued payment create: mobileUid=$mobileUid")
    }

    /**
     * Remove an item from the queue (after successful manual sync)
     */
    suspend fun removeFromQueue(mobileUid: String) {
        syncQueueDao.deleteByMobileUid(mobileUid)
        Log.d(TAG, "Removed from queue: mobileUid=$mobileUid")
    }

    /**
     * Process all pending items in the queue
     * Called by SyncWorker when network is available
     */
    suspend fun processQueue(): SyncQueueResult {
        if (!networkMonitor.isCurrentlyOnline()) {
            Log.d(TAG, "No network, skipping queue processing")
            return SyncQueueResult(0, 0, 0)
        }

        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "No API key configured, skipping queue processing")
            return SyncQueueResult(0, 0, 0)
        }

        val pendingItems = syncQueueDao.getPendingItems()
        Log.d(TAG, "Processing ${pendingItems.size} queued items")

        var successCount = 0
        var failCount = 0
        var skippedCount = 0

        for (item in pendingItems) {
            try {
                syncQueueDao.markAsProcessing(item.id)

                val success = processItem(item, apiKey)

                if (success) {
                    syncQueueDao.deleteById(item.id)
                    successCount++
                    Log.d(TAG, "Successfully synced queued item: ${item.entityType}/${item.mobileUid}")
                } else {
                    // Mark as failed with exponential backoff
                    val nextAttempt = calculateNextAttempt(item.retryCount + 1)
                    syncQueueDao.markAsFailed(
                        id = item.id,
                        error = "Sync returned unsuccessful response",
                        nextAttempt = nextAttempt
                    )
                    failCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process queued item: ${item.entityType}/${item.mobileUid}", e)
                val nextAttempt = calculateNextAttempt(item.retryCount + 1)
                syncQueueDao.markAsFailed(
                    id = item.id,
                    error = e.message ?: "Unknown error",
                    nextAttempt = nextAttempt
                )
                failCount++
            }
        }

        Log.d(TAG, "Queue processing complete: success=$successCount, failed=$failCount, skipped=$skippedCount")
        return SyncQueueResult(successCount, failCount, skippedCount)
    }

    /**
     * Process a single queue item
     * Returns true if sync was successful
     */
    private suspend fun processItem(item: SyncQueueEntity, apiKey: String): Boolean {
        return when (item.entityType) {
            SyncEntityType.CUSTOMER -> processCustomerCreate(item, apiKey)
            SyncEntityType.SALE -> processSaleCreate(item, apiKey)
            SyncEntityType.PAYMENT -> processPaymentCreate(item, apiKey)
            else -> {
                Log.w(TAG, "Unknown entity type: ${item.entityType}")
                false
            }
        }
    }

    private suspend fun processCustomerCreate(item: SyncQueueEntity, apiKey: String): Boolean {
        val request = gson.fromJson(item.payload, CustomerRequest::class.java)
        val response = apiService.createCustomers(apiKey, listOf(request))

        if (response.isSuccessful) {
            val createdCustomer = response.body()?.data?.firstOrNull { it.mobileUid == item.mobileUid }
            return createdCustomer != null && createdCustomer.odooId != null
        }
        return false
    }

    private suspend fun processSaleCreate(item: SyncQueueEntity, apiKey: String): Boolean {
        val request = gson.fromJson(item.payload, SaleRequest::class.java)
        val response = apiService.createSales(apiKey, listOf(request))

        if (response.isSuccessful) {
            val createdSale = response.body()?.data?.firstOrNull { it.mobileUid == item.mobileUid }
            return createdSale != null && createdSale.id != null
        }
        return false
    }

    private suspend fun processPaymentCreate(item: SyncQueueEntity, apiKey: String): Boolean {
        val request = gson.fromJson(item.payload, PaymentRequest::class.java)
        val response = apiService.createPayments(apiKey, listOf(request))

        if (response.isSuccessful) {
            val createdPayment = response.body()?.data?.firstOrNull { it.mobileUid == item.mobileUid }
            return createdPayment != null && createdPayment.id != null
        }
        return false
    }

    /**
     * Calculate next attempt time using exponential backoff
     */
    private fun calculateNextAttempt(retryCount: Int): Long {
        val backoffSeconds = (BASE_BACKOFF_SECONDS * 2.0.pow(retryCount.toDouble())).toLong()
        val cappedBackoff = backoffSeconds.coerceAtMost(MAX_BACKOFF_SECONDS)
        return System.currentTimeMillis() + (cappedBackoff * 1000)
    }
}

/**
 * Result of queue processing
 */
data class SyncQueueResult(
    val successCount: Int,
    val failCount: Int,
    val skippedCount: Int
)
