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
                else -> RegulatorHome(personaResult)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // New interactive Goal Plan Section
        GoalPlanSection(personaResult.personaType)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GoalPlanSection(personaType: String) {
    val goals = remember(personaType) {
        when (personaType) {
            "Achiever" -> listOf("Complete 3 high-priority tasks", "Win the daily challenge", "Log all meals")
            "Grower" -> listOf("Read 10 pages of a book", "Write in reflection journal", "Learn a new concept")
            "Socializer" -> listOf("Share progress with a friend", "Cheer on 3 community members", "Join a group challenge")
            "Explorer" -> listOf("Try a completely new habit", "Take a different route on your walk", "Research a new hobby")
            "Altruist" -> listOf("Do a random act of kindness", "Help a friend achieve a goal", "Donate to a local charity")
            "Regulator", "Architect" -> listOf("Wake up at exactly 6:00 AM", "Complete Morning Protocol", "No screens after 9:00 PM")
            else -> listOf("Set your first goal", "Complete your first task", "Review your day")
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Your Action Plan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        goals.forEach { goal ->
            InteractiveGoalItem(goal)
        }
    }
}

@Composable
private fun InteractiveGoalItem(goalText: String) {
    var isChecked by remember { mutableStateOf(false) }

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
            .clickable { isChecked = !isChecked },
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
                onCheckedChange = { isChecked = it },
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
