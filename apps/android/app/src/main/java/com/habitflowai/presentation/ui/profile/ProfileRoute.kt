package com.habitflowai.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.ChatUiState
import com.habitflowai.presentation.ui.chat.ChatOverlay
import com.habitflowai.presentation.ui.persona.*
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.habitflowai.presentation.viewmodel.OnboardingUiState
import com.habitflowai.presentation.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SmartToy

@Composable
fun ProfileRoute(
    viewModel: OnboardingViewModel,
    onRetakeAssessment: () -> Unit,
    onNavigateToSuccessJournal: () -> Unit,
    onLogout: () -> Unit,
    onToggleChat: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileContent(
        uiState = uiState,
        onRetakeAssessment = onRetakeAssessment,
        onNavigateToSuccessJournal = onNavigateToSuccessJournal,
        onLogout = onLogout,
        onToggleChat = onToggleChat
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    uiState: OnboardingUiState,
    onRetakeAssessment: () -> Unit,
    onNavigateToSuccessJournal: () -> Unit,
    onLogout: () -> Unit,
    onToggleChat: () -> Unit
) {
    val scrollState = rememberScrollState()
    val personaResult = uiState.personaResult

    if (personaResult == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No profile data found.")
                Button(onClick = onRetakeAssessment) {
                    Text("Start Assessment")
                }
            }
        }
        return
    }

    val details = remember(personaResult.personaType) {
        PersonaUiData.getDetails(personaResult.personaType)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onToggleChat) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = "AI Assistant",
                            tint = details.endColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PersonaBadge(details = details)

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.firstName.isNotEmpty() || uiState.lastName.isNotEmpty()) {
                Text(
                    text = "${uiState.firstName} ${uiState.lastName}".trim(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            SummaryCard(
                type = details.type,
                message = personaResult.motivationalMessage,
                details = details
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = onNavigateToSuccessJournal
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Book,
                        contentDescription = null,
                        tint = details.endColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Success Journal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Review your growth and milestones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TipsSection(details)

            Spacer(modifier = Modifier.height(32.dp))

            ChallengesSection(details)

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedButton(
                onClick = onRetakeAssessment,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
            ) {
                Text("Re-take Persona Assessment", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, name = "Profile with Chat FAB")
@Composable
fun ProfileWithChatPreview() {
    val personaType = "Grower"
    HabitFlowTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ProfileContent(
                uiState = OnboardingUiState(
                    personaResult = ClassifyPersonaResponse(
                        id = "1",
                        personaType = personaType,
                        motivationalMessage = "You are doing great! Keep growing.",
                        success = true
                    )
                ),
                onRetakeAssessment = {},
                onNavigateToSuccessJournal = {},
                onLogout = {},
                onToggleChat = {}
            )
            ChatOverlay(
                uiState = ChatUiState(personaType = personaType),
                onToggleChat = {},
                onInputChanged = {},
                onSendMessage = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Profile Details", device = "spec:width=411dp,height=891dp")
@Composable
fun ProfilePreview() {
    ProfileContent(
        uiState = OnboardingUiState(
            personaResult = ClassifyPersonaResponse(
                id = "1",
                personaType = "Grower",
                motivationalMessage = "You are doing great! Keep growing.",
                success = true
            )
        ),
        onRetakeAssessment = {},
        onNavigateToSuccessJournal = {},
        onLogout = {},
        onToggleChat = {}
    )
}

@Preview(showBackground = true, name = "Logout Section Only")
@Composable
fun ProfileLogoutPreview() {
    Box(modifier = Modifier.padding(24.dp)) {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Logout", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
