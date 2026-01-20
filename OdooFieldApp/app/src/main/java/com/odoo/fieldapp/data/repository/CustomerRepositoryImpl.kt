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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        private const val SYNC_KEY = "customers"
        private val dateTimeFormatter = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        }
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
        // 1. Emit loading state with current local data
        val localCustomers = customerDao.getAllCustomersOnce()
        emit(Resource.Loading(data = localCustomers.map { it.toDomain() }))

        // 2. Get API key
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            val cachedData = localCustomers.map { it.toDomain() }
            emit(Resource.Error("API key not configured. Please add your API key in settings.", data = cachedData))
            return@flow
        }

        // 3. Get last sync time for incremental sync
        // If database is empty but we have a lastSyncTime, force full sync
        // This handles cases where DB was cleared but DataStore persisted
        var lastSyncTime = apiKeyProvider.getLastSyncTime(SYNC_KEY)
        if (localCustomers.isEmpty() && lastSyncTime != null) {
            Log.w(TAG, "Database empty but lastSyncTime exists ($lastSyncTime). Forcing full sync.")
            apiKeyProvider.setLastSyncTime(SYNC_KEY, null)
            lastSyncTime = null
        }
        Log.d(TAG, "Last sync time: $lastSyncTime")

        // 4. Fetch from Odoo API with since filter
        val response = apiService.getCustomers(apiKey, lastSyncTime)

        if (response.isSuccessful) {
            val paginatedResponse = response.body()
            val customerResponses = paginatedResponse?.data ?: emptyList()

            // 5. Filter out records with null IDs (prevents primary key conflicts)
            val validResponses = customerResponses.filter { it.odooId != null }

            // 6. Convert API responses to domain models
            val customers = validResponses.map { it.toDomain() }

            // 7. Save to local database
            // For incremental sync, we upsert instead of full replace
            val entities = customers.map { it.toEntity() }
            if (lastSyncTime == null) {
                // Full sync: replace all
                customerDao.syncCustomers(entities)
            } else {
                // Incremental sync: upsert only
                customerDao.insertCustomers(entities)
            }

            // 8. Update last sync time
            val newSyncTime = dateTimeFormatter.get()!!.format(Date())
            apiKeyProvider.setLastSyncTime(SYNC_KEY, newSyncTime)
            Log.d(TAG, "Updated last sync time to: $newSyncTime")

            // 9. Emit success
            emit(Resource.Success(customers))
        } else {
            // API error - return cached data
            val cachedData = localCustomers.map { it.toDomain() }
            val errorMessage = when (response.code()) {
                401 -> "Authentication failed. Please check your API key."
                403 -> "Access denied. Your API key doesn't have permission."
                404 -> "Endpoint not found. Please check the base URL."
                500 -> "Odoo server error. Please try again later."
                else -> "Failed to sync: ${response.message()}"
            }
            emit(Resource.Error(errorMessage, data = cachedData))
        }
    }.catch { e ->
        // Log the full exception for debugging
        Log.e(TAG, "Customer sync failed", e)

        // Network or other error - return cached data
        val cachedData = customerDao.getAllCustomersOnce().map { it.toDomain() }
        val errorMessage = when {
            e.message?.contains("Unable to resolve host") == true ->
                "Network error. Please check your internet connection."
            e.message?.contains("timeout") == true ->
                "Request timed out. Please try again."
            else ->
                "Sync failed: ${e.message ?: "Unknown error"}"
        }
        emit(Resource.Error(errorMessage, data = cachedData))
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
            val createResponse = response.body()
            val createdCustomer = createResponse?.data?.firstOrNull { it.mobileUid == mobileUid }

            if (createdCustomer != null && createdCustomer.odooId != null) {
                // 8. Success: Update local record with Odoo ID
                val syncedCustomer = createdCustomer.toDomain()

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
    }.catch { e ->
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
    
    /**
     * Update customer GPS location coordinates
     *
     * Flow:
     * 1. Get customer from database
     * 2. Update with new location and set syncState = PENDING
     * 3. Save to local database
     * 4. If online and customer synced with Odoo, sync to API
     * 5. On success: update syncState to SYNCED
     * 6. On failure: location remains saved locally with PENDING state
     */
    override suspend fun updateCustomerLocation(
        customerId: Int,
        latitude: Double,
        longitude: Double
    ): Flow<Resource<Customer>> = flow {
        // 1. Emit loading
        emit(Resource.Loading())

        // 2. Get customer from database
        val existingCustomer = customerDao.getCustomerById(customerId)?.toDomain()
        if (existingCustomer == null) {
            emit(Resource.Error("Customer not found"))
            return@flow
        }

        // 3. Update customer with new location and PENDING state
        val updatedCustomer = existingCustomer.copy(
            latitude = latitude,
            longitude = longitude,
            syncState = SyncState.PENDING,
            lastModified = Date()
        )

        // 4. Save to local database
        customerDao.updateCustomer(updatedCustomer.toEntity())
        Log.d(TAG, "Customer location saved locally: customerId=$customerId, lat=$latitude, lng=$longitude")

        // 5. Get API key
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(Resource.Success(updatedCustomer, "Location saved locally. Will sync when online."))
            return@flow
        }

        // 6. Only sync to Odoo if customer has been synced (has positive ID or mobile_uid)
        if (updatedCustomer.id <= 0 && updatedCustomer.mobileUid == null) {
            emit(Resource.Success(updatedCustomer, "Location saved locally. Customer not yet synced."))
            return@flow
        }

        // 7. Sync location to Odoo API
        try {
            // For customers with Odoo ID, we use the update endpoint
            // For now, we'll need to use the create/update endpoint with mobile_uid
            val request = updatedCustomer.toRequest()
            val response = if (updatedCustomer.mobileUid != null) {
                // Use create endpoint which handles upserts by mobile_uid
                apiService.createCustomers(apiKey, listOf(request))
            } else {
                // Customer from Odoo (no mobile_uid) - use create which will update by ID
                apiService.createCustomers(apiKey, listOf(request))
            }

            if (response.isSuccessful) {
                // 8. Success: Update syncState to SYNCED
                val syncedCustomer = updatedCustomer.copy(syncState = SyncState.SYNCED)
                customerDao.updateCustomer(syncedCustomer.toEntity())

                Log.d(TAG, "Customer location synced to Odoo: customerId=$customerId")
                emit(Resource.Success(syncedCustomer, "Location captured and synced successfully"))
            } else {
                // API error - location remains saved locally with PENDING state
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed. Location saved locally."
                    403 -> "Access denied. Location saved locally."
                    404 -> "Endpoint not found. Location saved locally."
                    500 -> "Odoo server error. Location saved locally."
                    else -> "Sync failed. Location saved locally."
                }
                Log.w(TAG, "Customer location sync failed: $errorMessage")
                emit(Resource.Success(updatedCustomer, errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Customer location sync exception", e)

            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "Network error. Location saved locally."
                e.message?.contains("timeout") == true ->
                    "Request timed out. Location saved locally."
                else ->
                    "Sync failed. Location saved locally."
            }
            emit(Resource.Success(updatedCustomer, errorMessage))
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

    /**
     * Initialize cache from stored settings
     * Call this during app startup to ensure cached values match persisted values
     */
    suspend fun initializeCache()

    /**
     * Get last sync timestamp for a specific entity type
     * Returns ISO 8601 formatted string or null if never synced
     */
    suspend fun getLastSyncTime(entityType: String): String?

    /**
     * Set last sync timestamp for a specific entity type
     * Pass null to clear the sync time
     */
    suspend fun setLastSyncTime(entityType: String, timestamp: String?)
}
