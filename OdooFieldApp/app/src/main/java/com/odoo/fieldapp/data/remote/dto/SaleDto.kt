package com.odoo.fieldapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Sale API responses
 * Matches the structure returned by Odoo API endpoint
 */
data class SaleResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("name")
    val name: String,

    @SerializedName("date_order")
    val dateOrder: String?,  // Date string from Odoo (e.g., "2025-04-11")

    @SerializedName("amount_total")
    val amountTotal: Double?,

    @SerializedName("partner_id")
    val partnerId: Int?  // Customer ID (or null if no customer linked)
)

/**
 * Request payload for creating sales (future use)
 */
data class SaleRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("date_order")
    val dateOrder: String?,

    @SerializedName("amount_total")
    val amountTotal: Double?,

    @SerializedName("partner_id")
    val partnerId: Int?
)
