package com.habitflowai.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.habitflowai.presentation.ui.home.HomeRoute
import com.habitflowai.presentation.ui.habits.HabitDetailRoute
import com.habitflowai.presentation.ui.habits.HabitsRoute
import com.habitflowai.presentation.ui.social.SocialRoute
import com.habitflowai.presentation.ui.social.SuccessJournalRoute
import com.habitflowai.presentation.ui.onboarding.OnboardingRoute
import com.habitflowai.presentation.ui.auth.LoginRoute
import com.habitflowai.presentation.ui.auth.RegisterCredentialsRoute
import com.habitflowai.presentation.ui.profile.ProfileRoute
import com.habitflowai.presentation.ui.map.MapRoute
import com.habitflowai.presentation.ui.drift.DriftCheckRoute
import com.habitflowai.presentation.ui.drift.DriftReassessmentRoute
import com.habitflowai.presentation.ui.persona.ProfileRevealRoute
import com.habitflowai.presentation.ui.chat.ChatOverlay
import com.habitflowai.presentation.viewmodel.OnboardingViewModel
import com.habitflowai.presentation.viewmodel.HabitsViewModel
import com.habitflowai.presentation.viewmodel.LoginViewModel
import com.habitflowai.presentation.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFlowNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val habitsViewModel: HabitsViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val uiState by onboardingViewModel.uiState.collectAsState()
    val chatUiState by chatViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.personaResult) {
        uiState.personaResult?.personaType?.let {
            chatViewModel.setPersonaType(it)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isFullApp = currentRoute != NavRoute.Onboarding.route &&
                    currentRoute != NavRoute.Login.route &&
                    currentRoute != NavRoute.RegisterCredentials.route &&
                    currentRoute != NavRoute.ProfileReveal.route

    Scaffold(
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                        onSubmit = {
                            if (uiState.isRetakeMode) onboardingViewModel.reclassifyPersona() else onboardingViewModel.registerUser()
                        },
                        onPersonaClassified = {
                            if (uiState.isRetakeMode) {
                                navController.navigate(NavRoute.Profile.route) {
                                    popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(NavRoute.ProfileReveal.route) {
                                    popUpTo(NavRoute.Login.route) { inclusive = true }
                                }
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
                        userId = uiState.personaResult?.userId ?: uiState.personaResult?.id ?: "",
                        onNavigateToReassessment = {
                            navController.navigate(NavRoute.DriftReassessment.route)
                        },
                        onToggleChat = { chatViewModel.toggleChat() }
                    )
                }
                composable(NavRoute.Habits.route) {
                    HabitsRoute(
                        viewModel = habitsViewModel,
                        personaType = uiState.personaResult?.personaType ?: "Regulator",
                        onHabitClick = { habitId ->
                            navController.navigate(NavRoute.HabitDetail.createRoute(habitId))
                        },
                        onToggleChat = { chatViewModel.toggleChat() }
                    )
                }
                composable(NavRoute.HabitDetail.route) { backStackEntry ->
                    val habitId = backStackEntry.arguments?.getString("habitId") ?: ""
                    HabitDetailRoute(
                        habitId = habitId,
                        viewModel = habitsViewModel,
                        personaType = uiState.personaResult?.personaType ?: "Regulator",
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoute.Social.route) {
                    SocialRoute(
                        onToggleChat = { chatViewModel.toggleChat() }
                    )
                }
                composable(NavRoute.SuccessJournal.route) {
                    SuccessJournalRoute(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoute.Profile.route) {
                    ProfileRoute(
                        viewModel = onboardingViewModel,
                        onRetakeAssessment = {
                            onboardingViewModel.startRetake()
                            navController.navigate(NavRoute.Onboarding.route)
                        },
                        onNavigateToSuccessJournal = {
                            navController.navigate(NavRoute.SuccessJournal.route)
                        },
                        onLogout = {
                            onboardingViewModel.logout()
                            navController.navigate(NavRoute.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onToggleChat = { chatViewModel.toggleChat() }
                    )
                }
                composable(NavRoute.Map.route) {
                    MapRoute(
                        personaType = uiState.personaResult?.personaType ?: "Regulator"
                    )
                }
                composable(NavRoute.DriftCheck.route) {
                    DriftCheckRoute()
                }
                composable(NavRoute.DriftReassessment.route) {
                    DriftReassessmentRoute(
                        onFinished = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            if (isFullApp) {
                ChatOverlay(
                    uiState = chatUiState,
                    onToggleChat = chatViewModel::toggleChat,
                    onInputChanged = chatViewModel::onInputChanged,
                    onSendMessage = chatViewModel::sendMessage
                )
            }
        }
    }
}
