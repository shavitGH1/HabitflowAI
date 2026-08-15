package com.habitflowai.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.presentation.viewmodel.LoginViewModel
import com.habitflowai.presentation.viewmodel.LoginUiState

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.navigateToHome) {
        viewModel.onNavigated()
        onLoginSuccess()
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::login,
        onNavigateToRegister = onNavigateToRegister
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit
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
        LoginLogo()
        
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
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            val gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF64B5F6), Color(0xFF1E88E5))
            )
            
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(gradient, shape = RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Login",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        GoogleSignInButton(
            onClick = { /* TODO: Google Sign-In */ },
            text = "Login with Google"
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register", color = Color(0xFF1E88E5))
        }
    }
}

@Composable
fun LoginLogo() {
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
                imageVector = Icons.Rounded.Face,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "HabitFlow AI",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E88E5)
        )
        Text(
            text = "Master your habits with intelligence",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    LoginContent(
        uiState = LoginUiState(email = "test@example.com"),
        onEmailChange = {},
        onPasswordChange = {},
        onLoginClick = {},
        onNavigateToRegister = {}
    )
}

@Preview(showBackground = true)
@Composable
fun LoginErrorPreview() {
    LoginContent(
        uiState = LoginUiState(
            email = "user@example.com",
            errorMessage = "Invalid credentials. Please try again."
        ),
        onEmailChange = {},
        onPasswordChange = {},
        onLoginClick = {},
        onNavigateToRegister = {}
    )
}
