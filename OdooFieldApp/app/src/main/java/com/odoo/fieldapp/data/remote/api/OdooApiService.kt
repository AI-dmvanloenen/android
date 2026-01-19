package com.odoo.fieldapp.data.remote.api

import com.odoo.fieldapp.data.remote.dto.CustomerRequest
import com.odoo.fieldapp.data.remote.dto.CustomerResponse
import com.odoo.fieldapp.data.remote.dto.SaleResponse
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
     */
    @GET("customer")
    suspend fun getCustomers(
        @Header("Authorization") apiKey: String
    ): Response<List<CustomerResponse>>
    
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
     */
    @GET("sales")
    suspend fun getSales(
        @Header("Authorization") apiKey: String
    ): Response<List<SaleResponse>>

    // Future endpoints:
    // @GET("payment")
    // suspend fun getPayments(@Header("Authorization") apiKey: String): Response<List<PaymentResponse>>

    // @GET("delivery")
    // suspend fun getDeliveries(@Header("Authorization") apiKey: String): Response<List<DeliveryResponse>>
}
