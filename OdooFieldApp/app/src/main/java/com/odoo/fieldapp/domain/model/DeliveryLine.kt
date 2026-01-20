package com.odoo.fieldapp.domain.model

/**
 * Domain model for Delivery Line
 * This represents a single line item in a delivery order
 */
data class DeliveryLine(
    val id: Int,
    val productId: Int?,         // Odoo product ID for linking
    val productName: String,
    val quantity: Double,        // Quantity to deliver (demand)
    val quantityDone: Double,    // Quantity already delivered
    val uom: String
)
