package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.AppUser
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicProfileUiState(
    val userId: String = "",
    val username: String = "",
    val posts: List<Post> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false,
    val isMe: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val authManager: AuthManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private val targetUserId: String = savedStateHandle.get<String>("userId") ?: ""

    init {
        val currentUid = authManager.currentUserId.value ?: "me"
        _uiState.update { it.copy(
            userId = targetUserId,
            isMe = targetUserId == currentUid
        ) }
        loadProfile()
    }

    private fun loadProfile() {
        if (targetUserId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Get user info
            val allUsers = try { repository.getAllUsers() } catch (e: Exception) { emptyList() }
            val user = allUsers.find { it.id == targetUserId }
            var name = user?.email?.substringBefore('@') ?: targetUserId
            
            _uiState.update { it.copy(username = name) }

            // Get posts - try specific endpoint first, fallback to filtering general feed
            repository.getPostsByUserId(targetUserId).collectLatest { posts ->
                if (posts.isNotEmpty()) {
                    // Update name from post if we didn't find it in allUsers
                    if (name == targetUserId) {
                        posts.firstOrNull()?.let { p ->
                            val pName = p.authorName ?: p.authorEmail?.substringBefore('@')
                            if (pName != null) {
                                name = pName
                                _uiState.update { it.copy(username = pName) }
                            }
                        }
                    }
                    _uiState.update { it.copy(posts = posts, isLoading = false) }
                } else {
                    // Fallback: Fetch latest posts and filter locally
                    repository.getPosts(0, 100).collectLatest { allPosts ->
                        val filtered = allPosts.filter { it.authorId == targetUserId }
                        if (name == targetUserId && filtered.isNotEmpty()) {
                            filtered.firstOrNull()?.let { p ->
                                val pName = p.authorName ?: p.authorEmail?.substringBefore('@')
                                if (pName != null) {
                                    _uiState.update { it.copy(username = pName) }
                                }
                            }
                        }
                        _uiState.update { it.copy(posts = filtered, isLoading = false) }
                    }
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        val oldPosts = _uiState.value.posts
        val post = oldPosts.find { it.id == postId } ?: return
        val isLiking = !post.isLiked
        
        val updatedPosts = oldPosts.map { p ->
            if (p.id == postId) {
                p.copy(
                    isLiked = !p.isLiked,
                    likeCount = if (p.isLiked) p.likeCount - 1 else p.likeCount + 1
                )
            } else p
        }
        _uiState.update { it.copy(posts = updatedPosts) }

        viewModelScope.launch {
            val success = if (isLiking) repository.likePost(postId) else repository.unlikePost(postId)
            if (!success) {
                _uiState.update { it.copy(posts = oldPosts) }
            }
        }
    }

    fun startDm(onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val chat = repository.createDirectChat(targetUserId)
                onChatReady(chat.id)
            } catch (_: Exception) {}
        }
    }
}
