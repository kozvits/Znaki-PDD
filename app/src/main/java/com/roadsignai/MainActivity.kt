package com.roadsignai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roadsignai.presentation.camera.CameraScreen
import com.roadsignai.presentation.camera.CameraViewModel
import com.roadsignai.presentation.settings.SettingsScreen
import com.roadsignai.presentation.settings.SettingsViewModel
import com.roadsignai.presentation.theme.RoadSignAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadSignAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "camera"
                    ) {
                        composable("camera") {
                            val viewModel: CameraViewModel = hiltViewModel()
                            val uiState by viewModel.uiState.collectAsState()
                            CameraScreen(
                                uiState = uiState,
                                onSettingsClick = { navController.navigate("settings") },
                                onEvent = viewModel::onEvent
                            )
                        }
                        composable("settings") {
                            val viewModel: SettingsViewModel = hiltViewModel()
                            val settingsState by viewModel.settingsState.collectAsState()
                            SettingsScreen(
                                state = settingsState,
                                onEvent = viewModel::onEvent,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
