package com.habitflowai.presentation.viewmodel

import com.habitflowai.data.model.ClassifyPersonaResponse

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
