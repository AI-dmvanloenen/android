package com.odoo.fieldapp.domain.model

import java.util.Date

/**
 * Domain model for Visit
 *
 * Represents a customer visit logged by a field worker.
 * This is the core business model used throughout the app.
 */
data class Visit(
    val id: Int,
    val mobileUid: String?,       // UUID for deduplication
    val partnerId: Int,           // Customer ID
    val partnerName: String?,     // Denormalized customer name for display
    val visitDatetime: Date,      // Visit date and time
    val memo: String?,            // Visit notes and discussion summary
    val syncState: SyncState,
    val lastModified: Date
)
