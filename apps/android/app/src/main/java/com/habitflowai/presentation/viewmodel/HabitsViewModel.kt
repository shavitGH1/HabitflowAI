package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.data.model.ActiveGoalResponse
import com.habitflowai.domain.repository.HabitsRepository
import com.habitflowai.domain.repository.GoalsRepository
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HabitsUiState(
    val habits: List<HabitEntity> = emptyList(),
    val activeGoal: ActiveGoalResponse? = null,
    val onboardingGoal: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val habitStats: Map<String, Map<String, Any>> = emptyMap()
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitsRepository: HabitsRepository,
    private val goalsRepository: GoalsRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    private val userId = "local_user"

    init {
        fetchActiveGoal()
        viewModelScope.launch {
            habitsRepository.refreshHabits()
        }
        viewModelScope.launch {
            habitsRepository.getHabits(userId)
                .map { entities -> HabitsUiState(habits = entities, isLoading = false) }
                .catch { e -> emit(HabitsUiState(errorMessage = e.message, isLoading = false)) }
                .collect { state -> _uiState.value = state }
        }
    }

    fun addHabit(title: String, description: String, frequency: String) {
        viewModelScope.launch {
            val habit = HabitEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                frequency = frequency,
                userId = userId,
                completed = false,
                syncStatus = SyncStatus.PENDING_CREATE
            )
            habitsRepository.createHabit(habit)
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            val current = _uiState.value.habits.find { it.id == habitId } ?: return@launch
            habitsRepository.deleteHabit(current)
        }
    }

    fun completeHabit(habitId: String, isPublic: Boolean = true, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val habit = _uiState.value.habits.find { it.id == habitId }
            if (habit == null) {
                onResult(false)
                return@launch
            }
            val success = habitsRepository.completeHabit(habit)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    habits = _uiState.value.habits.map {
                        if (it.id == habitId) it.copy(completed = true) else it
                    }
                )
                locationRepository.captureAndSaveLocation(habit.id, isPublic, "habit")
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun fetchHabitStats(habitId: String) {
        viewModelScope.launch {
            val stats = habitsRepository.getHabitStats(habitId)
            val currentStats = _uiState.value.habitStats.toMutableMap()
            currentStats[habitId] = stats
            _uiState.value = _uiState.value.copy(habitStats = currentStats)
        }
    }

    fun fetchActiveGoal() {
        viewModelScope.launch {
            try {
                val goal = goalsRepository.getActiveGoal()
                _uiState.value = _uiState.value.copy(activeGoal = goal)
            } catch (e: Exception) {
                // If no actionable goal, try to get the onboarding goal from home data
                try {
                    val homeData = goalsRepository.getHomeData()
                    _uiState.value = _uiState.value.copy(onboardingGoal = homeData.goal)
                } catch (e2: Exception) {
                    // Ignore
                }
            }
        }
    }
}
