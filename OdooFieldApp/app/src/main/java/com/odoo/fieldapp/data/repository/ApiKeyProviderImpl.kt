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
    }
    
    override suspend fun getApiKey(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[API_KEY] }
            .first()
    }
    
    override suspend fun setApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
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
    }

    /**
     * Build the base URL from the server URL
     * Adds https:// if not present and ensures trailing slash
     */
    suspend fun getBaseUrl(): String {
        val url = getServerUrl()
        if (url.isNullOrBlank()) {
            return "https://test.odoo.com/"  // Fallback
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
}
