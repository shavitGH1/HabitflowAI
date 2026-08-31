package com.habitflowai.presentation.viewmodel

import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.LoginRequest
import com.habitflowai.data.model.LoginResponse
import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.model.RegisterResponse
import com.habitflowai.data.local.HabitFlowDatabase
import com.habitflowai.data.local.dao.RegistrationDraftDao
import com.habitflowai.data.local.entity.RegistrationDraftEntity
import com.habitflowai.data.model.CheckEmailResponse
import com.habitflowai.data.model.OnboardingSuggestionItem
import com.habitflowai.data.model.OnboardingSuggestionsRequest
import com.habitflowai.data.model.OnboardingSuggestionsResponse
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.AuthRepository
import com.habitflowai.domain.repository.PersonaRepository
import com.habitflowai.domain.repository.UserRepository
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
    private val userRepository: UserRepository = mockk()
    private val api: HabitFlowApi = mockk()
    private val authManager: AuthManager = mockk()
    private val database: HabitFlowDatabase = mockk()
    private val registrationDraftDao: RegistrationDraftDao = mockk()

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authManager.updateTokens(any(), any()) } just runs
        every { authManager.clearTokens() } just runs
        every { database.clearAllTables() } just runs
        coEvery { registrationDraftDao.save(any()) } just runs
        coEvery { registrationDraftDao.delete(any()) } just runs
        coEvery { registrationDraftDao.getByEmail(any()) } returns null
        viewModel = OnboardingViewModel(
            repository = personaRepository,
            authRepository = authRepository,
            userRepository = userRepository,
            api = api,
            authManager = authManager,
            database = database,
            registrationDraftDao = registrationDraftDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logout clears tokens and wipes locally cached data`() {
        viewModel.logout()

        verify { authManager.clearTokens() }
        verify(timeout = 1000) { database.clearAllTables() }
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

        coVerify {
            registrationDraftDao.save(match {
                it.email == "test@example.com" && it.goal == "Run a marathon"
            })
        }
        coVerify { registrationDraftDao.delete("test@example.com") }
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

    @Test
    fun `registerUser saves a draft before submitting and keeps it when registration fails`() {
        coEvery { authRepository.register(any<RegisterRequest>()) } throws RuntimeException("AI Service is currently overloaded.")

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onGoalChange("Run a marathon")
        viewModel.onQuizAnswerChange(0, "I want to push my limits")
        viewModel.onQuizAnswerChange(1, "Daily streaks kept me going")
        viewModel.onQuizAnswerChange(2, "My friend and I worked out together")
        viewModel.onQuizAnswerChange(3, "I change my playlist to stay fresh")

        viewModel.registerUser()

        coVerify { registrationDraftDao.save(match { it.email == "test@example.com" }) }
        coVerify(exactly = 0) { registrationDraftDao.delete(any()) }
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `checkEmail restores a saved draft's goal and quiz answers when one exists for that email`() {
        val savedDraft = RegistrationDraftEntity(
            email = "test@example.com",
            firstName = "",
            lastName = "",
            goal = "Run a marathon",
            quizAnswers = listOf("a", "b", "c", "d", "e", "f")
        )
        coEvery { registrationDraftDao.getByEmail("test@example.com") } returns savedDraft
        coEvery { api.checkEmail(any()) } returns CheckEmailResponse(available = true)

        viewModel.onEmailChange("test@example.com")
        viewModel.checkEmail()

        val state = viewModel.uiState.value
        assertEquals("Run a marathon", state.goal)
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), state.quizAnswers)
        assertTrue(state.proceedToOnboarding)
    }

    @Test
    fun `checkEmail leaves state untouched when no draft exists for that email`() {
        coEvery { registrationDraftDao.getByEmail("fresh@example.com") } returns null
        coEvery { api.checkEmail(any()) } returns CheckEmailResponse(available = true)

        viewModel.onEmailChange("fresh@example.com")
        viewModel.checkEmail()

        val state = viewModel.uiState.value
        assertEquals("", state.goal)
        assertTrue(state.proceedToOnboarding)
    }

    @Test
    fun `refreshOnboardingSuggestionsMidpoint sends answers so far and merges the result in`() {
        coEvery { api.getOnboardingSuggestions(any()) } returns OnboardingSuggestionsResponse(
            suggestions = listOf(
                OnboardingSuggestionItem(questionId = 4, options = listOf("d1", "d2", "d3")),
                OnboardingSuggestionItem(questionId = 5, options = listOf("e1", "e2", "e3")),
                OnboardingSuggestionItem(questionId = 6, options = listOf("f1", "f2", "f3"))
            )
        )

        viewModel.onGoalChange("Run a marathon")
        viewModel.onQuizAnswerChange(0, "I set a goal to run a marathon")
        viewModel.onQuizAnswerChange(1, "I've been learning Italian every day")
        viewModel.onQuizAnswerChange(2, "I started cooking dinner every night")

        viewModel.refreshOnboardingSuggestionsMidpoint()

        coVerify {
            api.getOnboardingSuggestions(match<OnboardingSuggestionsRequest> {
                it.goal == "Run a marathon" &&
                    it.answeredSoFar == listOf(
                        "I set a goal to run a marathon",
                        "I've been learning Italian every day",
                        "I started cooking dinner every night",
                        "", "", ""
                    )
            })
        }
        assertEquals(listOf("d1", "d2", "d3"), viewModel.uiState.value.suggestionsByQuestionId[4])
        assertEquals(listOf("e1", "e2", "e3"), viewModel.uiState.value.suggestionsByQuestionId[5])
        assertEquals(listOf("f1", "f2", "f3"), viewModel.uiState.value.suggestionsByQuestionId[6])
    }

    @Test
    fun `refreshOnboardingSuggestionsMidpoint only fires once`() {
        coEvery { api.getOnboardingSuggestions(any()) } returns OnboardingSuggestionsResponse(suggestions = emptyList())

        viewModel.onGoalChange("Run a marathon")
        viewModel.refreshOnboardingSuggestionsMidpoint()
        viewModel.refreshOnboardingSuggestionsMidpoint()

        coVerify(exactly = 1) { api.getOnboardingSuggestions(any()) }
    }
}
