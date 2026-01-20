package com.odoo.fieldapp.domain.model

/**
 * Domain model for Product
 * This represents a saleable product in the business logic layer
 */
data class Product(
    val id: Int,              // Odoo record ID (used as primary key)
    val name: String,
    val defaultCode: String?, // SKU
    val barcode: String?,     // For future scanner feature
    val listPrice: Double,
    val uomId: Int?,
    val uomName: String?,
    val categId: Int?,
    val categName: String?,
    val type: String,         // consu, service, product
    val active: Boolean
)
