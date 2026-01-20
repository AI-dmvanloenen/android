package com.odoo.fieldapp

import android.app.Application
import com.odoo.fieldapp.data.repository.ApiKeyProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class with Hilt support
 *
 * @HiltAndroidApp triggers Hilt's code generation and is required at the application level
 */
@HiltAndroidApp
class OdooFieldApplication : Application() {

    @Inject
    lateinit var apiKeyProvider: ApiKeyProvider

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Initialize cached URL from stored settings
        // This ensures the interceptor uses the correct URL immediately after app restart
        applicationScope.launch {
            apiKeyProvider.initializeCache()
        }
    }
}
