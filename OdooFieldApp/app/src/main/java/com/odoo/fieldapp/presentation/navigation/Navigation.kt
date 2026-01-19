package com.odoo.fieldapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.odoo.fieldapp.presentation.customer.CustomerDetailScreen
import com.odoo.fieldapp.presentation.customer.CustomerListScreen
import com.odoo.fieldapp.presentation.customer.CustomerViewModel
import com.odoo.fieldapp.presentation.settings.SettingsScreen
import com.odoo.fieldapp.presentation.settings.SettingsViewModel

/**
 * Navigation routes
 */
sealed class Screen(val route: String) {
    object CustomerList : Screen("customer_list")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: String) = "customer_detail/$customerId"
    }
    object Settings : Screen("settings")
}

/**
 * Main navigation graph
 */
@Composable
fun AppNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CustomerList.route
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
                onClearSyncState = viewModel::clearSyncState
            )
        }
        
        // Customer Detail Screen
        composable(
            route = Screen.CustomerDetail.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val viewModel: CustomerViewModel = hiltViewModel()
            val customerId = backStackEntry.arguments?.getString("customerId")
            val customer by viewModel.selectedCustomer.collectAsState()
            
            // Load customer when screen opens
            if (customer == null && customerId != null) {
                viewModel.loadCustomerById(customerId)
            }
            
            CustomerDetailScreen(
                customer = customer,
                onBackClick = {
                    viewModel.clearSelectedCustomer()
                    navController.popBackStack()
                }
            )
        }
        
        // Settings Screen
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val databaseName by viewModel.databaseName.collectAsState()
            val apiKey by viewModel.apiKey.collectAsState()
            val saveState by viewModel.saveState.collectAsState()

            SettingsScreen(
                databaseName = databaseName,
                apiKey = apiKey,
                saveState = saveState,
                onDatabaseNameChange = viewModel::onDatabaseNameChange,
                onApiKeyChange = viewModel::onApiKeyChange,
                onSaveClick = viewModel::saveSettings,
                onBackClick = { navController.popBackStack() },
                onClearSaveState = viewModel::clearSaveState
            )
        }
    }
}
