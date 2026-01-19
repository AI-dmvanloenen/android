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
}
