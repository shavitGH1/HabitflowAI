package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.habitflowai.data.model.HabitMarker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class MapUiState(
    val markers: List<HabitMarker> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResult: LatLng? = null
)

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
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

    private fun loadMarkers() {
        // Mock data for clustering
        val mockMarkers = listOf(
            HabitMarker("1", "Morning Run", "🏃", "Physical", LatLng(32.0853, 34.7818)),
            HabitMarker("2", "Meditation", "🧘", "Mental", LatLng(32.0860, 34.7825)),
            HabitMarker("3", "Deep Work", "💻", "Productivity", LatLng(32.0845, 34.7810)),
            HabitMarker("4", "Reading", "📚", "Growth", LatLng(32.0870, 34.7830), isPublic = true, username = "Alex"),
            HabitMarker("5", "Gym", "🏋️", "Physical", LatLng(32.1000, 34.8000)),
            HabitMarker("6", "Yoga", "🧘", "Mental", LatLng(32.1010, 34.8010))
        )
        _uiState.value = MapUiState(markers = mockMarkers)
    }
}
