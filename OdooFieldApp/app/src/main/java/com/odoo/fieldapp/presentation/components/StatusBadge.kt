package com.odoo.fieldapp.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Unified status badge component for deliveries, payments, and sales
 *
 * @param label The text to display
 * @param containerColor Background color of the badge
 * @param contentColor Text color
 * @param modifier Modifier for the component
 */
@Composable
fun StatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Status badge with alpha background (for outlined look)
 */
@Composable
fun StatusBadgeOutlined(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Delivery state badge with predefined colors
 */
@Composable
fun DeliveryStatusBadge(
    state: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (state.lowercase()) {
        "done" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "assigned" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "waiting" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "confirmed" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "cancel" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    StatusBadge(
        label = state.replaceFirstChar { it.uppercase() },
        containerColor = backgroundColor,
        contentColor = textColor,
        modifier = modifier
    )
}

/**
 * Payment state badge with predefined colors
 */
@Composable
fun PaymentStatusBadge(
    state: String,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, label) = when (state) {
        "posted" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Posted"
        )
        "draft" -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Draft"
        )
        "cancelled" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Cancelled"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            state.replaceFirstChar { it.uppercase() }
        )
    }

    StatusBadge(
        label = label,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier
    )
}

/**
 * Sale state badge with predefined colors
 */
@Composable
fun SaleStatusBadge(
    state: String,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (state) {
        "draft" -> MaterialTheme.colorScheme.outline to "Quotation"
        "sent" -> MaterialTheme.colorScheme.secondary to "Sent"
        "sale" -> MaterialTheme.colorScheme.primary to "Sales Order"
        "done" -> MaterialTheme.colorScheme.tertiary to "Locked"
        "cancel" -> MaterialTheme.colorScheme.error to "Cancelled"
        else -> MaterialTheme.colorScheme.outline to state.replaceFirstChar { it.uppercase() }
    }

    StatusBadgeOutlined(
        label = label,
        color = color,
        modifier = modifier
    )
}
