package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.SyncState
import java.util.Date

/**
 * Room entity for Customer table
 *
 * Uses Odoo record ID as primary key to prevent duplicates on sync
 * Index on name for fast lookups
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val id: Int,                  // Odoo record ID (prevents duplicates)
    val name: String,
    val city: String?,
    val taxId: String?,
    val email: String?,
    val phone: String?,
    val website: String?,
    val date: Long?,              // Stored as timestamp
    val syncState: String,        // Stored as String (SYNCED, PENDING, etc.)
    val lastModified: Long,       // Stored as timestamp
    val mobileUid: String? = null, // UUID for locally-created customers
    val latitude: Double? = null,  // GPS latitude coordinate
    val longitude: Double? = null  // GPS longitude coordinate
)

/**
 * Extension function to convert CustomerEntity to domain Customer
 */
fun CustomerEntity.toDomain(): Customer {
    return Customer(
        id = id,
        name = name,
        city = city,
        taxId = taxId,
        email = email,
        phone = phone,
        website = website,
        date = date?.let { Date(it) },
        syncState = SyncState.valueOf(syncState),
        lastModified = Date(lastModified),
        mobileUid = mobileUid,
        latitude = latitude,
        longitude = longitude
    )
}

/**
 * Extension function to convert domain Customer to CustomerEntity
 */
fun Customer.toEntity(): CustomerEntity {
    return CustomerEntity(
        id = id,
        name = name,
        city = city,
        taxId = taxId,
        email = email,
        phone = phone,
        website = website,
        date = date?.time,
        syncState = syncState.name,
        lastModified = lastModified.time,
        mobileUid = mobileUid,
        latitude = latitude,
        longitude = longitude
    )
}
