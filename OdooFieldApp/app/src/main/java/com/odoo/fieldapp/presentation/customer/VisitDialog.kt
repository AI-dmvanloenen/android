package com.odoo.fieldapp.presentation.customer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.domain.model.Visit
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for logging a customer visit
 *
 * For MVP, datetime defaults to current time and is not editable.
 * Future enhancement can add DatePicker/TimePicker.
 */
@Composable
fun VisitDialog(
    customerName: String,
    visitDatetime: Date,
    visitMemo: String,
    createState: Resource<Visit>?,
    onDismiss: () -> Unit,
    onMemoChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Log Visit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Customer Name (read-only)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Customer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = customerName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Datetime (read-only, formatted)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Visit Date & Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDatetime(visitDatetime),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Memo Field (multi-line, optional)
                OutlinedTextField(
                    value = visitMemo,
                    onValueChange = onMemoChange,
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("What did you discuss?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    singleLine = false,
                    maxLines = 5,
                    enabled = createState !is Resource.Loading
                )

                // Error Message
                if (createState is Resource.Error) {
                    Text(
                        text = createState.message ?: "An error occurred",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Loading Indicator
                if (createState is Resource.Loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = createState !is Resource.Loading
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = createState !is Resource.Loading
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Format datetime as "MMM dd, yyyy at h:mm a"
 * Example: "Jan 20, 2026 at 3:45 PM"
 */
private fun formatDatetime(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(date)
}
