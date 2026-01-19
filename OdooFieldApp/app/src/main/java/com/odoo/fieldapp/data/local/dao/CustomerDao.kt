package com.odoo.fieldapp.data.local.dao

import androidx.room.*
import com.odoo.fieldapp.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Customer operations
 * 
 * Uses Flow for reactive database queries - UI will automatically update when data changes
 */
@Dao
interface CustomerDao {
    
    /**
     * Get all customers as a Flow (reactive)
     * UI will automatically update when customers change
     */
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    /**
     * Get all customers once (non-reactive, for loading state)
     */
    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    /**
     * Get all customer IDs (for sync deletion check)
     */
    @Query("SELECT id FROM customers")
    suspend fun getAllCustomerIds(): List<Int>

    /**
     * Get a single customer by ID (Odoo record ID)
     */
    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: Int): CustomerEntity?
    
    /**
     * Search customers by name
     */
    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>
    
    /**
     * Get customers by sync state (useful for finding pending syncs)
     */
    @Query("SELECT * FROM customers WHERE syncState = :syncState")
    suspend fun getCustomersBySyncState(syncState: String): List<CustomerEntity>
    
    /**
     * Insert a single customer
     * OnConflictStrategy.REPLACE will update if customer already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)
    
    /**
     * Insert multiple customers (batch operation for sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    /**
     * Atomic sync operation: deletes stale records and inserts new ones in a single transaction
     * Ensures database consistency if sync is interrupted
     */
    @Transaction
    suspend fun syncCustomers(customers: List<CustomerEntity>) {
        val incomingIds = customers.map { it.id }.toSet()
        deleteCustomersNotIn(incomingIds)
        insertCustomers(customers)
    }
    
    /**
     * Update a customer
     */
    @Update
    suspend fun updateCustomer(customer: CustomerEntity)
    
    /**
     * Delete a customer
     */
    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)
    
    /**
     * Delete all customers (useful for full re-sync)
     */
    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    /**
     * Delete customers not in the given set of IDs
     * Used during sync to remove records deleted on the server
     */
    @Query("DELETE FROM customers WHERE id NOT IN (:ids)")
    suspend fun deleteCustomersNotIn(ids: Set<Int>)
    
    /**
     * Get count of customers (useful for UI)
     */
    @Query("SELECT COUNT(*) FROM customers")
    fun getCustomerCount(): Flow<Int>

    /**
     * Get minimum customer ID (for generating temporary negative IDs for new customers)
     */
    @Query("SELECT MIN(id) FROM customers")
    suspend fun getMinCustomerId(): Int?

    /**
     * Get customer by mobileUid (for finding local record after sync)
     */
    @Query("SELECT * FROM customers WHERE mobileUid = :mobileUid")
    suspend fun getCustomerByMobileUid(mobileUid: String): CustomerEntity?

    /**
     * Delete customer by ID
     */
    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: Int)
}
