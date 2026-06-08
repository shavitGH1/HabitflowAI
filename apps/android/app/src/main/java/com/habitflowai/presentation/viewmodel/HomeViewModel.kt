package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.repository.GoalsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.habitflowai.data.model.HomeResponse

data class HomeUiState(
    val homeData: HomeResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val goalsRepository: GoalsRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun completeTask(taskId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = goalsRepository.completeTask(taskId)
                if (success) {
                    _uiState.value.homeData?.let { currentData ->
                        val updatedCoreGoals = currentData.coreGoals.map {
                            if (it.id == taskId) it.copy(completed = true) else it
                        }
                        val updatedDailyVariations = currentData.dailyVariations.map {
                            if (it.id == taskId) it.copy(completed = true) else it
                        }

                        _uiState.value = _uiState.value.copy(
                            homeData = currentData.copy(
                                coreGoals = updatedCoreGoals,
                                dailyVariations = updatedDailyVariations
                            )
                        )
                    }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun fetchHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val data = goalsRepository.getHomeData()
                _uiState.value = _uiState.value.copy(homeData = data, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }
}

class HomeViewModelFactory(private val repository: GoalsRepositoryImpl) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
