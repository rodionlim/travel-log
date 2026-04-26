package com.wanderlog.android.data.sync

import com.wanderlog.android.data.local.entity.AttachmentEntity
import com.wanderlog.android.data.local.entity.TripDayEntity
import com.wanderlog.android.domain.model.sync.SyncEntityType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SyncRecordMappersTest {

    @Test
    fun `attachment mapper keeps hash and size metadata`() {
        val record = AttachmentEntity(
            id = "attachment-1",
            tripId = "trip-1",
            displayName = "ticket.pdf",
            mimeType = "application/pdf",
            localPath = "attachments/trip-1/ticket.pdf",
            sizeBytes = 4096,
            createdAt = 100L,
            updatedAt = 200L,
            deletedAt = 300L,
            lastModifiedByDeviceId = "device-a"
        ).toSyncRecord(contentHash = "abc123")

        assertEquals(SyncEntityType.ATTACHMENT, record.entityType)
        assertEquals("abc123", record.contentHash)
        assertEquals(4096L, record.sizeBytes)
        assertEquals(300L, record.deletedAt)
    }

    @Test
    fun `trip day mapper keeps sync timestamps`() {
        val record = TripDayEntity(
            id = "day-1",
            tripId = "trip-1",
            date = LocalDate.of(2026, 5, 1),
            dayNumber = 1,
            notes = "Arrival",
            createdAt = 50L,
            updatedAt = 120L,
            deletedAt = null,
            lastModifiedByDeviceId = "device-b"
        ).toSyncRecord()

        assertEquals(SyncEntityType.TRIP_DAY, record.entityType)
        assertEquals(120L, record.updatedAt)
        assertEquals("device-b", record.lastModifiedByDeviceId)
    }
}
