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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.presentation.components.AppSearchBar
import com.odoo.fieldapp.presentation.components.EmptyStateView
import com.odoo.fieldapp.presentation.components.ErrorBanner
import com.odoo.fieldapp.presentation.components.SaleStatusBadge
import com.odoo.fieldapp.presentation.components.SuccessBanner
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
    onCreateClick: () -> Unit,
    onClearSyncState: () -> Unit
) {
    val isRefreshing = syncState is Resource.Loading

    // Auto-dismiss success messages after 3 seconds
    LaunchedEffect(syncState) {
        if (syncState is Resource.Success) {
            delay(3000)
            onClearSyncState()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Sale Order")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar with sync button
            AppSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearClick = onClearSearch,
                placeholder = "Search sales...",
                onSyncClick = onSyncClick,
                isSyncing = isRefreshing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Sync state messages
            syncState?.let { state ->
                when (state) {
                    is Resource.Success -> {
                        SuccessBanner(
                            message = if ((state.data?.size ?: 0) == 0) "There is nothing new to sync" else "Synced ${state.data?.size} sales",
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
 * Single sale list item with chevron
 */
@Composable
fun SaleListItem(
    sale: Sale,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

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
                // Sale name with customer name and improved typography
                Text(
                    text = if (sale.partnerName != null) {
                        "${sale.name} - ${sale.partnerName}"
                    } else {
                        sale.name
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date and State
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
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

                    // State badge
                    SaleStatusBadge(state = sale.state)
                }
            }

            // Amount
            sale.amountTotal?.let { amount ->
                Text(
                    text = currencyFormatter.format(amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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
 * Empty state when no sales are found
 */
@Composable
fun SaleEmptyState(
    searchQuery: String,
    onSyncClick: () -> Unit
) {
    if (searchQuery.isNotEmpty()) {
        EmptyStateView(
            icon = Icons.Default.SearchOff,
            title = "No sales found",
            subtitle = "No results match \"$searchQuery\". Try a different search term."
        )
    } else {
        EmptyStateView(
            icon = Icons.Default.ShoppingCart,
            title = "No sales yet",
            subtitle = "Tap the button below to sync sales from Odoo",
            actionLabel = "Sync Now",
            onActionClick = onSyncClick
        )
    }
}
