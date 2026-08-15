package com.habitflowai.presentation.viewmodel

import com.habitflowai.data.model.ClassifyPersonaResponse

data class OnboardingUiState(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val goal: String = "",
    val quizAnswers: List<String> = List(6) { "" },
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val personaResult: ClassifyPersonaResponse? = null,
    val navigateToHome: Boolean = false,
    val proceedToOnboarding: Boolean = false,
    val isRetakeMode: Boolean = false,
    val suggestionsByQuestionId: Map<Int, List<String>> = emptyMap(),
    val suggestionsForGoal: String? = null
)
