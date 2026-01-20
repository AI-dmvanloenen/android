package com.odoo.fieldapp.data.local.dao

import androidx.room.*
import com.odoo.fieldapp.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Visit operations
 *
 * Uses Flow for reactive database queries - UI will automatically update when data changes
 */
@Dao
interface VisitDao {

    /**
     * Get all visits as a Flow (reactive)
     * UI will automatically update when visits change
     */
    @Query("SELECT * FROM visits ORDER BY visitDatetime DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    /**
     * Get visits by customer ID (reactive)
     * Critical for customer detail screen
     */
    @Query("SELECT * FROM visits WHERE partnerId = :customerId ORDER BY visitDatetime DESC")
    fun getVisitsByCustomer(customerId: Int): Flow<List<VisitEntity>>

    /**
     * Get a single visit by ID (Odoo record ID)
     */
    @Query("SELECT * FROM visits WHERE id = :visitId")
    suspend fun getVisitById(visitId: Int): VisitEntity?

    /**
     * Get visits by sync state (useful for finding pending syncs)
     */
    @Query("SELECT * FROM visits WHERE syncState = :syncState")
    suspend fun getVisitsBySyncState(syncState: String): List<VisitEntity>

    /**
     * Insert a single visit
     * OnConflictStrategy.REPLACE will update if visit already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    /**
     * Insert multiple visits (batch operation for sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)

    /**
     * Delete visits not in the given set of IDs
     * Used during sync to remove records deleted on the server
     */
    @Query("DELETE FROM visits WHERE id NOT IN (:ids)")
    suspend fun deleteVisitsNotIn(ids: Set<Int>)

    /**
     * Atomic sync operation: deletes stale records and inserts new ones in a single transaction
     * Ensures database consistency if sync is interrupted
     */
    @Transaction
    suspend fun syncVisits(visits: List<VisitEntity>) {
        val incomingIds = visits.map { it.id }.toSet()
        deleteVisitsNotIn(incomingIds)
        insertVisits(visits)
    }

    /**
     * Delete a visit by ID (useful for cleaning up temporary IDs after sync)
     */
    @Query("DELETE FROM visits WHERE id = :visitId")
    suspend fun deleteVisitById(visitId: Int)

    /**
     * Update a visit
     */
    @Update
    suspend fun updateVisit(visit: VisitEntity)

    /**
     * Delete a visit
     */
    @Delete
    suspend fun deleteVisit(visit: VisitEntity)

    /**
     * Delete all visits (useful for full re-sync)
     */
    @Query("DELETE FROM visits")
    suspend fun deleteAllVisits()

    /**
     * Get count of visits (useful for UI)
     */
    @Query("SELECT COUNT(*) FROM visits")
    fun getVisitCount(): Flow<Int>

    /**
     * Get count of visits for a specific customer
     */
    @Query("SELECT COUNT(*) FROM visits WHERE partnerId = :customerId")
    fun getVisitCountByCustomer(customerId: Int): Flow<Int>

    /**
     * Get minimum visit ID (for generating temporary negative IDs)
     */
    @Query("SELECT MIN(id) FROM visits")
    suspend fun getMinVisitId(): Int?
}
