package com.example.interpret.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.interpret.viewmodel.InterpreterViewModel
import com.example.interpret.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpreterScreen(viewModel: InterpreterViewModel = hiltViewModel(), context: Context) {
    val TAG = "InterpreterScreen"
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    var earbudLeftLanguage by remember { mutableStateOf("fr-FR") }
    var earbudRightLanguage by remember { mutableStateOf("lt-LT") }
    var expandedLeft by remember { mutableStateOf(false) }
    var expandedRight by remember { mutableStateOf(false) }
    val languages = listOf("fr-FR" to "French", "lt-LT" to "Lithuanian")

    // Automatically start/restart translation when languages change or on initial load
    LaunchedEffect(earbudLeftLanguage, earbudRightLanguage) {
        Log.d(TAG, "LaunchedEffect triggered: Restarting translation with outputLeft: $earbudLeftLanguage and outputRight: $earbudRightLanguage")
        viewModel.stopTranslation()  // Stop previous if running
        (context as? MainActivity)?.keepScreenOn(true)
        viewModel.startContinuousTranslation(
            leftEarbudLanguage = earbudLeftLanguage,
            rightEarbudLanguage = earbudRightLanguage,
            context = context
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                    readOnly = true,
                    value = languages.find { it.first == earbudLeftLanguage }?.second ?: "",
                    onValueChange = { },
                    label = { Text("Left Earbud Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLeft) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedLeft,
                    onDismissRequest = { expandedLeft = false }
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                Log.d(TAG, "Selected left earbud language: $code")
                                earbudLeftLanguage = code
                                expandedLeft = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
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
                    readOnly = true,
                    value = languages.find { it.first == earbudRightLanguage }?.second ?: "",
                    onValueChange = { },
                    label = { Text("Right Earbud Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRight) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedRight,
                    onDismissRequest = { expandedRight = false }
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                Log.d(TAG, "Selected right earbud language: $code")
                                earbudRightLanguage = code
                                expandedRight = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop Buttons
        Button(
            onClick = {
                Log.d(TAG, "Manual start: leftEarbud=$earbudLeftLanguage, rightEarbud=$earbudRightLanguage")
                viewModel.stopTranslation()
                (context as? MainActivity)?.keepScreenOn(true)
                viewModel.startContinuousTranslation(
                    leftEarbudLanguage = earbudLeftLanguage,
                    rightEarbudLanguage = earbudRightLanguage,
                    context = context
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Translation")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                Log.d(TAG, "Stopping translation")
                viewModel.stopTranslation()
                (context as? MainActivity)?.keepScreenOn(false)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Translation")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status
        Text("Status: $status", style = MaterialTheme.typography.bodyLarge)
    }
}