package com.wanderlog.android.domain.usecase.sync

import com.wanderlog.android.domain.model.sync.TripSyncRecord

object SyncMergePolicy {

    fun recordsAreEquivalent(left: TripSyncRecord, right: TripSyncRecord): Boolean =
        left.updatedAt == right.updatedAt &&
            left.deletedAt == right.deletedAt &&
            left.lastModifiedByDeviceId == right.lastModifiedByDeviceId &&
            left.contentHash == right.contentHash &&
            left.sizeBytes == right.sizeBytes

    fun shouldApplyIncoming(local: TripSyncRecord?, incoming: TripSyncRecord): Boolean {
        if (local == null) return true
        if (recordsAreEquivalent(local, incoming)) return false
        return comparePriority(incoming, local) > 0
    }

    fun comparePriority(left: TripSyncRecord, right: TripSyncRecord): Int {
        val timestampComparison = left.effectiveTimestamp.compareTo(right.effectiveTimestamp)
        if (timestampComparison != 0) return timestampComparison

        val deviceComparison = left.lastModifiedByDeviceId.compareTo(right.lastModifiedByDeviceId)
        if (deviceComparison != 0) return deviceComparison

        val hashComparison = left.contentHash.orEmpty().compareTo(right.contentHash.orEmpty())
        if (hashComparison != 0) return hashComparison

        return (left.sizeBytes ?: Long.MIN_VALUE).compareTo(right.sizeBytes ?: Long.MIN_VALUE)
    }
}
