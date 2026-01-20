package com.odoo.fieldapp.presentation.delivery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.DeliveryLine
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.presentation.components.DeliveryStatusBadge
import com.odoo.fieldapp.presentation.components.DetailRow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Delivery Detail Screen
 *
 * Displays detailed information about a selected delivery order
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailScreen(
    delivery: Delivery?,
    validateState: Resource<Delivery>? = null,
    onBackClick: () -> Unit,
    onValidateClick: (() -> Unit)? = null,
    onClearValidateState: (() -> Unit)? = null,
    onCustomerClick: ((Int) -> Unit)? = null,
    onSaleClick: ((Int) -> Unit)? = null
) {
    // Show snackbar for validation results
    val snackbarHostState = SnackbarHostState()

    LaunchedEffect(validateState) {
        when (validateState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Delivery validated successfully",
                    duration = SnackbarDuration.Short
                )
                onClearValidateState?.invoke()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    message = validateState.message ?: "Validation failed",
                    duration = SnackbarDuration.Long
                )
                onClearValidateState?.invoke()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Delivery Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (delivery == null) {
            // Loading or error state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        } else {
            // Delivery details
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Delivery header card
                item {
                    DeliveryHeaderCard(delivery)
                }

                // Validate button (only for draft or confirmed states)
                if (delivery.state in listOf("draft", "confirmed", "assigned", "waiting") && onValidateClick != null) {
                    item {
                        val isLoading = validateState is Resource.Loading
                        Button(
                            onClick = onValidateClick,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Validating...")
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Validate Delivery")
                            }
                        }
                    }
                }

                // Schedule information
                item {
                    ScheduleInfoCard(delivery)
                }

                // Delivery lines (Items)
                if (delivery.lines.isNotEmpty()) {
                    item {
                        Text(
                            text = "Items (${delivery.lines.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(delivery.lines, key = { it.id }) { line ->
                        DeliveryLineCard(line)
                    }
                }

                // Sale order information
                if (delivery.saleId != null || delivery.saleName != null) {
                    item {
                        SaleInfoCard(
                            delivery = delivery,
                            onSaleClick = onSaleClick
                        )
                    }
                }

                // Customer information
                if (delivery.partnerId != null || delivery.partnerName != null) {
                    item {
                        CustomerInfoCard(
                            delivery = delivery,
                            onCustomerClick = onCustomerClick
                        )
                    }
                }

                // System information
                item {
                    SystemInfoCard(delivery)
                }
            }
        }
    }
}

@Composable
private fun DeliveryHeaderCard(delivery: Delivery) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = delivery.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                DeliveryStatusBadge(state = delivery.state)
            }

            // Show customer name below delivery name
            delivery.partnerName?.let { customerName ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ScheduleInfoCard(delivery: Delivery) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            delivery.scheduledDate?.let { date ->
                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Scheduled Date",
                    value = dateFormatter.format(date)
                )
            } ?: DetailRow(
                icon = Icons.Default.Schedule,
                label = "Scheduled Date",
                value = "Not scheduled"
            )

            DetailRow(
                icon = Icons.Default.Flag,
                label = "Status",
                value = delivery.state.replaceFirstChar { it.uppercase() }
            )
        }
    }
}

@Composable
private fun DeliveryLineCard(line: DeliveryLine) {
    val progress = if (line.quantity > 0) (line.quantityDone / line.quantity).coerceIn(0.0, 1.0) else 0.0
    val isComplete = line.quantityDone >= line.quantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.productName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    line.productId?.let { productId ->
                        Text(
                            text = "Product ID: $productId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Show done/demand
                    Text(
                        text = "${line.quantityDone.toInt()} / ${line.quantity.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = line.uom,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress bar
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Status text
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isComplete) "Complete" else "${(progress * 100).toInt()}% delivered",
                style = MaterialTheme.typography.labelSmall,
                color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SaleInfoCard(
    delivery: Delivery,
    onSaleClick: ((Int) -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sale Order",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            delivery.saleName?.let { name ->
                DetailRow(
                    icon = Icons.Default.ShoppingCart,
                    label = "Sale Order",
                    value = name
                )
            }

            if (delivery.saleId != null && onSaleClick != null) {
                OutlinedButton(
                    onClick = { onSaleClick(delivery.saleId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Sale Order")
                }
            }
        }
    }
}

@Composable
private fun CustomerInfoCard(
    delivery: Delivery,
    onCustomerClick: ((Int) -> Unit)?
) {
    if (delivery.partnerId != null && onCustomerClick != null) {
        OutlinedButton(
            onClick = { onCustomerClick(delivery.partnerId) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Customer")
        }
    }
}

@Composable
private fun SystemInfoCard(delivery: Delivery) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            DetailRow(
                icon = Icons.Default.Info,
                label = "Odoo ID",
                value = delivery.id.toString()
            )

            DetailRow(
                icon = Icons.Default.Sync,
                label = "Sync Status",
                value = delivery.syncState.name
            )

            DetailRow(
                icon = Icons.Default.Update,
                label = "Last Modified",
                value = dateFormatter.format(delivery.lastModified)
            )
        }
    }
}
