package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.domain.repository.HabitFlowRepository
import com.habitflowai.presentation.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val goal: String = "",
    val quizAnswers: List<String> = listOf("", "", ""),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val personaResponse: ClassifyPersonaResponse? = null,
    val shouldNavigateToHome: Boolean = false
)

class OnboardingViewModel(
    private val repository: HabitFlowRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

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

    fun submitPersonaClassification() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null, shouldNavigateToHome = false)
            when (val result = repository.classifyPersona(
                ClassifyPersonaRequest(
                    goal = currentState.goal,
                    quizAnswers = currentState.quizAnswers
                )
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        personaResponse = result.data,
                        shouldNavigateToHome = true
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                Resource.Loading -> Unit
            }
        }
    }
}
