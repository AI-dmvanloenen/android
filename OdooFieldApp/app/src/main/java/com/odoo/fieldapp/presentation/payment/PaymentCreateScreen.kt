package com.odoo.fieldapp.presentation.payment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.domain.model.Customer
import com.odoo.fieldapp.domain.model.Resource

/**
 * Payment Create Screen
 *
 * Form for creating a new payment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCreateScreen(
    // Customer selection
    customers: List<Customer>,
    selectedPartnerId: Int?,
    selectedPartnerName: String,
    // Form fields
    amount: String,
    memo: String,
    // Errors
    partnerError: String?,
    amountError: String?,
    // Create state
    createState: Resource<*>?,
    // Callbacks
    onPartnerChange: (Int?, String) -> Unit,
    onAmountChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onClearCreateState: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCustomerDropdown by remember { mutableStateOf(false) }

    // Handle create results
    LaunchedEffect(createState) {
        when (createState) {
            is Resource.Success -> {
                // Navigate back immediately on success
                onClearCreateState()
                onBackClick()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    message = createState.message ?: "Creation failed",
                    duration = SnackbarDuration.Long
                )
                onClearCreateState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Payment") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer selection
            ExposedDropdownMenuBox(
                expanded = showCustomerDropdown,
                onExpandedChange = { showCustomerDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedPartnerName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer *") },
                    placeholder = { Text("Select a customer") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerDropdown)
                    },
                    isError = partnerError != null,
                    supportingText = partnerError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = showCustomerDropdown,
                    onDismissRequest = { showCustomerDropdown = false }
                ) {
                    if (customers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No customers available. Sync customers first.") },
                            onClick = { showCustomerDropdown = false },
                            enabled = false
                        )
                    } else {
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(customer.name)
                                        customer.city?.let { city ->
                                            Text(
                                                text = city,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onPartnerChange(customer.id, customer.name)
                                    showCustomerDropdown = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }

            // Amount field
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Amount *") },
                placeholder = { Text("0.00") },
                leadingIcon = {
                    Icon(Icons.Default.AttachMoney, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Memo field
            OutlinedTextField(
                value = memo,
                onValueChange = onMemoChange,
                label = { Text("Memo (optional)") },
                placeholder = { Text("Payment reference or notes") },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Info card
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "The payment will be created as a draft and synced to Odoo. A bank journal will be automatically assigned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button (bottom)
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = createState !is Resource.Loading
            ) {
                if (createState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Payment")
                }
            }
        }
    }
}
