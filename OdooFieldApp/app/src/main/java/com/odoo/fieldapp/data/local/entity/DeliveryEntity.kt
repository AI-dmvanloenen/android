package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.DeliveryLine
import com.odoo.fieldapp.domain.model.SyncState
import java.util.Date

/**
 * Room entity for Delivery table
 *
 * Uses Odoo record ID as primary key to prevent duplicates on sync
 * Indices on name, partnerId, and saleId for fast lookups
 */
@Entity(
    tableName = "deliveries",
    indices = [
        Index(value = ["name"]),
        Index(value = ["partnerId"]),
        Index(value = ["saleId"])
    ]
)
data class DeliveryEntity(
    @PrimaryKey
    val id: Int,                      // Odoo record ID (prevents duplicates)
    val name: String,                 // Delivery reference (e.g., "WH/OUT/00001")
    val partnerId: Int?,              // Customer ID (foreign key)
    val partnerName: String?,         // Customer name (denormalized for display)
    val scheduledDate: Long?,         // Stored as timestamp
    val state: String,                // Delivery state
    val saleId: Int?,                 // Sale order ID (foreign key)
    val saleName: String?,            // Sale order name (denormalized for display)
    val syncState: String,            // Stored as String (SYNCED, PENDING, etc.)
    val lastModified: Long            // Stored as timestamp
)

/**
 * Extension function to convert DeliveryEntity to domain Delivery
 * Note: Lines must be provided separately as they're stored in a different table
 */
fun DeliveryEntity.toDomain(lines: List<DeliveryLine> = emptyList()): Delivery {
    return Delivery(
        id = id,
        name = name,
        partnerId = partnerId,
        partnerName = partnerName,
        scheduledDate = scheduledDate?.let { Date(it) },
        state = state,
        saleId = saleId,
        saleName = saleName,
        lines = lines,
        syncState = SyncState.valueOf(syncState),
        lastModified = Date(lastModified)
    )
}

/**
 * Extension function to convert domain Delivery to DeliveryEntity
 * Note: Lines must be saved separately
 */
fun Delivery.toEntity(): DeliveryEntity {
    return DeliveryEntity(
        id = id,
        name = name,
        partnerId = partnerId,
        partnerName = partnerName,
        scheduledDate = scheduledDate?.time,
        state = state,
        saleId = saleId,
        saleName = saleName,
        syncState = syncState.name,
        lastModified = lastModified.time
    )
}
