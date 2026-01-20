package com.odoo.fieldapp.domain.repository

import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Sale operations
 *
 * This defines the contract for data operations.
 * The implementation will handle both local (Room) and remote (API) data sources.
 */
interface SaleRepository {

    /**
     * Get all sales from local database as a Flow
     * UI will automatically update when data changes
     */
    fun getSales(): Flow<List<Sale>>

    /**
     * Get a single sale by ID (Odoo record ID) from local database
     */
    suspend fun getSaleById(saleId: Int): Sale?

    /**
     * Search sales by name
     */
    fun searchSales(query: String): Flow<List<Sale>>

    /**
     * Get sales for a specific customer
     */
    fun getSalesByCustomer(customerId: Int): Flow<List<Sale>>

    /**
     * Sync sales from Odoo API to local database
     *
     * @return Resource wrapper with loading/success/error states
     */
    suspend fun syncSalesFromOdoo(): Flow<Resource<List<Sale>>>

    /**
     * Create a new sale order and sync to Odoo
     *
     * @param sale The sale to create (id will be ignored, mobileUid will be generated)
     * @return Flow emitting Loading, then Success (with created sale including Odoo ID) or Error
     */
    suspend fun createSale(sale: Sale): Flow<Resource<Sale>>
}
