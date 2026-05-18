package com.wanderlog.android.domain.usecase.note

import com.wanderlog.android.domain.model.TripNote
import com.wanderlog.android.domain.repository.TripNoteRepository
import javax.inject.Inject

class UpdateTripNoteUseCase @Inject constructor(private val repo: TripNoteRepository) {
    suspend operator fun invoke(note: TripNote) = repo.updateNote(note)
}
