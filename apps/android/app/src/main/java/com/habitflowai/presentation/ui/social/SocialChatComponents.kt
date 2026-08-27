package com.habitflowai.presentation.ui.social

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.style.TextOverflow
import com.habitflowai.util.formatChatDateSeparator
import com.habitflowai.util.formatChatListTime
import com.habitflowai.util.formatMessageTime
import com.habitflowai.util.isDifferentDay
import com.habitflowai.util.parseLocationShareLink

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.luminance
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.habitflowai.data.model.AppUser
import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.model.ChatResponse
import com.habitflowai.data.model.resolveProfilePicture
import com.habitflowai.presentation.viewmodel.LocationSharePreview
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.profile.PresetAvatars
import com.habitflowai.presentation.ui.theme.*
import java.io.File

fun resolveDisplayName(userId: String, allUsers: List<AppUser>): String {
    val user = allUsers.find { it.id == userId } ?: return userId
    return if (user.firstName != null || user.lastName != null) {
        "${user.firstName ?: ""} ${user.lastName ?: ""}".trim()
    } else {
        user.email.substringBefore('@')
    }
}

fun resolveFirstName(userId: String, allUsers: List<AppUser>): String {
    val user = allUsers.find { it.id == userId } ?: return userId
    return user.firstName?.takeIf { it.isNotBlank() } ?: user.email.substringBefore('@')
}

@Composable
fun SocialGroupChatScreen(
    chat: ChatResponse,
    messages: List<ChatMessage>,
    personaColor: Color,
    currentUserId: String = "me",
    typingUserIds: Set<String> = emptySet(),
    allUsers: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri) -> Unit = {},
    onSendLocation: (address: String?) -> Unit = {},
    locationPreview: LocationSharePreview? = null,
    onConfirmLocationShare: () -> Unit = {},
    onDismissLocationPreview: () -> Unit = {},
    onTypingChanged: (Boolean) -> Unit = {},
    onLikeMessage: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    onRenameGroup: (String) -> Unit = {},
    onUpdateDescription: (String) -> Unit = {},
    onUpdateVisibility: (Boolean) -> Unit = {},
    onPromoteAdmin: (String) -> Unit = {},
    onDemoteAdmin: (String) -> Unit = {},
    onDeleteGroup: () -> Unit = {},
    onUploadGroupPhoto: (Uri) -> Unit = {},
    error: com.habitflowai.presentation.viewmodel.SocialUiError? = null,
    onDismissError: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // A Dialog gets its own Android window, separate from the Activity's — it doesn't
        // inherit enableEdgeToEdge() or resize-mode from MainActivity, and defaults to
        // SOFT_INPUT_ADJUST_PAN. That's incompatible with imePadding() below (which expects
        // a resized window, not a panned one), and is what stranded the input near the top
        // with dead space underneath. Force this dialog's own window into resize mode.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        SocialGroupChatContent(
            chat = chat,
            messages = messages,
            personaColor = personaColor,
            currentUserId = currentUserId,
            typingUserIds = typingUserIds,
            allUsers = allUsers,
            onDismiss = onDismiss,
            onSendMessage = onSendMessage,
            onSendImage = onSendImage,
            onSendLocation = onSendLocation,
            locationPreview = locationPreview,
            onConfirmLocationShare = onConfirmLocationShare,
            onDismissLocationPreview = onDismissLocationPreview,
            onTypingChanged = onTypingChanged,
            onLikeMessage = onLikeMessage,
            onAddMember = onAddMember,
            onRemoveMember = onRemoveMember,
            onLeaveGroup = onLeaveGroup,
            onRenameGroup = onRenameGroup,
            onUpdateDescription = onUpdateDescription,
            onUpdateVisibility = onUpdateVisibility,
            onPromoteAdmin = onPromoteAdmin,
            onDemoteAdmin = onDemoteAdmin,
            onDeleteGroup = onDeleteGroup,
            onUploadGroupPhoto = onUploadGroupPhoto,
            error = error,
            onDismissError = onDismissError
        )
    }
}

