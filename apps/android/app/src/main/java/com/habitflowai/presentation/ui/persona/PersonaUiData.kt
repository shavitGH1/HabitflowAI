package com.habitflowai.presentation.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class PersonaDetails(
    val type: String,
    val startColor: Color,
    val endColor: Color,
    val tips: List<String>,
    val challenges: List<String>
)

object PersonaUiData {
    fun getDetails(type: String): PersonaDetails {
        val normalizedType = type.replaceFirstChar { it.uppercase() }
        return when (normalizedType) {
            "Achiever" -> PersonaDetails(
                type = "Achiever",
                startColor = Color(0xFFFFD54F),
                endColor = Color(0xFFFF8A65),
                tips = listOf(
                    "Set micro-goals to maintain constant momentum.",
                    "Track your streaks visually to boost motivation.",
                    "Reward yourself for reaching major milestones."
                ),
                challenges = listOf("Burnout from over-performance", "Perfectionism stalling progress")
            )
            "Grower" -> PersonaDetails(
                type = "Grower",
                startColor = Color(0xFFAED581),
                endColor = Color(0xFF4DB6AC),
                tips = listOf(
                    "Journal daily to reflect on your internal progress.",
                    "Focus on skill acquisition rather than just task completion.",
                    "Practice mindfulness to stay connected to your 'why'."
                ),
                challenges = listOf("Impatience with slow results", "Over-analyzing every setback")
            )
            "Regulator" -> PersonaDetails(
                type = "Regulator",
                startColor = Color(0xFF64B5F6),
                endColor = Color(0xFF1E88E5),
                tips = listOf(
                    "Use time-blocking to give every hour a purpose.",
                    "Standardize your morning and evening protocols.",
                    "Plan for disruptions before they happen."
                ),
                challenges = listOf("Rigidity in changing environments", "Anxiety when routines are broken")
            )
            "Architect" -> PersonaDetails(
                type = "Architect",
                startColor = Color(0xFF64B5F6),
                endColor = Color(0xFF1E88E5),
                tips = listOf(
                    "Map out habit dependencies to find efficiency.",
                    "Audit your physical environment for friction.",
                    "Design systems that make failure difficult."
                ),
                challenges = listOf("Complexifying simple tasks", "Analysis paralysis during planning")
            )
            "Socializer" -> PersonaDetails(
                type = "Socializer",
                startColor = Color(0xFFBA68C8),
                endColor = Color(0xFFF06292),
                tips = listOf(
                    "Find an accountability partner for your core habits.",
                    "Share your wins in the community feed daily.",
                    "Join group challenges to stay engaged."
                ),
                challenges = listOf("Distraction by social elements", "Relying too heavily on external praise")
            )
            "Explorer" -> PersonaDetails(
                type = "Explorer",
                startColor = Color(0xFFFFB74D),
                endColor = Color(0xFFE57373),
                tips = listOf(
                    "Change your environment frequently to stay fresh.",
                    "Gamify your most boring tasks.",
                    "Try new formats for old habits every 7 days."
                ),
                challenges = listOf("Quitting when novelty fades", "Lack of deep focus on routine tasks")
            )
            "Altruist" -> PersonaDetails(
                type = "Altruist",
                startColor = Color(0xFFF48FB1),
                endColor = Color(0xFFCE93D8),
                tips = listOf(
                    "Connect your personal habits to a larger cause.",
                    "Mentor others in the community.",
                    "Focus on the legacy your consistency creates."
                ),
                challenges = listOf("Neglecting self-care for others", "Emotional exhaustion from empathy")
            )
            else -> PersonaDetails(
                type = "Regulator",
                startColor = Color(0xFF81D4FA),
                endColor = Color(0xFFCE93D8),
                tips = listOf("Consistency is key.", "Keep showing up.", "Build your foundation."),
                challenges = listOf("Inconsistency", "Lack of clear goals")
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersonaDataGalleryPreview() {
    val personas = listOf("Achiever", "Grower", "Regulator", "Architect", "Socializer", "Explorer", "Altruist")
    MaterialTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Persona UI Data Gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(personas) { type ->
                val details = PersonaUiData.getDetails(type)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(details.startColor, details.endColor)))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = details.type,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tips: ${details.tips.size} | Challenges: ${details.challenges.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
