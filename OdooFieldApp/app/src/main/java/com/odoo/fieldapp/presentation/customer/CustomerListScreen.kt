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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.R
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Resource
import java.text.SimpleDateFormat
import java.util.*

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
                        enabled = syncState !is Resource.Loading
                    ) {
                        Icon(
                            imageVector = if (syncState is Resource.Loading) Icons.Default.Sync else Icons.Default.Refresh,
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
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearClick = onClearSearch,
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
                        SuccessMessage(
                            message = "Synced ${state.data?.size ?: 0} customers",
                            onDismiss = onClearSyncState
                        )
                    }
                    is Resource.Error -> {
                        ErrorMessage(
                            message = state.message ?: "Sync failed",
                            onDismiss = onClearSyncState
                        )
                    }
                    is Resource.Loading -> {
                        // Loading indicator shown in toolbar
                    }
                }
            }
            
            // Customer list or empty state
            if (customers.isEmpty()) {
                EmptyState(
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
 * Search bar component
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search customers...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
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
 * Single customer list item
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Customer name
            Text(
                text = customer.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // City and phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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
    }
}

/**
 * Empty state when no customers are found
 */
@Composable
fun EmptyState(
    searchQuery: String,
    onSyncClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (searchQuery.isEmpty()) Icons.Default.Person else Icons.Default.Search,
            contentDescription = if (searchQuery.isEmpty()) "No customers" else "No results",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (searchQuery.isEmpty()) {
                "No customers yet"
            } else {
                "No customers found"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (searchQuery.isEmpty()) {
                "Tap the sync button to load customers from Odoo"
            } else {
                "Try a different search term"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSyncClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Now")
            }
        }
    }
}

/**
 * Success message banner
 */
@Composable
fun SuccessMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Error message banner
 */
@Composable
fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
