package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.ItineraryItemDao
import com.wanderlog.android.data.local.entity.ItineraryItemEntity
import com.wanderlog.android.data.sync.SyncMetadataStamp
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.repository.ItineraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ItineraryRepositoryImpl @Inject constructor(
    private val dao: ItineraryItemDao,
    private val syncMetadataStamp: SyncMetadataStamp
) : ItineraryRepository {

    override fun getItemsForDay(dayId: String): Flow<List<ItineraryItem>> =
        dao.getItemsForDay(dayId).map { it.map(ItineraryItemEntity::toDomain) }

    override fun getItemsForTrip(tripId: String): Flow<List<ItineraryItem>> =
        dao.getItemsForTrip(tripId).map { it.map(ItineraryItemEntity::toDomain) }

    override suspend fun insertItem(item: ItineraryItem) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.insertItem(
            ItineraryItemEntity.fromDomain(
                item = item,
                createdAt = now,
                updatedAt = now,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun insertItems(items: List<ItineraryItem>) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.insertItems(
            items.map {
                ItineraryItemEntity.fromDomain(
                    item = it,
                    createdAt = now,
                    updatedAt = now,
                    lastModifiedByDeviceId = deviceId
                )
            }
        )
    }

    override suspend fun updateItem(item: ItineraryItem) {
        val existing = dao.getByIdIncludingDeleted(item.id)
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.updateItem(
            ItineraryItemEntity.fromDomain(
                item = item,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = existing?.deletedAt,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun deleteItem(item: ItineraryItem) {
        dao.markDeleted(
            itemId = item.id,
            deletedAt = syncMetadataStamp.now(),
            lastModifiedByDeviceId = syncMetadataStamp.currentDeviceId()
        )
    }

    override suspend fun updateSortOrder(itemId: String, order: Int) {
        dao.updateSortOrder(
            itemId = itemId,
            order = order,
            updatedAt = syncMetadataStamp.now(),
            lastModifiedByDeviceId = syncMetadataStamp.currentDeviceId()
        )
    }
}
