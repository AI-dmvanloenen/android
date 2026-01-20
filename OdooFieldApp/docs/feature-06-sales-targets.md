# Feature: Sales Targets

**Purpose:** Track sales performance against zone-based targets.

---

## Data Model

```kotlin
// domain/model/SalesTarget.kt
data class SalesTarget(
    val id: Int,
    val name: String,
    val userId: Int,
    val userName: String?,
    val zoneId: Int?,
    val zoneName: String?,
    val startDate: String,
    val endDate: String,
    val targetAmount: Double,
    val achievedAmount: Double,
    val syncState: SyncState
) {
    val progressPercent: Float
        get() = if (targetAmount > 0) {
            (achievedAmount / targetAmount * 100).toFloat().coerceIn(0f, 100f)
        } else 0f

    val remainingAmount: Double
        get() = (targetAmount - achievedAmount).coerceAtLeast(0.0)

    val isAchieved: Boolean
        get() = achievedAmount >= targetAmount
}
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `domain/model/SalesTarget.kt` | Domain model |
| `data/local/entity/SalesTargetEntity.kt` | Room entity + mappings |
| `data/local/dao/SalesTargetDao.kt` | Database queries |
| `data/remote/dto/SalesTargetDto.kt` | API DTO |
| `domain/repository/SalesTargetRepository.kt` | Interface |
| `data/repository/SalesTargetRepositoryImpl.kt` | Implementation |
| `presentation/target/TargetViewModel.kt` | ViewModel |
| `presentation/target/TargetScreen.kt` | Target progress UI |
| `presentation/components/ProgressIndicator.kt` | Visual progress component |

---

## Files to Modify

| File | Change |
|------|--------|
| `data/local/OdooDatabase.kt` | Add SalesTargetEntity, increment version, add migration |
| `data/remote/api/OdooApiService.kt` | Add GET /target endpoint |
| `di/AppModule.kt` | Provide SalesTargetDao, SalesTargetRepository |
| `presentation/navigation/Navigation.kt` | Add Target route |
| `presentation/dashboard/DashboardScreen.kt` | Add target progress widget |

---

## API Endpoint

```kotlin
@GET("target")
suspend fun getSalesTargets(
    @Header("Authorization") apiKey: String,
    @Query("start_date") startDate: String? = null,
    @Query("end_date") endDate: String? = null
): Response<List<SalesTargetResponse>>
```

---

## DTO

```kotlin
// data/remote/dto/SalesTargetDto.kt
data class SalesTargetResponse(
    val id: Int,
    val name: String?,
    @SerializedName("user_id")
    val userId: List<Any>,  // [id, name]
    @SerializedName("sales_manager_target_id")
    val salesManagerTargetId: List<Any>?,
    @SerializedName("zone_id")
    val zoneId: List<Any>?,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    @SerializedName("target_amount")
    val targetAmount: Double,
    @SerializedName("achieved_amount")
    val achievedAmount: Double
)

fun SalesTargetResponse.toDomain(): SalesTarget {
    return SalesTarget(
        id = id,
        name = name ?: "Target #$id",
        userId = (userId.firstOrNull() as? Number)?.toInt() ?: 0,
        userName = userId.getOrNull(1) as? String,
        zoneId = (zoneId?.firstOrNull() as? Number)?.toInt(),
        zoneName = zoneId?.getOrNull(1) as? String,
        startDate = startDate,
        endDate = endDate,
        targetAmount = targetAmount,
        achievedAmount = achievedAmount,
        syncState = SyncState.SYNCED
    )
}
```

---

## Database Entity

```kotlin
@Entity(
    tableName = "sales_targets",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["startDate", "endDate"])
    ]
)
data class SalesTargetEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val userId: Int,
    val userName: String?,
    val zoneId: Int?,
    val zoneName: String?,
    val startDate: String,
    val endDate: String,
    val targetAmount: Double,
    val achievedAmount: Double,
    val syncState: String,
    val lastModified: Long
)

fun SalesTargetEntity.toDomain(): SalesTarget {
    return SalesTarget(
        id = id,
        name = name,
        userId = userId,
        userName = userName,
        zoneId = zoneId,
        zoneName = zoneName,
        startDate = startDate,
        endDate = endDate,
        targetAmount = targetAmount,
        achievedAmount = achievedAmount,
        syncState = SyncState.valueOf(syncState)
    )
}

