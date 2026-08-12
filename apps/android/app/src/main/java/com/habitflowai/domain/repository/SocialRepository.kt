package com.habitflowai.domain.repository

import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.ChatResponse
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(page: Int, pageSize: Int): Flow<List<Post>>
    fun getComments(postId: Int): Flow<List<Comment>>
    suspend fun getAllChats(): List<ChatResponse>
    suspend fun getGroupChats(): List<ChatResponse>
    suspend fun createGroup(name: String, participantIds: List<String> = emptyList()): ChatResponse
    suspend fun uploadGroupImage(chatId: String, imageUri: android.net.Uri): ChatResponse
    suspend fun joinGroup(chatId: String): Boolean
    suspend fun addMember(chatId: String, userId: String): Boolean
    suspend fun removeMember(chatId: String, userId: String): Boolean
    suspend fun leaveGroup(chatId: String): Boolean
    suspend fun getMessages(chatId: String): List<com.habitflowai.data.model.ChatMessage>
    suspend fun toggleMessageLike(chatId: String, messageId: String): com.habitflowai.data.model.ChatMessage?
    suspend fun markAsRead(chatId: String)
    suspend fun renameGroup(chatId: String, name: String): ChatResponse
    suspend fun updateGroupDescription(chatId: String, description: String): ChatResponse
    suspend fun promoteAdmin(chatId: String, userId: String): ChatResponse
    suspend fun demoteAdmin(chatId: String, userId: String): ChatResponse
    suspend fun deleteGroup(chatId: String): Boolean
    suspend fun getDirectChats(): List<ChatResponse>
    suspend fun createDirectChat(userId: String): ChatResponse
    suspend fun getFollowing(userId: String): List<String>
    suspend fun getAllUsers(): List<com.habitflowai.data.model.AppUser>
}
