package com.habitflowai.presentation.ui.home

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.habitflowai.data.model.ClassifyPersonaResponse
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitflowai.presentation.viewmodel.HomeViewModel
import com.habitflowai.data.model.HomeGoalTask
import androidx.compose.material.icons.rounded.Close
import com.habitflowai.presentation.ui.theme.HabitFlowTheme

import com.habitflowai.presentation.viewmodel.HomeUiState
import com.habitflowai.data.model.HomeResponse

@Composable
fun HomeRoute(
    personaResult: ClassifyPersonaResponse?,
    userId: String,
    onNavigateToReassessment: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.fetchHomeData()
    }

    HomeScreen(
        uiState = uiState,
        personaResult = personaResult,
        onCompleteTask = { viewModel.completeTask(it) },
        onDismissDriftBanner = { viewModel.dismissDriftBanner() },
        onStartReassessment = onNavigateToReassessment
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    personaResult: ClassifyPersonaResponse?,
    onCompleteTask: (String) -> Unit,
    onDismissDriftBanner: () -> Unit,
    onStartReassessment: () -> Unit
) {
    val scrollState = rememberScrollState()
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    if (uiState.isLoading && uiState.homeData == null) {
        HomeSkeleton()
        return
    }

    val homeData = uiState.homeData
    if (homeData == null && !uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
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

    if (actualPersonaTypeRaw == null) {
        HomeSkeleton()
        return
    }

    val actualPersonaType = actualPersonaTypeRaw.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
    }

    val goalsForSelectedDate = remember(homeData) {
        val core = homeData?.coreGoals ?: emptyList()
        val daily = homeData?.dailyVariations ?: emptyList()
        core + daily
    }

    var checklistState by remember(goalsForSelectedDate) {
        val map = mutableMapOf<String, Boolean>()
        goalsForSelectedDate.forEach { task ->
            map[task.id] = task.completed
        }
        mutableStateOf(map)
    }

    val (startColor, endColor) = when (actualPersonaType) {
        "Achiever" -> Color(0xFFFFD54F) to Color(0xFFFF8A65)
        "Grower" -> Color(0xFFAED581) to Color(0xFF4DB6AC)
        "Regulator", "Architect" -> Color(0xFF64B5F6) to Color(0xFF1E88E5)
        "Socializer" -> Color(0xFFBA68C8) to Color(0xFFF06292)
        "Explorer" -> Color(0xFFFFB74D) to Color(0xFFE57373)
        "Altruist" -> Color(0xFFF48FB1) to Color(0xFFCE93D8)
        else -> Color(0xFF81D4FA) to Color(0xFFCE93D8)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        startColor.copy(alpha = 0.4f),
                        endColor.copy(alpha = 0.1f),
                        Color.White
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

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
            Icon(
                imageVector = Icons.Rounded.Face,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome, $actualPersonaType!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF37474F)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your tailored dashboard is ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF546E7A),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (homeData != null) {
                Text(
                    text = homeData.motivationalMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(24.dp)
                )
            }

            when (actualPersonaType) {
                "Achiever" -> AchieverHome()
                "Grower" -> GrowerHome()
                "Socializer" -> SocializerHome()
                "Explorer" -> ExplorerHome()
                "Altruist" -> AltruistHome()
                "Regulator" -> RegulatorHome()
                "Architect" -> ArchitectHome()
                else -> RegulatorHome()
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                GoalPlanSection(
                    goals = goalsForSelectedDate,
                    selectedDate = selectedDate,
                    today = today,
                    checkedGoals = checklistState,
                    onGoalToggled = { taskId, isChecked ->
                        if (isChecked) {
                            val newState = checklistState.toMutableMap()
                            newState[taskId] = true
                            checklistState = newState
                            onCompleteTask(taskId)
                        }
                    },
                    personaType = actualPersonaType
                )

                Spacer(modifier = Modifier.height(32.dp))

                HistoryCalendarSection(
                    today = today,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    checkedCount = checklistState.values.count { it },
                    totalCount = goalsForSelectedDate.size
                )

                Spacer(modifier = Modifier.height(24.dp))

                ProgressPhaseSection(actualPersonaType)

                Spacer(modifier = Modifier.height(24.dp))

                PlanExplanationSection(actualPersonaType)
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
private fun GoalPlanSection(
    goals: List<HomeGoalTask>,
    selectedDate: LocalDate,
    today: LocalDate,
    checkedGoals: Map<String, Boolean>,
    onGoalToggled: (String, Boolean) -> Unit,
    personaType: String
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val dateText = if (selectedDate == today) "Today's Checklist" else "Checklist for ${selectedDate.format(formatter)}"

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF37474F)
        )

        goals.forEach { task ->
            val isChecked = checkedGoals[task.id] ?: task.completed
            InteractiveGoalItem(
                goalText = task.description,
                score = if (personaType == "Achiever") task.points else 0,
                isChecked = isChecked,
                onCheckedChange = { onGoalToggled(task.id, it) }
            )
        }
    }
}

