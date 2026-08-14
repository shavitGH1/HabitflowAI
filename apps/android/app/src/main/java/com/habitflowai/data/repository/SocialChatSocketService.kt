package com.habitflowai.data.repository

import com.habitflowai.BuildConfig
import com.habitflowai.di.AuthManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

sealed class SocialChatSocketEvent {
    data class NewMessage(
        val chatId: String,
        val messageId: String,
        val text: String,
        val senderId: String,
        val imageUrl: String?
    ) : SocialChatSocketEvent()

    data class TypingChanged(val chatId: String, val userId: String, val isTyping: Boolean) : SocialChatSocketEvent()
    data class MessageLiked(val chatId: String, val messageId: String, val likes: List<String>) : SocialChatSocketEvent()
    data class MemberAdded(val chatId: String) : SocialChatSocketEvent()
    data class MemberRemoved(val chatId: String) : SocialChatSocketEvent()
    data class MemberLeft(val chatId: String) : SocialChatSocketEvent()
    data class GroupRenamed(val chatId: String, val name: String) : SocialChatSocketEvent()
    data class GroupUpdated(
        val chatId: String,
        val description: String? = null,
        val isPublic: Boolean? = null,
        val imageUrl: String? = null
    ) : SocialChatSocketEvent()
    data class AdminAdded(val chatId: String) : SocialChatSocketEvent()
    data class AdminRemoved(val chatId: String) : SocialChatSocketEvent()
    data class GroupDeleted(val chatId: String) : SocialChatSocketEvent()
}

/**
 * Shared Socket.IO client for the Social tab's group/DM chats. Event names and
 * payload shapes mirror the backend exactly (chat.gateway.ts + chat.controller.ts's
 * emitToRoom/emitToUser calls).
 */
@Singleton
class SocialChatSocketService @Inject constructor(
    private val authManager: AuthManager
) {
    private var socket: Socket? = null

    // 'typing' events carry no chatId in their payload (only the room they're
    // broadcast to does), so the currently-joined chat is stamped on client-side.
    private var activeChatId: String? = null

    private val _events = MutableSharedFlow<SocialChatSocketEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SocialChatSocketEvent> = _events.asSharedFlow()

    fun connect() {
        socket?.let { it.off(); it.disconnect() }
        val token = authManager.accessToken.value ?: return
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to "Bearer $token"))
            .build()

        try {
            val client = IO.socket(BuildConfig.BASE_URL, options)

            client.on("newMessage") { args ->
                (args.getOrNull(0) as? JSONObject)?.let { data ->
                    _events.tryEmit(
                        SocialChatSocketEvent.NewMessage(
                            chatId = data.optString("chatId"),
                            messageId = data.optString("id"),
                            text = data.optString("text"),
                            senderId = data.optString("senderId"),
                            imageUrl = data.optString("imageUrl").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            client.on("typing") { args ->
                val data = args.getOrNull(0) as? JSONObject
                val chatId = activeChatId
                if (data != null && chatId != null) {
                    _events.tryEmit(
                        SocialChatSocketEvent.TypingChanged(
                            chatId = chatId,
                            userId = data.optString("userId"),
                            isTyping = data.optBoolean("isTyping")
                        )
                    )
                }
            }
            client.on("messageLiked") { args ->
                (args.getOrNull(0) as? JSONObject)?.let { data ->
                    val likes = data.optJSONArray("likes")
                    _events.tryEmit(
                        SocialChatSocketEvent.MessageLiked(
                            chatId = data.optString("chatId"),
                            messageId = data.optString("messageId"),
                            likes = likes?.let { arr -> List(arr.length()) { i -> arr.getString(i) } } ?: emptyList()
                        )
                    )
                }
            }
            client.on("memberAdded") { args ->
                (args.getOrNull(0) as? JSONObject)?.let {
                    _events.tryEmit(SocialChatSocketEvent.MemberAdded(it.optString("chatId")))
                }
            }
            client.on("memberRemoved") { args ->
                (args.getOrNull(0) as? JSONObject)?.let {
                    _events.tryEmit(SocialChatSocketEvent.MemberRemoved(it.optString("chatId")))
                }
            }
            client.on("memberLeft") { args ->
                (args.getOrNull(0) as? JSONObject)?.let {
                    _events.tryEmit(SocialChatSocketEvent.MemberLeft(it.optString("chatId")))
                }
            }
            client.on("groupRenamed") { args ->
                (args.getOrNull(0) as? JSONObject)?.let {
                    _events.tryEmit(SocialChatSocketEvent.GroupRenamed(it.optString("chatId"), it.optString("name")))
                }
            }
            client.on("groupUpdated") { args ->
                (args.getOrNull(0) as? JSONObject)?.let { data ->
                    _events.tryEmit(
                        SocialChatSocketEvent.GroupUpdated(
                            chatId = data.optString("chatId"),
                            description = data.optString("description").takeIf { it.isNotBlank() },
                            isPublic = if (data.has("isPublic")) data.optBoolean("isPublic") else null,
                            imageUrl = data.optString("imageUrl").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            client.on("adminAdded") { args ->
                (args.getOrNull(0) as? JSONObject)?.let {
                    _events.tryEmit(SocialChatSocketEvent.AdminAdded(it.optString("chatId")))
                }
            }
            client.on("adminRemoved") { args ->
                (args.getOrNull(0) as? JSONObject)?.let {
                    _events.tryEmit(SocialChatSocketEvent.AdminRemoved(it.optString("chatId")))
                }
            }
            client.on("groupDeleted") { args ->
                (args.getOrNull(0) as? JSONObject)?.let {
                    _events.tryEmit(SocialChatSocketEvent.GroupDeleted(it.optString("chatId")))
                }
            }

            client.connect()
            socket = client
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        activeChatId = null
    }

    fun joinChat(chatId: String) {
        activeChatId = chatId
        socket?.emit("joinChat", JSONObject().put("chatId", chatId))
    }

    fun leaveChat(chatId: String) {
        if (activeChatId == chatId) activeChatId = null
        socket?.emit("leaveChat", JSONObject().put("chatId", chatId))
    }

    fun sendMessage(chatId: String, text: String) {
        socket?.emit("sendMessage", JSONObject().put("chatId", chatId).put("text", text))
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        socket?.emit("typing", JSONObject().put("chatId", chatId).put("isTyping", isTyping))
    }
}
