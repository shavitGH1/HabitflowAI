package com.example.habitflowai.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.habitflowai.data.network.NetworkModule
import com.example.habitflowai.data.repository.PersonaRepositoryImpl
import com.example.habitflowai.presentation.ui.home.HomeRoute
import com.example.habitflowai.presentation.ui.habits.HabitsRoute
import com.example.habitflowai.presentation.ui.social.SocialRoute
import com.example.habitflowai.presentation.ui.onboarding.OnboardingRoute
import com.example.habitflowai.presentation.ui.auth.LoginRoute
import com.example.habitflowai.presentation.ui.auth.RegisterCredentialsRoute
import com.example.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.example.habitflowai.presentation.viewmodel.OnboardingViewModelFactory
import com.example.habitflowai.presentation.viewmodel.LoginViewModel
import com.example.habitflowai.presentation.viewmodel.LoginViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFlowNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val repository = remember { PersonaRepositoryImpl(NetworkModule.habitFlowApi) }
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(repository)
    )
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(NetworkModule.habitFlowApi)
    )
    val uiState by onboardingViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isFullApp = currentRoute != NavRoute.Onboarding.route &&
                    currentRoute != NavRoute.Login.route &&
                    currentRoute != NavRoute.RegisterCredentials.route

    Scaffold(
        topBar = {
            if (isFullApp) {
                CenterAlignedTopAppBar(
                    title = { Text("HabitFlow AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Rounded.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        bottomBar = {
            if (isFullApp) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Home.route,
                        onClick = { navController.navigate(NavRoute.Home.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Habits.route,
                        onClick = { navController.navigate(NavRoute.Habits.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = "Habits") },
                        label = { Text("Habits") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Social.route,
                        onClick = { navController.navigate(NavRoute.Social.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Rounded.Face, contentDescription = "Social") },
                        label = { Text("Social") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoute.Login.route) {
                LoginRoute(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        navController.navigate(NavRoute.Home.route) {
                            popUpTo(NavRoute.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(NavRoute.RegisterCredentials.route)
                    }
                )
            }
            composable(NavRoute.RegisterCredentials.route) {
                RegisterCredentialsRoute(
                    viewModel = onboardingViewModel,
                    onNext = {
                        navController.navigate(NavRoute.Onboarding.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(NavRoute.Onboarding.route) {
                OnboardingRoute(
                    uiState = uiState,
                    onGoalChange = onboardingViewModel::onGoalChange,
                    onQuizAnswerChange = onboardingViewModel::onQuizAnswerChange,
                    onSubmit = onboardingViewModel::registerUser, // Updated to call register instead of classifyPersona for this flow
                    onPersonaClassified = {
                        navController.navigate(NavRoute.Home.route) {
                            popUpTo(NavRoute.Login.route) { inclusive = true }
                        }
                    },
                    onNavigationHandled = onboardingViewModel::onHomeNavigated
                )
            }
            composable(NavRoute.Home.route) {
                HomeRoute(
                    personaResult = uiState.personaResult,
                    userId = uiState.personaResult?.userId ?: uiState.personaResult?.id ?: ""
                )
            }
            composable(NavRoute.Habits.route) {
                HabitsRoute()
            }
            composable(NavRoute.Social.route) {
                SocialRoute()
            }
        }
    }
}
