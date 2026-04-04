package com.habitflowai.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.presentation.viewmodel.OnboardingUiState

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onGoalChange: (String) -> Unit,
    onQuizAnswerChange: (Int, String) -> Unit,
    onSubmit: () -> Unit,
    onSuccessNavigate: (ClassifyPersonaResponse) -> Unit
) {
    LaunchedEffect(uiState.personaResponse) {
        uiState.personaResponse?.let(onSuccessNavigate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "HabitFlow AI Onboarding", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = uiState.goal,
            onValueChange = onGoalChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Habit goal") }
        )

        OutlinedTextField(
            value = uiState.quizAnswers[0],
            onValueChange = { onQuizAnswerChange(0, it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Quiz question 1") }
        )
        OutlinedTextField(
            value = uiState.quizAnswers[1],
            onValueChange = { onQuizAnswerChange(1, it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Quiz question 2") }
        )
        OutlinedTextField(
            value = uiState.quizAnswers[2],
            onValueChange = { onQuizAnswerChange(2, it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Quiz question 3") }
        )

        Button(
            onClick = onSubmit,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Classify Persona")
            }
        }

        uiState.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    }
}
