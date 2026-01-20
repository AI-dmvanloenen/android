package com.odoo.fieldapp.domain.model

import java.util.Date

/**
 * Domain model for Sale Order
 * This represents a sale order in the business logic layer
 */
data class Sale(
    val id: Int,                 // Odoo record ID (used as primary key)
    val mobileUid: String?,      // UUID for mobile app synchronization
    val name: String,            // Sale order reference (e.g., "SO001")
    val dateOrder: Date?,        // Order date
    val amountTotal: Double?,    // Total amount
    val state: String,           // Order state (draft, sent, sale, done, cancel)
    val partnerId: Int?,         // Customer ID (foreign key to Customer)
    val partnerName: String?,    // Customer name (for display purposes)
    val lines: List<SaleLine>,   // Order line items
    val syncState: SyncState,
    val lastModified: Date
)
