package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.habitflowai.data.model.HabitMarker
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadMarkers()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onSearch() {
        // In a real app, use Geocoder or Places API here.
        // For now, if searching for "Tel Aviv", return its coordinates.
        if (_uiState.value.searchQuery.contains("Tel Aviv", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(searchResult = LatLng(32.0853, 34.7818))
        } else if (_uiState.value.searchQuery.contains("Haifa", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(searchResult = LatLng(32.7940, 34.9896))
        }
    }

    fun onCameraMoved() {
        _uiState.value = _uiState.value.copy(searchResult = null)
    }

    fun refreshMarkers() {
        loadMarkers()
    }

    private fun loadMarkers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val locations = locationRepository.getMyLocations()
            val markers = locations.map { location ->
                val taskName = location.taskDescription?.takeIf { it.isNotBlank() }
                val placeName = location.placeName?.takeIf { it.isNotBlank() }
                HabitMarker(
                    id = location.id,
                    habitName = taskName ?: placeName ?: "Completed task",
                    personaEmoji = "📍",
                    habitType = "Completed",
                    latLng = LatLng(location.latitude, location.longitude),
                    username = placeName
                )
            }
            _uiState.value = MapUiState(
                markers = markers,
                isLoading = false,
                searchQuery = _uiState.value.searchQuery,
                searchResult = _uiState.value.searchResult
            )
        }
    }
}
