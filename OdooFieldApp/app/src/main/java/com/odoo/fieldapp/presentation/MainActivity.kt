package com.odoo.fieldapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.odoo.fieldapp.presentation.navigation.AppNavigation
import com.odoo.fieldapp.presentation.navigation.Screen
import com.odoo.fieldapp.ui.theme.OdooFieldAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Bottom navigation items
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem(
        route = Screen.Dashboard.route,
        title = "Dashboard",
        icon = Icons.Default.Home
    )
    object Customers : BottomNavItem(
        route = Screen.CustomerList.route,
        title = "Customers",
        icon = Icons.Default.People
    )
    object Sales : BottomNavItem(
        route = Screen.SalesList.route,
        title = "Sales",
        icon = Icons.Default.ShoppingCart
    )
    object Deliveries : BottomNavItem(
        route = Screen.DeliveriesList.route,
        title = "Deliveries",
        icon = Icons.Default.LocalShipping
    )
    object Payments : BottomNavItem(
        route = Screen.PaymentsList.route,
        title = "Payments",
        icon = Icons.Default.Payment
    )
}

/**
 * Main Activity
 *
 * @AndroidEntryPoint enables Hilt dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OdooFieldAppTheme {
                MainScreen()
            }
        }
    }
}

/**
 * Main screen with bottom navigation
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Bottom navigation items
    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Customers,
        BottomNavItem.Sales,
        BottomNavItem.Deliveries,
        BottomNavItem.Payments
    )

    // Only show bottom nav on main screens (not detail or settings)
    val showBottomNav = currentDestination?.route in listOf(
        Screen.Dashboard.route,
        Screen.CustomerList.route,
        Screen.SalesList.route,
        Screen.DeliveriesList.route,
        Screen.PaymentsList.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination to avoid building up a large stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        AppNavigation(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

