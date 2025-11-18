package com.knowbody.interpret.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knowbody.interpret.service.AzureSpeechService
import com.knowbody.interpret.service.BluetoothAudioService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterpreterViewModel @Inject constructor(
    private val speechService: AzureSpeechService,
    private val bluetoothService: BluetoothAudioService
) : ViewModel() {

    private val TAG = "InterpreterViewModel"

    private val _status = MutableStateFlow("Select languages and start")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _lastTranslation = MutableStateFlow<Pair<String, String>?>(null)
    val lastTranslation: StateFlow<Pair<String, String>?> = _lastTranslation.asStateFlow()

    fun startTranslation(
        inputLanguage: String,
        outputLanguage1: String,
        outputLanguage2: String
    ) {
        if (_isTranslating.value) {
            _error.value = "Translation already in progress"
            return
        }

        // Validate inputs
        if (inputLanguage.isBlank() || outputLanguage1.isBlank() || outputLanguage2.isBlank()) {
            _error.value = "Please select all language fields"
            return
        }

        viewModelScope.launch {
            try {
                _status.value = "Starting translation..."
                _error.value = null
                _isTranslating.value = true

                Log.d(TAG, "Starting translation: $inputLanguage -> L:$outputLanguage1, R:$outputLanguage2")

                // Configure audio routing BEFORE starting translation
                bluetoothService.setLanguageRouting(outputLanguage1, outputLanguage2)

                speechService.startContinuousTranslation(
                    inputLanguage = inputLanguage,
                    outputLanguage1 = outputLanguage1,
                    outputLanguage2 = outputLanguage2,
                    onAudioOutput = { language, audio ->
                        viewModelScope.launch {
                            try {
                                bluetoothService.routeAudioToEarbud(language, audio)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error routing audio: ${e.message}")
                            }
                        }
                    },
                    onError = { errorMsg ->
                        Log.e(TAG, "Translation error: $errorMsg")
                        _error.value = errorMsg
                    },
                    onTranslationText = { language, text ->
                        _lastTranslation.value = Pair(language, text)
                    }
                )

                val audioDevice = bluetoothService.getAudioDeviceInfo()
                _status.value = "Translating... (Left: $outputLanguage1, Right: $outputLanguage2)\nOutput: $audioDevice"

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                Log.e(TAG, "Failed to start translation", e)
                _status.value = "Error starting translation"
                _error.value = errorMsg
                _isTranslating.value = false
            }
        }
    }

    fun stopTranslation() {
        viewModelScope.launch {
            try {
                _status.value = "Stopping..."
                speechService.stopTranslation()
                _status.value = "Translation stopped"
                _lastTranslation.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping translation", e)
                _error.value = "Error stopping: ${e.message}"
            } finally {
                _isTranslating.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, cleaning up resources")
        viewModelScope.launch {
            try {
                speechService.stopTranslation()
                bluetoothService.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}