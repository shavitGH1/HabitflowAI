package com.habitflowai.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habitflowai.data.model.ClassifyPersonaResponse

@Composable
fun HomeScreen(personaResponse: ClassifyPersonaResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Home", style = MaterialTheme.typography.headlineMedium)
        Text(text = personaResponse.motivationalMessage)

        when (personaResponse.personaType) {
            "Architect" -> ArchitectHomeContent()
            "Achiever" -> AchieverHomeContent()
            else -> Text(text = "Unknown persona")
        }
    }
}

@Composable
private fun ArchitectHomeContent() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Structure Mode", style = MaterialTheme.typography.titleLarge)
            Text(text = "Progress Chart: ████░░░░")
            Text(text = "Checklist: ✅ Morning routine\n✅ Water intake\n⬜ Evening review")
        }
    }
}

@Composable
private fun AchieverHomeContent() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Gamification Mode", style = MaterialTheme.typography.titleLarge)
            Text(text = "Leaderboard: #1 in your cohort")
            Text(text = "Streak Counter: 12 days")
            Text(text = "Badges: ⭐ Consistency • 🔥 Streak Master • 🏅 Early Bird")
        }
    }
}
