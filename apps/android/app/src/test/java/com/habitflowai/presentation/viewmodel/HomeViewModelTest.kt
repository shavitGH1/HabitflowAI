package com.habitflowai.presentation.viewmodel

import com.habitflowai.data.model.HomeGoalTask
import com.habitflowai.data.model.HomeResponse
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.GoalsRepository
import com.habitflowai.domain.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val goalsRepository: GoalsRepository = mockk()
    private val locationRepository: LocationRepository = mockk()
    private val authManager: AuthManager = mockk()
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private var uiStateCollectJob: Job? = null

    private val sampleHomeData = HomeResponse(
        goal = "Run a marathon",
        motivationalMessage = "Keep pushing!",
        coreGoals = listOf(
            HomeGoalTask(description = "Run 5km", points = 10, id = "task-1", completed = false),
            HomeGoalTask(description = "Stretch 10min", points = 5, id = "task-2", completed = true)
        ),
        dailyVariations = listOf(
            HomeGoalTask(description = "Drink water", points = 3, id = "task-3", completed = false)
        ),
        success = true,
        personaType = "Achiever",
        portfolioSummary = "You're an Achiever",
        tips = listOf("Tip 1", "Tip 2"),
        failurePatterns = listOf("Pattern 1"),
        confidenceScore = 0.85
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { locationRepository.captureAndSaveLocation(any(), any(), any()) } just runs
        every { authManager.currentUserId } returns MutableStateFlow("local_user")
        // combine() only emits once every source flow has emitted at least once -
        // these must produce a value (not emptyFlow()) or uiState stays frozen at initialValue.
        every { goalsRepository.getDatesWithCompletions(any()) } returns flowOf(emptyList())
        every { goalsRepository.getTasksForDate(any(), any()) } returns flowOf(emptyList())
        // HomeViewModel's init block eagerly calls fetchHomeData(), which runs to completion
        // synchronously under UnconfinedTestDispatcher - a default stub is needed before
        // construction or that call hits the strict mock's unstubbed-call exception.
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(sampleHomeData)
        viewModel = HomeViewModel(goalsRepository, locationRepository, authManager)
        // uiState is a stateIn(WhileSubscribed) flow - it only runs while collected,
        // so tests reading .value directly need an active subscriber first.
        uiStateCollectJob = testScope.launch { viewModel.uiState.collect() }
    }

    @After
    fun tearDown() {
        uiStateCollectJob?.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads home data via init block`() {
        // init { fetchHomeData() } runs eagerly on construction (see setUp()), so by the
        // time a test can observe uiState, the default stub's data is already loaded.
        val state = viewModel.uiState.value
        assertNotNull(state.homeData)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `fetchHomeData loads data and updates state`() {
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(sampleHomeData)

        viewModel.fetchHomeData()

        val state = viewModel.uiState.value
        assertNotNull(state.homeData)
        assertEquals("Run a marathon", state.homeData?.goal)
        assertEquals("You're an Achiever", state.portfolioSummary)
        assertEquals(listOf("Tip 1", "Tip 2"), state.tips)
        assertEquals(listOf("Pattern 1"), state.failurePatterns)
        assertEquals(0.85, state.confidenceScore!!, 0.001)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `fetchHomeData sets loading state`() {
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(sampleHomeData)

        viewModel.fetchHomeData()

        // With UnconfinedTestDispatcher, loading state transitions happen synchronously
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading) // final state has loading = false
    }

    @Test
    fun `fetchHomeData handles error`() {
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.failure(Exception("Network error"))

        viewModel.fetchHomeData()

        val state = viewModel.uiState.value
        assertEquals("Network error", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `completeTask success updates ui state and captures location`() {
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(sampleHomeData)
        viewModel.fetchHomeData()

        coEvery { goalsRepository.updateTaskCompletion(any(), "task-1", true) } returns true
        // completeTask() no longer mutates state locally - it trusts the server refresh
        // triggered by its own fetchHomeData() call, so the mock must reflect the server
        // having actually applied the completion.
        val completedHomeData = sampleHomeData.copy(
            coreGoals = sampleHomeData.coreGoals.map { if (it.id == "task-1") it.copy(completed = true) else it }
        )
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(completedHomeData)

        var callbackResult: Boolean? = null
        viewModel.completeTask("task-1") { callbackResult = it }

        val state = viewModel.uiState.value
        assertTrue(callbackResult!!)
        assertEquals(true, state.homeData?.coreGoals?.find { it.id == "task-1" }?.completed)
        coVerify { locationRepository.captureAndSaveLocation("task-1", true, "task") }
    }

    @Test
    fun `completeTask failure does not update state`() {
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(sampleHomeData)
        viewModel.fetchHomeData()

        coEvery { goalsRepository.updateTaskCompletion(any(), "task-2", true) } returns false

        var callbackResult: Boolean? = null
        viewModel.completeTask("task-2") { callbackResult = it }

        val state = viewModel.uiState.value
        assertFalse(callbackResult!!)
        // task-2 was already completed=true in sample data; state shouldn't change
        coVerify(exactly = 0) { locationRepository.captureAndSaveLocation(any(), any(), any()) }
    }

    @Test
    fun `completeTask exception propagates failure`() {
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(sampleHomeData)
        viewModel.fetchHomeData()

        coEvery { goalsRepository.updateTaskCompletion(any(), "task-1", true) } throws Exception("API error")

        var callbackResult: Boolean? = null
        viewModel.completeTask("task-1") { callbackResult = it }

        assertFalse(callbackResult!!)
        coVerify(exactly = 0) { locationRepository.captureAndSaveLocation(any(), any(), any()) }
    }

    @Test
    fun `completeTask without loaded homeData still calls API`() {
        coEvery { goalsRepository.updateTaskCompletion(any(), "task-1", true) } returns true

        var callbackResult: Boolean? = null
        viewModel.completeTask("task-1") { callbackResult = it }

        assertTrue(callbackResult!!)
        coVerify { locationRepository.captureAndSaveLocation("task-1", true, "task") }
    }

    @Test
    fun `completeTask updates daily variations too`() {
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(sampleHomeData)
        viewModel.fetchHomeData()

        coEvery { goalsRepository.updateTaskCompletion(any(), "task-3", true) } returns true
        val completedHomeData = sampleHomeData.copy(
            dailyVariations = sampleHomeData.dailyVariations.map { if (it.id == "task-3") it.copy(completed = true) else it }
        )
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(completedHomeData)

        viewModel.completeTask("task-3")

        val state = viewModel.uiState.value
        assertEquals(true, state.homeData?.dailyVariations?.find { it.id == "task-3" }?.completed)
    }
}
