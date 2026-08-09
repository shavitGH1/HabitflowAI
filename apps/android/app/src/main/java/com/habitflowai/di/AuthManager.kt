package com.habitflowai.di

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth_prefs_v1", Context.MODE_PRIVATE)

    private val _accessToken = MutableStateFlow<String?>(prefs.getString("access_token", null))
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _refreshToken = MutableStateFlow<String?>(prefs.getString("refresh_token", null))
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(
        prefs.getString("access_token", null)?.let { decodeUserIdFromJwt(it) }
    )
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    fun updateTokens(access: String?, refresh: String?) {
        _accessToken.value = access
        _refreshToken.value = refresh
        _currentUserId.value = access?.let { decodeUserIdFromJwt(it) }
        prefs.edit()
            .putString("access_token", access)
            .putString("refresh_token", refresh)
            .apply()
    }

    fun clearTokens() {
        _accessToken.value = null
        _refreshToken.value = null
        _currentUserId.value = null
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .apply()
    }

    /** Decodes the JWT payload (no verification) to extract sub/_id/userId. */
    private fun decodeUserIdFromJwt(token: String): String? = try {
        val payload = token.split(".").getOrNull(1) ?: return null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val json = JSONObject(String(decoded))
        json.optString("sub").takeIf { it.isNotBlank() }
            ?: json.optString("_id").takeIf { it.isNotBlank() }
            ?: json.optString("userId").takeIf { it.isNotBlank() }
    } catch (_: Exception) { null }
}
