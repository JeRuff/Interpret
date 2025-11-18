package com.knowbody.interpret.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knowbody.interpret.model.LanguageConfig
import com.knowbody.interpret.viewmodel.InterpreterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpreterScreen(viewModel: InterpreterViewModel = hiltViewModel()) {
    val languages = LanguageConfig.availableLanguages

    var inputLanguage by remember { mutableStateOf(languages[0].code) }
    var outputLanguage1 by remember { mutableStateOf(languages[1].code) }
    var outputLanguage2 by remember { mutableStateOf(languages[0].code) }

    var inputExpanded by remember { mutableStateOf(false) }
    var output1Expanded by remember { mutableStateOf(false) }
    var output2Expanded by remember { mutableStateOf(false) }

    val status by viewModel.status.collectAsState()
    val error by viewModel.error.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val lastTranslation by viewModel.lastTranslation.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Real-Time Interpreter", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        // Input Language Dropdown
        ExposedDropdownMenuBox(
            expanded = inputExpanded,
            onExpandedChange = { inputExpanded = !inputExpanded && !isTranslating }
        ) {
            OutlinedTextField(
                value = languages.find { it.code == inputLanguage }?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Input Language (Microphone)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = inputExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !isTranslating,
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = inputExpanded,
                onDismissRequest = { inputExpanded = false }
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.displayName) },
                        onClick = {
                            inputLanguage = language.code
                            inputExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Output Language 1 (Left Earbud)
        ExposedDropdownMenuBox(
            expanded = output1Expanded,
            onExpandedChange = { output1Expanded = !output1Expanded && !isTranslating }
        ) {
            OutlinedTextField(
                value = languages.find { it.code == outputLanguage1 }?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Left Earbud Language") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = output1Expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !isTranslating,
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = output1Expanded,
                onDismissRequest = { output1Expanded = false }
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.displayName) },
                        onClick = {
                            outputLanguage1 = language.code
                            output1Expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Output Language 2 (Right Earbud)
        ExposedDropdownMenuBox(
            expanded = output2Expanded,
            onExpandedChange = { output2Expanded = !output2Expanded && !isTranslating }
        ) {
            OutlinedTextField(
                value = languages.find { it.code == outputLanguage2 }?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Right Earbud Language") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = output2Expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !isTranslating,
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = output2Expanded,
                onDismissRequest = { output2Expanded = false }
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.displayName) },
                        onClick = {
                            outputLanguage2 = language.code
                            output2Expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start/Stop Buttons
        Button(
            onClick = {
                viewModel.startTranslation(
                    inputLanguage = inputLanguage,
                    outputLanguage1 = outputLanguage1,
                    outputLanguage2 = outputLanguage2
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTranslating
        ) {
            Text("Start Translation")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.stopTranslation() },
            modifier = Modifier.fillMaxWidth(),
            enabled = isTranslating,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Stop Translation")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status:", style = MaterialTheme.typography.labelLarge)
                Text(status, style = MaterialTheme.typography.bodyMedium)

                lastTranslation?.let { (lang, text) ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Last translation:", style = MaterialTheme.typography.labelLarge)
                    Text("[$lang]",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Error Display
        error?.let { errorMessage ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Error:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "🎧 Audio Routing Info",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "• Left Earbud: Output Language 1\n• Right Earbud: Output Language 2\n• Each earbud receives only its assigned language",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}