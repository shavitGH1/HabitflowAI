package com.example.habitflowai.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.habitflowai.presentation.viewmodel.OnboardingUiState

data class Option(val text: String, val character: String)
data class Question(val title: String, val options: List<Option>)

val quizQuestions = listOf(
    Question(
        title = "What is your primary motivation?",
        options = listOf(
            Option("Competition & Rewards", "Achiever"),
            Option("Self improvement & Growth", "Grower"),
            Option("Structure & Routine", "Regulator"),
            Option("Helping & Sharing", "Altruist"),
            Option("Connecting & Teamwork", "Socializer"),
            Option("Discovery & Novelty", "Explorer")
        )
    ),
    Question(
        title = "How do you prefer to track progress?",
        options = listOf(
            Option("Milestones and achievements", "Achiever"),
            Option("Reflecting on past habits", "Grower"),
            Option("Detailed logs and schedules", "Regulator"),
            Option("Community impact metrics", "Altruist"),
            Option("Leaderboards with friends", "Socializer"),
            Option("Unlocking new habit zones", "Explorer")
        )
    ),
    Question(
        title = "When you fail a habit, how do you react?",
        options = listOf(
            Option("Push harder to win it back", "Achiever"),
            Option("Analyze what went wrong to learn", "Grower"),
            Option("Adjust my strict daily schedule", "Regulator"),
            Option("Focus on how I can support others instead", "Altruist"),
            Option("Talk about it with my accountability group", "Socializer"),
            Option("Change to a different, exciting habit", "Explorer")
        )
    )
)

@OptIn(ExperimentalAnimationApi::class)
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

    var currentStep by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentStep < quizQuestions.size && !uiState.isLoading) {
            val progress = (currentStep + 1).toFloat() / quizQuestions.size
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Question ${currentStep + 1} of ${quizQuestions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentStep,
                label = "QuizQuestion"
            ) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = quizQuestions[step].title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    quizQuestions[step].options.forEach { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    // Submit character type string to our viewmodel
                                    onQuizAnswerChange(step, option.character)
                                    if (currentStep < quizQuestions.size - 1) {
                                        currentStep++
                                    } else {
                                        // Final question answered, trigger backend / local logic calculation
                                        currentStep++
                                        onSubmit()
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Loading State (Calculating result)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Analyzing your personality...",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
