package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a queued sync operation
 *
 * Used to store create/update operations that failed due to network issues
 * and need to be retried when connectivity is restored.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["entityType"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Type of entity being synced (e.g., "customer", "sale", "payment")
     */
    val entityType: String,

    /**
     * The operation type: "create", "update", "delete"
     */
    val operation: String,

    /**
     * JSON payload containing the data to sync
     */
    val payload: String,

    /**
     * Mobile UID used to match the record after sync
     */
    val mobileUid: String,

    /**
     * Current status: "pending", "processing", "failed"
     */
    val status: String = SyncQueueStatus.PENDING,

    /**
     * Number of sync attempts
     */
    val retryCount: Int = 0,

    /**
     * Maximum retry attempts before giving up
     */
    val maxRetries: Int = 5,

    /**
     * Last error message from sync attempt
     */
    val lastError: String? = null,

    /**
     * Timestamp when the queue item was created
     */
    val createdAt: Long = Date().time,

    /**
     * Timestamp of last sync attempt
     */
    val lastAttemptAt: Long? = null,

    /**
     * Next scheduled attempt time (for exponential backoff)
     */
    val nextAttemptAt: Long? = null
)

/**
 * Status constants for sync queue items
 */
object SyncQueueStatus {
    const val PENDING = "pending"
    const val PROCESSING = "processing"
    const val FAILED = "failed"
    const val COMPLETED = "completed"
}

/**
 * Operation type constants
 */
object SyncOperation {
    const val CREATE = "create"
    const val UPDATE = "update"
    const val DELETE = "delete"
}

/**
 * Entity type constants
 */
object SyncEntityType {
    const val CUSTOMER = "customer"
    const val SALE = "sale"
    const val PAYMENT = "payment"
}
