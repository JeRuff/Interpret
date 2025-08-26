package com.example.interpret.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.interpret.viewmodel.InterpreterViewModel
import kotlinx.coroutines.withContext
import com.example.interpret.service.BluetoothAudioService

@Preview
@Composable
fun InterpreterScreenPreview() {
    InterpreterScreenContent(
        inputLanguage = "fr-FR",
        earbudLeftLanguage = "lt-LT",
        earbudRightLanguage = "fr-FR",
        status = "Idle",
        onStartTranslation = {},
        onStopTranslation = {},
        context = LocalContext.current
    )
}

@Composable
fun InterpreterScreen(viewModel: InterpreterViewModel = hiltViewModel(), context: `Context`) {
    val TAG = "InterpreterScreen"
    // List of supported languages
    val languages = listOf(
        "French (fr-FR)" to "fr-FR",
        "Lithuanian (lt-LT)" to "lt-LT"
    )

    var inputLanguage by remember { mutableStateOf(languages[0].second) } // Default: fr-FR
    var earbudLeftLanguage by remember { mutableStateOf(languages[1].second) } // Default: lt-LT
    var earbudRightLanguage by remember { mutableStateOf(languages[0].second) } // Default: fr-FR

    val status by viewModel.status.collectAsState()

    // Automatically start/restart translation when languages change or on initial load
    LaunchedEffect(inputLanguage, earbudLeftLanguage, earbudRightLanguage) {
        Log.i("Interpret Service", "Restarting translation with input: $inputLanguage, output1: $earbudLeftLanguage, output2: $earbudRightLanguage");
        viewModel.stopTranslation()  // Stop previous if running
        viewModel.startContinuousTranslation(
            inputLanguage = inputLanguage,
            leftEarbudLanguage = earbudLeftLanguage,
            rightEarbudLanguage = earbudRightLanguage,
            context = context
        )
    }

    // Dispose: Stop translation when screen is disposed (e.g., app closed)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTranslation()
        }
    }

    InterpreterScreenContent(
        inputLanguage = inputLanguage,
        earbudLeftLanguage = earbudLeftLanguage,
        earbudRightLanguage = earbudRightLanguage,
        status = status,
        onStartTranslation = {
            viewModel.startContinuousTranslation(
                inputLanguage = inputLanguage,
                leftEarbudLanguage = earbudLeftLanguage,
                rightEarbudLanguage = earbudRightLanguage,
                context = context
            )
        },
        onStopTranslation = { viewModel.stopTranslation() },
        context = context
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpreterScreenContent(
     inputLanguage: String,
     earbudLeftLanguage: String,
     earbudRightLanguage: String,
     status: String,
     context: Context,
    onStartTranslation: () -> Unit,
    onStopTranslation: () -> Unit
) {

    // List of supported languages
    val languages = listOf(
        "French (fr-FR)" to "fr-FR",
        "Lithuanian (lt-LT)" to "lt-LT"
    )
    val TAG = "InterpreterScreen"

    var inputLanguage by remember { mutableStateOf(languages[0].second) } // Default: fr-FR
    var earbudLeftLanguage by remember { mutableStateOf(languages[1].second) } // Default: lt-LT
    var earbudRightLanguage by remember { mutableStateOf(languages[0].second) } // Default: fr-FR

    var inputExpanded by remember { mutableStateOf(false) }
    var earbudLeftExpanded by remember { mutableStateOf(false) }
    var earbudRightExpanded by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SpeakEZ", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Input Language Dropdown
        ExposedDropdownMenuBox(
            expanded = inputExpanded,
            onExpandedChange = { inputExpanded = !inputExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = languages.find { it.second == inputLanguage }?.first ?: inputLanguage,
                onValueChange = {},
                readOnly = true,
                label = { Text("Input Language") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = inputExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()            )
            ExposedDropdownMenu(
                expanded = inputExpanded,
                onDismissRequest = { inputExpanded = false }
            ) {
                languages.forEach { (display, code) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            inputLanguage = code
                            inputExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Earbud Left Language Dropdown
        ExposedDropdownMenuBox(
            expanded = earbudLeftExpanded,
            onExpandedChange = { earbudLeftExpanded = !earbudLeftExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = languages.find { it.second == earbudLeftLanguage }?.first ?: earbudLeftLanguage,
                onValueChange = {earbudLeftLanguage = it},
                readOnly = true,
                label = { Text("Earbud Left Language") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = earbudLeftExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = earbudLeftExpanded,
                onDismissRequest = { earbudLeftExpanded = false }
            ) {
                languages.forEach { (display, code) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            earbudLeftLanguage = code
                            earbudLeftExpanded = false
                            Log.d(TAG, "Selected left earbud language: $earbudLeftLanguage")

                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

// Earbud 2 Language Dropdown
        ExposedDropdownMenuBox(
            expanded = earbudRightExpanded,
            onExpandedChange = { earbudRightExpanded = !earbudRightExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = languages.find { it.second == earbudRightLanguage }?.first ?: earbudRightLanguage,
                onValueChange = {earbudRightLanguage = it},
                readOnly = true,
                label = { Text("Earbud Right Language") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = earbudRightExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = earbudRightExpanded,
                onDismissRequest = { earbudRightExpanded = false }
            ) {
                languages.forEach { (display, code) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            earbudRightLanguage = code
                            earbudRightExpanded = false
                            Log.d(TAG, "Selected right earbud language: $earbudRightLanguage")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test Button
        Button(
            onClick = {
                BluetoothAudioService().testEarbudChannel(
                    "left",
                    context = context
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Left Bud Sound")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                BluetoothAudioService().testEarbudChannel(
                    "right",
                    context = context
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Right Bud Sound")
        }

        Spacer(modifier = Modifier.height(16.dp))


        // Status
        Text("Status: $status", style = MaterialTheme.typography.bodyLarge)
    }
}

/*@Preview
@Composable
fun InterpreterScreen(viewModel: InterpreterViewModel = hiltViewModel()) {
    var inputLanguage by remember { mutableStateOf("fr-FR") }
    var earbudLeftLanguage by remember { mutableStateOf("lt-LT") }
    var earbudRightLanguage by remember { mutableStateOf("fr-FR") }
    val status by viewModel.status.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Real-Time Interpreter", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Language Selection
        OutlinedTextField(
            value = inputLanguage,
            onValueChange = { inputLanguage = it },
            label = { Text("Input Language (fr-FR or lt-LT)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = earbudLeftLanguage,
            onValueChange = { earbudLeftLanguage = it },
            label = { Text("Earbud 1 Language (fr-FR or lt-LT)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = earbudRightLanguage,
            onValueChange = { earbudRightLanguage = it },
            label = { Text("Earbud 2 Language (fr-FR or lt-LT)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop Buttons
        Button(
            onClick = {
                viewModel.startTranslation(
                    inputLanguage = inputLanguage,
                    outputLanguage1 = earbudLeftLanguage,
                    outputLanguage2 = earbudRightLanguage
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Translation")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.stopTranslation() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Translation")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status
        Text("Status: $status", style = MaterialTheme.typography.bodyLarge)
    }
}*/

