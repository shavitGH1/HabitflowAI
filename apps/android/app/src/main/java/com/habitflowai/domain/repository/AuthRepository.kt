package com.habitflowai.domain.repository

import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.model.RegisterResponse

interface AuthRepository {
    suspend fun register(request: RegisterRequest): RegisterResponse
}
