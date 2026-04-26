package com.wanderlog.android.domain.model.sync

import com.wanderlog.android.domain.model.TravellerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripSyncBundleTest {

    @Test
    fun `bundle converts payloads into manifest records`() {
        val bundle = TripSyncBundle(
            generatedAt = 999L,
            trip = SyncTripPayload(
                id = "trip-1",
                name = "Osaka",
                destination = "Osaka",
                startDate = "2026-06-01",
                endDate = "2026-06-05",
                currencyCode = "JPY",
                travellerProfiles = listOf(TravellerProfile(name = "A")),
                metadata = SyncMetadata(updatedAt = 100L, lastModifiedByDeviceId = "device-a")
            ),
            attachments = listOf(
                SyncAttachmentPayload(
                    id = "attachment-1",
                    tripId = "trip-1",
                    displayName = "ticket.pdf",
                    mimeType = "application/pdf",
                    localPath = "attachments/trip-1/ticket.pdf",
                    sizeBytes = 123L,
                    createdAt = 90L,
                    contentHash = "hash-1",
                    contentBase64 = "cGRm",
                    metadata = SyncMetadata(updatedAt = 110L, deletedAt = 120L, lastModifiedByDeviceId = "device-b")
                )
            )
        )

        val manifest = bundle.toManifest()

        assertEquals("trip-1", manifest.tripId)
        assertEquals(2, manifest.records.size)
        assertEquals(SyncEntityType.TRIP, manifest.records.first().entityType)
        assertEquals(SyncEntityType.ATTACHMENT, manifest.records.last().entityType)
        assertEquals("hash-1", manifest.records.last().contentHash)
        assertEquals(120L, manifest.records.last().deletedAt)
    }

    @Test
    fun `tombstoned attachment payload can omit file content`() {
        val payload = SyncAttachmentPayload(
            id = "attachment-2",
            tripId = "trip-1",
            displayName = "deleted.pdf",
            mimeType = "application/pdf",
            localPath = "attachments/trip-1/deleted.pdf",
            sizeBytes = 10L,
            createdAt = 1L,
            contentHash = null,
            contentBase64 = null,
            metadata = SyncMetadata(updatedAt = 2L, deletedAt = 3L, lastModifiedByDeviceId = "device-z")
        )

        assertNull(payload.contentBase64)
        assertEquals(3L, payload.metadata.deletedAt)
    }
}
