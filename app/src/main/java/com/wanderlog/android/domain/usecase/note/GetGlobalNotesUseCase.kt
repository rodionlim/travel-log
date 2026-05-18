package com.wanderlog.android.domain.usecase.note

import com.wanderlog.android.domain.model.TripNote
import com.wanderlog.android.domain.repository.TripNoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGlobalNotesUseCase @Inject constructor(private val repo: TripNoteRepository) {
    operator fun invoke(): Flow<List<TripNote>> = repo.getGlobalNotes()
}
