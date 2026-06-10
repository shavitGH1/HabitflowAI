package com.habitflowai.presentation.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.habitflowai.data.model.Habit
import com.habitflowai.data.model.HabitFrequency
import com.habitflowai.presentation.ui.persona.PersonaDetails
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.HabitsViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailRoute(
    habitId: String,
    viewModel: HabitsViewModel,
    personaType: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val habit = uiState.habits.find { it.id == habitId }

    HabitDetailContent(
        habit = habit,
        personaType = personaType,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailContent(
    habit: Habit?,
    personaType: String,
    onBack: () -> Unit
) {
    val details: PersonaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (habit == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Habit not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = details.startColor.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = details.endColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray
                        )
                    }
                }

                Text(
                    text = "Completion History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                CompletionCalendar(habit.completionHistory, details.endColor)
            }
        }
    }
}

@Composable
fun CompletionCalendar(history: List<LocalDate>, highlightColor: Color) {
    val today = LocalDate.now()
    val days = (0..27).map { today.minusDays(it.toLong()) }.reversed()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Last 28 Days", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text("${history.size} completions", style = MaterialTheme.typography.labelMedium, color = highlightColor)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        val rows = days.chunked(7)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { date ->
                    val isCompleted = history.contains(date)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                color = if (isCompleted) highlightColor else Color.LightGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Achiever Detail")
@Composable
fun HabitDetailAchieverPreview() {
    HabitFlowTheme {
        HabitDetailContent(
            habit = Habit(
                id = "1",
                title = "Weightlifting",
                description = "Push day at the gym",
                frequency = HabitFrequency.DAILY,
                completionHistory = listOf(
                    LocalDate.now().minusDays(1),
                    LocalDate.now().minusDays(2),
                    LocalDate.now().minusDays(4)
                )
            ),
            personaType = "Achiever",
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Grower Detail")
@Composable
fun HabitDetailGrowerPreview() {
    HabitFlowTheme {
        HabitDetailContent(
            habit = Habit(
                id = "2",
                title = "Meditation",
                description = "10 minutes of mindfulness",
                frequency = HabitFrequency.DAILY,
                completionHistory = listOf(
                    LocalDate.now().minusDays(1),
                    LocalDate.now().minusDays(3),
                    LocalDate.now().minusDays(5)
                )
            ),
            personaType = "Grower",
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Regulator Detail")
@Composable
fun HabitDetailRegulatorPreview() {
    HabitFlowTheme {
        HabitDetailContent(
            habit = Habit(
                id = "3",
                title = "Morning Protocol",
                description = "Wake up at 6 AM",
                frequency = HabitFrequency.DAILY,
                completionHistory = listOf(
                    LocalDate.now().minusDays(1),
                    LocalDate.now().minusDays(2),
                    LocalDate.now().minusDays(3)
                )
            ),
            personaType = "Regulator",
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Architect Detail")
@Composable
fun HabitDetailArchitectPreview() {
    HabitFlowTheme {
        HabitDetailContent(
            habit = Habit(
                id = "4",
                title = "Deep Work",
                description = "90 minutes of focused coding",
                frequency = HabitFrequency.DAILY,
                completionHistory = listOf(
                    LocalDate.now().minusDays(2),
                    LocalDate.now().minusDays(4),
                    LocalDate.now().minusDays(6)
                )
            ),
            personaType = "Architect",
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Socializer Detail")
@Composable
fun HabitDetailSocializerPreview() {
    HabitFlowTheme {
        HabitDetailContent(
            habit = Habit(
                id = "5",
                title = "Group Run",
                description = "Weekly 5km with the club",
                frequency = HabitFrequency.WEEKLY,
                completionHistory = listOf(
                    LocalDate.now().minusDays(7),
                    LocalDate.now().minusDays(14)
                )
            ),
            personaType = "Socializer",
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Explorer Detail")
@Composable
fun HabitDetailExplorerPreview() {
    HabitFlowTheme {
        HabitDetailContent(
            habit = Habit(
                id = "6",
                title = "New Recipe",
                description = "Cook something new once a week",
                frequency = HabitFrequency.WEEKLY,
                completionHistory = listOf(
                    LocalDate.now().minusDays(5),
                    LocalDate.now().minusDays(12)
                )
            ),
            personaType = "Explorer",
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Altruist Detail")
@Composable
fun HabitDetailAltruistPreview() {
    HabitFlowTheme {
        HabitDetailContent(
            habit = Habit(
                id = "7",
                title = "Volunteering",
                description = "Helping at the local shelter",
                frequency = HabitFrequency.MONTHLY,
                completionHistory = listOf(
                    LocalDate.now().minusDays(20)
                )
            ),
            personaType = "Altruist",
            onBack = {}
        )
    }
}
