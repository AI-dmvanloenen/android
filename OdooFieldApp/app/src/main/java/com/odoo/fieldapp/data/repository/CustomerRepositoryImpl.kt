package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.data.remote.mapper.toRequest
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
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

    companion object {
        private const val TAG = "CustomerRepository"
    }
    
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
     * 3. Save to local database (in transaction)
     * 4. Emit Success with updated data
     * 5. If error, emit Error with message
     */
    override suspend fun syncCustomersFromOdoo(): Flow<Resource<List<Customer>>> = flow {
        try {
            // 1. Emit loading state with current local data
            val localCustomers = customerDao.getAllCustomersOnce()
            emit(Resource.Loading(data = localCustomers.map { it.toDomain() }))

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

                // 4. Filter out records with null IDs (prevents primary key conflicts)
                val validResponses = customerResponses.filter { it.odooId != null }

                // 5. Convert API responses to domain models
                val customers = validResponses.map { it.toDomain() }

                // 6. Save to local database (atomic transaction)
                val entities = customers.map { it.toEntity() }
                customerDao.syncCustomers(entities)

                // 7. Emit success
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
            // Log the full exception for debugging
            Log.e(TAG, "Customer sync failed", e)

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
     * Create a new customer and sync to Odoo
     *
     * Flow:
     * 1. Generate UUID for mobileUid
     * 2. Save locally with temporary negative ID and syncState = PENDING
     * 3. Call API with Bearer token
     * 4. On success: update local record with Odoo ID, set SYNCED
     * 5. On failure: keep PENDING for retry, emit error
     */
    override suspend fun createCustomer(customer: Customer): Flow<Resource<Customer>> = flow {
        try {
            // 1. Emit loading
            emit(Resource.Loading())

            // 2. Generate UUID for mobileUid
            val mobileUid = UUID.randomUUID().toString()

            // 3. Generate temporary negative ID for local storage
            val minId = customerDao.getMinCustomerId() ?: 0
            val tempId = if (minId >= 0) -1 else minId - 1

            // 4. Create customer with PENDING state
            val pendingCustomer = customer.copy(
                id = tempId,
                mobileUid = mobileUid,
                syncState = SyncState.PENDING,
                lastModified = Date()
            )

            // 5. Save locally
            customerDao.insertCustomer(pendingCustomer.toEntity())
            Log.d(TAG, "Customer saved locally with tempId=$tempId, mobileUid=$mobileUid")

            // 6. Get API key
            val apiKey = apiKeyProvider.getApiKey()
            if (apiKey.isNullOrBlank()) {
                emit(Resource.Error("API key not configured. Customer saved locally.", data = pendingCustomer))
                return@flow
            }

            // 7. Call API to create customer
            val request = pendingCustomer.toRequest()
            val response = apiService.createCustomers(apiKey, listOf(request))

            if (response.isSuccessful) {
                val createdCustomers = response.body()
                val createdResponse = createdCustomers?.firstOrNull { it.mobileUid == mobileUid }

                if (createdResponse != null && createdResponse.odooId != null) {
                    // 8. Success: Update local record with Odoo ID
                    val syncedCustomer = createdResponse.toDomain()

                    // Delete the temp record and insert with real ID
                    customerDao.deleteCustomerById(tempId)
                    customerDao.insertCustomer(syncedCustomer.toEntity())

                    Log.d(TAG, "Customer synced successfully, odooId=${syncedCustomer.id}")
                    emit(Resource.Success(syncedCustomer))
                } else {
                    // API returned success but no matching customer
                    Log.w(TAG, "API response didn't include created customer")
                    emit(Resource.Error("Customer created but response incomplete", data = pendingCustomer))
                }
            } else {
                // API error - customer remains in PENDING state
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed. Please check your API key."
                    403 -> "Access denied. Your API key doesn't have permission."
                    404 -> "Endpoint not found. Please check the base URL."
                    500 -> "Odoo server error. Please try again later."
                    else -> "Failed to sync: ${response.message()}"
                }
                Log.e(TAG, "Customer creation API failed: $errorMessage")
                emit(Resource.Error(errorMessage, data = pendingCustomer))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Customer creation failed", e)

            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "Network error. Customer saved locally."
                e.message?.contains("timeout") == true ->
                    "Request timed out. Customer saved locally."
                else ->
                    "Creation failed: ${e.message ?: "Unknown error"}"
            }
            emit(Resource.Error(errorMessage))
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

    /**
     * Get cached base URL synchronously (for use in interceptors)
     * This avoids blocking the network thread
     */
    fun getCachedBaseUrl(): String
}
