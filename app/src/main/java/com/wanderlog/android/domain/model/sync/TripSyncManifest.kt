package com.wanderlog.android.domain.model.sync

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TripSyncManifest(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val tripId: String,
    val generatedAt: Long,
    val records: List<TripSyncRecord>
) {
    companion object {
        const val CURRENT_PROTOCOL_VERSION = 6
    }
}

@JsonClass(generateAdapter = true)
data class TripSyncRecord(
    val entityType: SyncEntityType,
    val id: String,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val lastModifiedByDeviceId: String,
    val contentHash: String? = null,
    val sizeBytes: Long? = null
) {
    val key: SyncRecordKey
        get() = SyncRecordKey(entityType = entityType, id = id)

    val effectiveTimestamp: Long
        get() = maxOf(updatedAt, deletedAt ?: Long.MIN_VALUE)
}

@JsonClass(generateAdapter = true)
data class SyncRecordKey(
    val entityType: SyncEntityType,
    val id: String
)

enum class SyncEntityType {
    TRIP,
    TRIP_DAY,
    ITINERARY_ITEM,
    ITEM_ATTACHMENT_LINK,
    EXPENSE,
    PACKING_ITEM,
    TRIP_NOTE,
    ATTACHMENT
}

data class TripSyncMergePlan(
    val recordsToPush: List<TripSyncRecord>,
    val recordsToPull: List<TripSyncRecord>,
    val unchangedKeys: List<SyncRecordKey>
)
