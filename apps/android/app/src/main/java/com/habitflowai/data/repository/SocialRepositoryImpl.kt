package com.habitflowai.data.repository

import android.content.Context
import android.net.Uri
import com.habitflowai.data.model.AddMembersRequest
import com.habitflowai.data.model.AppUser
import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.CreateChatRequest
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.RemoveMembersRequest
import com.habitflowai.data.model.UpdateAdminsRequest
import com.habitflowai.data.model.UpdateGroupDescriptionRequest
import com.habitflowai.data.model.UpdateGroupVisibilityRequest
import com.habitflowai.data.model.UpdateGroupNameRequest
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.data.local.ChatLocalStorage
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.SocialRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val cachedPosts = MutableStateFlow<List<Post>>(emptyList())

    override fun getPosts(page: Int, pageSize: Int): Flow<List<Post>> = flow {
        try {
            val posts = api.getPosts(page + 1, pageSize).map { p ->
                p.copy(
                    isLiked = p.likes.contains(myId),
                    likeCount = p.likes.size
                )
            }
            if (page == 0) cachedPosts.value = posts
            else cachedPosts.value = (cachedPosts.value + posts).distinctBy { it.id }
            emit(posts)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getPostsByUserId(userId: String): Flow<List<Post>> = flow {
        // First emit from cache if available
        val fromCache = cachedPosts.value.filter { it.authorId == userId || it.authorEmail == userId }
        if (fromCache.isNotEmpty()) emit(fromCache)

        try {
            val posts = api.getPostsByUserId(userId).map { p ->
                p.copy(
                    isLiked = p.likes.contains(myId),
                    likeCount = p.likes.size
                )
            }
            emit(posts)
        } catch (e: Exception) {
            // If specific endpoint fails and we already emitted cache, don't emit empty
            if (fromCache.isEmpty()) emit(emptyList())
        }
    }

    override fun getComments(postId: String): Flow<List<Comment>> = flow {
        try {
            val comments = api.getComments(postId)
            emit(comments)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun createPost(habitName: String, completionNote: String, imageUri: Uri?): Post? {
        return try {
            val hName = habitName.toRequestBody("text/plain".toMediaTypeOrNull())
            val cNote = completionNote.toRequestBody("text/plain".toMediaTypeOrNull())
            
            var imagePart: MultipartBody.Part? = null
            if (imageUri != null) {
                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("image", "post_${System.currentTimeMillis()}.jpg", requestBody)
                }
            }
            
            api.createPost(hName, cNote, imagePart).let { p ->
                p.copy(isLiked = p.likes.contains(myId), likeCount = p.likes.size)
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun likePost(postId: String): Boolean {
        return try {
            api.togglePostLike(postId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unlikePost(postId: String): Boolean {
        return try {
            api.unlikePost(postId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addComment(postId: String, content: String): Comment? {
        return try {
            api.addComment(postId, com.habitflowai.data.model.CommentRequest(content))
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getAllChats(): List<ChatResponse> {
        val cachedGroups = try { chatLocalStorage.loadGroupChats(myId) } catch (_: Exception) { emptyList() }
        val cachedDms = try { chatLocalStorage.loadDirectChats(myId) } catch (_: Exception) { emptyList() }
        val initialResult = cachedGroups + cachedDms

        return try {
            val serverChats = api.getChats()
            val groups = serverChats.filter { it.isGroup }
            val dms = serverChats.filter { !it.isGroup }
            try {
                chatLocalStorage.saveGroupChats(myId, groups)
                chatLocalStorage.saveDirectChats(myId, dms)
            } catch (_: Exception) {}
            
            // Merge: Prefer server data, but keep local-only chats (e.g. optimistic creations)
            val serverIds = serverChats.map { it.id }.toSet()
            val localOnly = initialResult.filter { it.id !in serverIds }
            serverChats + localOnly
        } catch (e: Exception) {
            initialResult
        }
    }

    override suspend fun getGroupChats(): List<ChatResponse> {
        return try {
            val chats = api.getChats().filter { it.isGroup }
            // Persist to local cache keyed by userId
            try { chatLocalStorage.saveGroupChats(myId, chats) } catch (_: Exception) {}
            chats
        } catch (e: Exception) {
            // Return cached data if available
            try {
                chatLocalStorage.loadGroupChats(myId)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun createGroup(name: String, participantIds: List<String>, isPublic: Boolean): ChatResponse {
        val tempId = (10..9999).random().toString()
        val tempChat = ChatResponse(
            id = tempId,
            name = name,
            isGroup = true,
            lastMessage = null,
            participantIds = participantIds + myId,
            admins = listOf(myId),
            owner = myId,
            isPublic = isPublic
        )
        
        // Optimistically save to local cache before network
        try {
            chatLocalStorage.saveGroupChats(myId, listOf(tempChat))
        } catch (_: Exception) {}

        return try {
            val chat = api.createChat(CreateChatRequest(name = name, participantIds = participantIds, isPublic = isPublic))
            // Replace temp with real server data
            try {
                chatLocalStorage.deleteChat(tempId)
                chatLocalStorage.saveGroupChats(myId, listOf(chat))
            } catch (_: Exception) {}
            chat
        } catch (e: Exception) {
            tempChat
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
            api.addMembers(chatId, AddMembersRequest(listOf(myId)))
            true
        } catch (e: Exception) {
            false
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

    override suspend fun getMessages(chatId: String): List<ChatMessage> {
        return try {
            val messages = api.getMessages(chatId).map { response ->
                ChatMessage(
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

    override suspend fun toggleMessageLike(chatId: String, messageId: String): ChatMessage? {
        return try {
            val response = api.toggleMessageLike(chatId, messageId)
            ChatMessage(
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

    override suspend fun updateGroupVisibility(chatId: String, isPublic: Boolean): ChatResponse =
        api.updateGroupVisibility(chatId, UpdateGroupVisibilityRequest(isPublic))

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
        val tempId = (10000..99999).random().toString()
        val tempChat = ChatResponse(
            id = tempId,
            name = userId,
            isGroup = false,
            lastMessage = null,
            participantIds = listOf(myId, userId)
        )

        // Optimistically save to local cache
        try {
            chatLocalStorage.saveDirectChats(myId, listOf(tempChat))
        } catch (_: Exception) {}

        return try {
            val chat = api.createChat(CreateChatRequest(name = userId, participantIds = listOf(userId), isGroup = false))
            try {
                chatLocalStorage.deleteChat(tempId)
                chatLocalStorage.saveDirectChats(myId, listOf(chat))
            } catch (_: Exception) {}
            chat
        } catch (_: Exception) {
            tempChat
        }
    }

    override suspend fun getFollowing(userId: String): List<String> {
        return try {
            api.getFollowing(userId)
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun getAllUsers(): List<AppUser> {
        return try {
            api.getUsers()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
