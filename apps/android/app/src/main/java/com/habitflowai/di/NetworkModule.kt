package com.habitflowai.di

import com.google.gson.Gson
import com.habitflowai.BuildConfig
import com.habitflowai.data.local.HabitFlowDatabase
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
        apiProvider: Provider<HabitFlowApi>,
        database: HabitFlowDatabase
    ): Authenticator {
        return object : Authenticator {
            private var isRefreshing = false

            override fun authenticate(route: Route?, response: Response): Request? {
                if (response.request.url.encodedPath.contains("/api/v1/auth/refresh")) {
                    return null
                }

                // If we've already tried to refresh for this specific request and failed, stop.
                if (response.priorResponse != null && response.priorResponse?.code == 401) {
                    return null
                }

                synchronized(this) {
                    val currentRefreshToken = authManager.refreshToken.value ?: return null
                    
                    // If another thread already updated the token, just retry with the new one
                    val tokenAtRequest = response.request.header("Authorization")
                    val currentAccessToken = authManager.accessToken.value
                    if (currentAccessToken != null && "Bearer $currentAccessToken" != tokenAtRequest) {
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $currentAccessToken")
                            .build()
                    }

                    return try {
                        val refreshCall = apiProvider.get().refresh(TokenRefreshRequest(currentRefreshToken))
                        val refreshResponse = refreshCall.execute()

                        if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                            val newAccessToken = refreshResponse.body()!!.accessToken
                            authManager.updateTokens(newAccessToken, currentRefreshToken)

                            response.request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                        } else {
                            authManager.clearTokens()
                            database.clearAllTables()
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
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
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
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
