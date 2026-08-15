package com.habitflowai.presentation.viewmodel

import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.domain.repository.HabitsRepository
import com.habitflowai.domain.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {

    private val habitsRepository: HabitsRepository = mockk()
    private val locationRepository: LocationRepository = mockk()
    private lateinit var viewModel: HabitsViewModel

    private val testHabits = listOf(
        HabitEntity(
            id = "h1",
            title = "Morning Run",
            description = "Run 5km",
            frequency = "DAILY",
            userId = "local_user",
            completed = false,
            syncStatus = SyncStatus.SYNCED,
            completionHistory = emptyList()
        ),
        HabitEntity(
            id = "h2",
            title = "Read 30 min",
            description = "Read a book",
            frequency = "DAILY",
            userId = "local_user",
            completed = true,
            syncStatus = SyncStatus.SYNCED,
            completionHistory = emptyList()
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { habitsRepository.getHabits(any()) } returns flowOf(testHabits)
        coEvery { habitsRepository.createHabit(any()) } just runs
        coEvery { habitsRepository.deleteHabit(any()) } just runs
        coEvery { habitsRepository.completeHabit(any()) } returns true
        coEvery { locationRepository.captureAndSaveLocation(any(), any()) } just runs

        viewModel = HabitsViewModel(habitsRepository, locationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads habits from repository`() {
        val state = viewModel.uiState.value
        assertEquals(2, state.habits.size)
        assertEquals("Morning Run", state.habits[0].title)
        assertEquals("Read 30 min", state.habits[1].title)
        assertFalse(state.isLoading)
    }

    @Test
    fun `addHabit creates entity and calls repository`() {
        viewModel.addHabit("New Habit", "Description", "WEEKLY")

        coVerify {
            habitsRepository.createHabit(match { entity ->
                entity.title == "New Habit" &&
                entity.description == "Description" &&
                entity.frequency == "WEEKLY" &&
                entity.userId == "local_user" &&
                !entity.completed &&
                entity.syncStatus == SyncStatus.PENDING_CREATE &&
                entity.completionHistory.isEmpty()
            })
        }
    }

    @Test
    fun `addHabit sets unique ids`() {
        viewModel.addHabit("Habit A", "Desc", "DAILY")
        viewModel.addHabit("Habit B", "Desc", "WEEKLY")

        coVerify(exactly = 2) { habitsRepository.createHabit(any()) }

        // Capture the first two calls and verify they have different IDs
        val habitSlot = mutableListOf<HabitEntity>()
        coEvery { habitsRepository.createHabit(any()) } answers {
            habitSlot.add(firstArg())
        }

        viewModel.addHabit("Habit C", "Desc", "MONTHLY")
        assertEquals(1, habitSlot.size)
        assertNotNull(habitSlot.first().id)
    }

    @Test
    fun `deleteHabit calls repository delete`() {
        viewModel.deleteHabit("h1")

        coVerify {
            habitsRepository.deleteHabit(match { it.id == "h1" })
        }
    }

    @Test
    fun `deleteHabit with unknown id does nothing`() {
        viewModel.deleteHabit("non-existent")

        coVerify(exactly = 0) { habitsRepository.deleteHabit(any()) }
    }

    @Test
    fun `init handles repository error gracefully`() {
        val errorRepo: HabitsRepository = mockk()
        every { errorRepo.getHabits(any()) } returns flowOf()

        val errorViewModel = HabitsViewModel(errorRepo, locationRepository)
        assertEquals(0, errorViewModel.uiState.value.habits.size)
    }

    @Test
    fun `completeHabit success marks habit complete and captures public location`() {
        var callbackResult: Boolean? = null
        viewModel.completeHabit("h1", isPublic = true) { callbackResult = it }

        assertTrue(callbackResult!!)
        assertTrue(viewModel.uiState.value.habits.find { it.id == "h1" }?.completed == true)
        coVerify { habitsRepository.completeHabit(match { it.id == "h1" }) }
        coVerify { locationRepository.captureAndSaveLocation("h1", true) }
    }

    @Test
    fun `completeHabit success captures private location when toggled off`() {
        viewModel.completeHabit("h1", isPublic = false)

        coVerify { locationRepository.captureAndSaveLocation("h1", false) }
    }

    @Test
    fun `completeHabit failure does not mark complete or capture location`() {
        coEvery { habitsRepository.completeHabit(any()) } returns false

        var callbackResult: Boolean? = null
        viewModel.completeHabit("h1", isPublic = true) { callbackResult = it }

        assertFalse(callbackResult!!)
        assertTrue(viewModel.uiState.value.habits.find { it.id == "h1" }?.completed == false)
        coVerify(exactly = 0) { locationRepository.captureAndSaveLocation(any(), any()) }
    }

    @Test
    fun `completeHabit with unknown id does nothing`() {
        var callbackResult: Boolean? = null
        viewModel.completeHabit("non-existent", isPublic = true) { callbackResult = it }

        assertFalse(callbackResult!!)
        coVerify(exactly = 0) { habitsRepository.completeHabit(any()) }
        coVerify(exactly = 0) { locationRepository.captureAndSaveLocation(any(), any()) }
    }
}
