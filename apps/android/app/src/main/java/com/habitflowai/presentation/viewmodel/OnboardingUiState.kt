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
    val profilePicture: String? = null,
    val navigateToHome: Boolean = false,
    val proceedToOnboarding: Boolean = false,
    val isRetakeMode: Boolean = false,
    val suggestionsByQuestionId: Map<Int, List<String>> = emptyMap(),
    val suggestionsForGoal: String? = null,
    // Set when Google Sign-In finds no matching account — carries the short-lived
    // token needed to complete registration once the onboarding quiz is done.
    val googleSignupToken: String? = null,
    // Distinct from navigateToHome: that flag is also used mid-onboarding-quiz
    // completion (handled by OnboardingRoute itself), while this one fires from
    // Login/RegisterCredentials for an existing account signing in via Google.
    val navigateToNameEntry: Boolean = false,
    val googleLoginSuccess: Boolean = false,
    // When the name was last changed — drives the 3-month edit-profile cooldown.
    val nameChangedAt: String? = null,
    // "local" or "google" — Google accounts have no password the user knows, so the
    // password section of Edit Profile is hidden entirely for them.
    val authProvider: String? = null,
    // One-shot flag: set on a successful password change, consumed by the UI then cleared
    // via onPasswordChangeHandled(), same pattern as navigateToHome/googleLoginSuccess.
    val passwordChangeSuccess: Boolean = false
)
