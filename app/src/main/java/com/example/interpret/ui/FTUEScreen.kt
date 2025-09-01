package com.example.interpret.ui

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.interpret.R
import kotlinx.coroutines.launch

@Composable
fun FTUEScreen(
    onPermissionsGranted: () -> Unit
) {
    val TAG = "FTUEScreen"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission states
    var micPermissionGranted by remember { mutableStateOf(false) }
    var bluetoothPermissionGranted by remember { mutableStateOf(false) }
    var bluetoothConnectPermissionGranted by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        micPermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        bluetoothPermissionGranted = permissions[Manifest.permission.BLUETOOTH] ?: false
        bluetoothConnectPermissionGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        Log.d(TAG, "Permissions result: mic=$micPermissionGranted, bluetooth=$bluetoothPermissionGranted, bluetoothConnect=$bluetoothConnectPermissionGranted")
        if (micPermissionGranted && bluetoothPermissionGranted && bluetoothConnectPermissionGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("All permissions granted!")
                // Save FTUE completion
                context.getSharedPreferences("InterpreterPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("ftue_completed", true)
                    .apply()
                onPermissionsGranted()
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please grant all permissions to continue")
            }
        }
    }

    // Check initial permission state
    LaunchedEffect(Unit) {
        micPermissionGranted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        bluetoothPermissionGranted = context.checkSelfPermission(Manifest.permission.BLUETOOTH) == android.content.pm.PackageManager.PERMISSION_GRANTED
        bluetoothConnectPermissionGranted = context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Initial permissions: mic=$micPermissionGranted, bluetooth=$bluetoothPermissionGranted, bluetoothConnect=$bluetoothConnectPermissionGranted")
        if (micPermissionGranted && bluetoothPermissionGranted && bluetoothConnectPermissionGranted) {
            context.getSharedPreferences("InterpreterPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("ftue_completed", true)
                .apply()
            onPermissionsGranted()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "Welcome to Real-Time Interpreter",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions
            Text(
                text = "To enable real-time translation, please grant the following permissions:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Microphone Permission
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (micPermissionGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = "Microphone permission",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Microphone",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Required for speech recognition",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (!micPermissionGranted) {
                        Button(
                            onClick = {
                                Log.d(TAG, "Requesting microphone permission")
                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text("Grant")
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "Microphone permission granted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bluetooth Permissions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (bluetoothPermissionGranted && bluetoothConnectPermissionGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bluetooth),
                        contentDescription = "Bluetooth permission",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Bluetooth",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Required for earbud audio routing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (!bluetoothPermissionGranted || !bluetoothConnectPermissionGranted) {
                        Button(
                            onClick = {
                                Log.d(TAG, "Requesting Bluetooth permissions")
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text("Grant")
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "Bluetooth permissions granted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Continue Button
            Button(
                onClick = {
                    Log.d(TAG, "Requesting all permissions")
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !(micPermissionGranted && bluetoothPermissionGranted && bluetoothConnectPermissionGranted),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Grant All Permissions", fontSize = 18.sp)
            }
        }
    }
}