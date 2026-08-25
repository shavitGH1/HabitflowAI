package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.data.model.ActiveGoalResponse
import com.habitflowai.domain.repository.HabitsRepository
import com.habitflowai.domain.repository.GoalsRepository
import com.habitflowai.domain.repository.LocationRepository
import com.habitflowai.di.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HabitsUiState(
    val habits: List<HabitEntity> = emptyList(),
    val activeGoal: ActiveGoalResponse? = null,
    val onboardingGoal: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val habitStats: Map<String, Map<String, Any>> = emptyMap(),
    val congratulationMessage: String? = null
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitsRepository: HabitsRepository,
    private val goalsRepository: GoalsRepository,
    private val locationRepository: LocationRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    private val userId: String
        get() = authManager.currentUserId.value ?: "local_user"

    init {
        fetchActiveGoal()
        viewModelScope.launch {
            habitsRepository.refreshHabits()
        }
        viewModelScope.launch {
            authManager.currentUserId.collect { id ->
                id?.let {
                    habitsRepository.getHabits(it)
                        .catch { e ->
                            _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                        }
                        .collect { entities ->
                            _uiState.update { it.copy(habits = entities, isLoading = false) }
                        }
                }
            }
        }
    }

    fun addHabit(title: String, description: String, frequency: String) {
        viewModelScope.launch {
            val habit = HabitEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                frequency = frequency.lowercase(),
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val habit = _uiState.value.habits.find { it.id == habitId }
            if (habit == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Habit not found locally") }
                onResult(false)
                return@launch
            }
            
            val success = habitsRepository.completeHabit(habit)
            if (success) {
                val congrats = listOf(
                    "Amazing job! One step closer to your goals! 🚀",
                    "Habit crushed! Keep that momentum going! 💪",
                    "Fantastic! You're becoming the best version of yourself! ✨",
                    "Victory! Another day of consistency in the books! 🏆"
                ).random()
                
                _uiState.update { state ->
                    state.copy(
                        habits = state.habits.map {
                            if (it.id == habitId) it.copy(
                                completed = true,
                                completionHistory = (it.completionHistory + java.time.LocalDate.now().toString()).distinct()
                            ) else it
                        },
                        isLoading = false,
                        congratulationMessage = congrats
                    )
                }
                locationRepository.captureAndSaveLocation(habit.id, isPublic, "habit")
                onResult(true)
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Server error. Please try again.") }
                onResult(false)
            }
        }
    }

    fun clearCongratulation() {
        _uiState.update { it.copy(congratulationMessage = null) }
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
