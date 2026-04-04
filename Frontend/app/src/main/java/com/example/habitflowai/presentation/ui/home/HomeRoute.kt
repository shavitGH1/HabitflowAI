package com.example.habitflowai.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Character Badge
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Face,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome, ${personaResult.personaType}!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your tailored dashboard is ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

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
    }
}

@Composable
private fun RegulatorHome(personaResult: ClassifyPersonaResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "STRUCTURE & ROUTINE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider()

            Text(text = "Current Consistency Score", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(
                progress = { 0.88f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF4CAF50),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(text = "88% Excellent", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Today's Prescribed Hours", style = MaterialTheme.typography.labelLarge)

            TimeSlotCard("06:00 AM", "Morning Protocol & Hydration")
            TimeSlotCard("08:00 AM", "Deep Work Block")
            TimeSlotCard("01:00 PM", "Nutrition & Movement")
            TimeSlotCard("09:00 PM", "Evening Wind Down")
        }
    }
}

@Composable
private fun TimeSlotCard(time: String, activity: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = time, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = activity, style = MaterialTheme.typography.bodyMedium)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            lines.forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
