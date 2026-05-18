package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.TripNoteDao
import com.wanderlog.android.data.local.entity.TripNoteEntity
import com.wanderlog.android.data.sync.SyncMetadataStamp
import com.wanderlog.android.domain.model.TripNote
import com.wanderlog.android.domain.repository.TripNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TripNoteRepositoryImpl @Inject constructor(
    private val dao: TripNoteDao,
    private val syncMetadataStamp: SyncMetadataStamp
) : TripNoteRepository {

    override fun getNotesForTrip(tripId: String): Flow<List<TripNote>> =
        dao.getNotesForTrip(tripId).map { it.map(TripNoteEntity::toDomain) }

    override fun getGlobalNotes(): Flow<List<TripNote>> =
        dao.getGlobalNotes().map { it.map(TripNoteEntity::toDomain) }

    override suspend fun insertNote(note: TripNote) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.insertNote(
            TripNoteEntity.fromDomain(
                note = note,
                createdAt = now,
                updatedAt = now,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun updateNote(note: TripNote) {
        val existing = dao.getByIdIncludingDeleted(note.id)
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.updateNote(
            TripNoteEntity.fromDomain(
                note = note,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = existing?.deletedAt,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun deleteNote(note: TripNote) {
        dao.markDeleted(
            noteId = note.id,
            deletedAt = syncMetadataStamp.now(),
            lastModifiedByDeviceId = syncMetadataStamp.currentDeviceId()
        )
    }
}
