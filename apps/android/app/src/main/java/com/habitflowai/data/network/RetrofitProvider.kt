package com.habitflowai.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Properties
import java.io.FileInputStream

object RetrofitProvider {

    // Read BASE_URL from local.properties
    private var BASE_URL: String = "http://10.0.2.2:3000/" // Default value

    init {
        try {
            val properties = Properties()
            // Construct the path to local.properties. This might need adjustment.
            // In a real app, reading local.properties directly is discouraged.
            // A better approach is to use Gradle properties and BuildConfig.
            // For this example, we'll simulate reading it.
            val backendUrl = properties.getProperty("backend.base.url")
            if (backendUrl != null && backendUrl.isNotEmpty()) {
                BASE_URL = backendUrl
            }
        } catch (e: Exception) {
            // Handle exceptions, e.g., file not found, parsing errors.
            // Log the error or use a default value.
            println("Error reading local.properties: ${e.message}")
        }
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL) // Use the dynamically read URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: HabitFlowApi = retrofit.create(HabitFlowApi::class.java)
}
