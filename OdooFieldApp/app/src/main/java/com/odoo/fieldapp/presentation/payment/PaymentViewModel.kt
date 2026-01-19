package com.odoo.fieldapp.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.domain.model.Payment
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for Payment screens
 *
 * Manages UI state and business logic for payment-related screens
 * Survives configuration changes (like screen rotation)
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sync state
    private val _syncState = MutableStateFlow<Resource<List<Payment>>?>(null)
    val syncState: StateFlow<Resource<List<Payment>>?> = _syncState.asStateFlow()

    // Payments list (filtered by search query)
    val payments: StateFlow<List<Payment>> = searchQuery
        .debounce(300)  // Wait 300ms after user stops typing
        .flatMapLatest { query ->
            if (query.isBlank()) {
                paymentRepository.getPayments()
            } else {
                paymentRepository.searchPayments(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected payment for detail view
    private val _selectedPayment = MutableStateFlow<Payment?>(null)
    val selectedPayment: StateFlow<Payment?> = _selectedPayment.asStateFlow()

    // === Create Payment Form State ===
    private val _createPartnerId = MutableStateFlow<Int?>(null)
    val createPartnerId: StateFlow<Int?> = _createPartnerId.asStateFlow()

    private val _createPartnerName = MutableStateFlow("")
    val createPartnerName: StateFlow<String> = _createPartnerName.asStateFlow()

    private val _createAmount = MutableStateFlow("")
    val createAmount: StateFlow<String> = _createAmount.asStateFlow()

    private val _createMemo = MutableStateFlow("")
    val createMemo: StateFlow<String> = _createMemo.asStateFlow()

    private val _createDate = MutableStateFlow<Date?>(Date())
    val createDate: StateFlow<Date?> = _createDate.asStateFlow()

    // Validation errors
    private val _partnerError = MutableStateFlow<String?>(null)
    val partnerError: StateFlow<String?> = _partnerError.asStateFlow()

    private val _amountError = MutableStateFlow<String?>(null)
    val amountError: StateFlow<String?> = _amountError.asStateFlow()

    // Create operation state
    private val _createState = MutableStateFlow<Resource<Payment>?>(null)
    val createState: StateFlow<Resource<Payment>?> = _createState.asStateFlow()

    /**
     * Update search query
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Sync payments from Odoo
     */
    fun syncPayments() {
        viewModelScope.launch {
            paymentRepository.syncPaymentsFromOdoo()
                .collect { resource ->
                    _syncState.value = resource
                }
        }
    }

    /**
     * Select a payment for detail view
     */
    fun selectPayment(payment: Payment) {
        _selectedPayment.value = payment
    }

    /**
     * Clear selected payment
     */
    fun clearSelectedPayment() {
        _selectedPayment.value = null
    }

    /**
     * Load payment by ID (Odoo record ID)
     */
    fun loadPaymentById(paymentId: Int) {
        viewModelScope.launch {
            val payment = paymentRepository.getPaymentById(paymentId)
            _selectedPayment.value = payment
        }
    }

    /**
     * Clear sync state (dismiss success/error messages)
     */
    fun clearSyncState() {
        _syncState.value = null
    }

    // === Create Payment Form Methods ===

    fun onCreatePartnerChange(partnerId: Int?, partnerName: String) {
        _createPartnerId.value = partnerId
        _createPartnerName.value = partnerName
        // Clear error when user selects a partner
        if (partnerId != null) {
            _partnerError.value = null
        }
    }

    fun onCreateAmountChange(amount: String) {
        _createAmount.value = amount
        // Clear error when user starts typing
        if (amount.isNotBlank()) {
            _amountError.value = null
        }
    }

    fun onCreateMemoChange(memo: String) {
        _createMemo.value = memo
    }

    fun onCreateDateChange(date: Date?) {
        _createDate.value = date
    }

    /**
     * Validate the create form
     * @return true if valid, false otherwise
     */
    private fun validateCreateForm(): Boolean {
        var isValid = true

        if (_createPartnerId.value == null) {
            _partnerError.value = "Customer is required"
            isValid = false
        } else {
            _partnerError.value = null
        }

        val amountStr = _createAmount.value.trim()
        if (amountStr.isBlank()) {
            _amountError.value = "Amount is required"
            isValid = false
        } else {
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                _amountError.value = "Please enter a valid amount"
                isValid = false
            } else {
                _amountError.value = null
            }
        }

        return isValid
    }

    /**
     * Create a new payment
     */
    fun createPayment() {
        if (!validateCreateForm()) {
            return
        }

        viewModelScope.launch {
            val payment = Payment(
                id = 0, // Will be assigned by repository
                mobileUid = null, // Will be generated by repository
                name = "", // Will be assigned by Odoo
                partnerId = _createPartnerId.value,
                partnerName = _createPartnerName.value.ifBlank { null },
                amount = _createAmount.value.toDoubleOrNull() ?: 0.0,
                date = _createDate.value,
                memo = _createMemo.value.trim().ifBlank { null },
                journalId = null, // Odoo will use default bank journal
                state = "draft",
                syncState = SyncState.PENDING,
                lastModified = Date()
            )

            paymentRepository.createPayment(payment)
                .collect { resource ->
                    _createState.value = resource
                }
        }
    }

    /**
     * Clear create form and state after successful creation or when navigating away
     */
    fun clearCreateForm() {
        _createPartnerId.value = null
        _createPartnerName.value = ""
        _createAmount.value = ""
        _createMemo.value = ""
        _createDate.value = Date()
        _partnerError.value = null
        _amountError.value = null
        _createState.value = null
    }

    /**
     * Clear create state (dismiss success/error messages)
     */
    fun clearCreateState() {
        _createState.value = null
    }
}
