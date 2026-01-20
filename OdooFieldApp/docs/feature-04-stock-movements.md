# Feature: Stock Movements

**Purpose:** Track allocated, delivered, and returned stock quantities per product.

---

## Data Model

```kotlin
// domain/model/StockMovement.kt
data class StockMovement(
    val id: Int,
    val productId: Int,
    val productName: String,
    val stockAllocated: Double,
    val stockDelivered: Double,
    val stockReturned: Double,
    val pickingIds: List<Int>,
    val syncState: SyncState
)
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `domain/model/StockMovement.kt` | Domain model |
| `data/local/entity/StockMovementEntity.kt` | Room entity + mappings |
| `data/local/dao/StockMovementDao.kt` | Database queries |
| `data/remote/dto/StockMovementDto.kt` | API DTO |
| `domain/repository/StockMovementRepository.kt` | Interface |
| `data/repository/StockMovementRepositoryImpl.kt` | Implementation |
| `presentation/stock/StockViewModel.kt` | ViewModel |
| `presentation/stock/StockListScreen.kt` | List by product |
| `presentation/stock/StockDetailScreen.kt` | Movement details |

---

## Files to Modify

| File | Change |
|------|--------|
| `data/local/OdooDatabase.kt` | Add StockMovementEntity, increment version, add migration |
| `data/remote/api/OdooApiService.kt` | Add GET /stock endpoint |
| `di/AppModule.kt` | Provide StockMovementDao, StockMovementRepository |
| `presentation/navigation/Navigation.kt` | Add Stock routes |
| `presentation/dashboard/DashboardScreen.kt` | Add stock summary card |

---

## API Endpoint

```kotlin
@GET("stock")
suspend fun getStockMovements(
    @Header("Authorization") apiKey: String,
    @Query("date") date: String? = null
): Response<List<StockMovementResponse>>
```

---

## DTO

```kotlin
// data/remote/dto/StockMovementDto.kt
data class StockMovementResponse(
    val id: Int,
    @SerializedName("product_name")
    val productName: String,
    @SerializedName("product_id")
    val productId: List<Any>,  // [id, name]
    @SerializedName("picking_ids")
    val pickingIds: List<Int>,
    @SerializedName("stock_allocated")
    val stockAllocated: Double,
    @SerializedName("stock_delivered")
    val stockDelivered: Double,
    @SerializedName("stock_returned")
    val stockReturned: Double
)

fun StockMovementResponse.toDomain(): StockMovement {
    return StockMovement(
        id = id,
        productId = (productId.firstOrNull() as? Number)?.toInt() ?: 0,
        productName = productName,
        stockAllocated = stockAllocated,
        stockDelivered = stockDelivered,
        stockReturned = stockReturned,
        pickingIds = pickingIds,
        syncState = SyncState.SYNCED
    )
}
```

---

## Database Entity

```kotlin
@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["productId"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey val id: Int,
    val productId: Int,
    val productName: String,
    val stockAllocated: Double,
    val stockDelivered: Double,
    val stockReturned: Double,
    val pickingIdsJson: String,  // JSON array of picking IDs
    val syncState: String,
    val lastModified: Long
)

fun StockMovementEntity.toDomain(): StockMovement {
    return StockMovement(
        id = id,
        productId = productId,
        productName = productName,
        stockAllocated = stockAllocated,
        stockDelivered = stockDelivered,
        stockReturned = stockReturned,
        pickingIds = Gson().fromJson(pickingIdsJson, Array<Int>::class.java).toList(),
        syncState = SyncState.valueOf(syncState)
    )
}

fun StockMovement.toEntity(): StockMovementEntity {
    return StockMovementEntity(
        id = id,
        productId = productId,
        productName = productName,
        stockAllocated = stockAllocated,
        stockDelivered = stockDelivered,
        stockReturned = stockReturned,
        pickingIdsJson = Gson().toJson(pickingIds),
        syncState = syncState.name,
        lastModified = System.currentTimeMillis()
    )
}
```

---

## DAO

```kotlin
@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY productName ASC")
    fun getAllStockMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId")
    fun getStockForProduct(productId: Int): Flow<StockMovementEntity?>

    @Query("""
        SELECT * FROM stock_movements
        WHERE stockAllocated > stockDelivered
        ORDER BY productName ASC
    """)
    fun getPendingStock(): Flow<List<StockMovementEntity>>

    @Query("""
        SELECT SUM(stockAllocated) as allocated,
               SUM(stockDelivered) as delivered,
               SUM(stockReturned) as returned
        FROM stock_movements
    """)
    fun getStockSummary(): Flow<StockSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAllStockMovements()
}

