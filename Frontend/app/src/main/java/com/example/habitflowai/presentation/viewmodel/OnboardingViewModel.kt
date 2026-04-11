package com.example.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitflowai.data.model.ClassifyPersonaRequest
import com.example.habitflowai.data.model.ClassifyPersonaResponse
import com.example.habitflowai.domain.repository.PersonaRepository
import com.example.habitflowai.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val goal: String = "",
    val quizAnswers: List<String> = listOf("", "", ""),
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

    fun onGoalChange(goal: String) {
        _uiState.value = _uiState.value.copy(goal = goal)
    }

    fun onQuizAnswerChange(index: Int, value: String) {
        val updatedAnswers = _uiState.value.quizAnswers.toMutableList()
        if (index in updatedAnswers.indices) {
            updatedAnswers[index] = value
            _uiState.value = _uiState.value.copy(quizAnswers = updatedAnswers)
        }
    }

    fun onHomeNavigated() {
        _uiState.value = _uiState.value.copy(navigateToHome = false)
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
                        // Assuming success=false from backend means invalid goal / gibberish
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            errorMessage = result.motivationalMessage ?: "Invalid goal or gibberish detected. Please enter a new valid goal."
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
