package com.habitflowai.data.local

import com.habitflowai.data.local.dao.ChatDao
import com.habitflowai.data.local.entity.ChatEntity
import com.habitflowai.data.local.entity.MessageEntity
import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.model.ChatResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed cache for chats/messages, scoped per user so history survives
 * logout/login (SSOT: Room is the source of truth for offline/cached reads).
 */
@Singleton
class ChatLocalStorage @Inject constructor(
    private val chatDao: ChatDao
) {
    suspend fun saveGroupChats(userId: String, chats: List<ChatResponse>) {
        chatDao.insertChats(chats.map { it.toEntity(userId) })
    }

    suspend fun loadGroupChats(userId: String): List<ChatResponse> =
        chatDao.getChats(userId).filter { it.isGroup }.map { it.toResponse() }

    suspend fun saveDirectChats(userId: String, chats: List<ChatResponse>) {
        chatDao.insertChats(chats.map { it.toEntity(userId) })
    }

    suspend fun loadDirectChats(userId: String): List<ChatResponse> =
        chatDao.getChats(userId).filter { !it.isGroup }.map { it.toResponse() }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
        chatDao.deleteMessagesForChat(chatId)
    }

    suspend fun saveMessages(chatId: String, messages: List<ChatMessage>) {
        chatDao.insertMessages(messages.map { it.toEntity(chatId) })
    }

    suspend fun loadMessages(chatId: String): List<ChatMessage> =
        chatDao.getMessages(chatId).map { it.toMessage() }
}

private fun ChatResponse.toEntity(userId: String) = ChatEntity(
    id = id,
    userId = userId,
    name = name,
    isGroup = isGroup,
    lastMessage = lastMessage,
    participantIds = participantIds,
    admins = admins,
    owner = owner,
    description = description,
    imageUrl = imageUrl
)

private fun ChatEntity.toResponse() = ChatResponse(
    id = id,
    name = name,
    isGroup = isGroup,
    lastMessage = lastMessage,
    participantIds = participantIds,
    admins = admins,
    owner = owner,
    description = description,
    imageUrl = imageUrl
)

private fun ChatMessage.toEntity(chatId: String) = MessageEntity(
    id = id,
    chatId = chatId,
    text = text,
    senderId = senderId,
    isFromBot = isFromBot,
    timestamp = timestamp,
    imageUrl = imageUrl,
    likedBy = likedBy
)

private fun MessageEntity.toMessage() = ChatMessage(
    id = id,
    text = text,
    senderId = senderId,
    isFromBot = isFromBot,
    timestamp = timestamp,
    likedBy = likedBy,
    imageUrl = imageUrl
)
