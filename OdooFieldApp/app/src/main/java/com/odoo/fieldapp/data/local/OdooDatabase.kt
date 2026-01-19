package com.odoo.fieldapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.data.local.entity.CustomerEntity
import com.odoo.fieldapp.data.local.entity.SaleEntity

/**
 * Room Database for the Odoo Field App
 *
 * Version 1: Initial schema with Customer entity
 * Version 2: Schema fix for Customer entity
 * Version 3: Changed Customer primary key to Odoo ID
 * Version 4: Added Sale entity
 *
 * Future versions will add:
 * - PaymentEntity
 * - DeliveryEntity
 * - SyncQueueEntity
 */
@Database(
    entities = [
        CustomerEntity::class,
        SaleEntity::class,
        // Future: PaymentEntity::class,
        // Future: DeliveryEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class OdooDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    // Future: abstract fun paymentDao(): PaymentDao
    // Future: abstract fun deliveryDao(): DeliveryDao

    companion object {
        const val DATABASE_NAME = "odoo_field_db"
    }
}
