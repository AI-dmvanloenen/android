package com.odoo.fieldapp.domain.model

import java.util.Date

/**
 * Domain model for Delivery Order
 * This represents a delivery order in the business logic layer
 */
data class Delivery(
    val id: Int,                     // Odoo record ID (used as primary key)
    val name: String,                // Delivery reference (e.g., "WH/OUT/00001")
    val partnerId: Int?,             // Customer ID (foreign key to Customer)
    val partnerName: String?,        // Customer name (denormalized for display)
    val scheduledDate: Date?,        // Scheduled delivery date
    val state: String,               // Delivery state (e.g., "assigned", "done")
    val saleId: Int?,                // Sale order ID (foreign key to Sale)
    val saleName: String?,           // Sale order name (denormalized for display)
    val lines: List<DeliveryLine>,   // Delivery line items
    val syncState: SyncState,
    val lastModified: Date
)
