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
    var inputLanguage by remember { mutableStateOf("fr-FR") }
    var earbud1Language by remember { mutableStateOf("lt-LT") }
    var earbud2Language by remember { mutableStateOf("fr-FR") }
    val status by viewModel.status.collectAsState()

    InterpreterScreenContent(
        inputLanguage = inputLanguage,
        earbud1Language = earbud1Language,
        earbud2Language = earbud2Language,
        status = status,
        onStartTranslation = {
            viewModel.startTranslation(
                inputLanguage = inputLanguage,
                outputLanguage1 = earbud1Language,
                outputLanguage2 = earbud2Language
            )
        },
        onStopTranslation = { viewModel.stopTranslation() }
    )
}

@Composable
fun InterpreterScreenContent(
    inputLanguage: String,
    earbud1Language: String,
    earbud2Language: String,
    status: String,
    onStartTranslation: () -> Unit,
    onStopTranslation: () -> Unit
) {
    var vinputlanguage by remember { mutableStateOf(inputLanguage) }
    var vearbud1Language by remember { mutableStateOf(earbud1Language) }
    var vearbud2Language by remember { mutableStateOf(earbud2Language) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SpeakEZ", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Language Selection
        OutlinedTextField(
            value = vinputlanguage,
            onValueChange = { vinputlanguage = it },
            label = { Text("Input Language (fr-FR or lt-LT)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = vearbud1Language,
            onValueChange = { vearbud1Language = it },
            label = { Text("Earbud 1 Language (fr-FR or lt-LT)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = vearbud2Language,
            onValueChange = { vearbud2Language = it },
            label = { Text("Earbud 2 Language (fr-FR or lt-LT)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop Buttons
        Button(
            onClick = { onStartTranslation() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Translation")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onStopTranslation() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Translation")
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

