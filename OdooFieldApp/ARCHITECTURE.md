# Architecture & Design Decisions

This document explains the technical architecture and key design decisions for the Odoo Field App.

## Architecture Overview

### Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (UI, ViewModels, Compose Screens)      │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│          Domain Layer                   │
│  (Business Models, Repository           │
│   Interfaces, Use Cases)                │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│           Data Layer                    │
│  ┌─────────────────┬─────────────────┐  │
│  │  Local          │    Remote       │  │
│  │  (Room DB)      │  (Retrofit API) │  │
│  └─────────────────┴─────────────────┘  │
└─────────────────────────────────────────┘
```

### Why Clean Architecture?

**Benefits:**
- **Testability:** Each layer can be tested independently
- **Maintainability:** Changes in one layer don't affect others
- **Scalability:** Easy to add new features (sales, payments, deliveries)
- **Separation of Concerns:** Clear responsibilities for each component

**Trade-offs:**
- More boilerplate code
- Steeper learning curve
- More files to navigate

**Decision:** Worth it for this project because it needs to scale to multiple modules (sales, payments, deliveries) and may need different developers to work on different parts.

## Key Technical Decisions

### 1. Jetpack Compose vs XML Layouts

**Decision:** Jetpack Compose

**Reasons:**
- **Modern:** Google's recommended approach for new apps
- **Declarative:** Easier to understand ("this is what the UI looks like")
- **Less boilerplate:** No XML parsing, no findViewById
- **Better for dynamic UIs:** List updates automatically when data changes
- **Future-proof:** All new Android features focus on Compose

**Trade-off:** Slightly steeper learning curve than XML

### 2. Room vs Raw SQLite

**Decision:** Room Database

**Reasons:**
- **Compile-time verification:** Catches SQL errors at build time, not runtime
- **Less boilerplate:** Automatically generates SQL from Kotlin
- **Type-safe:** No string-based queries
- **Coroutines support:** Built-in async/await
- **Migration support:** Easy database schema upgrades

**Example:**
```kotlin
// Room (simple)
@Query("SELECT * FROM customers WHERE name LIKE :query")
fun searchCustomers(query: String): Flow<List<CustomerEntity>>

// Raw SQLite (complex)
fun searchCustomers(query: String): List<Customer> {
    val cursor = db.rawQuery(
        "SELECT * FROM customers WHERE name LIKE ?",
        arrayOf(query)
    )
    val customers = mutableListOf<Customer>()
    while (cursor.moveToNext()) {
        // Manually parse each column...
    }
    cursor.close()
    return customers
}
```

### 3. Retrofit vs Manual HTTP Requests

**Decision:** Retrofit + OkHttp

**Reasons:**
- **Type-safe:** Kotlin data classes for request/response
- **Automatic parsing:** JSON ↔ Kotlin objects via Gson
- **Error handling:** Built-in HTTP error handling
- **Interceptors:** Easy to add logging, authentication
- **Coroutines support:** Seamless async operations

**Example:**
```kotlin
// Retrofit (simple)
@GET("customer")
suspend fun getCustomers(
    @Header("Authorization") apiKey: String
): Response<List<CustomerResponse>>

// Manual (complex)
suspend fun getCustomers(apiKey: String): List<Customer> {
    val connection = URL("https://...").openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("Authorization", apiKey)
    // Manually parse JSON response...
    // Handle errors manually...
}
```

### 4. Hilt vs Manual Dependency Injection

**Decision:** Hilt (Dagger wrapper)

**Reasons:**
- **Android-aware:** Understands Activity, ViewModel lifecycles
- **Compile-time safety:** Catches DI errors at build time
- **Scalability:** Easy to add new dependencies
- **Standard:** Industry standard for Android

**Trade-off:** More complex setup initially, but pays off as app grows

**Example:**
```kotlin
// With Hilt (automatic injection)
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel()

