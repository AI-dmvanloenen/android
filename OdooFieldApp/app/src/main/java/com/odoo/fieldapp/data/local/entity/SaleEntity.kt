package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.model.SyncState
import java.util.Date

/**
 * Room entity for Sale Order table
 *
 * Uses Odoo record ID as primary key to prevent duplicates on sync
 * Index on name for fast lookups
 */
@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["name"]),
        Index(value = ["partnerId"])
    ]
)
data class SaleEntity(
    @PrimaryKey
    val id: Int,                  // Odoo record ID (prevents duplicates)
    val mobileUid: String?,       // UUID for mobile app synchronization
    val name: String,             // Sale order reference (e.g., "SO001")
    val dateOrder: Long?,         // Stored as timestamp
    val amountTotal: Double?,     // Total amount
    val state: String,            // Order state (draft, sent, sale, done, cancel)
    val partnerId: Int?,          // Customer ID (foreign key)
    val partnerName: String?,     // Customer name (denormalized for display)
    val syncState: String,        // Stored as String (SYNCED, PENDING, etc.)
    val lastModified: Long        // Stored as timestamp
)

/**
 * Extension function to convert SaleEntity to domain Sale
 * Note: Lines are loaded separately and default to empty list here
 */
fun SaleEntity.toDomain(): Sale {
    return Sale(
        id = id,
        mobileUid = mobileUid,
        name = name,
        dateOrder = dateOrder?.let { Date(it) },
        amountTotal = amountTotal,
        state = state,
        partnerId = partnerId,
        partnerName = partnerName,
        lines = emptyList(),  // Lines loaded separately for performance
        syncState = SyncState.valueOf(syncState),
        lastModified = Date(lastModified)
    )
}

/**
 * Extension function to convert domain Sale to SaleEntity
 */
fun Sale.toEntity(): SaleEntity {
    return SaleEntity(
        id = id,
        mobileUid = mobileUid,
        name = name,
        dateOrder = dateOrder?.time,
        amountTotal = amountTotal,
        state = state,
        partnerId = partnerId,
        partnerName = partnerName,
        syncState = syncState.name,
        lastModified = lastModified.time
    )
}
