package com.odoo.fieldapp.domain.repository

import com.odoo.fieldapp.domain.model.Payment
import com.odoo.fieldapp.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Payment operations
 *
 * This defines the contract for data operations.
 * The implementation will handle both local (Room) and remote (API) data sources.
 */
interface PaymentRepository {

    /**
     * Get all payments from local database as a Flow
     * UI will automatically update when data changes
     */
    fun getPayments(): Flow<List<Payment>>

    /**
     * Get a single payment by ID (Odoo record ID) from local database
     */
    suspend fun getPaymentById(paymentId: Int): Payment?

    /**
     * Search payments by name or memo
     */
    fun searchPayments(query: String): Flow<List<Payment>>

    /**
     * Get payments for a specific customer
     */
    fun getPaymentsByCustomer(customerId: Int): Flow<List<Payment>>

    /**
     * Sync payments from Odoo API to local database
     *
     * @return Resource wrapper with loading/success/error states
     */
    suspend fun syncPaymentsFromOdoo(): Flow<Resource<List<Payment>>>

    /**
     * Create a new payment and sync to Odoo
     *
     * Flow:
     * 1. Save locally with PENDING state
     * 2. Call API to create in Odoo
     * 3. On success: update local record with Odoo ID, set SYNCED
     * 4. On failure: keep PENDING for retry
     *
     * @param payment The payment to create
     * @return Resource wrapper with the created payment or error
     */
    suspend fun createPayment(payment: Payment): Flow<Resource<Payment>>
}
