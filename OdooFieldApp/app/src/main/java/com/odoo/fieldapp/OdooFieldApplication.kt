package com.odoo.fieldapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.odoo.fieldapp.data.repository.ApiKeyProvider
import com.odoo.fieldapp.data.sync.SyncScheduler
import com.odoo.fieldapp.domain.connectivity.NetworkMonitor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class with Hilt support
 *
 * @HiltAndroidApp triggers Hilt's code generation and is required at the application level
 */
@HiltAndroidApp
class OdooFieldApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "OdooFieldApplication"
    }

    @Inject
    lateinit var apiKeyProvider: ApiKeyProvider

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize cached URL from stored settings
        // This ensures the interceptor uses the correct URL immediately after app restart
        applicationScope.launch {
            apiKeyProvider.initializeCache()
        }

        // Schedule periodic background sync
        syncScheduler.schedulePeriodicSync()

        // Monitor network connectivity and trigger sync on reconnection
        observeNetworkConnectivity()
    }

    /**
     * Observes network connectivity changes and triggers sync when coming back online
     */
    private fun observeNetworkConnectivity() {
        applicationScope.launch {
            var wasOffline = !networkMonitor.isCurrentlyOnline()

            networkMonitor.isOnline
                .distinctUntilChanged()
                .collect { isOnline ->
                    Log.d(TAG, "Network state changed: isOnline=$isOnline, wasOffline=$wasOffline")

                    if (isOnline && wasOffline) {
                        // Device just came back online, trigger immediate sync
                        Log.d(TAG, "Device back online, triggering immediate sync")
                        syncScheduler.triggerImmediateSync()
                    }

                    wasOffline = !isOnline
                }
        }
    }
}
