package com.odoo.fieldapp.presentation.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.repository.CustomerRepository
import com.odoo.fieldapp.domain.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for Customer screens
 *
 * Manages UI state and business logic for customer-related screens
 * Survives configuration changes (like screen rotation)
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Sync state
    private val _syncState = MutableStateFlow<Resource<List<Customer>>?>(null)
    val syncState: StateFlow<Resource<List<Customer>>?> = _syncState.asStateFlow()

    // === Create Customer Form State ===
    private val _createName = MutableStateFlow("")
    val createName: StateFlow<String> = _createName.asStateFlow()

    private val _createCity = MutableStateFlow("")
    val createCity: StateFlow<String> = _createCity.asStateFlow()

    private val _createTaxId = MutableStateFlow("")
    val createTaxId: StateFlow<String> = _createTaxId.asStateFlow()

    private val _createEmail = MutableStateFlow("")
    val createEmail: StateFlow<String> = _createEmail.asStateFlow()

    private val _createPhone = MutableStateFlow("")
    val createPhone: StateFlow<String> = _createPhone.asStateFlow()

    private val _createWebsite = MutableStateFlow("")
    val createWebsite: StateFlow<String> = _createWebsite.asStateFlow()

    // Validation errors
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    // Create operation state
    private val _createState = MutableStateFlow<Resource<Customer>?>(null)
    val createState: StateFlow<Resource<Customer>?> = _createState.asStateFlow()
    
    // Customers list (filtered by search query)
    val customers: StateFlow<List<Customer>> = searchQuery
        .debounce(300)  // Wait 300ms after user stops typing
        .flatMapLatest { query ->
            if (query.isBlank()) {
                customerRepository.getCustomers()
            } else {
                customerRepository.searchCustomers(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Selected customer for detail view
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    // Sales for the selected customer
    val salesForCustomer: StateFlow<List<Sale>> = _selectedCustomer
        .flatMapLatest { customer ->
            if (customer != null) {
                saleRepository.getSalesByCustomer(customer.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
     * Sync customers from Odoo
     */
    fun syncCustomers() {
        viewModelScope.launch {
            customerRepository.syncCustomersFromOdoo()
                .collect { resource ->
                    _syncState.value = resource
                }
        }
    }
    
    /**
     * Select a customer for detail view
     */
    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
    }
    
    /**
     * Clear selected customer
     */
    fun clearSelectedCustomer() {
        _selectedCustomer.value = null
    }
    
    /**
     * Load customer by ID (Odoo record ID)
     */
    fun loadCustomerById(customerId: Int) {
        viewModelScope.launch {
            val customer = customerRepository.getCustomerById(customerId)
            _selectedCustomer.value = customer
        }
    }
    
    /**
     * Clear sync state (dismiss success/error messages)
     */
    fun clearSyncState() {
        _syncState.value = null
    }

    // === Create Customer Form Methods ===

    fun onCreateNameChange(name: String) {
        _createName.value = name
        // Clear error when user starts typing
        if (name.isNotBlank()) {
            _nameError.value = null
        }
    }

    fun onCreateCityChange(city: String) {
        _createCity.value = city
    }

    fun onCreateTaxIdChange(taxId: String) {
        _createTaxId.value = taxId
    }

    fun onCreateEmailChange(email: String) {
        _createEmail.value = email
    }

    fun onCreatePhoneChange(phone: String) {
        _createPhone.value = phone
    }

    fun onCreateWebsiteChange(website: String) {
        _createWebsite.value = website
    }

    /**
     * Validate the create form
     * @return true if valid, false otherwise
     */
    private fun validateCreateForm(): Boolean {
        val name = _createName.value.trim()
        if (name.isBlank()) {
            _nameError.value = "Name is required"
            return false
        }
        _nameError.value = null
        return true
    }

    /**
     * Create a new customer
     */
    fun createCustomer() {
        if (!validateCreateForm()) {
            return
        }

        viewModelScope.launch {
            val customer = Customer(
                id = 0, // Will be assigned by repository
                name = _createName.value.trim(),
                city = _createCity.value.trim().ifBlank { null },
                taxId = _createTaxId.value.trim().ifBlank { null },
                email = _createEmail.value.trim().ifBlank { null },
                phone = _createPhone.value.trim().ifBlank { null },
                website = _createWebsite.value.trim().ifBlank { null },
                date = null,
                syncState = SyncState.PENDING,
                lastModified = Date()
            )

            customerRepository.createCustomer(customer)
                .collect { resource ->
                    _createState.value = resource
                }
        }
    }

    /**
     * Clear create form and state after successful creation or when navigating away
     */
    fun clearCreateForm() {
        _createName.value = ""
        _createCity.value = ""
        _createTaxId.value = ""
        _createEmail.value = ""
        _createPhone.value = ""
        _createWebsite.value = ""
        _nameError.value = null
        _createState.value = null
    }

    /**
     * Clear create state (dismiss success/error messages)
     */
    fun clearCreateState() {
        _createState.value = null
    }
}
