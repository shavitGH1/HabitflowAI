package com.habitflowai.data.model

import java.util.UUID

data class CoachChatResponse(
    val chatId: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val senderId: String,
    val isFromBot: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val likedBy: List<String> = emptyList(),
    val imageUrl: String? = null
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isChatOpen: Boolean = false,
    val inputText: String = "",
    val isTyping: Boolean = false,
    val personaType: String = "Regulator"
)

data class ChatResponse(
    val id: String,
    val name: String?,
    val isGroup: Boolean,
    val lastMessage: String?,
    val participantIds: List<String> = emptyList(),
    val admins: List<String> = emptyList(),
    val owner: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val unreadCount: Map<String, Int> = emptyMap()
)

data class CreateChatRequest(
    val name: String?,
    val participantIds: List<String>,
    val isGroup: Boolean = true
)

data class AddMembersRequest(val userIds: List<String>)

data class RemoveMembersRequest(val userIds: List<String>)

data class UpdateGroupNameRequest(val name: String)

data class UpdateGroupDescriptionRequest(val description: String)

data class UpdateAdminsRequest(val userIds: List<String>)

data class MessageResponse(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String?,
    val imageUrl: String?,
    val createdAt: String,
    val likes: List<String>? = emptyList()
)

/** Lightweight user record returned by GET /api/v1/users */
data class AppUser(
    val id: String,
    val email: String
)
