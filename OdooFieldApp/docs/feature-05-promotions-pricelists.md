# Feature: Promotions & Pricelists

**Purpose:** Apply customer-specific pricing and promotional discounts to orders.

---

## Data Models

### Pricelist
```kotlin
// domain/model/Pricelist.kt
data class Pricelist(
    val id: Int,
    val name: String,
    val active: Boolean
)
```

### PricelistItem
```kotlin
// domain/model/PricelistItem.kt
data class PricelistItem(
    val id: Int,
    val pricelistId: Int,
    val productId: Int?,        // null if applies to category
    val categId: Int?,          // null if applies to specific product
    val fixedPrice: Double?,    // fixed price override
    val percentPrice: Double?,  // percentage discount
    val minQuantity: Double     // minimum qty for rule to apply
)
```

### Promotion
```kotlin
// domain/model/Promotion.kt
data class Promotion(
    val id: Int,
    val programId: Int,
    val programName: String,
    val minimumQty: Int,
    val dateFrom: String,
    val dateTo: String,
    val productIds: List<Int>,
    val productCategoryId: Int?,
    val rewardType: String,       // "product", "discount"
    val rewardProductId: Int?,    // free product for "product" type
    val rewardProductQty: Double, // qty of free product
    val discountPercent: Double?  // for "discount" type
)
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `domain/model/Pricelist.kt` | Pricelist model |
| `domain/model/PricelistItem.kt` | Price rules model |
| `domain/model/Promotion.kt` | Promotion model |
| `data/local/entity/PricelistEntity.kt` | Room entity |
| `data/local/entity/PricelistItemEntity.kt` | Room entity |
| `data/local/entity/PromotionEntity.kt` | Room entity |
| `data/local/dao/PricelistDao.kt` | DAO |
| `data/local/dao/PromotionDao.kt` | DAO |
| `data/remote/dto/PricelistDto.kt` | API DTOs |
| `data/remote/dto/PromotionDto.kt` | API DTOs |
| `domain/repository/PricingRepository.kt` | Interface |
| `data/repository/PricingRepositoryImpl.kt` | Implementation |
| `util/PriceCalculator.kt` | Price calculation logic |

---

## Files to Modify

| File | Change |
|------|--------|
| `domain/model/Customer.kt` | Add pricelistId field |
| `data/local/entity/CustomerEntity.kt` | Add pricelistId column |
| `data/local/OdooDatabase.kt` | Add entities, migrations |
| `data/remote/api/OdooApiService.kt` | Add pricelist/promotion endpoints |
| `di/AppModule.kt` | Provide dependencies |
| `presentation/sale/SaleCreateScreen.kt` | Use PriceCalculator for line prices |
| `presentation/sale/SaleViewModel.kt` | Integrate pricing logic |

---

## API Endpoints

```kotlin
@GET("pricelist")
suspend fun getPricelists(
    @Header("Authorization") apiKey: String
): Response<List<PricelistResponse>>

@GET("pricelist/{id}/items")
suspend fun getPricelistItems(
    @Header("Authorization") apiKey: String,
    @Path("id") pricelistId: Int
): Response<List<PricelistItemResponse>>

@GET("promotion")
suspend fun getPromotions(
    @Header("Authorization") apiKey: String
): Response<List<PromotionResponse>>
```

---

## DTOs

```kotlin
// data/remote/dto/PricelistDto.kt
data class PricelistResponse(
    val id: Int,
    val name: String,
    val active: Boolean
)

data class PricelistItemResponse(
    val id: Int,
    @SerializedName("pricelist_id")
    val pricelistId: List<Any>,
    @SerializedName("product_id")
    val productId: List<Any>?,
    @SerializedName("categ_id")
    val categId: List<Any>?,
    @SerializedName("fixed_price")
    val fixedPrice: Double?,
    @SerializedName("percent_price")
    val percentPrice: Double?,
    @SerializedName("min_quantity")
    val minQuantity: Double
)

// data/remote/dto/PromotionDto.kt
data class PromotionResponse(
    val id: Int,
    @SerializedName("program_id")
    val programId: List<Any>,
    @SerializedName("minimum_qty")
    val minimumQty: Int,
    @SerializedName("date_from")
    val dateFrom: String?,
    @SerializedName("date_to")
    val dateTo: String?,
    @SerializedName("product_ids")
    val productIds: List<Int>,
    @SerializedName("product_category_id")
    val productCategoryId: List<Any>?,
    @SerializedName("reward_type")
    val rewardType: String,
    @SerializedName("reward_product_id")
    val rewardProductId: List<Any>?,
    @SerializedName("reward_product_qty")
    val rewardProductQty: Double?,
    @SerializedName("discount_percent")
    val discountPercent: Double?
)
```

---

## Database Entities

```kotlin
@Entity(tableName = "pricelists")
data class PricelistEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val active: Boolean,
    val lastModified: Long
)

