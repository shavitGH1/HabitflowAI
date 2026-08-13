package com.habitflowai.presentation.ui.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.luminance
import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import java.io.File

// ─────────────────────────────────────────────
// SocialGroupChatScreen
// Dialog wrapper — previews use SocialGroupChatContent directly.
// ─────────────────────────────────────────────
@Composable
fun SocialGroupChatScreen(
    chat: ChatResponse,
    messages: List<ChatMessage>,
    personaColor: Color,
    currentUserId: String = "me",
    typingUserIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit = {},
    onLikeMessage: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    onRenameGroup: (String) -> Unit = {},
    onUpdateDescription: (String) -> Unit = {},
    onPromoteAdmin: (String) -> Unit = {},
    onDemoteAdmin: (String) -> Unit = {},
    onDeleteGroup: () -> Unit = {},
    onUploadGroupPhoto: (android.net.Uri) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SocialGroupChatContent(
            chat = chat,
            messages = messages,
            personaColor = personaColor,
            currentUserId = currentUserId,
            typingUserIds = typingUserIds,
            onDismiss = onDismiss,
            onSendMessage = onSendMessage,
            onTypingChanged = onTypingChanged,
            onLikeMessage = onLikeMessage,
            onAddMember = onAddMember,
            onRemoveMember = onRemoveMember,
            onLeaveGroup = onLeaveGroup,
            onRenameGroup = onRenameGroup,
            onUpdateDescription = onUpdateDescription,
            onPromoteAdmin = onPromoteAdmin,
            onDemoteAdmin = onDemoteAdmin,
            onDeleteGroup = onDeleteGroup,
            onUploadGroupPhoto = onUploadGroupPhoto
        )
    }
}

// ─────────────────────────────────────────────
// SocialGroupChatContent
// The full-screen chat UI, decoupled from Dialog so it can be previewed.
// ─────────────────────────────────────────────
@Composable
fun SocialGroupChatContent(
    chat: ChatResponse,
    messages: List<ChatMessage>,
    personaColor: Color,
    currentUserId: String = "me",
    typingUserIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit = {},
    onLikeMessage: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    onRenameGroup: (String) -> Unit = {},
    onUpdateDescription: (String) -> Unit = {},
    onPromoteAdmin: (String) -> Unit = {},
    onDemoteAdmin: (String) -> Unit = {},
    onDeleteGroup: () -> Unit = {},
    onUploadGroupPhoto: (android.net.Uri) -> Unit = {}
) {
    val isAdmin = currentUserId in chat.admins ||
        currentUserId == chat.owner ||
        (chat.isGroup && chat.admins.isEmpty() && chat.owner == null)
    val isOwner = currentUserId == chat.owner ||
        (chat.isGroup && chat.owner == null && chat.admins.isEmpty())

    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var showDeleteDmConfirm by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            SocialChatTopBar(
                chat = chat,
                personaColor = personaColor,
                onBack = onDismiss,
                onGroupInfo = { showGroupInfoSheet = true },
                onDeleteConversation = { showDeleteDmConfirm = true }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(modifier = Modifier.weight(1f)) {
                SocialMessageList(
                    messages = messages,
                    personaColor = personaColor,
                    currentUserId = currentUserId,
                    onLikeMessage = onLikeMessage
                )
            }

            AnimatedVisibility(visible = typingUserIds.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                SocialTypingIndicator(typingUserIds = typingUserIds, personaColor = personaColor)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SocialChatInput(
                personaColor = personaColor,
                onSendMessage = onSendMessage,
                onTypingChanged = onTypingChanged
            )
        }
    }

    if (showGroupInfoSheet && chat.isGroup) {
        EditGroupSheet(
            chat = chat,
            currentUserId = currentUserId,
            personaColor = personaColor,
            canManage = isAdmin,
            isOwner = isOwner,
            onDismiss = { showGroupInfoSheet = false },
            onRenameGroup = onRenameGroup,
            onUpdateDescription = onUpdateDescription,
            onUploadGroupPhoto = onUploadGroupPhoto,
            onAddMember = { showGroupInfoSheet = false; onAddMember() },
            onRemoveMember = onRemoveMember,
            onPromoteAdmin = onPromoteAdmin,
            onDemoteAdmin = onDemoteAdmin,
            onLeaveGroup = { showGroupInfoSheet = false; onLeaveGroup() },
            onDeleteGroup = { showGroupInfoSheet = false; onDeleteGroup() }
        )
    }

    if (showDeleteDmConfirm) {
        SocialConfirmDialog(
            title = "Delete Conversation",
            message = "Delete this conversation? This cannot be undone.",
            confirmLabel = "Delete",
            personaColor = MaterialTheme.colorScheme.error,
            onDismiss = { showDeleteDmConfirm = false },
            onConfirm = { onDeleteGroup(); showDeleteDmConfirm = false }
        )
    }
}

