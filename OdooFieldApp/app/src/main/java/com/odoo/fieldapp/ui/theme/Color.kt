package com.odoo.fieldapp.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand colors (GoCongo teal-green)
val GoCongoGreen = Color(0xFF00897B)       // Modern teal
val GoCongoGreenLight = Color(0xFF4DB6AC)  // Vibrant light
val GoCongoGreenDark = Color(0xFF00695C)   // Deep teal

// Primary palette - Light
val Primary40 = Color(0xFF00897B)
val OnPrimary40 = Color(0xFFFFFFFF)
val PrimaryContainer40 = Color(0xFFB2DFDB)  // Light teal cards
val OnPrimaryContainer40 = Color(0xFF00251A)

// Primary palette - Dark
val Primary80 = Color(0xFF80CBC4)
val OnPrimary80 = Color(0xFF003731)
val PrimaryContainer80 = Color(0xFF00695C)
val OnPrimaryContainer80 = Color(0xFFB2DFDB)

// Secondary palette - Light (Coral Accent)
val Secondary40 = Color(0xFFFF7043)         // Warm coral
val OnSecondary40 = Color(0xFFFFFFFF)
val SecondaryContainer40 = Color(0xFFFFCCBC) // Light peach
val OnSecondaryContainer40 = Color(0xFF3E1500)

// Secondary palette - Dark
val Secondary80 = Color(0xFFFFAB91)
val OnSecondary80 = Color(0xFF5D1A00)
val SecondaryContainer80 = Color(0xFF7C3A21)
val OnSecondaryContainer80 = Color(0xFFFFCCBC)

// Tertiary palette - Light (Blue for variety)
val Tertiary40 = Color(0xFF0288D1)          // Brighter blue
val OnTertiary40 = Color(0xFFFFFFFF)
val TertiaryContainer40 = Color(0xFFB3E5FC) // Light blue cards
val OnTertiaryContainer40 = Color(0xFF001F23)

// Tertiary palette - Dark
val Tertiary80 = Color(0xFF81D4FA)
val OnTertiary80 = Color(0xFF00363C)
val TertiaryContainer80 = Color(0xFF01579B)
val OnTertiaryContainer80 = Color(0xFFB3E5FC)

// Error palette
val Error40 = Color(0xFFBA1A1A)
val OnError40 = Color(0xFFFFFFFF)
val ErrorContainer40 = Color(0xFFFFDAD6)
val OnErrorContainer40 = Color(0xFF410002)

val Error80 = Color(0xFFFFB4AB)
val OnError80 = Color(0xFF690005)
val ErrorContainer80 = Color(0xFF93000A)
val OnErrorContainer80 = Color(0xFFFFDAD6)

// Neutral palette - Light (Clean whites)
val Surface40 = Color(0xFFFFFFFF)           // Pure white
val OnSurface40 = Color(0xFF1C1B1F)         // Near black
val SurfaceVariant40 = Color(0xFFF5F5F5)    // Light gray
val OnSurfaceVariant40 = Color(0xFF49454F)
val Outline40 = Color(0xFFE0E0E0)           // Subtle gray
val OutlineVariant40 = Color(0xFFCAC4D0)

// Neutral palette - Dark
val Surface80 = Color(0xFF1C1B1F)
val OnSurface80 = Color(0xFFE6E1E5)
val SurfaceVariant80 = Color(0xFF49454F)
val OnSurfaceVariant80 = Color(0xFFCAC4D0)
val Outline80 = Color(0xFF938F99)
val OutlineVariant80 = Color(0xFF49454F)

// Semantic colors for business logic
object SemanticColors {
    // Status colors
    val StatusSynced = Color(0xFF4CAF50)      // Green - synced/done
    val StatusPending = Color(0xFFFF9800)     // Amber - pending/waiting
    val StatusError = Color(0xFFE53935)       // Red - error/cancelled
    val StatusDraft = Color(0xFF9E9E9E)       // Grey - draft

    // Financial colors
    val AmountPositive = Color(0xFF00897B)    // Teal for positive amounts (matches new primary)
    val AmountNegative = Color(0xFFD32F2F)    // Red for negative amounts

    // Delivery states
    val DeliveryDone = Color(0xFF4CAF50)
    val DeliveryAssigned = Color(0xFF2196F3)
    val DeliveryWaiting = Color(0xFFFF9800)
    val DeliveryCancelled = Color(0xFFE53935)

    // Payment states
    val PaymentPosted = Color(0xFF4CAF50)
    val PaymentDraft = Color(0xFF9E9E9E)
    val PaymentCancelled = Color(0xFFE53935)
}
