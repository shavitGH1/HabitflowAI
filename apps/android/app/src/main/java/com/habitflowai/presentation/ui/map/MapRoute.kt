package com.habitflowai.presentation.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitflowai.data.model.ChatUiState
import com.habitflowai.presentation.ui.chat.ChatOverlay
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import com.habitflowai.data.model.HabitMarker
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.MapUiState
import com.habitflowai.presentation.viewmodel.MapViewModel
import com.habitflowai.presentation.viewmodel.ChatViewModel

@Composable
fun MapRoute(
    personaType: String,
    viewModel: MapViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatUiState by chatViewModel.uiState.collectAsState()

    LaunchedEffect(personaType) {
        chatViewModel.setPersonaType(personaType)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapContent(
            uiState = uiState,
            personaType = personaType,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSearch = viewModel::onSearch,
            onCameraMoved = viewModel::onCameraMoved,
            onToggleChat = { chatViewModel.toggleChat() }
        )

        ChatOverlay(
            uiState = chatUiState,
            onToggleChat = chatViewModel::toggleChat,
            onInputChanged = chatViewModel::onInputChanged,
            onSendMessage = chatViewModel::sendMessage
        )
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapContent(
    uiState: MapUiState,
    personaType: String = "Regulator",
    onSearchQueryChange: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onCameraMoved: () -> Unit = {},
    onToggleChat: () -> Unit = {}
) {
    val telAviv = LatLng(32.0853, 34.7818)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(telAviv, 13f)
    }

    val personaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }

    LaunchedEffect(uiState.searchResult) {
        uiState.searchResult?.let { latLng ->
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 13f)
            )
            onCameraMoved()
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Physical", "Mental", "Productivity", "Growth")

    val filteredMarkers = remember(uiState.markers, selectedCategory) {
        if (selectedCategory == "All") uiState.markers
        else uiState.markers.filter { it.habitType == selectedCategory }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapStyleOptions = null
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                tiltGesturesEnabled = false
            )
        ) {
            Clustering(
                items = filteredMarkers,
                onClusterItemClick = {
                    false // Return false to show default info window
                }
            )
        }

        // Top Glassmorphism Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleChat,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = "AI Assistant",
                                tint = personaDetails.endColor
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                text = "Habit Flow Map",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = personaDetails.endColor
                            )
                            Text(
                                text = "Discover collective growth nearby",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        placeholder = { Text("Search location...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = personaDetails.endColor) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = onSearch) {
                                    Text("Go", color = personaDetails.endColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = personaDetails.startColor.copy(alpha = 0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSearch() }
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Icon(
                                Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp).padding(end = 4.dp)
                            )
                        }
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                isSelected = isSelected,
                                text = category,
                                personaColor = personaDetails.endColor,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }
                }
            }
        }
        
        // Stats overlay at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = personaDetails.endColor,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ ${filteredMarkers.size} Habits in this area",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    isSelected: Boolean,
    text: String,
    personaColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) personaColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, name = "Map - Achiever")
@Composable
fun MapAchieverPreview() {
    HabitFlowTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MapContent(
                uiState = MapUiState(
                    markers = listOf(
                        HabitMarker("1", "Morning Run", "🏃", "Physical", LatLng(32.0853, 34.7818))
                    )
                ),
                personaType = "Achiever"
            )
            ChatOverlay(
                uiState = ChatUiState(personaType = "Achiever"),
                onToggleChat = {},
                onInputChanged = {},
                onSendMessage = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Map - Grower")
@Composable
fun MapGrowerPreview() {
    HabitFlowTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MapContent(
                uiState = MapUiState(
                    markers = listOf(
                        HabitMarker("1", "Meditation", "🧘", "Mental", LatLng(32.0853, 34.7818))
                    )
                ),
                personaType = "Grower"
            )
            ChatOverlay(
                uiState = ChatUiState(personaType = "Grower"),
                onToggleChat = {},
                onInputChanged = {},
                onSendMessage = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Map - Regulator")
@Composable
fun MapRegulatorPreview() {
    HabitFlowTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MapContent(
                uiState = MapUiState(
                    markers = listOf(
                        HabitMarker("1", "Daily Review", "📅", "Productivity", LatLng(32.0853, 34.7818))
                    )
                ),
                personaType = "Regulator"
            )
            ChatOverlay(
                uiState = ChatUiState(personaType = "Regulator"),
                onToggleChat = {},
                onInputChanged = {},
                onSendMessage = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Map - Socializer")
@Composable
fun MapSocializerPreview() {
    HabitFlowTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MapContent(
                uiState = MapUiState(
                    markers = listOf(
                        HabitMarker("1", "Community Walk", "🚶‍♂️", "Social", LatLng(32.0853, 34.7818))
                    )
                ),
                personaType = "Socializer"
            )
            ChatOverlay(
                uiState = ChatUiState(personaType = "Socializer"),
                onToggleChat = {},
                onInputChanged = {},
                onSendMessage = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Map - Explorer")
@Composable
fun MapExplorerPreview() {
    HabitFlowTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MapContent(
                uiState = MapUiState(
                    markers = listOf(
                        HabitMarker("1", "New Trail", "🗺️", "Growth", LatLng(32.0853, 34.7818))
                    )
                ),
                personaType = "Explorer"
            )
            ChatOverlay(
                uiState = ChatUiState(personaType = "Explorer"),
                onToggleChat = {},
                onInputChanged = {},
                onSendMessage = {}
            )
        }
    }
}
