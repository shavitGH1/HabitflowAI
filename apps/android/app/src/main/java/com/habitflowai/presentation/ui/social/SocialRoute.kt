package com.habitflowai.presentation.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import com.habitflowai.data.model.ChatUiState
import com.habitflowai.presentation.ui.chat.ChatOverlay
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
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
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
                    onCreateGroupClick = { showCreateGroupDialog = true },
                    onJoinGroupClick = { showJoinGroupDialog = true }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Social Feed", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { 
                            viewModel.loadGroupChats()
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
            SocialContent(
                uiState = uiState,
                personaColor = personaDetails.endColor,
                modifier = Modifier.padding(paddingValues),
                onLikeClick = viewModel::toggleLike,
                onLoadMore = viewModel::loadMorePosts,
                onPostClick = { post ->
                    selectedPostForComments = post
                    viewModel.loadComments(post.id)
                },
                onLikeMessage = { messageId -> /* Handle if needed from feed, though usually in dialog */ }
            )
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
        ChatGroupDetailDialog(
            chat = selectedChatForView!!,
            messages = uiState.chatMessages[selectedChatForView!!.id] ?: emptyList(),
            personaColor = personaDetails.endColor,
            onDismiss = { selectedChatForView = null },
            onSendMessage = { content -> viewModel.sendMessage(selectedChatForView!!.id, content) },
            onLikeMessage = { messageId -> viewModel.toggleMessageLike(selectedChatForView!!.id, messageId) },
            onAddMember = { 
                viewModel.loadUsers()
                showAddMemberDialog = selectedChatForView 
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            personaColor = personaDetails.endColor,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name ->
                viewModel.createGroup(name)
                showCreateGroupDialog = false
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

    if (showAddMemberDialog != null) {
        AddMemberDialog(
            chat = showAddMemberDialog!!,
            users = uiState.appUsers,
            personaColor = personaDetails.endColor,
            onDismiss = { showAddMemberDialog = null },
            onAdd = { name ->
                viewModel.addMember(showAddMemberDialog!!.id, name)
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

@Composable
fun GroupChatsDrawerContent(
    uiState: SocialUiState,
    personaColor: Color,
    onChatClick: (ChatResponse) -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit
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
        
        Spacer(modifier = Modifier.height(24.dp))

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
        } else if (uiState.groupChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No groups found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(uiState.groupChats) { chat ->
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(chat.name ?: "Unnamed Group", fontWeight = FontWeight.Bold)
                                Text(chat.lastMessage ?: "No messages yet", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                            }
                        },
                        selected = false,
                        onClick = { onChatClick(chat) },
                        icon = {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Groups, contentDescription = null, tint = personaColor)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
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

@Composable
fun ChatGroupDetailDialog(
    chat: ChatResponse,
    messages: List<com.habitflowai.data.model.ChatMessage>,
    personaColor: Color,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onLikeMessage: (String) -> Unit,
    onAddMember: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Custom Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(personaColor, personaColor.copy(alpha = 0.8f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ChatGroupDetailHeader(chat = chat, personaColor = Color.White)
                    }
                    IconButton(onClick = onAddMember) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Member", tint = Color.White)
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Chat Content
                Box(modifier = Modifier.weight(1f)) {
                    ChatGroupDetailContent(
                        chat = chat, 
                        messages = messages, 
                        personaColor = personaColor,
                        onLikeMessage = onLikeMessage
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Message Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                onSendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send, 
                            contentDescription = "Send", 
                            tint = if (messageText.isNotBlank()) personaColor else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatGroupDetailHeader(
    chat: ChatResponse,
    personaColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(personaColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Groups, contentDescription = null, tint = personaColor)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(chat.name ?: "Group Chat", fontWeight = FontWeight.Bold, color = Color.White)
            if (chat.participantIds.isNotEmpty()) {
                Text(
                    "${chat.participantIds.size} members", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ChatGroupDetailContent(
    chat: ChatResponse,
    messages: List<com.habitflowai.data.model.ChatMessage>,
    personaColor: Color,
    onLikeMessage: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        personaColor.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Gray.copy(alpha = 0.03f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No messages yet. Say hi!", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageBubble(message, personaColor, onLikeMessage)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: com.habitflowai.data.model.ChatMessage,
    personaColor: Color,
    onLikeClick: (String) -> Unit
) {
    val isMe = message.senderId == "Me"
    val isLiked = message.likedBy.contains("Me") // Simulating "Me" as current user ID

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(personaColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = personaColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            if (!isMe) {
                Text(
                    text = message.senderId,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = personaColor
                )
            }
            
            Surface(
                color = if (isMe) personaColor else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(
                    topStart = if (isMe) 16.dp else 0.dp,
                    topEnd = if (isMe) 0.dp else 16.dp,
                    bottomEnd = 16.dp,
                    bottomStart = 16.dp
                ),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(
                        if (isMe) {
                            Brush.linearGradient(listOf(personaColor, personaColor.copy(alpha = 0.7f)))
                        } else {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f), MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)))
                        },
                        RoundedCornerShape(topStart = if (isMe) 16.dp else 0.dp, topEnd = if (isMe) 0.dp else 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    if (message.likedBy.isNotEmpty() || !isMe) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onLikeClick(message.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color.Red else (if (isMe) Color.White.copy(alpha = 0.7f) else personaColor.copy(alpha = 0.7f)),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (message.likedBy.isNotEmpty()) {
                                Text(
                                    "${message.likedBy.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isMe) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    personaColor: Color,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(groupName) },
                enabled = groupName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = personaColor)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

@Composable
fun AddMemberDialog(
    chat: ChatResponse,
    users: List<String>,
    personaColor: Color,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = remember(searchQuery, users) {
        if (searchQuery.isBlank()) users
        else users.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${chat.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by username...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (filteredUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No users found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredUsers) { username ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAdd(username) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = personaColor, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(username, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = personaColor)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
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
    onPostClick: (Post) -> Unit,
    onLikeMessage: (String) -> Unit
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
        var messageText by remember { mutableStateOf("") }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Simulated Top Bar with Gradient
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(personaColor, personaColor.copy(alpha = 0.8f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ChatGroupDetailHeader(
                            chat = ChatResponse("1", "Mountain Climbers", true, "Who's hiking Sunday?"),
                            personaColor = Color.White
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Member", tint = Color.White)
                    }
                }
                
                // Chat Content
                Box(modifier = Modifier.weight(1f)) {
                    ChatGroupDetailContent(
                        chat = ChatResponse("1", "Mountain Climbers", true, "Who's hiking Sunday?"),
                        messages = emptyList(),
                        personaColor = personaColor,
                        onLikeMessage = {}
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Simulated Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.Gray)
                    }
                }
            }
        }
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
                                Post(1, "Alex", "Just completely crushed my deep work block! 🚀", true, likeCount = 14),
                                Post(2, "Mia", "Woke up at 5am today. The sunrise was totally worth it.", false, likeCount = 5)
                            )
                        ),
                        personaColor = personaDetails.endColor,
                        onLikeClick = {},
                        onLoadMore = {},
                        onPostClick = {},
                        onLikeMessage = {}
                    )
                    ChatOverlay(
                        uiState = ChatUiState(personaType = "Socializer"),
                        onToggleChat = {},
                        onInputChanged = {},
                        onSendMessage = {}
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
                Post(1, "Alex", "Just completely crushed my deep work block! 🚀", true, likeCount = 14),
                Post(2, "Mia", "Woke up at 5am today. The sunrise was totally worth it.", false, likeCount = 5)
            )
        ),
        personaColor = Color(0xFF64B5F6),
        onLikeClick = {},
        onLoadMore = {},
        onPostClick = {},
        onLikeMessage = {}
    )
}
