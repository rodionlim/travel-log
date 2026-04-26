package com.wanderlog.android.domain.usecase.sync

import com.wanderlog.android.domain.model.sync.SyncEntityType
import com.wanderlog.android.domain.model.sync.TripSyncManifest
import com.wanderlog.android.domain.model.sync.TripSyncRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareTripSyncManifestsUseCaseTest {

    private val useCase = CompareTripSyncManifestsUseCase()

    @Test
    fun `pushes records missing from remote`() {
        val local = manifestOf(
            record(id = "trip-1", entityType = SyncEntityType.TRIP, updatedAt = 100L)
        )
        val remote = manifestOf()

        val plan = useCase(local, remote)

        assertEquals(listOf("trip-1"), plan.recordsToPush.map(TripSyncRecord::id))
        assertTrue(plan.recordsToPull.isEmpty())
    }

    @Test
    fun `pulls records missing from local`() {
        val local = manifestOf()
        val remote = manifestOf(
            record(id = "day-1", entityType = SyncEntityType.TRIP_DAY, updatedAt = 200L)
        )

        val plan = useCase(local, remote)

        assertEquals(listOf("day-1"), plan.recordsToPull.map(TripSyncRecord::id))
        assertTrue(plan.recordsToPush.isEmpty())
    }

    @Test
    fun `pulls newer remote records`() {
        val local = manifestOf(
            record(id = "item-1", entityType = SyncEntityType.ITINERARY_ITEM, updatedAt = 100L)
        )
        val remote = manifestOf(
            record(id = "item-1", entityType = SyncEntityType.ITINERARY_ITEM, updatedAt = 300L)
        )

        val plan = useCase(local, remote)

        assertEquals(listOf("item-1"), plan.recordsToPull.map(TripSyncRecord::id))
        assertTrue(plan.recordsToPush.isEmpty())
    }

    @Test
    fun `uses deterministic tie break when timestamps match`() {
        val local = manifestOf(
            record(
                id = "expense-1",
                entityType = SyncEntityType.EXPENSE,
                updatedAt = 500L,
                lastModifiedByDeviceId = "device-a",
                contentHash = "aaa"
            )
        )
        val remote = manifestOf(
            record(
                id = "expense-1",
                entityType = SyncEntityType.EXPENSE,
                updatedAt = 500L,
                lastModifiedByDeviceId = "device-b",
                contentHash = "bbb"
            )
        )

        val plan = useCase(local, remote)

        assertEquals(listOf("expense-1"), plan.recordsToPull.map(TripSyncRecord::id))
        assertTrue(plan.recordsToPush.isEmpty())
    }

    @Test
    fun `prefers newer tombstone over older live record`() {
        val local = manifestOf(
            record(id = "attachment-1", entityType = SyncEntityType.ATTACHMENT, updatedAt = 400L)
        )
        val remote = manifestOf(
            record(
                id = "attachment-1",
                entityType = SyncEntityType.ATTACHMENT,
                updatedAt = 350L,
                deletedAt = 450L
            )
        )

        val plan = useCase(local, remote)

        assertEquals(listOf("attachment-1"), plan.recordsToPull.map(TripSyncRecord::id))
        assertTrue(plan.recordsToPush.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects manifests for different trips`() {
        useCase(
            TripSyncManifest(tripId = "trip-a", generatedAt = 1L, records = emptyList()),
            TripSyncManifest(tripId = "trip-b", generatedAt = 1L, records = emptyList())
        )
    }

    private fun manifestOf(vararg records: TripSyncRecord): TripSyncManifest =
        TripSyncManifest(
            tripId = "trip-1",
            generatedAt = 999L,
            records = records.toList()
        )

    private fun record(
        id: String,
        entityType: SyncEntityType,
        updatedAt: Long,
        deletedAt: Long? = null,
        lastModifiedByDeviceId: String = "device-a",
        contentHash: String? = null,
        sizeBytes: Long? = null
    ): TripSyncRecord = TripSyncRecord(
        entityType = entityType,
        id = id,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        lastModifiedByDeviceId = lastModifiedByDeviceId,
        contentHash = contentHash,
        sizeBytes = sizeBytes
    )
}
