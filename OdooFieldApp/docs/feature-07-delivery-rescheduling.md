# Feature: Delivery Rescheduling

**Purpose:** Allow rescheduling deliveries with date tracking and status updates.

---

## Data Model Changes

```kotlin
// Add to domain/model/Delivery.kt
data class Delivery(
    val id: Int,
    val mobileUid: String,
    val name: String,
    val partnerId: Int,
    val partnerName: String,
    val scheduledDate: String,
    val state: String,
    val saleId: Int?,
    val saleName: String?,
    val lines: List<DeliveryLine>,
    val syncState: SyncState,
    // NEW FIELDS
    val citRescheduleDate: String?,
    val citRescheduleDelivery: Boolean,
    val deliveryCancelTime: String?,
    val pickingLatitude: Double?,
    val pickingLongitude: Double?
)
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `presentation/delivery/RescheduleDialog.kt` | Date picker dialog for rescheduling |

---

## Files to Modify

| File | Change |
|------|--------|
| `domain/model/Delivery.kt` | Add reschedule fields |
| `data/local/entity/DeliveryEntity.kt` | Add columns + update mappings |
| `data/local/OdooDatabase.kt` | Add migration for new columns |
| `data/remote/dto/DeliveryDto.kt` | Add fields to DTO |
| `domain/repository/DeliveryRepository.kt` | Add rescheduleDelivery() method |
| `data/repository/DeliveryRepositoryImpl.kt` | Implement reschedule logic |
| `data/remote/api/OdooApiService.kt` | Add reschedule endpoint |
| `presentation/delivery/DeliveryDetailScreen.kt` | Add reschedule button |
| `presentation/delivery/DeliveryViewModel.kt` | Add reschedule action |

---

## API Endpoint

```kotlin
@PUT("delivery/{id}/reschedule")
suspend fun rescheduleDelivery(
    @Header("Authorization") apiKey: String,
    @Path("id") deliveryId: Int,
    @Body request: RescheduleRequest
): Response<DeliveryResponse>

data class RescheduleRequest(
    @SerializedName("cit_reschedule_date")
    val rescheduleDate: String,
    @SerializedName("cit_reschedule_delivery")
    val rescheduleDelivery: Boolean = true
)
```

---

## DTO Updates

```kotlin
// data/remote/dto/DeliveryDto.kt
data class DeliveryResponse(
    val id: Int,
    val name: String,
    @SerializedName("partner_id")
    val partnerId: List<Any>,
    @SerializedName("scheduled_date")
    val scheduledDate: String,
    val state: String,
    @SerializedName("sale_id")
    val saleId: List<Any>?,
    @SerializedName("move_ids_without_package")
    val moveIds: List<Int>,
    // NEW FIELDS
    @SerializedName("cit_reschedule_date")
    val citRescheduleDate: String?,
    @SerializedName("cit_reschedule_delivery")
    val citRescheduleDelivery: Boolean?,
    @SerializedName("delivery_cancel_time")
    val deliveryCancelTime: String?,
    @SerializedName("picking_latitude")
    val pickingLatitude: Double?,
    @SerializedName("picking_longitude")
    val pickingLongitude: Double?
)
```

---

## Entity Updates

```kotlin
// data/local/entity/DeliveryEntity.kt
@Entity(
    tableName = "deliveries",
    indices = [
        Index(value = ["mobileUid"], unique = true),
        Index(value = ["partnerId"]),
        Index(value = ["state"]),
        Index(value = ["scheduledDate"])
    ]
)
data class DeliveryEntity(
    @PrimaryKey val id: Int,
    val mobileUid: String,
    val name: String,
    val partnerId: Int,
    val partnerName: String,
    val scheduledDate: String,
    val state: String,
    val saleId: Int?,
    val saleName: String?,
    val syncState: String,
    val lastModified: Long,
    // NEW FIELDS
    val citRescheduleDate: String?,
    val citRescheduleDelivery: Boolean,
    val deliveryCancelTime: String?,
    val pickingLatitude: Double?,
    val pickingLongitude: Double?
)
```

---

## Repository Updates

```kotlin
// domain/repository/DeliveryRepository.kt
interface DeliveryRepository {
    // ... existing methods
    suspend fun rescheduleDelivery(deliveryId: Int, newDate: String): Resource<Delivery>
    fun getRescheduledDeliveries(): Flow<List<Delivery>>
}

