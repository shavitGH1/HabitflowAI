package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.habitflowai.data.repository.UnreadChatsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin wrapper so the nav-graph root (a plain composable, not itself a ViewModel) can
 * observe UnreadChatsTracker's count for the bottom-nav Social badge — same reason
 * ChatViewModel is hoisted there for the coach bubble.
 */
@HiltViewModel
class UnreadBadgeViewModel @Inject constructor(
    tracker: UnreadChatsTracker
) : ViewModel() {
    val unreadChatCount: StateFlow<Int> = tracker.unreadChatCount
}
