package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.habitflowai.data.model.CheckEmailRequest
import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.LoginRequest
import com.habitflowai.data.model.OnboardingSuggestionsRequest
import com.habitflowai.data.model.ReclassifyRequest
import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.AuthRepository
import com.habitflowai.domain.repository.PersonaRepository
import com.habitflowai.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun extractErrorMessage(httpException: retrofit2.HttpException): String {
    val fallback = "Server error: ${httpException.code()}"
    val errorBody = httpException.response()?.errorBody()?.string() ?: return fallback
    return try {
        val messageElement = com.google.gson.JsonParser.parseString(errorBody).asJsonObject.get("message")
        when {
            messageElement == null || messageElement.isJsonNull -> fallback
            messageElement.isJsonArray -> messageElement.asJsonArray.joinToString("\n") { it.asString }
            else -> messageElement.asString
        }
    } catch (_: Exception) {
        fallback
    }
}

/**
 * ViewModel for the onboarding process, handling user goals, quiz answers,
 * persona classification, and user registration.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: PersonaRepository,
    private val authRepository: AuthRepository,
    private val api: HabitFlowApi,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onFirstNameChange(firstName: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName)
    }

    fun onLastNameChange(lastName: String) {
        _uiState.value = _uiState.value.copy(lastName = lastName)
    }

    fun onGoalChange(goal: String) {
        _uiState.value = _uiState.value.copy(goal = goal)
    }

    fun onQuizAnswerChange(index: Int, answer: String) {
        val updatedAnswers = _uiState.value.quizAnswers.toMutableList()
        if (index in updatedAnswers.indices) {
            updatedAnswers[index] = answer
            _uiState.value = _uiState.value.copy(quizAnswers = updatedAnswers)
        }
    }

    fun fetchOnboardingSuggestions() {
        val goal = _uiState.value.goal
        if (goal.isBlank() || goal == _uiState.value.suggestionsForGoal) return
        _uiState.value = _uiState.value.copy(suggestionsForGoal = goal)

        viewModelScope.launch {
            try {
                val response = api.getOnboardingSuggestions(OnboardingSuggestionsRequest(goal))
                val byQuestionId = response.suggestions.associate { it.questionId to it.options }
                _uiState.value = _uiState.value.copy(suggestionsByQuestionId = byQuestionId)
            } catch (_: Exception) {
                // Best-effort only — free-text input still works without suggestions.
            }
        }
    }

    fun onHomeNavigated() {
        _uiState.value = _uiState.value.copy(navigateToHome = false)
    }

    fun onOnboardingNavigated() {
        _uiState.value = _uiState.value.copy(proceedToOnboarding = false)
    }

    fun startRetake() {
        _uiState.value = _uiState.value.copy(isRetakeMode = true, errorMessage = null, navigateToHome = false)
    }

    fun checkEmail() {
        val email = _uiState.value.email
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = api.checkEmail(CheckEmailRequest(email))
                if (result.available) {
                    _uiState.value = _uiState.value.copy(isLoading = false, proceedToOnboarding = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "An account with this email already exists")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Network error. Please try again.")
            }
        }
    }

    fun fetchProfile() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val homeData = api.getHome()
                if (homeData.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        firstName = homeData.firstName ?: "",
                        lastName = homeData.lastName ?: "",
                        personaResult = ClassifyPersonaResponse(
                            id = "",
                            personaType = homeData.personaType ?: "Achiever",
                            motivationalMessage = homeData.motivationalMessage,
                            success = true
                        )
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException && e.code() == 429) {
                    "Taking a short break... the server is busy. Please wait a moment."
                } else null
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    fun logout() {
        _uiState.value = OnboardingUiState()
        authManager.clearTokens()
    }

    fun registerUser() {
        val currentState = _uiState.value
        val filledAnswers = currentState.quizAnswers.count { it.isNotBlank() }
        if (filledAnswers < 4) {
            _uiState.value = currentState.copy(errorMessage = "Please answer at least 4 questions before continuing.")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null, navigateToHome = false)

            try {
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (_: Exception) { null }

                val request = RegisterRequest(
                    email = currentState.email,
                    password = currentState.password,
                    goal = currentState.goal,
                    openAnswers = currentState.quizAnswers,
                    fcmToken = fcmToken,
                    firstName = currentState.firstName,
                    lastName = currentState.lastName
                )
                val response = authRepository.register(request)

                if (response.success) {
                    val loginRes = api.login(LoginRequest(currentState.email, currentState.password))
                    authManager.updateTokens(loginRes.accessToken, loginRes.refreshToken)

                    _uiState.value = currentState.copy(
                        isLoading = false,
                        personaResult = ClassifyPersonaResponse(
                            id = response.userId,
                            personaType = response.personaType ?: "Achiever",
                            motivationalMessage = response.motivationalMessage ?: "Welcome to your growth journey!",
                            success = true,
                            userId = response.userId
                        ),
                        errorMessage = null,
                        navigateToHome = true
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = response.message
                    )
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) {
                    extractErrorMessage(e)
                } else {
                    "Network error: ${e.message}"
                }
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    fun classifyPersona() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null, navigateToHome = false)

            try {
                val request = ClassifyPersonaRequest(currentState.goal, currentState.quizAnswers)
                val response = repository.classifyPersona(request)

                if (response is Resource.Success && response.data != null) {
                    val result = response.data
                    if (result.success) {
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            personaResult = result,
                            errorMessage = null,
                            navigateToHome = true
                        )
                    } else {
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            errorMessage = result.motivationalMessage ?: "Invalid response. Please check your inputs."
                        )
                    }
                } else if (response is Resource.Error) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to classify persona"
                    )
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) {
                    extractErrorMessage(e)
                } else {
                    "Network error: ${e.message}"
                }
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    fun reclassifyPersona() {
        val currentState = _uiState.value
        val filledAnswers = currentState.quizAnswers.count { it.isNotBlank() }
        if (filledAnswers < 4) {
            _uiState.value = currentState.copy(errorMessage = "Please answer at least 4 questions before continuing.")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null, navigateToHome = false)

            try {
                val response = repository.reclassifyPersona(
                    ReclassifyRequest(goal = currentState.goal, openAnswers = currentState.quizAnswers)
                )

                if (response is Resource.Success && response.data != null) {
                    val homeData = api.getHome()
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        isRetakeMode = false,
                        personaResult = ClassifyPersonaResponse(
                            id = "",
                            personaType = homeData.personaType ?: response.data.personaType,
                            motivationalMessage = homeData.motivationalMessage,
                            success = true
                        ),
                        errorMessage = null,
                        navigateToHome = true
                    )
                } else if (response is Resource.Error) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to update persona"
                    )
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) {
                    extractErrorMessage(e)
                } else {
                    "Network error: ${e.message}"
                }
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }
}
