package com.wanderlog.android.domain.repository

import com.wanderlog.android.domain.model.TripNote
import kotlinx.coroutines.flow.Flow

interface TripNoteRepository {
    fun getNotesForTrip(tripId: String): Flow<List<TripNote>>
    fun getGlobalNotes(): Flow<List<TripNote>>
    suspend fun insertNote(note: TripNote)
    suspend fun updateNote(note: TripNote)
    suspend fun deleteNote(note: TripNote)
}
