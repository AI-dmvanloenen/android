package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odoo.fieldapp.domain.model.Product

/**
 * Room entity for Product table
 *
 * Uses Odoo record ID as primary key to prevent duplicates on sync
 * Indices on name, defaultCode, and barcode for fast lookups
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["defaultCode"]),
        Index(value = ["barcode"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val id: Int,               // Odoo record ID (prevents duplicates)
    val name: String,
    val defaultCode: String?,  // SKU
    val barcode: String?,      // For future scanner feature
    val listPrice: Double,
    val uomId: Int?,
    val uomName: String?,
    val categId: Int?,
    val categName: String?,
    val type: String,          // consu, service, product
    val active: Boolean,
    val lastModified: Long     // Stored as timestamp
)

/**
 * Extension function to convert ProductEntity to domain Product
 */
fun ProductEntity.toDomain(): Product {
    return Product(
        id = id,
        name = name,
        defaultCode = defaultCode,
        barcode = barcode,
        listPrice = listPrice,
        uomId = uomId,
        uomName = uomName,
        categId = categId,
        categName = categName,
        type = type,
        active = active
    )
}

/**
 * Extension function to convert domain Product to ProductEntity
 */
fun Product.toEntity(lastModified: Long = System.currentTimeMillis()): ProductEntity {
    return ProductEntity(
        id = id,
        name = name,
        defaultCode = defaultCode,
        barcode = barcode,
        listPrice = listPrice,
        uomId = uomId,
        uomName = uomName,
        categId = categId,
        categName = categName,
        type = type,
        active = active,
        lastModified = lastModified
    )
}
