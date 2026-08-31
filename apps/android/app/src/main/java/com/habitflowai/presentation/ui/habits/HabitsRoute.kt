@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.habitflowai.presentation.ui.habits

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.model.ActiveGoalResponse
import com.habitflowai.presentation.ui.persona.PersonaDetails
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.HabitsUiState
import com.habitflowai.presentation.viewmodel.HabitsViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitsRoute(
    viewModel: HabitsViewModel,
    personaType: String,
    onHabitClick: (String) -> Unit,
    onGoalClick: (String) -> Unit,
    onToggleChat: () -> Unit,
    onSetGoal: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

        HabitsContent(
            uiState = uiState,
            personaType = personaType,
            onHabitClick = onHabitClick,
            onGoalClick = onGoalClick,
            onAddHabit = { title, desc, freq, linkToGoal ->
                viewModel.addHabit(title, desc, freq, linkToGoal)
            },
            onToggleChat = onToggleChat,
            onSetGoal = onSetGoal,
            onClearCongratulation = viewModel::clearCongratulation,
            onClearError = viewModel::clearError,
            onClearRelevanceWarning = viewModel::clearRelevanceWarning
        )
    }

    @Composable
    fun HabitsContent(
        uiState: HabitsUiState,
        personaType: String,
        onHabitClick: (String) -> Unit,
        onGoalClick: (String) -> Unit,
        onAddHabit: (String, String, String, Boolean) -> Unit,
        onToggleChat: () -> Unit,
        onSetGoal: () -> Unit,
        onClearCongratulation: () -> Unit = {},
        onClearError: () -> Unit = {},
        onClearRelevanceWarning: () -> Unit = {}
    ) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedSuggestionTitle by remember { mutableStateOf<String?>(null) }
    val details: PersonaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }
    val isDark = isSystemInDarkTheme()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.congratulationMessage) {
        uiState.congratulationMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            onClearCongratulation()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            onClearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF000000), Color(0xFF121212))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(details.startColor.copy(alpha = 0.15f), Color.White)
                    )
                }
            )
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Habit Hub", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onToggleChat) {
                            Icon(
                                Icons.Rounded.SmartToy,
                                contentDescription = "AI Assistant",
                                tint = details.endColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = details.endColor
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = details.endColor,
                    contentColor = if (details.endColor.luminance() > 0.5f) Color.Black else Color.White
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Habit")
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            // stays active until achieved, not just "done today"
            val habitsToDisplay = remember(uiState.habits) { uiState.habits.filter { it.implementedAt == null } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    if (uiState.activeGoal != null) {
                        GoalHighlightCard(
                            goal = uiState.activeGoal,
                            personaColor = details.endColor,
                            onClick = { onGoalClick(uiState.activeGoal.id) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    } else if (!uiState.onboardingGoal.isNullOrEmpty()) {
                        OnboardingGoalCard(
                            goalText = uiState.onboardingGoal,
                            personaColor = details.endColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    } else {
                        EmptyGoalCard(
                            personaColor = details.endColor,
                            onSetGoal = onSetGoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (uiState.suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "AI Suggested Habits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = details.endColor,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(uiState.suggestions) { suggestion ->
                                SuggestionChip(
                                    title = suggestion.description,
                                    personaColor = details.endColor,
                                    onClick = {
                                        selectedSuggestionTitle = suggestion.description
                                        showCreateSheet = true
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    val allCompletions = remember(uiState.habits) {
                        uiState.habits.flatMap { it.completionHistory }.distinct()
                    }
                    ConsistencyCalendar(
                        personaColor = details.endColor,
                        completionHistory = allCompletions
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Master your routine, one day at a time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = details.endColor)
                        }
                    }
                } else if (habitsToDisplay.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (uiState.habits.isEmpty()) "No habits yet. Tap + to start!" else "All habits achieved! 🎉 Check your Success Journal.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    val activeGoalId = uiState.activeGoal?.id
                    val goalHabits = habitsToDisplay.filter { 
                        it.goalId != null && (activeGoalId == null || it.goalId == activeGoalId)
                    }
                    val standaloneHabits = habitsToDisplay.filter { 
                        it.goalId == null || (activeGoalId != null && it.goalId != activeGoalId)
                    }

                    val totalGoalHabits = habitsToDisplay.count { it.goalId != null && (activeGoalId == null || it.goalId == activeGoalId) }
                    val totalStandaloneHabits = habitsToDisplay.count { it.goalId == null || (activeGoalId != null && it.goalId != activeGoalId) }

                    if (goalHabits.isNotEmpty()) {
                        item {
                            val headerText = if (activeGoalId != null) "Goal Related Habits ($totalGoalHabits/3)" else "Goal Related Habits"
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = details.endColor,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = goalHabits,
                            key = { habit: HabitEntity -> habit.id }
                        ) { habit: HabitEntity ->
                            HabitItem(
                                habit = habit,
                                personaColor = details.endColor,
                                onClick = { onHabitClick(habit.id) }
                            )
                        }
                    }

                    if (standaloneHabits.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Standalone Habits ($totalStandaloneHabits/2)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = standaloneHabits,
                            key = { habit: HabitEntity -> habit.id }
                        ) { habit: HabitEntity ->
                            HabitItem(
                                habit = habit,
                                personaColor = details.endColor,
                                onClick = { onHabitClick(habit.id) }
                            )
                        }
                    }
                }
                
                // Extra padding at bottom for FAB
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            if (uiState.relevanceWarning != null) {
                AlertDialog(
                    onDismissRequest = onClearRelevanceWarning,
                    icon = { Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("AI Relevance Warning") },
                    text = { Text(uiState.relevanceWarning) },
                    confirmButton = {
                        Button(onClick = onClearRelevanceWarning) {
                            Text("I Understand")
                        }
                    }
                )
            }

            if (showCreateSheet) {
                val activeGoalId = uiState.activeGoal?.id
                val hasGoal = activeGoalId != null || !uiState.onboardingGoal.isNullOrEmpty()
                val activeHabits = uiState.habits.filter { it.implementedAt == null }
                val goalHabitCount = activeHabits.count {
                    it.goalId != null && (activeGoalId == null || it.goalId == activeGoalId)
                }
                val standaloneHabitCount = activeHabits.count { it.goalId == null }

                HabitCreateBottomSheet(
                    personaColor = details.endColor,
                    hasActiveGoal = hasGoal,
                    goalHabitCount = goalHabitCount,
                    standaloneHabitCount = standaloneHabitCount,
                    initialTitle = selectedSuggestionTitle ?: "",
                    onDismiss = { 
                        showCreateSheet = false
                        selectedSuggestionTitle = null
                    },
                    onHabitCreated = { title, desc, freq, linkToGoal ->
                        onAddHabit(title, desc, freq, linkToGoal)
                        showCreateSheet = false
                        selectedSuggestionTitle = null
                    }
                )
            }
        }
    }
}

@Composable
fun SuggestionChip(
    title: String,
    personaColor: Color,
    onClick: () -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    Card(
        modifier = Modifier
            .widthIn(max = 200.dp)
            .clickable { 
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > 1000) {
                    lastClickTime = currentTime
                    onClick()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = personaColor.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, personaColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.SmartToy,
                contentDescription = null,
                tint = personaColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                color = personaColor
            )
        }
    }
}

@Composable
fun ConsistencyCalendar(
    personaColor: Color,
    completionHistory: List<String> = emptyList()
) {
    val today = LocalDate.now()
    val days = remember {
        (0 until 28).map { today.minusDays(it.toLong()) }.reversed()
    }
    
    val completionDates = remember(completionHistory) {
        completionHistory.mapNotNull {
            try {
                if (it.length == 10 && it.count { c -> c == '-' } == 2) {
                    LocalDate.parse(it)
                } else {
                    java.time.OffsetDateTime.parse(it).toLocalDate()
                }
            } catch (_: Exception) {
                try {
                    java.time.LocalDateTime.parse(it).toLocalDate()
                } catch (_: Exception) {
                    try {
                        java.time.Instant.parse(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }.toSet()
    }

    val completionMap = remember(days, completionDates) {
        days.associateWith { it in completionDates }
    }

    val streakCount = remember(completionDates, today) {
        var cursor = when {
            completionDates.contains(today) -> today
            completionDates.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> null
        }
        var streak = 0
        while (cursor != null && completionDates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        streak
    }

    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Consistency (Last 28 Days)" else "Calendar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (streakCount > 0) personaColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$streakCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (streakCount > 0) personaColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))

                    val gridSpacing = 10.dp
                    val leadingBlanks = remember(days) { days.first().dayOfWeek.value % 7 }
                    val gridItems: List<LocalDate?> = remember(days, leadingBlanks) {
                        List(leadingBlanks) { null } + days
                    }
                    val gridRows = (gridItems.size + 6) / 7

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gridSpacing)) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    BoxWithConstraints {
                        val cellSize = (maxWidth - gridSpacing * 6) / 7
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.height(cellSize * gridRows + gridSpacing * (gridRows - 1)),
                            verticalArrangement = Arrangement.spacedBy(gridSpacing),
                            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                            userScrollEnabled = false
                        ) {
                            items(gridItems) { day ->
                                if (day == null) {
                                    Box(modifier = Modifier.aspectRatio(1f))
                                } else {
                                    val isCompleted = completionMap[day] ?: false
                                    val isToday = day == today

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCompleted -> personaColor
                                                    isToday -> personaColor.copy(alpha = 0.2f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isCompleted) Color.White else if (isToday) personaColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isToday || isCompleted) FontWeight.ExtraBold else FontWeight.Bold
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
}

@Composable
fun GoalHighlightCard(
    goal: ActiveGoalResponse,
    personaColor: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = personaColor.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, personaColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = personaColor
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Main Objective",
                    style = MaterialTheme.typography.labelLarge,
                    color = personaColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            if (!goal.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (goal.progress != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { goal.progress.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = personaColor,
                    trackColor = personaColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun OnboardingGoalCard(
    goalText: String,
    personaColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = personaColor.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, personaColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = personaColor
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your Primary Focus",
                    style = MaterialTheme.typography.labelLarge,
                    color = personaColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = goalText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun EmptyGoalCard(
    personaColor: Color,
    onSetGoal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = personaColor.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, personaColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Let's set a new goal and start a new journey!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSetGoal,
                colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set My Goal", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HabitItem(
    habit: HabitEntity,
    personaColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = personaColor
                )
                if (!habit.description.isNullOrEmpty()) {
                    Text(
                        text = habit.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!habit.relevanceWarning.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = habit.relevanceWarning,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCreateBottomSheet(
    personaColor: Color,
    hasActiveGoal: Boolean,
    goalHabitCount: Int,
    standaloneHabitCount: Int,
    initialTitle: String = "",
    onDismiss: () -> Unit,
    onHabitCreated: (String, String, String, Boolean) -> Unit
) {
    val isSuggestion = initialTitle.isNotBlank()
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("daily") }
    // If it's an AI suggestion, we always want to link it to the goal by default 
    // even if the formal goal record is still loading in the background.
    var linkToGoal by remember(hasActiveGoal, isSuggestion) { 
        mutableStateOf((hasActiveGoal || isSuggestion) && goalHabitCount < 3) 
    }
    
    var isSubmitting by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = { it != SheetValue.Hidden },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        properties = ModalBottomSheetDefaults.properties(shouldDismissOnBackPress = false)
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        @OptIn(ExperimentalLayoutApi::class)
        val imeVisible = WindowInsets.isImeVisible
        BackHandler {
            if (imeVisible) keyboardController?.hide() else onDismiss()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Habit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = personaColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What habit to build?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Column {
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("daily" to "Daily", "weekly" to "Weekly").forEach { (value, label) ->
                        FilterChip(
                            selected = frequency == value,
                            onClick = { frequency = value },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = personaColor.copy(alpha = 0.2f),
                                selectedLabelColor = personaColor
                            )
                        )
                    }
                }
            }

            if (hasActiveGoal) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (goalHabitCount < 3 || linkToGoal) linkToGoal = !linkToGoal }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Link to active goal",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (goalHabitCount >= 3 && !linkToGoal) "Goal habit limit reached (3/3)" 
                                       else "AI will check if this habit supports your goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (goalHabitCount >= 3 && !linkToGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = linkToGoal,
                            onCheckedChange = { linkToGoal = it },
                            enabled = goalHabitCount < 3 || linkToGoal,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = personaColor
                            )
                        )
                    }
                }
            }

            val isStandaloneLimitReached = !linkToGoal && standaloneHabitCount >= 2
            if (isStandaloneLimitReached) {
                Text(
                    text = "Standalone habit limit reached (2/2). Link this to a goal or delete an old one.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Button(
                onClick = { 
                    if (!isSubmitting) {
                        isSubmitting = true
                        onHabitCreated(title, description, frequency, linkToGoal)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                enabled = title.isNotBlank() && !isStandaloneLimitReached && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Create Habit", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
