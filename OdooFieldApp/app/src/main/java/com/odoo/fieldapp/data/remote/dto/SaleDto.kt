package com.odoo.fieldapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Sale Line API responses
 */
data class SaleLineResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("product_id")
    val productId: Int?,

    @SerializedName("product_name")
    val productName: String?,  // Made nullable for defensive parsing

    @SerializedName("product_uom_qty")
    val productUomQty: Double?,  // Ordered quantity

    @SerializedName("qty_delivered")
    val qtyDelivered: Double?,   // Delivered quantity

    @SerializedName("qty_invoiced")
    val qtyInvoiced: Double?,    // Invoiced quantity

    @SerializedName("price_unit")
    val priceUnit: Double?,

    @SerializedName("discount")
    val discount: Double?,

    @SerializedName("price_subtotal")
    val priceSubtotal: Double?,

    @SerializedName("uom")
    val uom: String?
)

/**
 * Data Transfer Object for Sale API responses
 * Matches the structure returned by Odoo API endpoint
 */
data class SaleResponse(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("mobile_uid")
    val mobileUid: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("date_order")
    val dateOrder: String?,  // Date string from Odoo (e.g., "2025-04-11")

    @SerializedName("amount_total")
    val amountTotal: Double?,

    @SerializedName("state")
    val state: String?,  // Order state (draft, sent, sale, done, cancel)

    @SerializedName("partner_id")
    val partnerId: Int?,  // Customer ID (or null if no customer linked)

    @SerializedName("write_date")
    val writeDate: String?,  // Last modified timestamp

    @SerializedName("lines")
    val lines: List<SaleLineResponse>?
)

/**
 * Request payload for sale order lines when creating a sale
 */
data class SaleLineRequest(
    @SerializedName("product_id")
    val productId: Int,

    @SerializedName("product_uom_qty")
    val productUomQty: Double,

    @SerializedName("price_unit")
    val priceUnit: Double?
)

/**
 * Request payload for creating sales
 */
data class SaleRequest(
    @SerializedName("mobile_uid")
    val mobileUid: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("date_order")
    val dateOrder: String?,

    @SerializedName("amount_total")
    val amountTotal: Double?,

    @SerializedName("partner_id")
    val partnerId: Int?,

    @SerializedName("lines")
    val lines: List<SaleLineRequest>?
)

/**
 * Paginated response wrapper for sales endpoint
 */
data class SalePaginatedResponse(
    @SerializedName("data")
    val data: List<SaleResponse>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("offset")
    val offset: Int
)

/**
 * Response wrapper for sale creation endpoint
 */
data class SaleCreateResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("data")
    val data: List<SaleResponse>
)
