package com.knowbody.interpret.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knowbody.interpret.viewmodel.InterpreterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpreterScreen(viewModel: InterpreterViewModel = hiltViewModel<InterpreterViewModel>()) {
    val TAG = "InterpreterScreen"
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()

    var leftEarbudLanguage by remember { mutableStateOf("fr-FR") }
    var rightEarbudLanguage by remember { mutableStateOf("lt-LT") }
    var expandedLeft by remember { mutableStateOf(false) }
    var expandedRight by remember { mutableStateOf(false) }
    val languages = listOf("fr-FR" to "French", "lt-LT" to "Lithuanian")

    // State to trigger translation actions
    var startTranslation by remember { mutableStateOf(false) }
    var stopTranslation by remember { mutableStateOf(false) }

    // Handle translation actions outside @Composable context
    LaunchedEffect(startTranslation) {
        if (startTranslation) {
            viewModel.startTranslation(leftEarbudLanguage, rightEarbudLanguage, context)
            startTranslation = false
        }
    }
    LaunchedEffect(stopTranslation) {
        if (stopTranslation) {
            viewModel.stopTranslation()
            stopTranslation = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Real-Time Interpreter", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Left Earbud Language Selection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedLeft,
                onExpandedChange = { expandedLeft = it }
            ) {
                TextField(
                    value = languages.find { it.first == leftEarbudLanguage }?.second ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Left Earbud Language") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedLeft,
                    onDismissRequest = { expandedLeft = false }
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                leftEarbudLanguage = code
                                expandedLeft = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Right Earbud Language Selection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedRight,
                onExpandedChange = { expandedRight = it }
            ) {
                TextField(
                    value = languages.find { it.first == rightEarbudLanguage }?.second ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Right Earbud Language") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedRight,
                    onDismissRequest = { expandedRight = false }
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                rightEarbudLanguage = code
                                expandedRight = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop Buttons
        Button(
            onClick = { startTranslation = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Translation")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { stopTranslation = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Translation")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status
        Text("Status: $status", style = MaterialTheme.typography.bodyLarge)
    }
}