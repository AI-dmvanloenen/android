package com.odoo.fieldapp.data.local.dao

import androidx.room.*
import com.odoo.fieldapp.data.local.entity.DeliveryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Delivery operations
 *
 * Uses Flow for reactive database queries - UI will automatically update when data changes
 */
@Dao
interface DeliveryDao {

    /**
     * Get all deliveries as a Flow (reactive)
     * UI will automatically update when deliveries change
     */
    @Query("SELECT * FROM deliveries ORDER BY scheduledDate DESC")
    fun getAllDeliveries(): Flow<List<DeliveryEntity>>

    /**
     * Get all deliveries once (non-reactive, for loading state)
     */
    @Query("SELECT * FROM deliveries ORDER BY scheduledDate DESC")
    suspend fun getAllDeliveriesOnce(): List<DeliveryEntity>

    /**
     * Get all delivery IDs (for sync deletion check)
     */
    @Query("SELECT id FROM deliveries")
    suspend fun getAllDeliveryIds(): List<Int>

    /**
     * Get a single delivery by ID (Odoo record ID)
     */
    @Query("SELECT * FROM deliveries WHERE id = :deliveryId")
    suspend fun getDeliveryById(deliveryId: Int): DeliveryEntity?

    /**
     * Search deliveries by name
     */
    @Query("SELECT * FROM deliveries WHERE name LIKE '%' || :query || '%' ORDER BY scheduledDate DESC")
    fun searchDeliveries(query: String): Flow<List<DeliveryEntity>>

    /**
     * Get deliveries by customer ID
     */
    @Query("SELECT * FROM deliveries WHERE partnerId = :customerId ORDER BY scheduledDate DESC")
    fun getDeliveriesByCustomer(customerId: Int): Flow<List<DeliveryEntity>>

    /**
     * Get deliveries by sale order ID
     */
    @Query("SELECT * FROM deliveries WHERE saleId = :saleId ORDER BY scheduledDate DESC")
    fun getDeliveriesBySale(saleId: Int): Flow<List<DeliveryEntity>>

    /**
     * Get deliveries by sync state (useful for finding pending syncs)
     */
    @Query("SELECT * FROM deliveries WHERE syncState = :syncState")
    suspend fun getDeliveriesBySyncState(syncState: String): List<DeliveryEntity>

    /**
     * Insert a single delivery
     * OnConflictStrategy.REPLACE will update if delivery already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity)

    /**
     * Insert multiple deliveries (batch operation for sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(deliveries: List<DeliveryEntity>)

    /**
     * Delete deliveries not in the given set of IDs
     * Used during sync to remove records deleted on the server
     */
    @Query("DELETE FROM deliveries WHERE id NOT IN (:ids)")
    suspend fun deleteDeliveriesNotIn(ids: Set<Int>)

    /**
     * Atomic sync operation: deletes stale records and inserts new ones in a single transaction
     * Ensures database consistency if sync is interrupted
     */
    @Transaction
    suspend fun syncDeliveries(deliveries: List<DeliveryEntity>) {
        val incomingIds = deliveries.map { it.id }.toSet()
        deleteDeliveriesNotIn(incomingIds)
        insertDeliveries(deliveries)
    }

    /**
     * Update a delivery
     */
    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)

    /**
     * Delete a delivery
     */
    @Delete
    suspend fun deleteDelivery(delivery: DeliveryEntity)

    /**
     * Delete all deliveries (useful for full re-sync)
     */
    @Query("DELETE FROM deliveries")
    suspend fun deleteAllDeliveries()

    /**
     * Get count of deliveries (useful for UI)
     */
    @Query("SELECT COUNT(*) FROM deliveries")
    fun getDeliveryCount(): Flow<Int>
}
