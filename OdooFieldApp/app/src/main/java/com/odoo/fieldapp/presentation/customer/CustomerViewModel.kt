package com.odoo.fieldapp.presentation.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odoo.fieldapp.domain.location.LocationService
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.model.Visit
import com.odoo.fieldapp.domain.repository.CustomerRepository
import com.odoo.fieldapp.domain.repository.DeliveryRepository
import com.odoo.fieldapp.domain.repository.SaleRepository
import com.odoo.fieldapp.domain.repository.VisitRepository
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
    private val saleRepository: SaleRepository,
    private val deliveryRepository: DeliveryRepository,
    private val visitRepository: VisitRepository,
    private val locationService: LocationService
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

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError.asStateFlow()

    private val _websiteError = MutableStateFlow<String?>(null)
    val websiteError: StateFlow<String?> = _websiteError.asStateFlow()

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

    // Deliveries for the selected customer
    val deliveriesForCustomer: StateFlow<List<Delivery>> = _selectedCustomer
        .flatMapLatest { customer ->
            if (customer != null) {
                deliveryRepository.getDeliveriesByCustomer(customer.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Visits for the selected customer
    val visitsForCustomer: StateFlow<List<Visit>> = _selectedCustomer
        .flatMapLatest { customer ->
            if (customer != null) {
                visitRepository.getVisitsByCustomer(customer.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // === Visit Dialog State ===
    private val _showVisitDialog = MutableStateFlow(false)
    val showVisitDialog: StateFlow<Boolean> = _showVisitDialog.asStateFlow()

    private val _visitDatetime = MutableStateFlow(Date())
    val visitDatetime: StateFlow<Date> = _visitDatetime.asStateFlow()

    private val _visitMemo = MutableStateFlow("")
    val visitMemo: StateFlow<String> = _visitMemo.asStateFlow()

    private val _createVisitState = MutableStateFlow<Resource<Visit>?>(null)
    val createVisitState: StateFlow<Resource<Visit>?> = _createVisitState.asStateFlow()

    // === Location State ===
    private val _locationState = MutableStateFlow<Resource<Unit>?>(null)
    val locationState: StateFlow<Resource<Unit>?> = _locationState.asStateFlow()

    private val _needsLocationPermission = MutableStateFlow(false)
    val needsLocationPermission: StateFlow<Boolean> = _needsLocationPermission.asStateFlow()

    private val _showLocationConfirmDialog = MutableStateFlow(false)
    val showLocationConfirmDialog: StateFlow<Boolean> = _showLocationConfirmDialog.asStateFlow()

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
        // Clear error when user starts typing
        if (_emailError.value != null) {
            _emailError.value = null
        }
    }

    fun onCreatePhoneChange(phone: String) {
        _createPhone.value = phone
        // Clear error when user starts typing
        if (_phoneError.value != null) {
            _phoneError.value = null
        }
    }

    fun onCreateWebsiteChange(website: String) {
        _createWebsite.value = website
        // Clear error when user starts typing
        if (_websiteError.value != null) {
            _websiteError.value = null
        }
    }

    /**
     * Validate the create form
     * @return true if valid, false otherwise
     */
    private fun validateCreateForm(): Boolean {
        var isValid = true

        // Validate name (required)
        val name = _createName.value.trim()
        if (name.isBlank()) {
            _nameError.value = "Name is required"
            isValid = false
        } else {
            _nameError.value = null
        }

        // Validate email format (optional but must be valid if provided)
        val email = _createEmail.value.trim()
        if (email.isNotBlank()) {
            val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!emailRegex.matches(email)) {
                _emailError.value = "Invalid email format"
                isValid = false
            } else {
                _emailError.value = null
            }
        } else {
            _emailError.value = null
        }

        // Validate phone format (optional but must be reasonable if provided)
        val phone = _createPhone.value.trim()
        if (phone.isNotBlank()) {
            // Allow digits, spaces, dashes, parentheses, and + for international format
            val phoneRegex = Regex("^[+]?[0-9\\s\\-()]{6,20}$")
            if (!phoneRegex.matches(phone)) {
                _phoneError.value = "Invalid phone format"
                isValid = false
            } else {
                _phoneError.value = null
            }
        } else {
            _phoneError.value = null
        }

        // Validate website format (optional but must be valid URL if provided)
        val website = _createWebsite.value.trim()
        if (website.isNotBlank()) {
            // Simple URL validation - must have at least a domain with TLD
            val urlRegex = Regex("^(https?://)?[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)+(/.*)?$")
            if (!urlRegex.matches(website)) {
                _websiteError.value = "Invalid website URL"
                isValid = false
            } else {
                _websiteError.value = null
            }
        } else {
            _websiteError.value = null
        }

        return isValid
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
        _emailError.value = null
        _phoneError.value = null
        _websiteError.value = null
        _createState.value = null
    }

    /**
     * Clear create state (dismiss success/error messages)
     */
    fun clearCreateState() {
        _createState.value = null
    }

    // === Visit Dialog Methods ===

    /**
     * Show visit dialog and reset fields to default
     */
    fun showVisitDialog() {
        _visitDatetime.value = Date() // Default to current time
        _visitMemo.value = ""
        _createVisitState.value = null
        _showVisitDialog.value = true
    }

    /**
     * Hide visit dialog
     */
    fun hideVisitDialog() {
        _showVisitDialog.value = false
    }

    /**
     * Update visit datetime
     */
    fun onVisitDatetimeChange(datetime: Date) {
        _visitDatetime.value = datetime
    }

    /**
     * Update visit memo
     */
    fun onVisitMemoChange(memo: String) {
        _visitMemo.value = memo
    }

    /**
     * Create a new visit for the selected customer
     */
    fun createVisit() {
        val customer = _selectedCustomer.value ?: return

        viewModelScope.launch {
            val visit = Visit(
                id = 0, // Will be assigned by repository
                mobileUid = null, // Will be generated by repository
                partnerId = customer.id,
                partnerName = customer.name,
                visitDatetime = _visitDatetime.value,
                memo = _visitMemo.value.trim().ifBlank { null },
                syncState = SyncState.PENDING,
                lastModified = Date()
            )

            visitRepository.createVisit(visit)
                .collect { resource ->
                    _createVisitState.value = resource
                    // Close dialog on success
                    if (resource is Resource.Success) {
                        hideVisitDialog()
                    }
                }
        }
    }

    /**
     * Clear visit create state (dismiss success/error messages)
     */
    fun clearCreateVisitState() {
        _createVisitState.value = null
    }

    // === Location Methods ===

    /**
     * Check if location permission is granted
     */
    fun checkLocationPermission(): Boolean {
        return locationService.hasLocationPermission()
    }

    /**
     * Request location permission (triggers UI permission request)
     */
    fun requestLocationPermission() {
        _needsLocationPermission.value = true
    }

    /**
     * Clear permission request flag after it's been handled
     */
    fun clearLocationPermissionRequest() {
        _needsLocationPermission.value = false
    }

    /**
     * Handle capture location button click
     * Checks permission and shows confirmation dialog
     */
    fun onCaptureLocationClick() {
        // Check permission first
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }

        // Show confirmation dialog
        showLocationConfirmDialog()
    }

    /**
     * Show location confirmation dialog
     */
    fun showLocationConfirmDialog() {
        _showLocationConfirmDialog.value = true
    }

    /**
     * Hide location confirmation dialog
     */
    fun hideLocationConfirmDialog() {
        _showLocationConfirmDialog.value = false
    }

    /**
     * Capture customer location and save to repository
     * Called after user confirms in dialog
     */
    fun captureCustomerLocation() {
        val customer = _selectedCustomer.value ?: return

        viewModelScope.launch {
            // Emit loading state
            _locationState.value = Resource.Loading()

            // Hide dialog
            hideLocationConfirmDialog()

            // Check if location services are enabled
            if (!locationService.isLocationEnabled()) {
                _locationState.value = Resource.Error("GPS is disabled. Please enable location services in Settings.")
                return@launch
            }

            // Get current location from LocationService
            locationService.getCurrentLocation().collect { locationResource ->
                when (locationResource) {
                    is Resource.Loading -> {
                        _locationState.value = Resource.Loading()
                    }
                    is Resource.Success -> {
                        val location = locationResource.data
                        if (location != null) {
                            // Update customer location in repository
                            customerRepository.updateCustomerLocation(
                                customerId = customer.id,
                                latitude = location.latitude,
                                longitude = location.longitude
                            ).collect { updateResource ->
                                when (updateResource) {
                                    is Resource.Loading -> {
                                        _locationState.value = Resource.Loading()
                                    }
                                    is Resource.Success -> {
                                        // Reload customer to show updated location
                                        loadCustomerById(customer.id)
                                        _locationState.value = Resource.Success(
                                            Unit,
                                            updateResource.message ?: "Location captured successfully"
                                        )
                                    }
                                    is Resource.Error -> {
                                        _locationState.value = Resource.Error(
                                            updateResource.message ?: "Failed to save location"
                                        )
                                    }
                                }
                            }
                        } else {
                            _locationState.value = Resource.Error("Location data not available")
                        }
                    }
                    is Resource.Error -> {
                        _locationState.value = Resource.Error(
                            locationResource.message ?: "Failed to get location"
                        )
                    }
                }
            }
        }
    }

    /**
     * Clear location state (dismiss success/error messages)
     */
    fun clearLocationState() {
        _locationState.value = null
    }
}
