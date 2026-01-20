package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.ProductDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.domain.model.Product
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProductRepository
 *
 * This is the single source of truth for product data.
 * It coordinates between local database (Room) and remote API (Retrofit)
 */
@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : ProductRepository {

    companion object {
        private const val TAG = "ProductRepository"
    }

    /**
     * Get products from local database
     * Returns a Flow that emits whenever the database changes
     */
    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProducts()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get a single product by ID (Odoo record ID)
     */
    override suspend fun getProductById(productId: Int): Product? {
        return productDao.getProductById(productId)?.toDomain()
    }

    /**
     * Search products by name, SKU, or barcode
     */
    override fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get product by barcode (for future scanner feature)
     */
    override suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)?.toDomain()
    }

    /**
     * Get product by SKU (default_code)
     */
    override suspend fun getProductBySku(sku: String): Product? {
        return productDao.getProductBySku(sku)?.toDomain()
    }

    /**
     * Sync products from Odoo API to local database
     *
     * Flow of Resource states:
     * 1. Emit Loading with existing local data
     * 2. Fetch from API
     * 3. Save to local database (in transaction)
     * 4. Emit Success with updated data
     * 5. If error, emit Error with message
     */
    override suspend fun syncProductsFromOdoo(): Flow<Resource<List<Product>>> = flow {
        // 1. Emit loading state with current local data
        val localProducts = productDao.getAllProductsOnce()
        emit(Resource.Loading(data = localProducts.map { it.toDomain() }))

        // 2. Get API key
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            val cachedData = localProducts.map { it.toDomain() }
            emit(Resource.Error("API key not configured. Please add your API key in settings.", data = cachedData))
            return@flow
        }

        // 3. Always do full sync for products (no incremental sync for now)
        Log.d(TAG, "Performing full products sync")

        // 4. Fetch all products from Odoo API
        val response = apiService.getProducts(apiKey, null)

        if (response.isSuccessful) {
            val paginatedResponse = response.body()
            val productResponses = paginatedResponse?.data ?: emptyList()

            // 5. Filter out records with null IDs (prevents primary key conflicts)
            val validResponses = productResponses.filter { it.id != null }

            // 6. Convert API responses to domain models
            val products = validResponses.map { it.toDomain() }

            // 7. Save to local database (full replace)
            val entities = products.map { it.toEntity() }
            productDao.syncProducts(entities)
            Log.d(TAG, "Synced ${entities.size} products")

            // 8. Emit success
            emit(Resource.Success(products))
        } else {
            // API error - return cached data
            val cachedData = localProducts.map { it.toDomain() }
            val errorMessage = when (response.code()) {
                401 -> "Authentication failed. Please check your API key."
                403 -> "Access denied. Your API key doesn't have permission."
                404 -> "Products endpoint not found. Please check the server configuration."
                500 -> "Odoo server error. Please try again later."
                else -> "Failed to sync products: ${response.message()}"
            }
            emit(Resource.Error(errorMessage, data = cachedData))
        }
    }.catch { e ->
        // Log the full exception for debugging
        Log.e(TAG, "Product sync failed", e)

        // Network or other error - return cached data
        val cachedData = productDao.getAllProductsOnce().map { it.toDomain() }
        val errorMessage = when {
            e.message?.contains("Unable to resolve host") == true ->
                "Network error. Please check your internet connection."
            e.message?.contains("timeout") == true ->
                "Request timed out. Please try again."
            else ->
                "Sync failed: ${e.message ?: "Unknown error"}"
        }
        emit(Resource.Error(errorMessage, data = cachedData))
    }
}
