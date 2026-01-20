package com.odoo.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odoo.fieldapp.domain.model.DeliveryLine

/**
 * Room entity for Delivery Line table
 *
 * Uses Odoo record ID as primary key
 * Foreign key to DeliveryEntity with cascade delete
 */
@Entity(
    tableName = "delivery_lines",
    foreignKeys = [
        ForeignKey(
            entity = DeliveryEntity::class,
            parentColumns = ["id"],
            childColumns = ["deliveryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deliveryId"])]
)
data class DeliveryLineEntity(
    @PrimaryKey
    val id: Int,                      // Odoo record ID (prevents duplicates)
    val deliveryId: Int,              // Foreign key to DeliveryEntity
    val productId: Int?,              // Odoo product ID for linking
    val productName: String,          // Product name
    val quantity: Double,             // Quantity to deliver (demand)
    val quantityDone: Double,         // Quantity already delivered
    val uom: String                   // Unit of measure
)

/**
 * Extension function to convert DeliveryLineEntity to domain DeliveryLine
 */
fun DeliveryLineEntity.toDomain(): DeliveryLine {
    return DeliveryLine(
        id = id,
        productId = productId,
        productName = productName,
        quantity = quantity,
        quantityDone = quantityDone,
        uom = uom
    )
}

/**
 * Extension function to convert domain DeliveryLine to DeliveryLineEntity
 */
fun DeliveryLine.toEntity(deliveryId: Int): DeliveryLineEntity {
    return DeliveryLineEntity(
        id = id,
        deliveryId = deliveryId,
        productId = productId,
        productName = productName,
        quantity = quantity,
        quantityDone = quantityDone,
        uom = uom
    )
}
