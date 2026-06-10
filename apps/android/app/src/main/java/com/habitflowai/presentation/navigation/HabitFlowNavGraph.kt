package com.habitflowai.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import com.habitflowai.data.network.NetworkModule
import com.habitflowai.data.repository.PersonaRepositoryImpl
import com.habitflowai.presentation.ui.home.HomeRoute
import com.habitflowai.presentation.ui.habits.HabitsRoute
import com.habitflowai.presentation.ui.social.SocialRoute
import com.habitflowai.presentation.ui.onboarding.OnboardingRoute
import com.habitflowai.presentation.ui.auth.LoginRoute
import com.habitflowai.presentation.ui.auth.RegisterCredentialsRoute
import com.habitflowai.presentation.ui.profile.ProfileRoute
import com.habitflowai.presentation.ui.map.MapRoute
import com.habitflowai.presentation.ui.drift.DriftCheckRoute
import com.habitflowai.presentation.ui.persona.ProfileRevealRoute
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.habitflowai.presentation.viewmodel.OnboardingViewModelFactory
import com.habitflowai.presentation.viewmodel.LoginViewModel
import com.habitflowai.presentation.viewmodel.LoginViewModelFactory

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
                    currentRoute != NavRoute.RegisterCredentials.route &&
                    currentRoute != NavRoute.ProfileReveal.route

    Scaffold(
        topBar = {
            if (isFullApp) {
                CenterAlignedTopAppBar(
                    title = { Text("HabitFlow AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    actions = {
                        IconButton(onClick = { /* Could navigate to notification or search */ }) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary)
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
                        selected = currentRoute == NavRoute.Map.route,
                        onClick = { navController.navigate(NavRoute.Map.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Rounded.Place, contentDescription = "Map") },
                        label = { Text("Map") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Social.route,
                        onClick = { navController.navigate(NavRoute.Social.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Rounded.Face, contentDescription = "Social") },
                        label = { Text("Social") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Profile.route,
                        onClick = { navController.navigate(NavRoute.Profile.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Rounded.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
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
                        onboardingViewModel.fetchProfile()
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
                    onSubmit = onboardingViewModel::registerUser,
                    onPersonaClassified = {
                        navController.navigate(NavRoute.ProfileReveal.route) {
                            popUpTo(NavRoute.Login.route) { inclusive = true }
                        }
                    },
                    onNavigationHandled = onboardingViewModel::onHomeNavigated
                )
            }
            composable(NavRoute.ProfileReveal.route) {
                uiState.personaResult?.let { result ->
                    ProfileRevealRoute(
                        personaResponse = result,
                        onStartJourney = {
                            navController.navigate(NavRoute.Home.route) {
                                popUpTo(NavRoute.ProfileReveal.route) { inclusive = true }
                            }
                        }
                    )
                }
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
            composable(NavRoute.Profile.route) {
                ProfileRoute(
                    viewModel = onboardingViewModel,
                    onRetakeAssessment = {
                        navController.navigate(NavRoute.Onboarding.route) {
                            popUpTo(NavRoute.Home.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        onboardingViewModel.logout()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoute.Map.route) {
                MapRoute()
            }
            composable(NavRoute.DriftCheck.route) {
                DriftCheckRoute()
            }
        }
    }
}
