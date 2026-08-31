package com.habitflowai.presentation.ui.goals

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.model.ActiveGoalResponse
import com.habitflowai.presentation.ui.persona.PersonaDetails
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.viewmodel.HabitsViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailRoute(
    goalId: String,
    viewModel: HabitsViewModel,
    personaType: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val goal = uiState.activeGoal?.takeIf { it.id == goalId }
    val achievedHabits = remember(uiState.habits, goalId) {
        uiState.habits.filter { it.goalId == goalId && it.implementedAt != null }
    }

    GoalDetailContent(
        goal = goal,
        achievedHabits = achievedHabits,
        personaType = personaType,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        pendingHabitDecision = uiState.pendingHabitDecision,
        onBack = onBack,
        onTransition = { resolution, newGoalTitle, newGoalTargetDate ->
            viewModel.transitionGoal(goalId, resolution, newGoalTitle, newGoalTargetDate) { success ->
                if (success) onBack()
            }
        },
        onRetryResolveHabits = {
            viewModel.retryResolveHabits { success -> if (success) onBack() }
        },
        onDecideHabits = { decision ->
            viewModel.decideHabits(decision) { success -> if (success) onBack() }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailContent(
    goal: ActiveGoalResponse?,
    achievedHabits: List<HabitEntity>,
    personaType: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    pendingHabitDecision: com.habitflowai.presentation.viewmodel.HabitsUiState.PendingHabitDecision? = null,
    onBack: () -> Unit,
    onTransition: (resolution: String, newGoalTitle: String, newGoalTargetDate: String) -> Unit = { _, _, _ -> },
    onRetryResolveHabits: () -> Unit = {},
    onDecideHabits: (decision: String) -> Unit = {}
) {
    val details: PersonaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showAchieveConfirm by remember { mutableStateOf(false) }
    var showForfeitConfirm by remember { mutableStateOf(false) }
    // Set once achieve/forfeit is confirmed - drives the "set a new goal" sheet below.
    var pendingResolution by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Goal Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (goal == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Goal not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (!goal.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = goal.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = { showAchieveConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = details.endColor),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Complete Goal", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { showForfeitConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    enabled = !isLoading
                ) {
                    Text("Abandon Goal", fontWeight = FontWeight.Bold)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Habits Achieved Under This Goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (achievedHabits.isEmpty()) {
                            Text(
                                text = "None yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            achievedHabits.forEach { habit ->
                                Text(
                                    text = "• ${habit.title}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showAchieveConfirm) {
                AlertDialog(
                    onDismissRequest = { showAchieveConfirm = false },
                    title = { Text("Complete this goal?") },
                    text = { Text("You'll be asked to set a new goal next.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showAchieveConfirm = false
                            pendingResolution = "achieve"
                        }) {
                            Text("Complete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAchieveConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showForfeitConfirm) {
                AlertDialog(
                    onDismissRequest = { showForfeitConfirm = false },
                    title = { Text("Abandon this goal?") },
                    text = { Text("This can't be undone. You'll be asked to set a new goal next.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showForfeitConfirm = false
                            pendingResolution = "forfeit"
                        }) {
                            Text("Abandon", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showForfeitConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            pendingResolution?.let { resolution ->
                NewGoalBottomSheet(
                    personaColor = details.endColor,
                    isLoading = isLoading,
                    onDismiss = { pendingResolution = null },
                    onSubmit = { newTitle ->
                        val targetDate = LocalDate.now().plusMonths(3).toString()
                        onTransition(resolution, newTitle, targetDate)
                    }
                )
            }

            pendingHabitDecision?.let { decision ->
                HabitDecisionDialog(
                    decision = decision,
                    isLoading = isLoading,
                    onKeepAndLink = { onDecideHabits("link") },
                    onArchive = { onDecideHabits("archive") },
                    onRetry = onRetryResolveHabits
                )
            }
        }
    }
}

@Composable
private fun HabitDecisionDialog(
    decision: com.habitflowai.presentation.viewmodel.HabitsUiState.PendingHabitDecision,
    isLoading: Boolean,
    onKeepAndLink: () -> Unit,
    onArchive: () -> Unit,
    onRetry: () -> Unit
) {
    var remainingCooldownMs by remember(decision.cooldownUntil) {
        mutableStateOf((decision.cooldownUntil ?: 0L) - System.currentTimeMillis())
    }

    LaunchedEffect(decision.cooldownUntil) {
        while (decision.cooldownUntil != null) {
            remainingCooldownMs = decision.cooldownUntil - System.currentTimeMillis()
            if (remainingCooldownMs <= 0) break
            kotlinx.coroutines.delay(1000)
        }
    }

    val inCooldown = decision.cooldownUntil != null && remainingCooldownMs > 0

    AlertDialog(
        onDismissRequest = { /* Non-dismissable — the user must make an explicit choice. */ },
        title = { Text("We couldn't check your habits") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Our AI coach couldn't decide whether your ${decision.pendingHabitCount} " +
                        "current habit(s) fit your new goal. What would you like to do?"
                )
                if (inCooldown) {
                    val minutes = (remainingCooldownMs / 60000) + 1
                    Text(
                        text = "Too many attempts. Try again in $minutes minute(s).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onKeepAndLink,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Keep & Link to New Goal")
                }
                OutlinedButton(
                    onClick = onArchive,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Archive Habits")
                }
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && !inCooldown
                ) {
                    Text(if (inCooldown) "Try Again (${(remainingCooldownMs / 60000) + 1}m)" else "Try Again")
                }
            }
        },
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGoalBottomSheet(
    personaColor: androidx.compose.ui.graphics.Color,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = { it != SheetValue.Hidden },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Set Your Next Goal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Goal") },
                placeholder = { Text("e.g. Run 20km") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = { onSubmit(title.trim()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                enabled = title.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Start New Goal", fontWeight = FontWeight.Bold)
                }
            }
            if (isLoading) {
                LoadingStatusText()
            }
        }
    }
}

private val TRANSITION_LOADING_MESSAGES = listOf(
    "Your new goal is loading…",
    "Coach is assessing your habits…",
    "Deciding which habits carry over…",
    "Almost there…"
)

@Composable
private fun LoadingStatusText() {
    var messageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            messageIndex = (messageIndex + 1) % TRANSITION_LOADING_MESSAGES.size
        }
    }

    androidx.compose.animation.Crossfade(targetState = messageIndex, label = "loadingMessage") { index ->
        Text(
            text = TRANSITION_LOADING_MESSAGES[index],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
