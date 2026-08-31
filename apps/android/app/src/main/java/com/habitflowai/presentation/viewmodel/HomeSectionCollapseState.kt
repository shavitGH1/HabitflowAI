package com.habitflowai.presentation.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

// Survives HomeViewModel being recreated on every tab switch, unlike remember{}.
@Singleton
class HomeSectionCollapseState @Inject constructor() {
    private val _collapsedKeys = MutableStateFlow<Set<String>>(emptySet())
    val collapsedKeys: StateFlow<Set<String>> = _collapsedKeys.asStateFlow()

    fun toggle(key: String) {
        _collapsedKeys.update { current ->
            if (key in current) current - key else current + key
        }
    }
}
