package com.habitflowai.util

import com.google.gson.JsonParser
import retrofit2.HttpException

/** Extracts the backend's NestJS-style `{ message }` error body text, or a generic fallback. */
fun extractErrorMessage(httpException: HttpException): String {
    val fallback = "Server error: ${httpException.code()}"
    val errorBody = httpException.response()?.errorBody()?.string() ?: return fallback
    return try {
        val messageElement = JsonParser.parseString(errorBody).asJsonObject.get("message")
        when {
            messageElement == null || messageElement.isJsonNull -> fallback
            messageElement.isJsonArray -> messageElement.asJsonArray.joinToString("\n") { it.asString }
            else -> messageElement.asString
        }
    } catch (_: Exception) {
        fallback
    }
}
