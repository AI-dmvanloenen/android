package com.odoo.fieldapp.presentation.payment

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
import com.odoo.fieldapp.domain.model.Payment
import com.odoo.fieldapp.presentation.components.DetailRow
import com.odoo.fieldapp.presentation.components.PaymentStatusBadge
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Payment Detail Screen
 *
 * Displays detailed information about a selected payment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    payment: Payment?,
    onBackClick: () -> Unit,
    onCustomerClick: ((Int) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (payment == null) {
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
            // Payment details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Payment header card
                PaymentHeaderCard(payment)

                // Amount card
                AmountCard(payment)

                // Customer information
                if (payment.partnerId != null || payment.partnerName != null) {
                    CustomerCard(
                        payment = payment,
                        onCustomerClick = onCustomerClick
                    )
                }

                // Payment details
                PaymentDetailsCard(payment)

                // System information
                SystemInfoCard(payment)
            }
        }
    }
}

@Composable
private fun PaymentHeaderCard(payment: Payment) {
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
                    text = payment.name.ifBlank { "Draft Payment" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                PaymentStatusBadge(state = payment.state)
            }
        }
    }
}

@Composable
private fun AmountCard(payment: Payment) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currencyFormatter.format(payment.amount),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun CustomerCard(
    payment: Payment,
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

            payment.partnerName?.let { name ->
                DetailRow(
                    icon = Icons.Default.Person,
                    label = "Customer Name",
                    value = name
                )
            }

            if (payment.partnerId != null && onCustomerClick != null) {
                OutlinedButton(
                    onClick = { onCustomerClick(payment.partnerId) },
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
private fun PaymentDetailsCard(payment: Payment) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Payment Details",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            payment.date?.let { date ->
                DetailRow(
                    icon = Icons.Default.DateRange,
                    label = "Payment Date",
                    value = dateFormatter.format(date)
                )
            }

            DetailRow(
                icon = Icons.Default.Flag,
                label = "Status",
                value = payment.state.replaceFirstChar { it.uppercase() }
            )

            payment.memo?.let { memo ->
                DetailRow(
                    icon = Icons.Default.Description,
                    label = "Memo",
                    value = memo
                )
            }

            payment.journalId?.let { journalId ->
                DetailRow(
                    icon = Icons.Default.AccountBalance,
                    label = "Journal ID",
                    value = journalId.toString()
                )
            }
        }
    }
}

@Composable
private fun SystemInfoCard(payment: Payment) {
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
                value = payment.id.toString()
            )

            payment.mobileUid?.let { uid ->
                DetailRow(
                    icon = Icons.Default.Fingerprint,
                    label = "Mobile UID",
                    value = uid
                )
            }

            DetailRow(
                icon = Icons.Default.Sync,
                label = "Sync Status",
                value = payment.syncState.name
            )

            DetailRow(
                icon = Icons.Default.Update,
                label = "Last Modified",
                value = dateFormatter.format(payment.lastModified)
            )
        }
    }
}
