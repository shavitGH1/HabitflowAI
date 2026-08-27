package com.habitflowai.data.network

import com.habitflowai.data.model.CheckEmailRequest
import com.habitflowai.data.model.CheckEmailResponse
import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.DriftCheckRequest
import com.habitflowai.data.model.DriftCheckResponse
import com.habitflowai.data.model.FcmTokenUpdateRequest
import com.habitflowai.data.model.GenerateGoalsRequest
import com.habitflowai.data.model.GenerateGoalsResponse
import com.habitflowai.data.model.GoogleAuthRequest
import com.habitflowai.data.model.GoogleAuthResponse
import com.habitflowai.data.model.GoogleRegisterRequest
import com.habitflowai.data.model.GoogleRegisterResponse
import com.habitflowai.data.model.HabitRequest
import com.habitflowai.data.model.HabitResponse
import com.habitflowai.data.model.HomeResponse
import com.habitflowai.data.model.LocationResponse
import com.habitflowai.data.model.LocationSyncRequest
import com.habitflowai.data.model.LoginRequest
import com.habitflowai.data.model.LoginResponse
import com.habitflowai.data.model.OnboardingSuggestionsRequest
import com.habitflowai.data.model.OnboardingSuggestionsResponse
import com.habitflowai.data.model.ReclassifyRequest
import com.habitflowai.data.model.ReclassifyResponse
import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.model.RegisterResponse
import com.habitflowai.data.model.TokenRefreshRequest
import com.habitflowai.data.model.TokenRefreshResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface HabitFlowApi {
    @POST("api/v1/auth/check-email")
    suspend fun checkEmail(@Body request: CheckEmailRequest): CheckEmailResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/v1/auth/refresh")
    fun refresh(@Body request: TokenRefreshRequest): Call<TokenRefreshResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("api/v1/auth/google/verify")
    suspend fun verifyGoogleIdToken(@Body request: GoogleAuthRequest): GoogleAuthResponse

    @POST("api/v1/auth/register-google")
    suspend fun registerGoogle(@Body request: GoogleRegisterRequest): GoogleRegisterResponse

    @POST("api/v1/auth/onboarding-suggestions")
    suspend fun getOnboardingSuggestions(@Body request: OnboardingSuggestionsRequest): OnboardingSuggestionsResponse

    @GET("api/v1/users/me/home")
    suspend fun getHome(): HomeResponse

    @PATCH("api/v1/users/me/profile")
    suspend fun updateProfilePicture(@Body request: com.habitflowai.data.model.UpdateProfilePictureRequest): com.habitflowai.data.model.UpdateProfileResponse

    @Multipart
    @POST("api/v1/users/me/avatar")
    suspend fun uploadAvatar(@Part image: MultipartBody.Part): com.habitflowai.data.model.UpdateProfileResponse

    @PATCH("api/v1/users/me/name")
    suspend fun updateName(@Body request: com.habitflowai.data.model.UpdateNameRequest): com.habitflowai.data.model.UpdateNameResponse

    @PATCH("api/v1/users/me/password")
    suspend fun changePassword(@Body request: com.habitflowai.data.model.ChangePasswordRequest): com.habitflowai.data.model.ChangePasswordResponse

    @GET("api/v1/goals/active")
    suspend fun getActiveGoal(): com.habitflowai.data.model.ActiveGoalResponse

    @POST("api/v1/personas/classify")
    suspend fun classifyPersona(@Body request: ClassifyPersonaRequest): ClassifyPersonaResponse

    @POST("api/v1/personas/reclassify")
    suspend fun reclassifyPersona(@Body request: ReclassifyRequest): ReclassifyResponse

    @POST("api/v1/goals/generate")
    suspend fun generateGoals(@Body request: GenerateGoalsRequest): GenerateGoalsResponse

    @PATCH("api/v1/tasks/{taskId}/complete")
    suspend fun completeTask(@Path("taskId") taskId: String, @Body body: Map<String, String?> = emptyMap()): Response<Unit>

    @POST("api/v1/habits")
    suspend fun createHabit(@Body habit: HabitRequest): HabitResponse

    @PUT("api/v1/habits/{id}")
    suspend fun updateHabit(@Path("id") id: String, @Body habit: HabitRequest): HabitResponse

    @PATCH("api/v1/habits/{id}/complete")
    suspend fun completeHabit(@Path("id") id: String, @Body body: Map<String, String?> = emptyMap()): Response<HabitResponse>

    @DELETE("api/v1/habits/{id}")
    suspend fun deleteHabit(@Path("id") id: String): Response<Unit>

    @GET("api/v1/habits")
    suspend fun getHabits(): List<HabitResponse>

    @GET("api/v1/habits/{id}/stats")
    suspend fun getHabitStats(@Path("id") id: String): Map<String, Any>

    @POST("api/v1/personas/drift-check")
    suspend fun driftCheck(@Body request: DriftCheckRequest): DriftCheckResponse

    @POST("api/v1/auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenUpdateRequest): Response<Unit>

    @POST("api/v1/locations")
    suspend fun recordLocation(@Body request: LocationSyncRequest): Response<Unit>

    @GET("api/v1/locations/mine")
    suspend fun getMyLocations(): List<LocationResponse>

    @GET("api/v1/locations")
    suspend fun getPublicLocations(
        @Query("minLat") minLat: Double,
        @Query("maxLat") maxLat: Double,
        @Query("minLng") minLng: Double,
        @Query("maxLng") maxLng: Double,
        @Query("since") since: Long? = null,
        @Query("scope") scope: String? = null
    ): List<LocationResponse>

    // Chat
    @GET("api/v1/coach/chat")
    suspend fun getCoachChat(): com.habitflowai.data.model.CoachChatResponse

    @POST("api/v1/coach/chat")
    suspend fun postCoachChat(@Body request: com.habitflowai.data.model.CoachChatRequest): com.habitflowai.data.model.CoachChatApiResponse

    @GET("api/v1/chats")
    suspend fun getChats(): List<com.habitflowai.data.model.ChatResponse>

    @POST("api/v1/chats")
    suspend fun createChat(@Body request: com.habitflowai.data.model.CreateChatRequest): com.habitflowai.data.model.ChatResponse

    @POST("api/v1/chats/{chatId}/members")
    suspend fun addMembers(
        @Path("chatId") chatId: String,
        @Body request: com.habitflowai.data.model.AddMembersRequest
    ): com.habitflowai.data.model.ChatResponse

    @POST("api/v1/chats/{chatId}/messages/{messageId}/like")
    suspend fun toggleMessageLike(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): com.habitflowai.data.model.MessageResponse

    @GET("api/v1/chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<com.habitflowai.data.model.MessageResponse>

    @POST("api/v1/chats/{chatId}/read")
    suspend fun markAsRead(@Path("chatId") chatId: String): Response<Unit>

    @POST("api/v1/chats/{chatId}/pin")
    suspend fun togglePin(@Path("chatId") chatId: String): com.habitflowai.data.model.ChatResponse

    @POST("api/v1/chats/{chatId}/mute")
    suspend fun toggleMute(@Path("chatId") chatId: String): com.habitflowai.data.model.ChatResponse

    @HTTP(method = "DELETE", path = "api/v1/chats/{chatId}/members", hasBody = true)
    suspend fun removeMembers(
        @Path("chatId") chatId: String,
        @Body request: com.habitflowai.data.model.RemoveMembersRequest
    ): com.habitflowai.data.model.ChatResponse

    @POST("api/v1/chats/{chatId}/leave")
    suspend fun leaveGroup(@Path("chatId") chatId: String): Response<Unit>

    @PATCH("api/v1/chats/{chatId}/name")
    suspend fun renameGroup(
        @Path("chatId") chatId: String,
        @Body request: com.habitflowai.data.model.UpdateGroupNameRequest
    ): com.habitflowai.data.model.ChatResponse

    @PATCH("api/v1/chats/{chatId}/description")
    suspend fun updateGroupDescription(
        @Path("chatId") chatId: String,
        @Body request: com.habitflowai.data.model.UpdateGroupDescriptionRequest
    ): com.habitflowai.data.model.ChatResponse

    @PATCH("api/v1/chats/{chatId}/visibility")
    suspend fun updateGroupVisibility(
        @Path("chatId") chatId: String,
        @Body request: com.habitflowai.data.model.UpdateGroupVisibilityRequest
    ): com.habitflowai.data.model.ChatResponse

    @PATCH("api/v1/chats/{chatId}/admins")
    suspend fun promoteAdmin(
        @Path("chatId") chatId: String,
        @Body request: com.habitflowai.data.model.UpdateAdminsRequest
    ): com.habitflowai.data.model.ChatResponse

    @HTTP(method = "DELETE", path = "api/v1/chats/{chatId}/admins", hasBody = true)
    suspend fun demoteAdmin(
        @Path("chatId") chatId: String,
        @Body request: com.habitflowai.data.model.UpdateAdminsRequest
    ): com.habitflowai.data.model.ChatResponse

    @DELETE("api/v1/chats/{chatId}")
    suspend fun deleteGroup(@Path("chatId") chatId: String): Response<Unit>

    @Multipart
    @POST("api/v1/chats/{chatId}/image")
    suspend fun uploadGroupImage(
        @Path("chatId") chatId: String,
        @Part image: MultipartBody.Part
    ): com.habitflowai.data.model.ChatResponse

    @Multipart
    @POST("api/v1/chats/{chatId}/messages/image")
    suspend fun uploadMessageImage(
        @Path("chatId") chatId: String,
        @Part image: MultipartBody.Part
    ): com.habitflowai.data.model.UploadChatImageResponse

    @POST("api/v1/users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): Response<Unit>

    @DELETE("api/v1/users/{userId}/follow")
    suspend fun unfollowUser(@Path("userId") userId: String): Response<Unit>

    @GET("api/v1/users/{userId}/followers")
    suspend fun getFollowers(@Path("userId") userId: String): List<String>

    @GET("api/v1/users/{userId}/following")
    suspend fun getFollowing(@Path("userId") userId: String): List<String>

    @GET("api/v1/users")
    suspend fun getUsers(): List<com.habitflowai.data.model.AppUser>

    // Social Feed
    @GET("api/v1/posts")
    suspend fun getPosts(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("friendsOnly") friendsOnly: Boolean? = null
    ): List<com.habitflowai.data.model.Post>

    @GET("api/v1/posts/user/{userId}")
    suspend fun getPostsByUserId(
        @Path("userId") userId: String
    ): List<com.habitflowai.data.model.Post>

    @Multipart
    @POST("api/v1/posts")
    suspend fun createPost(
        @Part("habitName") habitName: okhttp3.RequestBody,
        @Part("completionNote") completionNote: okhttp3.RequestBody,
        @Part image: MultipartBody.Part? = null
    ): com.habitflowai.data.model.Post

    @POST("api/v1/posts/{id}/like")
    suspend fun togglePostLike(@Path("id") id: String): com.habitflowai.data.model.Post

    @DELETE("api/v1/posts/{id}/like")
    suspend fun unlikePost(@Path("id") id: String): com.habitflowai.data.model.Post

    @GET("api/v1/posts/{id}/comments")
    suspend fun getComments(@Path("id") id: String): List<com.habitflowai.data.model.Comment>

    @POST("api/v1/posts/{id}/comments")
    suspend fun addComment(
        @Path("id") id: String,
        @Body request: com.habitflowai.data.model.CommentRequest
    ): com.habitflowai.data.model.Comment

    @POST("api/v1/posts/{postId}/comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("postId") postId: String,
        @Path("commentId") commentId: String
    ): com.habitflowai.data.model.Comment

    @DELETE("api/v1/posts/{postId}/comments/{commentId}/like")
    suspend fun unlikeComment(
        @Path("postId") postId: String,
        @Path("commentId") commentId: String
    ): com.habitflowai.data.model.Comment
}
