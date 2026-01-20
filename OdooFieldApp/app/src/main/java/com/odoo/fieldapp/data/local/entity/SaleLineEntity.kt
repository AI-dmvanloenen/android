package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odoo.fieldapp.domain.model.SaleLine

/**
 * Room entity for Sale Line table
 *
 * Uses Odoo record ID as primary key
 * Foreign key to SaleEntity with cascade delete
 */
@Entity(
    tableName = "sale_lines",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ]
)
data class SaleLineEntity(
    @PrimaryKey
    val id: Int,                   // Odoo record ID (prevents duplicates)
    val saleId: Int,               // Foreign key to SaleEntity
    val productId: Int?,           // Odoo product ID for linking
    val productName: String,       // Product name
    val productUomQty: Double,     // Ordered quantity
    val qtyDelivered: Double,      // Delivered quantity
    val qtyInvoiced: Double,       // Invoiced quantity
    val priceUnit: Double,
    val discount: Double,
    val priceSubtotal: Double,
    val uom: String                // Unit of measure
)

/**
 * Extension function to convert SaleLineEntity to domain SaleLine
 */
fun SaleLineEntity.toDomain(): SaleLine {
    return SaleLine(
        id = id,
        productId = productId,
        productName = productName,
        productUomQty = productUomQty,
        qtyDelivered = qtyDelivered,
        qtyInvoiced = qtyInvoiced,
        priceUnit = priceUnit,
        discount = discount,
        priceSubtotal = priceSubtotal,
        uom = uom
    )
}

/**
 * Extension function to convert domain SaleLine to SaleLineEntity
 */
fun SaleLine.toEntity(saleId: Int): SaleLineEntity {
    return SaleLineEntity(
        id = id,
        saleId = saleId,
        productId = productId,
        productName = productName,
        productUomQty = productUomQty,
        qtyDelivered = qtyDelivered,
        qtyInvoiced = qtyInvoiced,
        priceUnit = priceUnit,
        discount = discount,
        priceSubtotal = priceSubtotal,
        uom = uom
    )
}
