package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.model.LoginRequest
import com.habitflowai.data.network.NetworkModule
import com.habitflowai.domain.repository.PersonaRepository
import com.habitflowai.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val email: String = "",
    val password: String = "",
    val goal: String = "",
    val quizAnswers: List<String> = listOf("", "", "", ""),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val personaResult: ClassifyPersonaResponse? = null,
    val navigateToHome: Boolean = false
)

class OnboardingViewModel(
    private val repository: PersonaRepository
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

    fun logout() {
        _uiState.value = OnboardingUiState()
        NetworkModule.accessToken = null
        NetworkModule.refreshToken = null
    }

    fun registerUser() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null, navigateToHome = false)

            try {
                val api = NetworkModule.habitFlowApi
                val request = RegisterRequest(
                    email = currentState.email,
                    password = currentState.password,
                    goal = currentState.goal,
                    quizAnswers = currentState.quizAnswers
                )
                val response = api.register(request)

                if (response.success) {
                    val loginRes = api.login(LoginRequest(currentState.email, currentState.password))
                    NetworkModule.accessToken = loginRes.accessToken
                    NetworkModule.refreshToken = loginRes.refreshToken

                    _uiState.value = currentState.copy(
                        isLoading = false,
                        personaResult = ClassifyPersonaResponse(
                            id = response.userId,
                            personaType = "Achiever", 
                            motivationalMessage = "Welcome to your growth journey!",
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
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
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
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            }
        }
    }
}

class OnboardingViewModelFactory(
    private val repository: PersonaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) return OnboardingViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
