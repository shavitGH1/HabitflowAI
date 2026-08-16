package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.habitflowai.data.model.AudienceFilter
import com.habitflowai.data.model.HabitMarker
import com.habitflowai.data.model.MarkerRelationship
import com.habitflowai.data.model.TimeRangeFilter
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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

    fun onTimeRangeChange(filter: TimeRangeFilter) {
        _uiState.value = _uiState.value.copy(timeRangeFilter = filter)
        loadMarkers()
    }

    fun onAudienceChange(filter: AudienceFilter) {
        _uiState.value = _uiState.value.copy(audienceFilter = filter)
        loadMarkers()
    }

    fun refreshMarkers() {
        loadMarkers()
    }

    /** One-shot fetch for centering the camera — null if permission is missing or it can't be resolved. */
    suspend fun getMyLocation(): LatLng? {
        val location = locationRepository.getCurrentDeviceLocation() ?: return null
        return LatLng(location.latitude, location.longitude)
    }

    private fun loadMarkers() {
        val timeRangeFilter = _uiState.value.timeRangeFilter
        val audienceFilter = _uiState.value.audienceFilter

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val since = timeRangeFilter.days?.let { days ->
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
            }
            val locations = locationRepository.getPublicLocations(
                minLat = -90.0,
                maxLat = 90.0,
                minLng = -180.0,
                maxLng = 180.0,
                since = since,
                scope = audienceFilter.apiValue
            )
            val markers = locations.map { location ->
                val taskName = location.taskDescription?.takeIf { it.isNotBlank() }
                val placeName = location.placeName?.takeIf { it.isNotBlank() }
                val displayName = location.username?.takeIf { it.isNotBlank() } ?: "User"
                HabitMarker(
                    id = location.id,
                    habitName = taskName ?: placeName ?: "Completed task",
                    personaEmoji = "📍",
                    habitType = location.type,
                    latLng = LatLng(location.latitude, location.longitude),
                    username = displayName,
                    userId = location.userId ?: "",
                    relationship = MarkerRelationship.from(location.relationship),
                    isPublic = location.isPublic
                )
            }
            _uiState.value = _uiState.value.copy(
                markers = markers,
                isLoading = false
            )
        }
    }
}
