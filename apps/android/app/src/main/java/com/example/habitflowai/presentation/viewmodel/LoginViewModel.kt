package com.example.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.habitflowai.data.model.LoginRequest
import com.example.habitflowai.data.network.HabitFlowApi
import com.example.habitflowai.data.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToHome: Boolean = false
)

class LoginViewModel(private val api: HabitFlowApi) : ViewModel() {
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
            _uiState.value = current.copy(errorMessage = "Email and password are required")
            return
        }

        _uiState.value = current.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val response = api.login(LoginRequest(current.email, current.password))
                NetworkModule.accessToken = response.accessToken
                NetworkModule.refreshToken = response.refreshToken
                _uiState.value = _uiState.value.copy(isLoading = false, navigateToHome = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Login failed")
            }
        }
    }

    fun onNavigated() {
        _uiState.value = _uiState.value.copy(navigateToHome = false)
    }
}

class LoginViewModelFactory(private val api: HabitFlowApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) return LoginViewModel(api) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
