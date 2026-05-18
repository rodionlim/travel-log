package com.wanderlog.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderlog.android.domain.model.TripNote

@Entity(
    tableName = "trip_notes",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["trip_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trip_id"), Index("is_global")]
)
data class TripNoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "trip_id") val tripId: String,
    val content: String,
    @ColumnInfo(name = "is_global") val isGlobal: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "last_modified_by_device_id") val lastModifiedByDeviceId: String = ""
) {
    fun toDomain() = TripNote(
        id = id,
        tripId = tripId,
        content = content,
        isGlobal = isGlobal
    )

    companion object {
        fun fromDomain(
            note: TripNote,
            createdAt: Long = System.currentTimeMillis(),
            updatedAt: Long = createdAt,
            deletedAt: Long? = null,
            lastModifiedByDeviceId: String = ""
        ) = TripNoteEntity(
            id = note.id,
            tripId = note.tripId,
            content = note.content,
            isGlobal = note.isGlobal,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            lastModifiedByDeviceId = lastModifiedByDeviceId
        )
    }
}
