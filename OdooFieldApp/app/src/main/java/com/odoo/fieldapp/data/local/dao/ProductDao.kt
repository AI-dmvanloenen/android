package com.odoo.fieldapp.data.local.dao

import androidx.room.*
import com.odoo.fieldapp.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Product operations
 *
 * Uses Flow for reactive database queries
 */
@Dao
interface ProductDao {

    /**
     * Get all products as a Flow (reactive)
     * Ordered by name for easy browsing
     */
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    /**
     * Get all products once (non-reactive)
     */
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    /**
     * Get a single product by ID
     */
    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Int): ProductEntity?

    /**
     * Get a product by barcode (for scanner feature)
     */
    @Query("SELECT * FROM products WHERE barcode = :barcode AND active = 1 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    /**
     * Get a product by SKU (default_code)
     */
    @Query("SELECT * FROM products WHERE defaultCode = :sku AND active = 1 LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    /**
     * Search products by name, SKU, or barcode
     */
    @Query("""
        SELECT * FROM products
        WHERE active = 1 AND (
            name LIKE '%' || :query || '%' OR
            defaultCode LIKE '%' || :query || '%' OR
            barcode LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    /**
     * Insert a single product
     * OnConflictStrategy.REPLACE will update if product already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    /**
     * Insert multiple products (batch operation for sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    /**
     * Delete products not in the given set of IDs
     * Used during sync to remove products that no longer exist in Odoo
     */
    @Query("DELETE FROM products WHERE id NOT IN (:ids)")
    suspend fun deleteProductsNotIn(ids: Set<Int>)

    /**
     * Atomic sync operation: deletes stale products and inserts new ones
     */
    @Transaction
    suspend fun syncProducts(products: List<ProductEntity>) {
        val incomingIds = products.map { it.id }.toSet()
        if (incomingIds.isEmpty()) {
            deleteAllProducts()
        } else {
            deleteProductsNotIn(incomingIds)
            insertProducts(products)
        }
    }

    /**
     * Delete a product
     */
    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    /**
     * Delete all products
     */
    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    /**
     * Get count of products
     */
    @Query("SELECT COUNT(*) FROM products WHERE active = 1")
    suspend fun getProductCount(): Int
}
