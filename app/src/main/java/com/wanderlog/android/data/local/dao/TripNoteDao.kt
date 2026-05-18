package com.wanderlog.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wanderlog.android.data.local.entity.TripNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripNoteDao {

    @Query("SELECT * FROM trip_notes WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): TripNoteEntity?

    @Query("SELECT * FROM trip_notes WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: String): TripNoteEntity?

    @Query(
        """
        SELECT * FROM trip_notes
        WHERE trip_id = :tripId
            AND deleted_at IS NULL
            AND EXISTS (
                SELECT 1 FROM trips
                WHERE trips.id = trip_notes.trip_id
                    AND trips.deleted_at IS NULL
            )
        ORDER BY updated_at DESC
        """
    )
    fun getNotesForTrip(tripId: String): Flow<List<TripNoteEntity>>

    @Query(
        """
        SELECT * FROM trip_notes
        WHERE is_global = 1
            AND deleted_at IS NULL
            AND EXISTS (
                SELECT 1 FROM trips
                WHERE trips.id = trip_notes.trip_id
                    AND trips.deleted_at IS NULL
            )
        ORDER BY updated_at DESC
        """
    )
    fun getGlobalNotes(): Flow<List<TripNoteEntity>>

    @Query("SELECT * FROM trip_notes WHERE trip_id = :tripId ORDER BY updated_at DESC")
    suspend fun getNotesForTripForSync(tripId: String): List<TripNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TripNoteEntity)

    @Update
    suspend fun updateNote(note: TripNoteEntity)

    @Query(
        """
        UPDATE trip_notes
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt,
            last_modified_by_device_id = :lastModifiedByDeviceId
        WHERE id = :noteId AND deleted_at IS NULL
        """
    )
    suspend fun markDeleted(
        noteId: String,
        deletedAt: Long,
        lastModifiedByDeviceId: String
    )
}
