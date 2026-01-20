package com.odoo.fieldapp.presentation.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.domain.model.Product
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.model.SaleLine
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.repository.CustomerRepository
import com.odoo.fieldapp.domain.repository.ProductRepository
import com.odoo.fieldapp.domain.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Helper class representing a line item in the create sale form
 */
data class CreateSaleLineItem(
    val localId: String = UUID.randomUUID().toString(),  // Unique ID for UI list management
    val product: Product,
    val quantity: Double = 1.0,
    val priceUnit: Double = product.listPrice,
    val lineTotal: Double = quantity * priceUnit
)

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
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository
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

    // ============ Create Sale Form State ============

    // Partner selection
    private val _createPartnerId = MutableStateFlow<Int?>(null)
    val createPartnerId: StateFlow<Int?> = _createPartnerId.asStateFlow()

    private val _createPartnerName = MutableStateFlow<String?>(null)
    val createPartnerName: StateFlow<String?> = _createPartnerName.asStateFlow()

    // Validation errors
    private val _partnerError = MutableStateFlow<String?>(null)
    val partnerError: StateFlow<String?> = _partnerError.asStateFlow()

    private val _linesError = MutableStateFlow<String?>(null)
    val linesError: StateFlow<String?> = _linesError.asStateFlow()

    // Create state
    private val _createState = MutableStateFlow<Resource<Sale>?>(null)
    val createState: StateFlow<Resource<Sale>?> = _createState.asStateFlow()

    // ============ Order Lines State ============

    // Line items for the create form
    private val _createLineItems = MutableStateFlow<List<CreateSaleLineItem>>(emptyList())
    val createLineItems: StateFlow<List<CreateSaleLineItem>> = _createLineItems.asStateFlow()

    // Order total (computed from line items)
    val orderTotal: StateFlow<Double> = _createLineItems.map { lines ->
        lines.sumOf { it.lineTotal }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // ============ Product Picker State ============

    // Show/hide product picker dialog
    private val _showProductPicker = MutableStateFlow(false)
    val showProductPicker: StateFlow<Boolean> = _showProductPicker.asStateFlow()

    // Product search query
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    // Products list (filtered by search query)
    val products: StateFlow<List<Product>> = _productSearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                productRepository.getProducts()
            } else {
                productRepository.searchProducts(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Update partner selection for create form
     */
    fun onCreatePartnerChange(partnerId: Int?, partnerName: String?) {
        _createPartnerId.value = partnerId
        _createPartnerName.value = partnerName
        // Clear error when user selects a partner
        if (partnerId != null) {
            _partnerError.value = null
        }
    }

    // ============ Product Picker Methods ============

    /**
     * Show the product picker dialog
     */
    fun showProductPicker() {
        _showProductPicker.value = true
        _productSearchQuery.value = ""
    }

    /**
     * Hide the product picker dialog
     */
    fun hideProductPicker() {
        _showProductPicker.value = false
        _productSearchQuery.value = ""
    }

    /**
     * Update product search query
     */
    fun onProductSearchQueryChange(query: String) {
        _productSearchQuery.value = query
    }

    // ============ Line Items Methods ============

    /**
     * Add a product as a line item
     */
    fun addLineItem(product: Product) {
        val newItem = CreateSaleLineItem(
            product = product,
            quantity = 1.0,
            priceUnit = product.listPrice,
            lineTotal = product.listPrice
        )
        _createLineItems.value = _createLineItems.value + newItem
        _linesError.value = null
        hideProductPicker()
    }

    /**
     * Remove a line item by its local ID
     */
    fun removeLineItem(localId: String) {
        _createLineItems.value = _createLineItems.value.filter { it.localId != localId }
    }

    /**
     * Update the quantity of a line item
     */
    fun updateLineQuantity(localId: String, quantity: Double) {
        if (quantity < 0) return
        _createLineItems.value = _createLineItems.value.map { item ->
            if (item.localId == localId) {
                item.copy(
                    quantity = quantity,
                    lineTotal = quantity * item.priceUnit
                )
            } else {
                item
            }
        }
    }

    /**
     * Increment line quantity by 1
     */
    fun incrementLineQuantity(localId: String) {
        _createLineItems.value = _createLineItems.value.map { item ->
            if (item.localId == localId) {
                val newQty = item.quantity + 1
                item.copy(quantity = newQty, lineTotal = newQty * item.priceUnit)
            } else {
                item
            }
        }
    }

    /**
     * Decrement line quantity by 1 (minimum 1)
     */
    fun decrementLineQuantity(localId: String) {
        _createLineItems.value = _createLineItems.value.map { item ->
            if (item.localId == localId && item.quantity > 1) {
                val newQty = item.quantity - 1
                item.copy(quantity = newQty, lineTotal = newQty * item.priceUnit)
            } else {
                item
            }
        }
    }

    /**
     * Validate the create form
     */
    private fun validateCreateForm(): Boolean {
        var isValid = true

        if (_createPartnerId.value == null) {
            _partnerError.value = "Customer is required"
            isValid = false
        }

        return isValid
    }

    /**
     * Create a new sale order
     */
    fun createSale() {
        if (!validateCreateForm()) return

        viewModelScope.launch {
            // Convert CreateSaleLineItem list to SaleLine list
            val saleLines = _createLineItems.value.map { lineItem ->
                SaleLine(
                    id = 0,  // Will be assigned by Odoo
                    productId = lineItem.product.id,
                    productName = lineItem.product.name,
                    productUomQty = lineItem.quantity,
                    qtyDelivered = 0.0,
                    qtyInvoiced = 0.0,
                    priceUnit = lineItem.priceUnit,
                    discount = 0.0,
                    priceSubtotal = lineItem.lineTotal,
                    uom = lineItem.product.uomName ?: "Units"
                )
            }

            val sale = Sale(
                id = 0,  // Will be assigned by repository
                mobileUid = null,  // Will be generated by repository
                name = "",  // Will be assigned by Odoo
                dateOrder = Date(),
                amountTotal = orderTotal.value,
                state = "draft",
                partnerId = _createPartnerId.value,
                partnerName = _createPartnerName.value,
                lines = saleLines,
                syncState = SyncState.PENDING,
                lastModified = Date()
            )

            saleRepository.createSale(sale)
                .collect { resource ->
                    _createState.value = resource
                }
        }
    }

    /**
     * Clear create form
     */
    fun clearCreateForm() {
        _createPartnerId.value = null
        _createPartnerName.value = null
        _partnerError.value = null
        _linesError.value = null
        _createState.value = null
        _createLineItems.value = emptyList()
        _showProductPicker.value = false
        _productSearchQuery.value = ""
    }

    /**
     * Clear create state (dismiss success/error messages)
     */
    fun clearCreateState() {
        _createState.value = null
    }
}
