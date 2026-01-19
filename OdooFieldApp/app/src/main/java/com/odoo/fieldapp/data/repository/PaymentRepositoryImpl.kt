package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.PaymentDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.data.remote.mapper.toRequest
import com.odoo.fieldapp.domain.model.Payment
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PaymentRepository
 *
 * This is the single source of truth for payment data.
 * It coordinates between local database (Room) and remote API (Retrofit)
 */
@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
    private val customerDao: CustomerDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : PaymentRepository {

    companion object {
        private const val TAG = "PaymentRepository"
    }

    /**
     * Get payments from local database
     * Returns a Flow that emits whenever the database changes
     */
    override fun getPayments(): Flow<List<Payment>> {
        return paymentDao.getAllPayments()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get a single payment by ID (Odoo record ID)
     */
    override suspend fun getPaymentById(paymentId: Int): Payment? {
        return paymentDao.getPaymentById(paymentId)?.toDomain()
    }

    /**
     * Search payments by name or memo
     */
    override fun searchPayments(query: String): Flow<List<Payment>> {
        return paymentDao.searchPayments(query)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get payments for a specific customer
     */
    override fun getPaymentsByCustomer(customerId: Int): Flow<List<Payment>> {
        return paymentDao.getPaymentsByCustomer(customerId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Sync payments from Odoo API to local database
     *
     * Flow of Resource states:
     * 1. Emit Loading with existing local data
     * 2. Fetch from API
     * 3. Resolve partner names from local data
     * 4. Save to local database (in transaction)
     * 5. Emit Success with updated data
     * 6. If error, emit Error with message
     */
    override suspend fun syncPaymentsFromOdoo(): Flow<Resource<List<Payment>>> = flow {
        try {
            // 1. Emit loading state with current local data
            val localPayments = paymentDao.getAllPaymentsOnce()
            emit(Resource.Loading(data = localPayments.map { it.toDomain() }))

            // 2. Get API key
            val apiKey = apiKeyProvider.getApiKey()
            if (apiKey.isNullOrBlank()) {
                emit(Resource.Error("API key not configured. Please add your API key in settings."))
                return@flow
            }

            // 3. Fetch from Odoo API
            val response = apiService.getPayments(apiKey)

            if (response.isSuccessful) {
                val paginatedResponse = response.body()
                val paymentResponses = paginatedResponse?.data ?: emptyList()

                // 4. Filter out records with null IDs (prevents primary key conflicts)
                val validResponses = paymentResponses.filter { it.id != null }

                // 5. Convert API responses to domain models
                val allPayments = validResponses.map { it.toDomain() }

                // 6. Resolve partner names from local database
                val enrichedPayments = allPayments.map { payment ->
                    val partnerName = payment.partnerId?.let { customerId ->
                        customerDao.getCustomerById(customerId)?.name
                    }
                    payment.copy(partnerName = partnerName)
                }

                // 7. Save to local database (atomic transaction)
                val entities = enrichedPayments.map { it.toEntity() }
                paymentDao.syncPayments(entities)

                // 8. Emit success
                emit(Resource.Success(enrichedPayments))
            } else {
                // API error
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed. Please check your API key."
                    403 -> "Access denied. Your API key doesn't have permission."
                    404 -> "Payments endpoint not found. Please check the server configuration."
                    500 -> "Odoo server error. Please try again later."
                    else -> "Failed to sync payments: ${response.message()}"
                }
                emit(Resource.Error(errorMessage))
            }

        } catch (e: Exception) {
            // Log the full exception for debugging
            Log.e(TAG, "Payment sync failed", e)

            // Network or other error
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "Network error. Please check your internet connection."
                e.message?.contains("timeout") == true ->
                    "Request timed out. Please try again."
                else ->
                    "Sync failed: ${e.message ?: "Unknown error"}"
            }
            emit(Resource.Error(errorMessage))
        }
    }

    /**
     * Create a new payment and sync to Odoo
     *
     * Flow:
     * 1. Generate UUID for mobileUid
     * 2. Save locally with temporary negative ID and syncState = PENDING
     * 3. Call API with Bearer token
     * 4. On success: update local record with Odoo ID, set SYNCED
     * 5. On failure: keep PENDING for retry, emit error
     */
    override suspend fun createPayment(payment: Payment): Flow<Resource<Payment>> = flow {
        try {
            // 1. Emit loading
            emit(Resource.Loading())

            // 2. Generate UUID for mobileUid
            val mobileUid = UUID.randomUUID().toString()

            // 3. Generate temporary negative ID for local storage
            val minId = paymentDao.getMinPaymentId() ?: 0
            val tempId = if (minId >= 0) -1 else minId - 1

            // 4. Create payment with PENDING state
            val pendingPayment = payment.copy(
                id = tempId,
                mobileUid = mobileUid,
                syncState = SyncState.PENDING,
                lastModified = Date()
            )

            // 5. Save locally
            paymentDao.insertPayment(pendingPayment.toEntity())
            Log.d(TAG, "Payment saved locally with tempId=$tempId, mobileUid=$mobileUid")

            // 6. Get API key
            val apiKey = apiKeyProvider.getApiKey()
            if (apiKey.isNullOrBlank()) {
                emit(Resource.Error("API key not configured. Payment saved locally.", data = pendingPayment))
                return@flow
            }

            // 7. Call API to create payment
            val request = pendingPayment.toRequest()
            val response = apiService.createPayments(apiKey, listOf(request))

            if (response.isSuccessful) {
                val createResponse = response.body()
                val createdPayment = createResponse?.data?.firstOrNull { it.mobileUid == mobileUid }

                if (createdPayment != null && createdPayment.id != null) {
                    // 8. Success: Convert to domain and update local record
                    val syncedPayment = createdPayment.toDomain()

                    // Resolve partner name
                    val partnerName = syncedPayment.partnerId?.let { customerId ->
                        customerDao.getCustomerById(customerId)?.name
                    }
                    val enrichedPayment = syncedPayment.copy(partnerName = partnerName)

                    // Delete the temp record and insert with real ID
                    paymentDao.deletePaymentById(tempId)
                    paymentDao.insertPayment(enrichedPayment.toEntity())

                    Log.d(TAG, "Payment synced successfully, odooId=${enrichedPayment.id}")
                    emit(Resource.Success(enrichedPayment))
                } else {
                    // API returned success but no matching payment
                    Log.w(TAG, "API response didn't include created payment")
                    emit(Resource.Error("Payment created but response incomplete", data = pendingPayment))
                }
            } else {
                // API error - payment remains in PENDING state
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed. Please check your API key."
                    403 -> "Access denied. Your API key doesn't have permission."
                    404 -> "Endpoint not found. Please check the base URL."
                    500 -> "Odoo server error. Please try again later."
                    else -> "Failed to sync: ${response.message()}"
                }
                Log.e(TAG, "Payment creation API failed: $errorMessage")
                emit(Resource.Error(errorMessage, data = pendingPayment))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Payment creation failed", e)

            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "Network error. Payment saved locally."
                e.message?.contains("timeout") == true ->
                    "Request timed out. Payment saved locally."
                else ->
                    "Creation failed: ${e.message ?: "Unknown error"}"
            }
            emit(Resource.Error(errorMessage))
        }
    }
}
