package com.example.habitflowai.presentation.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    when (personaResult.personaType) {
        "Architect" -> ArchitectHome(personaResult)
        "Achiever" -> AchieverHome(personaResult)
        else -> GenericHome(personaResult)
    }
}

@Composable
private fun Header(title: String, message: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ArchitectHome(personaResult: ClassifyPersonaResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header("Architect Mode", personaResult.motivationalMessage) }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Structure Progress", style = MaterialTheme.typography.titleMedium)
                    Text("Weekly Plan: 80%", style = MaterialTheme.typography.bodyMedium)
                    Text("████████░░", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Checklist", style = MaterialTheme.typography.titleMedium)
                    listOf("Define habit cue", "Pick reminder time", "Track streak daily").forEach {
                        Text("• $it")
                    }
                }
            }
        }
    }
}

@Composable
private fun AchieverHome(personaResult: ClassifyPersonaResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header("Achiever Mode", personaResult.motivationalMessage) }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Streak Counter", style = MaterialTheme.typography.titleMedium)
                    Text("21 Days", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Leaderboard", style = MaterialTheme.typography.titleMedium)
                    listOf("You", "Alex", "Mia").forEachIndexed { index, name ->
                        Text("${index + 1}. $name")
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Achievement Badges", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("7-Day", "Consistency", "Early Bird").forEach {
                            AssistChip(onClick = {}, label = { Text(it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenericHome(personaResult: ClassifyPersonaResponse) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(personaResult.personaType, personaResult.motivationalMessage)
        Spacer(Modifier.height(8.dp))
        Text("Your personalized dashboard will evolve as you log habits.")
    }
}
