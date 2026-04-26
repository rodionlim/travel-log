package com.wanderlog.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wanderlog.android.data.local.entity.PackingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackingItemDao {

        @Query("SELECT * FROM packing_items WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): PackingItemEntity?

        @Query("SELECT * FROM packing_items WHERE id = :id")
        suspend fun getByIdIncludingDeleted(id: String): PackingItemEntity?

        @Query(
                """
                SELECT * FROM packing_items
                WHERE trip_id = :tripId
                    AND deleted_at IS NULL
                    AND EXISTS (
                            SELECT 1 FROM trips
                            WHERE trips.id = packing_items.trip_id
                                AND trips.deleted_at IS NULL
                    )
                ORDER BY sort_order ASC
                """
        )
    fun getItemsForTrip(tripId: String): Flow<List<PackingItemEntity>>

        @Query(
                """
                SELECT * FROM packing_items
                WHERE trip_id = :tripId
                    AND deleted_at IS NULL
                    AND EXISTS (
                            SELECT 1 FROM trips
                            WHERE trips.id = packing_items.trip_id
                                AND trips.deleted_at IS NULL
                    )
                ORDER BY sort_order ASC
                """
        )
    suspend fun getItemsForTripOnce(tripId: String): List<PackingItemEntity>

        @Query("SELECT * FROM packing_items WHERE trip_id = :tripId ORDER BY sort_order ASC")
        suspend fun getItemsForTripForSync(tripId: String): List<PackingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PackingItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PackingItemEntity>)

    @Update
    suspend fun updateItem(item: PackingItemEntity)

    @Query("DELETE FROM packing_items WHERE deleted_at IS NOT NULL")
    suspend fun purgeDeletedItems()

    @Query("DELETE FROM packing_items WHERE trip_id = :tripId")
    suspend fun deleteItemsForTrip(tripId: String)

    @Query(
        """
        UPDATE packing_items
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt,
            last_modified_by_device_id = :lastModifiedByDeviceId
        WHERE id = :itemId AND deleted_at IS NULL
        """
    )
    suspend fun markDeleted(
        itemId: String,
        deletedAt: Long,
        lastModifiedByDeviceId: String
    )

    @androidx.room.Transaction
    suspend fun replaceItemsForTrip(tripId: String, items: List<PackingItemEntity>) {
        deleteItemsForTrip(tripId)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }
}
