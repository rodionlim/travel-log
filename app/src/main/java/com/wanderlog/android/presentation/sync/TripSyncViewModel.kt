package com.wanderlog.android.presentation.sync

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.data.sync.NearbySyncMode
import com.wanderlog.android.data.sync.NearbySyncPeer
import com.wanderlog.android.data.sync.NearbyTripSyncTransport
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripSyncUiState(
    val tripId: String? = null,
    val tripName: String = "",
    val mode: NearbySyncMode = NearbySyncMode.IDLE,
    val localEndpointName: String = "",
    val discoveredPeers: List<NearbySyncPeer> = emptyList(),
    val connectedPeerName: String? = null,
    val statusMessage: String = "Idle",
    val lastSyncSummary: String? = null,
    val logLines: List<String> = emptyList(),
    val errorMessage: String? = null,
    val isBusy: Boolean = false
)

@HiltViewModel
class TripSyncViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val nearbyTripSyncTransport: NearbyTripSyncTransport
) : ViewModel() {

    private val initialTripId = savedStateHandle.get<String>(Screen.TripSync.ARG_TRIP_ID)

    private val _state = MutableStateFlow(TripSyncUiState(tripId = initialTripId))
    val state: StateFlow<TripSyncUiState> = _state.asStateFlow()

    init {
        refreshTripName(initialTripId)

        viewModelScope.launch {
            nearbyTripSyncTransport.state.collect { transportState ->
                val previousTripId = _state.value.tripId
                val nextTripId = transportState.tripId ?: previousTripId
                _state.update {
                    it.copy(
                        tripId = nextTripId,
                        mode = transportState.mode,
                        localEndpointName = transportState.localEndpointName,
                        discoveredPeers = transportState.discoveredPeers,
                        connectedPeerName = transportState.connectedPeerName,
                        statusMessage = transportState.statusMessage,
                        lastSyncSummary = transportState.lastSyncSummary,
                        logLines = transportState.logLines,
                        errorMessage = transportState.errorMessage,
                        isBusy = transportState.isBusy
                    )
                }
                if (nextTripId != previousTripId || (_state.value.tripName.isBlank() && nextTripId != null)) {
                    refreshTripName(nextTripId)
                }
            }
        }
    }

    fun startHosting() {
        val tripId = state.value.tripId ?: return
        viewModelScope.launch { nearbyTripSyncTransport.startAdvertising(tripId) }
    }

    fun startDiscovery() {
        viewModelScope.launch { nearbyTripSyncTransport.startDiscovery(state.value.tripId) }
    }

    fun connect(peer: NearbySyncPeer) {
        viewModelScope.launch { nearbyTripSyncTransport.requestConnection(peer) }
    }

    fun stopSession() {
        nearbyTripSyncTransport.stop()
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyTripSyncTransport.stop()
    }

    private fun refreshTripName(tripId: String?) {
        viewModelScope.launch {
            val tripName = tripId?.let { tripRepository.getTripById(it)?.name }.orEmpty()
            _state.update {
                if (it.tripId == tripId) it.copy(tripName = tripName) else it
            }
        }
    }
}
