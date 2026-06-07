package com.habitflowai.presentation.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class Post(val id: Int, val author: String, val content: String, val hasPhoto: Boolean, var isLiked: Boolean = false, var likeCount: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialRoute() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Global", "Friends")
    val posts = remember {
        mutableStateListOf(
            Post(1, "Alex", "Just completely crushed my deep work block! 🚀", true, likeCount = 14),
            Post(2, "Mia", "Woke up at 5am today. The sunrise was totally worth it.", false, likeCount = 5),
            Post(3, "Sam", "Hit a 10 day streak on fitness goals! Consistency pays off.", true, likeCount = 22)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts.size) { index ->
                    PostCard(post = posts[index], onLikeClick = {
                        val currentPost = posts[index]
                        posts[index] = currentPost.copy(
                            isLiked = !currentPost.isLiked,
                            likeCount = if (currentPost.isLiked) currentPost.likeCount - 1 else currentPost.likeCount + 1
                        )
                    })
                }
            }
        }
        FloatingActionButton(
            onClick = { },
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
fun PostCard(post: Post, onLikeClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(post.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(post.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Just Pictures primarily
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📷 Posted Photo", color = Color.DarkGray, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onLikeClick) {
                    Icon(
                        if (post.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${post.likeCount} Likes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { }) {
                    Text("Comment", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SocialRoutePreview() {
    SocialRoute()
}

@Preview(showBackground = true)
@Composable
fun PostCardPreview() {
    PostCard(
        post = Post(1, "Alex", "Just completely crushed my deep work block! 🚀", true, likeCount = 14),
        onLikeClick = {}
    )
}
