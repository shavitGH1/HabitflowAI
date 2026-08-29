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
    val congratulationMessage: String? = null,
    val suggestions: List<com.habitflowai.data.model.HomeGoalTask> = emptyList(),
    val relevanceWarning: String? = null
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

    private var suggestionSource: List<com.habitflowai.data.model.HomeGoalTask> = emptyList()
    private var suggestionSourceGoalId: Any? = UNFETCHED

    companion object {
        private val UNFETCHED = Any()
    }

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
                            applySuggestionFilter()
                        }
                }
            }
        }
    }

    fun refresh() {
        fetchActiveGoal()
        viewModelScope.launch {
            habitsRepository.refreshHabits()
        }
    }

    fun addHabit(title: String, description: String, frequency: String, linkToGoal: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            var finalGoalId: String? = null
            if (linkToGoal) {
                // 1. Try memory
                finalGoalId = _uiState.value.activeGoal?.id
                
                // 2. Try priority fetch if missing
                if (finalGoalId == null) {
                    try {
                        val freshGoal = goalsRepository.getActiveGoal()
                        finalGoalId = freshGoal?.id
                        if (freshGoal != null) {
                            _uiState.update { it.copy(activeGoal = freshGoal) }
                        }
                    } catch (_: Exception) {}
                }
                
                // 3. Final safety check: if we STILL have no ID but user requested linking,
                // we MUST NOT proceed with goalId=null as it will land in Standalone.
                if (finalGoalId == null) {
                    _uiState.update { it.copy(
                        isLoading = false, 
                        errorMessage = "Still syncronizing your goal details. Please try again in a moment."
                    ) }
                    return@launch
                }
            }

            val habit = HabitEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                frequency = frequency.lowercase(),
                userId = userId,
                goalId = finalGoalId,
                completed = false,
                syncStatus = SyncStatus.SYNCED
            )
            
            habitsRepository.createHabit(habit, finalGoalId).onSuccess { createdHabit ->
                _uiState.update { it.copy(isLoading = false) }
                if (!createdHabit.relevanceWarning.isNullOrEmpty()) {
                    _uiState.update { it.copy(relevanceWarning = createdHabit.relevanceWarning) }
                }
            }.onFailure { e ->
                val message = if (e is retrofit2.HttpException) {
                    com.habitflowai.util.extractErrorMessage(e)
                } else {
                    "Couldn't reach the server — your habit will be added once you're back online."
                }
                _uiState.update { it.copy(errorMessage = message, isLoading = false) }
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            val current = _uiState.value.habits.find { it.id == habitId } ?: return@launch
            habitsRepository.deleteHabit(current)
        }
    }

    fun completeHabit(habitId: String, note: String? = null, isPublic: Boolean = true, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val habit = _uiState.value.habits.find { it.id == habitId }
            if (habit == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Habit not found locally") }
                onResult(false)
                return@launch
            }
            
            val success = habitsRepository.completeHabit(habit, note)
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, relevanceWarning = null) }
    }

    fun clearRelevanceWarning() {
        _uiState.update { it.copy(relevanceWarning = null) }
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
                if (goal != null) {
                    _uiState.update { it.copy(activeGoal = goal, onboardingGoal = null) }
                } else {
                    // Formal goal missing, fallback to profile goal title
                    val homeData = goalsRepository.getHomeData()
                    _uiState.update { it.copy(onboardingGoal = homeData.goal, activeGoal = null) }
                }
            } catch (e: Exception) {
                // Best effort fallback
                try {
                    val homeData = goalsRepository.getHomeData()
                    _uiState.update { it.copy(onboardingGoal = homeData.goal) }
                } catch (_: Exception) {}
            }
            refreshSuggestionsIfNeeded()
        }
    }

    private fun refreshSuggestionsIfNeeded() {
        val currentGoalId = _uiState.value.activeGoal?.id
        if (currentGoalId == suggestionSourceGoalId) {
            applySuggestionFilter()
            return
        }
        viewModelScope.launch {
            try {
                val homeData = goalsRepository.getHomeData()
                suggestionSource = homeData.coreGoals
                suggestionSourceGoalId = currentGoalId
            } catch (_: Exception) {}
            applySuggestionFilter()
        }
    }

    private fun applySuggestionFilter() {
        val suggestions = suggestionSource.filter { task ->
            // Only suggest if not already added as a habit
            _uiState.value.habits.none { it.title.contains(task.description, ignoreCase = true) }
        }
        _uiState.update { it.copy(suggestions = suggestions) }
    }
}
