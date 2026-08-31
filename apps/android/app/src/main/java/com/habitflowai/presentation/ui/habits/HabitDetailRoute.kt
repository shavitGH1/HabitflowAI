package com.habitflowai.presentation.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.local.entity.SyncStatus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import java.time.LocalDate
import java.time.ZonedDateTime
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.habitflowai.presentation.ui.persona.PersonaDetails
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.HabitsViewModel

private const val MIN_STREAK_FOR_MANUAL_ACHIEVEMENT = 21

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
    val stats = uiState.habitStats[habitId]
    val locations = uiState.habitLocations[habitId] ?: emptyList()

    LaunchedEffect(habitId) {
        viewModel.fetchHabitStats(habitId)
        viewModel.fetchHabitLocations(habitId)
    }

    HabitDetailContent(
        habit = habit,
        stats = stats,
        locations = locations,
        personaType = personaType,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onBack = onBack,
        onComplete = { note, isPublic ->
            viewModel.completeHabit(habitId, note, isPublic) { success ->
                if (success) onBack()
            }
        },
        onAbandon = {
            viewModel.deleteHabit(habitId)
            onBack()
        },
        onMarkAchieved = { viewModel.markHabitAchieved(habitId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailContent(
    habit: HabitEntity?,
    stats: Map<String, Any>?,
    locations: List<LocationEntity> = emptyList(),
    personaType: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit,
    onComplete: (String?, Boolean) -> Unit = { _, _ -> },
    onAbandon: () -> Unit = {},
    onMarkAchieved: () -> Unit = {}
) {
    val details: PersonaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showAbandonConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            var shareOnPublicMap by remember { mutableStateOf(true) }

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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (!habit.relevanceWarning.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SmartToy,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = habit.relevanceWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
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
                            text = "Today's Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (habit.completed) "Completed today" else "Not done yet today",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (habit.completed) Color(0xFF2E7D32) else Color(0xFFF57F17)
                        )
                    }
                }

                if (!habit.completed) {
                    var completionNote by remember { mutableStateOf("") }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Mark as Complete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = completionNote,
                                onValueChange = { completionNote = it },
                                label = { Text("What did you do? (Note)") },
                                placeholder = { Text("AI will check for plausibility") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Text(
                                text = "Record this completion and save your location on the map.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Share location on public map",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Off = private, only visible to you",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = shareOnPublicMap,
                                    onCheckedChange = { shareOnPublicMap = it },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = details.endColor,
                                        checkedThumbColor = Color.White
                                    )
                                )
                            }
                            
                            if (!habit.verificationWarning.isNullOrEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.SmartToy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = habit.verificationWarning,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { onComplete(completionNote.trim().ifBlank { null }, shareOnPublicMap) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = details.endColor),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Mark Complete", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (habit.implementedAt == null && habit.streak >= MIN_STREAK_FOR_MANUAL_ACHIEVEMENT) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Complete Habit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You've kept a ${habit.streak}-day streak — mark this habit as achieved now instead of waiting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onMarkAchieved,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = details.endColor),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Complete Habit", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (stats != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Statistics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            stats.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = key.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = value.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

                if (locations.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Completion Locations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HabitLocationsMap(locations = locations)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showAbandonConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Abandon Habit", fontWeight = FontWeight.Bold)
                }
            }

            if (showAbandonConfirm) {
                AlertDialog(
                    onDismissRequest = { showAbandonConfirm = false },
                    title = { Text("Abandon this habit?") },
                    text = { Text("This will delete \"${habit.title}\" and its completion history. This can't be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showAbandonConfirm = false
                            onAbandon()
                        }) {
                            Text("Abandon", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAbandonConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HabitLocationsMap(locations: List<LocationEntity>) {
    val centerLat = locations.map { it.latitude }.average()
    val centerLng = locations.map { it.longitude }.average()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 13f)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp)),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false
        )
    ) {
        locations.forEach { location ->
            Marker(state = MarkerState(position = LatLng(location.latitude, location.longitude)))
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

    val completionDates = remember(completionHistory) {
        completionHistory.mapNotNull {
            try {
                if (it.length == 10 && it.count { c -> c == '-' } == 2) {
                    LocalDate.parse(it)
                } else {
                    ZonedDateTime.parse(it).toLocalDate()
                }
            } catch (_: Exception) {
                try {
                    java.time.LocalDateTime.parse(it).toLocalDate()
                } catch (_: Exception) {
                    try {
                        java.time.Instant.parse(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    } catch (_: Exception) {
                        null
                    }
                }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${completionMap.values.count { it }}/${days.size} Completed",
                style = MaterialTheme.typography.labelMedium,
                color = personaColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        val gridSpacing = 8.dp
        val leadingBlanks = remember(days) { days.first().dayOfWeek.value % 7 }
        val gridItems: List<LocalDate?> = remember(days, leadingBlanks) {
            List(leadingBlanks) { null } + days
        }
        val gridRows = (gridItems.size + 6) / 7

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gridSpacing)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(32.dp * gridRows + gridSpacing * (gridRows - 1)),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            userScrollEnabled = false
        ) {
            items(gridItems) { day ->
                if (day == null) {
                    Box(modifier = Modifier.size(32.dp))
                } else {
                    val isCompleted = completionMap[day] ?: false
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
            stats = mapOf("consistency" to "85%", "total completions" to 12),
            personaType = "Achiever",
            onComplete = { _, _ -> },
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
            stats = mapOf("streak" to 5),
            personaType = "Grower",
            onComplete = { _, _ -> },
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
            stats = mapOf("on time" to "90%"),
            personaType = "Regulator",
            onComplete = { _, _ -> },
            onBack = {}
        )
    }
}
