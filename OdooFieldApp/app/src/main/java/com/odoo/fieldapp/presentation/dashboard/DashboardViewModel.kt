package com.odoo.fieldapp.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.data.sync.SyncQueueManager
import com.odoo.fieldapp.domain.model.DashboardStats
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for Dashboard screen
 *
 * Manages dashboard statistics and sync operations
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    syncQueueManager: SyncQueueManager
) : ViewModel() {

    /**
     * Dashboard statistics (reactive)
     */
    val stats: StateFlow<DashboardStats> = dashboardRepository.getDashboardStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    /**
     * Sync all state
     */
    private val _syncState = MutableStateFlow<Resource<Unit>?>(null)
    val syncState: StateFlow<Resource<Unit>?> = _syncState.asStateFlow()

    /**
     * Count of pending sync queue items
     */
    val pendingSyncCount: StateFlow<Int> = syncQueueManager.getPendingCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * Last successful sync timestamp
     */
    private val _lastSyncTime = MutableStateFlow<Date?>(null)
    val lastSyncTime: StateFlow<Date?> = _lastSyncTime.asStateFlow()

    /**
     * Sync all entities from Odoo
     */
    fun syncAll() {
        viewModelScope.launch {
            _syncState.value = Resource.Loading()
            val result = dashboardRepository.syncAll()
            _syncState.value = result

            // Update last sync time on success
            if (result is Resource.Success) {
                _lastSyncTime.value = Date()
            }
        }
    }

    /**
     * Clear sync state (dismiss messages)
     */
    fun clearSyncState() {
        _syncState.value = null
    }
}
