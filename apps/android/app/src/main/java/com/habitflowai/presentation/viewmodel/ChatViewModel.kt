package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.BuildConfig
import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.model.ChatUiState
import com.habitflowai.data.repository.ChatRepository
import com.habitflowai.di.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var socket: Socket? = null
    private var botChatId: String? = null

    init {
        viewModelScope.launch {
            botChatId = repository.getCoachChatId()
            setupSocket()
            loadHistory()
        }
    }

    private fun setupSocket() {
        val token = authManager.accessToken.value ?: return
        val chatId = botChatId ?: return
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to "Bearer $token"))
            .build()

        try {
            socket = IO.socket(BuildConfig.BASE_URL, options)
            socket?.on(Socket.EVENT_CONNECT) {
                socket?.emit("joinChat", JSONObject().put("chatId", chatId))
            }
            socket?.on("newMessage") { args ->
                val data = args[0] as JSONObject
                val message = ChatMessage(
                    id = data.getString("id"),
                    text = data.getString("text"),
                    senderId = data.getString("senderId"),
                    isFromBot = data.getString("senderId") == "bot"
                )
                viewModelScope.launch {
                    _uiState.update { it.copy(messages = it.messages + message, isTyping = false) }
                }
            }
            socket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadHistory() {
        val chatId = botChatId ?: return
        viewModelScope.launch {
            val history = repository.getHistory(chatId)
            if (history.isNotEmpty()) {
                _uiState.update { it.copy(messages = history) }
            } else {
                // Default welcome message if no history
                _uiState.update {
                    it.copy(
                        messages = listOf(
                            ChatMessage(
                                text = "Hi! I'm your HabitFlow assistant. How can I help you today?",
                                senderId = "bot",
                                isFromBot = true
                            )
                        )
                    )
                }
            }
        }
    }

    fun toggleChat() {
        _uiState.update { it.copy(isChatOpen = !it.isChatOpen) }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setPersonaType(type: String) {
        _uiState.update { it.copy(personaType = type) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        val chatId = botChatId ?: return

        val messageObj = JSONObject().apply {
            put("chatId", chatId)
            put("text", text)
        }
        socket?.emit("sendMessage", messageObj)

        val userMessage = ChatMessage(text = text, senderId = "user", isFromBot = false)
        _uiState.update { 
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isTyping = true
            )
        }
    }

    override fun onCleared() {
        socket?.disconnect()
        socket?.off()
        super.onCleared()
    }
}