fun SalesTarget.toEntity(): SalesTargetEntity {
    return SalesTargetEntity(
        id = id,
        name = name,
        userId = userId,
        userName = userName,
        zoneId = zoneId,
        zoneName = zoneName,
        startDate = startDate,
        endDate = endDate,
        targetAmount = targetAmount,
        achievedAmount = achievedAmount,
        syncState = syncState.name,
        lastModified = System.currentTimeMillis()
    )
}
```

---

## DAO

```kotlin
@Dao
interface SalesTargetDao {
    @Query("SELECT * FROM sales_targets ORDER BY startDate DESC")
    fun getAllTargets(): Flow<List<SalesTargetEntity>>

    @Query("""
        SELECT * FROM sales_targets
        WHERE startDate <= :today AND endDate >= :today
        ORDER BY zoneName ASC
    """)
    fun getCurrentTargets(today: String): Flow<List<SalesTargetEntity>>

    @Query("""
        SELECT * FROM sales_targets
        WHERE startDate >= :startDate AND endDate <= :endDate
    """)
    fun getTargetsByDateRange(startDate: String, endDate: String): Flow<List<SalesTargetEntity>>

    @Query("SELECT * FROM sales_targets WHERE zoneId = :zoneId")
    fun getTargetsByZone(zoneId: Int): Flow<List<SalesTargetEntity>>

    @Query("""
        SELECT SUM(targetAmount) as totalTarget,
               SUM(achievedAmount) as totalAchieved
        FROM sales_targets
        WHERE startDate <= :today AND endDate >= :today
    """)
    fun getCurrentPeriodSummary(today: String): Flow<TargetSummary?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<SalesTargetEntity>)

    @Query("DELETE FROM sales_targets")
    suspend fun deleteAllTargets()
}

data class TargetSummary(
    val totalTarget: Double,
    val totalAchieved: Double
) {
    val overallProgress: Float
        get() = if (totalTarget > 0) {
            (totalAchieved / totalTarget * 100).toFloat()
        } else 0f
}
```

---

## Repository

```kotlin
// domain/repository/SalesTargetRepository.kt
interface SalesTargetRepository {
    fun getAllTargets(): Flow<List<SalesTarget>>
    fun getCurrentTargets(): Flow<List<SalesTarget>>
    fun getTargetsByDateRange(startDate: String, endDate: String): Flow<List<SalesTarget>>
    fun getCurrentPeriodSummary(): Flow<TargetSummary?>
    suspend fun syncTargets(): Resource<Unit>
}

