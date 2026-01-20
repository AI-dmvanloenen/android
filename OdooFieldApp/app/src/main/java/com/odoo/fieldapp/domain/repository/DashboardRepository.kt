package com.odoo.fieldapp.domain.repository

import com.odoo.fieldapp.domain.model.DashboardStats
import com.odoo.fieldapp.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Dashboard operations
 */
interface DashboardRepository {
    /**
     * Get dashboard statistics as a Flow
     * Emits new values whenever any count changes
     */
    fun getDashboardStats(): Flow<DashboardStats>

    /**
     * Sync all entities (customers, sales, deliveries, payments)
     * Returns Resource to indicate success/failure
     */
    suspend fun syncAll(): Resource<Unit>
}
