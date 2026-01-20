package com.odoo.fieldapp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Generic empty state component with icon, title, subtitle, and optional action button
 *
 * @param icon The icon to display
 * @param title The main title text
 * @param subtitle The subtitle/description text
 * @param actionLabel Optional action button label
 * @param onActionClick Optional action button click handler
 * @param modifier Modifier for the component
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onActionClick) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}

/**
 * Empty state for search results
 */
@Composable
fun SearchEmptyState(
    searchQuery: String,
    entityName: String = "items",
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.SearchOff,
        title = "No $entityName found",
        subtitle = "No results match \"$searchQuery\". Try a different search term.",
        modifier = modifier
    )
}

/**
 * Empty state for no data (before sync)
 */
@Composable
fun NoDataEmptyState(
    entityName: String = "items",
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.CloudDownload,
        title = "No $entityName yet",
        subtitle = "Tap the button below to sync data from Odoo",
        actionLabel = "Sync Now",
        onActionClick = onSyncClick,
        modifier = modifier
    )
}
