package com.odoo.fieldapp.data.remote.mapper

import com.odoo.fieldapp.data.remote.dto.ProductResponse
import com.odoo.fieldapp.domain.model.Product

/**
 * Convert ProductResponse from API to domain Product model
 * Uses Odoo ID as the primary identifier to prevent duplicates on sync
 */
fun ProductResponse.toDomain(): Product {
    return Product(
        id = id ?: 0,
        name = name,
        defaultCode = defaultCode,
        barcode = barcode,
        listPrice = listPrice ?: 0.0,
        uomId = uomId,
        uomName = uomName,
        categId = categId,
        categName = categName,
        type = type ?: "consu",
        active = active ?: true
    )
}
