package com.odoo.fieldapp.presentation.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.repository.CustomerRepository
import com.odoo.fieldapp.domain.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Sale screens
 *
 * Manages UI state and business logic for sale-related screens
 * Survives configuration changes (like screen rotation)
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SaleViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sync state
    private val _syncState = MutableStateFlow<Resource<List<Sale>>?>(null)
    val syncState: StateFlow<Resource<List<Sale>>?> = _syncState.asStateFlow()

    // Sales list (filtered by search query)
    val sales: StateFlow<List<Sale>> = searchQuery
        .debounce(300)  // Wait 300ms after user stops typing
        .flatMapLatest { query ->
            if (query.isBlank()) {
                saleRepository.getSales()
            } else {
                saleRepository.searchSales(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected sale for detail view
    private val _selectedSale = MutableStateFlow<Sale?>(null)
    val selectedSale: StateFlow<Sale?> = _selectedSale.asStateFlow()

    // Customer name for the selected sale
    private val _customerName = MutableStateFlow<String?>(null)
    val customerName: StateFlow<String?> = _customerName.asStateFlow()

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
     * Sync sales from Odoo
     */
    fun syncSales() {
        viewModelScope.launch {
            saleRepository.syncSalesFromOdoo()
                .collect { resource ->
                    _syncState.value = resource
                }
        }
    }

    /**
     * Select a sale for detail view
     */
    fun selectSale(sale: Sale) {
        _selectedSale.value = sale
    }

    /**
     * Clear selected sale
     */
    fun clearSelectedSale() {
        _selectedSale.value = null
        _customerName.value = null
    }

    /**
     * Load sale by ID (Odoo record ID)
     * Also loads the customer name if the sale has a partner_id
     */
    fun loadSaleById(saleId: Int) {
        viewModelScope.launch {
            val sale = saleRepository.getSaleById(saleId)
            _selectedSale.value = sale

            // Load customer name if sale has a partner_id
            if (sale?.partnerId != null) {
                val customer = customerRepository.getCustomerById(sale.partnerId)
                _customerName.value = customer?.name
            } else {
                _customerName.value = null
            }
        }
    }

    /**
     * Clear sync state (dismiss success/error messages)
     */
    fun clearSyncState() {
        _syncState.value = null
    }
}
