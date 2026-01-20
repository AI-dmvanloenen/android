# Feature: Visit Management

**Purpose:** Schedule and track sales visits to outlets with timing, notes, and visit states.

---

## Data Model

```kotlin
// domain/model/Visit.kt
data class Visit(
    val id: Int,
    val mobileUid: String,
    val name: String,
    val partnerId: Int,
    val partnerName: String,
    val planDate: String,
    val startTime: String?,
    val endTime: String?,
    val visitState: String,        // planned, in_progress, completed, rescheduled
    val isReschedule: Boolean,
    val visitNotes: String?,
    val visitZoneId: Int?,
    val syncState: SyncState
)
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `domain/model/Visit.kt` | Domain model |
| `data/local/entity/VisitEntity.kt` | Room entity + mappings |
| `data/local/dao/VisitDao.kt` | Database queries |
| `data/remote/dto/VisitDto.kt` | API DTOs |
| `domain/repository/VisitRepository.kt` | Interface |
| `data/repository/VisitRepositoryImpl.kt` | Implementation |
| `presentation/visit/VisitViewModel.kt` | ViewModel |
| `presentation/visit/VisitListScreen.kt` | List UI |
| `presentation/visit/VisitDetailScreen.kt` | Detail/edit UI |
| `presentation/visit/VisitCreateScreen.kt` | Create form |

---

## Files to Modify

| File | Change |
|------|--------|
| `data/local/OdooDatabase.kt` | Add VisitEntity, increment version, add migration |
| `data/remote/api/OdooApiService.kt` | Add GET/POST /visit endpoints |
| `di/AppModule.kt` | Provide VisitDao, VisitRepository |
| `presentation/navigation/Navigation.kt` | Add Visit routes |
| `presentation/dashboard/DashboardScreen.kt` | Add visits navigation card |

---

## API Endpoints

```kotlin
@GET("visit")
suspend fun getVisits(
    @Header("Authorization") apiKey: String
): Response<List<VisitResponse>>

@POST("visit")
suspend fun createVisit(
    @Header("Authorization") apiKey: String,
    @Body visit: VisitCreateRequest
): Response<VisitResponse>

@PUT("visit/{id}")
suspend fun updateVisit(
    @Header("Authorization") apiKey: String,
    @Path("id") visitId: Int,
    @Body visit: VisitUpdateRequest
): Response<VisitResponse>
```

---

## Database Entity

```kotlin
@Entity(
    tableName = "visits",
    indices = [
        Index(value = ["mobileUid"], unique = true),
        Index(value = ["partnerId"]),
        Index(value = ["planDate"]),
        Index(value = ["visitState"])
    ]
)
data class VisitEntity(
    @PrimaryKey val id: Int,
    val mobileUid: String,
    val name: String,
    val partnerId: Int,
    val partnerName: String,
    val planDate: String,
    val startTime: String?,
    val endTime: String?,
    val visitState: String,
    val isReschedule: Boolean,
    val visitNotes: String?,
    val visitZoneId: Int?,
    val syncState: String,
    val lastModified: Long
)
```

---

## DAO Queries

```kotlin
@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY planDate DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE planDate = :date ORDER BY startTime")
    fun getVisitsByDate(date: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE visitState = :state")
    fun getVisitsByState(state: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE partnerId = :partnerId")
    fun getVisitsForPartner(partnerId: Int): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE syncState = 'PENDING'")
    suspend fun getPendingVisits(): List<VisitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Query("DELETE FROM visits")
    suspend fun deleteAllVisits()
}
```

---

## UI Screens

### VisitListScreen
- Filter tabs: Today | Planned | Completed | All
- Each card shows: outlet name, date, time, state badge
- FAB to create new visit
- Pull-to-refresh for sync

### VisitDetailScreen
- Display visit info and linked outlet
- "Start Visit" button (captures start time + GPS)
- "End Visit" button (captures end time)
- Notes text field
- Photo capture section (links to Photo Capture feature)

### VisitCreateScreen
- Partner/outlet selector (searchable dropdown)
- Date picker
- Planned time (optional)
- Zone selector (optional)
- Save button

---

## State Transitions

```
planned → in_progress → completed
    ↓
rescheduled → planned
```

- **planned**: Visit scheduled but not started
- **in_progress**: Salesperson has arrived (start time captured)
- **completed**: Visit finished (end time captured)
- **rescheduled**: Moved to another date

---

## Verification Steps

1. Create a visit from the app for a specific outlet and date
2. Verify it appears in the visit list with "planned" state
3. Tap "Start Visit" - verify startTime is set, state changes to "in_progress"
4. Tap "End Visit" - verify endTime is set, state changes to "completed"
5. Sync and verify visit data appears in Odoo
6. Pull visits from Odoo and verify they appear in the list
7. Test reschedule flow

---

## Dependencies

- GPS Location Tracking feature (optional, for auto-capturing location on start)
- Photo Capture feature (optional, for proof-of-visit photos)
