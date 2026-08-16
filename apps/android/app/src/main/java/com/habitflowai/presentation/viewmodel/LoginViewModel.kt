package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.habitflowai.data.model.LoginRequest
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToHome: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: HabitFlowApi,
    private val authManager: AuthManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun login() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(errorMessage = "Please enter your email and password")
            return
        }

        _uiState.value = current.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val response = api.login(LoginRequest(current.email, current.password))
                authManager.updateTokens(response.accessToken, response.refreshToken)
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (_: Exception) { null }
                if (fcmToken != null) {
                    authRepository.updateFcmToken(fcmToken)
                }
                _uiState.value = _uiState.value.copy(isLoading = false, navigateToHome = true)
            } catch (e: Exception) {
                val errorMsg = when {
                    e is retrofit2.HttpException && e.code() == 429 -> 
                        "Too many attempts. Please wait a moment before trying again."
                    e is retrofit2.HttpException && e.code() == 401 -> 
                        "Invalid email or password. Please try again."
                    else -> "We couldn't log you in. Please check your connection."
                }
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    fun onNavigated() {
        _uiState.value = _uiState.value.copy(navigateToHome = false)
    }
}
