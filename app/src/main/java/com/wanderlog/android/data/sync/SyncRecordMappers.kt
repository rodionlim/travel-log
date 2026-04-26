package com.wanderlog.android.data.sync

import com.wanderlog.android.data.local.entity.AttachmentEntity
import com.wanderlog.android.data.local.entity.ExpenseEntity
import com.wanderlog.android.data.local.entity.ItineraryItemEntity
import com.wanderlog.android.data.local.entity.PackingItemEntity
import com.wanderlog.android.data.local.entity.TripDayEntity
import com.wanderlog.android.data.local.entity.TripEntity
import com.wanderlog.android.domain.model.sync.SyncEntityType
import com.wanderlog.android.domain.model.sync.TripSyncRecord

fun TripEntity.toSyncRecord(): TripSyncRecord = TripSyncRecord(
    entityType = SyncEntityType.TRIP,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastModifiedByDeviceId = lastModifiedByDeviceId
)

fun TripDayEntity.toSyncRecord(): TripSyncRecord = TripSyncRecord(
    entityType = SyncEntityType.TRIP_DAY,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastModifiedByDeviceId = lastModifiedByDeviceId
)

fun ItineraryItemEntity.toSyncRecord(): TripSyncRecord = TripSyncRecord(
    entityType = SyncEntityType.ITINERARY_ITEM,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastModifiedByDeviceId = lastModifiedByDeviceId
)

fun ExpenseEntity.toSyncRecord(): TripSyncRecord = TripSyncRecord(
    entityType = SyncEntityType.EXPENSE,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastModifiedByDeviceId = lastModifiedByDeviceId
)

fun PackingItemEntity.toSyncRecord(): TripSyncRecord = TripSyncRecord(
    entityType = SyncEntityType.PACKING_ITEM,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastModifiedByDeviceId = lastModifiedByDeviceId
)

fun AttachmentEntity.toSyncRecord(contentHash: String?): TripSyncRecord = TripSyncRecord(
    entityType = SyncEntityType.ATTACHMENT,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastModifiedByDeviceId = lastModifiedByDeviceId,
    contentHash = contentHash,
    sizeBytes = sizeBytes
)