@Composable
private fun HistoryCalendarSection(
    today: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    checkedCount: Int,
    totalCount: Int
) {
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE\nd") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Weekly History & Tracking",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF37474F)
        )
        Text(
            text = "Tap any day to review your progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF546E7A)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(weekDates) { date ->
                val isToday = date == today
                val isSelected = date == selectedDate
                val isCompleted = isToday && (checkedCount >= totalCount && totalCount > 0)

                Card(
                    modifier = Modifier
                        .size(width = 68.dp, height = 86.dp)
                        .clickable { onDateSelected(date) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E88E5)
                                       else if (isCompleted) Color(0xFFC8E6C9)
                                       else Color.White
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
                            color = if (isSelected) Color.White
                                    else if (isCompleted) Color(0xFF2E7D32)
                                    else Color(0xFF37474F)
                        )
                        if (isCompleted) {
                            Spacer(Modifier.height(4.dp))
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if(isSelected) Color.White else Color(0xFF388E3C), modifier = Modifier.size(16.dp))
                        } else if (isToday && !isSelected) {
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF1E88E5), CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressPhaseSection(personaType: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFF8F00))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Phase: Foundation (Days 1-7)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your plan updates automatically every 7 days to ensure you keep making progress. Next week, we'll intensify the $personaType routine by introducing secondary habits.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFFB300),
                trackColor = Color(0xFFFFE082),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PlanExplanationSection(personaType: String) {
    val dailyExpl = "Your daily focus is consistency. Complete your scheduled core habits. Failure is okay; the goal is to show up."
    val monthlyExpl = "By day 30, the $personaType template will transition you into automatic pilot. You will have built neural pathways making these habits effortless."

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFF1E88E5))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your $personaType Master Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE3F2FD))

            Text(text = "Daily Strategy", style = MaterialTheme.typography.titleSmall, color = Color(0xFF0D47A1), fontWeight = FontWeight.Bold)
            Text(text = dailyExpl, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF455A64))

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Monthly Overview", style = MaterialTheme.typography.titleSmall, color = Color(0xFF0D47A1), fontWeight = FontWeight.Bold)
            Text(text = monthlyExpl, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF455A64))
        }
    }
}

@Composable
private fun InteractiveGoalItem(
    goalText: String,
    score: Int,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isChecked) Color(0xFFC8E6C9) else Color(0xFFFFF9C4),
        label = "GoalBackgroundAnimation"
    )

    val textColor by animateColorAsState(
        targetValue = if (isChecked) Color(0xFF2E7D32) else Color(0xFFF57F17),
        label = "GoalTextAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isChecked) { onCheckedChange(true) },
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
                enabled = !isChecked,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF388E3C),
                    uncheckedColor = Color(0xFFFBC02D),
                    checkmarkColor = Color.White
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
private fun RegulatorHome() {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DateRange, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "STRUCTURE & ROUTINE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D47A1))
            }

            HorizontalDivider(color = Color(0xFFBBDEFB))

            Text(text = "Current Consistency Score", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00695C))
            LinearProgressIndicator(
                progress = { 0.88f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFC8E6C9),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(text = "88% Excellent", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Today's Prescribed Hours", style = MaterialTheme.typography.titleMedium, color = Color(0xFF0277BD), fontWeight = FontWeight.Bold)

            TimeSlotCard("06:00 AM", "Morning Protocol & Hydration")
            TimeSlotCard("08:00 AM", "Deep Work Block")
            TimeSlotCard("01:00 PM", "Nutrition & Movement")
            TimeSlotCard("09:00 PM", "Evening Wind Down")
        }
}

