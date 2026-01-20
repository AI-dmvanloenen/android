package com.odoo.fieldapp.data.remote.mapper

import android.util.Log
import com.odoo.fieldapp.data.remote.dto.SaleLineRequest
import com.odoo.fieldapp.data.remote.dto.SaleLineResponse
import com.odoo.fieldapp.data.remote.dto.SaleRequest
import com.odoo.fieldapp.data.remote.dto.SaleResponse
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.model.SaleLine
import com.odoo.fieldapp.domain.model.SyncState
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "SaleMapper"

/**
 * Thread-safe date formatters for Odoo API (ISO format: 2025-04-11 or 2025-04-11 10:30:00)
 * SimpleDateFormat is not thread-safe, so we use ThreadLocal to provide each thread its own instance.
 */
private val dateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
}
private val dateTimeFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
}

/**
 * Convert SaleLineResponse from API to domain SaleLine model
 */
fun SaleLineResponse.toDomain(): SaleLine {
    return SaleLine(
        id = id ?: 0,
        productId = productId,
        productName = productName ?: "Unknown Product",
        productUomQty = productUomQty ?: 0.0,
        qtyDelivered = qtyDelivered ?: 0.0,
        qtyInvoiced = qtyInvoiced ?: 0.0,
        priceUnit = priceUnit ?: 0.0,
        discount = discount ?: 0.0,
        priceSubtotal = priceSubtotal ?: 0.0,
        uom = uom ?: "Units"
    )
}

/**
 * Convert SaleResponse from API to domain Sale model
 * Uses Odoo ID as the primary identifier to prevent duplicates on sync
 */
fun SaleResponse.toDomain(): Sale {
    return Sale(
        id = id ?: 0,
        mobileUid = mobileUid,
        name = name,
        dateOrder = parseDate(dateOrder),
        amountTotal = amountTotal,
        state = state ?: "draft",
        partnerId = partnerId,
        partnerName = null,  // Customer name not provided by endpoint
        lines = lines?.mapNotNull {
            if (it.id != null) it.toDomain() else null
        } ?: emptyList(),
        syncState = SyncState.SYNCED,
        lastModified = parseDate(writeDate) ?: Date()
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
            dateTimeFormatter.get()!!.parse(dateStr)
        } else {
            dateFormatter.get()!!.parse(dateStr)
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Convert domain SaleLine model to SaleLineRequest for API
 */
fun SaleLine.toRequest(): SaleLineRequest {
    return SaleLineRequest(
        productId = productId ?: 0,
        productUomQty = productUomQty,
        priceUnit = priceUnit
    )
}

/**
 * Convert domain Sale model to SaleRequest for API
 */
fun Sale.toRequest(): SaleRequest {
    val validLines = lines.filter { line ->
        val isValid = line.productId != null && line.productId > 0
        if (!isValid && lines.isNotEmpty()) {
            Log.w(TAG, "Filtering out line with invalid productId: ${line.productName} (productId=${line.productId})")
        }
        isValid
    }.map { it.toRequest() }

    return SaleRequest(
        mobileUid = mobileUid,
        name = name,
        dateOrder = dateOrder?.let { dateFormatter.get()!!.format(it) },
        amountTotal = amountTotal,
        partnerId = partnerId,
        lines = validLines.takeIf { it.isNotEmpty() }
    )
}
