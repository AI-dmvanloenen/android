package com.odoo.fieldapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore extension property
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Implementation of ApiKeyProvider using DataStore
 *
 * DataStore is a modern replacement for SharedPreferences
 * It's safer and supports coroutines
 */
@Singleton
class ApiKeyProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ApiKeyProvider {

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val SERVER_URL = stringPreferencesKey("server_url")

        // Sync timestamp keys for incremental sync
        private fun lastSyncKey(entityType: String) = stringPreferencesKey("last_sync_$entityType")
    }

    /**
     * Cached base URL for synchronous access (used by interceptor)
     * Volatile ensures visibility across threads
     * Empty string when not configured - requests will fail until user sets URL
     */
    @Volatile
    private var cachedBaseUrl: String = ""
    
    override suspend fun getApiKey(): String? {
        return context.dataStore.data
            .map { preferences ->
                preferences[API_KEY]
                    ?.replace("\n", "")
                    ?.replace("\r", "")
                    ?.trim()
            }
            .first()
    }

    override suspend fun setApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
                .replace("\n", "")
                .replace("\r", "")
                .trim()
        }
    }
    
    override suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }
    
    override suspend fun getServerUrl(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[SERVER_URL] }
            .first()
    }

    override suspend fun setServerUrl(serverUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = serverUrl
        }
        // Update cache immediately when URL changes
        cachedBaseUrl = buildBaseUrl(serverUrl)
    }

    /**
     * Build the base URL from the server URL
     * Adds https:// if not present and ensures trailing slash
     * Updates the cache for synchronous access
     */
    suspend fun getBaseUrl(): String {
        val url = getServerUrl()
        val baseUrl = buildBaseUrl(url)
        cachedBaseUrl = baseUrl
        return baseUrl
    }

    /**
     * Get cached base URL synchronously (for use in interceptors)
     * This avoids blocking the network thread
     */
    override fun getCachedBaseUrl(): String = cachedBaseUrl

    /**
     * Initialize cache from stored settings
     * Call this during app startup
     */
    override suspend fun initializeCache() {
        val url = getServerUrl()
        cachedBaseUrl = buildBaseUrl(url)
    }

    /**
     * Build base URL from server URL string
     * Returns empty string if URL not configured
     */
    private fun buildBaseUrl(url: String?): String {
        if (url.isNullOrBlank()) {
            return ""
        }

        // Add https:// if no protocol specified
        val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        // Ensure trailing slash
        return if (fullUrl.endsWith("/")) fullUrl else "$fullUrl/"
    }

    /**
     * Get last sync timestamp for a specific entity type
     * Returns ISO 8601 formatted string or null if never synced
     */
    override suspend fun getLastSyncTime(entityType: String): String? {
        return context.dataStore.data
            .map { preferences -> preferences[lastSyncKey(entityType)] }
            .first()
    }

    /**
     * Set last sync timestamp for a specific entity type
     * Pass null to clear the sync time
     */
    override suspend fun setLastSyncTime(entityType: String, timestamp: String?) {
        context.dataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(lastSyncKey(entityType))
            } else {
                preferences[lastSyncKey(entityType)] = timestamp
            }
        }
    }
}
