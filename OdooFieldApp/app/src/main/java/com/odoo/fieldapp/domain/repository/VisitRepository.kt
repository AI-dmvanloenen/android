package com.odoo.fieldapp.domain.repository

import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Visit
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Visit operations
 *
 * This defines the contract for data operations.
 * The implementation will handle both local (Room) and remote (API) data sources.
 */
interface VisitRepository {

    /**
     * Get all visits from local database as a Flow
     * UI will automatically update when data changes
     */
    fun getVisits(): Flow<List<Visit>>

    /**
     * Get visits for a specific customer from local database as a Flow
     * Critical for customer detail screen
     * UI will automatically update when data changes
     */
    fun getVisitsByCustomer(customerId: Int): Flow<List<Visit>>

    /**
     * Get a single visit by ID (Odoo record ID) from local database
     */
    suspend fun getVisitById(visitId: Int): Visit?

    /**
     * Sync visits from Odoo API to local database
     *
     * @return Resource wrapper with loading/success/error states
     */
    suspend fun syncVisitsFromOdoo(): Flow<Resource<List<Visit>>>

    /**
     * Create a new visit and sync to Odoo
     *
     * @param visit The visit to create (id will be ignored, mobileUid will be generated)
     * @return Flow emitting Loading, then Success (with created visit including Odoo ID) or Error
     */
    suspend fun createVisit(visit: Visit): Flow<Resource<Visit>>
}
