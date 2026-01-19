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
    val name: String,             // Sale order reference (e.g., "SO001")
    val dateOrder: Long?,         // Stored as timestamp
    val amountTotal: Double?,     // Total amount
    val partnerId: Int?,          // Customer ID (foreign key)
    val partnerName: String?,     // Customer name (denormalized for display)
    val syncState: String,        // Stored as String (SYNCED, PENDING, etc.)
    val lastModified: Long        // Stored as timestamp
)

/**
 * Extension function to convert SaleEntity to domain Sale
 */
fun SaleEntity.toDomain(): Sale {
    return Sale(
        id = id,
        name = name,
        dateOrder = dateOrder?.let { Date(it) },
        amountTotal = amountTotal,
        partnerId = partnerId,
        partnerName = partnerName,
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
        name = name,
        dateOrder = dateOrder?.time,
        amountTotal = amountTotal,
        partnerId = partnerId,
        partnerName = partnerName,
        syncState = syncState.name,
        lastModified = lastModified.time
    )
}
