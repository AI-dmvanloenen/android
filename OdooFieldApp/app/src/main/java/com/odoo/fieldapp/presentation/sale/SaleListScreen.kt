package com.odoo.fieldapp.presentation.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sale List Screen
 *
 * Displays a searchable list of sales with sync functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleListScreen(
    sales: List<Sale>,
    searchQuery: String,
    syncState: Resource<List<Sale>>?,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSyncClick: () -> Unit,
    onSaleClick: (Sale) -> Unit,
    onClearSyncState: () -> Unit
) {
    // Auto-dismiss success messages after 3 seconds
    LaunchedEffect(syncState) {
        if (syncState is Resource.Success) {
            delay(3000)
            onClearSyncState()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search bar
        SaleSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch,
            onSyncClick = onSyncClick,
            isSyncing = syncState is Resource.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Sync state messages
        syncState?.let { state ->
            when (state) {
                is Resource.Success -> {
                    SaleSuccessMessage(
                        message = "Synced ${state.data?.size ?: 0} sales",
                        onDismiss = onClearSyncState
                    )
                }
                is Resource.Error -> {
                    SaleErrorMessage(
                        message = state.message ?: "Sync failed",
                        onDismiss = onClearSyncState
                    )
                }
                is Resource.Loading -> {
                    // Loading indicator shown in search bar
                }
            }
        }

        // Sales list or empty state
        if (sales.isEmpty()) {
            SaleEmptyState(
                searchQuery = searchQuery,
                onSyncClick = onSyncClick
            )
        } else {
            SaleList(
                sales = sales,
                onSaleClick = onSaleClick
            )
        }
    }
}

/**
 * Search bar with sync button
 */
@Composable
fun SaleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onSyncClick: () -> Unit,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search sales...") },
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

        IconButton(
            onClick = onSyncClick,
            enabled = !isSyncing
        ) {
            Icon(
                imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.Refresh,
                contentDescription = "Sync"
            )
        }
    }
}

/**
 * Sales list component
 */
@Composable
fun SaleList(
    sales: List<Sale>,
    onSaleClick: (Sale) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sales, key = { it.id }) { sale ->
            SaleListItem(
                sale = sale,
                onClick = { onSaleClick(sale) }
            )
        }
    }
}

/**
 * Single sale list item
 */
@Composable
fun SaleListItem(
    sale: Sale,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

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
            // Sale name with customer name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Sale name and customer name combined
                    Text(
                        text = if (sale.partnerName != null) {
                            "${sale.name} - ${sale.partnerName}"
                        } else {
                            sale.name
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Amount
                sale.amountTotal?.let { amount ->
                    Text(
                        text = currencyFormatter.format(amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                sale.dateOrder?.let { date ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateFormatter.format(date),
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
 * Empty state when no sales are found
 */
@Composable
fun SaleEmptyState(
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
            imageVector = if (searchQuery.isEmpty()) Icons.Default.ShoppingCart else Icons.Default.Search,
            contentDescription = if (searchQuery.isEmpty()) "No sales" else "No results",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isEmpty()) {
                "No sales yet"
            } else {
                "No sales found"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (searchQuery.isEmpty()) {
                "Tap the sync button to load sales from Odoo"
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
fun SaleSuccessMessage(
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
fun SaleErrorMessage(
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
