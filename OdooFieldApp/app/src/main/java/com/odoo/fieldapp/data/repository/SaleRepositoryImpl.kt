package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.data.local.dao.SaleLineDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    private val saleLineDao: SaleLineDao,
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
     * Note: Lines are not loaded for list view for performance
     */
    override fun getSales(): Flow<List<Sale>> {
        return saleDao.getAllSales()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get a single sale by ID (Odoo record ID)
     * Includes loading the order lines from the database
     */
    override suspend fun getSaleById(saleId: Int): Sale? {
        val saleEntity = saleDao.getSaleById(saleId) ?: return null
        val lineEntities = saleLineDao.getLinesForSaleOnce(saleId)
        Log.d(TAG, "getSaleById($saleId): Found ${lineEntities.size} lines in database")
        val lines = lineEntities.map { it.toDomain() }
        return saleEntity.toDomain().copy(lines = lines)
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
        // 1. Emit loading state with current local data
        val localSales = saleDao.getAllSalesOnce()
        emit(Resource.Loading(data = localSales.map { it.toDomain() }))

        // 2. Get API key
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(Resource.Error("API key not configured. Please add your API key in settings."))
            return@flow
        }

        // 3. Always do full sync for sales (no incremental sync for now)
        Log.d(TAG, "Performing full sales sync")

        // 4. Fetch all sales from Odoo API
        val response = apiService.getSales(apiKey, null)

        if (response.isSuccessful) {
            val paginatedResponse = response.body()
            val saleResponses = paginatedResponse?.data ?: emptyList()

            // 5. Filter out records with null IDs (prevents primary key conflicts)
            val validResponses = saleResponses.filter { it.id != null }

            // 6. Convert API responses to domain models
            val sales = validResponses.map { it.toDomain() }

            // Debug: Log how many lines were received from API
            val totalApiLines = sales.sumOf { it.lines.size }
            Log.d(TAG, "API returned ${sales.size} sales with $totalApiLines total lines")
            sales.forEach { sale ->
                Log.d(TAG, "Sale ${sale.name} (id=${sale.id}) has ${sale.lines.size} lines from API")
            }

            // 7. Resolve partner names from local database
            val enrichedSales = sales.map { sale ->
                val partnerName = sale.partnerId?.let { customerId ->
                    customerDao.getCustomerById(customerId)?.name
                }
                sale.copy(partnerName = partnerName)
            }

            // 8. Save sales to local database (full replace)
            val entities = enrichedSales.map { it.toEntity() }
            saleDao.syncSales(entities)
            Log.d(TAG, "Synced ${entities.size} sales")

            // 9. Sync order lines for each sale
            var totalLines = 0
            for (sale in enrichedSales) {
                val lineEntities = sale.lines.map { it.toEntity(sale.id) }
                Log.d(TAG, "Saving ${lineEntities.size} lines for sale ${sale.name} (id=${sale.id})")
                saleLineDao.syncLinesForSale(sale.id, lineEntities)
                totalLines += lineEntities.size

                // Verify lines were saved
                val savedLines = saleLineDao.getLinesForSaleOnce(sale.id)
                Log.d(TAG, "Verified ${savedLines.size} lines saved for sale ${sale.name}")
            }
            Log.d(TAG, "Synced $totalLines sale lines total")

            // 10. Emit success
            emit(Resource.Success(enrichedSales))
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
    }.catch { e ->
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
