package com.odoo.fieldapp.data.local.dao

import androidx.room.*
import com.odoo.fieldapp.data.local.entity.SaleLineEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Sale Line operations
 *
 * Uses Flow for reactive database queries
 */
@Dao
interface SaleLineDao {

    /**
     * Get lines for a specific sale as a Flow (reactive)
     */
    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id ASC")
    fun getLinesForSale(saleId: Int): Flow<List<SaleLineEntity>>

    /**
     * Get lines for a specific sale once (non-reactive)
     */
    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id ASC")
    suspend fun getLinesForSaleOnce(saleId: Int): List<SaleLineEntity>

    /**
     * Insert a single line
     * OnConflictStrategy.REPLACE will update if line already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: SaleLineEntity)

    /**
     * Insert multiple lines (batch operation for sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<SaleLineEntity>)

    /**
     * Delete all lines for a specific sale
     */
    @Query("DELETE FROM sale_lines WHERE saleId = :saleId")
    suspend fun deleteLinesBySaleId(saleId: Int)

    /**
     * Delete lines not in the given set of IDs for a specific sale
     */
    @Query("DELETE FROM sale_lines WHERE saleId = :saleId AND id NOT IN (:ids)")
    suspend fun deleteLinesNotIn(saleId: Int, ids: Set<Int>)

    /**
     * Atomic sync operation for lines: deletes stale lines and inserts new ones
     */
    @Transaction
    suspend fun syncLinesForSale(saleId: Int, lines: List<SaleLineEntity>) {
        val incomingIds = lines.map { it.id }.toSet()
        if (incomingIds.isEmpty()) {
            deleteLinesBySaleId(saleId)
        } else {
            deleteLinesNotIn(saleId, incomingIds)
            insertLines(lines)
        }
    }

    /**
     * Delete a line
     */
    @Delete
    suspend fun deleteLine(line: SaleLineEntity)

    /**
     * Delete all lines (useful for full re-sync)
     */
    @Query("DELETE FROM sale_lines")
    suspend fun deleteAllLines()

    /**
     * Get count of lines for a sale
     */
    @Query("SELECT COUNT(*) FROM sale_lines WHERE saleId = :saleId")
    suspend fun getLineCount(saleId: Int): Int
}
