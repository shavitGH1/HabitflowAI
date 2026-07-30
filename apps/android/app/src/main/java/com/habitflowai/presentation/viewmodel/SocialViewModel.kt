package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialUiState(
    val posts: List<Post> = emptyList(),
    val comments: Map<Int, List<Comment>> = emptyMap(),
    val isLoading: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = true,
    val page: Int = 0
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val pageSize = 10

    init {
        loadMorePosts()
    }

    fun loadMorePosts() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getPosts(_uiState.value.page, pageSize).collectLatest { newPosts ->
                _uiState.value = _uiState.value.copy(
                    posts = _uiState.value.posts + newPosts,
                    isLoading = false,
                    page = _uiState.value.page + 1,
                    canLoadMore = newPosts.size == pageSize
                )
            }
        }
    }

    fun loadComments(postId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            repository.getComments(postId).collectLatest { comments ->
                val updatedComments = _uiState.value.comments.toMutableMap().apply {
                    put(postId, comments)
                }
                _uiState.value = _uiState.value.copy(
                    comments = updatedComments,
                    isLoadingComments = false
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, page = 0, canLoadMore = true)
            repository.getPosts(0, pageSize).collectLatest { newPosts ->
                _uiState.value = _uiState.value.copy(
                    posts = newPosts,
                    isRefreshing = false,
                    page = 1,
                    canLoadMore = newPosts.size == pageSize
                )
            }
        }
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

    fun addPost(content: String, imageUri: String?) {
        val newPost = Post(
            id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            author = "Me",
            content = content,
            hasPhoto = imageUri != null,
            imageUri = imageUri,
            likeCount = 0,
            isLiked = false
        )
        _uiState.value = _uiState.value.copy(posts = listOf(newPost) + _uiState.value.posts)
    }

    fun addComment(postId: Int, content: String) {
        val newComment = Comment(
            id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            postId = postId,
            author = "Me",
            content = content
        )
        
        val currentComments = _uiState.value.comments[postId] ?: emptyList()
        val updatedCommentsMap = _uiState.value.comments.toMutableMap().apply {
            put(postId, currentComments + newComment)
        }
        
        val updatedPosts = _uiState.value.posts.map { post ->
            if (post.id == postId) {
                post.copy(commentCount = post.commentCount + 1)
            } else post
        }
        
        _uiState.value = _uiState.value.copy(
            comments = updatedCommentsMap,
            posts = updatedPosts
        )
    }
}
