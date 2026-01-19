package com.odoo.fieldapp.data.repository

import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CustomerRepository
 * 
 * This is the single source of truth for customer data.
 * It coordinates between local database (Room) and remote API (Retrofit)
 */
@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : CustomerRepository {
    
    /**
     * Get customers from local database
     * Returns a Flow that emits whenever the database changes
     */
    override fun getCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    /**
     * Get a single customer by ID (Odoo record ID)
     */
    override suspend fun getCustomerById(customerId: Int): Customer? {
        return customerDao.getCustomerById(customerId)?.toDomain()
    }
    
    /**
     * Search customers by name
     */
    override fun searchCustomers(query: String): Flow<List<Customer>> {
        return customerDao.searchCustomers(query)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    /**
     * Sync customers from Odoo API to local database
     * 
     * Flow of Resource states:
     * 1. Emit Loading with existing local data
     * 2. Fetch from API
     * 3. Save to local database
     * 4. Emit Success with updated data
     * 5. If error, emit Error with existing local data
     */
    override suspend fun syncCustomersFromOdoo(): Flow<Resource<List<Customer>>> = flow {
        try {
            // 1. Emit loading state with current local data
            val localCustomers = customerDao.getAllCustomers()
            emit(Resource.Loading(data = null))
            
            // 2. Get API key
            val apiKey = apiKeyProvider.getApiKey()
            if (apiKey.isNullOrBlank()) {
                emit(Resource.Error("API key not configured. Please add your API key in settings."))
                return@flow
            }
            
            // 3. Fetch from Odoo API
            val response = apiService.getCustomers(apiKey)
            
            if (response.isSuccessful) {
                val customerResponses = response.body() ?: emptyList()
                
                // 4. Convert API responses to domain models
                val customers = customerResponses.map { it.toDomain() }
                
                // 5. Save to local database
                val entities = customers.map { it.toEntity() }
                customerDao.insertCustomers(entities)
                
                // 6. Emit success
                emit(Resource.Success(customers))
            } else {
                // API error
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed. Please check your API key."
                    403 -> "Access denied. Your API key doesn't have permission."
                    404 -> "Endpoint not found. Please check the base URL."
                    500 -> "Odoo server error. Please try again later."
                    else -> "Failed to sync: ${response.message()}"
                }
                emit(Resource.Error(errorMessage))
            }
            
        } catch (e: Exception) {
            // Network or other error
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true -> 
                    "Network error. Please check your internet connection."
                e.message?.contains("timeout") == true -> 
                    "Request timed out. Please try again."
                else -> 
                    "Sync failed: ${e.message ?: "Unknown error"}"
            }
            emit(Resource.Error(errorMessage))
        }
    }
    
    /**
     * Create a new customer locally (for future phases)
     */
    override suspend fun createCustomer(customer: Customer): Result<Customer> {
        return try {
            customerDao.insertCustomer(customer.toEntity())
            Result.success(customer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a customer (for future phases)
     */
    override suspend fun deleteCustomer(customer: Customer): Result<Unit> {
        return try {
            customerDao.deleteCustomer(customer.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Interface for providing API key and server URL
 * Implementation will use DataStore to persist the values
 */
interface ApiKeyProvider {
    suspend fun getApiKey(): String?
    suspend fun setApiKey(apiKey: String)
    suspend fun clearApiKey()
    suspend fun getServerUrl(): String?
    suspend fun setServerUrl(serverUrl: String)
}
