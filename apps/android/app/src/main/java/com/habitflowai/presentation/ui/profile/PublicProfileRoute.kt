package com.habitflowai.presentation.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.habitflowai.data.model.resolveProfilePicture
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.social.PostCard
import com.habitflowai.presentation.viewmodel.PublicProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileRoute(
    viewModel: PublicProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val personaType = uiState.personaType ?: "Regulator"
    val personaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }
    val personaColor = personaDetails.endColor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileHeader(
                    username = uiState.username,
                    profilePicture = uiState.profilePicture,
                    isMe = uiState.isMe,
                    isLoading = uiState.isCreatingChat,
                    isFollowing = uiState.isFollowing,
                    isFollowLoading = uiState.isFollowLoading,
                    followerCount = uiState.followerCount,
                    followingCount = uiState.followingCount,
                    personaColor = personaColor,
                    onSendMessage = {
                        viewModel.startDm { chatId ->
                            onSendMessage(chatId)
                        }
                    },
                    onToggleFollow = { viewModel.toggleFollow() }
                )
            }

            item {
                Text(
                    text = "Posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = personaColor)
                    }
                }
            } else if (uiState.posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No posts yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.posts) { post ->
                    PostCard(
                        post = post,
                        personaColor = personaColor,
                        onLikeClick = { viewModel.toggleLike(post.id) },
                        onUserClick = { /* Already on profile */ },
                        onClick = { /* Could navigate to post detail if exists */ }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    username: String,
    profilePicture: String? = null,
    isMe: Boolean,
    isLoading: Boolean,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    followerCount: Int,
    followingCount: Int,
    personaColor: Color,
    onSendMessage: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(personaColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val presetDrawable = PresetAvatars.drawableFor(profilePicture)
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
                    else -> Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = personaColor,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = username,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = followerCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Followers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = followingCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Following",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isMe) {
                    Button(
                        onClick = onToggleFollow,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isFollowing) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            ButtonDefaults.buttonColors(containerColor = personaColor)
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        enabled = !isFollowLoading
                    ) {
                        if (isFollowLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (isFollowing) Icons.Rounded.PersonRemove else Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isFollowing) "Unfollow" else "Follow",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = onSendMessage,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Message", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
