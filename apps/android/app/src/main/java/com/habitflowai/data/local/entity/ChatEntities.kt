package com.habitflowai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats", primaryKeys = ["id", "userId"])
data class ChatEntity(
    val id: String,
    val userId: String, // The user this chat belongs to (for multi-account support)
    val name: String?,
    val isGroup: Boolean,
    val lastMessage: String?,
    val participantIds: List<String>,
    val admins: List<String>,
    val owner: String?,
    val description: String?,
    val imageUrl: String?
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val text: String,
    val senderId: String,
    val isFromBot: Boolean,
    val timestamp: Long,
    val imageUrl: String?,
    val likedBy: List<String>
)
