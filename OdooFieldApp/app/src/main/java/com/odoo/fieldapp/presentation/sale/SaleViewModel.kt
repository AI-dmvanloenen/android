package com.odoo.fieldapp.presentation.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
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
    private val saleRepository: SaleRepository
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
    }

    /**
     * Load sale by ID (Odoo record ID)
     */
    fun loadSaleById(saleId: Int) {
        viewModelScope.launch {
            val sale = saleRepository.getSaleById(saleId)
            _selectedSale.value = sale
        }
    }

    /**
     * Clear sync state (dismiss success/error messages)
     */
    fun clearSyncState() {
        _syncState.value = null
    }
}
