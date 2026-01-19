package com.odoo.fieldapp.domain.repository

import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Delivery operations
 *
 * This defines the contract for data operations.
 * The implementation will handle both local (Room) and remote (API) data sources.
 */
interface DeliveryRepository {

    /**
     * Get all deliveries from local database as a Flow
     * UI will automatically update when data changes
     */
    fun getDeliveries(): Flow<List<Delivery>>

    /**
     * Get a single delivery by ID (Odoo record ID) from local database
     */
    suspend fun getDeliveryById(deliveryId: Int): Delivery?

    /**
     * Search deliveries by name
     */
    fun searchDeliveries(query: String): Flow<List<Delivery>>

    /**
     * Get deliveries for a specific customer
     */
    fun getDeliveriesByCustomer(customerId: Int): Flow<List<Delivery>>

    /**
     * Get deliveries for a specific sale order
     */
    fun getDeliveriesBySale(saleId: Int): Flow<List<Delivery>>

    /**
     * Sync deliveries from Odoo API to local database
     *
     * @return Resource wrapper with loading/success/error states
     */
    suspend fun syncDeliveriesFromOdoo(): Flow<Resource<List<Delivery>>>
}
