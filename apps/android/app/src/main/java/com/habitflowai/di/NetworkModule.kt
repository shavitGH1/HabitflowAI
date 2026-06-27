package com.habitflowai.di

import com.google.gson.Gson
import com.habitflowai.BuildConfig
import com.habitflowai.data.model.TokenRefreshRequest
import com.habitflowai.data.network.HabitFlowApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(authManager: AuthManager): Interceptor {
        return Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            authManager.accessToken.value?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    @Singleton
    fun provideAuthenticator(
        authManager: AuthManager,
        apiProvider: Provider<HabitFlowApi> // Use Provider to avoid circular dependency
    ): Authenticator {
        return object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                if (response.request.url.encodedPath.contains("/api/v1/auth/refresh")) {
                    return null
                }

                val currentRefreshToken = authManager.refreshToken.value ?: return null

                return try {
                    val refreshCall = apiProvider.get().refresh(TokenRefreshRequest(currentRefreshToken))
                    val refreshResponse = refreshCall.execute()

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newAccessToken = refreshResponse.body()!!.accessToken
                        // Note: If the backend returns a new refresh token, we should update both.
                        // Assuming it just returns a new access token here based on original code.
                        authManager.updateTokens(newAccessToken, currentRefreshToken)

                        response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    } else {
                        authManager.clearTokens()
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        authenticator: Authenticator
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }

    @Provides
    @Singleton
    fun provideHabitFlowApi(retrofit: Retrofit): HabitFlowApi {
        return retrofit.create(HabitFlowApi::class.java)
    }
}
