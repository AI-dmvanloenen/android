package com.odoo.fieldapp.presentation.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.R
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.presentation.components.AppSearchBar
import com.odoo.fieldapp.presentation.components.EmptyStateView
import com.odoo.fieldapp.presentation.components.ErrorBanner
import com.odoo.fieldapp.presentation.components.SuccessBanner

/**
 * Customer List Screen
 *
 * Displays a searchable list of customers with sync functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    customers: List<Customer>,
    searchQuery: String,
    syncState: Resource<List<Customer>>?,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSyncClick: () -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onSettingsClick: () -> Unit,
    onClearSyncState: () -> Unit,
    onCreateClick: () -> Unit = {}
) {
    val isRefreshing = syncState is Resource.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo_gocongo),
                        contentDescription = "GoCongo Logo",
                        modifier = Modifier.height(36.dp)
                    )
                },
                actions = {
                    // Sync button
                    IconButton(
                        onClick = onSyncClick,
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = if (isRefreshing) Icons.Default.Sync else Icons.Default.Refresh,
                            contentDescription = "Sync"
                        )
                    }

                    // Settings button
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "Create Customer")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            AppSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearClick = onClearSearch,
                placeholder = "Search customers...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Auto-dismiss success messages after 3 seconds
            LaunchedEffect(syncState) {
                if (syncState is Resource.Success) {
                    delay(3000)
                    onClearSyncState()
                }
            }

            // Sync state messages
            syncState?.let { state ->
                when (state) {
                    is Resource.Success -> {
                        SuccessBanner(
                            message = if ((state.data?.size ?: 0) == 0) "There is nothing new to sync" else "Synced ${state.data?.size} customers",
                            onDismiss = onClearSyncState
                        )
                    }
                    is Resource.Error -> {
                        ErrorBanner(
                            message = state.message ?: "Sync failed",
                            onDismiss = onClearSyncState
                        )
                    }
                    is Resource.Loading -> {
                        // Loading indicator shown via pull-to-refresh
                    }
                }
            }

            // Customer list or empty state
            if (customers.isEmpty()) {
                CustomerEmptyState(
                    searchQuery = searchQuery,
                    onSyncClick = onSyncClick
                )
            } else {
                CustomerList(
                    customers = customers,
                    onCustomerClick = onCustomerClick
                )
            }
        }
    }
}

/**
 * Customer list component
 */
@Composable
fun CustomerList(
    customers: List<Customer>,
    onCustomerClick: (Customer) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(customers, key = { it.id }) { customer ->
            CustomerListItem(
                customer = customer,
                onClick = { onCustomerClick(customer) }
            )
        }
    }
}

/**
 * Single customer list item with chevron
 */
@Composable
fun CustomerListItem(
    customer: Customer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Customer name with improved typography
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // City and phone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    customer.city?.let { city ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    customer.phone?.let { phone ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Chevron icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty state when no customers are found
 */
@Composable
fun CustomerEmptyState(
    searchQuery: String,
    onSyncClick: () -> Unit
) {
    if (searchQuery.isNotEmpty()) {
        EmptyStateView(
            icon = Icons.Default.SearchOff,
            title = "No customers found",
            subtitle = "No results match \"$searchQuery\". Try a different search term."
        )
    } else {
        EmptyStateView(
            icon = Icons.Default.People,
            title = "No customers yet",
            subtitle = "Tap the button below to sync customers from Odoo",
            actionLabel = "Sync Now",
            onActionClick = onSyncClick
        )
    }
}
