package com.odoo.fieldapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Product API responses
 * Matches the structure returned by Odoo API endpoint
 */
data class ProductResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("name")
    val name: String,

    @SerializedName("default_code")
    val defaultCode: String?,  // SKU

    @SerializedName("barcode")
    val barcode: String?,      // For future scanner feature

    @SerializedName("list_price")
    val listPrice: Double?,

    @SerializedName("uom_id")
    val uomId: Int?,

    @SerializedName("uom_name")
    val uomName: String?,

    @SerializedName("categ_id")
    val categId: Int?,

    @SerializedName("categ_name")
    val categName: String?,

    @SerializedName("type")
    val type: String?,         // consu, service, product

    @SerializedName("active")
    val active: Boolean?
)

/**
 * Paginated response wrapper for products endpoint
 */
data class ProductPaginatedResponse(
    @SerializedName("data")
    val data: List<ProductResponse>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("offset")
    val offset: Int
)
