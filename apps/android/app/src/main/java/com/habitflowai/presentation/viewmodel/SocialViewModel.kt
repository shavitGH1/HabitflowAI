package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialUiState(
    val posts: List<Post> = emptyList(),
    val comments: Map<Int, List<Comment>> = emptyMap(),
    val groupChats: List<ChatResponse> = emptyList(),
    val chatMessages: Map<String, List<com.habitflowai.data.model.ChatMessage>> = emptyMap(),
    val appUsers: List<String> = emptyList(), // Mock user list
    val isLoading: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isLoadingChats: Boolean = false,
    val isLoadingMessages: Boolean = false,
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

    fun loadGroupChats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            val chats = repository.getGroupChats()
            _uiState.update { it.copy(groupChats = chats, isLoadingChats = false) }
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMessages = true) }
            val messages = repository.getMessages(chatId)
            val updatedMap = _uiState.value.chatMessages.toMutableMap().apply {
                put(chatId, messages)
            }
            _uiState.update { 
                it.copy(
                    chatMessages = updatedMap,
                    isLoadingMessages = false
                )
            }
        }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            val newChat = repository.createGroup(name)
            _uiState.update { 
                it.copy(
                    groupChats = it.groupChats + newChat,
                    isLoadingChats = false
                )
            }
        }
    }

    fun joinGroup(chatId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            val success = repository.joinGroup(chatId)
            if (success) {
                loadGroupChats()
            } else {
                _uiState.update { it.copy(isLoadingChats = false) }
            }
        }
    }

    fun addMember(chatId: String, name: String) {
        viewModelScope.launch {
            repository.addMember(chatId, name)
            // Just a mock success feedback
        }
    }

    fun sendMessage(chatId: String, content: String) {
        val newMessage = com.habitflowai.data.model.ChatMessage(
            text = content,
            senderId = "Me",
            isFromBot = false
        )
        val currentMessages = _uiState.value.chatMessages[chatId] ?: emptyList()
        val updatedMap = _uiState.value.chatMessages.toMutableMap().apply {
            put(chatId, currentMessages + newMessage)
        }
        _uiState.update { it.copy(chatMessages = updatedMap) }
        
        // Also update the last message in the group list
        val updatedGroups = _uiState.value.groupChats.map { 
            if (it.id == chatId) it.copy(lastMessage = content) else it 
        }
        _uiState.update { it.copy(groupChats = updatedGroups) }
    }

    fun toggleMessageLike(chatId: String, messageId: String) {
        viewModelScope.launch {
            val updatedMessage = repository.toggleMessageLike(chatId, messageId) ?: return@launch
            val currentMessages = _uiState.value.chatMessages[chatId] ?: return@launch
            val updatedMessages = currentMessages.map {
                if (it.id == messageId) updatedMessage else it
            }
            val updatedMap = _uiState.value.chatMessages.toMutableMap().apply {
                put(chatId, updatedMessages)
            }
            _uiState.update { it.copy(chatMessages = updatedMap) }
        }
    }

    fun loadUsers() {
        // Mocking a list of users for search
        _uiState.update { 
            it.copy(appUsers = listOf("alex_achiever", "mia_grower", "sam_spirit", "jordan_habit", "taylor_flow", "casey_altruist"))
        }
    }
}
