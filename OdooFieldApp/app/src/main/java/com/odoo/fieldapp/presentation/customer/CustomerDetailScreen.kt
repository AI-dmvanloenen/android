package com.odoo.fieldapp.presentation.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Delivery
import com.odoo.fieldapp.domain.model.Sale
import com.odoo.fieldapp.presentation.components.DetailRow
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Customer Detail Screen
 *
 * Displays detailed information about a selected customer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: Customer?,
    sales: List<Sale> = emptyList(),
    deliveries: List<Delivery> = emptyList(),
    onBackClick: () -> Unit,
    onSaleClick: (Sale) -> Unit = {},
    onDeliveryClick: (Delivery) -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (customer == null) {
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
            // Customer details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer name card
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
                            text = customer.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        customer.taxId?.let { taxId ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tax ID: $taxId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Contact information with interactive elements
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Contact Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Divider()

                        // Phone - tap to call
                        customer.phone?.let { phone ->
                            DetailRow(
                                icon = Icons.Default.Phone,
                                label = "Phone",
                                value = phone,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // Email - tap to send email
                        customer.email?.let { email ->
                            DetailRow(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = email,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // Website - tap to open browser
                        customer.website?.let { website ->
                            DetailRow(
                                icon = Icons.Default.Public,
                                label = "Website",
                                value = website,
                                onClick = {
                                    val url = if (website.startsWith("http://") || website.startsWith("https://")) {
                                        website
                                    } else {
                                        "https://$website"
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // City - not interactive
                        customer.city?.let { city ->
                            DetailRow(
                                icon = Icons.Default.Place,
                                label = "City",
                                value = city
                            )
                        }
                    }
                }

                // Sales Orders
                SalesOrdersCard(
                    sales = sales,
                    onSaleClick = onSaleClick
                )

                // Deliveries
                DeliveriesCard(
                    deliveries = deliveries,
                    onDeliveryClick = onDeliveryClick
                )

                // Additional information
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Additional Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Divider()

                        customer.date?.let { date ->
                            val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            DetailRow(
                                icon = Icons.Default.DateRange,
                                label = "Date",
                                value = dateFormatter.format(date)
                            )
                        }

                        DetailRow(
                            icon = Icons.Default.Info,
                            label = "Odoo ID",
                            value = customer.id.toString()
                        )

                        DetailRow(
                            icon = Icons.Default.Sync,
                            label = "Sync Status",
                            value = customer.syncState.name
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sales Orders card showing all orders for this customer
 */
@Composable
private fun SalesOrdersCard(
    sales: List<Sale>,
    onSaleClick: (Sale) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sales Orders",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${sales.size} order${if (sales.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider()

            if (sales.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No sales orders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sales.forEach { sale ->
                    SaleOrderItem(
                        sale = sale,
                        dateFormatter = dateFormatter,
                        currencyFormatter = currencyFormatter,
                        onClick = { onSaleClick(sale) }
                    )
                }
            }
        }
    }
}

/**
 * Single sale order item in the list
 */
@Composable
private fun SaleOrderItem(
    sale: Sale,
    dateFormatter: SimpleDateFormat,
    currencyFormatter: NumberFormat,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sale.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                sale.dateOrder?.let { date ->
                    Text(
                        text = dateFormatter.format(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            sale.amountTotal?.let { amount ->
                Text(
                    text = currencyFormatter.format(amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Deliveries card showing all deliveries for this customer
 */
@Composable
private fun DeliveriesCard(
    deliveries: List<Delivery>,
    onDeliveryClick: (Delivery) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deliveries",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${deliveries.size} deliver${if (deliveries.size != 1) "ies" else "y"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider()

            if (deliveries.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No deliveries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                deliveries.forEach { delivery ->
                    DeliveryItem(
                        delivery = delivery,
                        dateFormatter = dateFormatter,
                        onClick = { onDeliveryClick(delivery) }
                    )
                }
            }
        }
    }
}

/**
 * Single delivery item in the list
 */
@Composable
private fun DeliveryItem(
    delivery: Delivery,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = delivery.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                delivery.scheduledDate?.let { date ->
                    Text(
                        text = dateFormatter.format(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // State badge
            Surface(
                color = when (delivery.state) {
                    "done" -> MaterialTheme.colorScheme.primaryContainer
                    "assigned" -> MaterialTheme.colorScheme.tertiaryContainer
                    "waiting" -> MaterialTheme.colorScheme.secondaryContainer
                    "cancel" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = delivery.state.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = when (delivery.state) {
                        "done" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "assigned" -> MaterialTheme.colorScheme.onTertiaryContainer
                        "waiting" -> MaterialTheme.colorScheme.onSecondaryContainer
                        "cancel" -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
