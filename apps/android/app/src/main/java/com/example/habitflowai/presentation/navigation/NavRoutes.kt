package com.example.habitflowai.presentation.navigation

sealed class NavRoute(val route: String) {
    data object Onboarding : NavRoute("onboarding")
    data object Home : NavRoute("home")
    data object Habits : NavRoute("habits")
    data object Social : NavRoute("social")
    data object Login : NavRoute("login")
    data object RegisterCredentials : NavRoute("register_credentials")
}
