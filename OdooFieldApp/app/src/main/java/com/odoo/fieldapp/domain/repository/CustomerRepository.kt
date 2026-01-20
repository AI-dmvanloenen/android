package com.odoo.fieldapp.domain.repository

import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Customer operations
 * 
 * This defines the contract for data operations.
 * The implementation will handle both local (Room) and remote (API) data sources.
 */
interface CustomerRepository {
    
    /**
     * Get all customers from local database as a Flow
     * UI will automatically update when data changes
     */
    fun getCustomers(): Flow<List<Customer>>
    
    /**
     * Get a single customer by ID (Odoo record ID) from local database
     */
    suspend fun getCustomerById(customerId: Int): Customer?
    
    /**
     * Search customers by name
     */
    fun searchCustomers(query: String): Flow<List<Customer>>
    
    /**
     * Sync customers from Odoo API to local database
     * This is the main operation for Phase 1
     * 
     * @return Resource wrapper with loading/success/error states
     */
    suspend fun syncCustomersFromOdoo(): Flow<Resource<List<Customer>>>
    
    /**
     * Create a new customer and sync to Odoo
     *
     * @param customer The customer to create (id will be ignored, mobileUid will be generated)
     * @return Flow emitting Loading, then Success (with created customer including Odoo ID) or Error
     */
    suspend fun createCustomer(customer: Customer): Flow<Resource<Customer>>

    /**
     * Update customer GPS location coordinates
     *
     * Updates the customer's latitude and longitude both locally and syncs to Odoo.
     * Location is saved locally first, then synced to Odoo if online.
     * If sync fails, the customer remains marked as PENDING for later sync.
     *
     * @param customerId The Odoo record ID of the customer
     * @param latitude GPS latitude coordinate (-90 to 90)
     * @param longitude GPS longitude coordinate (-180 to 180)
     * @return Flow emitting Loading, then Success (with updated customer) or Error
     */
    suspend fun updateCustomerLocation(
        customerId: Int,
        latitude: Double,
        longitude: Double
    ): Flow<Resource<Customer>>

    /**
     * Delete a customer locally (for future phases)
     */
    suspend fun deleteCustomer(customer: Customer): Result<Unit>
}
