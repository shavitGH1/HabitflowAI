package com.example.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.habitflowai.domain.repository.PersonaRepository

class OnboardingViewModelFactory(
    private val repository: PersonaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) return OnboardingViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

