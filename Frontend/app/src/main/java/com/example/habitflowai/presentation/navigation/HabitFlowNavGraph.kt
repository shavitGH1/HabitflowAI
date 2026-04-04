package com.example.habitflowai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.habitflowai.data.network.NetworkModule
import com.example.habitflowai.data.repository.PersonaRepositoryImpl
import com.example.habitflowai.presentation.ui.home.HomeRoute
import com.example.habitflowai.presentation.ui.onboarding.OnboardingRoute
import com.example.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.example.habitflowai.presentation.viewmodel.OnboardingViewModelFactory

@Composable
fun HabitFlowNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val repository = remember { PersonaRepositoryImpl(NetworkModule.habitFlowApi) }
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(repository)
    )
    val uiState by onboardingViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Onboarding.route
    ) {
        composable(NavRoute.Onboarding.route) {
            OnboardingRoute(
                uiState = uiState,
                onGoalChange = onboardingViewModel::onGoalChange,
                onQuizAnswerChange = onboardingViewModel::onQuizAnswerChange,
                onSubmit = onboardingViewModel::classifyPersona,
                onPersonaClassified = {
                    navController.navigate(NavRoute.Home.route)
                },
                onNavigationHandled = onboardingViewModel::onHomeNavigated
            )
        }
        composable(NavRoute.Home.route) {
            HomeRoute(personaResult = uiState.personaResult)
        }
    }
}
