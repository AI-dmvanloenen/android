package com.odoo.fieldapp.domain.model

/**
 * Domain model for Sale Order Line
 * This represents a single line item in a sale order
 */
data class SaleLine(
    val id: Int,
    val productId: Int?,         // Odoo product ID for linking
    val productName: String,
    val productUomQty: Double,   // Ordered quantity
    val qtyDelivered: Double,    // Delivered quantity
    val qtyInvoiced: Double,     // Invoiced quantity
    val priceUnit: Double,
    val discount: Double,
    val priceSubtotal: Double,
    val uom: String
)
