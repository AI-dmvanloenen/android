package com.odoo.fieldapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Visit API responses
 * Matches the structure returned by Odoo API endpoint
 */
data class VisitResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("mobile_uid")
    val mobileUid: String?,

    @SerializedName("partner_id")
    val partnerId: Int?,

    @SerializedName("partner_name")
    val partnerName: String?,

    @SerializedName("visit_datetime")
    val visitDatetime: String?,  // ISO 8601 datetime string

    @SerializedName("memo")
    val memo: String?,

    @SerializedName("write_date")
    val writeDate: String?  // Datetime string for last modified
)

/**
 * Request payload for creating visits
 */
data class VisitRequest(
    @SerializedName("mobile_uid")
    val mobileUid: String,

    @SerializedName("partner_id")
    val partnerId: Int,

    @SerializedName("visit_datetime")
    val visitDatetime: String,  // ISO 8601 datetime format

    @SerializedName("memo")
    val memo: String?
)

/**
 * Paginated response wrapper for visits endpoint
 */
data class VisitPaginatedResponse(
    @SerializedName("data")
    val data: List<VisitResponse>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("offset")
    val offset: Int
)

/**
 * Response from visit creation endpoint
 */
data class VisitCreateResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("data")
    val data: List<VisitResponse>
)
