package com.knowbody.interpret.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knowbody.interpret.service.AzureSpeechService
import com.knowbody.interpret.service.BluetoothAudioService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterpreterViewModel @Inject constructor(
    private val speechService: AzureSpeechService,
    private val bluetoothService: BluetoothAudioService
) : ViewModel() {
    private val _status = MutableStateFlow("Select languages and start")
    val status: StateFlow<String> = _status

    fun startTranslation(
        leftEarbudLanguage: String,
        rightEarbudLanguage: String,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                _status.value = "Starting translation..."
                speechService.startTranslation(
                    leftEarbudLanguage = leftEarbudLanguage,
                    rightEarbudLanguage = rightEarbudLanguage,
                    context = context,
                    onAudioOutput = { earbud, audioData ->
                        bluetoothService.routeAudioToEarbud(earbud, audioData, context)
                    }
                )
                _status.value = "Translation in progress"
            } catch (e: Exception) {
                Log.e("InterpreterViewModel", "Failed to start translation", e)
                _status.value = when {
                    e.message?.contains("Audio permission not granted") == true -> "Error: Please grant audio permission"
                    e.message?.contains("AppOps RECORD_AUDIO permission not granted") == true -> "Error: Microphone access blocked by system, please check permissions"
                    e.message?.contains("No speech detected for 15 seconds") == true -> "Error: No speech detected, please speak clearly and try again"
                    e.message?.contains("SPXERR_LOG_FILE_OPEN_FAILED") == true -> "Error: Failed to initialize logging, check storage permissions"
                    e.message?.contains("WS_OPEN_ERROR_UNDERLYING_IO_OPEN_FAILED") == true -> "Error: Failed to connect to Azure service, check network or credentials"
                    else -> "Error: ${e.message ?: "Unknown error, check network or Azure credentials"}"
                }
            }
        }
    }

    fun stopTranslation() {
        try {
            speechService.stopTranslation()
            _status.value = "Translation stopped"
        } catch (e: Exception) {
            Log.e("InterpreterViewModel", "Failed to stop translation", e)
            _status.value = "Error: ${e.message ?: "Unknown error"}"
        }
    }

    fun testEarbudChannel(earbud: String, context: Context) {
        viewModelScope.launch {
            try {
                _status.value = "Testing $earbud earbud..."
                bluetoothService.testEarbudChannel(earbud, context)
                _status.value = "Test $earbud earbud complete"
            } catch (e: Exception) {
                Log.e("InterpreterViewModel", "Failed to test earbud channel", e)
                _status.value = "Error: ${e.message ?: "Unknown error"}"
            }
        }
    }
}