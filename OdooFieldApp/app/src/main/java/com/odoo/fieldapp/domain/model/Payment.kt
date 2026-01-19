package com.odoo.fieldapp.domain.model

import java.util.Date

/**
 * Domain model for Payment
 *
 * Represents a customer payment (inbound payment) in the system.
 * This is the core business model used throughout the app.
 */
data class Payment(
    val id: Int,
    val mobileUid: String?,       // UUID for deduplication
    val name: String,             // Payment reference (e.g., "PAY/2024/0001")
    val partnerId: Int?,          // Customer ID
    val partnerName: String?,     // Denormalized customer name for display
    val amount: Double,
    val date: Date?,
    val memo: String?,            // Payment memo/reference (maps to 'ref' in Odoo)
    val journalId: Int?,          // Bank journal ID
    val state: String,            // draft, posted, cancelled
    val syncState: SyncState,
    val lastModified: Date
)
