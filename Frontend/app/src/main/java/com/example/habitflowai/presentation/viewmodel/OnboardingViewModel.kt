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

            // Local calculation logic (Bypass Network to prevent connection failure)
            kotlinx.coroutines.delay(1200) // Fake analysis delay

            // Extract dominant trait from answers (the beautiful quiz UI maps character names in the answers)
            val answers = currentState.quizAnswers.filter { it.isNotBlank() }
            val dominantCharacter = if (answers.isNotEmpty()) {
                answers.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Regulator"
            } else {
                "Regulator"
            }

            val response = ClassifyPersonaResponse(
                id = java.util.UUID.randomUUID().toString(),
                personaType = dominantCharacter,
                motivationalMessage = "Your adaptive journey begins here, $dominantCharacter!",
                success = true
            )

            _uiState.value = currentState.copy(
                isLoading = false,
                personaResult = response,
                errorMessage = null,
                navigateToHome = true
            )
        }
    }
}
