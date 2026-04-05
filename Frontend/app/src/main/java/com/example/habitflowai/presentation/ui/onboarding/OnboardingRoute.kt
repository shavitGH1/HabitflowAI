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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habitflowai.presentation.viewmodel.OnboardingUiState

data class Option(val text: String, val character: String, val color: Color)
data class Question(val title: String, val options: List<Option>)

val quizQuestions = listOf(
    Question(
        title = "What is your primary motivation?",
        options = listOf(
            Option("Competition & Rewards", "Achiever", Color(0xFFFFE082)),
            Option("Self improvement & Growth", "Grower", Color(0xFFA5D6A7)),
            Option("Structure & Routine", "Regulator", Color(0xFF90CAF9)),
            Option("Helping & Sharing", "Altruist", Color(0xFFF48FB1)),
            Option("Connecting & Teamwork", "Socializer", Color(0xFFCE93D8)),
            Option("Discovery & Novelty", "Explorer", Color(0xFFFFAB91))
        )
    ),
    Question(
        title = "How do you prefer to track progress?",
        options = listOf(
            Option("Milestones and achievements", "Achiever", Color(0xFFFFE082)),
            Option("Reflecting on past habits", "Grower", Color(0xFFA5D6A7)),
            Option("Detailed logs and schedules", "Regulator", Color(0xFF90CAF9)),
            Option("Community impact metrics", "Altruist", Color(0xFFF48FB1)),
            Option("Leaderboards with friends", "Socializer", Color(0xFFCE93D8)),
            Option("Unlocking new habit zones", "Explorer", Color(0xFFFFAB91))
        )
    ),
    Question(
        title = "When you fail a habit, how do you react?",
        options = listOf(
            Option("Push harder to win it back", "Achiever", Color(0xFFFFE082)),
            Option("Analyze what went wrong to learn", "Grower", Color(0xFFA5D6A7)),
            Option("Adjust my strict daily schedule", "Regulator", Color(0xFF90CAF9)),
            Option("Focus on how I can support others instead", "Altruist", Color(0xFFF48FB1)),
            Option("Talk about it with my accountability group", "Socializer", Color(0xFFCE93D8)),
            Option("Change to a different, exciting habit", "Explorer", Color(0xFFFFAB91))
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
    val totalSteps = quizQuestions.size + 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentStep < totalSteps && !uiState.isLoading) {
            val progress = (currentStep + 1).toFloat() / totalSteps
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
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentStep,
                label = "QuizStep"
            ) { step ->
                if (step == 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "What is your goal?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF2C3E50)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = uiState.goal,
                            onValueChange = onGoalChange,
                            placeholder = { Text("e.g. Run a marathon, study more", color = Color(0xFF37474F).copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.9f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { currentStep++ },
                            enabled = uiState.goal.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val questionIndex = step - 1
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = quizQuestions[questionIndex].title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF2C3E50)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        quizQuestions[questionIndex].options.forEach { option ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        // Submit character type string to our viewmodel
                                        onQuizAnswerChange(questionIndex, option.character)
                                        if (currentStep < totalSteps - 1) {
                                            currentStep++
                                        } else {
                                            // Final question answered, trigger backend / local logic calculation
                                            currentStep++
                                            onSubmit()
                                        }
                                    },
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF).copy(alpha = 0.9f))
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
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF37474F)
                                    )
                                }
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
                        modifier = Modifier.size(80.dp),
                        color = Color(0xFF6A1B9A),
                        strokeWidth = 8.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Analyzing your vibe...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF4A148C),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
