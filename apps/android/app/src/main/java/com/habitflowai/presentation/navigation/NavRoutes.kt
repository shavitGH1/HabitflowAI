package com.habitflowai.presentation.navigation

sealed class NavRoute(val route: String) {
    data object Onboarding : NavRoute("onboarding")
    data object ProfileReveal : NavRoute("profile_reveal")
    data object Home : NavRoute("home")
    data object Habits : NavRoute("habits")
    data object Social : NavRoute("social?chatId={chatId}") {
        fun createRoute(chatId: String? = null) = if (chatId != null) "social?chatId=$chatId" else "social"
    }
    data object Profile : NavRoute("profile")
    data object PublicProfile : NavRoute("public_profile/{userId}") {
        fun createRoute(userId: String) = "public_profile/$userId"
    }
    data object Map : NavRoute("map")
    data object DriftCheck : NavRoute("drift_check")
    data object DriftReassessment : NavRoute("drift_reassessment")
    data object SuccessJournal : NavRoute("success_journal")
    data object Login : NavRoute("login")
    data object RegisterCredentials : NavRoute("register_credentials")
    data object NameEntry : NavRoute("name_entry")
    data object HabitDetail : NavRoute("habit_detail/{habitId}") {
        fun createRoute(habitId: String) = "habit_detail/$habitId"
    }
    data object GoalDetail : NavRoute("goal_detail/{goalId}") {
        fun createRoute(goalId: String) = "goal_detail/$goalId"
    }
}
