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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.habitflowai.presentation.viewmodel.SocialUiState
import com.habitflowai.presentation.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialRoute(
    viewModel: SocialViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }

    SocialContent(
        uiState = uiState,
        onLikeClick = viewModel::toggleLike,
        onLoadMore = viewModel::loadMorePosts,
        onAddPostClick = { showCreateSheet = true },
        onPostClick = { post ->
            selectedPostForComments = post
            viewModel.loadComments(post.id)
        }
    )

    if (showCreateSheet) {
        CreatePostBottomSheet(
            onDismiss = { showCreateSheet = false },
            onPostCreated = { content, imageUri ->
                viewModel.addPost(content, imageUri?.toString())
                showCreateSheet = false
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
fun SocialContent(
    uiState: SocialUiState,
    onLikeClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onAddPostClick: () -> Unit,
    onPostClick: (Post) -> Unit
) {
    val listState = rememberLazyListState()

    // Automatic Load More Detection
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
        
        FloatingActionButton(
            onClick = onAddPostClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add Post")
        }
    }
}

@Composable
fun PostCard(
    post: Post,
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
                    Icon(Icons.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
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
        onLikeClick = {},
        onLoadMore = {},
        onAddPostClick = {},
        onPostClick = {}
    )
}
