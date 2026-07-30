package com.habitflowai.data.repository

import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.domain.repository.SocialRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepositoryImpl @Inject constructor() : SocialRepository {

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
}
