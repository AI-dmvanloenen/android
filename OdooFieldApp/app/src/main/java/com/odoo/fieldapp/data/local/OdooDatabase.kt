package com.odoo.fieldapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.DeliveryDao
import com.odoo.fieldapp.data.local.dao.DeliveryLineDao
import com.odoo.fieldapp.data.local.dao.PaymentDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.data.local.entity.CustomerEntity
import com.odoo.fieldapp.data.local.entity.DeliveryEntity
import com.odoo.fieldapp.data.local.entity.DeliveryLineEntity
import com.odoo.fieldapp.data.local.entity.PaymentEntity
import com.odoo.fieldapp.data.local.entity.SaleEntity

/**
 * Room Database for the Odoo Field App
 *
 * Version 1: Initial schema with Customer entity
 * Version 2: Schema fix for Customer entity
 * Version 3: Changed Customer primary key to Odoo ID
 * Version 4: Added Sale entity
 * Version 5: Added mobileUid column to customers table
 * Version 6: Added Delivery and DeliveryLine entities
 * Version 7: Added Payment entity
 *
 * Future versions will add:
 * - SyncQueueEntity
 */
@Database(
    entities = [
        CustomerEntity::class,
        SaleEntity::class,
        DeliveryEntity::class,
        DeliveryLineEntity::class,
        PaymentEntity::class,
    ],
    version = 7,
    exportSchema = true
)
abstract class OdooDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun deliveryLineDao(): DeliveryLineDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        const val DATABASE_NAME = "odoo_field_db"

        /**
         * Migration from version 4 to 5
         * Adds mobileUid column to customers table for locally-created customers
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN mobileUid TEXT")
            }
        }

        /**
         * Migration from version 5 to 6
         * Adds Delivery and DeliveryLine tables
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create deliveries table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS deliveries (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        partnerId INTEGER,
                        partnerName TEXT,
                        scheduledDate INTEGER,
                        state TEXT NOT NULL,
                        saleId INTEGER,
                        saleName TEXT,
                        syncState TEXT NOT NULL,
                        lastModified INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create indices for deliveries table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deliveries_name ON deliveries (name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deliveries_partnerId ON deliveries (partnerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deliveries_saleId ON deliveries (saleId)")

                // Create delivery_lines table with foreign key
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS delivery_lines (
                        id INTEGER NOT NULL PRIMARY KEY,
                        deliveryId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        uom TEXT NOT NULL,
                        FOREIGN KEY (deliveryId) REFERENCES deliveries(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create index for delivery_lines table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_delivery_lines_deliveryId ON delivery_lines (deliveryId)")
            }
        }

        /**
         * Migration from version 6 to 7
         * Adds Payment table
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create payments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER NOT NULL PRIMARY KEY,
                        mobileUid TEXT,
                        name TEXT NOT NULL,
                        partnerId INTEGER,
                        partnerName TEXT,
                        amount REAL NOT NULL,
                        date INTEGER,
                        memo TEXT,
                        journalId INTEGER,
                        state TEXT NOT NULL,
                        syncState TEXT NOT NULL,
                        lastModified INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create indices for payments table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_name ON payments (name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_partnerId ON payments (partnerId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payments_mobileUid ON payments (mobileUid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_state ON payments (state)")
            }
        }

        /**
         * All database migrations
         * Add new migrations here as schema evolves
         */
        val ALL_MIGRATIONS = arrayOf<Migration>(
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
        )
    }
}
