package com.habitflowai.presentation.viewmodel

import com.google.android.gms.maps.model.LatLng
import com.habitflowai.data.model.AudienceFilter
import com.habitflowai.data.model.HabitMarker
import com.habitflowai.data.model.TimeRangeFilter

data class MapUiState(
    val markers: List<HabitMarker> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResult: LatLng? = null,
    val timeRangeFilter: TimeRangeFilter = TimeRangeFilter.ALL_TIME,
    val audienceFilter: AudienceFilter = AudienceFilter.EVERYONE
)
