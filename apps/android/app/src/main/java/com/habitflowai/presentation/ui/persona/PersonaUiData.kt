package com.habitflowai.presentation.ui.persona

import androidx.compose.ui.graphics.Color

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
