package com.habitflowai.data.repository

import com.habitflowai.data.model.AddMembersRequest
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.CreateChatRequest
import com.habitflowai.data.model.Post
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.domain.repository.SocialRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val api: HabitFlowApi
) : SocialRepository {

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

    override suspend fun getGroupChats(): List<ChatResponse> {
        return try {
            api.getChats().filter { it.isGroup }
        } catch (e: Exception) {
            // Fallback mock if API fails for now
            listOf(
                ChatResponse("1", "Marathon Crew", true, "Let's run!"),
                ChatResponse("2", "Meditators", true, "Zen mode on"),
                ChatResponse("3", "Deep Work Squad", true, "Focused session starting")
            )
        }
    }

    override suspend fun createGroup(name: String): ChatResponse {
        return try {
            api.createChat(CreateChatRequest(name = name, participantIds = emptyList()))
        } catch (e: Exception) {
            ChatResponse(
                id = (10..1000).random().toString(),
                name = name,
                isGroup = true,
                lastMessage = "Group created (Local Fallback)"
            )
        }
    }

    override suspend fun joinGroup(chatId: String): Boolean {
        return try {
            // Backend join logic might be different, but for now we reuse addMember or similar
            // Assuming join is just adding oneself. For now let's keep it simple.
            delay(500)
            true
        } catch (e: Exception) {
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
            api.getMessages(chatId).map { response ->
                com.habitflowai.data.model.ChatMessage(
                    id = response.id,
                    text = response.text ?: "",
                    senderId = response.senderId,
                    isFromBot = response.senderId == "bot",
                    timestamp = System.currentTimeMillis(), // TODO: Parse response.createdAt ISO string
                    likedBy = response.likes ?: emptyList()
                )
            }
        } catch (e: Exception) {
            emptyList()
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
                likedBy = response.likes ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }
}
