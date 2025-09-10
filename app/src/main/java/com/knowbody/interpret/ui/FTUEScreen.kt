package com.knowbody.interpret.ui

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.knowbody.interpret.R
import kotlinx.coroutines.launch

@Composable
fun FTUEScreen(onPermissionsGranted: () -> Unit) {
    val TAG = "FTUEScreen"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Define a revised professional color palette
    val primaryColor = Color(0xFF1976D2) // Blue
    val surfaceColor = Color(0xFFF5F5F5) // Warm off-white
    val onSurfaceColor = Color(0xFF212121) // Dark slate gray
    val secondaryColor = Color(0xFFBBDEFB) // Soft blue accent

    // Permission states
    var micPermissionGranted by remember { mutableStateOf(false) }
    var bluetoothPermissionGranted by remember { mutableStateOf(false) }
    var bluetoothConnectPermissionGranted by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        micPermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        bluetoothPermissionGranted = permissions[Manifest.permission.BLUETOOTH] ?: false
        bluetoothConnectPermissionGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        Log.d(TAG, "Permissions result: mic=$micPermissionGranted, bluetooth=$bluetoothPermissionGranted, bluetoothConnect=$bluetoothConnectPermissionGranted")
        if (micPermissionGranted && bluetoothPermissionGranted && bluetoothConnectPermissionGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("All permissions granted!")
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
        micPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        bluetoothPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == android.content.pm.PackageManager.PERMISSION_GRANTED
        bluetoothConnectPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Initial permissions: mic=$micPermissionGranted, bluetooth=$bluetoothPermissionGranted, bluetoothConnect=$bluetoothConnectPermissionGranted")
        if (micPermissionGranted && bluetoothPermissionGranted && bluetoothConnectPermissionGranted) {
            context.getSharedPreferences("InterpreterPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("ftue_completed", true)
                .apply()
            onPermissionsGranted()
        }
    }

    // Main UI with centered content
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
            // Welcome Header
            Text(
                text = "Welcome to Real-Time Interpreter",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    color = onSurfaceColor
                ),
                textAlign = TextAlign.Center
            )

            // Permission Instructions
            Text(
                text = "To enable real-time translation, please grant the following permissions:",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = onSurfaceColor,
                    textAlign = TextAlign.Center
                )
            )

            // Permission Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PermissionCard(
                    iconRes = R.drawable.ic_mic,
                    label = "Microphone",
                    onGrant = { permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) },
                    granted = micPermissionGranted
                )
                PermissionCard(
                    iconRes = R.drawable.ic_bluetooth,
                    label = "Bluetooth",
                    onGrant = { permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT)) },
                    granted = bluetoothConnectPermissionGranted
                )
            }

            // Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Snackbar(
                    snackbarData = it,
                    containerColor = primaryColor.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    iconRes: Int,
    label: String,
    onGrant: () -> Unit,
    granted: Boolean
) {
    val primaryColor = Color(0xFF1976D2) // Blue
    val onSurfaceColor = Color(0xFF212121) // Dark slate gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = "$label Permission",
                tint = if (granted) primaryColor else onSurfaceColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(color = onSurfaceColor)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (!granted) {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Grant", color = Color.White)
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = "Granted",
                    tint = primaryColor
                )
            }
        }
    }
}