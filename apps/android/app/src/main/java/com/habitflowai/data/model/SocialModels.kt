package com.habitflowai.data.model

data class Post(
    val id: String,
    val authorId: String,
    val authorEmail: String? = null,
    val authorName: String? = null,
    val habitName: String,
    val completionNote: String,
    val imageUrl: String? = null,
    val likes: List<String> = emptyList(),
    val createdAt: String? = null,
    // UI-only properties for convenience
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val likeCount: Int = 0
)

data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val text: String,
    val createdAt: String? = null
)

data class PostRequest(
    val habitName: String,
    val completionNote: String
)

data class CommentRequest(
    val text: String
)
