package com.habitflowai.presentation.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.habitflowai.presentation.ui.persona.PersonaUiData
import com.habitflowai.presentation.ui.social.AuthErrorBanner
import com.habitflowai.presentation.viewmodel.OnboardingUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val NEXT_ELIGIBLE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

/** Returns null when the name can be changed now, or a formatted "next eligible" date otherwise. */
private fun nameChangeBlockedUntil(nameChangedAt: String?): String? {
    if (nameChangedAt.isNullOrBlank()) return null
    val changedAt = try { Instant.parse(nameChangedAt) } catch (e: Exception) { return null }
    val nextEligible = changedAt.atZone(ZoneId.systemDefault()).plusMonths(3)
    if (nextEligible.isBefore(java.time.ZonedDateTime.now())) return null
    return NEXT_ELIGIBLE_FORMATTER.format(nextEligible)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileSheet(
    uiState: OnboardingUiState,
    personaColor: Color,
    onDismiss: () -> Unit,
    onEditPictureClick: () -> Unit,
    onUpdateName: (firstName: String, lastName: String) -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onPasswordChangeHandled: () -> Unit
) {
    var firstName by remember { mutableStateOf(uiState.firstName) }
    var lastName by remember { mutableStateOf(uiState.lastName) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordFieldsVisible by remember { mutableStateOf(false) }
    var passwordSectionError by remember { mutableStateOf<String?>(null) }
    var passwordSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showNameChangeConfirm by remember { mutableStateOf(false) }

    val nameBlockedUntil = nameChangeBlockedUntil(uiState.nameChangedAt)

    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) {
            currentPassword = ""
            newPassword = ""
            confirmPassword = ""
            passwordSectionError = null
            passwordSuccessMessage = "Password updated."
            onPasswordChangeHandled()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        properties = androidx.compose.material3.ModalBottomSheetDefaults.properties(shouldDismissOnBackPress = false)
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val imeVisible = WindowInsets.isImeVisible
        BackHandler { if (imeVisible) keyboardController?.hide() else onDismiss() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    profilePicture = uiState.profilePicture,
                    details = PersonaUiData.getDetails(uiState.personaResult?.personaType ?: "Regulator"),
                    modifier = Modifier.size(56.dp),
                    onClick = onEditPictureClick
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Profile picture", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Tap the picture to change it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Name", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            if (nameBlockedUntil != null) {
                Text(
                    "You can change your name again on $nameBlockedUntil",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    enabled = nameBlockedUntil == null,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    enabled = nameBlockedUntil == null,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showNameChangeConfirm = true },
                enabled = nameBlockedUntil == null &&
                    !uiState.isLoading &&
                    firstName.isNotBlank() && lastName.isNotBlank() &&
                    (firstName != uiState.firstName || lastName != uiState.lastName),
                colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Name", fontWeight = FontWeight.Bold)
            }

            if (showNameChangeConfirm) {
                AlertDialog(
                    onDismissRequest = { showNameChangeConfirm = false },
                    title = { Text("Change name?", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "You're changing your name to \"${firstName.trim()} ${lastName.trim()}\". " +
                                "You'll only be able to change it again in 3 months. Do you still want to change it?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showNameChangeConfirm = false
                            onUpdateName(firstName.trim(), lastName.trim())
                        }) {
                            Text("Change Name", color = personaColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNameChangeConfirm = false }) { Text("Cancel") }
                    }
                )
            }

            if (uiState.authProvider != "google") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = personaColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Password", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))

                val visualTransformation = if (passwordFieldsVisible) VisualTransformation.None else PasswordVisualTransformation()
                val visibilityToggle: @Composable () -> Unit = {
                    IconButton(onClick = { passwordFieldsVisible = !passwordFieldsVisible }) {
                        Icon(
                            if (passwordFieldsVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (passwordFieldsVisible) "Hide passwords" else "Show passwords"
                        )
                    }
                }

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; passwordSectionError = null },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    trailingIcon = visibilityToggle,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; passwordSectionError = null },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    trailingIcon = visibilityToggle,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordSectionError = null },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    trailingIcon = visibilityToggle,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                passwordSectionError?.let {
                    AuthErrorBanner(message = it)
                    Spacer(Modifier.height(8.dp))
                }
                passwordSuccessMessage?.let {
                    Text(it, color = personaColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        when {
                            newPassword.length < 6 -> passwordSectionError = "New password must be at least 6 characters."
                            newPassword != confirmPassword -> passwordSectionError = "New passwords don't match."
                            else -> {
                                passwordSuccessMessage = null
                                onChangePassword(currentPassword, newPassword)
                            }
                        }
                    },
                    enabled = !uiState.isLoading &&
                        currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = personaColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Password", fontWeight = FontWeight.Bold)
                }
            }

            uiState.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                AuthErrorBanner(message = it)
            }
        }
    }
}
