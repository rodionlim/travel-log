package com.wanderlog.android.presentation.tripPhotos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.core.util.toCompactSlashDisplay
import com.wanderlog.android.domain.model.Trip
import com.wanderlog.android.domain.model.TripPhoto
import com.wanderlog.android.domain.repository.LocalPhotoDeleteResult
import com.wanderlog.android.domain.repository.LocalPhotoRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TripPhotosUiState(
    val tripName: String = "",
    val tripDateLabel: String = "",
    val tripStartDate: LocalDate? = null,
    val tripEndDate: LocalDate? = null,
    val photos: List<TripPhoto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isTripReady: Boolean = false
)

@HiltViewModel
class TripPhotosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val localPhotoRepository: LocalPhotoRepository
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.TripPhotos.ARG_TRIP_ID)!!

    private val _state = MutableStateFlow(TripPhotosUiState())
    val state: StateFlow<TripPhotosUiState> = _state.asStateFlow()

    private var trip: Trip? = null
    private var hasLoadedPhotos = false

    init {
        viewModelScope.launch {
            val loadedTrip = tripRepository.getTripById(tripId)
            trip = loadedTrip
            _state.update { current ->
                if (loadedTrip == null) {
                    current.copy(
                        isLoading = false,
                        error = "Trip not found",
                        isTripReady = false
                    )
                } else {
                    current.copy(
                        tripName = loadedTrip.name
                            .takeIf { it.isNotBlank() }
                            ?: loadedTrip.destination.ifBlank { "Trip" },
                        tripDateLabel = "${loadedTrip.startDate.toCompactSlashDisplay()} - ${loadedTrip.endDate.toCompactSlashDisplay()}",
                        tripStartDate = loadedTrip.startDate,
                        tripEndDate = loadedTrip.endDate,
                        isLoading = false,
                        error = null,
                        isTripReady = true
                    )
                }
            }
        }
    }

    fun loadPhotos(force: Boolean = false) {
        val loadedTrip = trip ?: return
        if (!force && (hasLoadedPhotos || _state.value.isLoading)) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                localPhotoRepository.getPhotosForDateRange(
                    startDate = loadedTrip.startDate,
                    endDate = loadedTrip.endDate
                )
            }.onSuccess { photos ->
                hasLoadedPhotos = true
                _state.update {
                    it.copy(
                        photos = photos,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load local photos"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadPhotos(force = true)
    }

    suspend fun deletePhotos(photos: List<TripPhoto>): LocalPhotoDeleteResult =
        localPhotoRepository.deletePhotos(photos)
}