// ─────────────────────────────────────────────
// SocialChatTopBar — simplified, all editing lives in EditGroupSheet
// ─────────────────────────────────────────────
@Composable
fun SocialChatTopBar(
    chat: ChatResponse,
    personaColor: Color,
    onBack: () -> Unit,
    onGroupInfo: () -> Unit = {},
    onDeleteConversation: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val contentColor = if (personaColor.luminance() > 0.5f) Color.Black else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(personaColor, personaColor.copy(alpha = 0.8f))))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = contentColor)
        }

        // Avatar + name row — tappable for group info
        Row(
            modifier = Modifier
                .weight(1f)
                .then(if (chat.isGroup) Modifier.clickable { onGroupInfo() } else Modifier)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (chat.imageUrl != null) {
                    AsyncImage(
                        model = chat.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(contentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (chat.isGroup) Icons.Rounded.Groups else Icons.Rounded.Person,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = chat.name ?: if (chat.isGroup) "Group Chat" else "Direct Message",
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium
                )
                val subtitle = when {
                    chat.isGroup && chat.participantIds.isNotEmpty() ->
                        "${chat.participantIds.size} members · tap for info"
                    !chat.description.isNullOrBlank() -> chat.description
                    else -> null
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
            }
        }

        // Overflow
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = contentColor)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (chat.isGroup) {
                    DropdownMenuItem(
                        text = { Text("Group Info") },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        onClick = { menuExpanded = false; onGroupInfo() }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Delete Conversation", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDeleteConversation() }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SocialMembersSheet
// Admin-only bottom sheet for member management.
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMembersSheet(
    chat: ChatResponse,
    currentUserId: String,
    personaColor: Color,
    canManage: Boolean = false,
    onDismiss: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteAdmin: (String) -> Unit,
    onDemoteAdmin: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${chat.participantIds.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = personaColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(chat.participantIds) { userId ->
                    val isAlreadyAdmin = userId in chat.admins
                    val isOwnerMark = userId == chat.owner
                    SocialMemberRow(
                        userId = userId,
                        isAdmin = isAlreadyAdmin,
                        isOwner = isOwnerMark,
                        isSelf = userId == currentUserId,
                        personaColor = personaColor,
                        canManage = canManage && userId != currentUserId,
                        onRemove = { onRemoveMember(userId) },
                        onToggleAdmin = {
                            if (isAlreadyAdmin) onDemoteAdmin(userId) else onPromoteAdmin(userId)
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SocialMemberRow
// ─────────────────────────────────────────────
@Composable
fun SocialMemberRow(
    userId: String,
    isAdmin: Boolean,
    isOwner: Boolean = false,
    isSelf: Boolean = false,
    personaColor: Color,
    canManage: Boolean = true,
    onRemove: () -> Unit,
    onToggleAdmin: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialUserAvatar(name = if (isSelf) "me" else userId, personaColor = personaColor, size = 36)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isSelf) "$userId (You)" else userId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            val badge = when {
                isOwner -> "Owner"
                isAdmin -> "Admin"
                else -> null
            }
            if (badge != null) {
                Text(text = badge, style = MaterialTheme.typography.labelSmall, color = personaColor)
            }
        }
        if (canManage) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Member options", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (!isOwner) {
                        DropdownMenuItem(
                            text = { Text(if (isAdmin) "Remove Admin" else "Make Admin") },
                            leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
                            onClick = { menuExpanded = false; onToggleAdmin() }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from Group", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onRemove() }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// EditGroupSheet — WhatsApp-style group info + all edit actions
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupSheet(
    chat: ChatResponse,
    currentUserId: String,
    personaColor: Color,
    canManage: Boolean,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onRenameGroup: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUploadGroupPhoto: (android.net.Uri) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteAdmin: (String) -> Unit,
    onDemoteAdmin: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    val context = LocalContext.current
    var editedName by remember(chat.name) { mutableStateOf(chat.name ?: "") }
    var editedDescription by remember(chat.description) { mutableStateOf(chat.description ?: "") }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createImageFileUri(context)
            pendingCameraUri = uri
        }
        showPhotoDialog = false
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { onUploadGroupPhoto(it) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUploadGroupPhoto(it) }
        showPhotoDialog = false
    }

    LaunchedEffect(pendingCameraUri) {
        pendingCameraUri?.let { cameraLauncher.launch(it) }
    }

    // Photo source dialog (AlertDialog — safe inside ModalBottomSheet)
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Group Photo") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier.weight(1f).height(80.dp).clickable {
                            cameraPermLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                                Text("Camera", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    OutlinedCard(
                        modifier = Modifier.weight(1f).height(80.dp).clickable {
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

    if (showLeaveConfirm) {
        SocialConfirmDialog(
            title = "Leave Group",
            message = "Leave \"${chat.name}\"?",
            confirmLabel = "Leave", personaColor = personaColor,
            onDismiss = { showLeaveConfirm = false },
            onConfirm = { onLeaveGroup() }
        )
    }

    if (showDeleteConfirm) {
        SocialConfirmDialog(
            title = "Delete Group",
            message = "Permanently delete \"${chat.name}\" for all members?",
            confirmLabel = "Delete", personaColor = MaterialTheme.colorScheme.error,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { onDeleteGroup() }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Group Photo ──────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(personaColor.copy(alpha = 0.1f))
                            .then(if (canManage) Modifier.clickable { showPhotoDialog = true } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (chat.imageUrl != null) {
                            AsyncImage(
                                model = chat.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = personaColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        if (canManage) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.PhotoCamera,
                                            contentDescription = "Edit photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Edit",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        chat.name ?: "Group Chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!chat.description.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            chat.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                HorizontalDivider()
            }

            // ── Edit Name (admin only) ────────────────────
            if (canManage) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            "Group Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = personaColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = personaColor,
                                    focusedLabelColor = personaColor
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { if (editedName.isNotBlank()) onRenameGroup(editedName.trim()) },
                                enabled = editedName.isNotBlank() && editedName.trim() != chat.name
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Save name",
                                    tint = if (editedName.isNotBlank() && editedName.trim() != chat.name)
                                        personaColor else MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // ── Edit Description ──────────────────────
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            "Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = personaColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { editedDescription = it },
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                placeholder = { Text("Add a description…") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = personaColor,
                                    focusedLabelColor = personaColor
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onUpdateDescription(editedDescription.trim()) },
                                enabled = editedDescription.trim() != (chat.description ?: "")
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Save description",
                                    tint = if (editedDescription.trim() != (chat.description ?: ""))
                                        personaColor else MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }

            // ── Members Header ────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Members",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = personaColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${chat.participantIds.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Add Member button (admin) ──────────────────
            if (canManage) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddMember() }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(personaColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = personaColor)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Add Members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = personaColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    HorizontalDivider()
                }
            }

            // ── Member list ───────────────────────────────
            items(chat.participantIds) { userId ->
                val isAlreadyAdmin = userId in chat.admins
                val isOwnerMark = userId == chat.owner
                val isSelf = userId == currentUserId
                SocialMemberRow(
                    userId = userId,
                    isAdmin = isAlreadyAdmin,
                    isOwner = isOwnerMark,
                    isSelf = isSelf,
                    personaColor = personaColor,
                    canManage = canManage && !isSelf && !isOwnerMark,
                    onRemove = { onRemoveMember(userId) },
                    onToggleAdmin = {
                        if (isAlreadyAdmin) onDemoteAdmin(userId) else onPromoteAdmin(userId)
                    }
                )
            }

            // ── Danger Zone ───────────────────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = { showLeaveConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Leave Group", color = MaterialTheme.colorScheme.error)
                    }
                    if (isOwner) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Group", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SocialMessageList
// ─────────────────────────────────────────────
@Composable
fun SocialMessageList(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    personaColor: Color,
    currentUserId: String = "me",
    onLikeMessage: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(personaColor.copy(alpha = 0.04f), MaterialTheme.colorScheme.background)))
    ) {
        if (messages.isEmpty()) {
            SocialChatEmptyState(personaColor = personaColor)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    SocialMessageBubble(
                        message = message,
                        personaColor = personaColor,
                        currentUserId = currentUserId,
                        onLikeClick = { onLikeMessage(message.id) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SocialChatEmptyState
// ─────────────────────────────────────────────
@Composable
fun SocialChatEmptyState(personaColor: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.Forum, 
                contentDescription = null, 
                tint = personaColor.copy(alpha = 0.4f), 
                modifier = Modifier.size(48.dp)
            )
            Text(
                "No messages yet", 
                style = MaterialTheme.typography.titleSmall, 
                color = personaColor.copy(alpha = 0.7f)
            )
            Text(
                "Be the first to say hi! 👋", 
                style = MaterialTheme.typography.bodySmall, 
                color = personaColor.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────
// SocialTypingIndicator
// ─────────────────────────────────────────────
@Composable
fun SocialTypingIndicator(typingUserIds: Set<String>, personaColor: Color) {
    val label = when (typingUserIds.size) {
        1 -> "${typingUserIds.first()} is typing…"
        2 -> "${typingUserIds.first()} and ${typingUserIds.last()} are typing…"
        else -> "Several people are typing…"
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.6f)))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────
// SocialMessageBubble
// ─────────────────────────────────────────────
@Composable
fun SocialMessageBubble(
    message: ChatMessage,
    personaColor: Color,
    currentUserId: String = "me",
    onLikeClick: () -> Unit
) {
    val isMe = message.senderId == currentUserId
    val isLiked = message.likedBy.contains(currentUserId)
    val likeCount = message.likedBy.size
    // Users can't like their own messages
    val canLike = !isMe

    val bubbleColor = if (isMe) personaColor else MaterialTheme.colorScheme.surfaceVariant
    val bubbleContentColor = if (isMe) {
        if (personaColor.luminance() > 0.5f) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            SocialUserAvatar(name = message.senderId, personaColor = personaColor, size = 32)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMe) {
                Text(
                    text = message.senderId,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = personaColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.imageUrl != null) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Image",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = bubbleContentColor
                        )
                    }

                    // Like row: only show for other messages, or when there are existing likes
                    if (canLike || likeCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (canLike) {
                                IconButton(onClick = onLikeClick, modifier = Modifier.size(20.dp)) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = if (isLiked) "Unlike" else "Like",
                                        tint = if (isLiked) Color.Red else personaColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (likeCount > 0) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$likeCount",
                                    fontSize = 11.sp,
                                    color = bubbleContentColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isMe) {
            Spacer(modifier = Modifier.width(8.dp))
            SocialUserAvatar(name = "me", personaColor = MaterialTheme.colorScheme.outline, size = 32)
        }
    }
}

// ─────────────────────────────────────────────
// SocialChatInput
// ─────────────────────────────────────────────
@Composable
fun SocialChatInput(
    modifier: Modifier = Modifier,
    personaColor: Color,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }

    // Debounce typing indicator to avoid spamming the socket on every keystroke
    LaunchedEffect(text) {
        if (text.isNotBlank()) {
            onTypingChanged(true)
            kotlinx.coroutines.delay(2000) // Keep "typing" active for 2s after last key
            onTypingChanged(false)
        } else {
            onTypingChanged(false)
        }
    }

    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Type a message…", fontSize = 14.sp) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = personaColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(10.dp))
            val canSend = text.isNotBlank()
            FloatingActionButton(
                onClick = { if (canSend) { onSendMessage(text.trim()); text = ""; onTypingChanged(false) } },
                modifier = Modifier.size(44.dp),
                containerColor = if (canSend) personaColor else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canSend) (if (personaColor.luminance() > 0.5f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// SocialUserAvatar
// ─────────────────────────────────────────────
@Composable
fun SocialUserAvatar(name: String, personaColor: Color, size: Int = 40) {
    val displayInitial = if (name.equals("me", ignoreCase = true)) {
        "M" 
    } else {
        name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
    
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = displayInitial, fontSize = (size * 0.4f).sp, fontWeight = FontWeight.Bold, color = personaColor)
    }
}

// ─────────────────────────────────────────────
// SocialGroupChatListItem — shows unread badge
// ─────────────────────────────────────────────
@Composable
fun SocialGroupChatListItem(
    chat: ChatResponse,
    personaColor: Color,
    currentUserId: String = "me",
    onClick: () -> Unit
) {
    val unreadCount = chat.unreadCount[currentUserId] ?: 0

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chat.imageUrl != null) {
            AsyncImage(model = chat.imageUrl, contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(
                    if (chat.isGroup) Icons.Rounded.Groups else Icons.Rounded.Person,
                    contentDescription = null,
                    tint = personaColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = chat.name ?: "Unnamed Group", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            val subtitle = chat.lastMessage?.takeIf { it.isNotBlank() } ?: chat.description
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (unreadCount > 0) {
            val badgeContentColor = if (personaColor.luminance() > 0.5f) Color.Black else Color.White
            Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(personaColor), contentAlignment = Alignment.Center) {
                Text(text = if (unreadCount > 99) "99+" else "$unreadCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeContentColor)
            }
        } else {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────
// SocialTextInputDialog
// ─────────────────────────────────────────────
@Composable
fun SocialTextInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    personaColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it },
                label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = personaColor)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }, enabled = value.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = personaColor)) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────
// SocialConfirmDialog
// ─────────────────────────────────────────────
@Composable
fun SocialConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    personaColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = personaColor)) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────
// createImageFileUri — FileProvider helper for camera
// ─────────────────────────────────────────────
fun createImageFileUri(context: Context): android.net.Uri {
    // Use external cache if available, as some camera apps struggle with internal cache URIs
    val directory = context.externalCacheDir ?: context.cacheDir
    val file = File(directory, "img_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// ─────────────────────────────────────────────
// ImageSourceDialog — AlertDialog-based photo source picker.
// Safe to use inside ModalBottomSheet (no nested sheets).
// ─────────────────────────────────────────────
@Composable
fun ImageSourceSheet(
    onDismiss: () -> Unit,
    onImageSelected: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createImageFileUri(context)
            pendingCameraUri = uri
        }
        onDismiss()
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { onImageSelected(it) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onImageSelected(it) }
    }

    LaunchedEffect(pendingCameraUri) {
        pendingCameraUri?.let { cameraLauncher.launch(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Photo") },
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
                        onDismiss()
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────
// MemberPickerSheet — searchable list of all app users with checkboxes.
// Falls back to a text-input if the list is empty (e.g. no network).
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPickerSheet(
    title: String,
    allUsers: List<com.habitflowai.data.model.AppUser>,
    /** IDs already in the chat — excluded from the list */
    excludeIds: Set<String> = emptySet(),
    /** Already-selected IDs to pre-check */
    initialSelected: Set<String> = emptySet(),
    /** Multi-select (group) vs single-select (DM / add one member) */
    multiSelect: Boolean = true,
    personaColor: Color,
    onDismiss: () -> Unit,
    /** Returns selected user IDs */
    onConfirm: (Set<String>) -> Unit
) {
    val eligible = remember(allUsers, excludeIds) { allUsers.filter { it.id !in excludeIds } }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initialSelected) }

    val filtered = remember(search, eligible) {
        if (search.isBlank()) eligible
        else eligible.filter { user ->
            val username = user.email.substringBefore('@')
            username.contains(search, ignoreCase = true) || 
            user.email.contains(search, ignoreCase = true) || 
            user.id.contains(search, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    if (multiSelect) {
                        Text(
                            if (selected.isEmpty()) "Select members to add" else "${selected.size} selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected.isNotEmpty()) personaColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (multiSelect && selected.isNotEmpty()) {
                    Button(
                        onClick = { onConfirm(selected); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search bar
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search for friends…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = personaColor) },
                    trailingIcon = if (search.isNotBlank()) {
                        { IconButton(onClick = { search = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") } }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = personaColor
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            // User list
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (eligible.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = CircleShape,
                                    color = personaColor.copy(alpha = 0.1f)
                                ) {
                                    Icon(
                                        Icons.Rounded.PeopleOutline, 
                                        contentDescription = null, 
                                        tint = personaColor.copy(alpha = 0.4f), 
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                                Text(
                                    "No users found", 
                                    style = MaterialTheme.typography.titleMedium, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Invite some friends to get started!", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("No results for \"$search\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                items(filtered, key = { it.id }) { user ->
                    val isSelected = user.id in selected
                    val username = user.email.substringBefore('@')
                    
                    Surface(
                        onClick = {
                            if (!multiSelect) {
                                onConfirm(setOf(user.id)); onDismiss()
                            } else {
                                selected = if (isSelected) selected - user.id else selected + user.id
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) personaColor.copy(alpha = 0.08f) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, personaColor.copy(alpha = 0.3f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SocialUserAvatar(name = username, personaColor = personaColor, size = 44)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(user.email, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                            }
                            if (multiSelect) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { selected = if (it) selected + user.id else selected - user.id },
                                    colors = CheckboxDefaults.colors(checkedColor = personaColor)
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.ChevronRight, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }

            // Confirm button (multi-select)
            if (multiSelect) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = { onConfirm(selected); onDismiss() },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = personaColor,
                            disabledContainerColor = personaColor.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selected.isNotEmpty()) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                if (selected.isEmpty()) "Select Members" else "Confirm Selection (${selected.size})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Previews — use SocialGroupChatContent (no Dialog wrapper)
// ─────────────────────────────────────────────

@Preview(showBackground = true, name = "Chat – Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Chat – Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewChatWithMessages() {
    HabitFlowTheme {
        val personaColor = PersonaUiData.getDetails("Socializer").endColor
        SocialGroupChatContent(
            chat = ChatResponse(
                id = "1", name = "Mountain Climbers", isGroup = true,
                lastMessage = "Who's hiking Sunday?", participantIds = listOf("alex", "mia", "me"),
                admins = listOf("me"), owner = "me"
            ),
            messages = listOf(
                ChatMessage(text = "Hey everyone! Ready for Sunday?", senderId = "alex"),
                ChatMessage(text = "Absolutely! 🏔️", senderId = "mia"),
                ChatMessage(text = "Count me in!", senderId = "me"),
                ChatMessage(text = "7am at the trailhead!", senderId = "alex", likedBy = listOf("me", "mia"))
            ),
            personaColor = personaColor, currentUserId = "me", typingUserIds = setOf("mia"),
            onDismiss = {}, onSendMessage = {}, onLikeMessage = {}, onAddMember = {}
        )
    }
}

@Preview(showBackground = true, name = "Chat – Empty Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewChatEmpty() {
    HabitFlowTheme {
        val personaColor = PersonaUiData.getDetails("Explorer").endColor
        SocialGroupChatContent(
            chat = ChatResponse("2", "Early Risers", true, null, listOf("alex", "me"), description = "5am club 🌅"),
            messages = emptyList(), personaColor = personaColor,
            onDismiss = {}, onSendMessage = {}, onLikeMessage = {}, onAddMember = {}
        )
    }
}

@Preview(showBackground = true, name = "Message bubbles - Light")
@Preview(showBackground = true, name = "Message bubbles - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMessageBubbles() {
    HabitFlowTheme {
        val personaColor = PersonaUiData.getDetails("Grower").endColor
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SocialMessageBubble(ChatMessage(text = "Just hit a new PR! 🏃", senderId = "alex", likedBy = listOf("me")), personaColor) {}
                SocialMessageBubble(ChatMessage(text = "That's awesome, congrats!", senderId = "me"), personaColor) {}
            }
        }
    }
}

@Preview(showBackground = true, name = "Group item - Light")
@Preview(showBackground = true, name = "Group item - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewGroupListItem() {
    HabitFlowTheme {
        val personaColor = PersonaUiData.getDetails("Altruist").endColor
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SocialGroupChatListItem(ChatResponse("1", "Mindfulness Tribe", true, "Today's gratitude ☀️", unreadCount = mapOf("Me" to 3)), personaColor, onClick = {})
                SocialGroupChatListItem(ChatResponse("2", "Marathon Crew", true, "Let's run!"), personaColor, onClick = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "Typing indicator")
@Composable
private fun PreviewTypingIndicator() {
    HabitFlowTheme {
        val personaColor = PersonaUiData.getDetails("Regulator").endColor
        SocialTypingIndicator(typingUserIds = setOf("alex"), personaColor = personaColor)
    }
}
