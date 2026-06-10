package com.habitflowai.data.model

data class Post(
    val id: Int,
    val author: String,
    val content: String,
    val hasPhoto: Boolean,
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val imageUri: String? = null
)
