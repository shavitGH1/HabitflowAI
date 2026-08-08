package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.model.HabitMarker
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MapUiState(
    val markers: List<HabitMarker> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            locationRepository.getLocationsFlow().collect { locations ->
                _uiState.value = _uiState.value.copy(markers = locations.map(::toHabitMarker))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            locationRepository.refreshFromServer()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun toHabitMarker(location: LocationEntity): HabitMarker {
        val detailParts = listOfNotNull(
            location.placeName,
            location.timestamp.takeIf { it > 0 }?.let { formatDate(it) }
        )
        return HabitMarker(
            id = location.id,
            habitName = location.taskTitle ?: "Completed task",
            personaEmoji = "✅",
            habitType = "Habit",
            latLng = LatLng(location.latitude, location.longitude),
            isPublic = false,
            detailText = detailParts.joinToString(" • ")
        )
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}
