package com.odoo.fieldapp.domain.model

import java.util.Date

/**
 * Domain model for Sale Order
 * This represents a sale order in the business logic layer
 *
 * Note: Sale order lines (products, quantities) will be added in a future phase
 */
data class Sale(
    val id: Int,                 // Odoo record ID (used as primary key)
    val name: String,            // Sale order reference (e.g., "SO001")
    val dateOrder: Date?,        // Order date
    val amountTotal: Double?,    // Total amount
    val partnerId: Int?,         // Customer ID (foreign key to Customer)
    val partnerName: String?,    // Customer name (for display purposes)
    val syncState: SyncState,
    val lastModified: Date
)