// Without Hilt (manual creation everywhere)
class CustomerViewModel(repository: CustomerRepository) : ViewModel()
// Every screen needs: 
val viewModel = CustomerViewModel(CustomerRepositoryImpl(
    customerDao = database.customerDao(),
    apiService = retrofit.create(...),
    apiKeyProvider = ApiKeyProviderImpl(context)
))
```

### 5. Coroutines + Flow vs RxJava

**Decision:** Kotlin Coroutines + Flow

**Reasons:**
- **Native Kotlin:** Built into the language
- **Simpler:** Less learning curve than RxJava
- **Suspend functions:** Natural async/await syntax
- **Flow:** Reactive streams for database changes
- **Better error handling:** Try/catch works naturally

**Example:**
```kotlin
// Coroutines + Flow (simple)
fun getCustomers(): Flow<List<Customer>> = 
    customerDao.getAllCustomers().map { it.map { entity -> entity.toDomain() } }

viewModelScope.launch {
    customers.collect { list ->
        // Handle updates
    }
}

// RxJava (more complex)
fun getCustomers(): Observable<List<Customer>> =
    customerDao.getAllCustomers()
        .map { it.map { entity -> entity.toDomain() } }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())

// Plus need to manage disposables...
```

### 6. MVVM Architecture Pattern

**Decision:** MVVM (Model-View-ViewModel)

**Layers:**
```
View (Compose UI)
    ↕
ViewModel (UI Logic)
    ↕
Repository (Data Operations)
    ↕
Data Sources (Room + Retrofit)
```

**Reasons:**
- **Android standard:** Recommended by Google
- **Lifecycle aware:** ViewModel survives configuration changes
- **Testable:** ViewModels can be unit tested
- **Reactive:** Flow/LiveData for automatic UI updates

### 7. Repository Pattern

**Decision:** Single source of truth via Repository

**Why:**
```kotlin
// UI doesn't know where data comes from
class CustomerRepository {
    fun getCustomers(): Flow<List<Customer>> {
        // Could be from database...
        return customerDao.getAllCustomers().map { ... }
        // Or from API...
        // Or from both...
        // UI doesn't care!
    }
}
```

**Benefits:**
- **Flexibility:** Can change data source without affecting UI
- **Caching:** Repository decides when to use cache vs fresh data
- **Sync logic:** Centralized place for sync operations

## Data Flow Example

### Syncing Customers from Odoo

```
1. User taps sync button
         ↓
2. CustomerViewModel.syncCustomers()
         ↓
3. CustomerRepository.syncCustomersFromOdoo()
         ↓
4. Fetch from OdooApiService (Retrofit)
         ↓
5. Convert API response to domain models
         ↓
6. Save to Room database (CustomerDao)
         ↓
7. Flow automatically notifies UI
         ↓
8. Compose recomposes with new data
```

### Viewing Customers (Offline)

```
1. Screen opens
         ↓
2. CustomerViewModel subscribes to customers Flow
         ↓
3. Repository returns Flow from Room database
         ↓
4. Room continuously emits updates
         ↓
