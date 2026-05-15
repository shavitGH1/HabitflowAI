package com.habitflowai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.presentation.ui.HomeScreen
import com.habitflowai.presentation.ui.OnboardingScreen
import com.habitflowai.presentation.viewmodel.OnboardingViewModel

sealed class Route(val route: String) {
    data object OnboardingRoute : Route("onboarding")
    data object HomeRoute : Route("home")
}

@Composable
fun HabitFlowNavGraph(viewModel: OnboardingViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Route.OnboardingRoute.route
    ) {
        composable(Route.OnboardingRoute.route) {
            OnboardingScreen(
                uiState = uiState,
                onGoalChange = viewModel::onGoalChange,
                onQuizAnswerChange = viewModel::onQuizAnswerChange,
                onSubmit = { viewModel.submitPersonaClassification() },
                onSuccessNavigate = { response ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("personaResponse", response)
                    navController.navigate(Route.HomeRoute.route) {
                        popUpTo(Route.OnboardingRoute.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.HomeRoute.route) {
            val response = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<ClassifyPersonaResponse>("personaResponse")
            response?.let { HomeScreen(personaResponse = it) }
        }
    }
}
