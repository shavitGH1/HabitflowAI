package com.habitflowai.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.habitflowai.presentation.viewmodel.OnboardingUiState
import com.habitflowai.presentation.ui.social.AuthErrorBanner
import com.habitflowai.util.GoogleSignInOutcome
import com.habitflowai.util.requestGoogleIdToken
import kotlinx.coroutines.launch

@Composable
fun RegisterCredentialsRoute(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.proceedToOnboarding) {
        if (uiState.proceedToOnboarding) {
            viewModel.onOnboardingNavigated()
            onNext()
        }
    }

    RegisterCredentialsContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onNext = viewModel::checkEmail,
        onGoogleClick = {
            scope.launch {
                when (val outcome = requestGoogleIdToken(context)) {
                    is GoogleSignInOutcome.Success -> viewModel.signInWithGoogle(outcome.idToken)
                    is GoogleSignInOutcome.Cancelled -> {}
                    is GoogleSignInOutcome.Error -> viewModel.onGoogleSignInFailed(outcome.message)
                }
            }
        },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun RegisterCredentialsContent(
    uiState: OnboardingUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onGoogleClick: () -> Unit = {},
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
        RegisterLogo()

        Spacer(modifier = Modifier.height(48.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = { Text("First Name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last Name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
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

        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
        ) {
            uiState.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                AuthErrorBanner(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val isEnabled = uiState.email.contains("@") && uiState.password.length >= 6 && !uiState.isLoading
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
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Continue",
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
            onClick = onGoogleClick,
            text = "Register with Google"
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text("Already a member? Log In", color = Color(0xFF1E88E5))
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
        onFirstNameChange = {},
        onLastNameChange = {},
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
        onFirstNameChange = {},
        onLastNameChange = {},
        onNext = {},
        onNavigateBack = {}
    )
}
