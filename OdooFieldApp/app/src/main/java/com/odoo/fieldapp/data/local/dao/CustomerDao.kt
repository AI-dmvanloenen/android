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
     * Get a single customer by ID
     */
    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: String): CustomerEntity?
    
    /**
     * Get a customer by cradleUid (useful for sync operations)
     */
    @Query("SELECT * FROM customers WHERE cradleUid = :cradleUid")
    suspend fun getCustomerByCradleUid(cradleUid: String): CustomerEntity?
    
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
     * Get count of customers (useful for UI)
     */
    @Query("SELECT COUNT(*) FROM customers")
    fun getCustomerCount(): Flow<Int>
}
