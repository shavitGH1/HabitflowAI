package com.habitflowai.presentation.navigation

sealed class NavRoute(val route: String) {
    data object Onboarding : NavRoute("onboarding")
    data object ProfileReveal : NavRoute("profile_reveal")
    data object Home : NavRoute("home")
    data object Habits : NavRoute("habits")
    data object Social : NavRoute("social")
    data object Profile : NavRoute("profile")
    data object Map : NavRoute("map")
    data object DriftCheck : NavRoute("drift_check")
    data object Login : NavRoute("login")
    data object RegisterCredentials : NavRoute("register_credentials")
}
