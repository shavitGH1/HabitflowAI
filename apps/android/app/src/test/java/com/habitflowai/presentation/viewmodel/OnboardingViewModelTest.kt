package com.habitflowai.presentation.viewmodel

import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.LoginRequest
import com.habitflowai.data.model.LoginResponse
import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.model.RegisterResponse
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.AuthRepository
import com.habitflowai.domain.repository.PersonaRepository
import com.habitflowai.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val personaRepository: PersonaRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val api: HabitFlowApi = mockk()
    private val authManager: AuthManager = mockk()

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authManager.updateTokens(any(), any()) } just runs
        viewModel = OnboardingViewModel(
            repository = personaRepository,
            authRepository = authRepository,
            api = api,
            authManager = authManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `registerUser success sets navigateToHome and stores tokens`() {
        val registerResponse = RegisterResponse(
            message = "User registered successfully",
            userId = "user-123",
            success = true,
            personaType = "Achiever",
            motivationalMessage = "Welcome!"
        )
        val loginResponse = LoginResponse(
            accessToken = "access-token-123",
            refreshToken = "refresh-token-456",
            success = true
        )

        coEvery { authRepository.register(any<RegisterRequest>()) } returns registerResponse
        coEvery { api.login(any<LoginRequest>()) } returns loginResponse

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onGoalChange("Run a marathon")
        viewModel.onQuizAnswerChange(0, "I want to push my limits")
        viewModel.onQuizAnswerChange(1, "Daily streaks kept me going")
        viewModel.onQuizAnswerChange(2, "My friend and I worked out together")
        viewModel.onQuizAnswerChange(3, "I change my playlist to stay fresh")
        viewModel.onQuizAnswerChange(4, "I want to make my family proud")
        viewModel.onQuizAnswerChange(5, "Fixed schedule works best for me")

        viewModel.registerUser()

        val state = viewModel.uiState.value
        assertTrue(state.navigateToHome)
        assertNotNull(state.personaResult)
        assertEquals("Achiever", state.personaResult?.personaType)
        assertEquals("Welcome!", state.personaResult?.motivationalMessage)
        assertEquals("user-123", state.personaResult?.userId)
        assertTrue(state.errorMessage == null)

        coVerify { authRepository.register(any<RegisterRequest>()) }
        coVerify { api.login(LoginRequest("test@example.com", "password123")) }
        verify { authManager.updateTokens("access-token-123", "refresh-token-456") }
    }

    @Test
    fun `registerUser with fewer than 4 answers shows error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onGoalChange("Run a marathon")
        viewModel.onQuizAnswerChange(0, "I want to push my limits")
        viewModel.onQuizAnswerChange(1, "Daily streaks kept me going")

        viewModel.registerUser()

        val state = viewModel.uiState.value
        assertEquals("Please answer at least 4 questions before continuing.", state.errorMessage)
        assertEquals(false, state.navigateToHome)
    }
}
