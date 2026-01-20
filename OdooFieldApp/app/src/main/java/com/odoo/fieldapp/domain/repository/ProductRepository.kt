package com.odoo.fieldapp.domain.repository

import com.odoo.fieldapp.domain.model.Product
import com.odoo.fieldapp.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Product operations
 *
 * This defines the contract for data operations.
 * The implementation will handle both local (Room) and remote (API) data sources.
 */
interface ProductRepository {

    /**
     * Get all products from local database as a Flow
     * UI will automatically update when data changes
     */
    fun getProducts(): Flow<List<Product>>

    /**
     * Get a single product by ID (Odoo record ID) from local database
     */
    suspend fun getProductById(productId: Int): Product?

    /**
     * Search products by name, SKU, or barcode
     */
    fun searchProducts(query: String): Flow<List<Product>>

    /**
     * Get product by barcode (for future scanner feature)
     */
    suspend fun getProductByBarcode(barcode: String): Product?

    /**
     * Get product by SKU (default_code)
     */
    suspend fun getProductBySku(sku: String): Product?

    /**
     * Sync products from Odoo API to local database
     *
     * @return Resource wrapper with loading/success/error states
     */
    suspend fun syncProductsFromOdoo(): Flow<Resource<List<Product>>>
}
