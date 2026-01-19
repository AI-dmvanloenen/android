package com.odoo.fieldapp.data.remote.mapper

import com.odoo.fieldapp.data.remote.dto.CustomerRequest
import com.odoo.fieldapp.data.remote.dto.CustomerResponse
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.SyncState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date formatter for Odoo API (ISO format: 2025-04-11)
 */
private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

/**
 * Convert CustomerResponse from API to domain Customer model
 * Uses odooId as the primary identifier to prevent duplicates on sync
 */
fun CustomerResponse.toDomain(): Customer {
    return Customer(
        id = odooId ?: 0,  // Use Odoo record ID directly (prevents duplicates)
        name = name,
        city = city,
        taxId = taxId,
        email = email,
        phone = phone,
        website = website,
        date = date?.let {
            try {
                dateFormatter.parse(it)
            } catch (e: Exception) {
                null
            }
        },
        syncState = SyncState.SYNCED,  // Data from Odoo is already synced
        lastModified = Date()
    )
}

/**
 * Convert domain Customer model to CustomerRequest for API
 */
fun Customer.toRequest(): CustomerRequest {
    return CustomerRequest(
        name = name,
        city = city,
        taxId = taxId,
        email = email,
        phone = phone,
        website = website,
        date = date?.let { dateFormatter.format(it) }
    )
}