@Composable
fun SocialGroupChatContent(
    chat: ChatResponse,
    messages: List<ChatMessage>,
    personaColor: Color,
    currentUserId: String = "me",
    typingUserIds: Set<String> = emptySet(),
    allUsers: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri) -> Unit = {},
    onSendLocation: (address: String?) -> Unit = {},
    locationPreview: LocationSharePreview? = null,
    onConfirmLocationShare: () -> Unit = {},
    onDismissLocationPreview: () -> Unit = {},
    onTypingChanged: (Boolean) -> Unit = {},
    onLikeMessage: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    onRenameGroup: (String) -> Unit = {},
    onUpdateDescription: (String) -> Unit = {},
    onUpdateVisibility: (Boolean) -> Unit = {},
    onPromoteAdmin: (String) -> Unit = {},
    onDemoteAdmin: (String) -> Unit = {},
    onDeleteGroup: () -> Unit = {},
    onUploadGroupPhoto: (Uri) -> Unit = {},
    error: com.habitflowai.presentation.viewmodel.SocialUiError? = null,
    onDismissError: () -> Unit = {}
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
                currentUserId = currentUserId,
                allUsers = allUsers,
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
                    allUsers = allUsers,
                    onLikeMessage = onLikeMessage
                )
            }

            AnimatedVisibility(visible = typingUserIds.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                SocialTypingIndicator(typingUserIds = typingUserIds, allUsers = allUsers, personaColor = personaColor)
            }

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                error?.let { SocialErrorBanner(error = it, personaColor = personaColor, onDismiss = onDismissError) }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SocialChatInput(
                personaColor = personaColor,
                onSendMessage = onSendMessage,
                onSendImage = onSendImage,
                onSendLocation = onSendLocation,
                onTypingChanged = onTypingChanged,
                chatMembers = remember(allUsers, chat.participantIds) { allUsers.filter { it.id in chat.participantIds } }
            )
        }
    }

    if (showGroupInfoSheet && chat.isGroup) {
        EditGroupSheet(
            chat = chat,
            currentUserId = currentUserId,
            personaColor = personaColor,
            allUsers = allUsers,
            canManage = isAdmin,
            isOwner = isOwner,
            onDismiss = { showGroupInfoSheet = false },
            onRenameGroup = onRenameGroup,
            onUpdateDescription = onUpdateDescription,
            onUpdateVisibility = onUpdateVisibility,
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

    if (locationPreview != null) {
        LocationPreviewSheet(
            preview = locationPreview,
            personaColor = personaColor,
            onConfirm = onConfirmLocationShare,
            onDismiss = onDismissLocationPreview
        )
    }
}

/**
 * Shows the resolved pin on a small map (plus the Geocoder's own formatted address, when
 * available) before a location share actually gets sent — a typed address can geocode to
 * the wrong city, and this is the point where the user can catch that and back out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPreviewSheet(
    preview: LocationSharePreview,
    personaColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val position = LatLng(preview.latitude, preview.longitude)
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 15f)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                text = "Share this location?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = preview.displayAddress ?: "Your current location",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = false,
                        tiltGesturesEnabled = false,
                        rotationGesturesEnabled = false,
                        myLocationButtonEnabled = false
                    )
                ) {
                    Marker(state = MarkerState(position = position))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = personaColor)
                ) {
                    Text(
                        "Share",
                        color = if (personaColor.luminance() > 0.5f) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SocialChatTopBar(
    chat: ChatResponse,
    personaColor: Color,
    currentUserId: String = "me",
    allUsers: List<AppUser> = emptyList(),
    onBack: () -> Unit,
    onGroupInfo: () -> Unit = {},
    onDeleteConversation: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val contentColor = if (personaColor.luminance() > 0.5f) Color.Black else Color.White
    // A DM never has its own chat.name — resolve the other participant instead of
    // falling back to a generic "Direct Message" label (this previously never even
    // attempted resolution, unlike the chat list row and message bubbles).
    val otherParticipant = if (!chat.isGroup) {
        chat.participantIds.find { it != currentUserId }?.let { id -> allUsers.find { it.id == id } }
    } else null
    val displayName = chat.name ?: if (chat.isGroup) {
        "Group Chat"
    } else {
        chat.participantIds.find { it != currentUserId }?.let { resolveDisplayName(it, allUsers) } ?: "Direct Message"
    }

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
                when {
                    chat.imageUrl != null -> AsyncImage(
                        model = chat.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    !chat.isGroup -> SocialUserAvatar(name = displayName, personaColor = personaColor, size = 40, profilePicture = otherParticipant?.profilePicture)
                    else -> Box(
                        modifier = Modifier.fillMaxSize().background(contentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = displayName,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMembersSheet(
    chat: ChatResponse,
    currentUserId: String,
    personaColor: Color,
    allUsers: List<AppUser> = emptyList(),
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
                    text = "${chat.participantIds.size}",
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
                        displayName = resolveDisplayName(userId, allUsers),
                        profilePicture = allUsers.find { it.id == userId }?.profilePicture,
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

@Composable
fun SocialMemberRow(
    userId: String,
    displayName: String = userId,
    profilePicture: String? = null,
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
        SocialUserAvatar(name = if (isSelf) "me" else displayName, personaColor = personaColor, size = 36, profilePicture = profilePicture)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isSelf) "$displayName (You)" else displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupSheet(
    chat: ChatResponse,
    currentUserId: String,
    personaColor: Color,
    allUsers: List<AppUser> = emptyList(),
    canManage: Boolean,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onRenameGroup: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateVisibility: (Boolean) -> Unit,
    onUploadGroupPhoto: (Uri) -> Unit,
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
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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
                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = personaColor, modifier = Modifier.size(48.dp))
                        }
                        if (canManage) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(text = chat.name ?: "Group Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (!chat.description.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = chat.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                HorizontalDivider()
            }

            if (canManage) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text("Group Name", style = MaterialTheme.typography.labelMedium, color = personaColor, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = personaColor, focusedLabelColor = personaColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { if (editedName.isNotBlank()) onRenameGroup(editedName.trim()) },
                                enabled = editedName.isNotBlank() && editedName.trim() != chat.name
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Save name",
                                    tint = if (editedName.isNotBlank() && editedName.trim() != chat.name) personaColor else MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text("Description", style = MaterialTheme.typography.labelMedium, color = personaColor, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { editedDescription = it },
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                placeholder = { Text("Add a description…") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = personaColor, focusedLabelColor = personaColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onUpdateDescription(editedDescription.trim()) },
                                enabled = editedDescription.trim() != (chat.description ?: "")
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Save description",
                                    tint = if (editedDescription.trim() != (chat.description ?: "")) personaColor else MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Public Group", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Anyone can find and join this group via search.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = chat.isPublic,
                                onCheckedChange = { onUpdateVisibility(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = personaColor)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Members", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = personaColor, modifier = Modifier.weight(1f))
                    Text(text = "${chat.participantIds.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (canManage) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onAddMember() }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = personaColor)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("Add Members", style = MaterialTheme.typography.bodyMedium, color = personaColor, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider()
                }
            }

            items(chat.participantIds) { userId ->
                val isAlreadyAdmin = userId in chat.admins
                val isOwnerMark = userId == chat.owner
                val isSelf = userId == currentUserId
                SocialMemberRow(
                    userId = userId,
                    displayName = resolveDisplayName(userId, allUsers),
                    isAdmin = isAlreadyAdmin,
                    isOwner = isOwnerMark,
                    isSelf = isSelf,
                    personaColor = personaColor,
                    canManage = canManage && !isSelf && !isOwnerMark,
                    onRemove = { onRemoveMember(userId) },
                    onToggleAdmin = { if (isAlreadyAdmin) onDemoteAdmin(userId) else onPromoteAdmin(userId) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showLeaveConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Leave Group", color = MaterialTheme.colorScheme.error)
                    }
                    if (isOwner) {
                        TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Group", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialMessageList(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    personaColor: Color,
    currentUserId: String = "me",
    allUsers: List<AppUser> = emptyList(),
    onLikeMessage: (String) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Box(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(personaColor.copy(alpha = 0.04f), MaterialTheme.colorScheme.background)))) {
        if (messages.isEmpty()) {
            SocialChatEmptyState(personaColor = personaColor)
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                    if (index == 0 || isDifferentDay(messages[index - 1].timestamp, message.timestamp)) {
                        ChatDateSeparator(formatChatDateSeparator(message.timestamp))
                    }
                    SocialMessageBubble(
                        message = message,
                        senderDisplayName = resolveDisplayName(message.senderId, allUsers),
                        senderProfilePicture = allUsers.find { it.id == message.senderId }?.profilePicture,
                        personaColor = personaColor,
                        currentUserId = currentUserId,
                        allUsers = allUsers,
                        onLikeClick = { onLikeMessage(message.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SocialChatEmptyState(personaColor: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Forum, contentDescription = null, tint = personaColor.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
            Text("No messages yet", style = MaterialTheme.typography.titleSmall, color = personaColor.copy(alpha = 0.7f))
            Text("Be the first to say hi! 👋", style = MaterialTheme.typography.bodySmall, color = personaColor.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun SocialTypingIndicator(typingUserIds: Set<String>, allUsers: List<AppUser> = emptyList(), personaColor: Color) {
    val names = typingUserIds.map { resolveDisplayName(it, allUsers) }
    val label = when (names.size) {
        1 -> "${names.first()} is typing…"
        2 -> "${names.first()} and ${names.last()} are typing…"
        else -> "Several people are typing…"
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.6f)))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SocialMessageBubble(
    message: ChatMessage,
    senderDisplayName: String = message.senderId,
    senderProfilePicture: String? = null,
    personaColor: Color,
    currentUserId: String = "me",
    allUsers: List<AppUser> = emptyList(),
    onLikeClick: () -> Unit
) {
    val isMe = message.senderId == currentUserId
    val isLiked = message.likedBy.contains(currentUserId)
    val likeCount = message.likedBy.size
    val bubbleColor = if (isMe) personaColor else MaterialTheme.colorScheme.surfaceVariant
    val bubbleContentColor = if (isMe) (if (personaColor.luminance() > 0.5f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
    val sharedLocation = remember(message.text) { parseLocationShareLink(message.text) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!isMe) {
            SocialUserAvatar(name = senderDisplayName, personaColor = personaColor, size = 32, profilePicture = senderProfilePicture)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 280.dp)) {
            if (!isMe) {
                Text(text = senderDisplayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = personaColor, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            Surface(color = bubbleColor, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMe) 16.dp else 4.dp, bottomEnd = if (isMe) 4.dp else 16.dp)) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.imageUrl != null) {
                        AsyncImage(model = message.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                    }
                    when {
                        sharedLocation != null -> LocationMessageChip(
                            lat = sharedLocation.first,
                            lng = sharedLocation.second,
                            contentColor = bubbleContentColor
                        )
                        message.text.isNotBlank() -> MentionAwareText(
                            text = message.text,
                            allUsers = allUsers,
                            color = bubbleContentColor,
                            personaColor = personaColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            // Like row and timestamp both live outside the bubble Surface now — they're
            // metadata about the message, not part of its content, so they shouldn't
            // share the bubble's background/padding.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)) {
                IconButton(onClick = onLikeClick, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = if (isLiked) "Unlike" else "Like", tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
                if (likeCount > 0) {
                    Text(text = "$likeCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
        if (isMe) {
            Spacer(modifier = Modifier.width(8.dp))
            SocialUserAvatar(name = "me", personaColor = MaterialTheme.colorScheme.outline, size = 32, profilePicture = senderProfilePicture)
        }
    }
}

/**
 * Renders [text] with any "@Name" substring matching a real chat participant highlighted —
 * a plain-text highlight, not a structured mention (no backend field, no push notification;
 * see Task 62's "Tag a group member" scope). Longer names are matched first so a short
 * name that's a prefix of a longer one (e.g. "@Al" vs "@Alex") never eats part of it.
 */
@Composable
fun MentionAwareText(text: String, allUsers: List<AppUser>, color: Color, personaColor: Color, style: TextStyle) {
    val mentionTokens = remember(allUsers) {
        allUsers.mapNotNull { user ->
            val name = resolveDisplayName(user.id, allUsers)
            if (name.isNotBlank()) "@$name" else null
        }.sortedByDescending { it.length }
    }
    val annotated = remember(text, mentionTokens) {
        buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                val match = if (text[i] == '@') mentionTokens.firstOrNull { text.startsWith(it, i) } else null
                if (match != null) {
                    withStyle(SpanStyle(color = personaColor, fontWeight = FontWeight.Bold)) { append(match) }
                    i += match.length
                } else {
                    append(text[i])
                    i++
                }
            }
        }
    }
    Text(text = annotated, style = style, color = color)
}

