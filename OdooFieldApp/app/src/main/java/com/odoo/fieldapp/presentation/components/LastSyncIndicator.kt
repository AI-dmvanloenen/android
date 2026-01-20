package com.odoo.fieldapp.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Indicator showing last sync time and pending sync count
 *
 * Displays relative time like "Synced 5m ago" and optional pending count.
 * Updates automatically every minute to keep time display current.
 */
@Composable
fun LastSyncIndicator(
    lastSyncTime: Date?,
    pendingSyncCount: Int,
    modifier: Modifier = Modifier
) {
    // Force recomposition every minute to update relative time
    var updateTrigger by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // Update every minute
            updateTrigger = System.currentTimeMillis()
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pending sync count
        if (pendingSyncCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Pending,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$pendingSyncCount pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Last sync time
        lastSyncTime?.let { syncTime ->
            val relativeTime = getRelativeTimeString(syncTime, updateTrigger)
            val icon = if (pendingSyncCount > 0) Icons.Default.CloudSync else Icons.Default.CloudDone
            val color = if (pendingSyncCount > 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Synced $relativeTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        } ?: run {
            // Never synced
            Text(
                text = "Not synced yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Get relative time string like "5m ago", "2h ago", "yesterday"
 */
@Suppress("UNUSED_PARAMETER")
private fun getRelativeTimeString(date: Date, updateTrigger: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time

    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "yesterday"
        days < 7 -> "${days}d ago"
        else -> "over a week ago"
    }
}
