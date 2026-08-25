package com.habitflowai.data.repository

import android.content.Context
import android.net.Uri
import com.habitflowai.data.local.dao.UserDao
import com.habitflowai.data.local.entity.UserEntity
import com.habitflowai.data.model.ChangePasswordRequest
import com.habitflowai.data.model.UpdateNameRequest
import com.habitflowai.data.model.UpdateProfilePictureRequest
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: HabitFlowApi,
    private val userDao: UserDao,
    private val authManager: AuthManager,
    @ApplicationContext private val context: Context
) : UserRepository {

    private val myId get() = authManager.currentUserId.value ?: "me"

    override suspend fun updateProfilePicture(profilePicture: String): String {
        val response = api.updateProfilePicture(UpdateProfilePictureRequest(profilePicture))
        cacheProfilePicture(response.profilePicture)
        return response.profilePicture
    }

    override suspend fun uploadProfilePicture(imageUri: Uri): String {
        val profilePicture = withContext(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw java.io.IOException("Cannot open input stream for URI: $imageUri")
            val bytes = inputStream.readBytes()
            inputStream.close()

            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(
                "image",
                "profile_${System.currentTimeMillis()}.jpg",
                requestBody
            )
            api.uploadAvatar(part).profilePicture
        }
        cacheProfilePicture(profilePicture)
        return profilePicture
    }

    override suspend fun updateName(firstName: String, lastName: String): String? {
        val response = api.updateName(UpdateNameRequest(firstName, lastName))
        return response.nameChangedAt
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }

    override suspend fun cacheProfile(
        userId: String,
        email: String?,
        goal: String?,
        personaType: String?,
        profilePicture: String?
    ) {
        if (email.isNullOrBlank()) return
        val existing = userDao.getUserById(userId)
        userDao.insert(
            UserEntity(
                id = userId,
                email = email,
                goal = goal ?: existing?.goal,
                personaType = personaType ?: existing?.personaType,
                portfolioSummary = existing?.portfolioSummary,
                tips = existing?.tips,
                failurePatterns = existing?.failurePatterns,
                confidenceScore = existing?.confidenceScore,
                profilePicture = profilePicture ?: existing?.profilePicture
            )
        )
    }

    private suspend fun cacheProfilePicture(profilePicture: String) {
        val existing = userDao.getUserById(myId) ?: return
        userDao.insert(existing.copy(profilePicture = profilePicture))
    }
}
