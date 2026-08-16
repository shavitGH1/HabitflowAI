package com.habitflowai.presentation.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import com.habitflowai.data.model.ChatUiState
import com.habitflowai.presentation.ui.chat.ChatOverlay
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import com.habitflowai.data.model.AudienceFilter
import com.habitflowai.data.model.HabitMarker
import com.habitflowai.data.model.MarkerRelationship
import com.habitflowai.data.model.TimeRangeFilter
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.MapUiState
import com.habitflowai.presentation.viewmodel.MapViewModel
import com.habitflowai.presentation.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun MapRoute(
    personaType: String,
    viewModel: MapViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatUiState by chatViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(personaType) {
        chatViewModel.setPersonaType(personaType)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshMarkers()
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        MapContent(
            uiState = uiState,
            personaType = personaType,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSearch = viewModel::onSearch,
            onCameraMoved = viewModel::onCameraMoved,
            onToggleChat = { chatViewModel.toggleChat() },
            onTimeRangeChange = viewModel::onTimeRangeChange,
            onAudienceChange = viewModel::onAudienceChange,
            hasLocationPermission = hasLocationPermission,
            onRequestLocationPermission = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            onLocateMe = { viewModel.getMyLocation() }
        )

        ChatOverlay(
            uiState = chatUiState,
            onToggleChat = chatViewModel::toggleChat,
            onInputChanged = chatViewModel::onInputChanged,
            onSendMessage = chatViewModel::sendMessage
        )
    }
}

