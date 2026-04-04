package com.habitflowai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.habitflowai.data.repository.HabitFlowRepositoryImpl
import com.habitflowai.data.network.RetrofitProvider
import com.habitflowai.presentation.navigation.HabitFlowNavGraph
import com.habitflowai.presentation.viewmodel.OnboardingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.habitflowai.presentation.ui.HabitFlowTheme {
                HabitFlowApp()
            }
        }
    }
}

@Composable
private fun HabitFlowApp() {
    val api = remember { RetrofitProvider.api }
    val repository = remember { HabitFlowRepositoryImpl(api) }
    val viewModel = remember { OnboardingViewModel(repository) }

    HabitFlowNavGraph(viewModel = viewModel)
}
