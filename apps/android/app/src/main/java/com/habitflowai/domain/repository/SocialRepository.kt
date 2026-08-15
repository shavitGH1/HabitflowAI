package com.habitflowai.domain.repository

import com.habitflowai.data.model.AppUser
import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.ChatResponse
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(page: Int, pageSize: Int): Flow<List<Post>>
    fun getComments(postId: String): Flow<List<Comment>>
    suspend fun createPost(habitName: String, completionNote: String, imageUri: android.net.Uri? = null): Post?
    suspend fun likePost(postId: String): Boolean
    suspend fun unlikePost(postId: String): Boolean
    suspend fun addComment(postId: String, content: String): Comment?
    suspend fun getAllChats(): List<ChatResponse>
    suspend fun getGroupChats(): List<ChatResponse>
    suspend fun createGroup(name: String, participantIds: List<String> = emptyList(), isPublic: Boolean = false): ChatResponse
    suspend fun uploadGroupImage(chatId: String, imageUri: android.net.Uri): ChatResponse
    suspend fun joinGroup(chatId: String): Boolean
    suspend fun addMember(chatId: String, userId: String): Boolean
    suspend fun removeMember(chatId: String, userId: String): Boolean
    suspend fun leaveGroup(chatId: String): Boolean
    suspend fun getMessages(chatId: String): List<ChatMessage>
    suspend fun toggleMessageLike(chatId: String, messageId: String): ChatMessage?
    suspend fun markAsRead(chatId: String)
    suspend fun renameGroup(chatId: String, name: String): ChatResponse
    suspend fun updateGroupDescription(chatId: String, description: String): ChatResponse
    suspend fun updateGroupVisibility(chatId: String, isPublic: Boolean): ChatResponse
    suspend fun promoteAdmin(chatId: String, userId: String): ChatResponse
    suspend fun demoteAdmin(chatId: String, userId: String): ChatResponse
    suspend fun deleteGroup(chatId: String): Boolean
    suspend fun getDirectChats(): List<ChatResponse>
    suspend fun createDirectChat(userId: String): ChatResponse
    suspend fun getFollowing(userId: String): List<String>
    suspend fun getAllUsers(): List<AppUser>
}
