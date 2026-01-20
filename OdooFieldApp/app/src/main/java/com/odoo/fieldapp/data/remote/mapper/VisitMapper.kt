package com.odoo.fieldapp.data.remote.mapper

import com.odoo.fieldapp.data.remote.dto.VisitRequest
import com.odoo.fieldapp.data.remote.dto.VisitResponse
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.model.Visit
import java.text.SimpleDateFormat
import java.util.*

/**
 * Thread-safe date formatters for ISO 8601 format
 * SimpleDateFormat is not thread-safe, so we use ThreadLocal to provide each thread its own instance.
 */
private val iso8601DateTimeFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

private val odooDateTimeFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
}

/**
 * Extension function to convert VisitResponse (API) to Visit domain model
 * @throws IllegalStateException if visitDatetime cannot be parsed (required field)
 */
fun VisitResponse.toDomain(): Visit {
    val parsedVisitDatetime = parseDateTime(visitDatetime)
        ?: throw IllegalStateException("visitDatetime is required and must be a valid datetime string, got: $visitDatetime")

    return Visit(
        id = id ?: 0,
        mobileUid = mobileUid,
        partnerId = partnerId ?: 0,
        partnerName = partnerName,
        visitDatetime = parsedVisitDatetime,
        memo = memo,
        syncState = SyncState.SYNCED,
        lastModified = parseDateTime(writeDate) ?: Date()
    )
}

/**
 * Parse datetime string from Odoo
 * Handles both ISO 8601 (yyyy-MM-dd'T'HH:mm:ss) and Odoo format (yyyy-MM-dd HH:mm:ss)
 */
private fun parseDateTime(dateTimeStr: String?): Date? {
    if (dateTimeStr.isNullOrBlank()) return null

    return try {
        // Try ISO 8601 format first (with T separator)
        if (dateTimeStr.contains("T")) {
            iso8601DateTimeFormatter.get()!!.parse(dateTimeStr)
        } else {
            // Fall back to Odoo format (with space separator)
            odooDateTimeFormatter.get()!!.parse(dateTimeStr)
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Extension function to convert Visit domain model to VisitRequest (for API)
 * Requires mobileUid to be set (should be generated when creating a new visit)
 */
fun Visit.toRequest(): VisitRequest {
    return VisitRequest(
        mobileUid = mobileUid ?: throw IllegalStateException("mobileUid is required for API request"),
        partnerId = partnerId,
        visitDatetime = iso8601DateTimeFormatter.get()!!.format(visitDatetime),
        memo = memo
    )
}

/**
 * Format Date to ISO 8601 datetime string
 */
fun Date.toIso8601String(): String {
    return iso8601DateTimeFormatter.get()!!.format(this)
}
