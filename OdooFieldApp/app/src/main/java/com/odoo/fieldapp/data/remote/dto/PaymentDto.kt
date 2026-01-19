package com.odoo.fieldapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Payment API responses
 * Matches the structure returned by Odoo API endpoint
 */
data class PaymentResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("mobile_uid")
    val mobileUid: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("partner_id")
    val partnerId: Int?,

    @SerializedName("amount")
    val amount: Double?,

    @SerializedName("date")
    val date: String?,  // Date string from Odoo (e.g., "2025-04-11")

    @SerializedName("memo")
    val memo: String?,

    @SerializedName("journal_id")
    val journalId: Int?,

    @SerializedName("state")
    val state: String?,

    @SerializedName("write_date")
    val writeDate: String?  // Datetime string for last modified
)

/**
 * Request payload for creating payments
 */
data class PaymentRequest(
    @SerializedName("mobile_uid")
    val mobileUid: String,

    @SerializedName("partner_id")
    val partnerId: Int,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("date")
    val date: String?,  // ISO date format (e.g., "2025-04-11")

    @SerializedName("memo")
    val memo: String?,

    @SerializedName("journal_id")
    val journalId: Int?
)

/**
 * Paginated response wrapper for payments endpoint
 */
data class PaymentPaginatedResponse(
    @SerializedName("data")
    val data: List<PaymentResponse>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("offset")
    val offset: Int
)

/**
 * Response from payment creation endpoint
 */
data class PaymentCreateResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("data")
    val data: List<PaymentResponse>
)
