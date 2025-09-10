package com.knowbody.interpret

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.knowbody.interpret.ui.ModeSelectionScreen
import com.knowbody.interpret.ui.InterpreterScreen
import com.knowbody.interpret.viewmodel.InterpreterViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.content.Context
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: InterpreterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Define the revised professional color scheme
            val darkTheme = isSystemInDarkTheme()
            val primaryColor = Color(0xFF1976D2) // Blue
            val surfaceColor = Color(0xFFF5F5F5) // Warm off-white
            val onSurfaceColor = Color(0xFF212121) // Dark slate gray
            val secondaryColor = Color(0xFFBBDEFB) // Soft blue accent

            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme(
                    primary = primaryColor,
                    surface = surfaceColor,
                    onSurface = onSurfaceColor,
                    secondary = secondaryColor
                ) else lightColorScheme(
                    primary = primaryColor,
                    surface = surfaceColor,
                    onSurface = onSurfaceColor,
                    secondary = secondaryColor
                )
            ) {
                val navController = rememberNavController()
                val modeSelectionCompleted by remember {
                    mutableStateOf(
                        getSharedPreferences("InterpreterPrefs", Context.MODE_PRIVATE)
                            .getBoolean("modeSelectionCompleted", false)
                    )
                }
                val startDestination = if (modeSelectionCompleted) "interpreter" else "modeSelection"

                // Observe translation state to control screen-on
                val isTranslating by viewModel.status.collectAsState()
                LaunchedEffect(isTranslating) {
                    keepScreenOn(isTranslating.contains("in progress"))
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(surfaceColor)
                        .padding(16.dp)
                ) {
                    composable("modeSelection") {
                        ModeSelectionScreen(navController)
                    }
                    composable("interpreter") {
                        InterpreterScreen(viewModel)
                    }
                }
            }
        }
    }

    private fun keepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        super.onPause()
        keepScreenOn(false) // Clear screen-on flag when app is paused
    }

    override fun onDestroy() {
        super.onDestroy()
        keepScreenOn(false) // Ensure screen-on flag is cleared
    }
}