package com.habitflowai.presentation.ui.persona

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitflowai.data.model.ClassifyPersonaResponse

@Composable
fun ProfileRevealRoute(
    personaResponse: ClassifyPersonaResponse,
    onStartJourney: () -> Unit
) {
    val details = remember(personaResponse.personaType) {
        PersonaUiData.getDetails(personaResponse.personaType)
    }

    val scrollState = rememberScrollState()

    // Animation States
    var showBadge by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    val badgeScale by animateFloatAsState(
        targetValue = if (showBadge) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "BadgeScale"
    )

    val badgeAlpha by animateFloatAsState(
        targetValue = if (showBadge) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "BadgeAlpha"
    )

    LaunchedEffect(Unit) {
        showBadge = true
        kotlinx.coroutines.delay(800)
        showContent = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(details.startColor.copy(alpha = 0.2f), Color.White)))
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        PersonaBadge(
            details = details,
            modifier = Modifier
                .scale(badgeScale)
                .alpha(badgeAlpha)
        )

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(1000)) + expandVertically(animationSpec = tween(1000))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SummaryCard(
                    type = details.type,
                    message = personaResponse.motivationalMessage,
                    details = details
                )

                Spacer(modifier = Modifier.height(32.dp))

                TipsSection(details)

                Spacer(modifier = Modifier.height(32.dp))

                ChallengesSection(details)

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onStartJourney,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = details.endColor)
                ) {
                    Text(
                        "Start My Journey",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
