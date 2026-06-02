package com.habitflowai.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.habitflowai.presentation.ui.persona.*
import com.habitflowai.presentation.viewmodel.OnboardingViewModel

@Composable
fun ProfileRoute(
    viewModel: OnboardingViewModel,
    onRetakeAssessment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PersonaBadge(details = details)

        Spacer(modifier = Modifier.height(24.dp))

        SummaryCard(
            type = details.type,
            message = personaResult.motivationalMessage,
            details = details
        )

        Spacer(modifier = Modifier.height(32.dp))

        TipsSection(details)

        Spacer(modifier = Modifier.height(32.dp))

        ChallengesSection(details)

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedButton(
            onClick = onRetakeAssessment,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
        ) {
            Text("Re-take Persona Assessment", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
