package com.odoo.fieldapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Customer API responses
 * Matches the structure returned by Odoo API
 */
data class CustomerResponse(
    @SerializedName("name")
    val name: String,

    @SerializedName("city")
    val city: String?,

    @SerializedName("tax_id")
    val taxId: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("phone")
    val phone: String?,

    @SerializedName("website")
    val website: String?,

    @SerializedName("date")
    val date: String?,  // ISO date string from Odoo

    @SerializedName("id")
    val odooId: Int?,   // Odoo record ID

    @SerializedName("mobile_uid")
    val mobileUid: String? = null,  // UUID for locally-created customers

    @SerializedName("partner_latitude")
    val latitude: Double? = null,  // GPS latitude coordinate

    @SerializedName("partner_longitude")
    val longitude: Double? = null  // GPS longitude coordinate
)

/**
 * Request payload for creating customers
 * Matches the structure expected by Odoo API
 */
data class CustomerRequest(
    @SerializedName("mobile_uid")
    val mobileUid: String,  // Required: UUID for locally-created customers

    @SerializedName("name")
    val name: String,

    @SerializedName("city")
    val city: String?,

    @SerializedName("tax_id")
    val taxId: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("phone")
    val phone: String?,

    @SerializedName("website")
    val website: String?,

    @SerializedName("date")
    val date: String?,  // ISO date string "2025-04-11"

    @SerializedName("partner_latitude")
    val latitude: Double? = null,  // GPS latitude coordinate

    @SerializedName("partner_longitude")
    val longitude: Double? = null  // GPS longitude coordinate
)

/**
 * Generic API response wrapper
 */
data class OdooApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T?,

    @SerializedName("error")
    val error: String?
)

/**
 * Paginated response wrapper for customers endpoint
 */
data class CustomerPaginatedResponse(
    @SerializedName("data")
    val data: List<CustomerResponse>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("offset")
    val offset: Int
)

/**
 * Response wrapper for customer creation endpoint
 */
data class CustomerCreateResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("data")
    val data: List<CustomerResponse>
)
