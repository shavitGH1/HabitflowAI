package com.habitflowai.data.repository

import android.content.Context
import android.net.Uri
import com.habitflowai.data.model.AddMembersRequest
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.CreateChatRequest
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.RemoveMembersRequest
import com.habitflowai.data.model.UpdateAdminsRequest
import com.habitflowai.data.model.UpdateGroupDescriptionRequest
import com.habitflowai.data.model.UpdateGroupNameRequest
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.data.local.ChatLocalStorage
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.SocialRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val api: HabitFlowApi,
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val chatLocalStorage: ChatLocalStorage
) : SocialRepository {

    private val myId get() = authManager.currentUserId.value ?: "me"

    private val mockAuthors = listOf("Alex", "Mia", "Sam", "Jordan", "Taylor", "Casey")
    private val mockContents = listOf(
        "Just completely crushed my deep work block! 🚀",
        "Woke up at 5am today. The sunrise was totally worth it.",
        "Hit a 10 day streak on fitness goals! Consistency pays off.",
        "Reflecting on my progress this week. Feeling grateful. 🙏",
        "New personal record in the gym today! Hard work works.",
        "Anyone else struggling with consistent meditation? Let's stay motivated!"
    )

    override fun getPosts(page: Int, pageSize: Int): Flow<List<Post>> = flow {
        // Simulate network delay
        delay(1000)
        
        val posts = List(pageSize) { index ->
            val globalIndex = page * pageSize + index
            Post(
                id = globalIndex,
                author = mockAuthors[globalIndex % mockAuthors.size],
                content = mockContents[globalIndex % mockContents.size],
                hasPhoto = globalIndex % 2 == 0,
                likeCount = (0..50).random(),
                commentCount = (0..10).random(),
                isLiked = false
            )
        }
        emit(posts)
    }

    override fun getComments(postId: Int): Flow<List<Comment>> = flow {
        delay(500)
        val comments = List((1..5).random()) { index ->
            Comment(
                id = index + (postId * 100),
                postId = postId,
                author = mockAuthors[index % mockAuthors.size],
                content = "Great job! Keep it up. 👍"
            )
        }
        emit(comments)
    }

    override suspend fun getAllChats(): List<ChatResponse> {
        return try {
            val chats = api.getChats()
            val groups = chats.filter { it.isGroup }
            val dms = chats.filter { !it.isGroup }
            try {
                chatLocalStorage.saveGroupChats(myId, groups)
                chatLocalStorage.saveDirectChats(myId, dms)
            } catch (_: Exception) {}
            chats
        } catch (e: Exception) {
            try {
                val cachedGroups = chatLocalStorage.loadGroupChats(myId)
                val cachedDms = chatLocalStorage.loadDirectChats(myId)
                if (cachedGroups.isNotEmpty() || cachedDms.isNotEmpty()) {
                    cachedGroups + cachedDms
                } else {
                    // Hardcoded fallback for testing/demo if cache is empty and network fails
                    listOf(
                        ChatResponse("1", "Marathon Crew", true, "Let's run!", participantIds = listOf(myId, "alex_id", "mia_id")),
                        ChatResponse("2", "Meditators", true, "Zen mode on", participantIds = listOf(myId, "sam_id")),
                        ChatResponse("dm_alex", "Alex", false, "Hey there!", participantIds = listOf(myId, "alex_id"))
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getGroupChats(): List<ChatResponse> {
        return try {
            val chats = api.getChats().filter { it.isGroup }
            // Persist to local cache keyed by userId
            try { chatLocalStorage.saveGroupChats(myId, chats) } catch (_: Exception) {}
            chats
        } catch (e: Exception) {
            // Return cached data if available, otherwise fall back to mock
            try {
                val cached = chatLocalStorage.loadGroupChats(myId)
                if (cached.isNotEmpty()) cached
                else listOf(
                    ChatResponse("1", "Marathon Crew", true, "Let's run!"),
                    ChatResponse("2", "Meditators", true, "Zen mode on"),
                    ChatResponse("3", "Deep Work Squad", true, "Focused session starting")
                )
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun createGroup(name: String, participantIds: List<String>): ChatResponse {
        return try {
            val chat = api.createChat(CreateChatRequest(name = name, participantIds = participantIds))
            // Append to cached groups
            try {
                val updated = chatLocalStorage.loadGroupChats(myId) + chat
                chatLocalStorage.saveGroupChats(myId, updated)
            } catch (_: Exception) {}
            chat
        } catch (e: Exception) {
            val chat = ChatResponse(
                id = (10..9999).random().toString(),
                name = name,
                isGroup = true,
                lastMessage = null,
                participantIds = participantIds,
                admins = listOf(myId),
                owner = myId
            )
            try {
                val updated = chatLocalStorage.loadGroupChats(myId) + chat
                chatLocalStorage.saveGroupChats(myId, updated)
            } catch (_: Exception) {}
            chat
        }
    }

    override suspend fun uploadGroupImage(chatId: String, imageUri: Uri): ChatResponse {
        return try {
            val updatedChat = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw java.io.IOException("Cannot open input stream for URI: $imageUri")
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", "group_photo_${System.currentTimeMillis()}.jpg", requestBody)
                
                api.uploadGroupImage(chatId, part)
            }
            updatedChat
        } catch (e: Exception) {
            // If upload fails, return the existing chat object but optimistically include the local URI
            // so the UI can at least show the photo during the current session.
            val currentChats = try { chatLocalStorage.loadGroupChats(myId) } catch (_: Exception) { emptyList() }
            val chat = currentChats.find { it.id == chatId } 
                ?: ChatResponse(chatId, "Group", true, null)
            
            chat.copy(imageUrl = imageUri.toString())
        }
    }

    override suspend fun joinGroup(chatId: String): Boolean {
        return try {
            delay(500)
            true
        } catch (_: Exception) {
            true
        }
    }

    override suspend fun addMember(chatId: String, userId: String): Boolean {
        return try {
            api.addMembers(chatId, AddMembersRequest(listOf(userId)))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getMessages(chatId: String): List<com.habitflowai.data.model.ChatMessage> {
        return try {
            val messages = api.getMessages(chatId).map { response ->
                com.habitflowai.data.model.ChatMessage(
                    id = response.id,
                    text = response.text ?: "",
                    senderId = response.senderId,
                    isFromBot = response.senderId == "bot",
                    timestamp = System.currentTimeMillis(),
                    likedBy = response.likes ?: emptyList(),
                    imageUrl = response.imageUrl
                )
            }
            if (messages.isNotEmpty()) {
                try { chatLocalStorage.saveMessages(chatId, messages) } catch (_: Exception) {}
            }
            messages
        } catch (e: Exception) {
            // Return cached messages — preserves offline view and survives restarts
            try { chatLocalStorage.loadMessages(chatId) } catch (_: Exception) { emptyList() }
        }
    }

    override suspend fun toggleMessageLike(chatId: String, messageId: String): com.habitflowai.data.model.ChatMessage? {
        return try {
            val response = api.toggleMessageLike(chatId, messageId)
            com.habitflowai.data.model.ChatMessage(
                id = response.id,
                text = response.text ?: "",
                senderId = response.senderId,
                isFromBot = response.senderId == "bot",
                timestamp = System.currentTimeMillis(),
                likedBy = response.likes ?: emptyList(),
                imageUrl = response.imageUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun markAsRead(chatId: String) {
        try { api.markAsRead(chatId) } catch (_: Exception) {}
    }

    override suspend fun removeMember(chatId: String, userId: String): Boolean {
        return try {
            api.removeMembers(chatId, RemoveMembersRequest(listOf(userId)))
            true
        } catch (e: Exception) { false }
    }

    override suspend fun leaveGroup(chatId: String): Boolean {
        return try {
            api.leaveGroup(chatId)
            try { chatLocalStorage.deleteChat(chatId) } catch (_: Exception) {}
            true
        } catch (e: Exception) { false }
    }

    override suspend fun renameGroup(chatId: String, name: String): ChatResponse =
        api.renameGroup(chatId, UpdateGroupNameRequest(name))

    override suspend fun updateGroupDescription(chatId: String, description: String): ChatResponse =
        api.updateGroupDescription(chatId, UpdateGroupDescriptionRequest(description))

    override suspend fun promoteAdmin(chatId: String, userId: String): ChatResponse =
        api.promoteAdmin(chatId, UpdateAdminsRequest(listOf(userId)))

    override suspend fun demoteAdmin(chatId: String, userId: String): ChatResponse =
        api.demoteAdmin(chatId, UpdateAdminsRequest(listOf(userId)))

    override suspend fun deleteGroup(chatId: String): Boolean {
        return try {
            api.deleteGroup(chatId)
            try { chatLocalStorage.deleteChat(chatId) } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            // Still remove from local cache even if API fails
            try { chatLocalStorage.deleteChat(chatId) } catch (_: Exception) {}
            false
        }
    }

    override suspend fun getDirectChats(): List<ChatResponse> {
        return try {
            val chats = api.getChats().filter { !it.isGroup }
            try { chatLocalStorage.saveDirectChats(myId, chats) } catch (_: Exception) {}
            chats
        } catch (_: Exception) {
            try { chatLocalStorage.loadDirectChats(myId) } catch (_: Exception) { emptyList() }
        }
    }

    override suspend fun createDirectChat(userId: String): ChatResponse {
        return try {
            val chat = api.createChat(CreateChatRequest(name = userId, participantIds = listOf(userId), isGroup = false))
            try {
                val updated = chatLocalStorage.loadDirectChats(myId) + chat
                chatLocalStorage.saveDirectChats(myId, updated)
            } catch (_: Exception) {}
            chat
        } catch (_: Exception) {
            val chat = ChatResponse(
                id = (10000..99999).random().toString(),
                name = userId,
                isGroup = false,
                lastMessage = null
            )
            try {
                val updated = chatLocalStorage.loadDirectChats(myId) + chat
                chatLocalStorage.saveDirectChats(myId, updated)
            } catch (_: Exception) {}
            chat
        }
    }

    override suspend fun getFollowing(userId: String): List<String> {
        return try {
            api.getFollowing(userId)
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun getAllUsers(): List<com.habitflowai.data.model.AppUser> {
        val testUsers = listOf(
            com.habitflowai.data.model.AppUser("alex_id", "alex_pro@habitflow.ai"),
            com.habitflowai.data.model.AppUser("mia_id", "mia_zen@habitflow.ai"),
            com.habitflowai.data.model.AppUser("sam_id", "sam_fit@habitflow.ai"),
            com.habitflowai.data.model.AppUser("jordan_id", "jordan_dev@habitflow.ai"),
            com.habitflowai.data.model.AppUser("taylor_id", "taylor_coach@habitflow.ai"),
            com.habitflowai.data.model.AppUser("casey_id", "casey_explorer@habitflow.ai"),
            com.habitflowai.data.model.AppUser("habit_bot", "habit_bot@habitflow.ai"),
            com.habitflowai.data.model.AppUser("growth_guru", "growth_guru@habitflow.ai"),
            com.habitflowai.data.model.AppUser("marathon_man", "marathon_man@fitness.com"),
            com.habitflowai.data.model.AppUser("yoga_girl", "yoga_girl@peace.io"),
            com.habitflowai.data.model.AppUser("code_master", "code_master@tech.com")
        )
        return try {
            val remoteUsers = api.getUsers()
            if (remoteUsers.isEmpty()) testUsers else (testUsers + remoteUsers).distinctBy { it.id }
        } catch (e: Exception) {
            testUsers
        }
    }
}
