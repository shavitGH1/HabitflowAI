package com.example.habitflowai.presentation.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habitflowai.presentation.viewmodel.OnboardingUiState

@Composable
fun OnboardingRoute(
    uiState: OnboardingUiState,
    onGoalChange: (String) -> Unit,
    onQuizAnswerChange: (Int, String) -> Unit,
    onSubmit: () -> Unit,
    onPersonaClassified: () -> Unit,
    onNavigationHandled: () -> Unit
) {
    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) {
            onPersonaClassified()
            onNavigationHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "HabitFlow AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Create your adaptive experience in under a minute.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your Goal", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = uiState.goal,
                    onValueChange = onGoalChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Habit goal") },
                    singleLine = true
                )
            }
        }

        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quick Quiz", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = uiState.quizAnswers[0],
                    onValueChange = { onQuizAnswerChange(0, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What motivates you most?") }
                )
                OutlinedTextField(
                    value = uiState.quizAnswers[1],
                    onValueChange = { onQuizAnswerChange(1, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("How do you plan your week?") }
                )
                OutlinedTextField(
                    value = uiState.quizAnswers[2],
                    onValueChange = { onQuizAnswerChange(2, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What keeps you consistent?") }
                )
            }
        }

        if (uiState.isLoading) {
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Analyzing your persona...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(strokeWidth = 2.dp)
                Text(text = "  Classifying...")
            } else {
                Text("Classify Persona", style = MaterialTheme.typography.titleMedium)
            }
        }

        uiState.errorMessage?.let {
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
