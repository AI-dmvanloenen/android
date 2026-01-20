# User Tracking Implementation Plan

## Problem
All app users share the same Odoo API key. Need to track individual users to filter records (sales orders, deliveries, payments) per user. Odoo assigns records to employees/users (e.g., salesperson on sales orders).

## Approach: Employee ID-based Filtering

Users provide their **Odoo Employee ID** on first launch. This ID is used to:
1. Filter records client-side by matching against `employeeId` field in synced data
2. Optionally request server-side filtering via `?employee_id=X` query param

---

## Implementation Overview

### App Changes
1. **User Setup Screen**: Capture name + employee ID on first launch
2. **Store in DataStore**: Employee ID alongside API key/server URL
3. **Add `employeeId` to entities**: Sales, Deliveries need employee assignment field
4. **Filter queries**: DAO methods filter by employee ID

### Odoo API Changes Required
Add `employee_id` field to API responses:

```json
// Sales endpoint response
{
  "id": 123,
  "name": "SO001",
  "employee_id": 5,  // NEW: assigned employee/salesperson
  ...
}

// Deliveries endpoint response
{
  "id": 456,
  "name": "WH/OUT/001",
  "employee_id": 5,  // NEW: assigned employee
  ...
}
```

Optional: Accept `?employee_id=X` parameter to filter server-side.

---

## Files to Create

| File | Purpose |
|------|---------|
| `domain/model/UserProfile.kt` | User profile model (name, employeeId) |
| `domain/repository/UserRepository.kt` | Interface for user operations |
| `data/repository/UserRepositoryImpl.kt` | DataStore-backed implementation |
| `presentation/user/UserSetupScreen.kt` | First-launch setup UI |
| `presentation/user/UserSetupViewModel.kt` | ViewModel for setup |

## Files to Modify

| File | Changes |
|------|---------|
| `data/repository/ApiKeyProviderImpl.kt` | Add `employeeId`, `userName` storage |
| `data/local/OdooDatabase.kt` | Migration 8→9, add `employeeId` columns |
| `data/local/entity/SaleEntity.kt` | Add `employeeId: Int?` |
| `data/local/entity/DeliveryEntity.kt` | Add `employeeId: Int?` |
| `data/local/entity/PaymentEntity.kt` | Add `createdByEmployeeId: Int?` |
| `data/remote/dto/SaleDto.kt` | Add `employeeId` field |
| `data/remote/dto/DeliveryDto.kt` | Add `employeeId` field |
| `data/remote/mapper/SaleMapper.kt` | Map employeeId |
| `data/remote/mapper/DeliveryMapper.kt` | Map employeeId |
| `data/local/dao/SaleDao.kt` | Add `getSalesByEmployee()` query |
| `data/local/dao/DeliveryDao.kt` | Add `getDeliveriesByEmployee()` query |
| `presentation/navigation/Navigation.kt` | Conditional start destination |
| `di/AppModule.kt` | Provide UserRepository |

---

## Database Migration (8→9)

```sql
-- Add employee assignment to synced records
ALTER TABLE sales ADD COLUMN employeeId INTEGER;
ALTER TABLE deliveries ADD COLUMN employeeId INTEGER;

-- Track who created local records
ALTER TABLE payments ADD COLUMN createdByEmployeeId INTEGER;
ALTER TABLE customers ADD COLUMN createdByEmployeeId INTEGER;

-- Indexes for filtering
CREATE INDEX index_sales_employeeId ON sales (employeeId);
CREATE INDEX index_deliveries_employeeId ON deliveries (employeeId);
```

---

## User Flow

```
App Launch
    │
    ▼
┌─────────────────────────┐
│ Is user setup complete? │
└───────────┬─────────────┘
            │
    ┌───────┴───────┐
    │ No            │ Yes
    ▼               ▼
┌──────────┐   ┌───────────┐
│ UserSetup│   │ Dashboard │
│ Screen   │   │           │
└────┬─────┘   └───────────┘
     │
     │ Enter name + employee ID
     ▼
┌──────────────────────────┐
│ Store in DataStore       │
│ Navigate to Dashboard    │
└──────────────────────────┘
```

---

## Filtering Logic

### Approach: Client-side filtering (start simple)
- Sync all records from Odoo
- Filter in DAO: `WHERE employeeId = :currentEmployeeId`
- **Default: "My records only"** - show only records assigned to current employee
- Toggle in UI to "Show all" when needed

### Future: Server-side filtering (when needed)
- Pass `?employee_id=X` to API endpoints
- Odoo filters before returning
- Reduces data transfer for large datasets

---

## Key Implementation Details

### UserProfile Model
```kotlin
data class UserProfile(
    val name: String,
    val employeeId: Int,
    val setupComplete: Boolean = true
)
```

### DataStore Keys
```kotlin
private val EMPLOYEE_ID = intPreferencesKey("employee_id")
private val USER_NAME = stringPreferencesKey("user_name")
private val USER_SETUP_COMPLETE = booleanPreferencesKey("user_setup_complete")
```

### DAO Query Example
```kotlin
@Query("SELECT * FROM sales WHERE employeeId = :employeeId ORDER BY dateOrder DESC")
fun getSalesByEmployee(employeeId: Int): Flow<List<SaleEntity>>

@Query("SELECT * FROM sales ORDER BY dateOrder DESC")
fun getAllSales(): Flow<List<SaleEntity>>  // Keep for "show all" mode
```

---

## Verification Plan

1. **Fresh install** → UserSetup screen appears
2. **Enter name + employee ID** → Stored in DataStore, navigate to Dashboard
3. **Sync sales** → `employeeId` populated from API response
4. **Filter sales by employee** → Only matching records shown
5. **Create payment** → `createdByEmployeeId` set to current user
6. **Toggle "show all"** → All records visible
7. **Reinstall app** → Setup screen appears (employee ID re-entered)

---

## Future Enhancements

1. **Server-side filtering**: Add `?employee_id=X` to API calls
2. **Multi-device**: Employee ID is already Odoo-linked, same ID works across devices
3. **Employee lookup**: Could add endpoint to validate employee ID exists
4. **Remember device**: Optional "remember me" with device token
