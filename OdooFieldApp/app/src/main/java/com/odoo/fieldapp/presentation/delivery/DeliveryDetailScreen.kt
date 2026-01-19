package com.odoo.fieldapp.presentation.delivery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.DeliveryLine
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
    onBackClick: () -> Unit,
    onCustomerClick: ((Int) -> Unit)? = null,
    onSaleClick: ((Int) -> Unit)? = null
) {
    Scaffold(
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

                // Customer information
                if (delivery.partnerId != null || delivery.partnerName != null) {
                    item {
                        CustomerInfoCard(
                            delivery = delivery,
                            onCustomerClick = onCustomerClick
                        )
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

                // Schedule information
                item {
                    ScheduleInfoCard(delivery)
                }

                // Delivery lines
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

                DeliveryStateBadge(state = delivery.state)
            }
        }
    }
}

@Composable
private fun CustomerInfoCard(
    delivery: Delivery,
    onCustomerClick: ((Int) -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Customer",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            delivery.partnerName?.let { name ->
                DeliveryDetailRow(
                    icon = Icons.Default.Person,
                    label = "Customer Name",
                    value = name
                )
            }

            delivery.partnerId?.let { id ->
                DeliveryDetailRow(
                    icon = Icons.Default.Badge,
                    label = "Customer ID",
                    value = id.toString()
                )
            }

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
                DeliveryDetailRow(
                    icon = Icons.Default.ShoppingCart,
                    label = "Sale Order",
                    value = name
                )
            }

            delivery.saleId?.let { id ->
                DeliveryDetailRow(
                    icon = Icons.Default.Badge,
                    label = "Sale ID",
                    value = id.toString()
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
                DeliveryDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Scheduled Date",
                    value = dateFormatter.format(date)
                )
            } ?: DeliveryDetailRow(
                icon = Icons.Default.Schedule,
                label = "Scheduled Date",
                value = "Not scheduled"
            )

            DeliveryDetailRow(
                icon = Icons.Default.Flag,
                label = "Status",
                value = delivery.state.replaceFirstChar { it.uppercase() }
            )
        }
    }
}

@Composable
private fun DeliveryLineCard(line: DeliveryLine) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = line.productName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "ID: ${line.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${line.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = line.uom,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

            DeliveryDetailRow(
                icon = Icons.Default.Info,
                label = "Odoo ID",
                value = delivery.id.toString()
            )

            DeliveryDetailRow(
                icon = Icons.Default.Sync,
                label = "Sync Status",
                value = delivery.syncState.name
            )

            DeliveryDetailRow(
                icon = Icons.Default.Update,
                label = "Last Modified",
                value = dateFormatter.format(delivery.lastModified)
            )
        }
    }
}

/**
 * Reusable detail row component
 */
@Composable
fun DeliveryDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
