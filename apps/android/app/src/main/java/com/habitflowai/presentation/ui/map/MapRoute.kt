package com.habitflowai.presentation.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitflowai.data.model.ChatUiState
import com.habitflowai.presentation.ui.chat.ChatOverlay
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import com.habitflowai.data.model.HabitMarker
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.theme.HabitFlowTheme
import com.habitflowai.presentation.viewmodel.MapUiState
import com.habitflowai.presentation.viewmodel.MapViewModel

@Composable
fun MapRoute(
    personaType: String,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    MapContent(
        uiState = uiState,
        personaType = personaType
    )
}

@Composable
fun MapContent(
    uiState: MapUiState,
    personaType: String = "Regulator",
) {
    val defaultCenter = LatLng(32.0853, 34.7818)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 13f)
    }

    val personaDetails = remember(personaType) { PersonaUiData.getDetails(personaType) }

    val initialFitDone = remember { mutableStateOf(false) }
    LaunchedEffect(uiState.markers) {
        if (!initialFitDone.value && uiState.markers.isNotEmpty()) {
            val center = LatLng(
                (uiState.markers.minOf { it.latLng.latitude } + uiState.markers.maxOf { it.latLng.latitude }) / 2,
                (uiState.markers.minOf { it.latLng.longitude } + uiState.markers.maxOf { it.latLng.longitude }) / 2
            )
            val zoom = if (uiState.markers.size == 1) {
                14f
            } else {
                val latSpan = uiState.markers.maxOf { it.latLng.latitude } - uiState.markers.minOf { it.latLng.latitude }
                val lngSpan = uiState.markers.maxOf { it.latLng.longitude } - uiState.markers.minOf { it.latLng.longitude }
                val span = maxOf(latSpan, lngSpan)
                (14.0 - (span * 8.0)).toFloat()
            }
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(center, zoom.coerceIn(5f, 16f))
            )
            initialFitDone.value = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapStyleOptions = null
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                tiltGesturesEnabled = false,
                myLocationButtonEnabled = true
            )
        ) {
            Clustering(
                items = uiState.markers,
                onClusterItemClick = { marker ->
                    false // Return false to show default info window
                }
            )
        }

        // Top header
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
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Habit Map",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = personaDetails.endColor
                        )
                        Text(
                            text = "Where you completed your tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = personaDetails.endColor
                        )
                    }
                }
            }
        }

        // Empty state
        if (uiState.markers.isEmpty() && !uiState.isLoading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📍", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No completions yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = personaDetails.endColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete a task on the Home screen and it will show up here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Stats overlay at bottom
        if (uiState.markers.isNotEmpty()) {
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
                        Icon(
                            Icons.Rounded.MyLocation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.markers.size} completions mapped",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
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
                        HabitMarker("1", "Community Walk", "🚶", "Social", LatLng(32.0853, 34.7818))
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
