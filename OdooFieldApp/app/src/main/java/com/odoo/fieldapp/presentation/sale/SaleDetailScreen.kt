package com.odoo.fieldapp.presentation.sale

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.domain.model.SaleLine
import com.odoo.fieldapp.presentation.components.DetailRow
import com.odoo.fieldapp.presentation.components.SaleStatusBadge
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sale Detail Screen
 *
 * Displays detailed information about a selected sale order
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    sale: Sale?,
    customerName: String? = null,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sale Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sale == null) {
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
            // Sale details
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sale header card
                item {
                    SaleHeaderCard(sale)
                }

                // Order information
                item {
                    SaleInfoCard(sale)
                }

                // Order lines (Items) - styled like DeliveryDetailScreen
                if (sale.lines.isNotEmpty()) {
                    item {
                        Text(
                            text = "Items (${sale.lines.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(sale.lines, key = { it.id }) { line ->
                        SaleLineCard(line)
                    }
                }

                // System information
                item {
                    SystemInfoCard(sale)
                }
            }
        }
    }
}

@Composable
private fun SaleHeaderCard(sale: Sale) {
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
                    text = sale.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                SaleStatusBadge(state = sale.state)
            }

            // Show customer name below SO name
            sale.partnerName?.let { customerName ->
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
private fun SaleInfoCard(sale: Sale) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Order Information",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            sale.dateOrder?.let { date ->
                DetailRow(
                    icon = Icons.Default.DateRange,
                    label = "Order Date",
                    value = dateFormatter.format(date)
                )
            }

            sale.amountTotal?.let { amount ->
                DetailRow(
                    icon = Icons.Default.AttachMoney,
                    label = "Total Amount",
                    value = currencyFormatter.format(amount)
                )
            }

            DetailRow(
                icon = Icons.Default.Flag,
                label = "Status",
                value = when (sale.state) {
                    "draft" -> "Quotation"
                    "sent" -> "Quotation Sent"
                    "sale" -> "Sales Order"
                    "done" -> "Locked"
                    "cancel" -> "Cancelled"
                    else -> sale.state.replaceFirstChar { it.uppercase() }
                }
            )
        }
    }
}

@Composable
private fun SaleLineCard(line: SaleLine) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

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
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
                    // Show quantity
                    Text(
                        text = "${line.productUomQty.formatQty()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = line.uom,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Price info
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unit price with discount
                Text(
                    text = buildString {
                        append(currencyFormatter.format(line.priceUnit))
                        if (line.discount > 0) {
                            append(" (-${line.discount.formatQty()}%)")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Subtotal
                Text(
                    text = currencyFormatter.format(line.priceSubtotal),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Delivery/Invoice status if applicable
            if (line.qtyDelivered > 0 || line.qtyInvoiced > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("${line.qtyDelivered.formatQty()} delivered")
                        if (line.qtyInvoiced > 0) {
                            append(", ${line.qtyInvoiced.formatQty()} invoiced")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Format quantity, removing unnecessary decimals
 */
private fun Double.formatQty(): String {
    return if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }
}

@Composable
private fun SystemInfoCard(sale: Sale) {
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
                value = sale.id.toString()
            )

            DetailRow(
                icon = Icons.Default.Sync,
                label = "Sync Status",
                value = sale.syncState.name
            )

            DetailRow(
                icon = Icons.Default.Update,
                label = "Last Modified",
                value = dateFormatter.format(sale.lastModified)
            )
        }
    }
}