@OptIn(MapsComposeExperimentalApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapContent(
    uiState: MapUiState,
    personaType: String = "Regulator",
    onSearchQueryChange: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onCameraMoved: () -> Unit = {},
    onToggleChat: () -> Unit = {},
    onTimeRangeChange: (TimeRangeFilter) -> Unit = {},
    onAudienceChange: (AudienceFilter) -> Unit = {},
    hasLocationPermission: Boolean = false,
    onRequestLocationPermission: () -> Unit = {},
    onLocateMe: suspend () -> LatLng? = { null }
) {
    val isDark = isSystemInDarkTheme()
    val telAviv = LatLng(32.0853, 34.7818)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(telAviv, 13f)
    }
    val scope = rememberCoroutineScope()

    val personaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }

    LaunchedEffect(uiState.searchResult) {
        uiState.searchResult?.let { latLng ->
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 13f)
            )
            onCameraMoved()
        }
    }

    // Default the map to the user's own location once permission is available —
    // if it can't be resolved (denied, GPS off, timeout), silently keep the
    // Tel Aviv fallback already set above instead of showing an error.
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            onLocateMe()?.let { latLng ->
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 14f)
                )
            }
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var searchExpanded by remember { mutableStateOf(true) }
    var selectedMarker by remember { mutableStateOf<HabitMarker?>(null) }
    var selectedCluster by remember { mutableStateOf<List<HabitMarker>?>(null) }
    val categories = listOf("All", "Habits", "Tasks")

    val filteredMarkers = remember(uiState.markers, selectedCategory) {
        val value = when (selectedCategory) {
            "Habits" -> "habit"
            "Tasks" -> "task"
            else -> null
        }
        if (value == null) uiState.markers
        else uiState.markers.filter { it.habitType == value }
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
            ),
            onMapClick = { selectedMarker = null }
        ) {
            Clustering(
                items = filteredMarkers,
                onClusterClick = { cluster ->
                    // Markers recorded at (near-)identical coordinates stay a single
                    // cluster bubble no matter how far you zoom in — the default
                    // "zoom to bounds" behavior can never separate them. Show a list
                    // of everything in the cluster instead.
                    selectedCluster = cluster.items.toList()
                    true
                },
                onClusterItemClick = { marker ->
                    selectedMarker = marker
                    true // Consume the click — show our own detail card instead of the default info window
                },
                clusterItemContent = { marker ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(markerColor(marker.relationship))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = marker.personaEmoji, fontSize = 14.sp)
                    }
                },
                clusterContent = { cluster ->
                    val clusterColor = personaDetails.endColor
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(clusterColor)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${cluster.size}",
                            color = if (clusterColor.luminance() > 0.5f) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
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
                color = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
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
                                Icons.Rounded.SmartToy,
                                contentDescription = "AI Assistant",
                                tint = personaDetails.endColor
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .clickable { searchExpanded = !searchExpanded }
                        ) {
                            Text(
                                text = "Habit Flow Map",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = personaDetails.endColor
                            )
                            Text(
                                text = "Where the community completed their tasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { searchExpanded = !searchExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (searchExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (searchExpanded) "Collapse search" else "Expand search",
                                tint = personaDetails.endColor
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = searchExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
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

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Time range",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(TimeRangeFilter.entries.toList()) { range ->
                                    FilterChip(
                                        isSelected = uiState.timeRangeFilter == range,
                                        text = range.label,
                                        personaColor = personaDetails.endColor,
                                        onClick = { onTimeRangeChange(range) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Show",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(AudienceFilter.entries.toList()) { audience ->
                                    FilterChip(
                                        isSelected = uiState.audienceFilter == audience,
                                        text = audience.label,
                                        personaColor = personaDetails.endColor,
                                        onClick = { onAudienceChange(audience) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MarkerLegendItem(color = markerColor(MarkerRelationship.MINE), label = "Mine")
                                MarkerLegendItem(color = markerColor(MarkerRelationship.FRIEND), label = "Friends")
                                MarkerLegendItem(color = markerColor(MarkerRelationship.STRANGER), label = "Others")
                            }
                        }
                    }
                }
            }
        }

        // Locate-me button — bottom-left, clear of the detail card/stats overlay
        // zone that spans the bottom-center, so it never gets covered.
        FloatingActionButton(
            onClick = {
                if (!hasLocationPermission) {
                    onRequestLocationPermission()
                } else {
                    scope.launch {
                        onLocateMe()?.let { latLng ->
                            cameraPositionState.animate(
                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 72.dp)
                .size(48.dp),
            containerColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface,
            contentColor = personaDetails.endColor,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = "Go to my location")
        }

        // Marker detail card — shown instead of the stock Google Maps info window,
        // which is small and doesn't reliably read as "here's the completed task".
        AnimatedVisibility(
            visible = selectedMarker != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val marker = selectedMarker
            if (marker != null) {
                Surface(
                    modifier = Modifier
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(markerColor(marker.relationship)))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = marker.personaEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = marker.habitName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = marker.username?.takeIf { it.isNotBlank() } ?: marker.habitType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { selectedMarker = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Stats overlay at bottom
        AnimatedVisibility(
            visible = selectedMarker == null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = personaDetails.endColor,
                    shadowElevation = 6.dp
                ) {
                    val statsContentColor = if (personaDetails.endColor.luminance() > 0.5f) Color.Black else Color.White
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ ${filteredMarkers.size} tasks completed here",
                            color = statsContentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    val cluster = selectedCluster
    if (cluster != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCluster = null },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    text = "${cluster.size} tasks here",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = personaDetails.endColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(cluster) { marker ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMarker = marker
                                    selectedCluster = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(markerColor(marker.relationship)))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = marker.personaEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = marker.habitName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = marker.username?.takeIf { it.isNotBlank() } ?: marker.habitType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun markerColor(relationship: MarkerRelationship): Color = when (relationship) {
    MarkerRelationship.MINE -> Color(0xFFE53935)
    MarkerRelationship.FRIEND -> Color(0xFF1E88E5)
    MarkerRelationship.STRANGER -> Color(0xFF43A047)
}

@Composable
fun MarkerLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                if (personaColor.luminance() > 0.5f) Color.Black else Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, name = "Map - Achiever - Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "Map - Achiever - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
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
