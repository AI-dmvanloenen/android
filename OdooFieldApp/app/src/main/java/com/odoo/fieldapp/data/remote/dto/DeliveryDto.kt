package com.odoo.fieldapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Delivery Line API responses
 */
data class DeliveryLineResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("product_id")
    val productId: Int?,

    @SerializedName("product_name")
    val productName: String,

    @SerializedName("quantity")
    val quantity: Double?,

    @SerializedName("quantity_done")
    val quantityDone: Double?,

    @SerializedName("uom")
    val uom: String?
)

/**
 * Data Transfer Object for Delivery API responses
 * Matches the structure returned by Odoo API endpoint
 */
data class DeliveryResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("name")
    val name: String,

    @SerializedName("partner_id")
    val partnerId: Int?,

    @SerializedName("scheduled_date")
    val scheduledDate: String?,  // Datetime string from Odoo (e.g., "2024-01-15 10:30:00")

    @SerializedName("state")
    val state: String?,

    @SerializedName("sale_id")
    val saleId: Int?,

    @SerializedName("lines")
    val lines: List<DeliveryLineResponse>?
)

/**
 * Paginated response wrapper for deliveries endpoint
 */
data class DeliveryPaginatedResponse(
    @SerializedName("data")
    val data: List<DeliveryResponse>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("offset")
    val offset: Int
)

/**
 * Request payload for validating a delivery (marking as done)
 */
data class ValidateDeliveryRequest(
    @SerializedName("id")
    val id: Int
)

/**
 * Response from delivery validation endpoint
 * Can contain either success with delivery data or error
 */
data class ValidateDeliveryResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("delivery")
    val delivery: DeliveryResponse?,

    @SerializedName("error")
    val error: String?,

    @SerializedName("status")
    val status: Int?
)
