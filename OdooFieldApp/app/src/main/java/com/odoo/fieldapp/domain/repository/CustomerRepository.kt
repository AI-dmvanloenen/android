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
     * Get a single customer by ID from local database
     */
    suspend fun getCustomerById(customerId: String): Customer?
    
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
     * Create a new customer locally (for future phases)
     * Will be marked as PENDING and synced later
     */
    suspend fun createCustomer(customer: Customer): Result<Customer>
    
    /**
     * Delete a customer locally (for future phases)
     */
    suspend fun deleteCustomer(customer: Customer): Result<Unit>
}
