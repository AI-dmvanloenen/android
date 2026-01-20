package com.odoo.fieldapp.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.odoo.fieldapp.domain.location.Location
import com.odoo.fieldapp.domain.location.LocationService
import com.odoo.fieldapp.domain.model.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Implementation of LocationService using Google Play Services FusedLocationProviderClient
 *
 * Provides high-accuracy GPS location capture with proper permission and error handling
 */
class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(): Flow<Resource<Location>> = callbackFlow {
        // Emit loading state
        trySend(Resource.Loading())

        // Check permissions first
        if (!hasLocationPermission()) {
            trySend(Resource.Error("Location permission not granted. Please enable location permission in app settings."))
            close()
            return@callbackFlow
        }

        // Check if location services are enabled
        if (!isLocationEnabled()) {
            trySend(Resource.Error("GPS is disabled. Please enable location services in Settings."))
            close()
            return@callbackFlow
        }

        try {
            // Create a cancellation token for timeout handling
            val cancellationTokenSource = CancellationTokenSource()

            // Request current location with high accuracy and 10-second timeout
            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(10000) // 10-second timeout
                .setMaxUpdateAgeMillis(0) // Don't accept cached locations
                .build()

            // Get current location using suspendCancellableCoroutine for proper cancellation
            val androidLocation = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    locationRequest,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener { exception ->
                    continuation.resume(null)
                }

                // Cancel the request if coroutine is cancelled
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }

            // Handle null location (timeout or no location available)
            if (androidLocation == null) {
                trySend(Resource.Error("Unable to get location. Please ensure GPS has a clear signal and try again."))
                close()
                return@callbackFlow
            }

            // Convert to domain Location model
            val location = Location(
                latitude = androidLocation.latitude,
                longitude = androidLocation.longitude,
                accuracy = androidLocation.accuracy,
                timestamp = Date(androidLocation.time)
            )

            // Emit success
            trySend(Resource.Success(location))
            close()

        } catch (e: SecurityException) {
            trySend(Resource.Error("Failed to get location: Permission denied. Please grant location access in app settings."))
            close()
        } catch (e: Exception) {
            trySend(Resource.Error("Failed to get location: ${e.message ?: "Unknown error"}"))
            close()
        }

        awaitClose {
            // Cleanup if needed
        }
    }

    override fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    override fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
