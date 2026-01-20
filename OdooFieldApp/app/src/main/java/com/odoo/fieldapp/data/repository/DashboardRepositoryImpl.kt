package com.odoo.fieldapp.data.repository

import android.util.Log
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.DeliveryDao
import com.odoo.fieldapp.data.local.dao.PaymentDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.domain.model.DashboardStats
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.repository.CustomerRepository
import com.odoo.fieldapp.domain.repository.DashboardRepository
import com.odoo.fieldapp.domain.repository.DeliveryRepository
import com.odoo.fieldapp.domain.repository.PaymentRepository
import com.odoo.fieldapp.domain.repository.ProductRepository
import com.odoo.fieldapp.domain.repository.SaleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DashboardRepository
 *
 * Aggregates statistics from all DAOs and coordinates sync operations
 */
@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val deliveryDao: DeliveryDao,
    private val paymentDao: PaymentDao,
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val deliveryRepository: DeliveryRepository,
    private val paymentRepository: PaymentRepository,
    private val productRepository: ProductRepository
) : DashboardRepository {

    companion object {
        private const val TAG = "DashboardRepository"
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    /**
     * Get dashboard statistics as a Flow
     * Combines counts from all DAOs and emits new values when any count changes
     * Note: Today's date is calculated when the flow is collected. For midnight rollover,
     * the user should restart the app or navigate away and back to the dashboard.
     */
    override fun getDashboardStats(): Flow<DashboardStats> {
        val today = dateFormatter.format(Date())

        // Combine delivery and payment counts (up to 5 flows per combine call)
        val deliveryStats = combine(
            deliveryDao.countPendingDeliveries(),
            deliveryDao.countTodaysDeliveries(today),
            deliveryDao.countDeliverySyncErrors()
        ) { pending, todays, errors ->
            Triple(pending, todays, errors)
        }

        val otherStats = combine(
            paymentDao.countDraftPayments(),
            paymentDao.countPaymentSyncErrors(),
            customerDao.countCustomerSyncErrors(),
            saleDao.countSaleSyncErrors()
        ) { draftPayments, paymentErrors, customerErrors, saleErrors ->
            Triple(draftPayments, paymentErrors + customerErrors + saleErrors, 0)
        }

        return combine(deliveryStats, otherStats) { delivery, other ->
            val (pendingDeliveries, todaysDeliveries, deliveryErrors) = delivery
            val (draftPayments, otherErrors, _) = other

            DashboardStats(
                deliveriesToComplete = pendingDeliveries,
                todaysDeliveries = todaysDeliveries,
                pendingPayments = draftPayments,
                syncErrors = deliveryErrors + otherErrors
            )
        }
    }

    /**
     * Sync all entities from Odoo
     * Calls each repository's sync method in parallel using coroutines
     */
    override suspend fun syncAll(): Resource<Unit> {
        return try {
            Log.d(TAG, "Starting sync all")

            // Run all sync operations in parallel
            coroutineScope {
                val customerDeferred = async {
                    customerRepository.syncCustomersFromOdoo().first { it !is Resource.Loading }
                }
                val saleDeferred = async {
                    saleRepository.syncSalesFromOdoo().first { it !is Resource.Loading }
                }
                val deliveryDeferred = async {
                    deliveryRepository.syncDeliveriesFromOdoo().first { it !is Resource.Loading }
                }
                val paymentDeferred = async {
                    paymentRepository.syncPaymentsFromOdoo().first { it !is Resource.Loading }
                }
                val productDeferred = async {
                    productRepository.syncProductsFromOdoo().first { it !is Resource.Loading }
                }

                // Await all results
                val customerResult = customerDeferred.await()
                val saleResult = saleDeferred.await()
                val deliveryResult = deliveryDeferred.await()
                val paymentResult = paymentDeferred.await()
                val productResult = productDeferred.await()

                // Check for errors
                val errors = mutableListOf<String>()
                if (customerResult is Resource.Error) errors.add("Customers: ${customerResult.message}")
                if (saleResult is Resource.Error) errors.add("Sales: ${saleResult.message}")
                if (deliveryResult is Resource.Error) errors.add("Deliveries: ${deliveryResult.message}")
                if (paymentResult is Resource.Error) errors.add("Payments: ${paymentResult.message}")
                if (productResult is Resource.Error) errors.add("Products: ${productResult.message}")

                if (errors.isNotEmpty()) {
                    Log.e(TAG, "Sync completed with errors: $errors")
                    Resource.Error(errors.joinToString("\n"))
                } else {
                    Log.d(TAG, "Sync all completed successfully")
                    Resource.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync all failed", e)
            Resource.Error("Sync failed: ${e.message ?: "Unknown error"}")
        }
    }
}
