package com.odoo.fieldapp.presentation.sale

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Sale
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sale header card
                SaleHeaderCard(sale)

                // Order information
                SaleInfoCard(sale)

                // Customer information
                if (sale.partnerId != null || customerName != null) {
                    CustomerInfoCard(sale = sale, customerName = customerName)
                }

                // System information
                SystemInfoCard(sale)
            }
        }
    }
}

@Composable
private fun SaleHeaderCard(sale: Sale) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

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
            Text(
                text = sale.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            sale.amountTotal?.let { amount ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currencyFormatter.format(amount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                SaleDetailRow(
                    icon = Icons.Default.DateRange,
                    label = "Order Date",
                    value = dateFormatter.format(date)
                )
            }

            sale.amountTotal?.let { amount ->
                SaleDetailRow(
                    icon = Icons.Default.AttachMoney,
                    label = "Total Amount",
                    value = currencyFormatter.format(amount)
                )
            }
        }
    }
}

@Composable
private fun CustomerInfoCard(sale: Sale, customerName: String?) {
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

            // Show customer name (from lookup) or fall back to partner name from sale
            val displayName = customerName ?: sale.partnerName
            displayName?.let { name ->
                SaleDetailRow(
                    icon = Icons.Default.Person,
                    label = "Customer Name",
                    value = name
                )
            }

            sale.partnerId?.let { id ->
                SaleDetailRow(
                    icon = Icons.Default.Badge,
                    label = "Customer ID",
                    value = id.toString()
                )
            }
        }
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

            SaleDetailRow(
                icon = Icons.Default.Info,
                label = "Odoo ID",
                value = sale.id.toString()
            )

            SaleDetailRow(
                icon = Icons.Default.Sync,
                label = "Sync Status",
                value = sale.syncState.name
            )

            SaleDetailRow(
                icon = Icons.Default.Update,
                label = "Last Modified",
                value = dateFormatter.format(sale.lastModified)
            )
        }
    }
}

/**
 * Reusable detail row component
 */
@Composable
fun SaleDetailRow(
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