@Composable
fun LocationMessageChip(lat: Double, lng: Double, contentColor: Color) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                )
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = contentColor)
        Text(
            text = "Shared location — tap to open in Maps",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun ChatDateSeparator(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SocialChatInput(
    modifier: Modifier = Modifier,
    personaColor: Color,
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri) -> Unit = {},
    onSendLocation: (address: String?) -> Unit = {},
    onTypingChanged: (Boolean) -> Unit = {},
    chatMembers: List<AppUser> = emptyList()
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var showImageSource by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showMentionPicker by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    // Only requested when the user actually picks "Use current location" — typing an
    // address never needs it, so asking up front (e.g. on opening the attach menu)
    // would be a permission prompt for a feature they might not even use.
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onSendLocation(null)
    }
    LaunchedEffect(text) {
        if (text.isNotBlank()) {
            onTypingChanged(true)
            kotlinx.coroutines.delay(2000)
            onTypingChanged(false)
        } else {
            onTypingChanged(false)
        }
        // Typing "@" opens the member picker — reuses MemberPickerSheet in single-select
        // mode. Highlight-only mention (no push notification, no structured backend field),
        // matching a plain "@Name" substring at render time in SocialMessageBubble.
        if (text.endsWith("@") && chatMembers.isNotEmpty()) {
            showMentionPicker = true
        }
    }
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { showAttachMenu = true }) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = "Attach", tint = personaColor)
                }
                DropdownMenu(expanded = showAttachMenu, onDismissRequest = { showAttachMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Photo") },
                        leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null) },
                        onClick = { showAttachMenu = false; showImageSource = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Location") },
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                        onClick = { showAttachMenu = false; showLocationDialog = true }
                    )
                }
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it }, placeholder = { Text("Type a message…", fontSize = 14.sp) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 120.dp), shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = personaColor, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(10.dp))
            val canSend = text.isNotBlank()
            FloatingActionButton(
                onClick = { if (canSend) { onSendMessage(text.trim()); text = ""; onTypingChanged(false) } },
                modifier = Modifier.size(44.dp), containerColor = if (canSend) personaColor else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canSend) (if (personaColor.luminance() > 0.5f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape, elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showImageSource) {
        ImageSourceSheet(
            onDismiss = { showImageSource = false },
            onImageSelected = { uri -> showImageSource = false; onSendImage(uri) }
        )
    }

    if (showMentionPicker) {
        MemberPickerSheet(
            title = "Mention someone",
            allUsers = chatMembers,
            multiSelect = false,
            personaColor = personaColor,
            onDismiss = { showMentionPicker = false; if (text.endsWith("@")) text = text.dropLast(1) },
            onConfirm = { selected ->
                showMentionPicker = false
                val userId = selected.firstOrNull()
                val name = chatMembers.find { it.id == userId }?.let { resolveDisplayName(it.id, chatMembers) }
                if (name != null) text = "${text.dropLast(1)}@$name "
            }
        )
    }

    if (showLocationDialog) {
        var addressInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Share Location") },
            text = {
                Column {
                    Text(
                        "Enter an address to share, or leave blank to share your current location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Address (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = personaColor)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationDialog = false
                        if (addressInput.isBlank()) {
                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                onSendLocation(null)
                            } else {
                                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        } else {
                            onSendLocation(addressInput.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = personaColor)
                ) {
                    Text(if (addressInput.isBlank()) "Use Current Location" else "Preview")
                }
            },
            dismissButton = { TextButton(onClick = { showLocationDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SocialUserAvatar(name: String, personaColor: Color, size: Int = 40, profilePicture: String? = null) {
    val presetDrawable = PresetAvatars.drawableFor(profilePicture)
    Box(modifier = Modifier.size(size.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        when {
            presetDrawable != null -> Image(
                painter = painterResource(presetDrawable),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            !profilePicture.isNullOrBlank() -> AsyncImage(
                model = resolveProfilePicture(profilePicture),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> {
                val displayInitial = if (name.equals("me", ignoreCase = true)) "M" else name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                Text(text = displayInitial, fontSize = (size * 0.4f).sp, fontWeight = FontWeight.Bold, color = personaColor)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocialGroupChatListItem(
    chat: ChatResponse,
    personaColor: Color,
    currentUserId: String = "me",
    allUsers: List<AppUser> = emptyList(),
    onClick: () -> Unit,
    onTogglePin: () -> Unit = {},
    onToggleMute: () -> Unit = {}
) {
    val unreadCount = chat.unreadCount[currentUserId] ?: 0
    val isPinned = chat.pinnedBy.contains(currentUserId)
    val isMuted = chat.mutedBy.contains(currentUserId)
    var showMenu by remember { mutableStateOf(false) }
    // A direct chat never has its own name — a nameless *group* is the unusual case worth
    // flagging as "Unnamed Group"; a nameless DM should just show the other participant
    // (this is where the auto-created coach chat lands).
    val otherParticipant = if (!chat.isGroup) {
        chat.participantIds.find { it != currentUserId }?.let { id -> allUsers.find { it.id == id } }
    } else null
    val displayName = chat.name ?: if (chat.isGroup) {
        "Unnamed Group"
    } else {
        chat.participantIds.find { it != currentUserId }?.let { resolveDisplayName(it, allUsers) } ?: "Direct Message"
    }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                chat.imageUrl != null -> AsyncImage(model = chat.imageUrl, contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                // DMs have no imageUrl of their own (that's a group-photo concept) — show
                // the other participant's actual profile picture instead of a generic icon.
                !chat.isGroup -> SocialUserAvatar(name = displayName, personaColor = personaColor, size = 44, profilePicture = otherParticipant?.profilePicture)
                else -> Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(personaColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = personaColor)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = personaColor, modifier = Modifier.size(14.dp))
                    }
                    if (isMuted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Rounded.NotificationsOff, contentDescription = "Muted", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                    val timeLabel = formatChatListTime(chat.lastMessageAt)
                    if (timeLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                val lastMessage = chat.lastMessage?.takeIf { it.isNotBlank() }
                val subtitle = if (lastMessage != null) {
                    val senderPrefix = chat.lastMessageSenderId?.let { senderId ->
                        if (senderId == currentUserId) "You" else resolveFirstName(senderId, allUsers)
                    }
                    if (senderPrefix != null) "$senderPrefix: $lastMessage" else lastMessage
                } else chat.description
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                val badgeContentColor = if (personaColor.luminance() > 0.5f) Color.Black else Color.White
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(personaColor), contentAlignment = Alignment.Center) {
                    Text(text = if (unreadCount > 99) "99+" else "$unreadCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeContentColor)
                }
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(if (isPinned) "Unpin" else "Pin") },
                leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                onClick = { showMenu = false; onTogglePin() }
            )
            DropdownMenuItem(
                text = { Text(if (isMuted) "Unmute" else "Mute") },
                leadingIcon = { Icon(if (isMuted) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff, contentDescription = null) },
                onClick = { showMenu = false; onToggleMute() }
            )
        }
    }
}

@Composable
fun SocialTextInputDialog(title: String, label: String, initialValue: String = "", personaColor: Color, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = personaColor)) },
        confirmButton = { Button(onClick = { onConfirm(value) }, enabled = value.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = personaColor)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SocialConfirmDialog(title: String, message: String, confirmLabel: String, personaColor: Color, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = personaColor)) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun createImageFileUri(context: Context): Uri {
    val directory = context.externalCacheDir ?: context.cacheDir
    val file = File(directory, "img_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * ACTION_IMAGE_CAPTURE's EXTRA_OUTPUT only tells the camera app *where* to save the photo —
 * it doesn't grant *permission* to write there. The manifest's `grantUriPermissions="true"`
 * on our FileProvider only makes granting possible, it doesn't grant automatically, and
 * ActivityResultContracts.TakePicture()'s intent carries no FLAG_GRANT_WRITE_URI_PERMISSION.
 * Without this, some camera apps still report success while silently failing to write the
 * file (it stays empty), so the "photo" that comes back has nothing in it.
 */
fun grantCameraWritePermission(context: Context, uri: Uri) {
    val captureIntent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
    val resolvedApps = context.packageManager.queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
    for (info in resolvedApps) {
        context.grantUriPermission(
            info.activityInfo.packageName,
            uri,
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

@Composable
fun ImageSourceSheet(onDismiss: () -> Unit, onImageSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    // onDismiss() must NOT fire until the picker/camera actually hands back a result (or is
    // cancelled/denied) — calling it any earlier disposes this composable, which tears down
    // these launchers before Android can deliver anything to them. That was the previous bug:
    // the permission callback dismissed unconditionally, before the camera ever launched.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { onImageSelected(it) }
        onDismiss()
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingCameraUri = createImageFileUri(context)
        } else {
            onDismiss()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onImageSelected(it) }
        onDismiss()
    }
    LaunchedEffect(pendingCameraUri) {
        pendingCameraUri?.let {
            grantCameraWritePermission(context, it)
            cameraLauncher.launch(it)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Choose Photo") },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedCard(modifier = Modifier.weight(1f).height(80.dp).clickable { cameraPermLauncher.launch(android.Manifest.permission.CAMERA) }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.Rounded.PhotoCamera, contentDescription = null); Text("Camera", style = MaterialTheme.typography.labelMedium) } }
                }
                OutlinedCard(modifier = Modifier.weight(1f).height(80.dp).clickable { galleryLauncher.launch("image/*") }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null); Text("Gallery", style = MaterialTheme.typography.labelMedium) } }
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPickerSheet(
    title: String,
    allUsers: List<AppUser>,
    excludeIds: Set<String> = emptySet(),
    initialSelected: Set<String> = emptySet(),
    multiSelect: Boolean = true,
    personaColor: Color,
    onDismiss: () -> Unit,
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
            user.id.contains(search, ignoreCase = true) ||
            (user.firstName?.contains(search, ignoreCase = true) == true) ||
            (user.lastName?.contains(search, ignoreCase = true) == true) ||
            "${user.firstName} ${user.lastName}".contains(search, ignoreCase = true)
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden })
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        properties = ModalBottomSheetDefaults.properties(shouldDismissOnBackPress = false)
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        @OptIn(ExperimentalLayoutApi::class)
        val imeVisible = WindowInsets.isImeVisible
        BackHandler { if (imeVisible) keyboardController?.hide() else onDismiss() }
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).imePadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    if (multiSelect) { Text(if (selected.isEmpty()) "Select members to add" else "${selected.size} selected", style = MaterialTheme.typography.labelMedium, color = if (selected.isNotEmpty()) personaColor else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (multiSelect && selected.isNotEmpty()) {
                    Button(onClick = { onConfirm(selected); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = personaColor), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Text("Add", fontWeight = FontWeight.Bold) }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                OutlinedTextField(
                    value = search, onValueChange = { search = it }, placeholder = { Text("Search for friends…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = personaColor) },
                    trailingIcon = if (search.isNotBlank()) { { IconButton(onClick = { search = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") } } } else null,
                    modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, cursorColor = personaColor)
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (eligible.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = personaColor.copy(alpha = 0.1f)) { Icon(Icons.Rounded.PeopleOutline, contentDescription = null, tint = personaColor.copy(alpha = 0.4f), modifier = Modifier.padding(20.dp)) }
                                Text("No users found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Invite some friends to get started!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                } else if (filtered.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Text("No results for \"$search\"", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
                items(filtered, key = { it.id }) { user ->
                    val isSelected = user.id in selected
                    val displayName = resolveDisplayName(user.id, allUsers)
                    Surface(
                        onClick = { if (!multiSelect) { onConfirm(setOf(user.id)); onDismiss() } else { selected = if (isSelected) selected - user.id else selected + user.id } },
                        shape = RoundedCornerShape(16.dp), color = if (isSelected) personaColor.copy(alpha = 0.08f) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, personaColor.copy(alpha = 0.3f)) else null, modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SocialUserAvatar(name = displayName, personaColor = personaColor, size = 44, profilePicture = user.profilePicture)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(user.email, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                            }
                            if (multiSelect) { Checkbox(checked = isSelected, onCheckedChange = { selected = if (it) selected + user.id else selected - user.id }, colors = CheckboxDefaults.colors(checkedColor = personaColor)) }
                            else { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant) }
                        }
                    }
                }
            }
            if (multiSelect) {
                Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 8.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Button(onClick = { onConfirm(selected); onDismiss() }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = personaColor, disabledContainerColor = personaColor.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selected.isNotEmpty()) { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp)) }
                            Text(if (selected.isEmpty()) "Select Members" else "Confirm Selection (${selected.size})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Chat – Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Chat – Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewChatWithMessages() {
    HabitFlowTheme {
        val personaColor = PersonaUiData.getDetails("Socializer").endColor
        SocialGroupChatContent(
            chat = ChatResponse(id = "1", name = "Mountain Climbers", isGroup = true, lastMessage = "Who's hiking Sunday?", participantIds = listOf("alex", "mia", "me"), admins = listOf("me"), owner = "me"),
            messages = listOf(ChatMessage(text = "Hey everyone! Ready for Sunday?", senderId = "alex"), ChatMessage(text = "Absolutely! 🏔️", senderId = "mia"), ChatMessage(text = "Count me in!", senderId = "me"), ChatMessage(text = "7am at the trailhead!", senderId = "alex", likedBy = listOf("me", "mia"))),
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
                SocialMessageBubble(message = ChatMessage(text = "Just hit a new PR! 🏃", senderId = "alex", likedBy = listOf("me")), personaColor = personaColor, onLikeClick = {})
                SocialMessageBubble(message = ChatMessage(text = "That's awesome, congrats!", senderId = "me"), personaColor = personaColor, onLikeClick = {})
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
