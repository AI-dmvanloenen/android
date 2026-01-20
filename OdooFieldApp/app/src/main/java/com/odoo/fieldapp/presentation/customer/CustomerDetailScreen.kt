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
import com.odoo.fieldapp.domain.model.SyncState
import com.odoo.fieldapp.domain.model.Visit
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
    visits: List<Visit> = emptyList(),
    onBackClick: () -> Unit,
    onSaleClick: (Sale) -> Unit = {},
    onDeliveryClick: (Delivery) -> Unit = {},
    onAddVisitClick: () -> Unit = {},
    locationState: com.odoo.fieldapp.domain.model.Resource<Unit>? = null,
    showLocationConfirmDialog: Boolean = false,
    isCapturingLocation: Boolean = false,
    onCaptureLocationClick: () -> Unit = {},
    onConfirmLocationUpdate: () -> Unit = {},
    onDismissLocationDialog: () -> Unit = {},
    onClearLocationState: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // Show snackbar for location state changes
    androidx.compose.runtime.LaunchedEffect(locationState) {
        when (locationState) {
            is com.odoo.fieldapp.domain.model.Resource.Success -> {
                snackbarHostState.showSnackbar(
                    message = locationState.message ?: "Location captured successfully",
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                onClearLocationState()
            }
            is com.odoo.fieldapp.domain.model.Resource.Error -> {
                snackbarHostState.showSnackbar(
                    message = locationState.message ?: "Failed to capture location",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                onClearLocationState()
            }
            else -> { /* Ignore Loading and null */ }
        }
    }

    // Location confirmation dialog
    if (showLocationConfirmDialog && customer != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissLocationDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = if (customer.latitude != null && customer.longitude != null) {
                        "Update Location?"
                    } else {
                        "Capture Location?"
                    }
                )
            },
            text = {
                Text(
                    if (customer.latitude != null && customer.longitude != null) {
                        "Are you sure you want to update/overwrite the GPS location of this customer?"
                    } else {
                        "Capture the current GPS location for this customer?"
                    }
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onConfirmLocationUpdate) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismissLocationDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Customer Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (customer != null) {
                FloatingActionButton(
                    onClick = onAddVisitClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Visit"
                    )
                }
            }
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

                // GPS Location Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "GPS Location",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Divider()

                        // Show location coordinates if available
                        if (customer.latitude != null && customer.longitude != null) {
                            DetailRow(
                                icon = Icons.Default.LocationOn,
                                label = "Latitude",
                                value = String.format("%.6f", customer.latitude)
                            )
                            DetailRow(
                                icon = Icons.Default.LocationOn,
                                label = "Longitude",
                                value = String.format("%.6f", customer.longitude)
                            )

                            // View on Map button
                            val isValidCoordinate = customer.latitude!! in -90.0..90.0 &&
                                    customer.longitude!! in -180.0..180.0
                            OutlinedButton(
                                onClick = {
                                    if (isValidCoordinate) {
                                        val geoUri = "geo:${customer.latitude},${customer.longitude}?q=${customer.latitude},${customer.longitude}(${Uri.encode(customer.name)})"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isValidCoordinate
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View on Map")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Update Location button
                            Button(
                                onClick = onCaptureLocationClick,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isCapturingLocation
                            ) {
                                if (isCapturingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capturing Location...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Update Location")
                                }
                            }
                        } else {
                            // No location yet
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No location captured yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Capture Location button
                            Button(
                                onClick = onCaptureLocationClick,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isCapturingLocation
                            ) {
                                if (isCapturingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capturing Location...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capture Location")
                                }
                            }
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

                // Visits
                VisitsCard(visits = visits)

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

/**
 * Visits card showing all visits logged for this customer
 */
@Composable
private fun VisitsCard(visits: List<Visit>) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

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
                    text = "Visits",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${visits.size} visit${if (visits.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider()

            if (visits.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No visits logged yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                visits.forEach { visit ->
                    VisitItem(
                        visit = visit,
                        dateFormatter = dateFormatter
                    )
                }
            }
        }
    }
}

/**
 * Single visit item in the list
 */
@Composable
private fun VisitItem(
    visit: Visit,
    dateFormatter: SimpleDateFormat
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(visit.visitDatetime),
                    style = MaterialTheme.typography.bodyLarge
                )

                // Sync state badge (only show if PENDING or ERROR)
                if (visit.syncState == SyncState.PENDING || visit.syncState == SyncState.ERROR) {
                    Surface(
                        color = when (visit.syncState) {
                            SyncState.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                            SyncState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = when (visit.syncState) {
                                SyncState.PENDING -> "Syncing..."
                                SyncState.ERROR -> "Error"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = when (visit.syncState) {
                                SyncState.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
                                SyncState.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            visit.memo?.let { memo ->
                if (memo.isNotBlank()) {
                    Text(
                        text = memo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
