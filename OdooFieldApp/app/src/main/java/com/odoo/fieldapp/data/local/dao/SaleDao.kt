package com.odoo.fieldapp.data.local.dao

import androidx.room.*
import com.odoo.fieldapp.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Sale operations
 *
 * Uses Flow for reactive database queries - UI will automatically update when data changes
 */
@Dao
interface SaleDao {

    /**
     * Get all sales as a Flow (reactive)
     * UI will automatically update when sales change
     */
    @Query("SELECT * FROM sales ORDER BY dateOrder DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    /**
     * Get all sales once (non-reactive, for loading state)
     */
    @Query("SELECT * FROM sales ORDER BY dateOrder DESC")
    suspend fun getAllSalesOnce(): List<SaleEntity>

    /**
     * Get all sale IDs (for sync deletion check)
     */
    @Query("SELECT id FROM sales")
    suspend fun getAllSaleIds(): List<Int>

    /**
     * Get a single sale by ID (Odoo record ID)
     */
    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: Int): SaleEntity?

    /**
     * Search sales by name
     */
    @Query("SELECT * FROM sales WHERE name LIKE '%' || :query || '%' ORDER BY dateOrder DESC")
    fun searchSales(query: String): Flow<List<SaleEntity>>

    /**
     * Get sales by customer ID
     */
    @Query("SELECT * FROM sales WHERE partnerId = :customerId ORDER BY dateOrder DESC")
    fun getSalesByCustomer(customerId: Int): Flow<List<SaleEntity>>

    /**
     * Get sales by sync state (useful for finding pending syncs)
     */
    @Query("SELECT * FROM sales WHERE syncState = :syncState")
    suspend fun getSalesBySyncState(syncState: String): List<SaleEntity>

    /**
     * Insert a single sale
     * OnConflictStrategy.REPLACE will update if sale already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    /**
     * Insert multiple sales (batch operation for sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleEntity>)

    /**
     * Delete sales not in the given set of IDs
     * Used during sync to remove records deleted on the server
     */
    @Query("DELETE FROM sales WHERE id NOT IN (:ids)")
    suspend fun deleteSalesNotIn(ids: Set<Int>)

    /**
     * Atomic sync operation: deletes stale records and inserts new ones in a single transaction
     * Ensures database consistency if sync is interrupted
     */
    @Transaction
    suspend fun syncSales(sales: List<SaleEntity>) {
        val incomingIds = sales.map { it.id }.toSet()
        deleteSalesNotIn(incomingIds)
        insertSales(sales)
    }

    /**
     * Update a sale
     */
    @Update
    suspend fun updateSale(sale: SaleEntity)

    /**
     * Delete a sale
     */
    @Delete
    suspend fun deleteSale(sale: SaleEntity)

    /**
     * Delete all sales (useful for full re-sync)
     */
    @Query("DELETE FROM sales")
    suspend fun deleteAllSales()

    /**
     * Get count of sales (useful for UI)
     */
    @Query("SELECT COUNT(*) FROM sales")
    fun getSaleCount(): Flow<Int>

    /**
     * Count sales with sync errors
     */
    @Query("SELECT COUNT(*) FROM sales WHERE syncState = 'ERROR'")
    fun countSaleSyncErrors(): Flow<Int>

    /**
     * Get minimum sale ID (for generating temporary negative IDs)
     */
    @Query("SELECT MIN(id) FROM sales")
    suspend fun getMinSaleId(): Int?

    /**
     * Delete a sale by ID
     */
    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSaleById(saleId: Int)
}
