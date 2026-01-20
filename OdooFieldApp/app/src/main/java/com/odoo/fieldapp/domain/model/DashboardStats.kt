package com.odoo.fieldapp.domain.model

/**
 * Domain model for Dashboard statistics
 * Contains counts for various pending work items
 */
data class DashboardStats(
    val deliveriesToComplete: Int = 0,
    val todaysDeliveries: Int = 0,
    val pendingPayments: Int = 0,
    val syncErrors: Int = 0
)
