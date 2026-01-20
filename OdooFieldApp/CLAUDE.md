# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Clean build
./gradlew clean build

# Build debug APK only
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented (Android) tests
./gradlew connectedAndroidTest

# Run a specific test class
./gradlew test --tests "com.odoo.fieldapp.ExampleUnitTest"

# Check for dependency updates
./gradlew dependencyUpdates
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture Overview

This is an **offline-first Android app** for field workers to access Odoo customer data. It uses **Clean Architecture with MVVM**.
The goal of the app is to present temporary information to field agents and allow them to take action. Odoo is and remains the single source of truth.

### Three-Layer Architecture

```
Presentation (UI)          → Jetpack Compose screens + ViewModels
        ↓
Domain (Business Logic)    → Models, Repository interfaces
        ↓
Data (Storage/Network)     → Room DB (local) + Retrofit (remote)
```

### Package Structure

```
com.odoo.fieldapp/
├── data/
│   ├── local/           # Room: OdooDatabase, CustomerDao, CustomerEntity
│   ├── remote/          # Retrofit: OdooApiService, CustomerDto, DynamicBaseUrlInterceptor
│   └── repository/      # CustomerRepositoryImpl, ApiKeyProvider
├── domain/
│   ├── model/           # Customer, SyncState (enum), Resource (sealed class)
│   └── repository/      # CustomerRepository (interface)
├── presentation/
│   ├── customer/        # CustomerListScreen, CustomerDetailScreen, CustomerViewModel
│   ├── settings/        # SettingsScreen, SettingsViewModel
│   └── navigation/      # Navigation.kt (Screen sealed class, NavHost setup)
└── di/                  # AppModule.kt (all Hilt DI configuration)
```

## Key Patterns

### Data Flow
1. UI collects `StateFlow` from ViewModel
2. ViewModel calls Repository methods
3. Repository returns `Flow<T>` from Room or `Flow<Resource<T>>` for network operations
4. Room queries emit updates automatically via Flow

### Mapping Convention
- `CustomerEntity.toDomain()` → `Customer` (DB to domain)
- `Customer.toEntity()` → `CustomerEntity` (domain to DB)
- `CustomerResponse.toDomain()` → `Customer` (API to domain)
- Extension functions live near their source types

### Resource Wrapper
All async operations use `sealed class Resource<T>`:
- `Resource.Loading(data?)` - in progress, optional cached data
- `Resource.Success(data)` - completed successfully
- `Resource.Error(message, data?)` - failed, optional fallback data

### Dynamic Base URL
`DynamicBaseUrlInterceptor` overrides the Retrofit base URL per-request using the server URL stored in DataStore. The hardcoded base URL in `AppModule.kt` is a placeholder.

## Tech Stack

- **Kotlin 1.9.20**, **JDK 17**, **compileSdk 34**, **minSdk 24**
- **Jetpack Compose** (BOM 2023.10.01) with Material 3
- **Room 2.6.1** for local SQLite database
- **Retrofit 2.9.0 + OkHttp 4.12.0** for networking
- **Hilt 2.48** for dependency injection
- **Coroutines 1.7.3 + Flow** for async operations
- **DataStore** for encrypted API key storage
- **KSP** for annotation processing (Room, Hilt)

## Current State (Phase 1)

- **Customer module only**: Pull-only sync from Odoo `/customer` endpoint
- **Database**: Single `customers` table with `cradleUid` for deduplication
- **No tests implemented yet** (framework set up in build.gradle)
- **Future phases**: Sales, Payments, Deliveries, two-way sync with WorkManager

## Adding a New Module

Follow this pattern (e.g., for Sales):
1. `domain/model/Sale.kt` - business model
2. `data/local/entity/SaleEntity.kt` - Room entity + mapping extensions
3. `data/local/dao/SaleDao.kt` - DAO with Flow queries
4. Update `OdooDatabase.kt` - add entity to `@Database`
5. `data/remote/dto/SaleDto.kt` - request/response DTOs
6. `data/remote/api/OdooApiService.kt` - add endpoint
7. `domain/repository/SaleRepository.kt` - interface
8. `data/repository/SaleRepositoryImpl.kt` - implementation
9. `presentation/sale/SaleViewModel.kt` - ViewModel
10. `presentation/sale/SaleListScreen.kt` - Compose UI
11. Update `Navigation.kt` - add Screen route

## API Integration

Odoo endpoint: `GET /customer` with `Authorization` header containing API key.

```kotlin
@GET("customer")
suspend fun getCustomers(@Header("Authorization") apiKey: String): Response<List<CustomerResponse>>
```

Expected JSON fields: `id`, `name`, `city`, `tax_id`, `email`, `phone`, `website`, `date` (ISO format "yyyy-MM-dd")
