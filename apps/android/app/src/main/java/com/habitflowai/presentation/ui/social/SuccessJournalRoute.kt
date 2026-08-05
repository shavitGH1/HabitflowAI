package com.habitflowai.presentation.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitflowai.data.model.Post
import com.habitflowai.presentation.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessJournalRoute(
    viewModel: SocialViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }
    
    val personalPosts = remember(uiState.posts) {
        uiState.posts.filter { it.author == "Me" || it.author == "Alex" } // Simulating personal posts
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Success Journal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (personalPosts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No entries yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Your journey starts with a single post!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(personalPosts) { post ->
                        PostCard(
                            post = post,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onClick = {
                                selectedPostForComments = post
                                viewModel.loadComments(post.id)
                            }
                        )
                    }
                }
            }
        }

        if (selectedPostForComments != null) {
            CommentsBottomSheet(
                post = selectedPostForComments!!,
                comments = uiState.comments[selectedPostForComments!!.id] ?: emptyList(),
                isLoadingComments = uiState.isLoadingComments,
                onDismiss = { selectedPostForComments = null },
                onLikeClick = { viewModel.toggleLike(selectedPostForComments!!.id) },
                onAddComment = { content ->
                    viewModel.addComment(selectedPostForComments!!.id, content)
                }
            )
        }
    }
}
