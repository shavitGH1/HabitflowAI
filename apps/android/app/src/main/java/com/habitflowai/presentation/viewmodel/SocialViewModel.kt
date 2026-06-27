package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class SocialUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

class SocialViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(1000)
            val initialPosts = listOf(
                Post(1, "Alex", "Just completely crushed my deep work block! 🚀", true, likeCount = 14),
                Post(2, "Mia", "Woke up at 5am today. The sunrise was totally worth it.", false, likeCount = 5),
                Post(3, "Sam", "Hit a 10 day streak on fitness goals! Consistency pays off.", true, likeCount = 22)
            )
            _uiState.value = _uiState.value.copy(posts = initialPosts, isLoading = false)
        }
    }

    fun loadMorePosts() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(1000)
            val currentSize = _uiState.value.posts.size
            val morePosts = (1..5).map { i ->
                Post(
                    id = currentSize + i,
                    author = "User ${currentSize + i}",
                    content = "Keep pushing forward! Personal growth is a marathon. Post #${currentSize + i}",
                    hasPhoto = i % 2 == 0,
                    likeCount = (0..50).random()
                )
            }
            _uiState.value = _uiState.value.copy(
                posts = _uiState.value.posts + morePosts,
                isLoading = false
            )
        }
    }

    fun createPost(content: String, imageUri: String?) {
        val newPost = Post(
            id = (_uiState.value.posts.maxByOrNull { it.id }?.id ?: 0) + 1,
            author = "Me",
            content = content,
            hasPhoto = imageUri != null,
            imageUri = imageUri,
            likeCount = 0
        )
        _uiState.value = _uiState.value.copy(posts = listOf(newPost) + _uiState.value.posts)
    }

    fun toggleLike(postId: Int) {
        val updatedPosts = _uiState.value.posts.map { post ->
            if (post.id == postId) {
                post.copy(
                    isLiked = !post.isLiked,
                    likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                )
            } else post
        }
        _uiState.value = _uiState.value.copy(posts = updatedPosts)
    }
}
