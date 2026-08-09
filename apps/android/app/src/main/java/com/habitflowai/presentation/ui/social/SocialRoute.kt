package com.habitflowai.presentation.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.SocialUiState
import com.habitflowai.presentation.viewmodel.SocialViewModel
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialRoute(
    viewModel: SocialViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    onToggleChat: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (onboardingState.personaResult == null) {
            onboardingViewModel.fetchProfile()
        }
    }

    val personaType = onboardingState.personaResult?.personaType ?: "Regulator"
    val personaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var showCreateSheet by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var showNewDmSheet by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }
    var selectedChatForView by remember { mutableStateOf<ChatResponse?>(null) }
    var showAddMemberDialog by remember { mutableStateOf<ChatResponse?>(null) }

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
                        scope.launch { drawerState.close() }
                    },
                    onCreateGroupClick = {
                        scope.launch { drawerState.close() }
                        showCreateGroupDialog = true
                    },
                    onJoinGroupClick = { 
                        scope.launch { drawerState.close() }
                        showJoinGroupDialog = true 
                    },
                    onNewDmClick = {
                        scope.launch { drawerState.close() }
                        showNewDmSheet = true
                    },
                    onDeleteDm = { chatId -> viewModel.deleteDirectChat(chatId) }
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Post")
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                SocialContent(
                    uiState = uiState,
                    personaColor = personaDetails.endColor,
                    modifier = Modifier.fillMaxSize(),
                    onLikeClick = viewModel::toggleLike,
                    onLoadMore = viewModel::loadMorePosts,
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
            onPostCreated = { content, imageUri ->
                viewModel.addPost(content, imageUri?.toString())
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
            onCreate = { name, participantIds, imageUri ->
                showCreateGroupDialog = false
                viewModel.createGroup(name, participantIds, imageUri) { newChat ->
                    selectedChatForView = newChat
                    viewModel.loadMessages(newChat.id)
                }
            }
        )
    }

    if (showJoinGroupDialog) {
        JoinGroupDialog(
            personaColor = personaDetails.endColor,
            onDismiss = { showJoinGroupDialog = false },
            onJoin = { id ->
                viewModel.joinGroup(id)
                showJoinGroupDialog = false
            }
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
            }
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
    onNewDmClick: () -> Unit = {},
    onDeleteDm: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Text("Join a Group")
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
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onDeleteDm(chat.id); true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            }
                        ) {
                            SocialGroupChatListItem(chat = chat, personaColor = personaColor, currentUserId = uiState.currentUserId, onClick = { onChatClick(chat) })
                        }
                    }
                }

                if (uiState.groupChats.isEmpty() && uiState.directChats.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text("No chats yet.", color = Color.Gray)
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
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("New Group", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDirectMessageSheet(
    personaColor: Color,
    allUsers: List<com.habitflowai.data.model.AppUser> = emptyList(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    personaColor: Color,
    allUsers: List<com.habitflowai.data.model.AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onCreate: (name: String, participantIds: List<String>, imageUri: Uri?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // rememberSaveable survives camera/gallery activity launches and process death
    var groupName by rememberSaveable { mutableStateOf("") }
    var selectedImageUriStr by rememberSaveable { mutableStateOf("") }
    val selectedImageUri: Uri? = selectedImageUriStr.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
    // Store selectedUsers as comma-separated string (Set<String> not directly saveable)
    var selectedUsersStr by rememberSaveable { mutableStateOf("") }
    val selectedUsers: Set<String> = selectedUsersStr.split(",").filter { it.isNotEmpty() }.toSet()
    var showPhotoDialog by rememberSaveable { mutableStateOf(false) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var pendingCameraUriStr by rememberSaveable { mutableStateOf("") }
    val pendingCameraUri: Uri? = pendingCameraUriStr.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }

    // Camera + gallery launchers at sheet level — no nested ModalBottomSheet
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

    // Member picker — safe to show on top of the sheet (AlertDialog-style state)
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
            title = { Text("Group Photo") },
            text = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedCard(
                        modifier = Modifier.weight(1f).height(80.dp).clickable {
                            cameraPermLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                                Text("Camera", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    OutlinedCard(
                        modifier = Modifier.weight(1f).height(80.dp).clickable {
                            galleryLauncher.launch("image/*")
                            showPhotoDialog = false
                        }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                                Text("Gallery", style = MaterialTheme.typography.labelMedium)
                            }
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
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("New Group", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            // Circular photo picker
            Box(
                modifier = Modifier
                    .size(84.dp)
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
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                } else {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = "Pick photo", tint = personaColor, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (selectedImageUri != null) "Tap to change" else "Add group photo",
                style = MaterialTheme.typography.labelSmall,
                color = personaColor
            )

            Spacer(Modifier.height(20.dp))

            // Group name field
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = personaColor,
                    focusedLabelColor = personaColor
                )
            )

            Spacer(Modifier.height(16.dp))

            // Selected member chips
            if (selectedUsers.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedUsers.toList()) { user ->
                        AssistChip(
                            onClick = { selectedUsersStr = (selectedUsers - user).joinToString(",") },
                            label = { Text(user.take(10) + if (user.length > 10) "…" else "", fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = personaColor.copy(alpha = 0.1f),
                                labelColor = personaColor
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Add Members button — opens MemberPickerSheet
            OutlinedButton(
                onClick = { showMemberPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = personaColor),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(personaColor, personaColor.copy(alpha = 0.5f)))
                )
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (selectedUsers.isEmpty()) "Add Members"
                    else "Members: ${selectedUsers.size} — tap to change"
                )
            }

            Spacer(Modifier.height(8.dp))

            // Create button
            Button(
                onClick = {
                    // Fallback name if user left it blank
                    val fallbackName = if (selectedUsers.isNotEmpty()) {
                        "Group with " + allUsers.find { it.id == selectedUsers.first() }?.email?.substringBefore('@')
                    } else "New Group"
                    
                    val nameToSave = groupName.trim().ifBlank { fallbackName }
                    val membersToSave = selectedUsers.toList()
                    val imageToSave = selectedImageUri
                    // Reset form before calling onCreate so state is clean if sheet is reopened
                    groupName = ""
                    selectedUsersStr = ""
                    selectedImageUriStr = ""
                    onCreate(nameToSave, membersToSave, imageToSave)
                },
                enabled = groupName.isNotBlank() || selectedUsers.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Create Group${if (selectedUsers.isNotEmpty()) " (${selectedUsers.size} members)" else ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun JoinGroupDialog(
    personaColor: Color,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var groupId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join a Group") },
        text = {
            Column {
                Text("Enter the group ID or name to search.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = groupId,
                    onValueChange = { groupId = it },
                    label = { Text("Group ID/Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(groupId) },
                enabled = groupId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = personaColor)
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
    chat: ChatResponse,
    personaColor: Color,
    allUsers: List<com.habitflowai.data.model.AppUser> = emptyList(),
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
    onLikeClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
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
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = uiState.posts,
                    key = { _, post -> post.id }
                ) { _, post ->
                    PostCard(
                        post = post,
                        personaColor = personaColor,
                        onLikeClick = { onLikeClick(post.id) },
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

@Composable
fun PostCard(
    post: Post,
    personaColor: Color,
    onLikeClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = personaColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(post.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Just now", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(post.content, style = MaterialTheme.typography.bodyLarge)
            
            if (post.hasPhoto || post.imageUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (post.imageUri != null) {
                    AsyncImage(
                        model = post.imageUri,
                        contentDescription = "Post image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    comments: List<Comment>,
    isLoadingComments: Boolean,
    onDismiss: () -> Unit,
    onLikeClick: () -> Unit,
    onAddComment: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
        ) {
            // Expanded Post Content
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(post.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Just now", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(post.content, style = MaterialTheme.typography.bodyLarge)
            
            if (post.imageUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = post.imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        CommentItem(comment)
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
fun CommentItem(comment: Comment) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(comment.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(comment.content, style = MaterialTheme.typography.bodySmall)
            Text(comment.timestamp, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostBottomSheet(
    onDismiss: () -> Unit,
    onPostCreated: (String, Uri?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Share Your Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
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
                onClick = { onPostCreated(content, selectedImageUri) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = content.isNotBlank()
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
            onDismiss = {},
            onSendMessage = {},
            onLikeMessage = {},
            onAddMember = {}
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
@Preview(showBackground = true, name = "v2_Social_Feed_Main")
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
                                Post(1, "Alex", "Just completely crushed my deep work block! \ud83d\ude80", true, likeCount = 14),
                                Post(2, "Mia", "Woke up at 5am today. The sunrise was totally worth it.", false, likeCount = 5)
                            )
                        ),
                        personaColor = personaDetails.endColor,
                        onLikeClick = {},
                        onLoadMore = {},
                        onPostClick = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SocialRoutePreview() {
    SocialContent(
        uiState = SocialUiState(
            posts = listOf(
                Post(1, "Alex", "Just completely crushed my deep work block! \ud83d\ude80", true, likeCount = 14),
                Post(2, "Mia", "Woke up at 5am today. The sunrise was totally worth it.", false, likeCount = 5)
            )
        ),
        personaColor = Color(0xFF64B5F6),
        onLikeClick = {},
        onLoadMore = {},
        onPostClick = {}
    )
}
