package com.odoo.fieldapp.domain.model

/**
 * A generic wrapper for handling API responses and state management
 * Useful for UI to show loading, success, and error states
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T, message: String? = null) : Resource<T>(data, message)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
