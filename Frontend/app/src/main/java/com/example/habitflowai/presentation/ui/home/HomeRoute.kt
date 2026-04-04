package com.example.habitflowai.presentation.ui.home

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.habitflowai.data.model.ClassifyPersonaResponse
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

fun getGoalsForPersona(personaType: String, date: LocalDate): List<String> {
    val dayOfWeek = date.dayOfWeek.value // 1 (Monday) to 7 (Sunday)

    val coreGoals = when (personaType) {
        "Achiever" -> listOf("Complete 3 high-priority tasks", "Log all daily stats", "Drink 2L of water", "Review leaderboard standing")
        "Grower" -> listOf("Write in reflection journal", "Observe a new emotional trigger", "Read 10 pages", "Practice mindfulness for 10 mins")
        "Socializer" -> listOf("Share progress on feed", "Cheer on 2 community members", "Message accountability partner", "Check group chat")
        "Explorer" -> listOf("Take a different route or approach", "Read an article on a new topic", "Try a new healthy snack", "Log one new discovery")
        "Altruist" -> listOf("Do one random act of kindness", "Leave positive feedback for a peer", "Check in on family/friend", "Express gratitude publicly")
        "Regulator", "Architect" -> listOf("Wake up by 6:00 AM", "No screens after 9:00 PM", "Follow morning protocol", "Track every hour of deep work")
        else -> listOf("Log daily habit", "Review end of day", "Stay hydrated", "Plan for tomorrow")
    }

    val dailyVariations = when (personaType) {
        "Achiever" -> when (dayOfWeek) {
            1 -> listOf("Plan the week's biggest win", "Set 3 micro-goals for Monday")
            2 -> listOf("Speed-run a minor task", "Optimize workspace for focus")
            3 -> listOf("Review halfway milestone tracker", "Push limits on core metric")
            4 -> listOf("Eliminate one distraction", "Review weekly metric velocity")
            5 -> listOf("Wrap up weekly main objective", "Log end-of-week reflection")
            6 -> listOf("Active recovery & low-intensity tracking", "Prepare next week's layout")
            else -> listOf("Strategize next week's targets", "Rest and visualize success")
        }
        "Grower" -> when (dayOfWeek) {
            1 -> listOf("Set a new learning intention", "Listen to a growth podcast")
            2 -> listOf("Practice a new skill for 20 mins", "Identify one limiting belief")
            3 -> listOf("Re-read notes from a favorite book", "Meditate on mid-week stress")
            4 -> listOf("Apply one learning to daily life", "Seek constructive feedback")
            5 -> listOf("Summarize the week's learnings", "Journal about a hurdle faced")
            6 -> listOf("Spend time in nature without tech", "Explore a creative outlet")
            else -> listOf("Plan personal growth goals for next week", "Rest your mind")
        }
        "Socializer" -> when (dayOfWeek) {
            1 -> listOf("Kick off the week with a team message", "Set a shared goal with a friend")
            2 -> listOf("Join a quick group challenge", "Leave 3 supportive comments")
            3 -> listOf("Reach out to an accountability partner", "Ask for advice on a struggle")
            4 -> listOf("Host or join a mini-habit pod", "Share a useful tip you learned")
            5 -> listOf("Celebrate someone else's weekly win", "Post your weekly recap")
            6 -> listOf("Attend a social or networking event", "Connect with someone new")
            else -> listOf("Reflect on community impact", "Plan a group activity for next week")
        }
        "Explorer" -> when (dayOfWeek) {
            1 -> listOf("Research a completely new hobby", "Pick a random documentary to watch")
            2 -> listOf("Try a new productivity tool", "Reorganize your daily schedule")
            3 -> listOf("Cook an unfamiliar recipe", "Listen to a distinct genre of music")
            4 -> listOf("Explore a different workout style", "Take a 15-minute curiosity dive")
            5 -> listOf("Explore a new part of your city", "Talk to someone with a different perspective")
            6 -> listOf("Break one routine intentionally", "Visit a new coffee shop or park")
            else -> listOf("Document the week's discoveries", "Brainstorm next week's adventures")
        }
        "Altruist" -> when (dayOfWeek) {
            1 -> listOf("Help someone plan their week", "Dedicate 10 mins to a cause")
            2 -> listOf("Donate a small amount or item", "Compliment a coworker or classmate")
            3 -> listOf("Offer a skill for free to a friend", "Write a thank-you note")
            4 -> listOf("Check in on someone you haven't talked to", "Share a resource that helped you")
            5 -> listOf("Volunteer for a quick 10-minute task", "Highlight someone else's achievement")
            6 -> listOf("Pick up litter in your neighborhood", "Support a local small business")
            else -> listOf("Plan next week's giving", "Reflect on how you helped others")
        }
        "Regulator", "Architect" -> when (dayOfWeek) {
            1 -> listOf("Finalize the weekly blueprint", "Meal prep for Monday & Tuesday")
            2 -> listOf("Audit time-wasting activities", "Strict adherence to block scheduling")
            3 -> listOf("Mid-week schedule realignment", "Review habit compliance score")
            4 -> listOf("Deep work block: 90 mins uninterrupted", "Inbox zero by 5 PM")
            5 -> listOf("Review schedule compliance score", "Plan weekend downtime limits")
            6 -> listOf("Flexible structure day (reduced rules)", "Declutter physical workspace")
            else -> listOf("Prepare Monday's outfit & meals", "Weekly calendar review")
        }
        else -> when (dayOfWeek) {
            1 -> listOf("Monday Motivation Setup", "Define 1 key priority")
            3 -> listOf("Wednesday Check-in", "Adjust course if needed")
            5 -> listOf("Friday Wrap-up", "Log weekly success")
            else -> listOf("Maintain consistent baseline", "Do a quick 5-min stretch")
        }
    }
    return coreGoals + dailyVariations
}

