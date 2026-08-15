package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.model.AppUser
import com.habitflowai.data.repository.SocialChatSocketEvent
import com.habitflowai.data.repository.SocialChatSocketService
import com.habitflowai.data.local.ChatLocalStorage
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchResult {
    data class User(val user: AppUser) : SearchResult()
    data class Group(val chat: ChatResponse) : SearchResult()
}

enum class FeedFilter {
    ALL, FRIENDS, MINE
}

data class SocialUiState(
    val posts: List<Post> = emptyList(),
    val comments: Map<String, List<Comment>> = emptyMap(),
    val groupChats: List<ChatResponse> = emptyList(),
    val directChats: List<ChatResponse> = emptyList(),
    val chatMessages: Map<String, List<ChatMessage>> = emptyMap(),
    val typingUsers: Map<String, Set<String>> = emptyMap(),
    val currentUserId: String = "me",
    val allUsers: List<AppUser> = emptyList(), // all app users for member picker
    val searchResults: List<SearchResult> = emptyList(),
    val filter: FeedFilter = FeedFilter.ALL,
    val followingIds: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isLoadingChats: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = true,
    val page: Int = 0,
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val socketService: SocialChatSocketService,
    private val authManager: AuthManager,
    private val chatLocalStorage: ChatLocalStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    val autoOpenChatId = MutableStateFlow<String?>(null)

    private val pageSize = 10
    private var isTaskLoading = false
    private var loadChatsJob: kotlinx.coroutines.Job? = null

    private val currentUserId: String
        get() = authManager.currentUserId.value ?: "me"

    init {
        // Observe chatId from savedStateHandle to support navigation with arguments even if ViewModel is reused
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>("chatId", null).collect { chatId ->
                if (chatId != null) {
                    // Try to find in current UI state first
                    val found = _uiState.value.groupChats.find { it.id == chatId }
                        ?: _uiState.value.directChats.find { it.id == chatId }
                    
                    if (found != null) {
                        autoOpenChatId.value = chatId
                    } else {
                        // Not in current list, try to fetch specifically or refresh all
                        val fetched = repository.getChat(chatId)
                        if (fetched != null) {
                            if (fetched.isGroup) {
                                _uiState.update { it.copy(groupChats = (it.groupChats.filter { it.id != chatId } + fetched)) }
                            } else {
                                _uiState.update { it.copy(directChats = (it.directChats.filter { it.id != chatId } + fetched)) }
                            }
                            autoOpenChatId.value = chatId
                        } else {
                            // Last resort: refresh everything
                            loadAllChats()
                            autoOpenChatId.value = chatId
                        }
                    }
                    // Clear the chatId in savedStateHandle so that navigating with the same ID again triggers a new emission
                    savedStateHandle["chatId"] = null
                }
            }
        }

        // Start socket and load posts once
        socketService.connect()
        collectSocketEvents()
        
        // Use a single launch for all initial sync logic to avoid parallel bursts
        viewModelScope.launch {
            // First load posts
            loadMorePosts()
            
            // Then wait for auth and load chats
            authManager.currentUserId.collect { uid ->
                val oldUid = _uiState.value.currentUserId
                val newUid = uid ?: "me"
                
                if (newUid != oldUid) {
                    _uiState.update { it.copy(currentUserId = newUid) }
                    if (newUid == "me") {
                        socketService.disconnect()
                        _uiState.update { it.copy(
                            groupChats = emptyList(),
                            directChats = emptyList(),
                            chatMessages = emptyMap(),
                            followingIds = emptyList()
                        ) }
                    } else {
                        // Logged in: Reconnect socket and sync data
                        socketService.connect()
                        loadFollowingIds()
                        kotlinx.coroutines.delay(800) // Staggered start
                        loadAllChats()
                    }
                } else if (newUid != "me" && (_uiState.value.groupChats.isEmpty())) {
                    // One-time sync for existing session
                    loadAllChats()
                }
            }
        }
    }

    // ─── Posts ───────────────────────────────────────────────────────────

    fun setFilter(filter: FeedFilter) {
        if (_uiState.value.filter == filter) return
        _uiState.update { it.copy(filter = filter, posts = emptyList(), page = 0, canLoadMore = true) }
        if (filter == FeedFilter.FRIENDS) {
            loadFollowingIds()
        }
        loadMorePosts()
    }

    private fun loadFollowingIds() {
        val uid = currentUserId
        if (uid == "me") return
        viewModelScope.launch {
            try {
                val following = repository.getFollowing(uid)
                _uiState.update { it.copy(followingIds = following) }
            } catch (_: Exception) {}
        }
    }

    fun loadMorePosts() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentFilter = _uiState.value.filter
            val page = _uiState.value.page
            val uid = currentUserId
            
            val postsFlow = when (currentFilter) {
                FeedFilter.ALL -> repository.getPosts(page, pageSize, friendsOnly = null)
                FeedFilter.FRIENDS -> repository.getPosts(page, pageSize, friendsOnly = true)
                FeedFilter.MINE -> repository.getPostsByUserId(uid)
            }

            postsFlow.collectLatest { newPosts ->
                if (currentFilter == FeedFilter.FRIENDS && newPosts.isEmpty() && page == 0) {
                    // Fallback for friends: if backend returns empty, try manual filtering once
                    repository.getPosts(0, 100, friendsOnly = null).collectLatest { allPosts ->
                        val following = _uiState.value.followingIds
                        val manualFiltered = allPosts.filter { it.authorId in following || it.authorId == uid }
                        _uiState.update { state ->
                            state.copy(
                                posts = manualFiltered,
                                isLoading = false,
                                page = state.page + 1,
                                canLoadMore = false
                            )
                        }
                    }
                } else {
                    _uiState.update { state ->
                        val updatedPosts = if (currentFilter == FeedFilter.MINE) newPosts else state.posts + newPosts
                        state.copy(
                            posts = updatedPosts,
                            isLoading = false,
                            page = state.page + 1,
                            canLoadMore = if (currentFilter == FeedFilter.MINE) false else newPosts.size == pageSize
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, page = 0, canLoadMore = true) }
            val currentFilter = _uiState.value.filter
            val uid = currentUserId
            
            val postsFlow = when (currentFilter) {
                FeedFilter.ALL -> repository.getPosts(0, pageSize, friendsOnly = null)
                FeedFilter.FRIENDS -> repository.getPosts(0, pageSize, friendsOnly = true)
                FeedFilter.MINE -> repository.getPostsByUserId(uid)
            }

            postsFlow.collectLatest { newPosts ->
                if (currentFilter == FeedFilter.FRIENDS && newPosts.isEmpty()) {
                    repository.getPosts(0, 100, friendsOnly = null).collectLatest { allPosts ->
                        val following = _uiState.value.followingIds
                        val manualFiltered = allPosts.filter { it.authorId in following || it.authorId == uid }
                        _uiState.update { it.copy(
                            posts = manualFiltered,
                            isRefreshing = false,
                            page = 1,
                            canLoadMore = false
                        ) }
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            posts = newPosts,
                            isRefreshing = false,
                            page = 1,
                            canLoadMore = if (currentFilter == FeedFilter.MINE) false else newPosts.size == pageSize
                        )
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
        _uiState.value = _uiState.value.copy(posts = updatedPosts)

        viewModelScope.launch {
            val success = if (isLiking) repository.likePost(postId) else repository.unlikePost(postId)
            if (!success) {
                // Revert on failure
                _uiState.value = _uiState.value.copy(posts = oldPosts)
            }
        }
    }

    fun addPost(habitName: String, completionNote: String, imageUri: android.net.Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val post = repository.createPost(habitName, completionNote, imageUri)
            if (post != null) {
                _uiState.update { it.copy(
                    posts = listOf(post) + it.posts,
                    isLoading = false
                ) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ─── Comments ────────────────────────────────────────────────────────

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            repository.getComments(postId).collectLatest { comments ->
                val updatedComments = _uiState.value.comments.toMutableMap().apply { put(postId, comments) }
                _uiState.value = _uiState.value.copy(comments = updatedComments, isLoadingComments = false)
            }
        }
    }

    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingComments = true) }
            val comment = repository.addComment(postId, content)
            if (comment != null) {
                val updatedComments = _uiState.value.comments.toMutableMap().apply {
                    put(postId, (get(postId) ?: emptyList()) + comment)
                }
                val updatedPosts = _uiState.value.posts.map { post ->
                    if (post.id == postId) post.copy(commentCount = post.commentCount + 1) else post
                }
                _uiState.value = _uiState.value.copy(comments = updatedComments, posts = updatedPosts, isLoadingComments = false)
            } else {
                _uiState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    // ─── Group Chats ─────────────────────────────────────────────────────

    fun loadAllChats() {
        if (isTaskLoading) return
        isTaskLoading = true
        
        viewModelScope.launch {
            val uid = currentUserId
            // 1. Seed from local cache immediately
            try {
                val cachedGroups = chatLocalStorage.loadGroupChats(uid)
                val cachedDms = chatLocalStorage.loadDirectChats(uid)
                if (cachedGroups.isNotEmpty() || cachedDms.isNotEmpty()) {
                    _uiState.update { it.copy(groupChats = cachedGroups, directChats = cachedDms) }
                }
            } catch (_: Exception) {}
            
            // 2. Refresh from network (Single call to get everything)
            _uiState.update { it.copy(isLoadingChats = true) }
            try {
                val allChats = repository.getAllChats()
                val groups = allChats.filter { it.isGroup }
                val dms = allChats.filter { !it.isGroup }
                
                _uiState.update { it.copy(
                    groupChats = groups,
                    directChats = dms
                ) }
                
                // Only load users if the list is nearly empty (avoid heavy call every startup)
                if (_uiState.value.allUsers.size <= 5) {
                    loadKnownUsers(allChats)
                }
            } catch (e: Exception) {
                // If it's a 429, we just log it and rely on cache. 
                // Don't crash or loop.
            } finally {
                _uiState.update { it.copy(isLoadingChats = false) }
                isTaskLoading = false
            }
        }
    }

    /** Loads all app users (excluding self) for the member picker. */
    private suspend fun loadKnownUsers(chats: List<ChatResponse> = emptyList()) {
        val uid = currentUserId
        try {
            val users = repository.getAllUsers().filter { it.id != uid }
            _uiState.update { it.copy(allUsers = users) }
        } catch (e: Exception) {
            // Log or handle exception if needed
            // Fallback: build from chat participants
            val fromChats = chats.asSequence().flatMap { it.participantIds }.toMutableSet()
            fromChats.remove(uid)
            val fallbackUsers = fromChats.map { id -> AppUser(id, id) }
            if (fallbackUsers.isNotEmpty()) {
                _uiState.update { it.copy(allUsers = fallbackUsers) }
            }
        }
    }

    fun loadGroupChats() {
        loadChatsJob?.cancel()
        loadChatsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            try {
                val chats = repository.getGroupChats()
                _uiState.update { it.copy(groupChats = chats, isLoadingChats = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingChats = false) }
            }
        }
    }

    fun createGroup(
        name: String,
        description: String = "",
        participantIds: List<String> = emptyList(),
        imageUri: android.net.Uri? = null,
        isPublic: Boolean = false,
        onCreated: (ChatResponse) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            val newChat = repository.createGroup(name, participantIds, isPublic)
            
            // Immediately update description if provided
            val chatWithDesc = if (description.isNotBlank()) {
                try { repository.updateGroupDescription(newChat.id, description) } catch (_: Exception) { newChat }
            } else newChat

            // Upload image and merge the returned imageUrl into final chat
            val finalChat = if (imageUri != null) {
                try {
                    val uploaded = repository.uploadGroupImage(chatWithDesc.id, imageUri)
                    chatWithDesc.copy(imageUrl = uploaded.imageUrl ?: chatWithDesc.imageUrl)
                } catch (_: Exception) {
                    chatWithDesc
                }
            } else chatWithDesc

            // Upsert: replace if already in list, else append
            _uiState.update { state ->
                val without = state.groupChats.filter { it.id != finalChat.id }
                state.copy(groupChats = without + finalChat, isLoadingChats = false)
            }
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
            onCreated(finalChat)
        }
    }

    fun uploadGroupImage(chatId: String, imageUri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val updated = repository.uploadGroupImage(chatId, imageUri)
                _uiState.update { state ->
                    val updatedGroups = state.groupChats.map { g -> 
                        if (g.id == chatId) updated else g 
                    }
                    state.copy(groupChats = updatedGroups)
                }
                // Also update local cache so it persists after restart
                try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
            } catch (e: Exception) {
                // If network fails (e.g. 429 or backend error), optimistically update UI with local URI 
                // for the current session if it's an important group.
                _uiState.update { state ->
                    state.copy(groupChats = state.groupChats.map { 
                        if (it.id == chatId) it.copy(imageUrl = imageUri.toString()) else it 
                    })
                }
            }
        }
    }

    fun createDirectChat(userId: String, onCreated: (ChatResponse) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val chat = repository.createDirectChat(userId)
                _uiState.update { it.copy(directChats = (it.directChats.filter { c -> c.id != chat.id } + chat)) }
                // Persist DMs to local cache
                try { chatLocalStorage.saveDirectChats(currentUserId, _uiState.value.directChats) } catch (_: Exception) {}
                onCreated(chat)
            } catch (_: Exception) {}
        }
    }

    fun joinGroup(chatId: String, onJoined: (ChatResponse) -> Unit = {}) {
        // Backend has no standalone "join" endpoint — add self as member via addMembers
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            val success = repository.addMember(chatId, currentUserId)
            if (success) {
                // Refresh chats and find the one we joined
                val allChats = repository.getAllChats()
                val groups = allChats.filter { it.isGroup }
                val dms = allChats.filter { !it.isGroup }
                
                _uiState.update { it.copy(
                    groupChats = groups,
                    directChats = dms,
                    isLoadingChats = false
                ) }
                
                groups.find { it.id == chatId }?.let { onJoined(it) }
            } else {
                _uiState.update { it.copy(isLoadingChats = false) }
            }
        }
    }

    fun addMember(chatId: String, userId: String) {
        viewModelScope.launch {
            repository.addMember(chatId, userId)
            // Optimistically add to participant list
            _uiState.update { state ->
                state.copy(groupChats = state.groupChats.map { chat ->
                    if (chat.id == chatId && userId !in chat.participantIds)
                        chat.copy(participantIds = chat.participantIds + userId)
                    else chat
                })
            }
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
        }
    }

    fun removeMember(chatId: String, userId: String) {
        viewModelScope.launch {
            repository.removeMember(chatId, userId)
            // Optimistically remove from participants list
            _uiState.update { state ->
                state.copy(groupChats = state.groupChats.map { chat ->
                    if (chat.id == chatId) chat.copy(participantIds = chat.participantIds - userId)
                    else chat
                })
            }
        }
    }

    fun leaveGroup(chatId: String, onLeft: () -> Unit) {
        viewModelScope.launch {
            repository.leaveGroup(chatId) // also clears from cache
            socketService.leaveChat(chatId)
            _uiState.update { it.copy(groupChats = it.groupChats.filter { c -> c.id != chatId }) }
            onLeft()
        }
    }

    fun renameGroup(chatId: String, newName: String) {
        // Optimistic update first
        _uiState.update { state ->
            state.copy(groupChats = state.groupChats.map {
                if (it.id == chatId) it.copy(name = newName) else it
            })
        }
        viewModelScope.launch {
            try {
                val updated = repository.renameGroup(chatId, newName)
                _uiState.update { state ->
                    state.copy(groupChats = state.groupChats.map { if (it.id == chatId) updated else it })
                }
            } catch (_: Exception) { /* optimistic update already applied */ }
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
        }
    }

    fun updateGroupDescription(chatId: String, description: String) {
        _uiState.update { state ->
            state.copy(groupChats = state.groupChats.map {
                if (it.id == chatId) it.copy(description = description) else it
            })
        }
        viewModelScope.launch {
            try {
                val updated = repository.updateGroupDescription(chatId, description)
                _uiState.update { state ->
                    state.copy(groupChats = state.groupChats.map { if (it.id == chatId) updated else it })
                }
            } catch (_: Exception) { /* optimistic update already applied */ }
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
        }
    }

    fun updateGroupVisibility(chatId: String, isPublic: Boolean) {
        _uiState.update { state ->
            state.copy(groupChats = state.groupChats.map {
                if (it.id == chatId) it.copy(isPublic = isPublic) else it
            })
        }
        viewModelScope.launch {
            try {
                val updated = repository.updateGroupVisibility(chatId, isPublic)
                _uiState.update { state ->
                    state.copy(groupChats = state.groupChats.map { if (it.id == chatId) updated else it })
                }
            } catch (_: Exception) { /* optimistic update already applied */ }
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
        }
    }

    fun promoteAdmin(chatId: String, userId: String) {
        _uiState.update { state ->
            state.copy(groupChats = state.groupChats.map {
                if (it.id == chatId) it.copy(admins = (it.admins + userId).distinct()) else it
            })
        }
        viewModelScope.launch {
            try {
                val updated = repository.promoteAdmin(chatId, userId)
                _uiState.update { state ->
                    state.copy(groupChats = state.groupChats.map { if (it.id == chatId) updated else it })
                }
            } catch (_: Exception) { /* optimistic */ }
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
        }
    }

    fun demoteAdmin(chatId: String, userId: String) {
        _uiState.update { state ->
            state.copy(groupChats = state.groupChats.map {
                if (it.id == chatId) it.copy(admins = it.admins - userId) else it
            })
        }
        viewModelScope.launch {
            try {
                val updated = repository.demoteAdmin(chatId, userId)
                _uiState.update { state ->
                    state.copy(groupChats = state.groupChats.map { if (it.id == chatId) updated else it })
                }
            } catch (_: Exception) { /* optimistic */ }
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
        }
    }

    fun deleteGroup(chatId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGroup(chatId) // also clears local cache on both success and failure
            socketService.leaveChat(chatId)
            _uiState.update { it.copy(groupChats = it.groupChats.filter { c -> c.id != chatId }) }
            onDeleted()
        }
    }

    fun deleteDirectChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteGroup(chatId)
            _uiState.update { it.copy(directChats = it.directChats.filter { c -> c.id != chatId }) }
        }
    }

    // ─── Search ─────────────────────────────────────────────────────────

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        _uiState.update { it.copy(isSearching = true) }
        
        // Local filtering for users (by username/email) and groups (by name)
        val userResults = _uiState.value.allUsers
            .filter { user -> 
                user.email.substringBefore('@').contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
            .take(10)
            .map { SearchResult.User(it) }

        val groupResults = _uiState.value.groupChats
            .filter { it.name?.contains(query, ignoreCase = true) == true }
            .take(5)
            .map { SearchResult.Group(it) }

        _uiState.update { it.copy(
            searchResults = (userResults + groupResults).sortedBy { result ->
                when(result) {
                    is SearchResult.User -> result.user.email
                    is SearchResult.Group -> result.chat.name ?: ""
                }
            },
            isSearching = false
        ) }
    }

    // ─── Messages ────────────────────────────────────────────────────────

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            // Seed from persisted cache immediately (no network delay shown to user)
            try {
                val persisted = chatLocalStorage.loadMessages(chatId)
                if (persisted.isNotEmpty()) {
                    val seedMap = _uiState.value.chatMessages.toMutableMap().apply { put(chatId, persisted) }
                    _uiState.update { it.copy(chatMessages = seedMap) }
                } else {
                    _uiState.update { it.copy(isLoadingMessages = true) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMessages = true) }
            }
            // Refresh from network
            val messages = repository.getMessages(chatId)
            if (messages.isNotEmpty()) {
                val updatedMap = _uiState.value.chatMessages.toMutableMap().apply { put(chatId, messages) }
                _uiState.update { it.copy(chatMessages = updatedMap, isLoadingMessages = false) }
            } else {
                _uiState.update { it.copy(isLoadingMessages = false) }
            }
            socketService.joinChat(chatId)
            repository.markAsRead(chatId)
            _uiState.update { state ->
                state.copy(groupChats = state.groupChats.map { chat ->
                    if (chat.id == chatId) chat.copy(unreadCount = chat.unreadCount - currentUserId) else chat
                })
            }
        }
    }

    fun sendMessage(chatId: String, content: String) {
        val newMessage = ChatMessage(
            text = content,
            senderId = currentUserId,
            isFromBot = false
        )
        val updatedMessages = (_uiState.value.chatMessages[chatId] ?: emptyList()) + newMessage
        val updatedMap = _uiState.value.chatMessages.toMutableMap().apply { put(chatId, updatedMessages) }
        _uiState.update { it.copy(chatMessages = updatedMap) }
        _uiState.update { state ->
            state.copy(groupChats = state.groupChats.map {
                if (it.id == chatId) it.copy(lastMessage = content) else it
            })
        }
        // Persist new message immediately so it survives restarts
        viewModelScope.launch {
            try { chatLocalStorage.saveMessages(chatId, updatedMessages) } catch (_: Exception) {}
        }
        // Send via socket for real-time delivery
        socketService.sendMessage(chatId, content)
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        socketService.setTyping(chatId, isTyping)
    }

    fun toggleMessageLike(chatId: String, messageId: String) {
        viewModelScope.launch {
            val updatedMessage = repository.toggleMessageLike(chatId, messageId) ?: return@launch
            val updatedMessages = (_uiState.value.chatMessages[chatId] ?: emptyList()).map {
                if (it.id == messageId) updatedMessage else it
            }
            val updatedMap = _uiState.value.chatMessages.toMutableMap().apply { put(chatId, updatedMessages) }
            _uiState.update { it.copy(chatMessages = updatedMap) }
        }
    }

    // ─── Socket event handling ───────────────────────────────────────────

    private fun collectSocketEvents() {
        viewModelScope.launch {
            socketService.events.collect { event ->
                when (event) {
                    is SocialChatSocketEvent.NewMessage -> handleNewMessage(event)
                    is SocialChatSocketEvent.TypingChanged -> handleTypingChanged(event)
                    is SocialChatSocketEvent.MessageLiked -> handleMessageLiked(event)
                    is SocialChatSocketEvent.MemberAdded -> loadGroupChats()
                    is SocialChatSocketEvent.MemberRemoved -> loadGroupChats()
                    is SocialChatSocketEvent.MemberLeft -> loadGroupChats()
                    is SocialChatSocketEvent.GroupRenamed -> handleGroupRenamed(event)
                    is SocialChatSocketEvent.GroupUpdated -> handleGroupUpdated(event)
                    is SocialChatSocketEvent.AdminAdded -> loadGroupChats()
                    is SocialChatSocketEvent.AdminRemoved -> loadGroupChats()
                    is SocialChatSocketEvent.GroupDeleted -> handleGroupDeleted(event)
                }
            }
        }
    }

    private fun handleNewMessage(event: SocialChatSocketEvent.NewMessage) {
        val incomingMessage = ChatMessage(
            id = event.messageId,
            text = event.text,
            senderId = event.senderId,
            imageUrl = event.imageUrl
        )
        val updatedMessages = (_uiState.value.chatMessages[event.chatId] ?: emptyList()) + incomingMessage
        val updatedMap = _uiState.value.chatMessages.toMutableMap().apply { put(event.chatId, updatedMessages) }
        _uiState.update { state ->
            state.copy(
                chatMessages = updatedMap,
                groupChats = state.groupChats.map { chat ->
                    if (chat.id == event.chatId) chat.copy(lastMessage = event.text) else chat
                }
            )
        }
        // Persist incoming message
        viewModelScope.launch {
            try { chatLocalStorage.saveMessages(event.chatId, updatedMessages) } catch (_: Exception) {}
        }
    }

    private fun handleTypingChanged(event: SocialChatSocketEvent.TypingChanged) {
        _uiState.update { state ->
            val current = state.typingUsers[event.chatId]?.toMutableSet() ?: mutableSetOf()
            if (event.isTyping) current.add(event.userId) else current.remove(event.userId)
            state.copy(typingUsers = state.typingUsers + (event.chatId to current))
        }
    }

    private fun handleMessageLiked(event: SocialChatSocketEvent.MessageLiked) {
        val updatedMessages = (_uiState.value.chatMessages[event.chatId] ?: emptyList()).map { msg ->
            if (msg.id == event.messageId) msg.copy(likedBy = event.likes) else msg
        }
        val updatedMap = _uiState.value.chatMessages.toMutableMap().apply {
            put(event.chatId, updatedMessages)
        }
        _uiState.update { it.copy(chatMessages = updatedMap) }
    }

    private fun handleGroupRenamed(event: SocialChatSocketEvent.GroupRenamed) {
        _uiState.update { state ->
            state.copy(groupChats = state.groupChats.map { chat ->
                if (chat.id == event.chatId) chat.copy(name = event.name) else chat
            })
        }
    }

    private fun handleGroupUpdated(event: SocialChatSocketEvent.GroupUpdated) {
        _uiState.update { state ->
            state.copy(groupChats = state.groupChats.map { chat ->
                if (chat.id == event.chatId) {
                    chat.copy(
                        description = event.description ?: chat.description,
                        isPublic = event.isPublic ?: chat.isPublic,
                        imageUrl = event.imageUrl ?: chat.imageUrl
                    )
                } else chat
            })
        }
        // If it was a major update (like image), might still want to refresh or at least ensure local storage is updated
        viewModelScope.launch {
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
        }
    }

    private fun handleGroupDeleted(event: SocialChatSocketEvent.GroupDeleted) {
        _uiState.update { state ->
            state.copy(
                groupChats = state.groupChats.filter { it.id != event.chatId },
                chatMessages = state.chatMessages - event.chatId
            )
        }
    }

    override fun onCleared() {
        socketService.disconnect()
        super.onCleared()
    }
}
