package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odoo.fieldapp.domain.model.Payment
import com.odoo.fieldapp.domain.model.SyncState
import java.util.Date

/**
 * Room entity for Payment
 *
 * This is the database representation of a payment.
 * Stored in the local SQLite database for offline access.
 */
@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["name"]),
        Index(value = ["partnerId"]),
        Index(value = ["mobileUid"], unique = true),
        Index(value = ["state"])
    ]
)
data class PaymentEntity(
    @PrimaryKey val id: Int,
    val mobileUid: String?,
    val name: String,
    val partnerId: Int?,
    val partnerName: String?,
    val amount: Double,
    val date: Long?,           // Stored as epoch millis
    val memo: String?,
    val journalId: Int?,
    val state: String,
    val syncState: String,     // Stored as string, maps to SyncState enum
    val lastModified: Long     // Stored as epoch millis
)

/**
 * Extension function to convert PaymentEntity to domain model
 */
fun PaymentEntity.toDomain(): Payment {
    return Payment(
        id = id,
        mobileUid = mobileUid,
        name = name,
        partnerId = partnerId,
        partnerName = partnerName,
        amount = amount,
        date = date?.let { Date(it) },
        memo = memo,
        journalId = journalId,
        state = state,
        syncState = SyncState.valueOf(syncState),
        lastModified = Date(lastModified)
    )
}

/**
 * Extension function to convert Payment domain model to entity
 */
fun Payment.toEntity(): PaymentEntity {
    return PaymentEntity(
        id = id,
        mobileUid = mobileUid,
        name = name,
        partnerId = partnerId,
        partnerName = partnerName,
        amount = amount,
        date = date?.time,
        memo = memo,
        journalId = journalId,
        state = state,
        syncState = syncState.name,
        lastModified = lastModified.time
    )
}