data class StockSummary(
    val allocated: Double,
    val delivered: Double,
    val returned: Double
)
```

---

## Repository

```kotlin
// domain/repository/StockMovementRepository.kt
interface StockMovementRepository {
    fun getAllStockMovements(): Flow<List<StockMovement>>
    fun getStockForProduct(productId: Int): Flow<StockMovement?>
    fun getPendingStock(): Flow<List<StockMovement>>
    fun getStockSummary(): Flow<StockSummary>
    suspend fun syncStockMovements(): Resource<Unit>
}

// data/repository/StockMovementRepositoryImpl.kt
class StockMovementRepositoryImpl @Inject constructor(
    private val stockMovementDao: StockMovementDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : StockMovementRepository {

    override fun getAllStockMovements(): Flow<List<StockMovement>> {
        return stockMovementDao.getAllStockMovements()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun syncStockMovements(): Resource<Unit> {
        return try {
            val apiKey = apiKeyProvider.getApiKey()
                ?: return Resource.Error("API key not configured")

            val response = apiService.getStockMovements(apiKey)
            if (response.isSuccessful) {
                val movements = response.body()?.map { it.toDomain() } ?: emptyList()
                stockMovementDao.deleteAllStockMovements()
                stockMovementDao.insertStockMovements(movements.map { it.toEntity() })
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to fetch stock: ${response.message()}")
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
class StockViewModel @Inject constructor(
    private val repository: StockMovementRepository
) : ViewModel() {

    val stockMovements = repository.getAllStockMovements()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val stockSummary = repository.getStockSummary()
        .stateIn(viewModelScope, SharingStarted.Lazily, StockSummary(0.0, 0.0, 0.0))

    val pendingStock = repository.getPendingStock()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _syncState = MutableStateFlow<Resource<Unit>?>(null)
    val syncState: StateFlow<Resource<Unit>?> = _syncState

    fun syncStock() {
        viewModelScope.launch {
            _syncState.value = Resource.Loading()
            _syncState.value = repository.syncStockMovements()
        }
    }
}
```

---

## UI Screens

### StockListScreen
```kotlin
@Composable
fun StockListScreen(
    viewModel: StockViewModel = hiltViewModel(),
    onProductClick: (Int) -> Unit
) {
    val stockMovements by viewModel.stockMovements.collectAsState()
    val summary by viewModel.stockSummary.collectAsState()

    Column {
        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryCard("Allocated", summary.allocated)
            SummaryCard("Delivered", summary.delivered)
            SummaryCard("Returned", summary.returned)
        }

        // Stock list
        LazyColumn {
            items(stockMovements) { movement ->
                StockMovementItem(
                    movement = movement,
                    onClick = { onProductClick(movement.productId) }
                )
            }
        }
    }
}

@Composable
fun StockMovementItem(movement: StockMovement, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(movement.productName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                StockChip("Allocated", movement.stockAllocated, Color.Blue)
                StockChip("Delivered", movement.stockDelivered, Color.Green)
                StockChip("Returned", movement.stockReturned, Color.Red)
            }

            // Progress bar showing delivered vs allocated
            val progress = if (movement.stockAllocated > 0) {
                (movement.stockDelivered / movement.stockAllocated).toFloat()
            } else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
```

---

## Dashboard Integration

```kotlin
// Add to DashboardStats
data class DashboardStats(
    val deliveriesToComplete: Int,
    val todaysDeliveries: Int,
    val pendingPayments: Int,
    val syncErrors: Int,
    val stockAllocated: Double,    // NEW
    val stockDelivered: Double,    // NEW
    val stockPending: Double       // NEW (allocated - delivered)
)

// Add card to DashboardScreen
StockSummaryCard(
    allocated = stats.stockAllocated,
    delivered = stats.stockDelivered,
    pending = stats.stockPending,
    onClick = { navController.navigate(Screen.StockList.route) }
)
```

---

## Database Migration

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS stock_movements (
                id INTEGER PRIMARY KEY NOT NULL,
                productId INTEGER NOT NULL,
                productName TEXT NOT NULL,
                stockAllocated REAL NOT NULL,
                stockDelivered REAL NOT NULL,
                stockReturned REAL NOT NULL,
                pickingIdsJson TEXT NOT NULL,
                syncState TEXT NOT NULL,
                lastModified INTEGER NOT NULL
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_productId ON stock_movements(productId)")
    }
}
```

---

## Verification Steps

1. Navigate to Stock screen from dashboard
2. Verify stock movements sync from Odoo
3. Check summary cards show correct totals
4. Tap on a product to see details
5. Verify progress bars reflect delivery completion
6. Filter by pending (not fully delivered)
7. Pull-to-refresh syncs latest data

---

## Notes

- Stock movements are read-only (pulled from Odoo)
- Useful for delivery drivers to see what's on their truck
- Link pickingIds to Delivery entities for navigation
- Consider adding date filter for historical data
