package com.habitflowai.presentation.ui.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitflowai.presentation.viewmodel.DriftViewModel
import com.habitflowai.presentation.viewmodel.ReassessmentQuestion

@Composable
fun DriftReassessmentRoute(
    onFinished: () -> Unit,
    viewModel: DriftViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isSuccess) {
        SuccessScreen(onFinished)
    } else {
        ReassessmentContent(
            questions = uiState.questions,
            answers = uiState.answers,
            isSubmitting = uiState.isSubmitting,
            onAnswer = viewModel::onAnswerSelected,
            onSubmit = viewModel::submitReassessment
        )
    }
}

@Composable
fun ReassessmentContent(
    questions: List<ReassessmentQuestion>,
    answers: Map<String, String>,
    isSubmitting: Boolean,
    onAnswer: (String, String) -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFEBEE), Color.White)))
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Icon(
            Icons.Rounded.QuestionAnswer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Realign Your Path",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFC62828)
        )

        Text(
            text = "Your patterns are shifting. Let's adjust your plan together.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5D4037)
        )

        Spacer(modifier = Modifier.height(32.dp))

        questions.forEach { question ->
            QuestionCard(
                question = question,
                selectedAnswer = answers[question.id],
                onAnswer = { onAnswer(question.id, it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = answers.size == questions.size && !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Update My Plan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun QuestionCard(
    question: ReassessmentQuestion,
    selectedAnswer: String?,
    onAnswer: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            question.options.forEach { option ->
                val isSelected = selectedAnswer == option
                OutlinedButton(
                    onClick = { onAnswer(option) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Color(0xFFFFEBEE) else Color.Transparent,
                        contentColor = if (isSelected) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(if (isSelected) Color(0xFFC62828) else Color.LightGray)
                    )
                ) {
                    Text(text = option)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SuccessScreen(onFinished: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Plan Updated!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "AI has adjusted your core habits based on your feedback. Stay consistent!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onFinished,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Back to Dashboard", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReassessmentPreview() {
    MaterialTheme {
        ReassessmentContent(
            questions = listOf(
                ReassessmentQuestion("1", "Is the morning routine working?", listOf("Yes", "No")),
                ReassessmentQuestion("2", "Want more social habits?", listOf("Yes", "Maybe", "No"))
            ),
            answers = mapOf("1" to "Yes"),
            isSubmitting = false,
            onAnswer = { _, _ -> },
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReassessmentSuccessPreview() {
    MaterialTheme {
        SuccessScreen(onFinished = {})
    }
}
