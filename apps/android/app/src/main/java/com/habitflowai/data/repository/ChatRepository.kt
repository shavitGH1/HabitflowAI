package com.habitflowai.data.repository

import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.network.HabitFlowApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: HabitFlowApi
) {
    private val _messages = MutableSharedFlow<ChatMessage>()
    val messages: Flow<ChatMessage> = _messages

    suspend fun getCoachChatId(): String? {
        return try {
            api.getCoachChat().chatId
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getHistory(chatId: String): List<ChatMessage> {
        return try {
            api.getMessages(chatId).map { 
                ChatMessage(
                    id = it.id,
                    text = it.text ?: "",
                    senderId = it.senderId,
                    isFromBot = it.senderId == "bot" // Simplification
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
