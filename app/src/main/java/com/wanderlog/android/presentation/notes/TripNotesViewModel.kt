package com.wanderlog.android.presentation.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.TripNote
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.note.AddTripNoteUseCase
import com.wanderlog.android.domain.usecase.note.DeleteTripNoteUseCase
import com.wanderlog.android.domain.usecase.note.GetGlobalNotesUseCase
import com.wanderlog.android.domain.usecase.note.GetTripNotesUseCase
import com.wanderlog.android.domain.usecase.note.UpdateTripNoteUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripNoteListItem(
    val note: TripNote,
    val tripName: String? = null
)

data class TripNotesUiState(
    val tripId: String? = null,
    val tripName: String = "",
    val title: String = "Global Notes",
    val notes: List<TripNoteListItem> = emptyList(),
    val isGlobalMode: Boolean = true,
    val canAdd: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TripNotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tripRepository: TripRepository,
    getTripNotes: GetTripNotesUseCase,
    getGlobalNotes: GetGlobalNotesUseCase,
    private val addTripNote: AddTripNoteUseCase,
    private val updateTripNote: UpdateTripNoteUseCase,
    private val deleteTripNote: DeleteTripNoteUseCase
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.Notes.ARG_TRIP_ID)

    private val _state = MutableStateFlow(
        TripNotesUiState(
            tripId = tripId,
            title = if (tripId == null) "Global Notes" else "Trip Notes",
            isGlobalMode = tripId == null,
            canAdd = tripId != null
        )
    )
    val state: StateFlow<TripNotesUiState> = _state.asStateFlow()

    init {
        if (tripId == null) {
            viewModelScope.launch {
                combine(getGlobalNotes(), tripRepository.getAllTrips()) { notes, trips ->
                    val tripsById = trips.associateBy { it.id }
                    notes.map { note ->
                        TripNoteListItem(
                            note = note,
                            tripName = tripsById[note.tripId]?.name
                        )
                    }
                }.collect { notes ->
                    _state.update {
                        it.copy(
                            notes = notes,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        } else {
            viewModelScope.launch {
                getTripNotes(tripId).collect { notes ->
                    _state.update {
                        it.copy(
                            notes = notes.map(::TripNoteListItem),
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
            viewModelScope.launch {
                val tripName = tripRepository.getTripById(tripId)?.name.orEmpty()
                _state.update { it.copy(tripName = tripName) }
            }
        }
    }

    fun addNote(content: String, isGlobal: Boolean) {
        val ownerTripId = state.value.tripId ?: return
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            _state.update { it.copy(error = "Note cannot be empty.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                addTripNote(
                    TripNote(
                        id = UUID.randomUUID().toString(),
                        tripId = ownerTripId,
                        content = trimmedContent,
                        isGlobal = isGlobal
                    )
                )
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to save note.") }
            }
        }
    }

    fun updateNote(note: TripNote, content: String, isGlobal: Boolean) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            _state.update { it.copy(error = "Note cannot be empty.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                updateTripNote(
                    note.copy(
                        content = trimmedContent,
                        isGlobal = isGlobal
                    )
                )
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to update note.") }
            }
        }
    }

    fun deleteNote(note: TripNote) {
        viewModelScope.launch {
            runCatching { deleteTripNote(note) }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "Failed to delete note.") }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
