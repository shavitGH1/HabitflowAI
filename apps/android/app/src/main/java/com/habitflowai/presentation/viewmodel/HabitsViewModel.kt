package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.Habit
import com.habitflowai.data.model.HabitFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HabitsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        // Load initial mock data
        fetchHabits()
    }

    private fun fetchHabits() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Mock delay
            kotlinx.coroutines.delay(500)
            val mockHabits = listOf(
                Habit(UUID.randomUUID().toString(), "Morning Run", "5km in the park", HabitFrequency.DAILY),
                Habit(UUID.randomUUID().toString(), "Read Book", "20 pages before bed", HabitFrequency.DAILY),
                Habit(UUID.randomUUID().toString(), "Weekly Review", "Plan the next week", HabitFrequency.WEEKLY)
            )
            _uiState.value = _uiState.value.copy(habits = mockHabits, isLoading = false)
        }
    }

    fun addHabit(title: String, description: String, frequency: HabitFrequency) {
        val newHabit = Habit(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            frequency = frequency
        )
        _uiState.value = _uiState.value.copy(habits = _uiState.value.habits + newHabit)
    }

    fun deleteHabit(habitId: String) {
        _uiState.value = _uiState.value.copy(
            habits = _uiState.value.habits.filter { it.id != habitId }
        )
    }
}
