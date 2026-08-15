package com.habitflowai.presentation.viewmodel

import com.google.android.gms.maps.model.LatLng
import com.habitflowai.data.model.LocationResponse
import com.habitflowai.domain.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
            userId = "u1",
            habitId = "h1",
            latitude = 32.0853,
            longitude = 34.7818,
            placeName = "Tel Aviv",
            taskDescription = "Morning Run",
            timestamp = 1000,
            isPublic = true,
            username = "shavi",
            type = "habit"
        ),
        LocationResponse(
            id = "loc-2",
            userId = "u2",
            habitId = "h2",
            latitude = 31.5,
            longitude = 34.5,
            placeName = "Home",
            timestamp = 2000,
            isPublic = false,
            username = "yossi",
            type = "task"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { locationRepository.getPublicLocations(any(), any(), any(), any()) } returns myLocations
        viewModel = MapViewModel(locationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `markers are loaded from all public locations`() {
        val markers = viewModel.uiState.value.markers
        assertEquals(2, markers.size)
        assertEquals("Morning Run", markers[0].habitName)
        assertEquals(LatLng(32.0853, 34.7818), markers[0].latLng)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify { locationRepository.getPublicLocations(any(), any(), any(), any()) }
    }

    @Test
    fun `markers preserve isPublic from the backend`() {
        val markers = viewModel.uiState.value.markers
        assertEquals(true, markers[0].isPublic)
        assertEquals(false, markers[1].isPublic)
    }

    @Test
    fun `marker tag shows the completing user`() {
        val markers = viewModel.uiState.value.markers
        assertEquals("shavi", markers[0].username)
        assertEquals("yossi", markers[1].username)
    }

    @Test
    fun `marker type maps habit vs task`() {
        val markers = viewModel.uiState.value.markers
        assertEquals("habit", markers[0].habitType)
        assertEquals("task", markers[1].habitType)
    }

    @Test
    fun `markers fall back to place name when no task description`() {
        val markers = viewModel.uiState.value.markers
        assertEquals("Home", markers[1].habitName)
    }
}
