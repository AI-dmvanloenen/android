# Feature: GPS Location Tracking

**Purpose:** Capture GPS coordinates for outlets and during visits/deliveries.

---

## Data Model Changes

### Customer Model Additions
```kotlin
// Add to domain/model/Customer.kt
val latitude: Double?,
val longitude: Double?,
val dateLocalization: String?
```

### Visit Model Additions
```kotlin
// Add to domain/model/Visit.kt
val shopLatitude: Double?,
val shopLongitude: Double?
```

### Delivery Model Additions
```kotlin
// Add to domain/model/Delivery.kt
val pickingLatitude: Double?,
val pickingLongitude: Double?
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `util/LocationHelper.kt` | Location permission handling + FusedLocationProvider wrapper |

---

## Files to Modify

| File | Change |
|------|--------|
| `domain/model/Customer.kt` | Add latitude, longitude, dateLocalization fields |
| `domain/model/Visit.kt` | Add shopLatitude, shopLongitude fields |
| `domain/model/Delivery.kt` | Add pickingLatitude, pickingLongitude fields |
| `data/local/entity/CustomerEntity.kt` | Add columns + update mappings |
| `data/local/entity/VisitEntity.kt` | Add columns |
| `data/local/entity/DeliveryEntity.kt` | Add columns |
| `data/local/OdooDatabase.kt` | Add migration for new columns |
| `data/remote/dto/CustomerDto.kt` | Add location fields |
| `data/remote/dto/VisitDto.kt` | Add location fields |
| `data/remote/dto/DeliveryDto.kt` | Add location fields |
| `AndroidManifest.xml` | Add location permissions |
| `presentation/customer/CustomerCreateScreen.kt` | Add "Capture Location" button |
| `presentation/customer/CustomerDetailScreen.kt` | Display coordinates |
| `presentation/visit/VisitDetailScreen.kt` | Auto-capture location on visit start |

---

## Permissions

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

## Dependencies

### build.gradle (app)
```kotlin
implementation("com.google.android.gms:play-services-location:21.0.1")
```

---

## LocationHelper Implementation

```kotlin
// util/LocationHelper.kt
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getAccurateLocation(): Location? {
        if (!hasLocationPermission()) return null

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).setMaxUpdates(1).build()

        return suspendCancellableCoroutine { continuation ->
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    continuation.resume(result.lastLocation)
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }
}
```

---

## Database Migration

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add columns to customers table
        database.execSQL("ALTER TABLE customers ADD COLUMN latitude REAL")
        database.execSQL("ALTER TABLE customers ADD COLUMN longitude REAL")
        database.execSQL("ALTER TABLE customers ADD COLUMN dateLocalization TEXT")

        // Add columns to deliveries table
        database.execSQL("ALTER TABLE deliveries ADD COLUMN pickingLatitude REAL")
        database.execSQL("ALTER TABLE deliveries ADD COLUMN pickingLongitude REAL")
    }
}
```

---

## UI Components

### Location Capture Button
```kotlin
@Composable
fun LocationCaptureButton(
    latitude: Double?,
    longitude: Double?,
    onCaptureClick: () -> Unit,
    isLoading: Boolean = false
) {
    Column {
        Button(
            onClick = onCaptureClick,
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (latitude != null) "Update Location" else "Capture Location")
        }

        if (latitude != null && longitude != null) {
            Text(
                text = "Lat: $latitude, Long: $longitude",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

### Permission Request
```kotlin
val locationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
    if (fineGranted || coarseGranted) {
        // Proceed with location capture
    }
}

// Request permissions
locationPermissionLauncher.launch(
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
)
```

---

## ViewModel Integration

```kotlin
// In CustomerViewModel or VisitViewModel
private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
val locationState: StateFlow<LocationState> = _locationState

fun captureLocation() {
    viewModelScope.launch {
        _locationState.value = LocationState.Loading
        val location = locationHelper.getAccurateLocation()
        if (location != null) {
            _locationState.value = LocationState.Success(
                latitude = location.latitude,
                longitude = location.longitude
            )
        } else {
            _locationState.value = LocationState.Error("Could not get location")
        }
    }
}

sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    data class Success(val latitude: Double, val longitude: Double) : LocationState()
    data class Error(val message: String) : LocationState()
}
```

---

## Verification Steps

1. Install app and verify location permission dialog appears on first capture attempt
2. Grant permission and tap "Capture Location" on customer create screen
3. Verify coordinates are displayed and saved
4. Create/edit a customer with location, sync to Odoo
5. Verify lat/long values appear in Odoo partner record
6. Test on Visit: start visit should auto-capture coordinates
7. Test denied permission flow (graceful handling)

---

## Notes

- Always check for permission before accessing location
- Use `getAccurateLocation()` for important captures (outlet registration)
- Use `getCurrentLocation()` for quick checks (may return cached location)
- Store `dateLocalization` as ISO timestamp when coordinates are captured
- Consider showing location accuracy to user
