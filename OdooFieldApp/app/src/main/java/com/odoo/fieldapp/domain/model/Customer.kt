package com.odoo.fieldapp.domain.model

import java.util.Date

/**
 * Domain model for Customer
 * This represents a customer in the business logic layer
 */
data class Customer(
    val id: Int,                 // Odoo record ID (used as primary key)
    val name: String,
    val city: String?,
    val taxId: String?,
    val email: String?,
    val phone: String?,
    val website: String?,
    val date: Date?,
    val syncState: SyncState,
    val lastModified: Date,
    val mobileUid: String? = null  // UUID for locally-created customers
)

/**
 * Sync state for tracking record synchronization status
 */
enum class SyncState {
    SYNCED,      // Successfully synced with Odoo
    PENDING,     // Waiting to be synced
    SYNCING,     // Currently being synced
    ERROR        // Sync failed
}
