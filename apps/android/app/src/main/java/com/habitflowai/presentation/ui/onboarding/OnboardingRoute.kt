package com.habitflowai.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.presentation.viewmodel.OnboardingUiState

data class Option(val text: String, val value: String, val color: Color)

enum class QuestionType { OPEN, SINGLE_CHOICE, MULTI_CHOICE }

data class Question(
    val title: String,
    val type: QuestionType,
    val options: List<Option> = emptyList(),
    val placeholder: String = ""
)

val onboardingQuestions = listOf(
    Question(
        title = "What is your primary goal?",
        type = QuestionType.OPEN,
        placeholder = "e.g. Run a marathon, study more..."
    ),
    Question(
        title = "What is your primary motivation?",
        type = QuestionType.SINGLE_CHOICE,
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
        title = "How do you prefer to track progress? (Choose one or more)",
        type = QuestionType.MULTI_CHOICE,
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
        type = QuestionType.SINGLE_CHOICE,
        options = listOf(
            Option("Push harder to win it back", "Achiever", Color(0xFFFFE082)),
            Option("Analyze what went wrong to learn", "Grower", Color(0xFFA5D6A7)),
            Option("Adjust my daily schedule", "Regulator", Color(0xFF90CAF9)),
            Option("Focus on supporting others", "Altruist", Color(0xFFF48FB1)),
            Option("Talk with my group", "Socializer", Color(0xFFCE93D8)),
            Option("Change to something exciting", "Explorer", Color(0xFFFFAB91))
        )
    )
)

@Composable
fun OnboardingRoute(
    uiState: OnboardingUiState,
    onGoalChange: (String) -> Unit,
    onQuizAnswerChange: (Int, String) -> Unit,
    onSubmit: () -> Unit,
    onPersonaClassified: () -> Unit,
    onNavigationHandled: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    OnboardingScreen(
        uiState = uiState,
        currentStep = currentStep,
        onStepChange = { currentStep = it },
        onGoalChange = onGoalChange,
        onQuizAnswerChange = onQuizAnswerChange,
        onSubmit = onSubmit,
        onPersonaClassified = onPersonaClassified,
        onNavigationHandled = onNavigationHandled
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    currentStep: Int,
    onStepChange: (Int) -> Unit,
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

    val totalSteps = onboardingQuestions.size
    val scrollState = rememberScrollState()

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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing your vibe...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val progress = (currentStep + 1).toFloat() / totalSteps
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedContent(
                targetState = currentStep,
                label = "QuestionCard",
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it }
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                val question = onboardingQuestions[step]
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = question.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF2C3E50)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        when (question.type) {
                            QuestionType.OPEN -> {
                                OutlinedTextField(
                                    value = uiState.goal,
                                    onValueChange = onGoalChange,
                                    placeholder = { Text(question.placeholder) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = { onStepChange(currentStep + 1) },
                                    enabled = uiState.goal.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            QuestionType.SINGLE_CHOICE -> {
                                question.options.forEach { option ->
                                    val isSelected = uiState.quizAnswers[step] == option.value
                                    val shape = RoundedCornerShape(16.dp)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clip(shape)
                                            .then(
                                                if (isSelected) Modifier.background(Color(0xFF2C3E50).copy(alpha = 0.1f))
                                                else Modifier
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF2C3E50) else Color.LightGray.copy(alpha = 0.5f),
                                                shape = shape
                                            )
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        option.color.copy(alpha = 0.3f),
                                                        option.color.copy(alpha = 0.8f)
                                                    )
                                                )
                                            )
                                            .clickable {
                                                onQuizAnswerChange(step, option.value)
                                                if (currentStep < totalSteps - 1) {
                                                    onStepChange(currentStep + 1)
                                                } else {
                                                    onSubmit()
                                                }
                                            }
                                            .padding(20.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = option.text,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                color = Color(0xFF2C3E50),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2C3E50),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            QuestionType.MULTI_CHOICE -> {
                                val currentSelections = uiState.quizAnswers[step].split(",").filter { it.isNotBlank() }.toSet()
                                question.options.forEach { option ->
                                    val isSelected = option.value in currentSelections
                                    val shape = RoundedCornerShape(16.dp)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clip(shape)
                                            .then(
                                                if (isSelected) Modifier.background(Color(0xFF2C3E50).copy(alpha = 0.1f))
                                                else Modifier
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF2C3E50) else Color.LightGray.copy(alpha = 0.5f),
                                                shape = shape
                                            )
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        option.color.copy(alpha = 0.3f),
                                                        option.color.copy(alpha = 0.8f)
                                                    )
                                                )
                                            )
                                            .clickable {
                                                val newSelections = if (isSelected) {
                                                    currentSelections - option.value
                                                } else {
                                                    currentSelections + option.value
                                                }
                                                onQuizAnswerChange(step, newSelections.joinToString(","))
                                            }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = option.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                color = Color(0xFF2C3E50)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { 
                                        if (currentStep < totalSteps - 1) onStepChange(currentStep + 1) else onSubmit()
                                    },
                                    enabled = uiState.quizAnswers[step].isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (currentStep > 0) {
                TextButton(
                    onClick = { onStepChange(currentStep - 1) },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Back", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Step 1: Goal", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingStep1Preview() {
    OnboardingScreen(
        uiState = OnboardingUiState(goal = ""),
        currentStep = 0,
        onStepChange = {},
        onGoalChange = {},
        onQuizAnswerChange = { _, _ -> },
        onSubmit = {},
        onPersonaClassified = {},
        onNavigationHandled = {}
    )
}

@Preview(showBackground = true, name = "Step 2: Motivation", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingStep2Preview() {
    OnboardingScreen(
        uiState = OnboardingUiState(goal = "Run a marathon"),
        currentStep = 1,
        onStepChange = {},
        onGoalChange = {},
        onQuizAnswerChange = { _, _ -> },
        onSubmit = {},
        onPersonaClassified = {},
        onNavigationHandled = {}
    )
}

@Preview(showBackground = true, name = "Step 3: Tracking", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingStep3Preview() {
    OnboardingScreen(
        uiState = OnboardingUiState(
            goal = "Run a marathon",
            quizAnswers = listOf("", "", "Achiever,Grower", "")
        ),
        currentStep = 2,
        onStepChange = {},
        onGoalChange = {},
        onQuizAnswerChange = { _, _ -> },
        onSubmit = {},
        onPersonaClassified = {},
        onNavigationHandled = {}
    )
}

@Preview(showBackground = true, name = "Step 4: Reaction", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingStep4Preview() {
    OnboardingScreen(
        uiState = OnboardingUiState(goal = "Run a marathon"),
        currentStep = 3,
        onStepChange = {},
        onGoalChange = {},
        onQuizAnswerChange = { _, _ -> },
        onSubmit = {},
        onPersonaClassified = {},
        onNavigationHandled = {}
    )
}

@Preview(showBackground = true, name = "Loading State", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingLoadingPreview() {
    OnboardingScreen(
        uiState = OnboardingUiState(isLoading = true),
        currentStep = 3,
        onStepChange = {},
        onGoalChange = {},
        onQuizAnswerChange = { _, _ -> },
        onSubmit = {},
        onPersonaClassified = {},
        onNavigationHandled = {}
    )
}
