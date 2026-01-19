package com.odoo.fieldapp.data.repository

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
     * 1. Emit Loading
     * 2. Fetch from API
     * 3. Save to local database
     * 4. Emit Success with updated data
     * 5. If error, emit Error with message
     */
    override suspend fun syncSalesFromOdoo(): Flow<Resource<List<Sale>>> = flow {
        try {
            // 1. Emit loading state
            emit(Resource.Loading(data = null))

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

                // 4. Convert API responses to domain models
                val sales = saleResponses.map { it.toDomain() }

                // 5. Save to local database
                val entities = sales.map { it.toEntity() }
                saleDao.insertSales(entities)

                // 6. Emit success
                emit(Resource.Success(sales))
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
