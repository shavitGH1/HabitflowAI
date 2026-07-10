package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.habitflowai.data.model.CheckEmailRequest
import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.LoginRequest
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

data class OnboardingUiState(
    val email: String = "",
    val password: String = "",
    val goal: String = "",
    val quizAnswers: List<String> = List(6) { "" },
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val personaResult: ClassifyPersonaResponse? = null,
    val navigateToHome: Boolean = false,
    val proceedToOnboarding: Boolean = false
)

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

    fun onHomeNavigated() {
        _uiState.value = _uiState.value.copy(navigateToHome = false)
    }

    fun onOnboardingNavigated() {
        _uiState.value = _uiState.value.copy(proceedToOnboarding = false)
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val homeData = api.getHome()
                if (homeData.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        personaResult = ClassifyPersonaResponse(
                            id = "",
                            personaType = homeData.personaType ?: "Achiever",
                            motivationalMessage = homeData.motivationalMessage,
                            success = true
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
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
                    openAnswers = listOf(currentState.goal) + currentState.quizAnswers.drop(1).take(5),
                    fcmToken = fcmToken
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
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        val jsonObject = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                        jsonObject.get("message").asString
                    } catch (inner: Exception) {
                        "Server error: ${e.code()}"
                    }
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
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        val jsonObject = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                        jsonObject.get("message").asString
                    } catch (inner: Exception) {
                        "Server error: ${e.code()}"
                    }
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