5. Compose automatically recomposes when data changes
```

## Database Design

### Why separate entity and domain models?

**CustomerEntity (Database):**
```kotlin
data class CustomerEntity(
    val id: String,
    val date: Long,           // Stored as timestamp
    val syncState: String     // Stored as string
)
```

**Customer (Domain):**
```kotlin
data class Customer(
    val id: String,
    val date: Date,           // Business logic uses Date
    val syncState: SyncState  // Type-safe enum
)
```

**Benefits:**
- **Flexibility:** Can change database structure without affecting business logic
- **Type safety:** Domain uses proper types (Date, Enums)
- **Testability:** Domain models are pure Kotlin, no Android dependencies

### UUID Strategy

**Local ID:** UUID generated on device
**Cradle UID:** UUID for Odoo sync (prevents duplicates)
**Odoo ID:** Integer ID from Odoo (null until synced)

**Why three IDs?**
- **Local ID:** Device-specific, never changes
- **Cradle UID:** Cross-device unique ID for sync
- **Odoo ID:** Reference back to Odoo record

## Sync Strategy

### Phase 1: Pull-Only Sync

```kotlin
suspend fun syncCustomersFromOdoo(): Flow<Resource<List<Customer>>> = flow {
    emit(Resource.Loading())
    try {
        val response = apiService.getCustomers(apiKey)
        if (response.isSuccessful) {
            val customers = response.body()?.map { it.toDomain() }
            customerDao.insertCustomers(customers.map { it.toEntity() })
            emit(Resource.Success(customers))
        } else {
            emit(Resource.Error("API error: ${response.code()}"))
        }
    } catch (e: Exception) {
        emit(Resource.Error("Network error: ${e.message}"))
    }
}
```

### Future: Push Sync (Phase 2+)

Will add:
1. **SyncQueue table** - Track pending operations
2. **WorkManager** - Background sync when online
3. **Conflict resolution** - Handle concurrent edits
4. **Batch operations** - Sync multiple records efficiently

## UI State Management

### Resource Wrapper Pattern

```kotlin
sealed class Resource<T> {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String) : Resource<T>(data, message)
    class Loading<T> : Resource<T>(data)
}
```

**Benefits:**
- Single type represents all states
- Forces handling of loading/error states
- Type-safe (compiler ensures you handle all cases)

**UI responds automatically:**
```kotlin
when (syncState) {
    is Resource.Loading -> CircularProgressIndicator()
    is Resource.Success -> ShowSuccessMessage()
    is Resource.Error -> ShowErrorMessage()
}
```

## Performance Considerations

### 1. Database Queries
- **Indexed columns:** name, cradleUid for fast searches
- **Flow-based queries:** Only emit when data changes
- **Pagination:** Will add for large datasets (1000+ customers)

### 2. Network Efficiency
- **Batch requests:** Send/receive multiple records at once
- **Timeout handling:** 30-second timeouts for reliability
- **Retry logic:** Exponential backoff (future)

### 3. Memory Management
- **LazyColumn:** Only renders visible items
- **ViewModel scope:** Automatic cancellation on screen close
- **No memory leaks:** Coroutines tied to lifecycle

## Security Considerations

### 1. API Key Storage
- **DataStore:** Encrypted preferences (secure)
- **No hardcoding:** User enters API key
- **Per-device:** Each device can have different key

### 2. Network Security
- **HTTPS only:** TLS encryption
- **API key in header:** Not in URL (no logging)
- **No sensitive data caching:** Follows Android best practices

### 3. Local Data
- **Room database:** Stored in app-private directory
- **Not accessible:** Other apps can't read data
- **Cleared on uninstall:** No data leakage

## Testing Strategy

### Unit Tests (Future)
```kotlin
class CustomerRepositoryTest {
    @Test
    fun `sync customers saves to database`() {
        // Test repository logic
    }
}
```

### Integration Tests (Future)
```kotlin
class DatabaseTest {
    @Test
    fun `insert and retrieve customer`() {
        // Test Room database
    }
}
```

### UI Tests (Future)
```kotlin
class CustomerListScreenTest {
    @Test
    fun `displays customers correctly`() {
        // Test Compose UI
    }
}
```

## Scalability Plan

### Adding Sales Module

1. **Create domain model:** `Sale.kt`
2. **Create database entity:** `SaleEntity.kt`
3. **Add DAO:** `SaleDao.kt`
4. **Update database:** Add `SaleEntity` to `@Database` annotation
5. **Create DTOs:** `SaleRequest.kt`, `SaleResponse.kt`
6. **Add API endpoint:** Method in `OdooApiService.kt`
7. **Create repository:** `SaleRepository.kt`
8. **Build ViewModel:** `SaleViewModel.kt`
9. **Create screens:** `SaleListScreen.kt`, `SaleDetailScreen.kt`
10. **Add navigation:** Update `Navigation.kt`

**Estimated time:** 4-6 hours per module (once you understand the pattern)

## Lessons Learned (To Be Updated)

### What Worked Well
- Clean architecture makes it easy to test individual components
- Compose UI is intuitive once you understand declarative paradigm
- Room + Flow combo provides reactive database

### Challenges
- Initial setup time (Hilt, Room, Retrofit configuration)
- Learning curve for Compose
- Gradle build times

### If Starting Over
- Consider using Kotlin Multiplatform for iOS support
- Maybe use Ktor instead of Retrofit (Kotlin-native)
- Possibly use Koin instead of Hilt (simpler DI)

---

**Last Updated:** January 2025
**Version:** 1.0 (Phase 1 - Customer Module)
