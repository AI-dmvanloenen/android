package com.odoo.fieldapp.data.remote

import com.odoo.fieldapp.data.repository.ApiKeyProvider
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that dynamically rewrites the base URL
 * based on the server URL stored in settings.
 *
 * Supports any custom server URL (e.g., mycompany.odoo.com, custom.ownserver.com)
 *
 * Uses cached base URL to avoid blocking the network thread.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // Get the cached base URL (non-blocking)
        val baseUrl = apiKeyProvider.getCachedBaseUrl()
        val newBaseUrl = baseUrl.toHttpUrlOrNull()

        if (newBaseUrl == null) {
            // If we can't parse the URL, proceed with original
            return chain.proceed(originalRequest)
        }

        // Rebuild the URL with the new base
        val newUrl = originalUrl.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
