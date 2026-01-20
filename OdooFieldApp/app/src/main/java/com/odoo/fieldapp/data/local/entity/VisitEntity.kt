package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.model.Visit
import java.util.Date

/**
 * Room entity for Visit
 *
 * This is the database representation of a customer visit.
 * Stored in the local SQLite database for offline access.
 */
@Entity(
    tableName = "visits",
    indices = [
        Index(value = ["partnerId"]),
        Index(value = ["visitDatetime"]),
        Index(value = ["mobileUid"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["partnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VisitEntity(
    @PrimaryKey val id: Int,
    val mobileUid: String?,
    val partnerId: Int,
    val partnerName: String?,
    val visitDatetime: Long,     // Stored as epoch millis
    val memo: String?,
    val syncState: String,       // Stored as string, maps to SyncState enum
    val lastModified: Long       // Stored as epoch millis
)

/**
 * Extension function to convert VisitEntity to domain model
 */
fun VisitEntity.toDomain(): Visit {
    return Visit(
        id = id,
        mobileUid = mobileUid,
        partnerId = partnerId,
        partnerName = partnerName,
        visitDatetime = Date(visitDatetime),
        memo = memo,
        syncState = SyncState.valueOf(syncState),
        lastModified = Date(lastModified)
    )
}

/**
 * Extension function to convert Visit domain model to entity
 */
fun Visit.toEntity(): VisitEntity {
    return VisitEntity(
        id = id,
        mobileUid = mobileUid,
        partnerId = partnerId,
        partnerName = partnerName,
        visitDatetime = visitDatetime.time,
        memo = memo,
        syncState = syncState.name,
        lastModified = lastModified.time
    )
}
