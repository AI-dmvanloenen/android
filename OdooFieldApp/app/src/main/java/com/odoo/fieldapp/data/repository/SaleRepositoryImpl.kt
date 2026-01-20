package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.data.local.dao.SaleLineDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.data.remote.mapper.toRequest
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
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
            val cachedData = localSales.map { it.toDomain() }
            emit(Resource.Error("API key not configured. Please add your API key in settings.", data = cachedData))
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
            val totalApiLines = sales.sumOf { it.lines.size }
            Log.d(TAG, "Fetched ${sales.size} sales with $totalApiLines lines from API")

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

            // 9. Sync order lines for each sale
            var totalLines = 0
            for (sale in enrichedSales) {
                val lineEntities = sale.lines.map { it.toEntity(sale.id) }
                saleLineDao.syncLinesForSale(sale.id, lineEntities)
                totalLines += lineEntities.size
            }
            Log.d(TAG, "Synced ${entities.size} sales with $totalLines lines")

            // 10. Emit success
            emit(Resource.Success(enrichedSales))
        } else {
            // API error - return cached data
            val cachedData = localSales.map { it.toDomain() }
            val errorMessage = when (response.code()) {
                401 -> "Authentication failed. Please check your API key."
                403 -> "Access denied. Your API key doesn't have permission."
                404 -> "Sales endpoint not found. Please check the server configuration."
                500 -> "Odoo server error. Please try again later."
                else -> "Failed to sync sales: ${response.message()}"
            }
            emit(Resource.Error(errorMessage, data = cachedData))
        }
    }.catch { e ->
        // Log the full exception for debugging
        Log.e(TAG, "Sale sync failed", e)

        // Network or other error - return cached data
        val cachedData = saleDao.getAllSalesOnce().map { it.toDomain() }
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
     * Create a new sale order and sync to Odoo
     *
     * Flow:
     * 1. Generate UUID for mobileUid
     * 2. Save locally with temporary negative ID and syncState = PENDING
     * 3. Call API with Bearer token
     * 4. On success: update local record with Odoo ID, set SYNCED
     * 5. On failure: keep PENDING for retry, emit error
     */
    override suspend fun createSale(sale: Sale): Flow<Resource<Sale>> = flow {
        // 1. Emit loading
        emit(Resource.Loading())

        // 2. Generate UUID for mobileUid
        val mobileUid = UUID.randomUUID().toString()

        // 3. Generate temporary negative ID for local storage
        val minId = saleDao.getMinSaleId() ?: 0
        val tempId = if (minId >= 0) -1 else minId - 1

        // 4. Resolve partner name for local display
        val partnerName = sale.partnerId?.let { customerId ->
            customerDao.getCustomerById(customerId)?.name
        }

        // 5. Create sale with PENDING state
        val pendingSale = sale.copy(
            id = tempId,
            mobileUid = mobileUid,
            partnerName = partnerName,
            syncState = SyncState.PENDING,
            lastModified = Date()
        )

        // 6. Save locally (both sale and lines)
        saleDao.insertSale(pendingSale.toEntity())
        if (pendingSale.lines.isNotEmpty()) {
            val lineEntities = pendingSale.lines.mapIndexed { index, line ->
                // Use negative IDs for local lines to avoid conflicts with Odoo IDs
                line.copy(id = tempId * 1000 - index).toEntity(tempId)
            }
            saleLineDao.insertLines(lineEntities)
        }
        Log.d(TAG, "Sale saved locally with tempId=$tempId, mobileUid=$mobileUid, lines=${pendingSale.lines.size}")

        // 7. Get API key
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(Resource.Error("API key not configured. Sale saved locally.", data = pendingSale))
            return@flow
        }

        // 8. Call API to create sale
        val request = pendingSale.toRequest()
        val response = apiService.createSales(apiKey, listOf(request))

        if (response.isSuccessful) {
            val createResponse = response.body()
            val createdSale = createResponse?.data?.firstOrNull { it.mobileUid == mobileUid }

            if (createdSale != null && createdSale.id != null) {
                // 9. Success: Update local record with Odoo ID
                val syncedSale = createdSale.toDomain().copy(
                    partnerName = partnerName,
                    syncState = SyncState.SYNCED
                )

                // Delete the temp record (CASCADE will remove lines too) and insert with real ID
                saleDao.deleteSaleById(tempId)
                saleDao.insertSale(syncedSale.toEntity())

                // Save the synced lines with real Odoo IDs
                if (syncedSale.lines.isNotEmpty()) {
                    val lineEntities = syncedSale.lines.map { it.toEntity(syncedSale.id) }
                    saleLineDao.insertLines(lineEntities)
                }

                Log.d(TAG, "Sale synced successfully, odooId=${syncedSale.id}, lines=${syncedSale.lines.size}")
                emit(Resource.Success(syncedSale))
            } else {
                // API returned success but no matching sale
                Log.w(TAG, "API response didn't include created sale")
                emit(Resource.Error("Sale created but response incomplete", data = pendingSale))
            }
        } else {
            // API error - sale remains in PENDING state
            val errorMessage = when (response.code()) {
                401 -> "Authentication failed. Please check your API key."
                403 -> "Access denied. Your API key doesn't have permission."
                404 -> "Endpoint not found. Please check the base URL."
                500 -> "Odoo server error. Please try again later."
                else -> "Failed to sync: ${response.message()}"
            }
            Log.e(TAG, "Sale creation API failed: $errorMessage")
            emit(Resource.Error(errorMessage, data = pendingSale))
        }
    }.catch { e ->
        Log.e(TAG, "Sale creation failed", e)

        val errorMessage = when {
            e.message?.contains("Unable to resolve host") == true ->
                "Network error. Sale saved locally."
            e.message?.contains("timeout") == true ->
                "Request timed out. Sale saved locally."
            else ->
                "Creation failed: ${e.message ?: "Unknown error"}"
        }
        emit(Resource.Error(errorMessage))
    }
}
