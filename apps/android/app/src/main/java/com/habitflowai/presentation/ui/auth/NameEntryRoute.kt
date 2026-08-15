package com.habitflowai.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.habitflowai.presentation.viewmodel.OnboardingUiState

@Composable
fun NameEntryRoute(
    viewModel: OnboardingViewModel,
    uiState: OnboardingUiState,
    onNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    NameEntryContent(
        uiState = uiState,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onNext = onNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun NameEntryContent(
    uiState: OnboardingUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Arrangement.Center doesn't combine reliably with verticalScroll (the scroll
    // container measures children with unbounded height, so "centering" collapses
    // to top-aligned content plus leftover space stranded below it) — once
    // imePadding() shrinks the available height for the keyboard, that leftover
    // space showed up as a dead gap right above the keyboard. Top-aligned content
    // (the default) avoids the ambiguity entirely.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF64B5F6), Color(0xFF1E88E5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "What's your name?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E88E5)
            )
            Text(
                text = "This is how you'll appear to your friends",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = uiState.firstName,
            onValueChange = onFirstNameChange,
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.lastName,
            onValueChange = onLastNameChange,
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        val isEnabled = uiState.firstName.isNotBlank() && uiState.lastName.isNotBlank()
        val gradient = if (isEnabled) {
            Brush.linearGradient(colors = listOf(Color(0xFF64B5F6), Color(0xFF1E88E5)))
        } else {
            Brush.linearGradient(colors = listOf(Color.LightGray, Color.Gray))
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(gradient, shape = RoundedCornerShape(16.dp)),
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues()
        ) {
            Text(
                "Next",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text("Back", color = Color(0xFF1E88E5))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NameEntryPreview() {
    NameEntryContent(
        uiState = OnboardingUiState(),
        onFirstNameChange = {},
        onLastNameChange = {},
        onNext = {},
        onNavigateBack = {}
    )
}
