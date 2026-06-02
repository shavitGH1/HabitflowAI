package com.habitflowai.data.network

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import com.habitflowai.BuildConfig
import com.habitflowai.data.model.TokenRefreshRequest

object NetworkModule {
    private val BASE_URL = BuildConfig.BASE_URL

    var accessToken: String? = null
    var refreshToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        accessToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val authenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // Avoid infinite loops if refresh also fails with 401
            if (response.request.url.encodedPath.contains("/api/v1/auth/refresh")) {
                return null
            }

            val currentRefreshToken = refreshToken ?: return null

            // Call the refresh API synchronously
            try {
                val refreshCall = habitFlowApi.refresh(TokenRefreshRequest(currentRefreshToken))
                val refreshResponse = refreshCall.execute()

                return if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val newAccessToken = refreshResponse.body()!!.accessToken
                    accessToken = newAccessToken

                    // Proceed with the updated access token
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else {
                    // Refresh failed, maybe log the user out here
                    accessToken = null
                    refreshToken = null
                    null
                }
            } catch (e: Exception) {
                return null
            }
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .build()

    val habitFlowApi: HabitFlowApi by lazy {
        retrofit.create(HabitFlowApi::class.java)
    }
}