// data/repository/DeliveryRepositoryImpl.kt
class DeliveryRepositoryImpl @Inject constructor(
    private val deliveryDao: DeliveryDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : DeliveryRepository {

    override suspend fun rescheduleDelivery(deliveryId: Int, newDate: String): Resource<Delivery> {
        return try {
            val apiKey = apiKeyProvider.getApiKey()
                ?: return Resource.Error("API key not configured")

            // Update locally first (optimistic update)
            val existingDelivery = deliveryDao.getDeliveryById(deliveryId)
            if (existingDelivery != null) {
                val updatedEntity = existingDelivery.copy(
                    citRescheduleDate = newDate,
                    citRescheduleDelivery = true,
                    state = "rescheduled",
                    syncState = SyncState.PENDING.name,
                    lastModified = System.currentTimeMillis()
                )
                deliveryDao.insertDelivery(updatedEntity)
            }

            // Sync to server
            val response = apiService.rescheduleDelivery(
                apiKey = apiKey,
                deliveryId = deliveryId,
                request = RescheduleRequest(rescheduleDate = newDate)
            )

            if (response.isSuccessful) {
                val delivery = response.body()?.toDomain()
                if (delivery != null) {
                    deliveryDao.insertDelivery(delivery.toEntity().copy(
                        syncState = SyncState.SYNCED.name
                    ))
                    Resource.Success(delivery)
                } else {
                    Resource.Error("Empty response")
                }
            } else {
                Resource.Error("Failed to reschedule: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }

    override fun getRescheduledDeliveries(): Flow<List<Delivery>> {
        return deliveryDao.getDeliveriesByState("rescheduled")
            .map { entities -> entities.map { it.toDomain() } }
    }
}
```

---

## DAO Updates

```kotlin
// data/local/dao/DeliveryDao.kt
@Dao
interface DeliveryDao {
    // ... existing methods

    @Query("SELECT * FROM deliveries WHERE state = :state ORDER BY scheduledDate DESC")
    fun getDeliveriesByState(state: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE citRescheduleDelivery = 1 ORDER BY citRescheduleDate ASC")
    fun getRescheduledDeliveries(): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE id = :id")
    suspend fun getDeliveryById(id: Int): DeliveryEntity?
}
```

---

## RescheduleDialog

```kotlin
// presentation/delivery/RescheduleDialog.kt
@Composable
fun RescheduleDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (newDate: String) -> Unit
) {
    var selectedDate by remember {
        mutableStateOf(LocalDate.parse(currentDate))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Delivery") },
        text = {
            Column {
                Text("Current date: $currentDate")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New date: ${selectedDate}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "The delivery will be rescheduled to the selected date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate.toString()) },
                enabled = selectedDate.toString() != currentDate
            ) {
                Text("Reschedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedDate,
            minDate = LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    minDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
```

---

## ViewModel Updates

```kotlin
// presentation/delivery/DeliveryViewModel.kt
@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val repository: DeliveryRepository
) : ViewModel() {

    // ... existing code

    private val _rescheduleState = MutableStateFlow<Resource<Delivery>?>(null)
    val rescheduleState: StateFlow<Resource<Delivery>?> = _rescheduleState

    fun rescheduleDelivery(deliveryId: Int, newDate: String) {
        viewModelScope.launch {
            _rescheduleState.value = Resource.Loading()
            _rescheduleState.value = repository.rescheduleDelivery(deliveryId, newDate)
        }
    }

    fun clearRescheduleState() {
        _rescheduleState.value = null
    }
}
```

---

## DeliveryDetailScreen Updates

```kotlin
// presentation/delivery/DeliveryDetailScreen.kt
@Composable
fun DeliveryDetailScreen(
    deliveryId: Int,
    viewModel: DeliveryViewModel = hiltViewModel()
) {
    val delivery by viewModel.getDeliveryById(deliveryId).collectAsState(initial = null)
    val rescheduleState by viewModel.rescheduleState.collectAsState()
    var showRescheduleDialog by remember { mutableStateOf(false) }

    // Handle reschedule result
    LaunchedEffect(rescheduleState) {
        when (rescheduleState) {
            is Resource.Success -> {
                // Show success message
                viewModel.clearRescheduleState()
            }
            is Resource.Error -> {
                // Show error message
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(delivery?.name ?: "Delivery") }
            )
        }
    ) { padding ->
        delivery?.let { del ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Delivery info
                DeliveryInfoCard(delivery = del)

                // Show reschedule info if rescheduled
                if (del.citRescheduleDelivery && del.citRescheduleDate != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rescheduled to: ${del.citRescheduleDate}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delivery lines
                Text("Items", style = MaterialTheme.typography.titleMedium)
                del.lines.forEach { line ->
                    DeliveryLineItem(line = line)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reschedule button (only for non-done deliveries)
                    if (del.state != "done" && del.state != "cancel") {
                        OutlinedButton(
                            onClick = { showRescheduleDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reschedule")
                        }
                    }

                    // Validate button (for assigned deliveries)
                    if (del.state == "assigned") {
                        Button(
                            onClick = { viewModel.validateDelivery(del.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Validate")
                        }
                    }
                }
            }
        }
    }

    // Reschedule dialog
    if (showRescheduleDialog && delivery != null) {
        RescheduleDialog(
            currentDate = delivery!!.scheduledDate,
            onDismiss = { showRescheduleDialog = false },
            onConfirm = { newDate ->
                viewModel.rescheduleDelivery(delivery!!.id, newDate)
                showRescheduleDialog = false
            }
        )
    }
}
```

---

## Database Migration

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE deliveries ADD COLUMN citRescheduleDate TEXT")
        database.execSQL("ALTER TABLE deliveries ADD COLUMN citRescheduleDelivery INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE deliveries ADD COLUMN deliveryCancelTime TEXT")
        database.execSQL("ALTER TABLE deliveries ADD COLUMN pickingLatitude REAL")
        database.execSQL("ALTER TABLE deliveries ADD COLUMN pickingLongitude REAL")
    }
}
```

---

## Verification Steps

1. Open a delivery detail screen
2. Verify "Reschedule" button appears for non-completed deliveries
3. Tap Reschedule and select a future date
4. Confirm rescheduling
5. Verify delivery shows new scheduled date and "rescheduled" indicator
6. Verify state changes appropriately
7. Sync and verify reschedule date appears in Odoo
8. Pull deliveries from Odoo and verify rescheduled ones show correct status
9. Test offline: reschedule while offline, verify syncs when back online

---

## Notes

- Only allow rescheduling for deliveries not yet done/cancelled
- Minimum reschedule date should be today or future
- Keep original scheduled date for reference
- Consider adding a reason field for rescheduling
- Rescheduled deliveries should appear in a filtered view
