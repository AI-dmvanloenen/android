package com.odoo.fieldapp.data.local.dao

import androidx.room.*
import com.odoo.fieldapp.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Payment operations
 *
 * Provides methods to interact with the payments table in Room database.
 * Uses Flow for reactive queries that emit updates when data changes.
 */
@Dao
interface PaymentDao {

    /**
     * Get all payments ordered by date descending
     * Returns a Flow that emits whenever the data changes
     */
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    /**
     * Get all payments once (not as Flow)
     * Used for sync operations
     */
    @Query("SELECT * FROM payments ORDER BY date DESC")
    suspend fun getAllPaymentsOnce(): List<PaymentEntity>

    /**
     * Get a single payment by ID
     */
    @Query("SELECT * FROM payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: Int): PaymentEntity?

    /**
     * Get a payment by mobile UID
     */
    @Query("SELECT * FROM payments WHERE mobileUid = :mobileUid")
    suspend fun getPaymentByMobileUid(mobileUid: String): PaymentEntity?

    /**
     * Search payments by name or memo
     */
    @Query("SELECT * FROM payments WHERE name LIKE '%' || :query || '%' OR memo LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchPayments(query: String): Flow<List<PaymentEntity>>

    /**
     * Get payments for a specific customer
     */
    @Query("SELECT * FROM payments WHERE partnerId = :customerId ORDER BY date DESC")
    fun getPaymentsByCustomer(customerId: Int): Flow<List<PaymentEntity>>

    /**
     * Get the minimum payment ID (for generating temp IDs for new records)
     */
    @Query("SELECT MIN(id) FROM payments")
    suspend fun getMinPaymentId(): Int?

    /**
     * Insert a single payment
     * OnConflictStrategy.REPLACE will update if record exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    /**
     * Insert multiple payments
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    /**
     * Update a payment
     */
    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    /**
     * Delete a payment
     */
    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    /**
     * Delete a payment by ID
     */
    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Int)

    /**
     * Sync payments from API
     * This is a transaction that deletes old records with positive IDs and inserts new ones
     * Keeps records with negative IDs (pending local records)
     */
    @Transaction
    suspend fun syncPayments(payments: List<PaymentEntity>) {
        // Delete all synced records (positive IDs)
        deleteAllSyncedPayments()
        // Insert new records
        insertPayments(payments)
    }

    /**
     * Delete all payments with positive IDs (synced from server)
     * Keeps payments with negative IDs (pending local records)
     */
    @Query("DELETE FROM payments WHERE id > 0")
    suspend fun deleteAllSyncedPayments()
}
