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
import androidx.compose.ui.unit.sp
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import com.habitflowai.presentation.ui.persona.PersonaDetails
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.HabitsViewModel

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
    habit: HabitEntity?,
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
                            text = habit.description.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (habit.completed) Color(0xFFC8E6C9) else Color(0xFFFFF9C4)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (habit.completed) "Completed" else "In Progress",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (habit.completed) Color(0xFF2E7D32) else Color(0xFFF57F17)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Completion History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CompletionCalendar(
                            personaColor = details.endColor,
                            completionHistory = habit.completionHistory
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Frequency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = habit.frequency.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionCalendar(
    personaColor: Color,
    completionHistory: List<String> = emptyList()
) {
    val today = LocalDate.now()
    val days = remember {
        (0 until 28).map { today.minusDays(it.toLong()) }.reversed()
    }
    
    // Parse completion history dates
    val completionDates = remember(completionHistory) {
        completionHistory.mapNotNull {
            try {
                // Try to parse typical ISO formats
                if (it.contains("T")) {
                    java.time.ZonedDateTime.parse(it).toLocalDate()
                } else {
                    LocalDate.parse(it)
                }
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }

    val completionMap = remember(days, completionDates) {
        days.associateWith { it in completionDates }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Last 28 Days",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Text(
                text = "${completionMap.values.count { it }}/${days.size} Completed",
                style = MaterialTheme.typography.labelMedium,
                color = personaColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(days) { day ->
                val isCompleted = completionMap[day] ?: false
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) personaColor else personaColor.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompleted) Color.White else personaColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Achiever Detail")
@Composable
fun HabitDetailAchieverPreview() {
    HabitFlowTheme {
        HabitDetailContent(
            habit = HabitEntity(
                id = "1",
                title = "Weightlifting",
                description = "Push day at the gym",
                frequency = "DAILY",
                userId = "user1",
                completed = false,
                completionHistory = listOf(
                    LocalDate.now().minusDays(1).toString(),
                    LocalDate.now().minusDays(3).toString(),
                    LocalDate.now().minusDays(5).toString()
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
            habit = HabitEntity(
                id = "2",
                title = "Meditation",
                description = "10 minutes of mindfulness",
                frequency = "DAILY",
                userId = "user1",
                completed = false,
                completionHistory = listOf(
                    LocalDate.now().minusDays(1).toString(),
                    LocalDate.now().minusDays(2).toString(),
                    LocalDate.now().minusDays(4).toString()
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
            habit = HabitEntity(
                id = "3",
                title = "Morning Protocol",
                description = "Wake up at 6 AM",
                frequency = "DAILY",
                userId = "user1",
                completed = false,
                completionHistory = listOf(
                    LocalDate.now().minusDays(1).toString(),
                    LocalDate.now().minusDays(2).toString(),
                    LocalDate.now().minusDays(3).toString(),
                    LocalDate.now().minusDays(4).toString()
                )
            ),
            personaType = "Regulator",
            onBack = {}
        )
    }
}
