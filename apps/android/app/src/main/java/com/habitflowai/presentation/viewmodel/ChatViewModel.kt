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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var socket: Socket? = null
    private var botChatId: String? = null

    init {
        // Setup will be triggered manually after login or on first need to avoid loops
    }

    fun initializeChat() {
        if (botChatId != null) return
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
                val senderId = data.getString("senderId")
                val message = ChatMessage(
                    id = data.getString("id"),
                    text = data.getString("text"),
                    senderId = senderId,
                    isFromBot = senderId != authManager.currentUserId.value
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
            val history = repository.getHistory(chatId, authManager.currentUserId.value)
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
        if (botChatId == null) return

        _uiState.update { it.copy(inputText = "", isTyping = true) }

        // The coach chat needs a real AI reply, not just a stored message, so this goes
        // through the coach endpoint instead of the generic chat socket. The backend persists
        // both sides of the conversation and broadcasts them back over the same "newMessage"
        // socket event this screen already listens to, so no local optimistic bubble is needed.
        viewModelScope.launch {
            val result = repository.sendCoachMessage(text)
            if (result == null) {
                _uiState.update { it.copy(isTyping = false) }
            }
        }
    }

    override fun onCleared() {
        socket?.disconnect()
        socket?.off()
        super.onCleared()
    }
}
