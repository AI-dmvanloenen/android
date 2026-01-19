package com.odoo.fieldapp.presentation.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.DeliveryLine
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.repository.DeliveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Delivery screens
 *
 * Manages UI state and business logic for delivery-related screens
 * Survives configuration changes (like screen rotation)
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sync state
    private val _syncState = MutableStateFlow<Resource<List<Delivery>>?>(null)
    val syncState: StateFlow<Resource<List<Delivery>>?> = _syncState.asStateFlow()

    // Deliveries list (filtered by search query)
    val deliveries: StateFlow<List<Delivery>> = searchQuery
        .debounce(300)  // Wait 300ms after user stops typing
        .flatMapLatest { query ->
            if (query.isBlank()) {
                deliveryRepository.getDeliveries()
            } else {
                deliveryRepository.searchDeliveries(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected delivery for detail view
    private val _selectedDelivery = MutableStateFlow<Delivery?>(null)
    val selectedDelivery: StateFlow<Delivery?> = _selectedDelivery.asStateFlow()

    // Delivery lines for selected delivery
    private val _deliveryLines = MutableStateFlow<List<DeliveryLine>>(emptyList())
    val deliveryLines: StateFlow<List<DeliveryLine>> = _deliveryLines.asStateFlow()

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
     * Sync deliveries from Odoo
     */
    fun syncDeliveries() {
        viewModelScope.launch {
            deliveryRepository.syncDeliveriesFromOdoo()
                .collect { resource ->
                    _syncState.value = resource
                }
        }
    }

    /**
     * Select a delivery for detail view
     */
    fun selectDelivery(delivery: Delivery) {
        _selectedDelivery.value = delivery
        _deliveryLines.value = delivery.lines
    }

    /**
     * Clear selected delivery
     */
    fun clearSelectedDelivery() {
        _selectedDelivery.value = null
        _deliveryLines.value = emptyList()
    }

    /**
     * Load delivery by ID (Odoo record ID)
     */
    fun loadDeliveryById(deliveryId: Int) {
        viewModelScope.launch {
            val delivery = deliveryRepository.getDeliveryById(deliveryId)
            _selectedDelivery.value = delivery
            _deliveryLines.value = delivery?.lines ?: emptyList()
        }
    }

    /**
     * Clear sync state (dismiss success/error messages)
     */
    fun clearSyncState() {
        _syncState.value = null
    }
}
