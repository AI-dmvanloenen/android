package com.odoo.fieldapp.data.remote.mapper

import com.odoo.fieldapp.data.remote.dto.DeliveryLineResponse
import com.odoo.fieldapp.data.remote.dto.DeliveryResponse
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.DeliveryLine
import com.odoo.fieldapp.domain.model.SyncState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Thread-safe date formatters for Odoo API
 * SimpleDateFormat is not thread-safe, so we use ThreadLocal to provide each thread its own instance.
 */
private val dateTimeFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
}
private val dateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

/**
 * Convert DeliveryLineResponse from API to domain DeliveryLine model
 */
fun DeliveryLineResponse.toDomain(): DeliveryLine {
    return DeliveryLine(
        id = id ?: 0,
        productId = productId,
        productName = productName,
        quantity = quantity ?: 0.0,
        quantityDone = quantityDone ?: 0.0,
        uom = uom ?: "Units"
    )
}

/**
 * Convert DeliveryResponse from API to domain Delivery model
 * Uses Odoo ID as the primary identifier to prevent duplicates on sync
 */
fun DeliveryResponse.toDomain(): Delivery {
    return Delivery(
        id = id ?: 0,
        name = name,
        partnerId = partnerId,
        partnerName = null,  // Will be resolved from Customer table
        scheduledDate = parseDateTime(scheduledDate),
        state = state ?: "draft",
        saleId = saleId,
        saleName = null,  // Will be resolved from Sale table
        lines = lines?.mapNotNull {
            if (it.id != null) it.toDomain() else null
        } ?: emptyList(),
        syncState = SyncState.SYNCED,
        lastModified = Date()
    )
}

/**
 * Parse datetime string from Odoo (handles both date and datetime formats)
 */
private fun parseDateTime(dateStr: String?): Date? {
    if (dateStr.isNullOrBlank()) return null

    return try {
        // Try datetime format first (more specific)
        if (dateStr.contains(" ")) {
            dateTimeFormatter.get()!!.parse(dateStr)
        } else {
            dateFormatter.get()!!.parse(dateStr)
        }
    } catch (e: Exception) {
        null
    }
}
