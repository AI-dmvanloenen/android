package com.odoo.fieldapp.data.remote.api

import com.odoo.fieldapp.data.remote.dto.CustomerPaginatedResponse
import com.odoo.fieldapp.data.remote.dto.CustomerRequest
import com.odoo.fieldapp.data.remote.dto.CustomerResponse
import com.odoo.fieldapp.data.remote.dto.DeliveryPaginatedResponse
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
     */
    @GET("customer")
    suspend fun getCustomers(
        @Header("Authorization") apiKey: String
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
    ): Response<List<CustomerResponse>>
    
    /**
     * Fetch all sales from Odoo
     * GET /sales
     * Returns paginated response wrapper
     */
    @GET("sales")
    suspend fun getSales(
        @Header("Authorization") apiKey: String
    ): Response<SalePaginatedResponse>

    /**
     * Fetch all deliveries from Odoo
     * GET /deliveries
     * Returns paginated response wrapper
     */
    @GET("deliveries")
    suspend fun getDeliveries(
        @Header("Authorization") apiKey: String
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

    // Future endpoints:
    // @GET("payment")
    // suspend fun getPayments(@Header("Authorization") apiKey: String): Response<List<PaymentResponse>>
}
