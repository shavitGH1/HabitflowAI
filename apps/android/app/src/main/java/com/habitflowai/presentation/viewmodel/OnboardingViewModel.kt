package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.google.firebase.messaging.FirebaseMessaging
import com.habitflowai.data.model.CheckEmailRequest
import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.GoogleAuthRequest
import com.habitflowai.data.model.GoogleRegisterRequest
import com.habitflowai.data.model.LoginRequest
import com.habitflowai.data.model.OnboardingSuggestionsRequest
import com.habitflowai.data.model.ReclassifyRequest
import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.data.local.HabitFlowDatabase
import com.habitflowai.data.local.dao.RegistrationDraftDao
import com.habitflowai.data.local.entity.RegistrationDraftEntity
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.AuthRepository
import com.habitflowai.domain.repository.PersonaRepository
import com.habitflowai.domain.repository.UserRepository
import com.habitflowai.util.Resource
import com.habitflowai.util.extractErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the onboarding process, handling user goals, quiz answers,
 * persona classification, and user registration.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: PersonaRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val api: HabitFlowApi,
    private val authManager: AuthManager,
    private val database: HabitFlowDatabase,
    private val registrationDraftDao: RegistrationDraftDao
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

    /** Re-fetches suggestions for the remaining questions once, informed by answers 1-3, so
     *  later questions can reflect things the user already told us (e.g. a mentioned habit)
     *  instead of only ever knowing the stated goal. */
    fun refreshOnboardingSuggestionsMidpoint() {
        val currentState = _uiState.value
        if (currentState.midpointSuggestionsFetched || currentState.goal.isBlank()) return
        _uiState.value = currentState.copy(midpointSuggestionsFetched = true)

        viewModelScope.launch {
            try {
                val response = api.getOnboardingSuggestions(
                    OnboardingSuggestionsRequest(currentState.goal, answeredSoFar = currentState.quizAnswers)
                )
                val refreshed = response.suggestions.associate { it.questionId to it.options }
                _uiState.value = _uiState.value.copy(
                    suggestionsByQuestionId = _uiState.value.suggestionsByQuestionId + refreshed
                )
            } catch (_: Exception) {
                // Best-effort only — the goal-only suggestions from the initial fetch still work.
            }
        }
    }

    fun onHomeNavigated() {
        _uiState.value = _uiState.value.copy(navigateToHome = false)
    }

    fun onSuggestionsNavigated() {
        _uiState.value = _uiState.value.copy(navigateToSuggestions = false)
    }

    fun finishOnboarding() {
        _uiState.value = _uiState.value.copy(navigateToHome = true)
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
                    // A previous attempt with this email may have gotten this far and then
                    // failed after submitting (e.g. an AI timeout) - restore what they'd
                    // already filled in rather than making them redo the whole quiz.
                    val draft = registrationDraftDao.getByEmail(email)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        proceedToOnboarding = true,
                        goal = draft?.goal ?: _uiState.value.goal,
                        quizAnswers = draft?.quizAnswers ?: _uiState.value.quizAnswers
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "This email is already in use. Try logging in instead.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Couldn't check email. Please check your connection.")
            }
        }
    }

    fun onNameEntryNavigated() {
        _uiState.value = _uiState.value.copy(navigateToNameEntry = false)
    }

    fun onGoogleLoginHandled() {
        _uiState.value = _uiState.value.copy(googleLoginSuccess = false)
    }

    fun onGoogleSignInFailed(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = message)
    }

    /** Verifies a Google ID token from the native Sign-In flow; branches on isNewUser. */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = api.verifyGoogleIdToken(GoogleAuthRequest(idToken))
                if (response.isNewUser) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        email = response.email ?: "",
                        firstName = response.firstName ?: "",
                        lastName = response.lastName ?: "",
                        googleSignupToken = response.signupToken,
                        navigateToNameEntry = true
                    )
                } else {
                    authManager.updateTokens(response.accessToken, response.refreshToken)
                    _uiState.value = _uiState.value.copy(isLoading = false, googleLoginSuccess = true)
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) extractErrorMessage(e) else "Network error: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    /** Completes registration for a Google Sign-In user after the onboarding quiz. */
    fun registerViaGoogle() {
        val currentState = _uiState.value
        val signupToken = currentState.googleSignupToken
        if (signupToken == null) {
            _uiState.value = currentState.copy(errorMessage = "Google signup session expired. Please sign in with Google again.")
            return
        }
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

                val response = api.registerGoogle(
                    GoogleRegisterRequest(
                        signupToken = signupToken,
                        goal = currentState.goal,
                        openAnswers = currentState.quizAnswers,
                        fcmToken = fcmToken
                    )
                )
                authManager.updateTokens(response.accessToken, response.refreshToken)

                _uiState.value = currentState.copy(
                    isLoading = false,
                    googleSignupToken = null,
                    personaResult = ClassifyPersonaResponse(
                        id = response.userId,
                        personaType = response.personaType ?: "Achiever",
                        motivationalMessage = response.motivationalMessage ?: "Welcome to your growth journey!",
                        success = true,
                        userId = response.userId
                    ),
                    suggestedHabits = response.coreGoals ?: emptyList(),
                    errorMessage = null,
                    navigateToHome = true
                )
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) {
                    extractErrorMessage(e)
                } else {
                    "We couldn't complete your registration. Please check your connection."
                }
                _uiState.value = currentState.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    fun fetchProfile() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val homeData = api.getHome(date = java.time.LocalDate.now().toString())
                if (homeData.success) {
                    authManager.currentUserId.value?.let { userId ->
                        userRepository.cacheProfile(
                            userId = userId,
                            email = homeData.email,
                            goal = homeData.goal,
                            personaType = homeData.personaType,
                            profilePicture = homeData.profilePicture
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        firstName = homeData.firstName ?: "",
                        lastName = homeData.lastName ?: "",
                        personaResult = ClassifyPersonaResponse(
                            id = "",
                            personaType = homeData.personaType ?: "Achiever",
                            motivationalMessage = homeData.motivationalMessage,
                            success = true
                        ),
                        achievements = homeData.achievements ?: emptyList(),
                        profilePicture = homeData.profilePicture,
                        nameChangedAt = homeData.nameChangedAt,
                        authProvider = homeData.authProvider
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException && e.code() == 429) {
                    "The server is a bit busy. Please wait a moment."
                } else null
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    /** Persists a bundled preset avatar (e.g. key "1" -> "preset:1") to the user profile. */
    fun selectPresetAvatar(presetKey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val profilePicture = userRepository.updateProfilePicture("preset:$presetKey")
                _uiState.value = _uiState.value.copy(isLoading = false, profilePicture = profilePicture)
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) extractErrorMessage(e)
                    else "Network error: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    /** Uploads a camera/gallery image and persists the returned /uploads URL to the profile. */
    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val profilePicture = userRepository.uploadProfilePicture(uri)
                _uiState.value = _uiState.value.copy(isLoading = false, profilePicture = profilePicture)
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) extractErrorMessage(e)
                    else "Network error: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    /** Updates first/last name. Server rejects with a 400 if still within the 3-month cooldown. */
    fun updateName(firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val nameChangedAt = userRepository.updateName(firstName, lastName)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    firstName = firstName,
                    lastName = lastName,
                    nameChangedAt = nameChangedAt
                )
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) extractErrorMessage(e)
                    else "Network error: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    /** Changes the password after the server verifies [currentPassword] matches. */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                userRepository.changePassword(currentPassword, newPassword)
                _uiState.value = _uiState.value.copy(isLoading = false, passwordChangeSuccess = true)
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) extractErrorMessage(e)
                    else "Network error: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    fun onPasswordChangeHandled() {
        _uiState.value = _uiState.value.copy(passwordChangeSuccess = false)
    }

    fun logout() {
        _uiState.value = OnboardingUiState()
        authManager.clearTokens()
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
        }
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

                // Saved before submitting, not after failing - the AI pipeline this triggers
                // can take a while (see GeminiClient's hedged model race) and nothing is saved
                // server-side until it fully succeeds, so this is the last safe point to
                // capture the user's progress before a timeout could lose it.
                registrationDraftDao.save(
                    RegistrationDraftEntity(
                        email = currentState.email,
                        firstName = currentState.firstName,
                        lastName = currentState.lastName,
                        goal = currentState.goal,
                        quizAnswers = currentState.quizAnswers
                    )
                )

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
                    registrationDraftDao.delete(currentState.email)
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
                        suggestedHabits = response.coreGoals ?: emptyList(),
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
                    "Something went wrong. Please check your connection and try again."
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
                        errorMessage = response.message
                    )
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) {
                    extractErrorMessage(e)
                } else {
                    "Something went wrong. Please check your connection and try again."
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
                    val homeData = api.getHome(date = java.time.LocalDate.now().toString())
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
                        errorMessage = response.message
                    )
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) {
                    extractErrorMessage(e)
                } else {
                    "Something went wrong. Please check your connection and try again."
                }
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }
}
