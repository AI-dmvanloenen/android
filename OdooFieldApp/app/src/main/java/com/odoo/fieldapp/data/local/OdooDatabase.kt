package com.odoo.fieldapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.local.dao.DeliveryDao
import com.odoo.fieldapp.data.local.dao.DeliveryLineDao
import com.odoo.fieldapp.data.local.dao.PaymentDao
import com.odoo.fieldapp.data.local.dao.ProductDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.data.local.dao.SaleLineDao
import com.odoo.fieldapp.data.local.dao.SyncQueueDao
import com.odoo.fieldapp.data.local.entity.CustomerEntity
import com.odoo.fieldapp.data.local.entity.DeliveryEntity
import com.odoo.fieldapp.data.local.entity.DeliveryLineEntity
import com.odoo.fieldapp.data.local.entity.PaymentEntity
import com.odoo.fieldapp.data.local.entity.ProductEntity
import com.odoo.fieldapp.data.local.entity.SaleEntity
import com.odoo.fieldapp.data.local.entity.SaleLineEntity
import com.odoo.fieldapp.data.local.entity.SyncQueueEntity

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
 * Version 8: Added mobileUid, state to sales; productId, quantityDone to delivery_lines
 * Version 9: Added SaleLine entity
 * Version 10: Added Product entity
 * Version 11: Added SyncQueue entity for offline operations queue
 */
@Database(
    entities = [
        CustomerEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        DeliveryEntity::class,
        DeliveryLineEntity::class,
        PaymentEntity::class,
        ProductEntity::class,
        SyncQueueEntity::class,
    ],
    version = 11,
    exportSchema = true
)
abstract class OdooDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun saleLineDao(): SaleLineDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun deliveryLineDao(): DeliveryLineDao
    abstract fun paymentDao(): PaymentDao
    abstract fun productDao(): ProductDao
    abstract fun syncQueueDao(): SyncQueueDao

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
         * Migration from version 7 to 8
         * Adds mobileUid and state to sales table
         * Adds productId and quantityDone to delivery_lines table
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add mobileUid and state columns to sales table
                db.execSQL("ALTER TABLE sales ADD COLUMN mobileUid TEXT")
                db.execSQL("ALTER TABLE sales ADD COLUMN state TEXT NOT NULL DEFAULT 'draft'")

                // Create unique index on mobileUid
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sales_mobileUid ON sales (mobileUid)")

                // Add productId and quantityDone columns to delivery_lines table
                db.execSQL("ALTER TABLE delivery_lines ADD COLUMN productId INTEGER")
                db.execSQL("ALTER TABLE delivery_lines ADD COLUMN quantityDone REAL NOT NULL DEFAULT 0.0")
            }
        }

        /**
         * Migration from version 8 to 9
         * Adds SaleLine table for sale order lines
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create sale_lines table with foreign key
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sale_lines (
                        id INTEGER NOT NULL PRIMARY KEY,
                        saleId INTEGER NOT NULL,
                        productId INTEGER,
                        productName TEXT NOT NULL,
                        productUomQty REAL NOT NULL,
                        qtyDelivered REAL NOT NULL,
                        qtyInvoiced REAL NOT NULL,
                        priceUnit REAL NOT NULL,
                        discount REAL NOT NULL,
                        priceSubtotal REAL NOT NULL,
                        uom TEXT NOT NULL,
                        FOREIGN KEY (saleId) REFERENCES sales(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create indices for sale_lines table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_lines_saleId ON sale_lines (saleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_lines_productId ON sale_lines (productId)")
            }
        }

        /**
         * Migration from version 9 to 10
         * Adds Product table
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create products table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS products (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        defaultCode TEXT,
                        barcode TEXT,
                        listPrice REAL NOT NULL,
                        uomId INTEGER,
                        uomName TEXT,
                        categId INTEGER,
                        categName TEXT,
                        type TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        lastModified INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create indices for products table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_name ON products (name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_defaultCode ON products (defaultCode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products (barcode)")
            }
        }

        /**
         * Migration from version 10 to 11
         * Adds SyncQueue table for offline operations queue
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create sync_queue table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        mobileUid TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'pending',
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        maxRetries INTEGER NOT NULL DEFAULT 5,
                        lastError TEXT,
                        createdAt INTEGER NOT NULL,
                        lastAttemptAt INTEGER,
                        nextAttemptAt INTEGER
                    )
                """.trimIndent())

                // Create indices for sync_queue table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType ON sync_queue (entityType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdAt ON sync_queue (createdAt)")
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
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
        )
    }
}
