package com.wanderlog.android.presentation.ai.generate

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlog.android.domain.model.ItineraryItem
import com.wanderlog.android.domain.model.TripDay
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.TripRepository
import com.wanderlog.android.domain.usecase.ai.GenerateItineraryUseCase
import com.wanderlog.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class AiGenerateMode {
    FULL_TRIP,
    UPDATE_MULTIPLE_DAYS
}

data class AiGenerateState(
    val destination: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(6),
    val availableDays: List<TripDay> = emptyList(),
    val mode: AiGenerateMode = AiGenerateMode.FULL_TRIP,
    val preferences: String = "",
    val travellers: String = "1",
    val isLoading: Boolean = false,
    val generatedDays: List<TripDay> = emptyList(),
    val error: String? = null,
    val committed: Boolean = false
)

@HiltViewModel
class AiGenerateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val generateItinerary: GenerateItineraryUseCase,
    @ApplicationContext context: Context
) : ViewModel() {

    private val tripId = savedStateHandle.get<String>(Screen.AiGenerate.ARG_TRIP_ID)!!
    private val prefs = context.getSharedPreferences("wanderlog_ai_generate", Context.MODE_PRIVATE)
    private val travellersKey = "travellers_$tripId"

    private val _state = MutableStateFlow(AiGenerateState())
    val state: StateFlow<AiGenerateState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId) ?: return@launch
            val days = tripRepository.getDaysForTrip(tripId)
            _state.update {
                it.copy(
                    destination = trip.destination,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    availableDays = days,
                    travellers = prefs.getString(travellersKey, "1").orEmpty().ifBlank { "1" }
                )
            }
        }
    }

    fun onDestinationChange(v: String) = _state.update { it.copy(destination = v) }
    fun onModeChange(v: AiGenerateMode) = _state.update { it.copy(mode = v, generatedDays = emptyList(), error = null) }
    fun onPreferencesChange(v: String) = _state.update { it.copy(preferences = v) }
    fun onTravellersChange(v: String) {
        _state.update { it.copy(travellers = v) }
        prefs.edit().putString(travellersKey, v.ifBlank { "1" }).apply()
    }

    fun generate() {
        val s = _state.value
        if (s.destination.isBlank()) { _state.update { it.copy(error = "Destination required") }; return }
        if (s.mode != AiGenerateMode.FULL_TRIP && s.preferences.isBlank()) {
            _state.update { it.copy(error = "Tell AI what should change and it can choose the best existing days") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null, generatedDays = emptyList()) }
        viewModelScope.launch {
            runCatching {
                val existingDays = if (s.mode == AiGenerateMode.UPDATE_MULTIPLE_DAYS) {
                    s.availableDays.map { day ->
                        day.copy(items = itineraryRepository.getItemsForDay(day.id).first())
                    }
                } else {
                    emptyList()
                }
                generateItinerary(
                    destination = s.destination,
                    startDate = s.startDate.toString(),
                    endDate = s.endDate.toString(),
                    preferences = s.preferences.ifBlank { "balanced mix of culture and food" },
                    travellers = s.travellers.toIntOrNull() ?: 1,
                    updatePrompt = if (s.mode == AiGenerateMode.FULL_TRIP) null else s.preferences,
                    existingDays = existingDays
                )
            }.onSuccess { days ->
                _state.update { it.copy(isLoading = false, generatedDays = days) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message ?: "Generation failed") }
            }
        }
    }

    fun commitToItinerary() {
        val state = _state.value
        val days = state.generatedDays
        if (days.isEmpty()) return
        viewModelScope.launch {
            when (state.mode) {
                AiGenerateMode.UPDATE_MULTIPLE_DAYS -> {
                    val matchedItems = buildList {
                        days.forEach { generatedDay ->
                            val existingDay = state.availableDays.firstOrNull {
                                it.date == generatedDay.date || it.dayNumber == generatedDay.dayNumber
                            } ?: return@forEach

                            val existingItems = itineraryRepository.getItemsForDay(existingDay.id).first()
                            val startingSortOrder = existingItems.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                            addAll(
                                generatedDay.items.mapIndexed { index, item ->
                                    item.copy(
                                        tripId = tripId,
                                        tripDayId = existingDay.id,
                                        sortOrder = startingSortOrder + index
                                    )
                                }
                            )
                        }
                    }

                    if (matchedItems.isEmpty()) {
                        _state.update {
                            it.copy(error = "AI did not return any updates for existing trip days")
                        }
                        return@launch
                    }

                    itineraryRepository.insertItems(matchedItems)
                }

                AiGenerateMode.FULL_TRIP -> {
                    tripRepository.deleteDaysForTrip(tripId)
                    tripRepository.createDaysForTrip(days.map { it.copy(tripId = tripId) })
                    val allItems: List<ItineraryItem> = days.flatMap { day ->
                        day.items.map { item -> item.copy(tripId = tripId, tripDayId = day.id) }
                    }
                    itineraryRepository.insertItems(allItems)
                }
            }
            _state.update { it.copy(committed = true) }
        }
    }
}
