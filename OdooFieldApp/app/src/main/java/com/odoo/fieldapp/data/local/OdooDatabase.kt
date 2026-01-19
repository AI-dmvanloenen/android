package com.odoo.fieldapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.entity.CustomerEntity

/**
 * Room Database for the Odoo Field App
 * 
 * Version 1: Initial schema with Customer entity
 * 
 * Future versions will add:
 * - SaleEntity
 * - PaymentEntity  
 * - DeliveryEntity
 * - SyncQueueEntity
 */
@Database(
    entities = [
        CustomerEntity::class,
        // Future: SaleEntity::class,
        // Future: PaymentEntity::class,
        // Future: DeliveryEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class OdooDatabase : RoomDatabase() {
    
    abstract fun customerDao(): CustomerDao
    // Future: abstract fun saleDao(): SaleDao
    // Future: abstract fun paymentDao(): PaymentDao
    // Future: abstract fun deliveryDao(): DeliveryDao
    
    companion object {
        const val DATABASE_NAME = "odoo_field_db"
    }
}