fun generateMockHistory(today: LocalDate, personaType: String): Map<LocalDate, Map<String, Boolean>> {
    val map = mutableMapOf<LocalDate, Map<String, Boolean>>()
    // Generate history for the past 5 days
    for (i in 1..5) {
        val pastDate = today.minusDays(i.toLong())
        val dayGoals = mutableMapOf<String, Boolean>()
        val specificGoals = getGoalsForPersona(personaType, pastDate)

        specificGoals.forEachIndexed { index, goal ->
            // Recent days fully complete, older days partially complete
            dayGoals[goal] = if (i <= 2) true else (index % 2 == 0)
        }
        map[pastDate] = dayGoals
    }
    return map
}

@Composable
fun HomeRoute(personaResult: ClassifyPersonaResponse?) {
    if (personaResult == null) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text(text = "No persona data yet.", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Complete onboarding to unlock your adaptive dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val scrollState = rememberScrollState()

    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    // Dynamic goals fetch based on selected day!
    val goalsForSelectedDate = remember(personaResult.personaType, selectedDate) {
        getGoalsForPersona(personaResult.personaType, selectedDate)
    }

    var checklistState by remember { mutableStateOf(generateMockHistory(today, personaResult.personaType)) }

    // Determine specific persona colors
    val (startColor, endColor) = when (personaResult.personaType) {
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
            .background(Brush.verticalGradient(listOf(startColor.copy(alpha = 0.3f), Color.White)))
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Character Badge with vibrant gradient
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
            text = "Welcome, ${personaResult.personaType}!",
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
            when (personaResult.personaType) {
                "Achiever" -> AchieverHome(personaResult)
                "Grower" -> GrowerHome(personaResult)
                "Socializer" -> SocializerHome(personaResult)
                "Explorer" -> ExplorerHome(personaResult)
                "Altruist" -> AltruistHome(personaResult)
                "Regulator" -> RegulatorHome(personaResult)
                "Architect" -> ArchitectHome(personaResult)
                else -> RegulatorHome(personaResult) // Default fallback to requested Regulator layout
            }

            Spacer(modifier = Modifier.height(32.dp))

            // New interactive Goal Plan Section
            GoalPlanSection(
                goals = goalsForSelectedDate,
                selectedDate = selectedDate,
                today = today,
                checkedGoals = checklistState[selectedDate] ?: emptyMap(),
                onGoalToggled = { goal, isChecked ->
                    val dayMap = (checklistState[selectedDate] ?: emptyMap()).toMutableMap()
                    dayMap[goal] = isChecked
                    val newState = checklistState.toMutableMap()
                    newState[selectedDate] = dayMap
                    checklistState = newState
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // History Calendar Strip
            HistoryCalendarSection(
                today = today,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                checklistState = checklistState,
                personaType = personaResult.personaType
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Phase Indicator
            ProgressPhaseSection(personaResult.personaType)

            Spacer(modifier = Modifier.height(24.dp))

            // Comprehensive Plan Explanation
            PlanExplanationSection(personaResult.personaType)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GoalPlanSection(
    goals: List<String>,
    selectedDate: LocalDate,
    today: LocalDate,
    checkedGoals: Map<String, Boolean>,
    onGoalToggled: (String, Boolean) -> Unit
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

        goals.forEach { goal ->
            val isChecked = checkedGoals[goal] ?: false
            InteractiveGoalItem(
                goalText = goal,
                isChecked = isChecked,
                onCheckedChange = { onGoalToggled(goal, it) }
            )
        }
    }
}

@Composable
private fun HistoryCalendarSection(
    today: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    checklistState: Map<LocalDate, Map<String, Boolean>>,
    personaType: String
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
                val dayChecks = checklistState[date]
                val specificDateGoalsCount = remember(personaType, date) { getGoalsForPersona(personaType, date).size }

                val checkedCount = dayChecks?.count { it.value } ?: 0
                val isCompleted = checkedCount >= specificDateGoalsCount && specificDateGoalsCount > 0
                val isToday = date == today
                val isSelected = date == selectedDate

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
            .clickable { onCheckedChange(!isChecked) },
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
                onCheckedChange = { onCheckedChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF388E3C),
                    uncheckedColor = Color(0xFFFBC02D),
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = goalText,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Bold,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null
            )
        }
    }
}

@Composable
private fun RegulatorHome(personaResult: ClassifyPersonaResponse) {
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
private fun AchieverHome(personaResult: ClassifyPersonaResponse) {
    BaseTemplate(
        title = "ACHIEVER TEMPLATE",
        lines = listOf("⭐ Current Rank: Platinum", "🔥 Streak: 14 days", "🏆 Goal Progress: 80%")
    )
}

@Composable
private fun GrowerHome(personaResult: ClassifyPersonaResponse) {
    BaseTemplate(
        title = "GROWER TEMPLATE",
        lines = listOf("🌱 Daily Reflection Log", "📚 Skills Acquired", "📈 Effort Graph")
    )
}

@Composable
private fun SocializerHome(personaResult: ClassifyPersonaResponse) {
    BaseTemplate(
        title = "SOCIALIZER TEMPLATE",
        lines = listOf("👥 Team Challenges", "🗣️ Community Feed", "🙌 Supportive Cheers Sent: 12")
    )
}

@Composable
private fun ExplorerHome(personaResult: ClassifyPersonaResponse) {
    BaseTemplate(
        title = "EXPLORER TEMPLATE",
        lines = listOf("🧭 New Habits Discovered", "🗺️ Unknown Territory Unlocked", "🚀 Curiosity Quests")
    )
}

@Composable
private fun AltruistHome(personaResult: ClassifyPersonaResponse) {
    BaseTemplate(
        title = "ALTRUIST TEMPLATE",
        lines = listOf("🤝 Points Donated to Charity", "❤️ Friends Assisted", "🌟 Community Impact Score")
    )
}

@Composable
private fun ArchitectHome(personaResult: ClassifyPersonaResponse) {
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
