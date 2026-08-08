package com.habitflowai.presentation.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.habitflowai.data.model.ChatMessage
import com.habitflowai.data.model.ChatUiState
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import kotlin.math.roundToInt

@Composable
fun ChatOverlay(
    uiState: ChatUiState,
    onToggleChat: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onUpdateFabPosition: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val currentOnUpdateFabPosition by rememberUpdatedState(onUpdateFabPosition)
    val currentOnToggleChat by rememberUpdatedState(onToggleChat)
    val currentFabOffsetX by rememberUpdatedState(uiState.fabOffsetX)
    val currentFabOffsetY by rememberUpdatedState(uiState.fabOffsetY)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Chat Window
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AnimatedVisibility(
                visible = uiState.isChatOpen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                ChatCard(
                    uiState = uiState,
                    onInputChanged = onInputChanged,
                    onSendMessage = onSendMessage,
                    onClose = onToggleChat
                )
            }
        }

        // Floating Action Button - Draggable
        AnimatedVisibility(
            visible = !uiState.isChatOpen,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
                .offset { IntOffset(uiState.fabOffsetX.roundToInt(), uiState.fabOffsetY.roundToInt()) }
        ) {
            val personaDetails = PersonaUiData.getDetails(uiState.personaType)
            val gradient = Brush.linearGradient(
                colors = listOf(personaDetails.startColor, personaDetails.endColor)
            )

            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            currentOnUpdateFabPosition(
                                currentFabOffsetX + dragAmount.x,
                                currentFabOffsetY + dragAmount.y
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            currentOnToggleChat()
                        }
                    },
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = "Chat Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatCard(
    uiState: ChatUiState,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onClose: () -> Unit
) {
    val personaDetails = PersonaUiData.getDetails(uiState.personaType)
    val gradient = Brush.linearGradient(
        colors = listOf(personaDetails.startColor, personaDetails.endColor)
    )

    Card(
        modifier = Modifier
            .width(320.dp)
            .height(450.dp)
            .padding(bottom = 80.dp), // Above the FAB
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "HabitFlow AI Assistant",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Messages
            val listState = rememberLazyListState()
            LaunchedEffect(uiState.messages.size) {
                if (uiState.messages.isNotEmpty()) {
                    listState.animateScrollToItem(uiState.messages.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message, uiState.personaType)
                }
                if (uiState.isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Input
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = uiState.inputText,
                        onValueChange = onInputChanged,
                        placeholder = { Text("Ask me something...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 100.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                        onClick = onSendMessage,
                        modifier = Modifier.size(40.dp),
                        containerColor = personaDetails.startColor,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, personaType: String = "Regulator") {
    val personaDetails = PersonaUiData.getDetails(personaType)
    val alignment = if (message.isFromBot) Alignment.Start else Alignment.End
    val color = if (message.isFromBot) MaterialTheme.colorScheme.surfaceVariant else personaDetails.startColor
    val contentColor = if (message.isFromBot) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
    val shape = if (message.isFromBot) {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (message.isFromBot) {
            Row(verticalAlignment = Alignment.Bottom) {
                Icon(
                    Icons.Rounded.SmartToy,
                    contentDescription = null,
                    tint = personaDetails.startColor,
                    modifier = Modifier.size(24.dp).padding(bottom = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = color,
                    shape = shape,
                    modifier = Modifier.widthIn(max = 240.dp)
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        color = contentColor
                    )
                }
            }
        } else {
            Surface(
                color = color,
                shape = shape,
                modifier = Modifier.widthIn(max = 240.dp)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Thinking...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageBubbleBot() {
    HabitFlowTheme {
        Box(Modifier.padding(16.dp)) {
            MessageBubble(
                message = ChatMessage(
                    text = "Hello! How can I help you build better habits today?",
                    senderId = "bot",
                    isFromBot = true
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageBubbleUser() {
    HabitFlowTheme {
        Box(Modifier.padding(16.dp)) {
            MessageBubble(
                message = ChatMessage(
                    text = "I want to start running every morning.",
                    senderId = "user",
                    isFromBot = false
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatCardAchiever() {
    HabitFlowTheme {
        ChatCard(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(text = "Hi!", senderId = "bot", isFromBot = true),
                    ChatMessage(text = "I'm an Achiever!", senderId = "user", isFromBot = false)
                ),
                isTyping = true,
                personaType = "Achiever"
            ),
            onInputChanged = {},
            onSendMessage = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatCardGrower() {
    HabitFlowTheme {
        ChatCard(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(text = "Hi!", senderId = "bot", isFromBot = true),
                    ChatMessage(text = "I'm a Grower!", senderId = "user", isFromBot = false)
                ),
                isTyping = true,
                personaType = "Grower"
            ),
            onInputChanged = {},
            onSendMessage = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatOverlay() {
    HabitFlowTheme {
        ChatOverlay(
            uiState = ChatUiState(isChatOpen = true, personaType = "Socializer"),
            onToggleChat = {},
            onInputChanged = {},
            onSendMessage = {}
        )
    }
}
