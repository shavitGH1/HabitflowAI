package com.habitflowai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.ChatResponse
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

data class SocialUiState(
    val posts: List<Post> = emptyList(),
    val comments: Map<Int, List<Comment>> = emptyMap(),
    val groupChats: List<ChatResponse> = emptyList(),
    val directChats: List<ChatResponse> = emptyList(),
    val chatMessages: Map<String, List<com.habitflowai.data.model.ChatMessage>> = emptyMap(),
    val typingUsers: Map<String, Set<String>> = emptyMap(),
    val currentUserId: String = "me",
    val allUsers: List<com.habitflowai.data.model.AppUser> = emptyList(), // all app users for member picker
    val knownUserIds: List<String> = emptyList(), // deprecated — use allUsers
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
    private val repository: SocialRepository,
    private val socketService: SocialChatSocketService,
    private val authManager: AuthManager,
    private val chatLocalStorage: ChatLocalStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val pageSize = 10
    private var isTaskLoading = false

    /** The real user ID from the JWT token, falling back to "me" if not yet authenticated. */
    private val currentUserId: String
        get() = authManager.currentUserId.value ?: "me"

    init {
        // Seed with some test users immediately so the UI isn't empty while loading
        val initialTestUsers = listOf(
            com.habitflowai.data.model.AppUser("alex_id", "alex_pro@habitflow.ai"),
            com.habitflowai.data.model.AppUser("mia_id", "mia_zen@habitflow.ai"),
            com.habitflowai.data.model.AppUser("sam_id", "sam_fit@habitflow.ai"),
            com.habitflowai.data.model.AppUser("jordan_id", "jordan_dev@habitflow.ai"),
            com.habitflowai.data.model.AppUser("taylor_id", "taylor_coach@habitflow.ai")
        )
        _uiState.update { it.copy(allUsers = initialTestUsers) }

        loadMorePosts()
        socketService.connect()
        collectSocketEvents()

        // Sync with Auth: only load data when the user ID actually changes to a valid one.
        // This avoids redundant calls (HTTP 429) at startup.
        viewModelScope.launch {
            authManager.currentUserId.collect { uid ->
                val oldUid = _uiState.value.currentUserId
                val newUid = uid ?: "me"
                
                if (newUid != oldUid) {
                    _uiState.update { it.copy(currentUserId = newUid) }
                    if (newUid == "me") {
                        // Logout: Clear messages and refresh lists (loads mocks/guest cache)
                        _uiState.update { it.copy(
                            groupChats = emptyList(),
                            directChats = emptyList(),
                            chatMessages = emptyMap()
                        ) }
                    }
                    loadAllChats()
                } else if (newUid == "me" && _uiState.value.groupChats.isEmpty()) {
                    // Initial load if not logged in yet (shows mocks/cached)
                    loadAllChats()
                }
            }
        }
    }

    // ─── Posts ───────────────────────────────────────────────────────────

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
            author = currentUserId,
            content = content,
            hasPhoto = imageUri != null,
            imageUri = imageUri,
            likeCount = 0,
            isLiked = false
        )
        _uiState.value = _uiState.value.copy(posts = listOf(newPost) + _uiState.value.posts)
    }

    // ─── Comments ────────────────────────────────────────────────────────

    fun loadComments(postId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            repository.getComments(postId).collectLatest { comments ->
                val updatedComments = _uiState.value.comments.toMutableMap().apply { put(postId, comments) }
                _uiState.value = _uiState.value.copy(comments = updatedComments, isLoadingComments = false)
            }
        }
    }

    fun addComment(postId: Int, content: String) {
        val newComment = Comment(
            id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            postId = postId,
            author = currentUserId,
            content = content
        )
        val updatedComments = _uiState.value.comments.toMutableMap().apply {
            put(postId, (get(postId) ?: emptyList()) + newComment)
        }
        val updatedPosts = _uiState.value.posts.map { post ->
            if (post.id == postId) post.copy(commentCount = post.commentCount + 1) else post
        }
        _uiState.value = _uiState.value.copy(comments = updatedComments, posts = updatedPosts)
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
                    directChats = dms,
                    isLoadingChats = false
                ) }
                loadKnownUsers(allChats)
            } finally {
                _uiState.update { it.copy(isLoadingChats = false) }
                isTaskLoading = false
            }
        }
    }

    /** Loads all app users (excluding self) for the member picker. */
    private fun loadKnownUsers(chats: List<ChatResponse> = emptyList()) {
        // If we already have a significant number of users (e.g. from the hardcoded seed),
        // we can skip the network call if we are under rate-limit pressure.
        if (_uiState.value.allUsers.size > 10 && _uiState.value.isLoadingChats) return

        viewModelScope.launch {
            val uid = currentUserId
            try {
                val users = repository.getAllUsers().filter { it.id != uid }
                _uiState.update { it.copy(allUsers = users, knownUserIds = users.map { u -> u.id }) }
            } catch (_: Exception) {
                // Fallback: build from chat participants + follows
                val fromChats = chats.flatMap { it.participantIds }.toMutableSet()
                try { fromChats.addAll(repository.getFollowing(uid)) } catch (_: Exception) {}
                fromChats.remove(uid)
                // Even on failure, populate allUsers with what we have (ID fallback) so the list isn't empty
                val fallbackUsers = fromChats.map { id -> com.habitflowai.data.model.AppUser(id, id) }
                _uiState.update { it.copy(allUsers = fallbackUsers, knownUserIds = fromChats.sorted()) }
            }
        }
    }

    fun loadGroupChats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            val chats = repository.getGroupChats()
            _uiState.update { it.copy(groupChats = chats, isLoadingChats = false) }
        }
    }

    fun createGroup(
        name: String,
        participantIds: List<String> = emptyList(),
        imageUri: android.net.Uri? = null,
        onCreated: (ChatResponse) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            val newChat = repository.createGroup(name, participantIds)

            // Upload image and merge the returned imageUrl into newChat (fallback keeps newChat intact)
            val finalChat = if (imageUri != null) {
                try {
                    val uploaded = repository.uploadGroupImage(newChat.id, imageUri)
                    // Merge: keep all fields from newChat, only pick up imageUrl from upload response
                    newChat.copy(imageUrl = uploaded.imageUrl ?: newChat.imageUrl)
                } catch (_: Exception) {
                    newChat  // image upload failed — keep the full chat data, just no photo
                }
            } else newChat

            // Upsert: replace if already in list (repo.createGroup may have added it), else append
            _uiState.update { state ->
                val without = state.groupChats.filter { it.id != finalChat.id }
                state.copy(groupChats = without + finalChat, isLoadingChats = false)
            }
            // Persist the final merged state to local cache
            try { chatLocalStorage.saveGroupChats(currentUserId, _uiState.value.groupChats) } catch (_: Exception) {}
            onCreated(finalChat)
        }
    }

    fun uploadGroupImage(chatId: String, imageUri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val updated = repository.uploadGroupImage(chatId, imageUri)
                _uiState.update { state ->
                    val updatedGroups = state.groupChats.map { if (it.id == chatId) updated else it }
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

    fun joinGroup(chatId: String) {
        // Backend has no standalone "join" endpoint — add self as member via addMembers
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            val success = repository.addMember(chatId, currentUserId)
            if (success) loadGroupChats() else _uiState.update { it.copy(isLoadingChats = false) }
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
        val newMessage = com.habitflowai.data.model.ChatMessage(
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
                    is SocialChatSocketEvent.GroupUpdated -> loadGroupChats()
                    is SocialChatSocketEvent.AdminAdded -> loadGroupChats()
                    is SocialChatSocketEvent.AdminRemoved -> loadGroupChats()
                    is SocialChatSocketEvent.GroupDeleted -> handleGroupDeleted(event)
                }
            }
        }
    }

    private fun handleNewMessage(event: SocialChatSocketEvent.NewMessage) {
        val incomingMessage = com.habitflowai.data.model.ChatMessage(
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
