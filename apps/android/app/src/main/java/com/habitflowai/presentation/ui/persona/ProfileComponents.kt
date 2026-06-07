package com.habitflowai.presentation.ui.persona

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PersonaBadge(details: PersonaDetails, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(details.startColor, details.endColor))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Face,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(70.dp)
        )
    }
}

@Composable
fun SummaryCard(type: String, message: String, details: PersonaDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val article = if (listOf('A', 'E', 'I', 'O', 'U').contains(type.firstOrNull()?.uppercaseChar())) "an" else "a"
            Text(
                text = "You are $article",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
            Text(
                text = type,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = details.endColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 24.sp,
                color = Color(0xFF37474F)
            )
        }
    }
}

@Composable
fun TipsSection(details: PersonaDetails) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = details.endColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Personalized Tips",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        details.tips.forEach { tip ->
            ExpandableTipCard(tip, details.startColor.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ExpandableTipCard(tip: String, backgroundColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "HabitFlow AI suggests implementing this by setting a daily reminder and tracking your success for 21 days straight.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun ChallengesSection(details: PersonaDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Likely Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            details.challenges.forEach { challenge ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("•", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = challenge, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersonaBadgePreview() {
    val details = PersonaUiData.getDetails("Achiever")
    Box(modifier = Modifier.padding(16.dp)) {
        PersonaBadge(details = details)
    }
}

@Preview(showBackground = true)
@Composable
fun SummaryCardPreview() {
    val details = PersonaUiData.getDetails("Achiever")
    Box(modifier = Modifier.padding(16.dp)) {
        SummaryCard(
            type = details.type,
            message = "You have a natural drive for excellence. Your competitive spirit will help you master new habits faster than most.",
            details = details
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TipsSectionPreview() {
    val details = PersonaUiData.getDetails("Grower")
    Box(modifier = Modifier.padding(16.dp)) {
        TipsSection(details = details)
    }
}

@Preview(showBackground = true)
@Composable
fun ChallengesSectionPreview() {
    val details = PersonaUiData.getDetails("Regulator")
    Box(modifier = Modifier.padding(16.dp)) {
        ChallengesSection(details = details)
    }
}
