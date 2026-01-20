package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.VisitDao
import com.odoo.fieldapp.data.local.entity.toDomain
import com.odoo.fieldapp.data.local.entity.toEntity
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.remote.mapper.toDomain
import com.odoo.fieldapp.data.remote.mapper.toRequest
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.model.Visit
import com.odoo.fieldapp.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of VisitRepository
 *
 * This is the single source of truth for visit data.
 * It coordinates between local database (Room) and remote API (Retrofit)
 */
@Singleton
class VisitRepositoryImpl @Inject constructor(
    private val visitDao: VisitDao,
    private val customerDao: CustomerDao,
    private val apiService: OdooApiService,
    private val apiKeyProvider: ApiKeyProvider
) : VisitRepository {

    companion object {
        private const val TAG = "VisitRepository"
    }

    /**
     * Get all visits from local database
     * Returns a Flow that emits whenever the database changes
     */
    override fun getVisits(): Flow<List<Visit>> {
        return visitDao.getAllVisits()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get visits for a specific customer
     * Critical for customer detail screen
     */
    override fun getVisitsByCustomer(customerId: Int): Flow<List<Visit>> {
        return visitDao.getVisitsByCustomer(customerId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get a single visit by ID (Odoo record ID)
     */
    override suspend fun getVisitById(visitId: Int): Visit? {
        return visitDao.getVisitById(visitId)?.toDomain()
    }

    /**
     * Sync visits from Odoo API to local database
     *
     * Flow of Resource states:
     * 1. Emit Loading with existing local data
     * 2. Fetch from API
     * 3. Save to local database (in transaction)
     * 4. Emit Success with updated data
     * 5. If error, emit Error with message
     */
    override suspend fun syncVisitsFromOdoo(): Flow<Resource<List<Visit>>> = flow {
        // 1. Emit loading state with current local data
        val localVisits = visitDao.getAllVisits().first()
        emit(Resource.Loading(data = localVisits.map { it.toDomain() }))

        // 2. Get API key
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            val cachedData = localVisits.map { it.toDomain() }
            emit(Resource.Error("API key not configured. Please add your API key in settings.", data = cachedData))
            return@flow
        }

        // 3. Always do full sync for visits (no incremental sync for now)
        Log.d(TAG, "Performing full visits sync")

        // 4. Fetch all visits from Odoo API
        val response = apiService.getVisits(apiKey, null)

        if (response.isSuccessful) {
            val paginatedResponse = response.body()
            val visitResponses = paginatedResponse?.data ?: emptyList()

            // 5. Filter out records with null IDs (prevents primary key conflicts)
            val validResponses = visitResponses.filter { it.id != null }

            // 6. Convert API responses to domain models
            val visits = validResponses.map { it.toDomain() }
            Log.d(TAG, "Fetched ${visits.size} visits from API")

            // 7. Resolve partner names from local database
            val enrichedVisits = visits.map { visit ->
                val partnerName = customerDao.getCustomerById(visit.partnerId)?.name
                visit.copy(partnerName = partnerName)
            }

            // 8. Save visits to local database (full replace)
            val entities = enrichedVisits.map { it.toEntity() }
            visitDao.syncVisits(entities)
            Log.d(TAG, "Synced ${entities.size} visits")

            // 9. Emit success
            emit(Resource.Success(enrichedVisits))
        } else {
            // API error - return cached data
            val cachedData = localVisits.map { it.toDomain() }
            val errorMessage = when (response.code()) {
                401 -> "Authentication failed. Please check your API key."
                403 -> "Access denied. Your API key doesn't have permission."
                404 -> "Visits endpoint not found. Please check the server configuration."
                500 -> "Odoo server error. Please try again later."
                else -> "Failed to sync visits: ${response.message()}"
            }
            emit(Resource.Error(errorMessage, data = cachedData))
        }
    }.catch { e ->
        // Log the full exception for debugging
        Log.e(TAG, "Visit sync failed", e)

        // Network or other error - return cached data
        val cachedData = visitDao.getAllVisits().first().map { it.toDomain() }
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

    /**
     * Create a new visit and sync to Odoo
     *
     * Flow:
     * 1. Validate input data
     * 2. Generate UUID for mobileUid
     * 3. Save locally with temporary negative ID and syncState = PENDING
     * 4. Call API with Bearer token
     * 5. On success: update local record with Odoo ID, set SYNCED
     * 6. On failure: keep PENDING for retry, emit error
     */
    override suspend fun createVisit(visit: Visit): Flow<Resource<Visit>> = flow {
        // 1. Validate input data
        if (visit.partnerId <= 0) {
            emit(Resource.Error("Invalid customer: partnerId must be a positive number"))
            return@flow
        }

        // Validate visitDatetime is not too far in the future (allow 1 hour buffer for timezone issues)
        val oneHourFromNow = Date(System.currentTimeMillis() + 60 * 60 * 1000)
        if (visit.visitDatetime.after(oneHourFromNow)) {
            emit(Resource.Error("Invalid visit date: cannot be in the future"))
            return@flow
        }

        // 2. Emit loading
        emit(Resource.Loading())

        // 2. Generate UUID for mobileUid
        val mobileUid = UUID.randomUUID().toString()

        // 3. Generate temporary negative ID for local storage
        val minId = visitDao.getMinVisitId() ?: 0
        val tempId = if (minId >= 0) -1 else minId - 1

        // 4. Resolve partner name for local display
        val partnerName = customerDao.getCustomerById(visit.partnerId)?.name

        // 5. Create visit with PENDING state
        val pendingVisit = visit.copy(
            id = tempId,
            mobileUid = mobileUid,
            partnerName = partnerName,
            syncState = SyncState.PENDING,
            lastModified = Date()
        )

        // 6. Save locally
        visitDao.insertVisit(pendingVisit.toEntity())
        Log.d(TAG, "Visit saved locally with tempId=$tempId, mobileUid=$mobileUid")

        // 7. Get API key
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(Resource.Error("API key not configured. Visit saved locally.", data = pendingVisit))
            return@flow
        }

        // 8. Call API to create visit
        val request = pendingVisit.toRequest()
        val response = apiService.createVisits(apiKey, listOf(request))

        if (response.isSuccessful) {
            val createResponse = response.body()
            val createdVisit = createResponse?.data?.firstOrNull { it.mobileUid == mobileUid }

            if (createdVisit != null && createdVisit.id != null) {
                // 9. Success: Update local record with Odoo ID
                val syncedVisit = createdVisit.toDomain().copy(
                    partnerName = partnerName,
                    syncState = SyncState.SYNCED
                )

                // Delete the temp record and insert with real ID
                visitDao.deleteVisitById(tempId)
                visitDao.insertVisit(syncedVisit.toEntity())

                Log.d(TAG, "Visit synced successfully, odooId=${syncedVisit.id}")
                emit(Resource.Success(syncedVisit))
            } else {
                // API returned success but no matching visit
                Log.w(TAG, "API response didn't include created visit")
                emit(Resource.Error("Visit created but response incomplete", data = pendingVisit))
            }
        } else {
            // API error - visit remains in PENDING state
            val errorMessage = when (response.code()) {
                401 -> "Authentication failed. Please check your API key."
                403 -> "Access denied. Your API key doesn't have permission."
                404 -> "Endpoint not found. Please check the base URL."
                500 -> "Odoo server error. Please try again later."
                else -> "Failed to sync: ${response.message()}"
            }
            Log.e(TAG, "Visit creation API failed: $errorMessage")
            emit(Resource.Error(errorMessage, data = pendingVisit))
        }
    }.catch { e ->
        Log.e(TAG, "Visit creation failed", e)

        val errorMessage = when {
            e.message?.contains("Unable to resolve host") == true ->
                "Network error. Visit saved locally."
            e.message?.contains("timeout") == true ->
                "Request timed out. Visit saved locally."
            else ->
                "Creation failed: ${e.message ?: "Unknown error"}"
        }
        emit(Resource.Error(errorMessage))
    }
}
