package com.odoo.fieldapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.data.sync.SyncQueueManager
import com.odoo.fieldapp.domain.connectivity.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

/**
 * App-wide ViewModel for managing connectivity state and sync status
 *
 * Provides reactive state for:
 * - Network connectivity (online/offline)
 * - Pending sync queue count
 * - Last successful sync time
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
    syncQueueManager: SyncQueueManager
) : ViewModel() {

    /**
     * Network connectivity state
     * True when device is online, false when offline
     */
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkMonitor.isCurrentlyOnline()
        )

    /**
     * Count of pending items in the sync queue
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
     * Update the last sync time (called after successful sync)
     */
    fun updateLastSyncTime() {
        _lastSyncTime.value = Date()
    }
}
