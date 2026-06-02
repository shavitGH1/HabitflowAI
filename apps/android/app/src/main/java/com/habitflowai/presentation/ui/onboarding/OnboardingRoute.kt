package com.habitflowai.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.presentation.viewmodel.OnboardingUiState

val openQuestions = listOf(
    "What is your primary goal for using HabitFlow AI?",
    "Describe your current daily routine and where you feel it's lacking.",
    "What are the biggest obstacles preventing you from reaching your goals?",
    "How do you usually handle setbacks or missed habits?",
    "What kind of rewards or recognition motivate you the most?",
    "Tell us about a habit you've successfully built in the past and how you did it."
)

@Composable
fun OnboardingRoute(
    uiState: OnboardingUiState,
    onGoalChange: (String) -> Unit,
    onQuizAnswerChange: (Int, String) -> Unit, // Renamed in ViewModel but NavGraph still uses this name for now
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

    val scrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Analyzing your persona...", style = MaterialTheme.typography.titleMedium)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color.White)))
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Personalize Your Journey",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1565C0)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Help us understand your habits to build your perfect AI coach.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF546E7A)
            )

            Spacer(modifier = Modifier.height(32.dp))

            openQuestions.forEachIndexed { index, question ->
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF37474F)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.openAnswers[index],
                    onValueChange = {
                        if (it.length <= 500) {
                            onQuizAnswerChange(index, it)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp),
                    placeholder = { Text("Share your thoughts...") },
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text(
                            text = "${uiState.openAnswers[index].length} / 500",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.openAnswers.all { it.isNotBlank() }
            ) {
                Text("Complete Onboarding", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
