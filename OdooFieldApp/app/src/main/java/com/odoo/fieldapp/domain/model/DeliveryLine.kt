package com.odoo.fieldapp.domain.model

/**
 * Domain model for Delivery Line
 * This represents a single line item in a delivery order
 */
data class DeliveryLine(
    val id: Int,
    val productName: String,
    val quantity: Double,
    val uom: String
)
