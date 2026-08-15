package com.habitflowai.presentation.ui.social

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.luminance
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.data.model.AppUser
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.SocialUiState
import com.habitflowai.presentation.viewmodel.SocialViewModel
import com.habitflowai.presentation.viewmodel.SearchResult
import com.habitflowai.presentation.viewmodel.FeedFilter
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialRoute(
    viewModel: SocialViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    onUserClick: (String) -> Unit,
    onToggleChat: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (onboardingState.personaResult == null && !onboardingState.isLoading) {
            onboardingViewModel.fetchProfile()
        }
    }

    val personaType = onboardingState.personaResult?.personaType ?: "Regulator"
    val personaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val autoOpenChatId by viewModel.autoOpenChatId.collectAsState()
    
    var showCreateSheet by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var showNewDmSheet by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }
    var selectedChatForView by remember { mutableStateOf<ChatResponse?>(null) }
    var showAddMemberDialog by remember { mutableStateOf<ChatResponse?>(null) }

    LaunchedEffect(autoOpenChatId, uiState.groupChats, uiState.directChats, uiState.isLoadingChats) {
        autoOpenChatId?.let { chatId ->
            val chat = uiState.groupChats.find { it.id == chatId }
                ?: uiState.directChats.find { it.id == chatId }
            
            if (chat != null) {
                selectedChatForView = chat
                viewModel.loadMessages(chatId)
                viewModel.autoOpenChatId.value = null
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                GroupChatsDrawerContent(
                    uiState = uiState,
                    personaColor = personaDetails.endColor,
                    onChatClick = { chat ->
                        selectedChatForView = chat
                        viewModel.loadMessages(chat.id)
                    },
                    onCreateGroupClick = {
                        showCreateGroupDialog = true
                    },
                    onJoinGroupClick = { 
                        showJoinGroupDialog = true 
                    },
                    onNewDmClick = {
                        showNewDmSheet = true
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Social Feed", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onToggleChat) {
                            Icon(
                                Icons.Rounded.SmartToy,
                                contentDescription = "AI Assistant",
                                tint = personaDetails.endColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Rounded.Forum, 
                                contentDescription = "Community Chats", 
                                tint = personaDetails.endColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = personaDetails.endColor,
                        actionIconContentColor = personaDetails.endColor
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Post")
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                SocialContent(
                    uiState = uiState,
                    personaColor = personaDetails.endColor,
                    modifier = Modifier.fillMaxSize(),
                    onLikeClick = viewModel::toggleLike,
                    onLoadMore = viewModel::loadMorePosts,
                    onFilterChange = viewModel::setFilter,
                    onUserClick = onUserClick,
                    onPostClick = { post ->
                        selectedPostForComments = post
                        viewModel.loadComments(post.id)
                    }
                )
            }
        }
    }

    if (showCreateSheet) {
        CreatePostBottomSheet(
            onDismiss = { showCreateSheet = false },
            onPostCreated = { habitName, completionNote, imageUri ->
                viewModel.addPost(habitName, completionNote, imageUri)
                showCreateSheet = false
            }
        )
    }

    if (selectedChatForView != null) {
        val activeChat = selectedChatForView!!
        val activeChatId = activeChat.id
        // Always find the latest chat state (groups or DMs)
        val liveChat = uiState.groupChats.find { it.id == activeChatId }
            ?: uiState.directChats.find { it.id == activeChatId }
            ?: activeChat
        val goBack: () -> Unit = {
            selectedChatForView = null
            scope.launch { drawerState.open() }
        }
        SocialGroupChatScreen(
            chat = liveChat,
            messages = uiState.chatMessages[activeChatId] ?: emptyList(),
            personaColor = personaDetails.endColor,
            currentUserId = uiState.currentUserId,
            typingUserIds = uiState.typingUsers[activeChatId] ?: emptySet(),
            allUsers = uiState.allUsers,
            onDismiss = goBack,
            onSendMessage = { content -> viewModel.sendMessage(activeChatId, content) },
            onTypingChanged = { isTyping -> viewModel.setTyping(activeChatId, isTyping) },
            onLikeMessage = { messageId -> viewModel.toggleMessageLike(activeChatId, messageId) },
            onAddMember = {
                showAddMemberDialog = selectedChatForView
            },
            onRemoveMember = { userId -> viewModel.removeMember(activeChatId, userId) },
            onLeaveGroup = {
                viewModel.leaveGroup(activeChatId) { goBack() }
            },
            onRenameGroup = { name -> viewModel.renameGroup(activeChatId, name) },
            onUpdateDescription = { desc -> viewModel.updateGroupDescription(activeChatId, desc) },
            onUpdateVisibility = { isPublic -> viewModel.updateGroupVisibility(activeChatId, isPublic) },
            onPromoteAdmin = { userId -> viewModel.promoteAdmin(activeChatId, userId) },
            onDemoteAdmin = { userId -> viewModel.demoteAdmin(activeChatId, userId) },
            onDeleteGroup = {
                if (liveChat.isGroup) {
                    viewModel.deleteGroup(activeChatId) { goBack() }
                } else {
                    viewModel.deleteDirectChat(activeChatId)
                    goBack()
                }
            },
            onUploadGroupPhoto = { uri -> viewModel.uploadGroupImage(activeChatId, uri) }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupBottomSheet(
            personaColor = personaDetails.endColor,
            allUsers = uiState.allUsers,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, description, participantIds, imageUri, isPublic ->
                showCreateGroupDialog = false
                viewModel.createGroup(name, description, participantIds, imageUri, isPublic) { newChat ->
                    selectedChatForView = newChat
                    viewModel.loadMessages(newChat.id)
                }
            }
        )
    }

    if (showJoinGroupDialog) {
        JoinGroupDialog(
            uiState = uiState,
            personaColor = personaDetails.endColor,
            onSearch = viewModel::search,
            onJoinGroup = { groupChat ->
                showJoinGroupDialog = false
                // If already a member, just open it. Otherwise, join first.
                if (uiState.currentUserId in groupChat.participantIds) {
                    selectedChatForView = groupChat
                    viewModel.loadMessages(groupChat.id)
                } else {
                    viewModel.joinGroup(groupChat.id) { updatedChat ->
                        selectedChatForView = updatedChat
                        viewModel.loadMessages(updatedChat.id)
                    }
                }
            },
            onStartDm = { userId ->
                showJoinGroupDialog = false
                // Check if a DM with this user already exists
                val existingDm = uiState.directChats.find { userId in it.participantIds }
                if (existingDm != null) {
                    selectedChatForView = existingDm
                    viewModel.loadMessages(existingDm.id)
                } else {
                    viewModel.createDirectChat(userId) { chat ->
                        selectedChatForView = chat
                        viewModel.loadMessages(chat.id)
                    }
                }
            },
            onUserClick = onUserClick,
            onDismiss = { showJoinGroupDialog = false }
        )
    }

    if (showNewDmSheet) {
        NewDirectMessageSheet(
            personaColor = personaDetails.endColor,
            allUsers = uiState.allUsers,
            onDismiss = { showNewDmSheet = false },
            onStart = { userId ->
                showNewDmSheet = false
                viewModel.createDirectChat(userId) { chat ->
                    selectedChatForView = chat
                    viewModel.loadMessages(chat.id)
                }
            }
        )
    }

    if (showAddMemberDialog != null) {
        AddMemberDialog(
            chat = showAddMemberDialog!!,
            personaColor = personaDetails.endColor,
            allUsers = uiState.allUsers,
            onDismiss = { showAddMemberDialog = null },
            onAdd = { userId ->
                viewModel.addMember(showAddMemberDialog!!.id, userId)
                showAddMemberDialog = null
            }
        )
    }

    if (selectedPostForComments != null) {
        val currentPost = uiState.posts.find { it.id == selectedPostForComments!!.id } ?: selectedPostForComments!!
        CommentsBottomSheet(
            post = currentPost,
            comments = uiState.comments[currentPost.id] ?: emptyList(),
            isLoadingComments = uiState.isLoadingComments,
            onDismiss = { selectedPostForComments = null },
            onLikeClick = { viewModel.toggleLike(currentPost.id) },
            onAddComment = { content ->
                viewModel.addComment(currentPost.id, content)
            },
            onUserClick = { onUserClick(currentPost.authorId) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatsDrawerContent(
    uiState: SocialUiState,
    personaColor: Color,
    onChatClick: (ChatResponse) -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onNewDmClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Text(
            text = "Community",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = personaColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Join Existing Button
        OutlinedButton(
            onClick = onJoinGroupClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = personaColor),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = Brush.linearGradient(listOf(personaColor, personaColor.copy(alpha = 0.5f))))
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Search Groups & Friends")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoadingChats) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = personaColor)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                // Groups section
                if (uiState.groupChats.isNotEmpty()) {
                    item {
                        Text(
                            "Groups",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(uiState.groupChats) { chat ->
                        SocialGroupChatListItem(chat = chat, personaColor = personaColor, currentUserId = uiState.currentUserId, onClick = { onChatClick(chat) })
                    }
                }

                // Direct Messages section
                if (uiState.directChats.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Direct Messages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(uiState.directChats, key = { it.id }) { chat ->
                        SocialGroupChatListItem(chat = chat, personaColor = personaColor, currentUserId = uiState.currentUserId, onClick = { onChatClick(chat) })
                    }
                }

                if (uiState.groupChats.isEmpty() && uiState.directChats.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text("No chats yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // New Message (DM) button
        OutlinedButton(
            onClick = onNewDmClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = personaColor),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = Brush.linearGradient(listOf(personaColor, personaColor.copy(alpha = 0.5f))))
        ) {
            Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("New Message")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gradient "New Group" Button
        val newGroupContentColor = if (personaColor.luminance() > 0.5f) Color.Black else Color.White
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(personaColor, personaColor.copy(alpha = 0.7f))))
                .clickable { onCreateGroupClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = newGroupContentColor)
                Spacer(Modifier.width(8.dp))
                Text("New Group", color = newGroupContentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDirectMessageSheet(
    personaColor: Color,
    allUsers: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onStart: (String) -> Unit
) {
    MemberPickerSheet(
        title = "New Message",
        allUsers = allUsers,
        multiSelect = false,
        personaColor = personaColor,
        onDismiss = onDismiss,
        onConfirm = { picked -> picked.firstOrNull()?.let { onStart(it) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupBottomSheet(
    personaColor: Color,
    allUsers: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, participantIds: List<String>, imageUri: Uri?, isPublic: Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Prevent Material3's ModalBottomSheet from auto-collapsing to Hidden when the
    // IME closes and its content remeasures (a known Compose bug) — the explicit
    // close button below remains the guaranteed way to dismiss.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    var groupName by rememberSaveable { mutableStateOf("") }
    var groupDescription by rememberSaveable { mutableStateOf("") }
    var isPublic by rememberSaveable { mutableStateOf(false) }
    var selectedImageUriStr by rememberSaveable { mutableStateOf("") }
    val selectedImageUri: Uri? = selectedImageUriStr.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
    var selectedUsersStr by rememberSaveable { mutableStateOf("") }
    val selectedUsers: Set<String> = selectedUsersStr.split(",").filter { it.isNotEmpty() }.toSet()
    var showPhotoDialog by rememberSaveable { mutableStateOf(false) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var pendingCameraUriStr by rememberSaveable { mutableStateOf("") }
    val pendingCameraUri: Uri? = pendingCameraUriStr.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createImageFileUri(context)
            pendingCameraUriStr = uri.toString()
        }
        showPhotoDialog = false
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraUriStr.isNotEmpty()) selectedImageUriStr = pendingCameraUriStr
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUriStr = it.toString() }
    }

    LaunchedEffect(pendingCameraUri) {
        pendingCameraUri?.let { cameraLauncher.launch(it) }
    }

    if (showMemberPicker) {
        MemberPickerSheet(
            title = "Add Members",
            allUsers = allUsers,
            excludeIds = emptySet(),
            initialSelected = selectedUsers,
            multiSelect = true,
            personaColor = personaColor,
            onDismiss = { showMemberPicker = false },
            onConfirm = { picked -> selectedUsersStr = picked.joinToString(",") }
        )
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Group Photo", fontWeight = FontWeight.Bold) },
            text = {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        onClick = { cameraPermLauncher.launch(android.Manifest.permission.CAMERA) }
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = personaColor, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Camera", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        onClick = { galleryLauncher.launch("image/*"); showPhotoDialog = false }
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = personaColor, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Gallery", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPhotoDialog = false }) { Text("Cancel") } }
        )
    }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            // Material3's own back-press dismissal fires *before* any BackHandler we
            // add inside content can reliably win priority over it (confirmed on
            // device — inconsistent across sheets). Disable it at the source and let
            // our own BackHandler below be the only thing back presses reach.
            properties = ModalBottomSheetDefaults.properties(
                shouldDismissOnBackPress = false
            )
        ) {
        // First back press should only dismiss the keyboard; only close the
        // sheet on a second press once the IME is already hidden.
        val keyboardController = LocalSoftwareKeyboardController.current
        val imeVisible = WindowInsets.isImeVisible
        BackHandler {
            if (imeVisible) keyboardController?.hide() else onDismiss()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable so the description field and members card stay reachable
            // once the keyboard eats into the available height — the Create
            // button below is a fixed sibling, not part of this scroll area, so
            // it stays pinned instead of using Modifier.weight() (incompatible
            // with verticalScroll).
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Create New Group", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Set up your community space", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Circular photo picker with nicer UI
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(personaColor.copy(alpha = 0.1f))
                    .clickable { showPhotoDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.AddAPhoto, contentDescription = "Pick photo", tint = personaColor, modifier = Modifier.size(36.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (selectedImageUri != null) "Tap to change photo" else "Add group cover",
                style = MaterialTheme.typography.labelLarge,
                color = personaColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(32.dp))

            // Group name & description field in a nice card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Details", style = MaterialTheme.typography.labelMedium, color = personaColor, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = { Text("Enter group name...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = personaColor
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it },
                        placeholder = { Text("What is this group about? (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = personaColor
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Member selection card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                onClick = { showMemberPicker = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Members", style = MaterialTheme.typography.labelMedium, color = personaColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${selectedUsers.size} added", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    if (selectedUsers.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = personaColor.copy(alpha = 0.1f)) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = personaColor, modifier = Modifier.padding(10.dp))
                            }
                            Text("Add participants", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(selectedUsers.toList()) { userId ->
                                val username = resolveDisplayName(userId, allUsers)
                                AssistChip(
                                    onClick = { selectedUsersStr = (selectedUsers - userId).joinToString(",") },
                                    label = { Text(username, fontSize = 12.sp) },
                                    leadingIcon = {
                                        SocialUserAvatar(name = username, personaColor = personaColor, size = 20)
                                    },
                                    trailingIcon = {
                                        Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Public Group Toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Public Group", style = MaterialTheme.typography.labelMedium, color = personaColor, fontWeight = FontWeight.Bold)
                        Text("Anyone can find and join via search", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = personaColor
                        )
                    )
                }
            }

            } // end scrollable Column

            Spacer(Modifier.height(16.dp))

            // Create button - big and bold
            Button(
                onClick = {
                    val fallbackName = if (selectedUsers.isNotEmpty()) {
                        "Group with " + allUsers.find { it.id == selectedUsers.first() }?.email?.substringBefore('@')
                    } else "New Group"
                    
                    val nameToSave = groupName.trim().ifBlank { fallbackName }
                    val descToSave = groupDescription.trim()
                    val membersToSave = selectedUsers.toList()
                    val imageToSave = selectedImageUri
                    val isPublicToSave = isPublic
                    groupName = ""
                    groupDescription = ""
                    selectedUsersStr = ""
                    selectedImageUriStr = ""
                    isPublic = false
                    onCreate(nameToSave, descToSave, membersToSave, imageToSave, isPublicToSave)
                },
                enabled = groupName.isNotBlank() || selectedUsers.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = personaColor,
                    disabledContainerColor = personaColor.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Groups, contentDescription = null)
                    Text(
                        "Launch Group",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun JoinGroupDialog(
    uiState: SocialUiState,
    personaColor: Color,
    onSearch: (String) -> Unit,
    onJoinGroup: (ChatResponse) -> Unit,
    onStartDm: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        onSearch(query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = personaColor.copy(alpha = 0.1f)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = personaColor, modifier = Modifier.padding(8.dp))
                }
                Text("Search Groups & Friends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                Text(
                    "Find communities or start a conversation with someone new.", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by name or username...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = personaColor,
                        focusedLabelColor = personaColor,
                        cursorColor = personaColor
                    ),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = personaColor.copy(alpha = 0.6f)) }
                )
                
                Spacer(Modifier.height(16.dp))
                
                Box(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = personaColor)
                    } else if (query.isNotBlank() && uiState.searchResults.isEmpty()) {
                        Text("No results found for \"$query\"", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.searchResults) { result ->
                                when (result) {
                                    is SearchResult.User -> {
                                        val username = result.user.email.substringBefore('@')
                                        SearchItemRow(
                                            title = username,
                                            subtitle = result.user.email,
                                            icon = Icons.Rounded.Person,
                                            personaColor = personaColor,
                                            onClick = { onUserClick(result.user.id) },
                                            trailing = {
                                                Row {
                                                    // Follow icon could be added here if we had follow state in SearchResult
                                                    // For now, let's keep it simple with just DM, or add a generic "View Profile" + icon
                                                    IconButton(onClick = { onUserClick(result.user.id) }) {
                                                        Icon(Icons.Rounded.PersonAdd, contentDescription = "View Profile", tint = personaColor)
                                                    }
                                                    IconButton(onClick = { onStartDm(result.user.id) }) {
                                                        Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = "Message", tint = personaColor)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    is SearchResult.Group -> {
                                        val isMember = uiState.currentUserId in result.chat.participantIds
                                        SearchItemRow(
                                            title = result.chat.name ?: "Unnamed Group",
                                            subtitle = "${result.chat.participantIds.size} members" + if (isMember) " • Joined" else "",
                                            icon = Icons.Rounded.Groups,
                                            personaColor = personaColor,
                                            onClick = { onJoinGroup(result.chat) },
                                            trailing = {
                                                if (isMember) {
                                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
                                                } else {
                                                    Button(
                                                        onClick = { onJoinGroup(result.chat) },
                                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                                        modifier = Modifier.height(32.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun SearchItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    personaColor: Color,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = personaColor.copy(alpha = 0.1f)
            ) {
                Icon(icon, contentDescription = null, tint = personaColor, modifier = Modifier.padding(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
    chat: ChatResponse,
    personaColor: Color,
    allUsers: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    // Exclude users already in the chat
    val eligibleUsers = remember(allUsers, chat.participantIds) {
        allUsers.filter { it.id !in chat.participantIds }
    }
    MemberPickerSheet(
        title = "Add to ${chat.name}",
        allUsers = eligibleUsers,
        initialSelected = emptySet(),
        multiSelect = false,
        personaColor = personaColor,
        onDismiss = onDismiss,
        onConfirm = { picked -> picked.firstOrNull()?.let { onAdd(it) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialContent(
    uiState: SocialUiState,
    personaColor: Color,
    modifier: Modifier = Modifier,
    onLikeClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onFilterChange: (FeedFilter) -> Unit,
    onUserClick: (String) -> Unit,
    onPostClick: (Post) -> Unit
) {
    val listState = rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isLoading && uiState.canLoadMore) {
            onLoadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Feed Filter
            TabRow(
                selectedTabIndex = uiState.filter.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = personaColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.filter.ordinal]),
                        color = personaColor
                    )
                }
            ) {
                FeedFilter.entries.forEach { filter ->
                    Tab(
                        selected = uiState.filter == filter,
                        onClick = { onFilterChange(filter) },
                        text = {
                            Text(
                                text = when (filter) {
                                    FeedFilter.ALL -> "All"
                                    FeedFilter.FRIENDS -> "Friends"
                                    FeedFilter.MINE -> "Mine"
                                },
                                fontWeight = if (uiState.filter == filter) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Temporary ID Search for Profile
            var searchId by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchId,
                    onValueChange = { searchId = it },
                    placeholder = { Text("Search User ID...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (searchId.isNotBlank()) onUserClick(searchId) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.posts.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize().padding(bottom = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyMessage = when (uiState.filter) {
                                FeedFilter.FRIENDS -> {
                                    if (uiState.followingIds.isEmpty()) "Seems like you didn't connect with friends yet"
                                    else "Looks like your friends didn't post anything yet"
                                }
                                FeedFilter.MINE -> "You haven't posted anything yet"
                                FeedFilter.ALL -> "No posts found"
                            }
                            Text(
                                text = emptyMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.posts,
                        key = { _, post -> post.id }
                    ) { _, post ->
                        PostCard(
                            post = post,
                            personaColor = personaColor,
                            onLikeClick = { onLikeClick(post.id) },
                            onUserClick = { onUserClick(post.authorId) },
                            onClick = { onPostClick(post) }
                        )
                    }

                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    personaColor: Color,
    onLikeClick: () -> Unit,
    onUserClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUserClick() }
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = personaColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val displayName = post.authorName ?: post.authorEmail?.substringBefore('@') ?: "User"
                    Text(displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(post.createdAt ?: "Just now", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(post.habitName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = personaColor)
            Text(post.completionNote, style = MaterialTheme.typography.bodyLarge)
            
            if (post.imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUserClick() }
            ) {
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            if (post.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${post.likeCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
                    Icon(
                        Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${post.commentCount} Comments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    comments: List<Comment>,
    isLoadingComments: Boolean,
    onDismiss: () -> Unit,
    onLikeClick: () -> Unit,
    onAddComment: (String) -> Unit,
    onUserClick: () -> Unit
) {
    // Prevent Material3's ModalBottomSheet from auto-collapsing to Hidden when the
    // IME closes and its content remeasures (a known Compose bug) — the explicit
    // close button below remains the guaranteed way to dismiss.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // Material3's own back-press dismissal fires *before* any BackHandler we
        // add inside content can reliably win priority over it (confirmed on
        // device — inconsistent across sheets). Disable it at the source and let
        // our own BackHandler below be the only thing back presses reach.
        properties = ModalBottomSheetDefaults.properties(
            shouldDismissOnBackPress = false
        )
    ) {
        // First back press should only dismiss the keyboard; only close the
        // sheet on a second press once the IME is already hidden.
        val keyboardController = LocalSoftwareKeyboardController.current
        val imeVisible = WindowInsets.isImeVisible
        BackHandler {
            if (imeVisible) keyboardController?.hide() else onDismiss()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Expanded Post Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onUserClick() }
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val displayName = post.authorName ?: post.authorEmail?.substringBefore('@') ?: "User"
                        Text(displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(post.createdAt ?: "Just now", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(post.habitName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(post.completionNote, style = MaterialTheme.typography.bodyLarge)
            
            if (post.imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUserClick() }
            ) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        if (post.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${post.likeCount} Likes", style = MaterialTheme.typography.bodySmall)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Comments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingComments) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            onClick = onUserClick
                        )
                    }
                }
            }

            // Add Comment Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(comment.userId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(comment.text, style = MaterialTheme.typography.bodySmall)
            Text(comment.createdAt ?: "Just now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostBottomSheet(
    onDismiss: () -> Unit,
    onPostCreated: (String, String, Uri?) -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var completionNote by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    // Prevent Material3's ModalBottomSheet from auto-collapsing to Hidden when the
    // IME closes and its content remeasures (a known Compose bug) — the explicit
    // close button below remains the guaranteed way to dismiss.
    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = { it != SheetValue.Hidden },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // Material3's own back-press dismissal fires *before* any BackHandler we
        // add inside content can reliably win priority over it (confirmed on
        // device — inconsistent across sheets). Disable it at the source and let
        // our own BackHandler below be the only thing back presses reach.
        properties = ModalBottomSheetDefaults.properties(
            shouldDismissOnBackPress = false
        )
    ) {
        // First back press should only dismiss the keyboard; only close the
        // sheet on a second press once the IME is already hidden.
        val keyboardController = LocalSoftwareKeyboardController.current
        val imeVisible = WindowInsets.isImeVisible
        BackHandler {
            if (imeVisible) keyboardController?.hide() else onDismiss()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share Your Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = habitName,
                onValueChange = { habitName = it },
                placeholder = { Text("What habit did you complete? (e.g. Morning Run)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = completionNote,
                onValueChange = { completionNote = it },
                placeholder = { Text("How did it go?") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (selectedImageUri != null) {
                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attach Image")
            }

            Button(
                onClick = { onPostCreated(habitName, completionNote, selectedImageUri) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = habitName.isNotBlank() && completionNote.isNotBlank()
            ) {
                Text("Post", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, name = "v2_Chat_Detail_Full")
@Composable
fun PreviewChatDetailV2() {
    HabitFlowTheme {
        val explorerDetails = PersonaUiData.getDetails("Explorer")
        val personaColor = explorerDetails.endColor
        SocialGroupChatScreen(
            chat = ChatResponse("1", "Mountain Climbers", true, "Who's hiking Sunday?", listOf("alex", "mia")),
            messages = emptyList(),
            personaColor = personaColor,
            currentUserId = "me",
            typingUserIds = emptySet(),
            onDismiss = {},
            onSendMessage = {},
            onTypingChanged = {},
            onLikeMessage = {},
            onAddMember = {},
            onRemoveMember = {},
            onLeaveGroup = {},
            onRenameGroup = {},
            onUpdateDescription = {},
            onUpdateVisibility = {},
            onPromoteAdmin = {},
            onDemoteAdmin = {},
            onDeleteGroup = {},
            onUploadGroupPhoto = {}
        )
    }
}

@Preview(showBackground = true, name = "v2_Drawer_Explorer")
@Composable
fun PreviewDrawerExplorerV2() {
    HabitFlowTheme {
        val explorerDetails = PersonaUiData.getDetails("Explorer")
        Surface(modifier = Modifier.width(300.dp)) {
            GroupChatsDrawerContent(
                uiState = SocialUiState(
                    groupChats = listOf(
                        ChatResponse("1", "Mountain Climbers", true, "Who's hiking Sunday?"),
                        ChatResponse("2", "Travel Hackers", true, "Found a cheap flight!")
                    )
                ),
                personaColor = explorerDetails.endColor,
                onChatClick = {},
                onCreateGroupClick = {},
                onJoinGroupClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "v2_Drawer_Grower")
@Composable
fun PreviewDrawerGrowerV2() {
    HabitFlowTheme {
        val growerDetails = PersonaUiData.getDetails("Grower")
        Surface(modifier = Modifier.width(300.dp)) {
            GroupChatsDrawerContent(
                uiState = SocialUiState(
                    groupChats = listOf(
                        ChatResponse("1", "Daily Journaling", true, "What's your win today?"),
                        ChatResponse("2", "Mindfulness Tribe", true, "Just finished 20min meditation")
                    )
                ),
                personaColor = growerDetails.endColor,
                onChatClick = {},
                onCreateGroupClick = {},
                onJoinGroupClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Social Feed - Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Social Feed - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSocialFeedV2() {
    HabitFlowTheme {
        val personaDetails = PersonaUiData.getDetails("Socializer")
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    GroupChatsDrawerContent(
                        uiState = SocialUiState(),
                        personaColor = personaDetails.endColor,
                        onChatClick = {},
                        onCreateGroupClick = {},
                        onJoinGroupClick = {}
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Social Feed", fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(
                                    Icons.Rounded.Forum, 
                                    contentDescription = "Community Chats", 
                                    tint = personaDetails.endColor
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    SocialContent(
                        uiState = SocialUiState(
                            posts = listOf(
                                Post(id = "1", authorId = "Alex", authorEmail = null, authorName = "Alex", habitName = "Deep Work", completionNote = "Just completely crushed my deep work block! 🚀", likes = listOf("1", "2")),
                                Post(id = "2", authorId = "Mia", authorEmail = null, authorName = "Mia", habitName = "Morning Run", completionNote = "Woke up at 5am today. The sunrise was totally worth it.", likes = listOf("1"))
                            )
                        ),
                        personaColor = personaDetails.endColor,
                        onLikeClick = {},
                        onLoadMore = {},
                        onFilterChange = {},
                        onUserClick = { _ -> },
                        onPostClick = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SocialRoutePreview() {
    HabitFlowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SocialContent(
                uiState = SocialUiState(
                    posts = listOf(
                        Post(id = "1", authorId = "Alex", authorEmail = null, authorName = "Alex", habitName = "Deep Work", completionNote = "Just completely crushed my deep work block! 🚀", likes = listOf("1", "2")),
                        Post(id = "2", authorId = "Mia", authorEmail = null, authorName = "Mia", habitName = "Morning Run", completionNote = "Woke up at 5am today. The sunrise was totally worth it.", likes = listOf("1"))
                    )
                ),
                personaColor = Color(0xFF64B5F6),
                onLikeClick = {},
                onLoadMore = {},
                onFilterChange = {},
                onUserClick = { _ -> },
                onPostClick = {}
            )
        }
    }
}
