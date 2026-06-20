@file:OptIn(ExperimentalMaterial3Api::class)
package com.habitflowai.presentation.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.data.model.HabitFrequency
import com.habitflowai.presentation.ui.persona.PersonaDetails
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.HabitsUiState
import com.habitflowai.presentation.viewmodel.HabitsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsRoute(
    viewModel: HabitsViewModel,
    personaType: String,
    onHabitClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    HabitsContent(
        uiState = uiState,
        personaType = personaType,
        onHabitClick = onHabitClick,
        onAddHabit = viewModel::addHabit,
        onDeleteHabit = viewModel::deleteHabit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsContent(
    uiState: HabitsUiState,
    personaType: String,
    onHabitClick: (String) -> Unit,
    onAddHabit: (String, String, String) -> Unit,
    onDeleteHabit: (String) -> Unit
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    val details: PersonaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        details.startColor.copy(alpha = 0.15f),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Habit Hub",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = details.endColor
            )
            
            Text(
                text = "Master your routine, one day at a time.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = details.endColor)
                }
            } else if (uiState.habits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No habits yet. Tap + to start!", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    items(
                        items = uiState.habits,
                        key = { it.id }
                    ) { habit ->
                        HabitItem(
                            habit = habit,
                            personaColor = details.endColor,
                            onDelete = { onDeleteHabit(habit.id) },
                            onClick = { onHabitClick(habit.id) }
                        )
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showCreateSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = details.endColor,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add Habit")
        }

        if (showCreateSheet) {
            HabitCreateBottomSheet(
                personaColor = details.endColor,
                onDismiss = { showCreateSheet = false },
                onHabitCreated = { title, desc, freq ->
                    onAddHabit(title, desc, freq)
                    showCreateSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitItem(
    habit: HabitEntity,
    personaColor: Color,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Color.Red.copy(alpha = 0.2f)
            } else Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(personaColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = habit.title.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = personaColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = habit.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = habit.frequency.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = personaColor,
                    modifier = Modifier
                        .background(personaColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCreateBottomSheet(
    personaColor: Color,
    onDismiss: () -> Unit,
    onHabitCreated: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("DAILY") }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "New Habit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = personaColor
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = personaColor,
                    focusedLabelColor = personaColor
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = personaColor,
                    focusedLabelColor = personaColor
                )
            )

            Text(
                text = "Frequency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq.name,
                        onClick = { frequency = freq.name },
                        label = { 
                            Text(
                                text = freq.name.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) 
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = personaColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onHabitCreated(title, description, frequency)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = personaColor)
            ) {
                Text("Create Habit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Achiever Hub")
@Composable
fun HabitsAchieverPreview() {
    HabitFlowTheme {
        HabitsContent(
            uiState = HabitsUiState(
                habits = listOf(
                    HabitEntity("1", "Workout", "Gym session", "DAILY", "", false),
                    HabitEntity("2", "Beat PR", "Run faster", "WEEKLY", "", false)
                )
            ),
            personaType = "Achiever",
            onHabitClick = {},
            onAddHabit = { _, _, _ -> },
            onDeleteHabit = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Grower Hub")
@Composable
fun HabitsGrowerPreview() {
    HabitFlowTheme {
        HabitsContent(
            uiState = HabitsUiState(
                habits = listOf(
                    HabitEntity("1", "Meditation", "10 mins daily", "DAILY", "", false),
                    HabitEntity("2", "Journaling", "Reflect on day", "DAILY", "", false)
                )
            ),
            personaType = "Grower",
            onHabitClick = {},
            onAddHabit = { _, _, _ -> },
            onDeleteHabit = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Regulator Hub")
@Composable
fun HabitsRegulatorPreview() {
    HabitFlowTheme {
        HabitsContent(
            uiState = HabitsUiState(
                habits = listOf(
                    HabitEntity("1", "Morning Routine", "Follow schedule", "DAILY", "", false),
                    HabitEntity("2", "Deep Work", "Block time", "DAILY", "", false)
                )
            ),
            personaType = "Regulator",
            onHabitClick = {},
            onAddHabit = { _, _, _ -> },
            onDeleteHabit = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Socializer Hub")
@Composable
fun HabitsSocializerPreview() {
    HabitFlowTheme {
        HabitsContent(
            uiState = HabitsUiState(
                habits = listOf(
                    HabitEntity("1", "Call Friend", "Stay connected", "WEEKLY", "", false),
                    HabitEntity("2", "Group Class", "Fitness with others", "WEEKLY", "", false)
                )
            ),
            personaType = "Socializer",
            onHabitClick = {},
            onAddHabit = { _, _, _ -> },
            onDeleteHabit = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Explorer Hub")
@Composable
fun HabitsExplorerPreview() {
    HabitFlowTheme {
        HabitsContent(
            uiState = HabitsUiState(
                habits = listOf(
                    HabitEntity("1", "Try New Food", "Explore cuisine", "WEEKLY", "", false),
                    HabitEntity("2", "Random Walk", "Discover paths", "DAILY", "", false)
                )
            ),
            personaType = "Explorer",
            onHabitClick = {},
            onAddHabit = { _, _, _ -> },
            onDeleteHabit = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Altruist Hub")
@Composable
fun HabitsAltruistPreview() {
    HabitFlowTheme {
        HabitsContent(
            uiState = HabitsUiState(
                habits = listOf(
                    HabitEntity("1", "Volunteer", "Community help", "MONTHLY", "", false),
                    HabitEntity("2", "Help Neighbor", "Small acts", "WEEKLY", "", false)
                )
            ),
            personaType = "Altruist",
            onHabitClick = {},
            onAddHabit = { _, _, _ -> },
            onDeleteHabit = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HabitCreateBottomSheetPreview() {
    HabitFlowTheme {
        Surface {
            HabitCreateBottomSheet(
                personaColor = Color(0xFFBA68C8),
                onDismiss = {},
                onHabitCreated = { _, _, _ -> }
            )
        }
    }
}
