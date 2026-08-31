package com.habitflowai.presentation.viewmodel

import com.habitflowai.data.model.HomeResponse
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.GoalsRepository
import com.habitflowai.domain.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val goalsRepository: GoalsRepository = mockk()
    private val locationRepository: LocationRepository = mockk()
    private val authManager: AuthManager = mockk()
    private val collapseState = HomeSectionCollapseState()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        coEvery { authManager.currentUserId } returns MutableStateFlow("local_user")
        coEvery { goalsRepository.getDatesWithCompletions(any()) } returns flowOf(emptyList())
        coEvery { goalsRepository.getTasksForDate(any(), any()) } returns flowOf(emptyList())
        val sampleHomeData = HomeResponse(
            goal = "Stay consistent",
            motivationalMessage = "Keep going",
            coreGoals = emptyList(),
            dailyVariations = emptyList(),
            success = true
        )
        coEvery { goalsRepository.syncDailyTasks(any(), any()) } returns Result.success(sampleHomeData)
        coEvery { goalsRepository.ensureHistoryLoaded(any(), any()) } returns Unit

        viewModel = HomeViewModel(goalsRepository, locationRepository, authManager, collapseState)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting a past date loads its history instead of syncing today`() {
        val pastDate = LocalDate.now().minusDays(3)

        viewModel.onDateSelected(pastDate)

        coVerify { goalsRepository.ensureHistoryLoaded("local_user", pastDate.toString()) }
    }

    @Test
    fun `selecting today does not load history`() {
        viewModel.onDateSelected(LocalDate.now())

        coVerify(exactly = 0) { goalsRepository.ensureHistoryLoaded(any(), any()) }
    }

    @Test
    fun `toggleSection collapses and re-expands a section`() {
        viewModel.toggleSection("tips")
        assertTrue("tips" in collapseState.collapsedKeys.value)

        viewModel.toggleSection("tips")
        assertTrue("tips" !in collapseState.collapsedKeys.value)
    }

    @Test
    fun `collapsed sections survive the ViewModel being recreated, since the collapse state is shared`() = runTest {
        viewModel.toggleSection("progress")

        val recreatedViewModel = HomeViewModel(goalsRepository, locationRepository, authManager, collapseState)

        // uiState only starts combining its upstream flows once it has a subscriber.
        assertTrue("progress" in recreatedViewModel.uiState.first().collapsedSections)
    }
}
