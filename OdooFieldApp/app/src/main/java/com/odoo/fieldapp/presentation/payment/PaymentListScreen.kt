package com.odoo.fieldapp.presentation.payment

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
import com.odoo.fieldapp.domain.model.Payment
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.presentation.components.EmptyStateView
import com.odoo.fieldapp.presentation.components.PaymentStatusBadge
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Payment List Screen
 *
 * Displays a list of all payments with search and sync functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentListScreen(
    payments: List<Payment>,
    searchQuery: String,
    syncState: Resource<List<Payment>>?,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSyncClick: () -> Unit,
    onPaymentClick: (Payment) -> Unit,
    onCreateClick: () -> Unit,
    onClearSyncState: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val isRefreshing = syncState is Resource.Loading

    // Show snackbar for sync results
    LaunchedEffect(syncState) {
        when (syncState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(
                    message = if ((syncState.data?.size ?: 0) == 0) "There is nothing new to sync" else "Synced ${syncState.data?.size} payments",
                    duration = SnackbarDuration.Short
                )
                onClearSyncState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    message = syncState.message ?: "Sync failed",
                    duration = SnackbarDuration.Long
                )
                onClearSyncState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Payments") },
                actions = {
                    // Sync button
                    IconButton(
                        onClick = onSyncClick,
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search payments...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            // Payments list or empty state
            if (payments.isEmpty()) {
                PaymentEmptyState(
                    searchQuery = searchQuery,
                    onSyncClick = onSyncClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(payments, key = { it.id }) { payment ->
                        PaymentCard(
                            payment = payment,
                            onClick = { onPaymentClick(payment) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single payment card in the list with chevron
 */
@Composable
private fun PaymentCard(
    payment: Payment,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Payment name/reference with improved typography
                Text(
                    text = payment.name.ifBlank { "Draft Payment" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Customer name
                Text(
                    text = payment.partnerName ?: "Customer #${payment.partnerId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date
                payment.date?.let { date ->
                    Text(
                        text = dateFormatter.format(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                // Amount
                Text(
                    text = currencyFormatter.format(payment.amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // State badge
                PaymentStatusBadge(state = payment.state)
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
 * Empty state for payments
 */
@Composable
fun PaymentEmptyState(
    searchQuery: String,
    onSyncClick: () -> Unit
) {
    if (searchQuery.isNotEmpty()) {
        EmptyStateView(
            icon = Icons.Default.SearchOff,
            title = "No payments found",
            subtitle = "No results match \"$searchQuery\". Try a different search term."
        )
    } else {
        EmptyStateView(
            icon = Icons.Default.Payment,
            title = "No payments yet",
            subtitle = "Tap the button below to sync payments from Odoo",
            actionLabel = "Sync Now",
            onActionClick = onSyncClick
        )
    }
}
