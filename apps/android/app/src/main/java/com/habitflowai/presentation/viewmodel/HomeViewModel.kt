package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.HomeResponse
import com.habitflowai.domain.repository.GoalsRepository
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val homeData: HomeResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val portfolioSummary: String? = null,
    val tips: List<String>? = null,
    val failurePatterns: List<String>? = null,
    val confidenceScore: Double? = null,
    val isDriftDetected: Boolean = false,
    val driftRationale: String? = null,
    val isDriftBannerDismissed: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val goalsRepository: GoalsRepository,
    private val locationRepository: LocationRepository
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
                    locationRepository.captureAndSaveLocation(taskId)
                    fetchHomeData() // Refresh to get updated consistency score and history
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
                _uiState.value = _uiState.value.copy(
                    homeData = data,
                    isLoading = false,
                    portfolioSummary = data.portfolioSummary,
                    tips = data.tips,
                    failurePatterns = data.failurePatterns,
                    confidenceScore = data.confidenceScore,
                    isDriftDetected = data.driftDetected,
                    driftRationale = data.driftRationale
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun dismissDriftBanner() {
        _uiState.value = _uiState.value.copy(isDriftBannerDismissed = true)
    }
}
