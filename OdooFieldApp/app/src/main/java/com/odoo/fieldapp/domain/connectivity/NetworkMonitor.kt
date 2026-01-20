package com.odoo.fieldapp.domain.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Interface for monitoring network connectivity
 *
 * Provides a reactive way to observe network state changes.
 * Used by repositories to determine when to attempt sync operations
 * and by UI to show offline indicators.
 */
interface NetworkMonitor {
    /**
     * Flow that emits true when the device has internet connectivity,
     * false when offline. Emits current state immediately upon collection.
     */
    val isOnline: Flow<Boolean>

    /**
     * Returns current connectivity state synchronously.
     * Useful for checking state before attempting network operations.
     */
    fun isCurrentlyOnline(): Boolean
}
