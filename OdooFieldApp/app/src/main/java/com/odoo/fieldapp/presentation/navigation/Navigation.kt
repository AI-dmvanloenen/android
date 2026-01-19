package com.odoo.fieldapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.odoo.fieldapp.presentation.customer.CustomerCreateScreen
import com.odoo.fieldapp.presentation.customer.CustomerDetailScreen
import com.odoo.fieldapp.presentation.customer.CustomerListScreen
import com.odoo.fieldapp.presentation.customer.CustomerViewModel
import com.odoo.fieldapp.presentation.delivery.DeliveryDetailScreen
import com.odoo.fieldapp.presentation.delivery.DeliveryListScreen
import com.odoo.fieldapp.presentation.delivery.DeliveryViewModel
import com.odoo.fieldapp.presentation.payment.PaymentCreateScreen
import com.odoo.fieldapp.presentation.payment.PaymentDetailScreen
import com.odoo.fieldapp.presentation.payment.PaymentListScreen
import com.odoo.fieldapp.presentation.payment.PaymentViewModel
import com.odoo.fieldapp.presentation.sale.SaleDetailScreen
import com.odoo.fieldapp.presentation.sale.SaleListScreen
import com.odoo.fieldapp.presentation.sale.SaleViewModel
import com.odoo.fieldapp.presentation.settings.SettingsScreen
import com.odoo.fieldapp.presentation.settings.SettingsViewModel

/**
 * Navigation routes
 */
sealed class Screen(val route: String) {
    object CustomerList : Screen("customer_list")
    object CustomerCreate : Screen("customer_create")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: Int) = "customer_detail/$customerId"
    }
    object SalesList : Screen("sales_list")
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
 * Main navigation graph
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CustomerList.route,
        modifier = modifier
    ) {
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
            val createState by viewModel.createState.collectAsState()

            CustomerCreateScreen(
                name = name,
                city = city,
                taxId = taxId,
                email = email,
                phone = phone,
                website = website,
                nameError = nameError,
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

            // Load customer when screen opens
            if (customer == null && customerId != 0) {
                viewModel.loadCustomerById(customerId)
            }

            CustomerDetailScreen(
                customer = customer,
                sales = salesForCustomer,
                deliveries = deliveriesForCustomer,
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
                }
            )
        }
        
        // Settings Screen
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val serverUrl by viewModel.serverUrl.collectAsState()
            val apiKey by viewModel.apiKey.collectAsState()
            val saveState by viewModel.saveState.collectAsState()

            SettingsScreen(
                serverUrl = serverUrl,
                apiKey = apiKey,
                saveState = saveState,
                onServerUrlChange = viewModel::onServerUrlChange,
                onApiKeyChange = viewModel::onApiKeyChange,
                onSaveClick = viewModel::saveSettings,
                onBackClick = { navController.popBackStack() },
                onClearSaveState = viewModel::clearSaveState
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
                onClearSyncState = viewModel::clearSyncState
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
