package com.odoo.fieldapp.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odoo.fieldapp.R
import com.odoo.fieldapp.domain.model.DashboardStats
import com.odoo.fieldapp.domain.model.Resource
import com.odoo.fieldapp.presentation.components.ErrorBanner
import com.odoo.fieldapp.presentation.components.LastSyncIndicator
import com.odoo.fieldapp.presentation.components.SuccessBanner
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard Screen
 *
 * Shows action cards with pending work counts and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    stats: DashboardStats,
    syncState: Resource<Unit>?,
    pendingSyncCount: Int,
    lastSyncTime: Date?,
    onDeliveriesClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onSyncErrorsClick: () -> Unit,
    onCreatePaymentClick: () -> Unit,
    onCreateCustomerClick: () -> Unit,
    onCreateSaleClick: () -> Unit,
    onSyncAllClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClearSyncState: () -> Unit
) {
    val isSyncing = syncState is Resource.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo_gocongo),
                        contentDescription = "GoCongo Logo",
                        modifier = Modifier.height(36.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSyncAllClick,
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Greeting header
            GreetingHeader()

            Spacer(modifier = Modifier.height(8.dp))

            // Last sync indicator
            LastSyncIndicator(
                lastSyncTime = lastSyncTime,
                pendingSyncCount = pendingSyncCount
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-dismiss success messages after 3 seconds
            LaunchedEffect(syncState) {
                if (syncState is Resource.Success) {
                    delay(3000)
                    onClearSyncState()
                }
            }

            // Sync state messages
            syncState?.let { state ->
                when (state) {
                    is Resource.Success -> {
                        SuccessBanner(
                            message = "All data synced successfully",
                            onDismiss = onClearSyncState
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is Resource.Error -> {
                        ErrorBanner(
                            message = state.message ?: "Sync failed",
                            onDismiss = onClearSyncState
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is Resource.Loading -> {
                        // Loading indicator is in the Sync All button
                    }
                }
            }

            // Action cards grid
            ActionCardsGrid(
                stats = stats,
                onDeliveriesClick = onDeliveriesClick,
                onPaymentsClick = onPaymentsClick,
                onSyncErrorsClick = onSyncErrorsClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick actions section
            QuickActionsSection(
                onCreatePaymentClick = onCreatePaymentClick,
                onCreateCustomerClick = onCreateCustomerClick,
                onCreateSaleClick = onCreateSaleClick
            )
        }
    }
}

/**
 * Greeting header with date
 */
@Composable
private fun GreetingHeader() {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val dateString = dateFormat.format(calendar.time)

    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 2x2 grid of action cards
 */
@Composable
private fun ActionCardsGrid(
    stats: DashboardStats,
    onDeliveriesClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onSyncErrorsClick: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(280.dp)
    ) {
        item {
            ActionCard(
                count = stats.deliveriesToComplete,
                title = "Deliveries",
                subtitle = "to Complete",
                icon = Icons.Default.LocalShipping,
                cardType = if (stats.deliveriesToComplete > 0) CardType.WARNING else CardType.NEUTRAL,
                onClick = onDeliveriesClick
            )
        }
        item {
            ActionCard(
                count = stats.todaysDeliveries,
                title = "Today's",
                subtitle = "Deliveries",
                icon = Icons.Default.Today,
                cardType = if (stats.todaysDeliveries > 0) CardType.INFO else CardType.NEUTRAL,
                onClick = onDeliveriesClick
            )
        }
        item {
            ActionCard(
                count = stats.pendingPayments,
                title = "Pending",
                subtitle = "Payments",
                icon = Icons.Default.Payment,
                cardType = if (stats.pendingPayments > 0) CardType.WARNING else CardType.NEUTRAL,
                onClick = onPaymentsClick
            )
        }
        item {
            ActionCard(
                count = stats.syncErrors,
                title = "Sync",
                subtitle = "Errors",
                icon = Icons.Default.SyncProblem,
                cardType = if (stats.syncErrors > 0) CardType.ERROR else CardType.SUCCESS,
                onClick = onSyncErrorsClick
            )
        }
    }
}

/**
 * Card type determines color scheme
 */
private enum class CardType {
    SUCCESS, // Green - all clear
    WARNING, // Orange - needs attention
    ERROR,   // Red - critical
    INFO,    // Blue - informational
    NEUTRAL  // Gray - no items
}

/**
 * Single action card
 */
@Composable
private fun ActionCard(
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    cardType: CardType,
    onClick: () -> Unit
) {
    val (containerColor, contentColor) = when (cardType) {
        CardType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        CardType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        CardType.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        CardType.INFO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        CardType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Quick actions section with buttons
 */
@Composable
private fun QuickActionsSection(
    onCreatePaymentClick: () -> Unit,
    onCreateCustomerClick: () -> Unit,
    onCreateSaleClick: () -> Unit
) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = onCreateSaleClick,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sale")
        }

        OutlinedButton(
            onClick = onCreatePaymentClick,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Icon(
                Icons.Default.AttachMoney,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Payment")
        }

        OutlinedButton(
            onClick = onCreateCustomerClick,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Customer")
        }
    }
}