@Entity(
    tableName = "pricelist_items",
    foreignKeys = [
        ForeignKey(
            entity = PricelistEntity::class,
            parentColumns = ["id"],
            childColumns = ["pricelistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pricelistId"), Index("productId"), Index("categId")]
)
data class PricelistItemEntity(
    @PrimaryKey val id: Int,
    val pricelistId: Int,
    val productId: Int?,
    val categId: Int?,
    val fixedPrice: Double?,
    val percentPrice: Double?,
    val minQuantity: Double
)

@Entity(tableName = "promotions")
data class PromotionEntity(
    @PrimaryKey val id: Int,
    val programId: Int,
    val programName: String,
    val minimumQty: Int,
    val dateFrom: String?,
    val dateTo: String?,
    val productIdsJson: String,  // JSON array
    val productCategoryId: Int?,
    val rewardType: String,
    val rewardProductId: Int?,
    val rewardProductQty: Double,
    val discountPercent: Double?,
    val lastModified: Long
)
```

---

## DAOs

```kotlin
@Dao
interface PricelistDao {
    @Query("SELECT * FROM pricelists WHERE active = 1")
    fun getActivePricelists(): Flow<List<PricelistEntity>>

    @Query("SELECT * FROM pricelists WHERE id = :id")
    suspend fun getPricelistById(id: Int): PricelistEntity?

    @Query("""
        SELECT * FROM pricelist_items
        WHERE pricelistId = :pricelistId
        AND (productId = :productId OR categId = :categId)
        AND minQuantity <= :quantity
        ORDER BY minQuantity DESC
        LIMIT 1
    """)
    suspend fun findPriceRule(
        pricelistId: Int,
        productId: Int,
        categId: Int,
        quantity: Double
    ): PricelistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricelists(pricelists: List<PricelistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricelistItems(items: List<PricelistItemEntity>)
}

@Dao
interface PromotionDao {
    @Query("""
        SELECT * FROM promotions
        WHERE (dateFrom IS NULL OR dateFrom <= :today)
        AND (dateTo IS NULL OR dateTo >= :today)
    """)
    fun getActivePromotions(today: String): Flow<List<PromotionEntity>>

    @Query("""
        SELECT * FROM promotions
        WHERE (productIdsJson LIKE '%' || :productId || '%'
               OR productCategoryId = :categId)
        AND minimumQty <= :quantity
        AND (dateFrom IS NULL OR dateFrom <= :today)
        AND (dateTo IS NULL OR dateTo >= :today)
    """)
    suspend fun findApplicablePromotions(
        productId: Int,
        categId: Int,
        quantity: Int,
        today: String
    ): List<PromotionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotions(promotions: List<PromotionEntity>)
}
```

---

## PriceCalculator

```kotlin
// util/PriceCalculator.kt
class PriceCalculator @Inject constructor(
    private val pricelistDao: PricelistDao,
    private val promotionDao: PromotionDao
) {
    /**
     * Calculate the final price for a product based on customer's pricelist
     */
    suspend fun calculatePrice(
        product: Product,
        pricelistId: Int?,
        quantity: Double
    ): PriceResult {
        val basePrice = product.listPrice

        // 1. Check pricelist rules
        val pricelistPrice = if (pricelistId != null) {
            val rule = pricelistDao.findPriceRule(
                pricelistId = pricelistId,
                productId = product.id,
                categId = product.categId ?: 0,
                quantity = quantity
            )
            when {
                rule?.fixedPrice != null -> rule.fixedPrice
                rule?.percentPrice != null -> basePrice * (1 - rule.percentPrice / 100)
                else -> basePrice
            }
        } else {
            basePrice
        }

        return PriceResult(
            unitPrice = pricelistPrice,
            subtotal = pricelistPrice * quantity,
            appliedRule = "Pricelist"
        )
    }

    /**
     * Find applicable promotions for an order line
     */
    suspend fun findPromotions(
        product: Product,
        quantity: Int
    ): List<ApplicablePromotion> {
        val today = LocalDate.now().toString()
        val promotions = promotionDao.findApplicablePromotions(
            productId = product.id,
            categId = product.categId ?: 0,
            quantity = quantity,
            today = today
        )

        return promotions.map { promo ->
            ApplicablePromotion(
                promotionId = promo.id,
                programName = promo.programName,
                rewardType = promo.rewardType,
                rewardProductId = promo.rewardProductId,
                rewardProductQty = promo.rewardProductQty,
                discountPercent = promo.discountPercent
            )
        }
    }

    /**
     * Apply promotion to order and return reward lines
     */
    fun applyPromotion(
        promotion: ApplicablePromotion,
        orderLines: List<SaleLine>
    ): List<SaleLine> {
        return when (promotion.rewardType) {
            "product" -> {
                // Add free product line
                listOf(
                    SaleLine(
                        id = 0,
                        productId = promotion.rewardProductId!!,
                        productName = "Free: ${promotion.programName}",
                        quantity = promotion.rewardProductQty,
                        priceUnit = 0.0,
                        priceSubtotal = 0.0,
                        isReward = true,
                        rewardId = promotion.promotionId
                    )
                )
            }
            "discount" -> {
                // Apply discount to existing lines (handled differently)
                emptyList()
            }
            else -> emptyList()
        }
    }
}

data class PriceResult(
    val unitPrice: Double,
    val subtotal: Double,
    val appliedRule: String?
)

data class ApplicablePromotion(
    val promotionId: Int,
    val programName: String,
    val rewardType: String,
    val rewardProductId: Int?,
    val rewardProductQty: Double,
    val discountPercent: Double?
)
```

---

## ViewModel Integration

```kotlin
// In SaleViewModel
@HiltViewModel
class SaleViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository,
    private val priceCalculator: PriceCalculator
) : ViewModel() {

    private val _orderLines = MutableStateFlow<List<OrderLineState>>(emptyList())

    fun addProduct(product: Product, quantity: Double) {
        viewModelScope.launch {
            // Get customer's pricelist
            val customer = _selectedCustomer.value
            val pricelistId = customer?.pricelistId

            // Calculate price
            val priceResult = priceCalculator.calculatePrice(
                product = product,
                pricelistId = pricelistId,
                quantity = quantity
            )

            // Check for promotions
            val promotions = priceCalculator.findPromotions(product, quantity.toInt())

            val newLine = OrderLineState(
                product = product,
                quantity = quantity,
                unitPrice = priceResult.unitPrice,
                subtotal = priceResult.subtotal,
                availablePromotions = promotions
            )

            _orderLines.update { it + newLine }
        }
    }

    fun applyPromotion(lineIndex: Int, promotion: ApplicablePromotion) {
        viewModelScope.launch {
            val rewardLines = priceCalculator.applyPromotion(
                promotion = promotion,
                orderLines = _orderLines.value.map { it.toSaleLine() }
            )
            // Add reward lines to order
            _rewardLines.update { it + rewardLines }
        }
    }
}
```

---

## UI Integration

```kotlin
// In SaleCreateScreen - show price with pricelist indicator
@Composable
fun OrderLineItem(
    line: OrderLineState,
    onPromotionClick: (ApplicablePromotion) -> Unit
) {
    Card {
        Column {
            Row {
                Text(line.product.name)
                Spacer(modifier = Modifier.weight(1f))
                Text("${line.unitPrice} x ${line.quantity}")
            }

            // Show if price differs from list price
            if (line.unitPrice != line.product.listPrice) {
                Text(
                    "List: ${line.product.listPrice}",
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.LineThrough
                )
            }

            // Show available promotions
            if (line.availablePromotions.isNotEmpty()) {
                Text("Available promotions:", style = MaterialTheme.typography.labelSmall)
                line.availablePromotions.forEach { promo ->
                    TextButton(onClick = { onPromotionClick(promo) }) {
                        Text(promo.programName)
                    }
                }
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
        // Pricelists table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS pricelists (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                active INTEGER NOT NULL,
                lastModified INTEGER NOT NULL
            )
        """)

        // Pricelist items table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS pricelist_items (
                id INTEGER PRIMARY KEY NOT NULL,
                pricelistId INTEGER NOT NULL,
                productId INTEGER,
                categId INTEGER,
                fixedPrice REAL,
                percentPrice REAL,
                minQuantity REAL NOT NULL,
                FOREIGN KEY (pricelistId) REFERENCES pricelists(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_pricelist_items_pricelistId ON pricelist_items(pricelistId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_pricelist_items_productId ON pricelist_items(productId)")

        // Promotions table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS promotions (
                id INTEGER PRIMARY KEY NOT NULL,
                programId INTEGER NOT NULL,
                programName TEXT NOT NULL,
                minimumQty INTEGER NOT NULL,
                dateFrom TEXT,
                dateTo TEXT,
                productIdsJson TEXT NOT NULL,
                productCategoryId INTEGER,
                rewardType TEXT NOT NULL,
                rewardProductId INTEGER,
                rewardProductQty REAL NOT NULL,
                discountPercent REAL,
                lastModified INTEGER NOT NULL
            )
        """)

        // Add pricelistId to customers
        database.execSQL("ALTER TABLE customers ADD COLUMN pricelistId INTEGER")
    }
}
```

---

## Verification Steps

1. Sync pricelists from Odoo
2. Sync promotions from Odoo
3. Create a customer with a specific pricelist assigned
4. Create an order for that customer
5. Add a product - verify price differs from list price based on pricelist rules
6. Add enough quantity to trigger a promotion
7. Apply promotion - verify reward line added (free product) or discount applied
8. Complete order and sync to Odoo
9. Verify prices in Odoo match app calculations

---

## Notes

- Pricelist rules have priority: product-specific > category > default
- Minimum quantity thresholds affect which rule applies
- Promotions have date validity - check against today's date
- Reward lines should be marked with `isReward = true`
- Some promotions are exclusive (can't combine), handle this logic
- Consider caching price calculations during order creation
