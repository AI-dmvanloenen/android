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
 * Indices on cradleUid and name for fast lookups
 * Unique constraint on cradleUid to prevent duplicates
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["cradleUid"], unique = true),
        Index(value = ["name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val id: String,
    val cradleUid: String,
    val name: String,
    val city: String?,
    val taxId: String?,
    val email: String?,
    val phone: String?,
    val website: String?,
    val date: Long?,              // Stored as timestamp
    val syncState: String,        // Stored as String (SYNCED, PENDING, etc.)
    val lastModified: Long,       // Stored as timestamp
    val odooId: Int?
)

/**
 * Extension function to convert CustomerEntity to domain Customer
 */
fun CustomerEntity.toDomain(): Customer {
    return Customer(
        id = id,
        cradleUid = cradleUid,
        name = name,
        city = city,
        taxId = taxId,
        email = email,
        phone = phone,
        website = website,
        date = date?.let { Date(it) },
        syncState = SyncState.valueOf(syncState),
        lastModified = Date(lastModified),
        odooId = odooId
    )
}

/**
 * Extension function to convert domain Customer to CustomerEntity
 */
fun Customer.toEntity(): CustomerEntity {
    return CustomerEntity(
        id = id,
        cradleUid = cradleUid,
        name = name,
        city = city,
        taxId = taxId,
        email = email,
        phone = phone,
        website = website,
        date = date?.time,
        syncState = syncState.name,
        lastModified = lastModified.time,
        odooId = odooId
    )
}
