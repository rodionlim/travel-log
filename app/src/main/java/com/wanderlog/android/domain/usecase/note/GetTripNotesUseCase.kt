package com.wanderlog.android.domain.usecase.note

import com.wanderlog.android.domain.model.TripNote
import com.wanderlog.android.domain.repository.TripNoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTripNotesUseCase @Inject constructor(private val repo: TripNoteRepository) {
    operator fun invoke(tripId: String): Flow<List<TripNote>> = repo.getNotesForTrip(tripId)
}
