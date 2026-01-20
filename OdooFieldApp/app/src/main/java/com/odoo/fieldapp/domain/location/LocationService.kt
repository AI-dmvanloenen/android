package com.odoo.fieldapp.domain.location

import com.odoo.fieldapp.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Represents a GPS location with metadata
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,      // Accuracy in meters
    val timestamp: Date = Date()       // When the location was captured
)

/**
 * Service interface for GPS location operations
 *
 * Provides methods to:
 * - Get the current device location
 * - Check location permission status
 * - Check if location services are enabled
 */
interface LocationService {

    /**
     * Get the current GPS location of the device
     *
     * Returns a Flow that emits Resource states:
     * - Loading: Location request is in progress
     * - Success: Location was obtained successfully
     * - Error: Failed to get location (permission denied, GPS disabled, timeout, etc.)
     *
     * Requires ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission.
     * Will return Error if permission is not granted or GPS is disabled.
     */
    suspend fun getCurrentLocation(): Flow<Resource<Location>>

    /**
     * Check if location permissions are granted
     *
     * Returns true if either ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is granted
     */
    fun hasLocationPermission(): Boolean

    /**
     * Check if location services (GPS) are enabled on the device
     *
     * Returns true if location services are enabled in device settings
     */
    fun isLocationEnabled(): Boolean
}
