package com.odoo.fieldapp.presentation.delivery

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
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.presentation.components.AppSearchBar
import com.odoo.fieldapp.presentation.components.DeliveryStatusBadge
import com.odoo.fieldapp.presentation.components.EmptyStateView
import com.odoo.fieldapp.presentation.components.ErrorBanner
import com.odoo.fieldapp.presentation.components.SuccessBanner
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Filter options for delivery list
 */
enum class DeliveryFilter {
    OPEN,   // All deliveries with state NOT "done"
    CLOSED  // All deliveries with state "done"
}

/**
 * Delivery List Screen
 *
 * Displays a searchable list of deliveries with sync functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryListScreen(
    deliveries: List<Delivery>,
    searchQuery: String,
    syncState: Resource<List<Delivery>>?,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSyncClick: () -> Unit,
    onDeliveryClick: (Delivery) -> Unit,
    onClearSyncState: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(DeliveryFilter.OPEN) }
    val isRefreshing = syncState is Resource.Loading

    // Filter deliveries based on selected filter
    val filteredDeliveries = remember(deliveries, selectedFilter) {
        when (selectedFilter) {
            DeliveryFilter.OPEN -> deliveries.filter { it.state.lowercase() != "done" }
            DeliveryFilter.CLOSED -> deliveries.filter { it.state.lowercase() == "done" }
        }
    }

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
        // Search bar with sync button
        AppSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch,
            placeholder = "Search deliveries...",
            onSyncClick = onSyncClick,
            isSyncing = isRefreshing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Filter buttons
        DeliveryFilterButtons(
            selectedFilter = selectedFilter,
            onFilterChange = { selectedFilter = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sync state messages
        syncState?.let { state ->
            when (state) {
                is Resource.Success -> {
                    SuccessBanner(
                        message = if ((state.data?.size ?: 0) == 0) "There is nothing new to sync" else "Synced ${state.data?.size} deliveries",
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

        // Deliveries list or empty state
        if (filteredDeliveries.isEmpty()) {
            DeliveryEmptyState(
                searchQuery = searchQuery,
                onSyncClick = onSyncClick,
                filter = selectedFilter
            )
        } else {
            DeliveryList(
                deliveries = filteredDeliveries,
                onDeliveryClick = onDeliveryClick
            )
        }
    }
}

/**
 * Filter buttons for Open/Closed deliveries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryFilterButtons(
    selectedFilter: DeliveryFilter,
    onFilterChange: (DeliveryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == DeliveryFilter.OPEN,
            onClick = { onFilterChange(DeliveryFilter.OPEN) },
            label = { Text("Open") },
            leadingIcon = if (selectedFilter == DeliveryFilter.OPEN) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            modifier = Modifier.weight(1f)
        )

        FilterChip(
            selected = selectedFilter == DeliveryFilter.CLOSED,
            onClick = { onFilterChange(DeliveryFilter.CLOSED) },
            label = { Text("Closed") },
            leadingIcon = if (selectedFilter == DeliveryFilter.CLOSED) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Deliveries list component
 */
@Composable
fun DeliveryList(
    deliveries: List<Delivery>,
    onDeliveryClick: (Delivery) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(deliveries, key = { it.id }) { delivery ->
            DeliveryListItem(
                delivery = delivery,
                onClick = { onDeliveryClick(delivery) }
            )
        }
    }
}

/**
 * Single delivery list item with chevron
 */
@Composable
fun DeliveryListItem(
    delivery: Delivery,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }

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
                // Delivery name/reference with improved typography
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = delivery.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // State badge
                    DeliveryStatusBadge(state = delivery.state)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Customer and scheduled date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Customer name
                    delivery.partnerName?.let { customerName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = customerName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Scheduled date
                    delivery.scheduledDate?.let { date ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
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

                // Line count indicator
                if (delivery.lines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${delivery.lines.size} item${if (delivery.lines.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
 * Empty state when no deliveries are found
 */
@Composable
fun DeliveryEmptyState(
    searchQuery: String,
    onSyncClick: () -> Unit,
    filter: DeliveryFilter = DeliveryFilter.OPEN
) {
    when {
        searchQuery.isNotEmpty() -> {
            EmptyStateView(
                icon = Icons.Default.SearchOff,
                title = "No deliveries found",
                subtitle = "No results match \"$searchQuery\". Try a different search term."
            )
        }
        filter == DeliveryFilter.CLOSED -> {
            EmptyStateView(
                icon = Icons.Default.CheckCircle,
                title = "No closed deliveries",
                subtitle = "No deliveries have been completed yet"
            )
        }
        else -> {
            EmptyStateView(
                icon = Icons.Default.LocalShipping,
                title = "No open deliveries",
                subtitle = "Tap the button below to sync deliveries from Odoo",
                actionLabel = "Sync Now",
                onActionClick = onSyncClick
            )
        }
    }
}
