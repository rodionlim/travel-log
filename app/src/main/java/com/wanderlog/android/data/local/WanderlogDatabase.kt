package com.wanderlog.android.data.local

import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wanderlog.android.data.local.converter.RoomConverters
import com.wanderlog.android.data.local.dao.AttachmentDao
import com.wanderlog.android.data.local.dao.ExpenseDao
import com.wanderlog.android.data.local.dao.ItineraryItemDao
import com.wanderlog.android.data.local.dao.PackingItemDao
import com.wanderlog.android.data.local.dao.TripDao
import com.wanderlog.android.data.local.dao.TripDayDao
import com.wanderlog.android.data.local.entity.AttachmentEntity
import com.wanderlog.android.data.local.entity.ExpenseEntity
import com.wanderlog.android.data.local.entity.ItineraryItemEntity
import com.wanderlog.android.data.local.entity.PackingItemEntity
import com.wanderlog.android.data.local.entity.TripDayEntity
import com.wanderlog.android.data.local.entity.TripEntity

@Database(
    entities = [
        TripEntity::class,
        TripDayEntity::class,
        ItineraryItemEntity::class,
        ExpenseEntity::class,
        PackingItemEntity::class,
        AttachmentEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class WanderlogDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun tripDayDao(): TripDayDao
    abstract fun itineraryItemDao(): ItineraryItemDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun packingItemDao(): PackingItemDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE itinerary_items ADD COLUMN linked_expense_id TEXT"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE trips ADD COLUMN traveller_names TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE packing_items ADD COLUMN traveller_name TEXT"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE packing_items ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val migratedAt = System.currentTimeMillis()

                database.execSQL(
                    "ALTER TABLE trips ADD COLUMN deleted_at INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE trips ADD COLUMN last_modified_by_device_id TEXT NOT NULL DEFAULT 'legacy'"
                )

                database.execSQL(
                    "ALTER TABLE trip_days ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE trip_days ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE trip_days ADD COLUMN deleted_at INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE trip_days ADD COLUMN last_modified_by_device_id TEXT NOT NULL DEFAULT 'legacy'"
                )
                database.execSQL(
                    "UPDATE trip_days SET created_at = $migratedAt, updated_at = $migratedAt"
                )

                database.execSQL(
                    "ALTER TABLE itinerary_items ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE itinerary_items ADD COLUMN deleted_at INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE itinerary_items ADD COLUMN last_modified_by_device_id TEXT NOT NULL DEFAULT 'legacy'"
                )
                database.execSQL(
                    "UPDATE itinerary_items SET updated_at = created_at"
                )

                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN deleted_at INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN last_modified_by_device_id TEXT NOT NULL DEFAULT 'legacy'"
                )
                database.execSQL(
                    "UPDATE expenses SET updated_at = created_at"
                )

                database.execSQL(
                    "ALTER TABLE packing_items ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE packing_items ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE packing_items ADD COLUMN deleted_at INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE packing_items ADD COLUMN last_modified_by_device_id TEXT NOT NULL DEFAULT 'legacy'"
                )
                database.execSQL(
                    "UPDATE packing_items SET created_at = $migratedAt, updated_at = $migratedAt"
                )

                database.execSQL(
                    "ALTER TABLE attachments ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE attachments ADD COLUMN deleted_at INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE attachments ADD COLUMN last_modified_by_device_id TEXT NOT NULL DEFAULT 'legacy'"
                )
                database.execSQL(
                    "UPDATE attachments SET updated_at = created_at"
                )
            }
        }
    }
}
