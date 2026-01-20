package com.odoo.fieldapp.data.remote.api

import com.odoo.fieldapp.data.remote.dto.CustomerCreateResponse
import com.odoo.fieldapp.data.remote.dto.CustomerPaginatedResponse
import com.odoo.fieldapp.data.remote.dto.CustomerRequest
import com.odoo.fieldapp.data.remote.dto.DeliveryPaginatedResponse
import com.odoo.fieldapp.data.remote.dto.PaymentCreateResponse
import com.odoo.fieldapp.data.remote.dto.PaymentPaginatedResponse
import com.odoo.fieldapp.data.remote.dto.PaymentRequest
import com.odoo.fieldapp.data.remote.dto.ProductPaginatedResponse
import com.odoo.fieldapp.data.remote.dto.SalePaginatedResponse
import com.odoo.fieldapp.data.remote.dto.ValidateDeliveryRequest
import com.odoo.fieldapp.data.remote.dto.ValidateDeliveryResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service for Odoo endpoints
 * 
 * All endpoints require Authorization header with API key
 */
interface OdooApiService {
    
    /**
     * Fetch all customers from Odoo
     * GET https://moko.odoo.com/customer
     * Returns paginated response wrapper
     *
     * @param since Optional ISO 8601 datetime to fetch only records modified since this time
     */
    @GET("customer")
    suspend fun getCustomers(
        @Header("Authorization") apiKey: String,
        @Query("since") since: String? = null
    ): Response<CustomerPaginatedResponse>
    
    /**
     * Create new customers in Odoo (batch operation)
     * POST https://moko.odoo.com/customer
     *
     * Accepts a list of customers in the request body
     */
    @POST("customer")
    suspend fun createCustomers(
        @Header("Authorization") apiKey: String,
        @Body customers: List<CustomerRequest>
    ): Response<CustomerCreateResponse>
    
    /**
     * Fetch all sales from Odoo
     * GET /sales
     * Returns paginated response wrapper
     *
     * @param since Optional ISO 8601 datetime to fetch only records modified since this time
     */
    @GET("sales")
    suspend fun getSales(
        @Header("Authorization") apiKey: String,
        @Query("since") since: String? = null
    ): Response<SalePaginatedResponse>

    /**
     * Fetch all deliveries from Odoo
     * GET /deliveries
     * Returns paginated response wrapper
     *
     * @param since Optional ISO 8601 datetime to fetch only records modified since this time
     */
    @GET("deliveries")
    suspend fun getDeliveries(
        @Header("Authorization") apiKey: String,
        @Query("since") since: String? = null
    ): Response<DeliveryPaginatedResponse>

    /**
     * Validate a delivery (mark as done)
     * POST /deliveries
     *
     * Sets quantities done to match demand and validates the picking
     */
    @POST("deliveries")
    suspend fun validateDelivery(
        @Header("Authorization") apiKey: String,
        @Body request: ValidateDeliveryRequest
    ): Response<ValidateDeliveryResponse>

    /**
     * Fetch all payments from Odoo
     * GET /payments
     * Returns paginated response wrapper
     *
     * @param since Optional ISO 8601 datetime to fetch only records modified since this time
     */
    @GET("payments")
    suspend fun getPayments(
        @Header("Authorization") apiKey: String,
        @Query("since") since: String? = null
    ): Response<PaymentPaginatedResponse>

    /**
     * Create new payments in Odoo (batch operation)
     * POST /payments
     *
     * Accepts a list of payments in the request body
     */
    @POST("payments")
    suspend fun createPayments(
        @Header("Authorization") apiKey: String,
        @Body payments: List<PaymentRequest>
    ): Response<PaymentCreateResponse>

    /**
     * Fetch all saleable products from Odoo
     * GET /products
     * Returns paginated response wrapper
     *
     * @param since Optional ISO 8601 datetime to fetch only records modified since this time
     */
    @GET("products")
    suspend fun getProducts(
        @Header("Authorization") apiKey: String,
        @Query("since") since: String? = null
    ): Response<ProductPaginatedResponse>
}