@Composable
private fun TimeSlotCard(time: String, activity: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = time, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0))
        Text(text = activity, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF0D47A1), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AchieverHome() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFF8F00), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "GLOBAL COMPETITION", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
            }
            HorizontalDivider(color = Color(0xFFFFE082))

            Text(text = "You are competing against 4,203 Achievers globally.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF57F17))

            LeaderboardRow(rank = 1, name = "Strider AI", score = 14500, highlight = false)
            LeaderboardRow(rank = 2, name = "Sarah.H", score = 14220, highlight = false)
            LeaderboardRow(rank = 3, name = "Kev99", score = 13800, highlight = false)
            LeaderboardRow(rank = 4, name = "You", score = 13150, highlight = true)
            LeaderboardRow(rank = 5, name = "UnknownGamer", score = 12900, highlight = false)

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Full Leaderboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, name: String, score: Int, highlight: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlight) Color(0xFFFFECB3) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.Medium, color = if (highlight) Color(0xFFE65100) else Color(0xFF5D4037))
        }
    }
}

@Composable
private fun GrowerHome() {
    BaseTemplate(
        title = "GROWER TEMPLATE",
        lines = listOf("🌱 Daily Reflection Log", "📚 Skills Acquired", "📈 Effort Graph")
    )
}

@Composable
private fun SocializerHome() {
    BaseTemplate(
        title = "SOCIALIZER TEMPLATE",
        lines = listOf("👥 Team Challenges", "🗣️ Community Feed", "🙌 Supportive Cheers Sent: 12")
    )
}

@Composable
private fun ExplorerHome() {
    BaseTemplate(
        title = "EXPLORER TEMPLATE",
        lines = listOf("🧭 New Habits Discovered", "🗺️ Unknown Territory Unlocked", "🚀 Curiosity Quests")
    )
}

@Composable
private fun AltruistHome() {
    BaseTemplate(
        title = "ALTRUIST TEMPLATE",
        lines = listOf("🤝 Points Donated to Charity", "❤️ Friends Assisted", "🌟 Community Impact Score")
    )
}

@Composable
private fun ArchitectHome() {
    BaseTemplate(
        title = "ARCHITECT TEMPLATE",
        lines = listOf("🏗️ Foundation Habits", "📐 Daily Blueprint", "☑️ Checklists")
    )
}

@Composable
private fun BaseTemplate(title: String, lines: List<String>) {
    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
        }
        HorizontalDivider(color = Color(0xFFFFE082))
        lines.forEach { line ->
            Text(text = line, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4E342E), fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true, name = "Achiever Persona", device = "spec:width=411dp,height=891dp")
@Composable
fun AchieverHomePreview() {
    HomePersonaPreview("Achiever")
}

@Preview(showBackground = true, name = "Grower Persona", device = "spec:width=411dp,height=891dp")
@Composable
fun GrowerHomePreview() {
    HomePersonaPreview("Grower")
}

@Preview(showBackground = true, name = "Regulator Persona", device = "spec:width=411dp,height=891dp")
@Composable
fun RegulatorHomePreview() {
    HomePersonaPreview("Regulator")
}

@Preview(showBackground = true, name = "Socializer Persona", device = "spec:width=411dp,height=891dp")
@Composable
fun SocializerHomePreview() {
    HomePersonaPreview("Socializer")
}

@Preview(showBackground = true, name = "Explorer Persona", device = "spec:width=411dp,height=891dp")
@Composable
fun ExplorerHomePreview() {
    HomePersonaPreview("Explorer")
}

@Preview(showBackground = true, name = "Altruist Persona", device = "spec:width=411dp,height=891dp")
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
            InteractiveGoalItem("Unchecked Task", 10, false, {})
            InteractiveGoalItem("Checked Task", 10, true, {})

            HorizontalDivider()
            Text("Progress Section", style = MaterialTheme.typography.titleLarge)
            ProgressPhaseSection("Achiever")

            HorizontalDivider()
            Text("Plan Explanation", style = MaterialTheme.typography.titleLarge)
            PlanExplanationSection("Explorer")
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

@Composable
private fun HomePersonaPreview(personaType: String) {
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
            HomeGoalTask("Daily habit one", 10, "1", false),
            HomeGoalTask("Daily habit two", 5, "2", true)
        ),
        dailyVariations = emptyList(),
        success = true,
        personaType = personaType
    )
    val uiState = HomeUiState(homeData = sampleHomeData, isLoading = false)
    
    HabitFlowTheme {
        HomeScreen(
            uiState = uiState,
            personaResult = samplePersona,
            onCompleteTask = {},
            onDismissDriftBanner = {},
            onStartReassessment = {}
        )
    }
}
