package com.habitflowai.data.repository

import com.habitflowai.data.model.ChatResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight, app-wide count of chats with at least one unread message — backs the
 * bottom-nav Social badge. Deliberately not a live-socket-connected source: only
 * SocialViewModel updates this, whenever it loads or refreshes chats (already happens
 * today), so no extra network/socket activity is added just for the badge. This means
 * the badge has no data until the user has opened Social at least once this session —
 * an accepted tradeoff, not a bug, given the alternative is keeping the full chat/socket
 * ViewModel alive for the entire app session.
 */
@Singleton
class UnreadChatsTracker @Inject constructor() {
    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    fun update(chats: List<ChatResponse>, currentUserId: String) {
        _unreadChatCount.value = chats.count { (it.unreadCount[currentUserId] ?: 0) > 0 }
    }
}
