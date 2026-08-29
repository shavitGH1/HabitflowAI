package com.habitflowai.presentation.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.habitflowai.data.model.ClassifyPersonaResponse
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import com.habitflowai.presentation.viewmodel.HomeViewModel
import com.habitflowai.data.model.HomeGoalTask
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SmartToy
import com.habitflowai.presentation.ui.chat.ChatOverlay
import com.habitflowai.data.model.ChatUiState
import com.habitflowai.presentation.ui.profile.PresetAvatars
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.data.local.entity.DailyTaskEntity
import com.habitflowai.presentation.viewmodel.HomeUiState
import com.habitflowai.data.model.HomeResponse
import com.habitflowai.data.model.resolveProfilePicture
import coil.compose.AsyncImage

@Composable
fun HomeRoute(
    personaResult: ClassifyPersonaResponse?,
    userId: String,
    profilePicture: String? = null,
    onNavigateToReassessment: () -> Unit,
    onToggleChat: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(userId) {
        viewModel.fetchHomeData()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    HomeScreen(
        uiState = uiState,
        personaResult = personaResult,
        profilePicture = profilePicture,
        onCompleteTask = { viewModel.completeTask(it) },
        onDateSelected = { viewModel.onDateSelected(it) },
        onDismissDriftBanner = { viewModel.dismissDriftBanner() },
        onStartReassessment = onNavigateToReassessment,
        onToggleChat = onToggleChat,
        onRefreshPlan = { viewModel.refreshHomeData() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    personaResult: ClassifyPersonaResponse?,
    profilePicture: String? = null,
    onCompleteTask: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDismissDriftBanner: () -> Unit,
    onStartReassessment: () -> Unit,
    onToggleChat: () -> Unit,
    onRefreshPlan: () -> Unit
) {
    val scrollState = rememberScrollState()
    val today = remember { LocalDate.now() }

    if (uiState.isLoading && uiState.homeData == null) {
        HomeSkeleton()
        return
    }

    val homeData = uiState.homeData
    if (homeData == null && uiState.dailyTasks.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Text(text = "No goal data yet.", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Complete onboarding to unlock your adaptive dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val actualPersonaTypeRaw = homeData?.personaType ?: personaResult?.personaType
    val actualPersonaType = actualPersonaTypeRaw?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
    } ?: "Achiever"

    val (startColor, endColor) = when (actualPersonaType) {
        "Achiever" -> Color(0xFFFFD54F) to Color(0xFFFF8A65)
        "Grower" -> Color(0xFFAED581) to Color(0xFF4DB6AC)
        "Regulator", "Architect" -> Color(0xFF64B5F6) to Color(0xFF1E88E5)
        "Socializer" -> Color(0xFFBA68C8) to Color(0xFFF06292)
        "Explorer" -> Color(0xFFFF8A80) to Color(0xFFFF5252)
        "Altruist" -> Color(0xFFF48FB1) to Color(0xFFCE93D8)
        else -> Color(0xFF81D4FA) to Color(0xFFCE93D8)
    }

    val isViewingHistory = uiState.selectedDate != today

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HabitFlow AI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onToggleChat) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = "AI Assistant",
                            tint = endColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            startColor.copy(alpha = 0.4f),
                            endColor.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isDriftDetected && !uiState.isDriftBannerDismissed) {
                DriftCheckBanner(
                    rationale = uiState.driftRationale ?: "We've detected a shift in your habit patterns.",
                    onDismiss = onDismissDriftBanner,
                    onAction = onStartReassessment
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(startColor, endColor))),
                contentAlignment = Alignment.Center
            ) {
                val presetDrawable = PresetAvatars.drawableFor(profilePicture)
                when {
                    presetDrawable != null -> Image(
                        painter = painterResource(presetDrawable),
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    !profilePicture.isNullOrBlank() -> AsyncImage(
                        model = resolveProfilePicture(profilePicture),
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    else -> Icon(
                        imageVector = Icons.Rounded.Face,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome, $actualPersonaType!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!uiState.isRefreshing) {
                TextButton(
                    onClick = onRefreshPlan,
                    colors = ButtonDefaults.textButtonColors(contentColor = endColor)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh AI Plan", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(vertical = 8.dp),
                    strokeWidth = 2.dp,
                    color = endColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your tailored dashboard is ready.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (homeData != null) {
                    Text(
                        text = homeData.motivationalMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(24.dp)
                    )

                    // Data-driven Persona Dashboard
                    PersonaDashboard(
                        personaType = actualPersonaType,
                        homeData = homeData,
                        personaColor = endColor
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    HistoryCalendarSection(
                        today = today,
                        selectedDate = uiState.selectedDate,
                        onDateSelected = onDateSelected,
                        completionHistory = uiState.datesWithCompletions
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (uiState.dailyTasks.isNotEmpty()) {
                        GoalPlanSection(
                            tasks = uiState.dailyTasks,
                            selectedDate = uiState.selectedDate,
                            today = today,
                            onTaskToggled = { taskId, isChecked ->
                                if (isChecked && !isViewingHistory) {
                                    onCompleteTask(taskId)
                                }
                            },
                            personaType = actualPersonaType
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val message = if (isViewingHistory)
                                "No tasks were recorded for this date."
                            else "No tasks generated for today yet."

                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    ProgressPhaseSection(actualPersonaType, homeData?.confidenceScore ?: 0.0)

                    Spacer(modifier = Modifier.height(24.dp))

                    PlanExplanationSection(actualPersonaType, homeData?.portfolioSummary)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).shimmerEffect())
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.width(200.dp).height(32.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.width(150.dp).height(20.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(32.dp))
        Box(modifier = Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
    }
}

fun Modifier.shimmerEffect(): Modifier = this.then(
    // Simple implementation of shimmer or just a pulsing alpha for now
    Modifier.background(Color.LightGray.copy(alpha = 0.3f))
)

@Composable
fun GoalPlanSection(
    tasks: List<DailyTaskEntity>,
    selectedDate: LocalDate,
    today: LocalDate,
    onTaskToggled: (String, Boolean) -> Unit,
    personaType: String
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val dateText = if (selectedDate == today) "Today's Checklist" else "Checklist for ${selectedDate.format(formatter)}"
    
    // Group tasks by habitTitle, exclude "General", and ensure priority ones are at the top
    val groupedTasks = remember(tasks) {
        tasks.filter { it.habitTitle != "General" }
            .groupBy { it.habitTitle }
            .entries
            .sortedBy { entry ->
                if (entry.key == "Main Goal") 0 else 1
            }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (selectedDate == today) {
            Text(
                text = "AI-optimized: Up to 5 general tasks + 3 per habit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        groupedTasks.forEach { (habitTitle, habitTasks) ->
            val subtitle = if (habitTitle == "Main Goal") "Strategic Goal Progress" else "Targeted Habit Actions"
            val accentColor = if (habitTitle == "Main Goal") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

            HabitGroup(
                title = habitTitle,
                subtitle = subtitle,
                accentColor = accentColor,
                tasks = habitTasks,
                onTaskToggled = onTaskToggled,
                isPastDate = selectedDate < today
            )
        }
    }
}

@Composable
fun HabitGroup(
    title: String,
    subtitle: String,
    accentColor: Color,
    tasks: List<DailyTaskEntity>,
    onTaskToggled: (String, Boolean) -> Unit,
    isPastDate: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        tasks.forEach { task ->
            InteractiveGoalItem(
                goalText = task.description,
                isChecked = task.isCompleted,
                onCheckedChange = { onTaskToggled(task.id, it) },
                enabled = !isPastDate && !task.isCompleted
            )
        }
    }
}

@Composable
fun HistoryCalendarSection(
    today: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    completionHistory: List<String>
) {
    // Show 28 days of history
    val startOfHistory = today.minusDays(27)
    val dates = (0..27).map { startOfHistory.plusDays(it.toLong()) }
    
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE\nd") }
    val historySet = remember(completionHistory) { completionHistory.toSet() }
    
    val scrollState = rememberLazyListState()
    
    // Auto-scroll to end (today) on first launch
    LaunchedEffect(Unit) {
        scrollState.scrollToItem(dates.size - 1)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Activity History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Review your progress over the last 28 days.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            state = scrollState,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dates) { date ->
                val isToday = date == today
                val isSelected = date == selectedDate
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isCompleted = historySet.contains(dateStr)

                Card(
                    modifier = Modifier
                        .size(width = 68.dp, height = 86.dp)
                        .clickable { onDateSelected(date) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                       else if (isCompleted) MaterialTheme.colorScheme.primaryContainer
                                       else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = date.format(dayFormatter),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isCompleted) {
                            Spacer(Modifier.height(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = if(isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (isToday && !isSelected) {
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressPhaseSection(personaType: String, confidenceScore: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                val scorePercent = (confidenceScore * 100).toInt()
                Text(
                    text = "Consistency Score: $scorePercent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your plan updates automatically every 7 days to ensure you keep making progress. Next week, we'll intensify the $personaType routine.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { confidenceScore.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun PlanExplanationSection(personaType: String, portfolioSummary: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your $personaType Master Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

            if (!portfolioSummary.isNullOrBlank()) {
                Text(text = "Strategic Overview", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = portfolioSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(text = "Daily Strategy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = "Your daily focus is consistency. Complete your scheduled core habits. Failure is okay; the goal is to show up.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun InteractiveGoalItem(
    goalText: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                      else MaterialTheme.colorScheme.surfaceVariant,
        label = "GoalBackgroundAnimation"
    )

    val textColor by animateColorAsState(
        targetValue = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "GoalTextAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onCheckedChange(true) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isChecked) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { if (it) onCheckedChange(true) },
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    disabledCheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    disabledUncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goalText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PersonaDashboard(
    personaType: String,
    homeData: HomeResponse,
    personaColor: Color
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!homeData.tips.isNullOrEmpty()) {
            Text(
                text = "Personalized Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = personaColor
            )
            homeData.tips.forEach { tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = personaColor,
                        modifier = Modifier.size(16.dp).padding(top = 4.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = tip, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (!homeData.failurePatterns.isNullOrEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Pattern Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            homeData.failurePatterns.forEach { pattern ->
                Text(
                    text = "• $pattern",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Achiever Persona - Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Achiever Persona - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AchieverHomePreview() {
    HomePersonaPreview("Achiever")
}

@Preview(showBackground = true, name = "Grower Persona - Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Grower Persona - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GrowerHomePreview() {
    HomePersonaPreview("Grower")
}

@Preview(showBackground = true, name = "Regulator Persona - Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Regulator Persona - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RegulatorHomePreview() {
    HomePersonaPreview("Regulator")
}

@Preview(showBackground = true, name = "Socializer Persona - Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Socializer Persona - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SocializerHomePreview() {
    HomePersonaPreview("Socializer")
}

@Preview(showBackground = true, name = "Explorer Persona - Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Explorer Persona - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExplorerHomePreview() {
    HomePersonaPreview("Explorer")
}

@Preview(showBackground = true, name = "Altruist Persona - Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Altruist Persona - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AltruistHomePreview() {
    HomePersonaPreview("Altruist")
}

@Preview(showBackground = true, name = "Component Gallery")
@Composable
fun ComponentGalleryPreview() {
    HabitFlowTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Goal Items", style = MaterialTheme.typography.titleLarge)
            InteractiveGoalItem("Unchecked Task", false, {})
            InteractiveGoalItem("Checked Task", true, {})

            HorizontalDivider()
            Text("Progress Section", style = MaterialTheme.typography.titleLarge)
            ProgressPhaseSection("Achiever", 0.5)

            HorizontalDivider()
            Text("Plan Explanation", style = MaterialTheme.typography.titleLarge)
            PlanExplanationSection("Explorer", "This is a sample summary.")
        }
    }
}

@Composable
fun DriftCheckBanner(
    rationale: String,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Habit Drift Detected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Start Reassessment")
            }
        }
    }
}

@Preview(showBackground = true, name = "Home with Chat FAB")
@Composable
fun HomeWithChatPreview() {
    val personaType = "Achiever"
    val sampleHomeData = HomeResponse(
        goal = "Master my routine",
        motivationalMessage = "You are making incredible progress as an Achiever!",
        coreGoals = listOf(
            HomeGoalTask("Daily habit one", 10, "1", false, genre = "goal"),
            HomeGoalTask("Daily habit two", 5, "2", true, genre = "goal")
        ),
        dailyVariations = listOf(
            HomeGoalTask("Plan tomorrow's priorities", 10, "3", false, genre = "goal"),
            HomeGoalTask("Journal for 5 minutes", 5, "4", false, genre = "persona")
        ),
        success = true,
        personaType = personaType
    )
    val uiState = HomeUiState(homeData = sampleHomeData, isLoading = false)

    HabitFlowTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeScreen(
                uiState = uiState,
                personaResult = null,
                onCompleteTask = {},
                onDateSelected = {},
                onDismissDriftBanner = {},
                onStartReassessment = {},
                onToggleChat = {},
                onRefreshPlan = {}
            )
            ChatOverlay(
                uiState = ChatUiState(personaType = personaType),
                onToggleChat = {},
                onInputChanged = {},
                onSendMessage = {}
            )
        }
    }
}

@Composable
fun HomePersonaPreview(personaType: String) {
    val article = if (listOf('A', 'E', 'I', 'O', 'U').contains(personaType.firstOrNull()?.uppercaseChar())) "an" else "a"
    val samplePersona = ClassifyPersonaResponse(
        id = "1",
        personaType = personaType,
        motivationalMessage = "Your unique $personaType drive is your greatest asset.",
        success = true
    )
    val sampleHomeData = HomeResponse(
        goal = "Master my routine",
        motivationalMessage = "You are making incredible progress as $article $personaType!",
        coreGoals = listOf(
            HomeGoalTask("Daily habit one", 10, "1", false, genre = "goal"),
            HomeGoalTask("Daily habit two", 5, "2", true, genre = "goal")
        ),
        dailyVariations = listOf(
            HomeGoalTask("Plan tomorrow's priorities", 10, "3", false, genre = "goal"),
            HomeGoalTask("Journal for 5 minutes", 5, "4", false, genre = "persona")
        ),
        success = true,
        personaType = personaType
    )
    val uiState = HomeUiState(homeData = sampleHomeData, isLoading = false)

    HabitFlowTheme {
        HomeScreen(
            uiState = uiState,
            personaResult = samplePersona,
            onCompleteTask = {},
            onDateSelected = {},
            onDismissDriftBanner = {},
            onStartReassessment = {},
            onToggleChat = {},
            onRefreshPlan = {}
        )
    }
}
