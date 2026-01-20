package com.odoo.fieldapp.presentation.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.odoo.fieldapp.presentation.customer.CustomerCreateScreen
import com.odoo.fieldapp.presentation.customer.CustomerDetailScreen
import com.odoo.fieldapp.presentation.customer.CustomerListScreen
import com.odoo.fieldapp.presentation.customer.CustomerViewModel
import com.odoo.fieldapp.presentation.customer.VisitDialog
import com.odoo.fieldapp.presentation.dashboard.DashboardScreen
import com.odoo.fieldapp.presentation.dashboard.DashboardViewModel
import com.odoo.fieldapp.presentation.delivery.DeliveryDetailScreen
import com.odoo.fieldapp.presentation.delivery.DeliveryListScreen
import com.odoo.fieldapp.presentation.delivery.DeliveryViewModel
import com.odoo.fieldapp.presentation.payment.PaymentCreateScreen
import com.odoo.fieldapp.presentation.payment.PaymentDetailScreen
import com.odoo.fieldapp.presentation.payment.PaymentListScreen
import com.odoo.fieldapp.presentation.payment.PaymentViewModel
import com.odoo.fieldapp.presentation.sale.SaleCreateScreen
import com.odoo.fieldapp.presentation.sale.SaleDetailScreen
import com.odoo.fieldapp.presentation.sale.SaleListScreen
import com.odoo.fieldapp.presentation.sale.SaleViewModel
import com.odoo.fieldapp.presentation.settings.SettingsScreen
import com.odoo.fieldapp.presentation.settings.SettingsViewModel

/**
 * Navigation routes
 */
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object CustomerList : Screen("customer_list")
    object CustomerCreate : Screen("customer_create")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: Int) = "customer_detail/$customerId"
    }
    object SalesList : Screen("sales_list")
    object SaleCreate : Screen("sale_create")
    object SaleDetail : Screen("sale_detail/{saleId}") {
        fun createRoute(saleId: Int) = "sale_detail/$saleId"
    }
    object DeliveriesList : Screen("deliveries_list")
    object DeliveryDetail : Screen("delivery_detail/{deliveryId}") {
        fun createRoute(deliveryId: Int) = "delivery_detail/$deliveryId"
    }
    object PaymentsList : Screen("payments_list")
    object PaymentCreate : Screen("payment_create")
    object PaymentDetail : Screen("payment_detail/{paymentId}") {
        fun createRoute(paymentId: Int) = "payment_detail/$paymentId"
    }
    object Settings : Screen("settings")
}

/**
 * Bottom navigation items
 */
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Dashboard : BottomNavItem(Screen.Dashboard.route, Icons.Default.Home, "Dashboard")
    object Customers : BottomNavItem(Screen.CustomerList.route, Icons.Default.People, "Customers")
    object Sales : BottomNavItem(Screen.SalesList.route, Icons.Default.ShoppingCart, "Sales")
    object Deliveries : BottomNavItem(Screen.DeliveriesList.route, Icons.Default.LocalShipping, "Deliveries")
    object Payments : BottomNavItem(Screen.PaymentsList.route, Icons.Default.Payment, "Payments")
}

/**
 * List of bottom navigation items
 */
val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Customers,
    BottomNavItem.Sales,
    BottomNavItem.Deliveries,
    BottomNavItem.Payments
)

/**
 * Routes that should show the bottom navigation bar
 */
private val bottomNavRoutes = bottomNavItems.map { it.route }

/**
 * Bottom Navigation Bar
 */
