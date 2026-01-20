package com.odoo.fieldapp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared search bar component with optional sync button
 *
 * @param query Current search query
 * @param onQueryChange Callback when query changes
 * @param onClearClick Callback when clear button is clicked
 * @param placeholder Placeholder text
 * @param onSyncClick Optional sync button click handler
 * @param isSyncing Whether sync is in progress
 * @param modifier Modifier for the component
 */
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    placeholder: String = "Search...",
    onSyncClick: (() -> Unit)? = null,
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearClick) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        if (onSyncClick != null) {
            IconButton(
                onClick = onSyncClick,
                enabled = !isSyncing
            ) {
                Icon(
                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.Refresh,
                    contentDescription = "Sync"
                )
            }
        }
    }
}
