package com.odoo.fieldapp.data.remote.mapper

import com.odoo.fieldapp.data.remote.dto.PaymentRequest
import com.odoo.fieldapp.data.remote.dto.PaymentResponse
import com.odoo.fieldapp.domain.model.Payment
import com.odoo.fieldapp.domain.model.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extension function to convert PaymentResponse (API) to Payment domain model
 */
fun PaymentResponse.toDomain(): Payment {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val datetimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    return Payment(
        id = id ?: 0,
        mobileUid = mobileUid,
        name = name,
        partnerId = partnerId,
        partnerName = null,  // Will be resolved from local database
        amount = amount ?: 0.0,
        date = date?.let {
            try {
                dateFormatter.parse(it)
            } catch (e: Exception) {
                null
            }
        },
        memo = memo,
        journalId = journalId,
        state = state ?: "draft",
        syncState = SyncState.SYNCED,
        lastModified = writeDate?.let {
            try {
                datetimeFormatter.parse(it)
            } catch (e: Exception) {
                Date()
            }
        } ?: Date()
    )
}

/**
 * Extension function to convert Payment domain model to PaymentRequest (for API)
 */
fun Payment.toRequest(): PaymentRequest {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    return PaymentRequest(
        mobileUid = mobileUid ?: "",
        partnerId = partnerId ?: 0,
        amount = amount,
        date = date?.let { dateFormatter.format(it) },
        memo = memo,
        journalId = journalId
    )
}
