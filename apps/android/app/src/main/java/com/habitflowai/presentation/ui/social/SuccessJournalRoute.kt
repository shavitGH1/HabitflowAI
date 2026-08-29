package com.habitflowai.presentation.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.habitflowai.data.model.HomeAchievement
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.viewmodel.HabitsViewModel
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.habitflowai.util.parseIsoToMillis
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessJournalRoute(
    habitsViewModel: HabitsViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val habitsState by habitsViewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (onboardingState.personaResult == null) {
            onboardingViewModel.fetchProfile()
        }
    }

    val personaType = onboardingState.personaResult?.personaType ?: "Regulator"
    val personaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }

    var selectedFilter by remember { mutableStateOf(JournalFilter.ALL) }

    val journalItems = remember(habitsState.habits, onboardingState.achievements, selectedFilter) {
        val achievedHabits = if (selectedFilter == JournalFilter.ALL || selectedFilter == JournalFilter.HABITS) {
            habitsState.habits.mapNotNull { habit ->
                habit.implementedAt?.let { implementedAt ->
                    JournalItem.HabitAchievement(
                        habitId = habit.id,
                        habitName = habit.title,
                        description = habit.description,
                        implementedAt = implementedAt
                    )
                }
            }
        } else emptyList()

        val achievements = if (selectedFilter == JournalFilter.ALL || selectedFilter == JournalFilter.GOALS) {
            onboardingState.achievements.map { JournalItem.GoalAchievement(it) }
        } else emptyList()

        (achievedHabits + achievements).sortedByDescending { it.timestamp }
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
                                    JournalFilter.GOALS -> "Goals"
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
                                JournalFilter.ALL -> "Stay consistent to earn your first habit or goal achievement!"
                                JournalFilter.HABITS -> "No habits achieved yet."
                                JournalFilter.GOALS -> "No goals achieved yet."
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
                                is JournalItem.HabitAchievement -> {
                                    HabitAchievementCard(
                                        achievement = item,
                                        personaColor = personaDetails.endColor
                                    )
                                }
                                is JournalItem.GoalAchievement -> {
                                    GoalAchievementCard(
                                        achievement = item,
                                        personaColor = personaDetails.endColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class JournalFilter {
    ALL, GOALS, HABITS
}

sealed class JournalItem {
    abstract val timestamp: Long

    data class GoalAchievement(val achievement: HomeAchievement) : JournalItem() {
        override val timestamp: Long = parseIsoToMillis(achievement.awardedAt)
    }

    data class HabitAchievement(
        val habitId: String,
        val habitName: String,
        val description: String?,
        val implementedAt: String
    ) : JournalItem() {
        override val timestamp: Long = parseIsoToMillis(implementedAt)
    }
}

@Composable
fun HabitAchievementCard(
    achievement: JournalItem.HabitAchievement,
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
                    text = "Habit achieved: ${achievement.habitName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!achievement.description.isNullOrBlank()) {
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatJournalDate(achievement.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun GoalAchievementCard(
    achievement: JournalItem.GoalAchievement,
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
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = personaColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Achieved: ${achievement.achievement.goalTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${achievement.achievement.medal.replaceFirstChar { it.uppercase() }} medal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatJournalDate(achievement.timestamp),
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
