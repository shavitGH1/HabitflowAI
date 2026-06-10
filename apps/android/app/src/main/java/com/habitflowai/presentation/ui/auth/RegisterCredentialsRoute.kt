package com.habitflowai.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.habitflowai.presentation.viewmodel.OnboardingUiState

@Composable
fun RegisterCredentialsRoute(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    RegisterCredentialsContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onNext = onNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun RegisterCredentialsContent(
    uiState: OnboardingUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RegisterLogo()

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val isEnabled = uiState.email.contains("@") && uiState.password.length >= 6
        val gradient = if (isEnabled) {
            Brush.linearGradient(colors = listOf(Color(0xFF64B5F6), Color(0xFF1E88E5)))
        } else {
            Brush.linearGradient(colors = listOf(Color.LightGray, Color.Gray))
        }

        if (!isEnabled && uiState.password.isNotEmpty() && uiState.password.length < 6) {
            Text(
                text = "Password must be at least 6 characters",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Next",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text("Already have an account? Login", color = Color(0xFF1E88E5))
        }
    }
}

@Composable
fun RegisterLogo() {
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
            text = "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E88E5)
        )
        Text(
            text = "Join HabitFlow AI and start your journey",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterCredentialsPreview() {
    RegisterCredentialsContent(
        uiState = OnboardingUiState(),
        onEmailChange = {},
        onPasswordChange = {},
        onNext = {},
        onNavigateBack = {}
    )
}

@Preview(showBackground = true)
@Composable
fun RegisterCredentialsErrorPreview() {
    RegisterCredentialsContent(
        uiState = OnboardingUiState(
            email = "invalid-email",
            password = "123",
            errorMessage = "Invalid email format or password too short"
        ),
        onEmailChange = {},
        onPasswordChange = {},
        onNext = {},
        onNavigateBack = {}
    )
}
