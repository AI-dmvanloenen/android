package com.odoo.fieldapp.presentation.customer

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
import com.odoo.fieldapp.domain.model.Customer
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
    onBackClick: () -> Unit
) {
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
                
                // Contact information
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
                        
                        customer.phone?.let { phone ->
                            DetailRow(
                                icon = Icons.Default.Phone,
                                label = "Phone",
                                value = phone
                            )
                        }
                        
                        customer.email?.let { email ->
                            DetailRow(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = email
                            )
                        }
                        
                        customer.website?.let { website ->
                            DetailRow(
                                icon = Icons.Default.Public,
                                label = "Website",
                                value = website
                            )
                        }
                        
                        customer.city?.let { city ->
                            DetailRow(
                                icon = Icons.Default.Place,
                                label = "City",
                                value = city
                            )
                        }
                    }
                }
                
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
 * Reusable detail row component
 */
@Composable
fun DetailRow(
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
