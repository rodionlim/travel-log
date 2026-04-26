package com.wanderlog.android.domain.usecase.sync

import com.wanderlog.android.domain.model.sync.SyncEntityType
import com.wanderlog.android.domain.model.sync.TripSyncRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergePolicyTest {

    @Test
    fun `incoming newer record should apply`() {
        val local = record(updatedAt = 100L, deviceId = "device-a")
        val incoming = record(updatedAt = 200L, deviceId = "device-b")

        assertTrue(SyncMergePolicy.shouldApplyIncoming(local, incoming))
    }

    @Test
    fun `equivalent records should not apply`() {
        val local = record(updatedAt = 100L, deviceId = "device-a", hash = "hash")
        val incoming = record(updatedAt = 100L, deviceId = "device-a", hash = "hash")

        assertFalse(SyncMergePolicy.shouldApplyIncoming(local, incoming))
    }

    @Test
    fun `tombstone beats older live record`() {
        val local = record(updatedAt = 300L, deviceId = "device-a")
        val incoming = record(updatedAt = 250L, deletedAt = 400L, deviceId = "device-b")

        assertTrue(SyncMergePolicy.shouldApplyIncoming(local, incoming))
    }

    private fun record(
        updatedAt: Long,
        deletedAt: Long? = null,
        deviceId: String,
        hash: String? = null
    ): TripSyncRecord = TripSyncRecord(
        entityType = SyncEntityType.ITINERARY_ITEM,
        id = "item-1",
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        lastModifiedByDeviceId = deviceId,
        contentHash = hash
    )
}
