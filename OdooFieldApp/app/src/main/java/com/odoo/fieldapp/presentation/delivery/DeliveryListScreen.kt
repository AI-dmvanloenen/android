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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.Resource
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
        // Search bar
        DeliverySearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch,
            onSyncClick = onSyncClick,
            isSyncing = syncState is Resource.Loading,
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
                    DeliverySuccessMessage(
                        message = "Synced ${state.data?.size ?: 0} deliveries",
                        onDismiss = onClearSyncState
                    )
                }
                is Resource.Error -> {
                    DeliveryErrorMessage(
                        message = state.message ?: "Sync failed",
                        onDismiss = onClearSyncState
                    )
                }
                is Resource.Loading -> {
                    // Loading indicator shown in search bar
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
 * Search bar with sync button
 */
@Composable
fun DeliverySearchBar(
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
            placeholder = { Text("Search deliveries...") },
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
 * Single delivery list item
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Delivery name/reference and state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = delivery.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // State badge
                DeliveryStateBadge(state = delivery.state)
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
    }
}

/**
 * State badge component
 */
@Composable
fun DeliveryStateBadge(state: String) {
    val (backgroundColor, textColor) = when (state.lowercase()) {
        "done" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "assigned" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "waiting" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "confirmed" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "cancel" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = state.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (searchQuery.isEmpty()) Icons.Default.LocalShipping else Icons.Default.Search,
            contentDescription = if (searchQuery.isEmpty()) "No deliveries" else "No results",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                searchQuery.isNotEmpty() -> "No deliveries found"
                filter == DeliveryFilter.OPEN -> "No open deliveries"
                filter == DeliveryFilter.CLOSED -> "No closed deliveries"
                else -> "No deliveries yet"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                searchQuery.isNotEmpty() -> "Try a different search term"
                filter == DeliveryFilter.OPEN -> "All deliveries have been completed"
                filter == DeliveryFilter.CLOSED -> "No deliveries have been completed yet"
                else -> "Tap the sync button to load deliveries from Odoo"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (searchQuery.isEmpty() && filter == DeliveryFilter.OPEN) {
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
fun DeliverySuccessMessage(
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
fun DeliveryErrorMessage(
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