@Composable
fun OdooBottomNavBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom nav on main list screens
    if (currentRoute !in bottomNavRoutes) return

    NavigationBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            selected = currentRoute == Screen.Dashboard.route,
            onClick = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
            label = { Text("Customers") },
            selected = currentRoute == Screen.CustomerList.route,
            onClick = {
                navController.navigate(Screen.CustomerList.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Sales") },
            label = { Text("Sales") },
            selected = currentRoute == Screen.SalesList.route,
            onClick = {
                navController.navigate(Screen.SalesList.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Deliveries") },
            label = { Text("Deliveries") },
            selected = currentRoute == Screen.DeliveriesList.route,
            onClick = {
                navController.navigate(Screen.DeliveriesList.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Payment, contentDescription = "Payments") },
            label = { Text("Payments") },
            selected = currentRoute == Screen.PaymentsList.route,
            onClick = {
                navController.navigate(Screen.PaymentsList.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

/**
 * Main navigation graph
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
            // Dashboard Screen
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                val stats by viewModel.stats.collectAsState()
                val syncState by viewModel.syncState.collectAsState()
                val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
                val lastSyncTime by viewModel.lastSyncTime.collectAsState()

                DashboardScreen(
                    stats = stats,
                    syncState = syncState,
                    pendingSyncCount = pendingSyncCount,
                    lastSyncTime = lastSyncTime,
                    onDeliveriesClick = {
                        navController.navigate(Screen.DeliveriesList.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onPaymentsClick = {
                        navController.navigate(Screen.PaymentsList.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSyncErrorsClick = {
                        // For now, just navigate to deliveries (could show a dedicated error screen later)
                        navController.navigate(Screen.DeliveriesList.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreatePaymentClick = {
                        navController.navigate(Screen.PaymentCreate.route)
                    },
                    onCreateCustomerClick = {
                        navController.navigate(Screen.CustomerCreate.route)
                    },
                    onCreateSaleClick = {
                        navController.navigate(Screen.SaleCreate.route)
                    },
                    onSyncAllClick = viewModel::syncAll,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onClearSyncState = viewModel::clearSyncState
                )
            }

            // Customer List Screen
            composable(Screen.CustomerList.route) {
                val viewModel: CustomerViewModel = hiltViewModel()
                val customers by viewModel.customers.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val syncState by viewModel.syncState.collectAsState()

                CustomerListScreen(
                    customers = customers,
                    searchQuery = searchQuery,
                    syncState = syncState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClearSearch = viewModel::clearSearch,
                    onSyncClick = viewModel::syncCustomers,
                    onCustomerClick = { customer ->
                        navController.navigate(
                            Screen.CustomerDetail.createRoute(customer.id)
                        )
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onClearSyncState = viewModel::clearSyncState,
                    onCreateClick = {
                        navController.navigate(Screen.CustomerCreate.route)
                    }
                )
            }

            // Customer Create Screen
            composable(Screen.CustomerCreate.route) {
                val viewModel: CustomerViewModel = hiltViewModel()
                val name by viewModel.createName.collectAsState()
                val city by viewModel.createCity.collectAsState()
                val taxId by viewModel.createTaxId.collectAsState()
                val email by viewModel.createEmail.collectAsState()
                val phone by viewModel.createPhone.collectAsState()
                val website by viewModel.createWebsite.collectAsState()
                val nameError by viewModel.nameError.collectAsState()
                val emailError by viewModel.emailError.collectAsState()
                val phoneError by viewModel.phoneError.collectAsState()
                val websiteError by viewModel.websiteError.collectAsState()
                val createState by viewModel.createState.collectAsState()

                CustomerCreateScreen(
                    name = name,
                    city = city,
                    taxId = taxId,
                    email = email,
                    phone = phone,
                    website = website,
                    nameError = nameError,
                    emailError = emailError,
                    phoneError = phoneError,
                    websiteError = websiteError,
                    createState = createState,
                    onNameChange = viewModel::onCreateNameChange,
                    onCityChange = viewModel::onCreateCityChange,
                    onTaxIdChange = viewModel::onCreateTaxIdChange,
                    onEmailChange = viewModel::onCreateEmailChange,
                    onPhoneChange = viewModel::onCreatePhoneChange,
                    onWebsiteChange = viewModel::onCreateWebsiteChange,
                    onSaveClick = viewModel::createCustomer,
                    onBackClick = {
                        viewModel.clearCreateForm()
                        navController.popBackStack()
                    },
                    onClearCreateState = viewModel::clearCreateState
                )
            }

            // Customer Detail Screen
            composable(
                route = Screen.CustomerDetail.route,
                arguments = listOf(
                    navArgument("customerId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val viewModel: CustomerViewModel = hiltViewModel()
                val customerId = backStackEntry.arguments?.getInt("customerId") ?: 0
                val customer by viewModel.selectedCustomer.collectAsState()
                val salesForCustomer by viewModel.salesForCustomer.collectAsState()
                val deliveriesForCustomer by viewModel.deliveriesForCustomer.collectAsState()
                val visitsForCustomer by viewModel.visitsForCustomer.collectAsState()

                // Visit dialog state
                val showVisitDialog by viewModel.showVisitDialog.collectAsState()
                val visitDatetime by viewModel.visitDatetime.collectAsState()
                val visitMemo by viewModel.visitMemo.collectAsState()
                val createVisitState by viewModel.createVisitState.collectAsState()

                // Location state
                val locationState by viewModel.locationState.collectAsState()
                val needsLocationPermission by viewModel.needsLocationPermission.collectAsState()
                val showLocationConfirmDialog by viewModel.showLocationConfirmDialog.collectAsState()

                // Permission launcher for location
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                    val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                    if (fineLocationGranted || coarseLocationGranted) {
                        // Permission granted, proceed with location capture
                        viewModel.clearLocationPermissionRequest()
                        viewModel.showLocationConfirmDialog()
                    } else {
                        // Permission denied
                        viewModel.clearLocationPermissionRequest()
                    }
                }

                // Handle permission request trigger
                LaunchedEffect(needsLocationPermission) {
                    if (needsLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                // Load customer when screen opens
                if (customer == null && customerId != 0) {
                    viewModel.loadCustomerById(customerId)
                }

                CustomerDetailScreen(
                    customer = customer,
                    sales = salesForCustomer,
                    deliveries = deliveriesForCustomer,
                    visits = visitsForCustomer,
                    onBackClick = {
                        viewModel.clearSelectedCustomer()
                        navController.popBackStack()
                    },
                    onSaleClick = { sale ->
                        navController.navigate(
                            Screen.SaleDetail.createRoute(sale.id)
                        )
                    },
                    onDeliveryClick = { delivery ->
                        navController.navigate(
                            Screen.DeliveryDetail.createRoute(delivery.id)
                        )
                    },
                    onAddVisitClick = {
                        viewModel.showVisitDialog()
                    },
                    locationState = locationState,
                    showLocationConfirmDialog = showLocationConfirmDialog,
                    isCapturingLocation = locationState is com.odoo.fieldapp.domain.model.Resource.Loading,
                    onCaptureLocationClick = viewModel::onCaptureLocationClick,
                    onConfirmLocationUpdate = viewModel::captureCustomerLocation,
                    onDismissLocationDialog = viewModel::hideLocationConfirmDialog,
                    onClearLocationState = viewModel::clearLocationState
                )

                // Visit Dialog
                val currentCustomer = customer
                if (showVisitDialog && currentCustomer != null) {
                    VisitDialog(
                        customerName = currentCustomer.name,
                        visitDatetime = visitDatetime,
                        visitMemo = visitMemo,
                        createState = createVisitState,
                        onDismiss = {
                            viewModel.hideVisitDialog()
                            viewModel.clearCreateVisitState()
                        },
                        onMemoChange = viewModel::onVisitMemoChange,
                        onSave = {
                            viewModel.createVisit()
                        }
                    )
                }
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                val serverUrl by viewModel.serverUrl.collectAsState()
                val apiKey by viewModel.apiKey.collectAsState()
                val saveState by viewModel.saveState.collectAsState()
                val showProductListDialog by viewModel.showProductListDialog.collectAsState()
                val products by viewModel.products.collectAsState()
                val productSearchQuery by viewModel.productSearchQuery.collectAsState()

                SettingsScreen(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    saveState = saveState,
                    showProductListDialog = showProductListDialog,
                    products = products,
                    productSearchQuery = productSearchQuery,
                    onServerUrlChange = viewModel::onServerUrlChange,
                    onApiKeyChange = viewModel::onApiKeyChange,
                    onSaveClick = viewModel::saveSettings,
                    onBackClick = { navController.popBackStack() },
                    onClearSaveState = viewModel::clearSaveState,
                    onShowProductListDialog = viewModel::showProductListDialog,
                    onHideProductListDialog = viewModel::hideProductListDialog,
                    onProductSearchQueryChange = viewModel::onProductSearchQueryChange
                )
            }

            // Sales List Screen
            composable(Screen.SalesList.route) {
                val viewModel: SaleViewModel = hiltViewModel()
                val sales by viewModel.sales.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val syncState by viewModel.syncState.collectAsState()

                SaleListScreen(
                    sales = sales,
                    searchQuery = searchQuery,
                    syncState = syncState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClearSearch = viewModel::clearSearch,
                    onSyncClick = viewModel::syncSales,
                    onSaleClick = { sale ->
                        navController.navigate(
                            Screen.SaleDetail.createRoute(sale.id)
                        )
                    },
                    onCreateClick = {
                        navController.navigate(Screen.SaleCreate.route)
                    },
                    onClearSyncState = viewModel::clearSyncState
                )
            }

            // Sale Create Screen
            composable(Screen.SaleCreate.route) {
                val saleViewModel: SaleViewModel = hiltViewModel()
                val customerViewModel: CustomerViewModel = hiltViewModel()

                val customers by customerViewModel.customers.collectAsState()
                val selectedPartnerId by saleViewModel.createPartnerId.collectAsState()
                val selectedPartnerName by saleViewModel.createPartnerName.collectAsState()
                val partnerError by saleViewModel.partnerError.collectAsState()
                val linesError by saleViewModel.linesError.collectAsState()
                val createState by saleViewModel.createState.collectAsState()

                // Order lines state
                val lineItems by saleViewModel.createLineItems.collectAsState()
                val orderTotal by saleViewModel.orderTotal.collectAsState()

                // Product picker state
                val products by saleViewModel.products.collectAsState()
                val showProductPicker by saleViewModel.showProductPicker.collectAsState()
                val productSearchQuery by saleViewModel.productSearchQuery.collectAsState()

                SaleCreateScreen(
                    customers = customers,
                    selectedPartnerId = selectedPartnerId,
                    selectedPartnerName = selectedPartnerName,
                    lineItems = lineItems,
                    orderTotal = orderTotal,
                    products = products,
                    showProductPicker = showProductPicker,
                    productSearchQuery = productSearchQuery,
                    partnerError = partnerError,
                    linesError = linesError,
                    createState = createState,
                    onPartnerChange = saleViewModel::onCreatePartnerChange,
                    onShowProductPicker = saleViewModel::showProductPicker,
                    onHideProductPicker = saleViewModel::hideProductPicker,
                    onProductSearchQueryChange = saleViewModel::onProductSearchQueryChange,
                    onAddLineItem = saleViewModel::addLineItem,
                    onRemoveLineItem = saleViewModel::removeLineItem,
                    onIncrementQuantity = saleViewModel::incrementLineQuantity,
                    onDecrementQuantity = saleViewModel::decrementLineQuantity,
                    onSaveClick = saleViewModel::createSale,
                    onBackClick = {
                        saleViewModel.clearCreateForm()
                        navController.popBackStack()
                    },
                    onClearCreateState = saleViewModel::clearCreateState
                )
            }

            // Sale Detail Screen
            composable(
                route = Screen.SaleDetail.route,
                arguments = listOf(
                    navArgument("saleId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val viewModel: SaleViewModel = hiltViewModel()
                val saleId = backStackEntry.arguments?.getInt("saleId") ?: 0
                val sale by viewModel.selectedSale.collectAsState()
                val customerName by viewModel.customerName.collectAsState()

                // Load sale when screen opens
                if (sale == null && saleId != 0) {
                    viewModel.loadSaleById(saleId)
                }

                SaleDetailScreen(
                    sale = sale,
                    customerName = customerName,
                    onBackClick = {
                        viewModel.clearSelectedSale()
                        navController.popBackStack()
                    }
                )
            }

            // Deliveries List Screen
            composable(Screen.DeliveriesList.route) {
                val viewModel: DeliveryViewModel = hiltViewModel()
                val deliveries by viewModel.deliveries.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val syncState by viewModel.syncState.collectAsState()

                DeliveryListScreen(
                    deliveries = deliveries,
                    searchQuery = searchQuery,
                    syncState = syncState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClearSearch = viewModel::clearSearch,
                    onSyncClick = viewModel::syncDeliveries,
                    onDeliveryClick = { delivery ->
                        navController.navigate(
                            Screen.DeliveryDetail.createRoute(delivery.id)
                        )
                    },
                    onClearSyncState = viewModel::clearSyncState
                )
            }

            // Delivery Detail Screen
            composable(
                route = Screen.DeliveryDetail.route,
                arguments = listOf(
                    navArgument("deliveryId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val viewModel: DeliveryViewModel = hiltViewModel()
                val deliveryId = backStackEntry.arguments?.getInt("deliveryId") ?: 0
                val delivery by viewModel.selectedDelivery.collectAsState()
                val validateState by viewModel.validateState.collectAsState()

                // Load delivery when screen opens
                if (delivery == null && deliveryId != 0) {
                    viewModel.loadDeliveryById(deliveryId)
                }

                DeliveryDetailScreen(
                    delivery = delivery,
                    validateState = validateState,
                    onBackClick = {
                        viewModel.clearSelectedDelivery()
                        viewModel.clearValidateState()
                        navController.popBackStack()
                    },
                    onValidateClick = viewModel::validateDelivery,
                    onClearValidateState = viewModel::clearValidateState,
                    onCustomerClick = { customerId ->
                        navController.navigate(
                            Screen.CustomerDetail.createRoute(customerId)
                        )
                    },
                    onSaleClick = { saleId ->
                        navController.navigate(
                            Screen.SaleDetail.createRoute(saleId)
                        )
                    }
                )
            }

            // Payments List Screen
            composable(Screen.PaymentsList.route) {
                val viewModel: PaymentViewModel = hiltViewModel()
                val payments by viewModel.payments.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val syncState by viewModel.syncState.collectAsState()

                PaymentListScreen(
                    payments = payments,
                    searchQuery = searchQuery,
                    syncState = syncState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClearSearch = viewModel::clearSearch,
                    onSyncClick = viewModel::syncPayments,
                    onPaymentClick = { payment ->
                        navController.navigate(
                            Screen.PaymentDetail.createRoute(payment.id)
                        )
                    },
                    onCreateClick = {
                        navController.navigate(Screen.PaymentCreate.route)
                    },
                    onClearSyncState = viewModel::clearSyncState
                )
            }

            // Payment Create Screen
            composable(Screen.PaymentCreate.route) {
                val paymentViewModel: PaymentViewModel = hiltViewModel()
                val customerViewModel: CustomerViewModel = hiltViewModel()

                val customers by customerViewModel.customers.collectAsState()
                val selectedPartnerId by paymentViewModel.createPartnerId.collectAsState()
                val selectedPartnerName by paymentViewModel.createPartnerName.collectAsState()
                val amount by paymentViewModel.createAmount.collectAsState()
                val memo by paymentViewModel.createMemo.collectAsState()
                val partnerError by paymentViewModel.partnerError.collectAsState()
                val amountError by paymentViewModel.amountError.collectAsState()
                val createState by paymentViewModel.createState.collectAsState()

                PaymentCreateScreen(
                    customers = customers,
                    selectedPartnerId = selectedPartnerId,
                    selectedPartnerName = selectedPartnerName,
                    amount = amount,
                    memo = memo,
                    partnerError = partnerError,
                    amountError = amountError,
                    createState = createState,
                    onPartnerChange = paymentViewModel::onCreatePartnerChange,
                    onAmountChange = paymentViewModel::onCreateAmountChange,
                    onMemoChange = paymentViewModel::onCreateMemoChange,
                    onSaveClick = paymentViewModel::createPayment,
                    onBackClick = {
                        paymentViewModel.clearCreateForm()
                        navController.popBackStack()
                    },
                    onClearCreateState = paymentViewModel::clearCreateState
                )
            }

            // Payment Detail Screen
            composable(
                route = Screen.PaymentDetail.route,
                arguments = listOf(
                    navArgument("paymentId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val viewModel: PaymentViewModel = hiltViewModel()
                val paymentId = backStackEntry.arguments?.getInt("paymentId") ?: 0
                val payment by viewModel.selectedPayment.collectAsState()

                // Load payment when screen opens
                if (payment == null && paymentId != 0) {
                    viewModel.loadPaymentById(paymentId)
                }

                PaymentDetailScreen(
                    payment = payment,
                    onBackClick = {
                        viewModel.clearSelectedPayment()
                        navController.popBackStack()
                    },
                    onCustomerClick = { customerId ->
                        navController.navigate(
                            Screen.CustomerDetail.createRoute(customerId)
                        )
                    }
                )
            }
        }
}
