package com.wanderlog.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderlog.android.domain.model.TripDay
import java.time.LocalDate

@Entity(
    tableName = "trip_days",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["trip_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trip_id"), Index(value = ["trip_id", "date"], unique = true)]
)
data class TripDayEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "trip_id") val tripId: String,
    val date: LocalDate,
    @ColumnInfo(name = "day_number") val dayNumber: Int,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "last_modified_by_device_id") val lastModifiedByDeviceId: String = ""
) {
    fun toDomain() = TripDay(
        id = id,
        tripId = tripId,
        date = date,
        dayNumber = dayNumber,
        notes = notes
    )

    companion object {
        fun fromDomain(
            day: TripDay,
            createdAt: Long = System.currentTimeMillis(),
            updatedAt: Long = createdAt,
            deletedAt: Long? = null,
            lastModifiedByDeviceId: String = ""
        ) = TripDayEntity(
            id = day.id,
            tripId = day.tripId,
            date = day.date,
            dayNumber = day.dayNumber,
            notes = day.notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            lastModifiedByDeviceId = lastModifiedByDeviceId
        )
    }
}
