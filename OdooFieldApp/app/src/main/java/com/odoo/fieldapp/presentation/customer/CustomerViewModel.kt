package com.odoo.fieldapp.presentation.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Customer screens
 * 
 * Manages UI state and business logic for customer-related screens
 * Survives configuration changes (like screen rotation)
 */
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Sync state
    private val _syncState = MutableStateFlow<Resource<List<Customer>>?>(null)
    val syncState: StateFlow<Resource<List<Customer>>?> = _syncState.asStateFlow()
    
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
     * Load customer by ID
     */
    fun loadCustomerById(customerId: String) {
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
}
