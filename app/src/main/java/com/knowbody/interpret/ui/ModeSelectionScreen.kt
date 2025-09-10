package com.knowbody.interpret.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.knowbody.interpret.viewmodel.InterpreterViewModel
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(navController: NavController) {
    val viewModel: InterpreterViewModel = hiltViewModel<InterpreterViewModel>()
    val context = LocalContext.current

    // State for language selection
    var leftEarbudLanguage by remember { mutableStateOf("fr-FR") }
    var rightEarbudLanguage by remember { mutableStateOf("lt-LT") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    // Language options
    val languageOptions = listOf("fr-FR" to "French", "lt-LT" to "Lithuanian", "en-US" to "English")

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startTranslation(leftEarbudLanguage, rightEarbudLanguage, context)
            context.getSharedPreferences("InterpreterPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("modeSelectionCompleted", true)
                .apply()
            navController.navigate("interpreter") {
                popUpTo("modeSelection") { inclusive = true }
            }
        } else {
            Log.w("ModeSelectionScreen", "Permissions denied")
        }
    }

    // Check and request permissions
    fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allPermissionsGranted) {
            permissionLauncher.launch(permissions)
        } else {
            viewModel.startTranslation(leftEarbudLanguage, rightEarbudLanguage, context)
            context.getSharedPreferences("InterpreterPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("modeSelectionCompleted", true)
                .apply()
            navController.navigate("interpreter") {
                popUpTo("modeSelection") { inclusive = true }
            }
        }
    }

    // Define a revised professional color palette with M3 standards
    val primaryColor = Color(0xFF1976D2) // Blue
    val surfaceColor = Color(0xFFF5F5F5) // Warm off-white
    val onSurfaceColor = Color(0xFF212121) // Dark slate gray
    val secondaryColor = Color(0xFFBBDEFB) // Soft blue accent
    val surfaceVariantColor = Color(0xFFE0E0E0) // Lighter gray for dropdown background
    val onSurfaceVariantColor = Color(0xFF424242) // Medium gray for dropdown text

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Select Translation Mode",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    color = onSurfaceColor
                ),
                textAlign = TextAlign.Center
            )

            // Left Earbud Language Dropdown
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = isMenuExpanded,
                    onExpandedChange = { isMenuExpanded = !isMenuExpanded }
                ) {
                    TextField(
                        value = languageOptions.find { it.first == leftEarbudLanguage }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Left Earbud Language") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = primaryColor,
                            unfocusedIndicatorColor = onSurfaceColor.copy(alpha = 0.5f),
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(surfaceVariantColor)
                    ) {
                        languageOptions.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, color = onSurfaceVariantColor) },
                                onClick = {
                                    leftEarbudLanguage = code
                                    isMenuExpanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = onSurfaceVariantColor,
                                    leadingIconColor = onSurfaceVariantColor
                                )
                            )
                        }
                    }
                }
            }

            // Right Earbud Language Dropdown
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = isMenuExpanded,
                    onExpandedChange = { isMenuExpanded = !isMenuExpanded }
                ) {
                    TextField(
                        value = languageOptions.find { it.first == rightEarbudLanguage }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Right Earbud Language") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = primaryColor,
                            unfocusedIndicatorColor = onSurfaceColor.copy(alpha = 0.5f),
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(surfaceVariantColor)
                    ) {
                        languageOptions.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, color = onSurfaceVariantColor) },
                                onClick = {
                                    if (code != leftEarbudLanguage) {
                                        rightEarbudLanguage = code
                                        isMenuExpanded = false
                                    }
                                },
                                enabled = code != leftEarbudLanguage,
                                colors = MenuDefaults.itemColors(
                                    textColor = onSurfaceVariantColor,
                                    leadingIconColor = onSurfaceVariantColor
                                )
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { checkPermissions() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Start Translation", color = Color.White)
            }
        }
    }
}