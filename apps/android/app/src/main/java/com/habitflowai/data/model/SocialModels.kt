package com.habitflowai.data.model

data class Post(
    val id: Int,
    val author: String,
    val content: String,
    val hasPhoto: Boolean,
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val imageUri: String? = null
)

data class Comment(
    val id: Int,
    val postId: Int,
    val author: String,
    val content: String,
    val timestamp: String = "Just now"
)
