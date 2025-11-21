package com.knowbody.interpret.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knowbody.interpret.model.LanguageConfig
import com.knowbody.interpret.viewmodel.InterpreterViewModel
import androidx.compose.ui.res.stringResource
import com.knowbody.interpret.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpreterScreen(viewModel: InterpreterViewModel = hiltViewModel()) {
    val languages = LanguageConfig.availableLanguages

    var outputLanguage1 by remember { mutableStateOf(languages[0].code) } // French default
    var outputLanguage2 by remember { mutableStateOf(languages[1].code) } // Lithuanian default

    var output1Expanded by remember { mutableStateOf(false) }
    var output2Expanded by remember { mutableStateOf(false) }

    val status by viewModel.status.collectAsState()
    val error by viewModel.error.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val lastTranslation by viewModel.lastTranslation.collectAsState()
    val detectedLanguage by viewModel.detectedLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.app_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.auto_language_detection),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Detected Language Indicator (when translating)
        if (isTranslating && detectedLanguage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎤 ", style = MaterialTheme.typography.titleLarge)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.speaking_in),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            languages.find { it.code == detectedLanguage }?.displayName ?: detectedLanguage!!,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Output Language 1 (Left Earbud)
        ExposedDropdownMenuBox(
            expanded = output1Expanded,
            onExpandedChange = { output1Expanded = !output1Expanded && !isTranslating }
        ) {
            OutlinedTextField(
                value = languages.find { it.code == outputLanguage1 }?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.left_earbud_language)) },
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
                        text = { Text(language.displayName, color = Color.White) },
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
                label = { Text(stringResource(R.string.right_earbud_language)) },
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
                        text = { Text(language.displayName, color = Color.White)},
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
                    outputLanguage1 = outputLanguage1,
                    outputLanguage2 = outputLanguage2
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTranslating
        ) {
            Text(stringResource(R.string.start_translation))
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
            Text(stringResource(R.string.stop_translation))
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
                Text(stringResource(R.string.status_label), style = MaterialTheme.typography.labelLarge)
                Text(status, style = MaterialTheme.typography.bodyMedium)
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
                            stringResource(R.string.error_label),
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
                        Text(stringResource(R.string.dismiss))
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
                    stringResource(R.string.how_it_works_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.how_it_works_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}