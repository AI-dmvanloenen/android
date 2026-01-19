package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SaleRepository
 *
 * This is the single source of truth for sale data.
 * It coordinates between local database (Room) and remote API (Retrofit)
 */
@Singleton
class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : SaleRepository {

    companion object {
        private const val TAG = "SaleRepository"
    }

    /**
     * Get sales from local database
     * Returns a Flow that emits whenever the database changes
     */
    override fun getSales(): Flow<List<Sale>> {
        return saleDao.getAllSales()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get a single sale by ID (Odoo record ID)
     */
    override suspend fun getSaleById(saleId: Int): Sale? {
        return saleDao.getSaleById(saleId)?.toDomain()
    }

    /**
     * Search sales by name
     */
    override fun searchSales(query: String): Flow<List<Sale>> {
        return saleDao.searchSales(query)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get sales for a specific customer
     */
    override fun getSalesByCustomer(customerId: Int): Flow<List<Sale>> {
        return saleDao.getSalesByCustomer(customerId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Sync sales from Odoo API to local database
     *
     * Flow of Resource states:
     * 1. Emit Loading with existing local data
     * 2. Fetch from API
     * 3. Save to local database (in transaction)
     * 4. Emit Success with updated data
     * 5. If error, emit Error with message
     */
    override suspend fun syncSalesFromOdoo(): Flow<Resource<List<Sale>>> = flow {
        try {
            // 1. Emit loading state with current local data
            val localSales = saleDao.getAllSalesOnce()
            emit(Resource.Loading(data = localSales.map { it.toDomain() }))

            // 2. Get API key
            val apiKey = apiKeyProvider.getApiKey()
            if (apiKey.isNullOrBlank()) {
                emit(Resource.Error("API key not configured. Please add your API key in settings."))
                return@flow
            }

            // 3. Fetch from Odoo API
            val response = apiService.getSales(apiKey)

            if (response.isSuccessful) {
                val saleResponses = response.body() ?: emptyList()

                // 4. Filter out records with null IDs (prevents primary key conflicts)
                val validResponses = saleResponses.filter { it.id != null }

                // 5. Convert API responses to domain models
                val allSales = validResponses.map { it.toDomain() }

                // 6. Filter out sales where customer doesn't exist locally
                val validSales = allSales.filter { sale ->
                    sale.partnerId == null || customerDao.getCustomerById(sale.partnerId) != null
                }

                // 7. Save to local database (atomic transaction, only sales with valid customers)
                val entities = validSales.map { it.toEntity() }
                saleDao.syncSales(entities)

                // 8. Emit success
                emit(Resource.Success(validSales))
            } else {
                // API error
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed. Please check your API key."
                    403 -> "Access denied. Your API key doesn't have permission."
                    404 -> "Sales endpoint not found. Please check the server configuration."
                    500 -> "Odoo server error. Please try again later."
                    else -> "Failed to sync sales: ${response.message()}"
                }
                emit(Resource.Error(errorMessage))
            }

        } catch (e: Exception) {
            // Log the full exception for debugging
            Log.e(TAG, "Sale sync failed", e)

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
}
