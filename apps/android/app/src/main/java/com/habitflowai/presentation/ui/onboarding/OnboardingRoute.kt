package com.habitflowai.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.OnboardingUiState

// Onboarding options data class
data class Option(val text: String, val value: String, val color: Color)

enum class QuestionType { OPEN, SINGLE_CHOICE, MULTI_CHOICE }

data class Question(
    val title: String,
    val type: QuestionType,
    val options: List<Option> = emptyList(),
    val placeholder: String = "",
)

val onboardingQuestions = listOf(
    Question(
        title = "What is your primary goal?",
        type = QuestionType.OPEN,
        placeholder = "e.g. Run a marathon, study more...",
    ),
    Question(
        title = "Describe a recent goal you set for yourself. What pushed you to start it, and how did you measure progress?",
        type = QuestionType.OPEN,
        placeholder = "Your answer..."
    ),
    Question(
        title = "Think of a habit or skill you stuck with for more than a month. What kept you coming back to it?",
        type = QuestionType.OPEN,
        placeholder = "Your answer..."
    ),
    Question(
        title = "When you want to make a change in your life, do you tend to involve other people? Describe a specific example.",
        type = QuestionType.OPEN,
        placeholder = "Your answer..."
    ),
    Question(
        title = "How do you react when a routine starts to feel boring or stale? Give an example.",
        type = QuestionType.OPEN,
        placeholder = "Your answer..."
    ),
    Question(
        title = "What is the deeper reason you want to build better habits right now? Who or what is this for?",
        type = QuestionType.OPEN,
        placeholder = "Your answer..."
    ),
    Question(
        title = "Describe your ideal daily routine. How structured or flexible is it, and why does that work for you?",
        type = QuestionType.OPEN,
        placeholder = "Your answer..."
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) {
            keyboardController?.hide()
            onPersonaClassified()
            onNavigationHandled()
        }
    }

    val totalSteps = onboardingQuestions.size
    val scrollState = rememberScrollState()

    val isDark = isSystemInDarkTheme()
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF000000), Color(0xFF1A1A1A))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .imePadding() // Avoid keyboard blocking
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Analyzing your vibe...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            val progress = (currentStep + 1).toFloat() / totalSteps
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isDark) MaterialTheme.colorScheme.primary else Color.White,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
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
                    (fadeIn() + slideInHorizontally { it }) togetherWith (fadeOut() + slideOutHorizontally { -it })
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                val question = onboardingQuestions[step]
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.White
                    ),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        when (question.type) {
                            QuestionType.OPEN -> {
                                val answerIndex = step - 1
                                val value = if (step == 0) uiState.goal else uiState.quizAnswers.getOrElse(answerIndex) { "" }
                                val onValueChange: (String) -> Unit = {
                                    if (step == 0) onGoalChange(it) else onQuizAnswerChange(answerIndex, it)
                                }

                                OutlinedTextField(
                                    value = value,
                                    onValueChange = onValueChange,
                                    placeholder = { Text(question.placeholder) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    minLines = if (step == 0) 1 else 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                        unfocusedContainerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent
                                    )
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        if (currentStep < totalSteps - 1) onStepChange(currentStep + 1) else onSubmit()
                                    },
                                    enabled = value.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (currentStep < totalSteps - 1) "Next" else "Analyze My Persona",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
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
                                                if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                else Modifier
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
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
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
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
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
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
                                                if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                else Modifier
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
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
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
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
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onStepChange(currentStep - 1)
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Back",
                        color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Step 1: Goal - Light", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Step 1: Goal - Dark", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingStep1Preview() {
    HabitFlowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            OnboardingScreen(
                uiState = OnboardingUiState(goal = "I want to Run a marathon and stay fit"),
                currentStep = 0,
                onStepChange = {},
                onGoalChange = {},
                onQuizAnswerChange = { _, _ -> },
                onSubmit = {},
                onPersonaClassified = {},
                onNavigationHandled = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Step 2: Motivation - Light", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Step 2: Motivation - Dark", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingStep2Preview() {
    HabitFlowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            OnboardingScreen(
                uiState = OnboardingUiState(
                    goal = "Run a marathon",
                    quizAnswers = listOf("I want to Proof myself and be healthy") + List(5) { "" }
                ),
                currentStep = 1,
                onStepChange = {},
                onGoalChange = {},
                onQuizAnswerChange = { _, _ -> },
                onSubmit = {},
                onPersonaClassified = {},
                onNavigationHandled = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Step 3: Tracking - Light", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Step 3: Tracking - Dark", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingStep3Preview() {
    HabitFlowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            OnboardingScreen(
                uiState = OnboardingUiState(
                    goal = "Run a marathon",
                    quizAnswers = listOf(
                        "Proof myself", 
                        "I track my steps every single day with my watch",
                        "Consistency",
                        "Friends",
                        "Exciting",
                        "Family",
                        "Structured"
                    ) + List(2) { "" }
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
    }
}

@Preview(showBackground = true, name = "Step 4: Reaction - Light", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Step 4: Reaction - Dark", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingStep4Preview() {
    HabitFlowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            OnboardingScreen(
                uiState = OnboardingUiState(
                    goal = "Run a marathon",
                    quizAnswers = listOf(
                        "Proof myself", 
                        "I track my steps every single day with my watch",
                        "I usually run with a friend",
                        "I try to change my route to keep it fresh"
                    ) + List(2) { "" }
                ),
                currentStep = 3,
                onStepChange = {},
                onGoalChange = {},
                onQuizAnswerChange = { _, _ -> },
                onSubmit = {},
                onPersonaClassified = {},
                onNavigationHandled = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Loading State - Light", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Loading State - Dark", device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingLoadingPreview() {
    HabitFlowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
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
    }
}
