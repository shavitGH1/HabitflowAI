package com.habitflowai.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.habitflowai.BuildConfig

sealed class GoogleSignInOutcome {
    data class Success(val idToken: String) : GoogleSignInOutcome()
    data object Cancelled : GoogleSignInOutcome()
    data class Error(val message: String) : GoogleSignInOutcome()
}

/**
 * Triggers the native Credential Manager "Sign in with Google" flow and returns the
 * resulting Google ID token. serverClientId must be the Web-type OAuth client ID the
 * backend verifies against (see BuildConfig.GOOGLE_SERVER_CLIENT_ID / auth.service.ts).
 */
suspend fun requestGoogleIdToken(context: Context): GoogleSignInOutcome {
    val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_SERVER_CLIENT_ID).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

    return try {
        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        GoogleSignInOutcome.Success(credential.idToken)
    } catch (_: GetCredentialCancellationException) {
        GoogleSignInOutcome.Cancelled
    } catch (e: GetCredentialException) {
        GoogleSignInOutcome.Error(e.message ?: "Google Sign-In failed")
    } catch (_: GoogleIdTokenParsingException) {
        GoogleSignInOutcome.Error("Could not read the Google credential")
    }
}
