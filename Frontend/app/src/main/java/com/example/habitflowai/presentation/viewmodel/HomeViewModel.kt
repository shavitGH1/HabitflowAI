package com.example.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.habitflowai.data.repository.GoalsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val goalsMap: Map<LocalDate, List<Pair<String, Int>>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val goalsRepository: GoalsRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun fetchGoalsForDateRange(dates: List<LocalDate>, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val updatedMap = _uiState.value.goalsMap.toMutableMap()
            try {
                for (date in dates) {
                    if (!updatedMap.containsKey(date)) {
                        val goals = goalsRepository.fetchGoals(userId, date.dayOfWeek.value)
                        updatedMap[date] = goals
                    }
                }
                _uiState.value = _uiState.value.copy(goalsMap = updatedMap, isLoading = false)
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
