package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.local.entity.DailyTaskEntity
import com.habitflowai.data.model.HomeResponse
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.GoalsRepository
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val homeData: HomeResponse? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val portfolioSummary: String? = null,
    val tips: List<String>? = null,
    val failurePatterns: List<String>? = null,
    val confidenceScore: Double? = null,
    val isDriftDetected: Boolean = false,
    val driftRationale: String? = null,
    val isDriftBannerDismissed: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyTasks: List<DailyTaskEntity> = emptyList(),
    val datesWithCompletions: List<String> = emptyList(),
    val collapsedSections: Set<String> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val goalsRepository: GoalsRepository,
    private val locationRepository: LocationRepository,
    private val authManager: AuthManager,
    private val collapseState: HomeSectionCollapseState
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _isDriftBannerDismissed = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _homeData = MutableStateFlow<HomeResponse?>(null)

    private val _datesWithCompletions = authManager.currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList())
        else goalsRepository.getDatesWithCompletions(userId)
    }

    private val _dailyTasks = combine(authManager.currentUserId, _selectedDate) { userId, date ->
        userId to date
    }.flatMapLatest { (userId, date) ->
        if (userId == null) flowOf(emptyList())
        else goalsRepository.getTasksForDate(userId, date.toString())
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        _isDriftBannerDismissed,
        _isLoading,
        _isRefreshing,
        _errorMessage,
        _homeData,
        _datesWithCompletions,
        _dailyTasks,
        collapseState.collapsedKeys
    ) { args: Array<Any?> ->
        val selectedDate = args[0] as LocalDate
        val driftDismissed = args[1] as Boolean
        val loading = args[2] as Boolean
        val refreshing = args[3] as Boolean
        val error = args[4] as? String
        val home = args[5] as? HomeResponse
        val completions = args[6] as List<String>
        val tasks = args[7] as List<DailyTaskEntity>
        val collapsedSections = args[8] as Set<String>

        HomeUiState(
            homeData = home,
            isLoading = loading,
            isRefreshing = refreshing,
            errorMessage = error,
            portfolioSummary = home?.portfolioSummary,
            tips = home?.tips,
            failurePatterns = home?.failurePatterns,
            confidenceScore = home?.confidenceScore,
            isDriftDetected = home?.driftDetected ?: false,
            driftRationale = home?.driftRationale,
            isDriftBannerDismissed = driftDismissed,
            selectedDate = selectedDate,
            dailyTasks = tasks,
            datesWithCompletions = completions,
            collapsedSections = collapsedSections
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        fetchHomeData()
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        if (date == LocalDate.now()) {
            syncTasksIfEmpty()
        } else {
            loadHistoryIfNeeded(date)
        }
    }

    private fun loadHistoryIfNeeded(date: LocalDate) {
        val userId = authManager.currentUserId.value ?: return
        viewModelScope.launch {
            goalsRepository.ensureHistoryLoaded(userId, date.toString())
        }
    }

    private fun syncTasksIfEmpty() {
        val today = LocalDate.now().toString()
        viewModelScope.launch {
            goalsRepository.syncDailyTasks(today)
        }
    }

    fun completeTask(
        taskId: String,
        isCompleted: Boolean = true,
        note: String? = null,
        isPublic: Boolean = true,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val userId = authManager.currentUserId.value ?: return@launch onResult(false)
            try {
                val success = goalsRepository.updateTaskCompletion(userId, taskId, isCompleted, note)
                if (success) {
                    locationRepository.captureAndSaveLocation(taskId, isPublic)
                    fetchHomeData()
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
            _isLoading.value = true
            _errorMessage.value = null
            loadData(force = false)
            _isLoading.value = false
        }
    }

    fun refreshHomeData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            loadData(force = true)
            _isRefreshing.value = false
        }
    }

    private suspend fun loadData(force: Boolean) {
        try {
            // One call to sync handles both DB update and HomeData StateFlow
            val result = goalsRepository.syncDailyTasks(LocalDate.now().toString(), force)
            result.onSuccess { data ->
                _homeData.value = data
            }.onFailure { e ->
                _errorMessage.value = e.message
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    fun dismissDriftBanner() {
        _isDriftBannerDismissed.value = true
    }

    fun toggleSection(key: String) {
        collapseState.toggle(key)
    }
}
