package com.habitflowai.data.repository

import com.habitflowai.data.model.FcmTokenUpdateRequest
import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.model.RegisterResponse
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: HabitFlowApi
) : AuthRepository {
    override suspend fun register(request: RegisterRequest): RegisterResponse {
        return api.register(request)
    }

    override suspend fun updateFcmToken(token: String) {
        try {
            api.updateFcmToken(FcmTokenUpdateRequest(token))
        } catch (_: Exception) {
            // Will retry on next app launch
        }
    }
}
