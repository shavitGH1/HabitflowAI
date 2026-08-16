package com.habitflowai.domain.repository

import android.net.Uri

interface UserRepository {
    /** Persists a preset key or uploaded URL as the profile picture. Returns the persisted value. */
    suspend fun updateProfilePicture(profilePicture: String): String

    /** Uploads a camera/gallery image as the profile picture. Returns the persisted /uploads URL. */
    suspend fun uploadProfilePicture(imageUri: Uri): String

    /** Upserts the current user row (seeds Room with profile data so it survives app restarts). */
    suspend fun cacheProfile(
        userId: String,
        email: String?,
        goal: String?,
        personaType: String?,
        profilePicture: String?
    )
}
