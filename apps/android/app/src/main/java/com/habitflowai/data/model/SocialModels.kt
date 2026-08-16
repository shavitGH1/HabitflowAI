package com.habitflowai.data.model

import com.google.gson.annotations.JsonAdapter

data class Post(
    val id: String,
    val authorId: String,
    val authorEmail: String? = null,
    val authorName: String? = null,
    @JsonAdapter(ProfilePictureDeserializer::class)
    val authorProfilePicture: String? = null,
    val habitName: String,
    val completionNote: String,
    @JsonAdapter(ImageUrlDeserializer::class)
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
    @JsonAdapter(ProfilePictureDeserializer::class)
    val userProfilePicture: String? = null,
    val text: String,
    val createdAt: String? = null,
    val likes: List<String> = emptyList(),
    // UI-only properties for convenience
    val isLiked: Boolean = false,
    val likeCount: Int = 0
)

data class PostRequest(
    val habitName: String,
    val completionNote: String
)

data class CommentRequest(
    val text: String
)
