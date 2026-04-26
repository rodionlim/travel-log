package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.TripDao
import com.wanderlog.android.data.local.dao.TripDayDao
import com.wanderlog.android.data.local.entity.TripDayEntity
import com.wanderlog.android.data.local.entity.TripEntity
import com.wanderlog.android.data.sync.SyncMetadataStamp
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val tripDayDao: TripDayDao,
    private val syncMetadataStamp: SyncMetadataStamp
) : TripRepository {

    override fun getAllTrips(): Flow<List<Trip>> =
        tripDao.getAllTrips().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTripById(id: String): Trip? =
        tripDao.getTripById(id)?.toDomain()

    override suspend fun createTrip(trip: Trip) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        tripDao.insertTrip(
            TripEntity.fromDomain(
                trip = trip,
                createdAt = now,
                updatedAt = now,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun updateTrip(trip: Trip) {
        val existing = tripDao.getTripByIdIncludingDeleted(trip.id)
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        tripDao.updateTrip(
            TripEntity.fromDomain(
                trip = trip,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = existing?.deletedAt,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTripById(trip.id)
    }

    override suspend fun getDaysForTrip(tripId: String): List<TripDay> =
        tripDayDao.getDaysForTripOnce(tripId).map { it.toDomain() }

    override fun getDaysForTripFlow(tripId: String): Flow<List<TripDay>> =
        tripDayDao.getDaysForTrip(tripId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createDaysForTrip(days: List<TripDay>) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        tripDayDao.insertAll(
            days.map {
                TripDayEntity.fromDomain(
                    day = it,
                    createdAt = now,
                    updatedAt = now,
                    lastModifiedByDeviceId = deviceId
                )
            }
        )
    }

    override suspend fun deleteDaysForTrip(tripId: String) =
        tripDayDao.deleteAllForTrip(tripId)
}
