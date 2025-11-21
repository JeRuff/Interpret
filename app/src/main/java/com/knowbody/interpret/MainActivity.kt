package com.knowbody.interpret

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.knowbody.interpret.ui.InterpreterScreen
import com.knowbody.interpret.ui.theme.InterpretTheme
import dagger.hilt.android.AndroidEntryPoint
import android.bluetooth.BluetoothManager
import android.content.Context

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissionRequestId = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, permissionRequestId)
        }

        // Enable Bluetooth if not enabled
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }.launch(enableBtIntent)
        }

        setContent {
            InterpretTheme {
                // Simple - just show the interpreter screen
                InterpreterScreen()
            }
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}