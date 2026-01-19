package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.DeliveryDao
import com.odoo.fieldapp.data.local.dao.DeliveryLineDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.DeliveryLine
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DeliveryRepository
 *
 * This is the single source of truth for delivery data.
 * It coordinates between local database (Room) and remote API (Retrofit)
 */
@Singleton
class DeliveryRepositoryImpl @Inject constructor(
    private val deliveryDao: DeliveryDao,
    private val deliveryLineDao: DeliveryLineDao,
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : DeliveryRepository {

    companion object {
        private const val TAG = "DeliveryRepository"
    }

    /**
     * Get deliveries from local database
     * Returns a Flow that emits whenever the database changes
     */
    override fun getDeliveries(): Flow<List<Delivery>> {
        return deliveryDao.getAllDeliveries()
            .map { entities ->
                entities.map { entity ->
                    val lines = deliveryLineDao.getLinesForDeliveryOnce(entity.id)
                    entity.toDomain(lines.map { it.toDomain() })
                }
            }
    }

    /**
     * Get a single delivery by ID (Odoo record ID)
     */
    override suspend fun getDeliveryById(deliveryId: Int): Delivery? {
        val entity = deliveryDao.getDeliveryById(deliveryId) ?: return null
        val lines = deliveryLineDao.getLinesForDeliveryOnce(deliveryId)
        return entity.toDomain(lines.map { it.toDomain() })
    }

    /**
     * Search deliveries by name
     */
    override fun searchDeliveries(query: String): Flow<List<Delivery>> {
        return deliveryDao.searchDeliveries(query)
            .map { entities ->
                entities.map { entity ->
                    val lines = deliveryLineDao.getLinesForDeliveryOnce(entity.id)
                    entity.toDomain(lines.map { it.toDomain() })
                }
            }
    }

    /**
     * Get deliveries for a specific customer
     */
    override fun getDeliveriesByCustomer(customerId: Int): Flow<List<Delivery>> {
        return deliveryDao.getDeliveriesByCustomer(customerId)
            .map { entities ->
                entities.map { entity ->
                    val lines = deliveryLineDao.getLinesForDeliveryOnce(entity.id)
                    entity.toDomain(lines.map { it.toDomain() })
                }
            }
    }

    /**
     * Get deliveries for a specific sale order
     */
    override fun getDeliveriesBySale(saleId: Int): Flow<List<Delivery>> {
        return deliveryDao.getDeliveriesBySale(saleId)
            .map { entities ->
                entities.map { entity ->
                    val lines = deliveryLineDao.getLinesForDeliveryOnce(entity.id)
                    entity.toDomain(lines.map { it.toDomain() })
                }
            }
    }

    /**
     * Sync deliveries from Odoo API to local database
     *
     * Flow of Resource states:
     * 1. Emit Loading with existing local data
     * 2. Fetch from API
     * 3. Resolve partner names and sale names from local data
     * 4. Save to local database (in transaction)
     * 5. Emit Success with updated data
     * 6. If error, emit Error with message
     */
    override suspend fun syncDeliveriesFromOdoo(): Flow<Resource<List<Delivery>>> = flow {
        try {
            // 1. Emit loading state with current local data
            val localDeliveries = deliveryDao.getAllDeliveriesOnce()
            val localWithLines = localDeliveries.map { entity ->
                val lines = deliveryLineDao.getLinesForDeliveryOnce(entity.id)
                entity.toDomain(lines.map { it.toDomain() })
            }
            emit(Resource.Loading(data = localWithLines))

            // 2. Get API key
            val apiKey = apiKeyProvider.getApiKey()
            if (apiKey.isNullOrBlank()) {
                emit(Resource.Error("API key not configured. Please add your API key in settings."))
                return@flow
            }

            // 3. Fetch from Odoo API
            val response = apiService.getDeliveries(apiKey)

            if (response.isSuccessful) {
                val deliveryResponses = response.body() ?: emptyList()

                // 4. Filter out records with null IDs (prevents primary key conflicts)
                val validResponses = deliveryResponses.filter { it.id != null }

                // 5. Convert API responses to domain models
                val allDeliveries = validResponses.map { it.toDomain() }

                // 6. Resolve partner names and sale names from local database
                val enrichedDeliveries = allDeliveries.map { delivery ->
                    val partnerName = delivery.partnerId?.let { customerId ->
                        customerDao.getCustomerById(customerId)?.name
                    }
                    val saleName = delivery.saleId?.let { saleId ->
                        saleDao.getSaleById(saleId)?.name
                    }
                    delivery.copy(
                        partnerName = partnerName,
                        saleName = saleName
                    )
                }

                // 7. Save to local database (atomic transaction)
                val entities = enrichedDeliveries.map { it.toEntity() }
                deliveryDao.syncDeliveries(entities)

                // 8. Save delivery lines
                enrichedDeliveries.forEach { delivery ->
                    val lineEntities = delivery.lines.map { it.toEntity(delivery.id) }
                    deliveryLineDao.syncLinesForDelivery(delivery.id, lineEntities)
                }

                // 9. Emit success
                emit(Resource.Success(enrichedDeliveries))
            } else {
                // API error
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed. Please check your API key."
                    403 -> "Access denied. Your API key doesn't have permission."
                    404 -> "Deliveries endpoint not found. Please check the server configuration."
                    500 -> "Odoo server error. Please try again later."
                    else -> "Failed to sync deliveries: ${response.message()}"
                }
                emit(Resource.Error(errorMessage))
            }

        } catch (e: Exception) {
            // Log the full exception for debugging
            Log.e(TAG, "Delivery sync failed", e)

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