// data/repository/SalesTargetRepositoryImpl.kt
class SalesTargetRepositoryImpl @Inject constructor(
    private val salesTargetDao: SalesTargetDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : SalesTargetRepository {

    override fun getCurrentTargets(): Flow<List<SalesTarget>> {
        val today = LocalDate.now().toString()
        return salesTargetDao.getCurrentTargets(today)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getCurrentPeriodSummary(): Flow<TargetSummary?> {
        val today = LocalDate.now().toString()
        return salesTargetDao.getCurrentPeriodSummary(today)
    }

    override suspend fun syncTargets(): Resource<Unit> {
        return try {
            val apiKey = apiKeyProvider.getApiKey()
                ?: return Resource.Error("API key not configured")

            val response = apiService.getSalesTargets(apiKey)
            if (response.isSuccessful) {
                val targets = response.body()?.map { it.toDomain() } ?: emptyList()
                salesTargetDao.deleteAllTargets()
                salesTargetDao.insertTargets(targets.map { it.toEntity() })
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to fetch targets: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }
}
```

---

## ViewModel

```kotlin
@HiltViewModel
class TargetViewModel @Inject constructor(
    private val repository: SalesTargetRepository
) : ViewModel() {

    val currentTargets = repository.getCurrentTargets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val periodSummary = repository.getCurrentPeriodSummary()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _selectedDateRange = MutableStateFlow<Pair<String, String>?>(null)
    val selectedDateRange: StateFlow<Pair<String, String>?> = _selectedDateRange

    val filteredTargets = _selectedDateRange.flatMapLatest { range ->
        if (range != null) {
            repository.getTargetsByDateRange(range.first, range.second)
        } else {
            repository.getCurrentTargets()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _syncState = MutableStateFlow<Resource<Unit>?>(null)
    val syncState: StateFlow<Resource<Unit>?> = _syncState

    fun setDateRange(startDate: String, endDate: String) {
        _selectedDateRange.value = startDate to endDate
    }

    fun clearDateRange() {
        _selectedDateRange.value = null
    }

    fun syncTargets() {
        viewModelScope.launch {
            _syncState.value = Resource.Loading()
            _syncState.value = repository.syncTargets()
        }
    }
}
```

---

## UI Components

### ProgressIndicator
```kotlin
@Composable
fun TargetProgressIndicator(
    progress: Float,
    targetAmount: Double,
    achievedAmount: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Achieved: ${achievedAmount.formatCurrency()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Target: ${targetAmount.formatCurrency()}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = when {
                progress >= 100 -> Color.Green
                progress >= 75 -> Color(0xFF4CAF50)
                progress >= 50 -> Color(0xFFFFC107)
                else -> Color(0xFFFF5722)
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${progress.toInt()}% Complete",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
```

### TargetScreen
```kotlin
@Composable
fun TargetScreen(
    viewModel: TargetViewModel = hiltViewModel()
) {
    val targets by viewModel.filteredTargets.collectAsState()
    val summary by viewModel.periodSummary.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Overall summary card
        summary?.let { s ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Current Period Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TargetProgressIndicator(
                        progress = s.overallProgress,
                        targetAmount = s.totalTarget,
                        achievedAmount = s.totalAchieved
                    )
                }
            }
        }

        // Date range filter
        DateRangeFilter(
            onRangeSelected = { start, end ->
                viewModel.setDateRange(start, end)
            },
            onClear = { viewModel.clearDateRange() }
        )

        // Target list by zone
        LazyColumn {
            items(targets) { target ->
                TargetCard(target = target)
            }
        }
    }
}

@Composable
fun TargetCard(target: SalesTarget) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = target.zoneName ?: target.name,
                    style = MaterialTheme.typography.titleSmall
                )
                if (target.isAchieved) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Achieved",
                        tint = Color.Green
                    )
                }
            }

            Text(
                text = "${target.startDate} - ${target.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            TargetProgressIndicator(
                progress = target.progressPercent,
                targetAmount = target.targetAmount,
                achievedAmount = target.achievedAmount
            )

            if (!target.isAchieved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Remaining: ${target.remainingAmount.formatCurrency()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

---

## Dashboard Integration

```kotlin
// Add to DashboardScreen
@Composable
fun TargetSummaryWidget(
    summary: TargetSummary?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sales Target", style = MaterialTheme.typography.titleSmall)

            if (summary != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = summary.overallProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${summary.overallProgress.toInt()}% of ${summary.totalTarget.formatCurrency()}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "No active targets",
                    style = MaterialTheme.typography.bodySmall
                )
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
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS sales_targets (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                userId INTEGER NOT NULL,
                userName TEXT,
                zoneId INTEGER,
                zoneName TEXT,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL,
                targetAmount REAL NOT NULL,
                achievedAmount REAL NOT NULL,
                syncState TEXT NOT NULL,
                lastModified INTEGER NOT NULL
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_targets_userId ON sales_targets(userId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sales_targets_dates ON sales_targets(startDate, endDate)")
    }
}
```

---

## Verification Steps

1. Navigate to Targets screen from dashboard
2. Verify targets sync from Odoo
3. Check overall summary shows correct totals
4. Verify individual zone targets display with progress bars
5. Test date range filter (e.g., last month, this quarter)
6. Verify achieved targets show checkmark
7. Dashboard widget shows current period progress
8. Pull-to-refresh syncs latest achieved amounts

---

## Notes

- Targets are read-only (achievement calculated in Odoo based on sales)
- Sync frequently to get updated achieved amounts
- Color-code progress bars based on achievement level
- Consider push notifications when targets are reached
- Zone-based grouping helps field managers see team performance
