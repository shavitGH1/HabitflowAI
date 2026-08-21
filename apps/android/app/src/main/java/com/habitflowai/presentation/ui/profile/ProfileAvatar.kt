package com.habitflowai.presentation.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.habitflowai.R
import com.habitflowai.data.model.resolveProfilePicture
import com.habitflowai.presentation.ui.persona.PersonaBadge
import com.habitflowai.presentation.ui.persona.PersonaDetails
import com.habitflowai.presentation.ui.social.createImageFileUri

/** Bundled preset avatars referenced by their "preset:N" key, so the stored value is portable. */
object PresetAvatars {
    val presets: List<Int> = listOf(
        R.drawable.ic_avatar_preset_1,
        R.drawable.ic_avatar_preset_2,
        R.drawable.ic_avatar_preset_3,
        R.drawable.ic_avatar_preset_4,
        R.drawable.ic_avatar_preset_5,
        R.drawable.ic_avatar_preset_6,
        R.drawable.ic_avatar_preset_7,
        R.drawable.ic_avatar_preset_8,
    )

    fun keyForIndex(index: Int): String = (index + 1).toString()

    fun valueForIndex(index: Int): String = "preset:${keyForIndex(index)}"

    /** Returns the drawable for a stored value like "preset:3", or null when it isn't a preset. */
    fun drawableFor(profilePicture: String?): Int? {
        val index = profilePicture?.removePrefix("preset:")?.toIntOrNull()?.minus(1) ?: return null
        return presets.getOrNull(index)
    }
}

/**
 * Circular profile avatar. Renders a bundled preset drawable, an uploaded photo via Coil,
 * or the persona badge as a fallback. When [onClick] is provided an edit overlay is shown.
 */
@Composable
fun ProfileAvatar(
    profilePicture: String?,
    details: PersonaDetails,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showEditOverlay: Boolean = true
) {
    val presetDrawable = PresetAvatars.drawableFor(profilePicture)
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            presetDrawable != null -> Image(
                painter = painterResource(presetDrawable),
                contentDescription = "Profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            !profilePicture.isNullOrBlank() -> AsyncImage(
                model = resolveProfilePicture(profilePicture),
                contentDescription = "Profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> PersonaBadge(details = details, modifier = Modifier.size(120.dp))
        }

        if (onClick != null && showEditOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PhotoCamera,
                    contentDescription = "Change profile picture",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Full-screen viewer for the current profile picture ("a window in a window") — opened by
 * tapping the avatar on the main Profile screen, where tapping should preview the picture,
 * not jump straight into editing (that lives in Edit Profile instead).
 */
@Composable
fun FullScreenAvatarViewer(
    profilePicture: String?,
    details: PersonaDetails,
    onDismiss: () -> Unit
) {
    val presetDrawable = PresetAvatars.drawableFor(profilePicture)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            when {
                presetDrawable != null -> Image(
                    painter = painterResource(presetDrawable),
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                !profilePicture.isNullOrBlank() -> AsyncImage(
                    model = resolveProfilePicture(profilePicture),
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                else -> PersonaBadge(details = details, modifier = Modifier.size(200.dp))
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

/**
 * Profile picture editor: a source dialog (Presets / Camera / Gallery) that swaps to a
 * preset grid when "Choose from presets" is tapped. Camera/gallery selection reuses the
 * same FileProvider + permission pattern as the social screens.
 */
@Composable
fun ProfilePictureEditor(
    personaColor: Color,
    currentPicture: String?,
    onDismiss: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    var showPresets by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingCameraUri = createImageFileUri(context)
        } else {
            onDismiss()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let(onImageSelected)
        onDismiss()
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onImageSelected)
        onDismiss()
    }

    LaunchedEffect(pendingCameraUri) {
        pendingCameraUri?.let { cameraLauncher.launch(it) }
    }

    if (showPresets) {
        PresetAvatarPicker(
            currentPicture = currentPicture,
            personaColor = personaColor,
            onDismiss = { showPresets = false },
            onSelect = { key ->
                showPresets = false
                onSelectPreset(key)
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Profile Picture", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvatarSourceOption(
                    icon = Icons.Rounded.Face,
                    label = "Choose from presets",
                    description = "Pick a pre-made avatar icon",
                    tint = personaColor
                ) { showPresets = true }

                AvatarSourceOption(
                    icon = Icons.Rounded.PhotoCamera,
                    label = "Take photo",
                    description = "Open the camera for a new picture",
                    tint = personaColor
                ) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingCameraUri = createImageFileUri(context)
                    } else {
                        cameraPermLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                AvatarSourceOption(
                    icon = Icons.Rounded.PhotoLibrary,
                    label = "Upload from gallery",
                    description = "Choose an image from your device",
                    tint = personaColor
                ) {
                    galleryLauncher.launch("image/*")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AvatarSourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.padding(start = 40.dp)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PresetAvatarPicker(
    currentPicture: String?,
    personaColor: Color,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a preset avatar", fontWeight = FontWeight.Bold) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(PresetAvatars.presets) { index, drawableRes ->
                    val value = PresetAvatars.valueForIndex(index)
                    val selected = currentPicture == value
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable { onSelect(PresetAvatars.keyForIndex(index)) }
                            .then(
                                if (selected) Modifier.border(3.dp, personaColor, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(drawableRes),
                            contentDescription = "Preset avatar ${PresetAvatars.keyForIndex(index)}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
