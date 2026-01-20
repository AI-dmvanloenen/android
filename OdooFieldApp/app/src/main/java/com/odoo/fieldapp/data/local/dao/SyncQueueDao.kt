package com.odoo.fieldapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.odoo.fieldapp.data.local.entity.SyncQueueEntity
import com.odoo.fieldapp.data.local.entity.SyncQueueStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for sync queue operations
 *
 * Provides methods to manage the queue of pending sync operations
 */
@Dao
interface SyncQueueDao {

    /**
     * Insert a new queue item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    /**
     * Update an existing queue item
     */
    @Update
    suspend fun update(item: SyncQueueEntity)

    /**
     * Delete a queue item
     */
    @Delete
    suspend fun delete(item: SyncQueueEntity)

    /**
     * Delete a queue item by ID
     */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Delete queue items by mobileUid (used after successful sync)
     */
    @Query("DELETE FROM sync_queue WHERE mobileUid = :mobileUid")
    suspend fun deleteByMobileUid(mobileUid: String)

    /**
     * Get all pending items that are ready for sync
     * (status is pending or failed with retry count below max, and next attempt time has passed)
     */
    @Query("""
        SELECT * FROM sync_queue
        WHERE (status = :pendingStatus OR (status = :failedStatus AND retryCount < maxRetries))
        AND (nextAttemptAt IS NULL OR nextAttemptAt <= :currentTime)
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingItems(
        pendingStatus: String = SyncQueueStatus.PENDING,
        failedStatus: String = SyncQueueStatus.FAILED,
        currentTime: Long = System.currentTimeMillis()
    ): List<SyncQueueEntity>

    /**
     * Get all queue items as a Flow for UI observation
     */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<SyncQueueEntity>>

    /**
     * Get count of pending items
     */
    @Query("""
        SELECT COUNT(*) FROM sync_queue
        WHERE status = :pendingStatus OR (status = :failedStatus AND retryCount < maxRetries)
    """)
    fun getPendingCount(
        pendingStatus: String = SyncQueueStatus.PENDING,
        failedStatus: String = SyncQueueStatus.FAILED
    ): Flow<Int>

    /**
     * Get count of permanently failed items (exceeded max retries)
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :failedStatus AND retryCount >= maxRetries")
    fun getFailedCount(failedStatus: String = SyncQueueStatus.FAILED): Flow<Int>

    /**
     * Get item by mobileUid
     */
    @Query("SELECT * FROM sync_queue WHERE mobileUid = :mobileUid LIMIT 1")
    suspend fun getByMobileUid(mobileUid: String): SyncQueueEntity?

    /**
     * Get items by entity type
     */
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType ORDER BY createdAt ASC")
    suspend fun getByEntityType(entityType: String): List<SyncQueueEntity>

    /**
     * Mark an item as processing
     */
    @Query("UPDATE sync_queue SET status = :status, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun markAsProcessing(
        id: Long,
        status: String = SyncQueueStatus.PROCESSING,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Mark an item as failed with error message
     */
    @Query("""
        UPDATE sync_queue
        SET status = :status,
            lastError = :error,
            retryCount = retryCount + 1,
            lastAttemptAt = :timestamp,
            nextAttemptAt = :nextAttempt
        WHERE id = :id
    """)
    suspend fun markAsFailed(
        id: Long,
        error: String,
        status: String = SyncQueueStatus.FAILED,
        timestamp: Long = System.currentTimeMillis(),
        nextAttempt: Long
    )

    /**
     * Clear all completed items older than specified time
     */
    @Query("DELETE FROM sync_queue WHERE status = :completedStatus AND createdAt < :olderThan")
    suspend fun clearOldCompleted(
        completedStatus: String = SyncQueueStatus.COMPLETED,
        olderThan: Long
    )

    /**
     * Clear all items (for testing or full reset)
     */
    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
