package com.habitflowai.presentation.viewmodel

import com.google.android.gms.maps.model.LatLng
import com.habitflowai.data.model.HabitMarker

data class MapUiState(
    val markers: List<HabitMarker> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResult: LatLng? = null
)
