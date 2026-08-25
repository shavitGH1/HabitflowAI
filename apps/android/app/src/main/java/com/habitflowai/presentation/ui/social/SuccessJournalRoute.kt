package com.habitflowai.presentation.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.habitflowai.data.model.Post
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.viewmodel.HabitsViewModel
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.habitflowai.presentation.viewmodel.SocialViewModel
import com.habitflowai.util.parseIsoToMillis
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessJournalRoute(
    viewModel: SocialViewModel = hiltViewModel(),
    habitsViewModel: HabitsViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    onUserClick: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val habitsState by habitsViewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (onboardingState.personaResult == null) {
            onboardingViewModel.fetchProfile()
        }
    }

    val personaType = onboardingState.personaResult?.personaType ?: "Regulator"
    val personaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }

    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }
    var selectedFilter by remember { mutableStateOf(JournalFilter.ALL) }
    
    // Combine personal posts and habit completions into a single "Journal" view
    val journalItems = remember(uiState.posts, uiState.currentUserId, habitsState.habits, selectedFilter) {
        val posts = if (selectedFilter == JournalFilter.ALL || selectedFilter == JournalFilter.POSTS) {
            uiState.posts
                .filter { it.authorId == uiState.currentUserId }
                .map { JournalItem.SocialPost(it) }
        } else emptyList()
        
        val completions = if (selectedFilter == JournalFilter.ALL || selectedFilter == JournalFilter.HABITS) {
            habitsState.habits.flatMap { habit ->
                habit.completionHistory.map { date ->
                    JournalItem.HabitCompletion(
                        habitId = habit.id,
                        habitName = habit.title,
                        date = date,
                        description = habit.description
                    )
                }
            }
        } else emptyList()
        
        (posts + completions).sortedByDescending { it.timestamp }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedFilter.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = personaDetails.endColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilter.ordinal]),
                        color = personaDetails.endColor
                    )
                }
            ) {
                JournalFilter.entries.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = {
                            Text(
                                text = when (filter) {
                                    JournalFilter.ALL -> "All"
                                    JournalFilter.HABITS -> "Habits"
                                    JournalFilter.POSTS -> "Posts"
                                },
                                fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (journalItems.isEmpty()) {
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
                            text = when (selectedFilter) {
                                JournalFilter.ALL -> "Complete habits or share posts to fill your journal!"
                                JournalFilter.HABITS -> "No completed habits found."
                                JournalFilter.POSTS -> "You haven't shared any posts yet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(journalItems) { item ->
                            when (item) {
                                is JournalItem.SocialPost -> {
                                    PostCard(
                                        post = item.post,
                                        personaColor = personaDetails.endColor,
                                        onLikeClick = { viewModel.toggleLike(item.post.id) },
                                        onUserClick = { onUserClick(item.post.authorId) },
                                        onClick = {
                                            selectedPostForComments = item.post
                                            viewModel.loadComments(item.post.id)
                                        }
                                    )
                                }
                                is JournalItem.HabitCompletion -> {
                                    HabitCompletionCard(
                                        completion = item,
                                        personaColor = personaDetails.endColor
                                    )
                                }
                            }
                        }
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
                },
                onUserClick = { onUserClick(selectedPostForComments!!.authorId) }
            )
        }
    }
}

enum class JournalFilter {
    ALL, HABITS, POSTS
}

sealed class JournalItem {
    abstract val timestamp: Long

    data class SocialPost(val post: Post) : JournalItem() {
        override val timestamp: Long = parseIsoToMillis(post.createdAt)
    }

    data class HabitCompletion(
        val habitId: String,
        val habitName: String,
        val date: String,
        val description: String?
    ) : JournalItem() {
        override val timestamp: Long = parseIsoToMillis(date)
    }
}

@Composable
fun HabitCompletionCard(
    completion: JournalItem.HabitCompletion,
    personaColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = personaColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = personaColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Completed: ${completion.habitName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!completion.description.isNullOrBlank()) {
                    Text(
                        text = completion.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatJournalDate(completion.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private val JOURNAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

private fun formatJournalDate(timestamp: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(timestamp)
        JOURNAL_DATE_FORMATTER.withZone(ZoneId.systemDefault()).format(instant)
    } catch (e: Exception) {
        ""
    }
}


