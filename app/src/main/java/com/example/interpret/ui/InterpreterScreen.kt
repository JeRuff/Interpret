package com.example.interpret.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.interpret.viewmodel.InterpreterViewModel

@Preview
@Composable
fun InterpreterScreenPreview() {
    InterpreterScreenContent(
        inputLanguage = "fr-FR",
        earbud1Language = "lt-LT",
        earbud2Language = "fr-FR",
        status = "Idle",
        onStartTranslation = {},
        onStopTranslation = {}
    )
}

@Composable
fun InterpreterScreen(viewModel: InterpreterViewModel = hiltViewModel()) {

    // List of supported languages
    val languages = listOf(
        "French (fr-FR)" to "fr-FR",
        "Lithuanian (lt-LT)" to "lt-LT"
    )

    var inputLanguage by remember { mutableStateOf(languages[0].second) } // Default: fr-FR
    var earbud1Language by remember { mutableStateOf(languages[1].second) } // Default: lt-LT
    var earbud2Language by remember { mutableStateOf(languages[0].second) } // Default: fr-FR

    val status by viewModel.status.collectAsState()



    // Automatically start/restart translation when languages change or on initial load
    LaunchedEffect(inputLanguage, earbud1Language, earbud2Language) {
        viewModel.stopTranslation()  // Stop previous if running
        viewModel.startContinuousTranslation(
            inputLanguage = inputLanguage,
            outputLanguage1 = earbud1Language,
            outputLanguage2 = earbud2Language
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
        earbud1Language = earbud1Language,
        earbud2Language = earbud2Language,
        status = status,
        onStartTranslation = {
            viewModel.startContinuousTranslation(
                inputLanguage = inputLanguage,
                outputLanguage1 = earbud1Language,
                outputLanguage2 = earbud2Language
            )
        },
        onStopTranslation = { viewModel.stopTranslation() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpreterScreenContent(
    inputLanguage: String,
    earbud1Language: String,
    earbud2Language: String,
    status: String,
    onStartTranslation: () -> Unit,
    onStopTranslation: () -> Unit
) {

    // List of supported languages
    val languages = listOf(
        "French (fr-FR)" to "fr-FR",
        "Lithuanian (lt-LT)" to "lt-LT"
    )

    var inputLanguage by remember { mutableStateOf(languages[0].second) } // Default: fr-FR
    var earbud1Language by remember { mutableStateOf(languages[1].second) } // Default: lt-LT
    var earbud2Language by remember { mutableStateOf(languages[0].second) } // Default: fr-FR

    var inputExpanded by remember { mutableStateOf(false) }
    var earbud1Expanded by remember { mutableStateOf(false) }
    var earbud2Expanded by remember { mutableStateOf(false) }


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

        // Earbud 1 Language Dropdown
        ExposedDropdownMenuBox(
            expanded = earbud1Expanded,
            onExpandedChange = { earbud1Expanded = !earbud1Expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = languages.find { it.second == earbud1Language }?.first ?: earbud1Language,
                onValueChange = {},
                readOnly = true,
                label = { Text("Earbud 1 Language") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = earbud1Expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = earbud1Expanded,
                onDismissRequest = { earbud1Expanded = false }
            ) {
                languages.forEach { (display, code) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            earbud1Language = code
                            earbud1Expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

// Earbud 2 Language Dropdown
        ExposedDropdownMenuBox(
            expanded = earbud2Expanded,
            onExpandedChange = { earbud2Expanded = !earbud2Expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = languages.find { it.second == earbud2Language }?.first ?: earbud2Language,
                onValueChange = {},
                readOnly = true,
                label = { Text("Earbud 2 Language") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = earbud2Expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = earbud2Expanded,
                onDismissRequest = { earbud2Expanded = false }
            ) {
                languages.forEach { (display, code) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            earbud2Language = code
                            earbud2Expanded = false
                        }
                    )
                }
            }
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
    var earbud1Language by remember { mutableStateOf("lt-LT") }
    var earbud2Language by remember { mutableStateOf("fr-FR") }
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
            value = earbud1Language,
            onValueChange = { earbud1Language = it },
            label = { Text("Earbud 1 Language (fr-FR or lt-LT)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = earbud2Language,
            onValueChange = { earbud2Language = it },
            label = { Text("Earbud 2 Language (fr-FR or lt-LT)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop Buttons
        Button(
            onClick = {
                viewModel.startTranslation(
                    inputLanguage = inputLanguage,
                    outputLanguage1 = earbud1Language,
                    outputLanguage2 = earbud2Language
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

