package com.habitflowai.presentation.viewmodel

import com.google.android.gms.maps.model.LatLng
import com.habitflowai.data.model.LocationResponse
import com.habitflowai.domain.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val locationRepository: LocationRepository = mockk()
    private lateinit var viewModel: MapViewModel

    private val myLocations = listOf(
        LocationResponse(
            id = "loc-1",
            habitId = "h1",
            latitude = 32.0853,
            longitude = 34.7818,
            placeName = "Tel Aviv",
            taskDescription = "Morning Run",
            timestamp = 1000,
            isPublic = true
        ),
        LocationResponse(
            id = "loc-2",
            habitId = "h2",
            latitude = 31.5,
            longitude = 34.5,
            placeName = "Home",
            timestamp = 2000,
            isPublic = false
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { locationRepository.getMyLocations() } returns myLocations
        viewModel = MapViewModel(locationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `markers are loaded only from my locations`() {
        val markers = viewModel.uiState.value.markers
        assertEquals(2, markers.size)
        assertEquals("Morning Run", markers[0].habitName)
        assertEquals(LatLng(32.0853, 34.7818), markers[0].latLng)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `markers preserve isPublic from the backend`() {
        val markers = viewModel.uiState.value.markers
        assertEquals(true, markers[0].isPublic)
        assertEquals(false, markers[1].isPublic)
    }

    @Test
    fun `markers fall back to place name when no task description`() {
        val markers = viewModel.uiState.value.markers
        assertEquals("Home", markers[1].habitName)
    }
}
