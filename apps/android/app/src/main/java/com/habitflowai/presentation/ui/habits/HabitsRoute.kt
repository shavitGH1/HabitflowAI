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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
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
import com.habitflowai.presentation.ui.persona.PersonaDetails
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.HabitsUiState
import com.habitflowai.presentation.viewmodel.HabitsViewModel

@Composable
fun HabitsRoute(
    viewModel: HabitsViewModel,
    personaType: String,
    onHabitClick: (String) -> Unit,
    onToggleChat: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    HabitsContent(
        uiState = uiState,
        personaType = personaType,
        onHabitClick = onHabitClick,
        onAddHabit = viewModel::addHabit,
        onDeleteHabit = viewModel::deleteHabit,
        onToggleChat = onToggleChat
    )
}

@Composable
fun HabitsContent(
    uiState: HabitsUiState,
    personaType: String,
    onHabitClick: (String) -> Unit,
    onAddHabit: (String, String, String) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onToggleChat: () -> Unit
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    val details: PersonaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Habit Hub", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onToggleChat) {
                        Icon(
                            Icons.Rounded.SmartToy,
                            contentDescription = "AI Assistant",
                            tint = details.endColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = details.endColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Habit")
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Master your routine, one day at a time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(24.dp))

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
                            key = { habit: HabitEntity -> habit.id }
                        ) { habit: HabitEntity ->
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
}

@Composable
fun HabitItem(
    habit: HabitEntity,
    personaColor: Color,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = personaColor
                )
                if (!habit.description.isNullOrEmpty()) {
                    Text(
                        text = habit.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete Habit",
                    tint = Color.Gray.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun HabitCreateBottomSheet(
    personaColor: Color,
    onDismiss: () -> Unit,
    onHabitCreated: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("DAILY") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                label = { Text("What habit to build?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { onHabitCreated(title, description, frequency) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                enabled = title.isNotBlank()
            ) {
                Text("Create Habit", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitsWithChatPreview() {
    HabitFlowTheme {
        HabitsContent(
            uiState = HabitsUiState(),
            personaType = "Grower",
            onHabitClick = {},
            onAddHabit = { _, _, _ -> },
            onDeleteHabit = {},
            onToggleChat = {}
        )
    }
}
