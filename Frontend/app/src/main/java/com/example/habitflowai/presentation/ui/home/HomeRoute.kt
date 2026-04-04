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

fun getGoalsForPersona(personaType: String, date: LocalDate): List<Pair<String, Int>> {
    val dayOfWeek = date.dayOfWeek.value // 1 (Monday) to 7 (Sunday)

    val coreGoals = when (personaType) {
        "Achiever" -> listOf("Complete 3 high-priority tasks" to 50, "Log all daily stats" to 20, "Drink 2L of water" to 10, "Review leaderboard standing" to 15)
        "Grower" -> listOf("Write in reflection journal" to 0, "Observe a new emotional trigger" to 0, "Read 10 pages" to 0, "Practice mindfulness for 10 mins" to 0)
        "Socializer" -> listOf("Share progress on feed" to 0, "Cheer on 2 community members" to 0, "Message accountability partner" to 0, "Check group chat" to 0)
        "Explorer" -> listOf("Take a different route or approach" to 0, "Read an article on a new topic" to 0, "Try a new healthy snack" to 0, "Log one new discovery" to 0)
        "Altruist" -> listOf("Do one random act of kindness" to 0, "Leave positive feedback for a peer" to 0, "Check in on family/friend" to 0, "Express gratitude publicly" to 0)
        "Regulator", "Architect" -> listOf("Wake up by 6:00 AM" to 0, "No screens after 9:00 PM" to 0, "Follow morning protocol" to 0, "Track every hour of deep work" to 0)
        else -> listOf("Log daily habit" to 0, "Review end of day" to 0, "Stay hydrated" to 0, "Plan for tomorrow" to 0)
    }

    val dailyVariations = when (personaType) {
        "Achiever" -> when (dayOfWeek) {
            1 -> listOf("Plan the week's biggest win" to 30, "Set 3 micro-goals for Monday" to 20)
            2 -> listOf("Speed-run a minor task" to 25, "Optimize workspace for focus" to 15)
            3 -> listOf("Review halfway milestone tracker" to 25, "Push limits on core metric" to 50)
            4 -> listOf("Eliminate one distraction" to 20, "Review weekly metric velocity" to 30)
            5 -> listOf("Wrap up weekly main objective" to 100, "Log end-of-week reflection" to 20)
            6 -> listOf("Active recovery & low-intensity tracking" to 10, "Prepare next week's layout" to 25)
            else -> listOf("Strategize next week's targets" to 20, "Rest and visualize success" to 10)
        }
        "Grower" -> when (dayOfWeek) {
            1 -> listOf("Set a new learning intention" to 0, "Listen to a growth podcast" to 0)
            2 -> listOf("Practice a new skill for 20 mins" to 0, "Identify one limiting belief" to 0)
            3 -> listOf("Re-read notes from a favorite book" to 0, "Meditate on mid-week stress" to 0)
            4 -> listOf("Apply one learning to daily life" to 0, "Seek constructive feedback" to 0)
            5 -> listOf("Summarize the week's learnings" to 0, "Journal about a hurdle faced" to 0)
            6 -> listOf("Spend time in nature without tech" to 0, "Explore a creative outlet" to 0)
            else -> listOf("Plan personal growth goals for next week" to 0, "Rest your mind" to 0)
        }
        "Socializer" -> when (dayOfWeek) {
            1 -> listOf("Kick off the week with a team message" to 0, "Set a shared goal with a friend" to 0)
            2 -> listOf("Join a quick group challenge" to 0, "Leave 3 supportive comments" to 0)
            3 -> listOf("Reach out to an accountability partner" to 0, "Ask for advice on a struggle" to 0)
            4 -> listOf("Host or join a mini-habit pod" to 0, "Share a useful tip you learned" to 0)
            5 -> listOf("Celebrate someone else's weekly win" to 0, "Post your weekly recap" to 0)
            6 -> listOf("Attend a social or networking event" to 0, "Connect with someone new" to 0)
            else -> listOf("Reflect on community impact" to 0, "Plan a group activity for next week" to 0)
        }
        "Explorer" -> when (dayOfWeek) {
            1 -> listOf("Research a completely new hobby" to 0, "Pick a random documentary to watch" to 0)
            2 -> listOf("Try a new productivity tool" to 0, "Reorganize your daily schedule" to 0)
            3 -> listOf("Cook an unfamiliar recipe" to 0, "Listen to a distinct genre of music" to 0)
            4 -> listOf("Explore a different workout style" to 0, "Take a 15-minute curiosity dive" to 0)
            5 -> listOf("Explore a new part of your city" to 0, "Talk to someone with a different perspective" to 0)
            6 -> listOf("Break one routine intentionally" to 0, "Visit a new coffee shop or park" to 0)
            else -> listOf("Document the week's discoveries" to 0, "Brainstorm next week's adventures" to 0)
        }
        "Altruist" -> when (dayOfWeek) {
            1 -> listOf("Help someone plan their week" to 0, "Dedicate 10 mins to a cause" to 0)
            2 -> listOf("Donate a small amount or item" to 0, "Compliment a coworker or classmate" to 0)
            3 -> listOf("Offer a skill for free to a friend" to 0, "Write a thank-you note" to 0)
            4 -> listOf("Check in on someone you haven't talked to" to 0, "Share a resource that helped you" to 0)
            5 -> listOf("Volunteer for a quick 10-minute task" to 0, "Highlight someone else's achievement" to 0)
            6 -> listOf("Pick up litter in your neighborhood" to 0, "Support a local small business" to 0)
            else -> listOf("Plan next week's giving" to 0, "Reflect on how you helped others" to 0)
        }
        "Regulator", "Architect" -> when (dayOfWeek) {
            1 -> listOf("Finalize the weekly blueprint" to 0, "Meal prep for Monday & Tuesday" to 0)
            2 -> listOf("Audit time-wasting activities" to 0, "Strict adherence to block scheduling" to 0)
            3 -> listOf("Mid-week schedule realignment" to 0, "Review habit compliance score" to 0)
            4 -> listOf("Deep work block: 90 mins uninterrupted" to 0, "Inbox zero by 5 PM" to 0)
            5 -> listOf("Review schedule compliance score" to 0, "Plan weekend downtime limits" to 0)
            6 -> listOf("Flexible structure day (reduced rules)" to 0, "Declutter physical workspace" to 0)
            else -> listOf("Prepare Monday's outfit & meals" to 0, "Weekly calendar review" to 0)
        }
        else -> when (dayOfWeek) {
            1 -> listOf("Monday Motivation Setup" to 0, "Define 1 key priority" to 0)
            3 -> listOf("Wednesday Check-in" to 0, "Adjust course if needed" to 0)
            5 -> listOf("Friday Wrap-up" to 0, "Log weekly success" to 0)
            else -> listOf("Maintain consistent baseline" to 0, "Do a quick 5-min stretch" to 0)
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

        specificGoals.forEachIndexed { index, (goal, _) ->
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
                },
                personaType = personaResult.personaType
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
    goals: List<Pair<String, Int>>,
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

        goals.forEach { (goal, score) ->
            val isChecked = checkedGoals[goal] ?: false
            InteractiveGoalItem(
                goalText = goal,
                score = if (personaType == "Achiever") score else 0,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goalText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Bold,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null
                )
            }
            if (false) {
                Spacer(modifier = Modifier.width(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isChecked) Color(0xFF4CAF50) else Color(0xFFFFB300))
                ) {
                    Text(
                        text = "+$score pts",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
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
        // Score removed to cancel points display
    }
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
