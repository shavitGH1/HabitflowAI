package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.domain.repository.GoalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.habitflowai.data.model.HomeResponse
import javax.inject.Inject

data class HomeUiState(
    val homeData: HomeResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val goalsRepository: GoalsRepository
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

