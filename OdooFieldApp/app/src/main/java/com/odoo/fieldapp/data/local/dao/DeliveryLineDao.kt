package com.odoo.fieldapp.data.local.dao

import androidx.room.*
import com.odoo.fieldapp.data.local.entity.DeliveryLineEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Delivery Line operations
 *
 * Uses Flow for reactive database queries
 */
@Dao
interface DeliveryLineDao {

    /**
     * Get lines for a specific delivery as a Flow (reactive)
     */
    @Query("SELECT * FROM delivery_lines WHERE deliveryId = :deliveryId ORDER BY id ASC")
    fun getLinesForDelivery(deliveryId: Int): Flow<List<DeliveryLineEntity>>

    /**
     * Get lines for a specific delivery once (non-reactive)
     */
    @Query("SELECT * FROM delivery_lines WHERE deliveryId = :deliveryId ORDER BY id ASC")
    suspend fun getLinesForDeliveryOnce(deliveryId: Int): List<DeliveryLineEntity>

    /**
     * Insert a single line
     * OnConflictStrategy.REPLACE will update if line already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: DeliveryLineEntity)

    /**
     * Insert multiple lines (batch operation for sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<DeliveryLineEntity>)

    /**
     * Delete all lines for a specific delivery
     */
    @Query("DELETE FROM delivery_lines WHERE deliveryId = :deliveryId")
    suspend fun deleteLinesByDeliveryId(deliveryId: Int)

    /**
     * Delete lines not in the given set of IDs for a specific delivery
     */
    @Query("DELETE FROM delivery_lines WHERE deliveryId = :deliveryId AND id NOT IN (:ids)")
    suspend fun deleteLinesNotIn(deliveryId: Int, ids: Set<Int>)

    /**
     * Atomic sync operation for lines: deletes stale lines and inserts new ones
     */
    @Transaction
    suspend fun syncLinesForDelivery(deliveryId: Int, lines: List<DeliveryLineEntity>) {
        val incomingIds = lines.map { it.id }.toSet()
        if (incomingIds.isEmpty()) {
            deleteLinesByDeliveryId(deliveryId)
        } else {
            deleteLinesNotIn(deliveryId, incomingIds)
            insertLines(lines)
        }
    }

    /**
     * Delete a line
     */
    @Delete
    suspend fun deleteLine(line: DeliveryLineEntity)

    /**
     * Delete all lines (useful for full re-sync)
     */
    @Query("DELETE FROM delivery_lines")
    suspend fun deleteAllLines()

    /**
     * Get count of lines for a delivery
     */
    @Query("SELECT COUNT(*) FROM delivery_lines WHERE deliveryId = :deliveryId")
    suspend fun getLineCount(deliveryId: Int): Int
}
