package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.domain.repository.HabitsRepository
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitsRepository: HabitsRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    private val userId = "local_user"

    init {
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
                locationRepository.captureAndSaveLocation(habit.id, isPublic)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }
}
