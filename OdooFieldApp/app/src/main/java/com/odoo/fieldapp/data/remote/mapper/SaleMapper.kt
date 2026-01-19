package com.odoo.fieldapp.data.remote.mapper

import com.odoo.fieldapp.data.remote.dto.SaleRequest
import com.odoo.fieldapp.data.remote.dto.SaleResponse
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.model.SyncState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date formatter for Odoo API (ISO format: 2025-04-11 or 2025-04-11 10:30:00)
 */
private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

/**
 * Convert SaleResponse from API to domain Sale model
 * Uses Odoo ID as the primary identifier to prevent duplicates on sync
 */
fun SaleResponse.toDomain(): Sale {
    return Sale(
        id = id ?: 0,
        name = name,
        dateOrder = parseDate(dateOrder),
        amountTotal = amountTotal,
        partnerId = partnerId,
        partnerName = null,  // Customer name not provided by endpoint
        syncState = SyncState.SYNCED,
        lastModified = Date()
    )
}

/**
 * Parse date string from Odoo (handles both date and datetime formats)
 */
private fun parseDate(dateStr: String?): Date? {
    if (dateStr.isNullOrBlank()) return null

    return try {
        // Try datetime format first (more specific)
        if (dateStr.contains(" ")) {
            dateTimeFormatter.parse(dateStr)
        } else {
            dateFormatter.parse(dateStr)
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Convert domain Sale model to SaleRequest for API
 */
fun Sale.toRequest(): SaleRequest {
    return SaleRequest(
        name = name,
        dateOrder = dateOrder?.let { dateFormatter.format(it) },
        amountTotal = amountTotal,
        partnerId = partnerId
    )
}
