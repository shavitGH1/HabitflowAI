package com.habitflowai.domain.repository

import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.ChatResponse
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(page: Int, pageSize: Int): Flow<List<Post>>
    fun getComments(postId: Int): Flow<List<Comment>>
    suspend fun getGroupChats(): List<ChatResponse>
    suspend fun createGroup(name: String): ChatResponse
    suspend fun joinGroup(chatId: String): Boolean
    suspend fun addMember(chatId: String, userId: String): Boolean
    suspend fun getMessages(chatId: String): List<com.habitflowai.data.model.ChatMessage>
    suspend fun toggleMessageLike(chatId: String, messageId: String): com.habitflowai.data.model.ChatMessage?
}
