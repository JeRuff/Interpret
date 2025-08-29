package com.example.interpret.ui

import android.content.Context
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.interpret.MainActivity
import com.example.interpret.R
import com.example.interpret.viewmodel.InterpreterViewModel
import kotlinx.coroutines.launch


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
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    // Automatically start/restart translation when languages change or on initial load
    LaunchedEffect(earbudLeftLanguage, earbudRightLanguage) {
        Log.d(
            TAG,
            "LaunchedEffect triggered: Restarting translation with outputLeft: $earbudLeftLanguage and outputRight: $earbudRightLanguage"
        )
        viewModel.stopTranslation()  // Stop previous if running
        (context as? MainActivity)?.keepScreenOn(true)
        viewModel.startContinuousTranslation(
            leftEarbudLanguage = earbudLeftLanguage,
            rightEarbudLanguage = earbudRightLanguage,
            context = context
        )
    }

    // Prevent same language for both earbuds - not in use - currently allowing for testing
    fun validateLanguageSelection(newLang: String, otherLang: String): Boolean {
        if (newLang == otherLang) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Cannot select the same language for both earbuds")
            }
            return false
        }
        return true
    }

    // Animation for translation status
    val isTranslating = status == "Translation in progress"
    val statusBackground by animateColorAsState(
        targetValue = if (isTranslating) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(500)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        content = { padding: PaddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header
                Text(
                    text = "SpeakEZ",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))
                // Left Earbud Language Selection
                    ExposedDropdownMenuBox(
                        expanded = expandedLeft,
                        onExpandedChange = { expandedLeft = it }
                    ) {
                        OutlinedTextField(
                            value = languages.find { it.first == earbudLeftLanguage }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Left Earbud Language") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_earbud_left),
                                    contentDescription = "Left earbud",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLeft) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLeft,
                            onDismissRequest = { expandedLeft = false }
                        ) {
                            languages.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, style = MaterialTheme.typography.bodyLarge) },
                                    onClick = {
                                        Log.d(TAG, "Selected left earbud language: $code")
                                        earbudLeftLanguage = code
                                        expandedLeft = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Left earbud set to $name")
                                        }
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(16.dp))

                // Right Earbud Language Selection
                ExposedDropdownMenuBox(
                    expanded = expandedRight,
                    onExpandedChange = { expandedRight = it }
                ) {
                    OutlinedTextField(
                        value = languages.find { it.first == earbudRightLanguage }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Right Earbud Language") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_earbud_right),
                                contentDescription = "Right earbud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRight) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRight,
                        onDismissRequest = { expandedRight = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    Log.d(TAG, "Selected right earbud language: $code")
                                    earbudRightLanguage = code
                                    expandedRight = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Right earbud set to $name")
                                    }
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Status Display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = statusBackground
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isTranslating) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                        }
                        Text(
                            text = when (status) {
                                "Translation in progress" -> "Listening for French or Lithuanian"
                                "Select languages and start" -> "Ready to start translation"
                                else -> status
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}