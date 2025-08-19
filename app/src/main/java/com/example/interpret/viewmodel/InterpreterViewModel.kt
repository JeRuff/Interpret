package com.example.interpret.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.interpret.service.AzureSpeechService
import com.example.interpret.service.BluetoothAudioService
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
    private val _status = MutableStateFlow("Initializing translation...")
    val status: StateFlow<String> = _status

    fun startContinuousTranslation(
        inputLanguage: String,
        outputLanguage1: String,
        outputLanguage2: String
    ) {
        viewModelScope.launch {
            try {
                _status.value = "Listening for speech..."
                speechService.startContinuousTranslation(
                    inputLanguage = inputLanguage,
                    outputLanguage1 = outputLanguage1,
                    outputLanguage2 = outputLanguage2,
                    onAudioOutput = { language, audio ->
                        bluetoothService.routeAudioToEarbud(language, audio)
                        _status.value = "Translating to $language"

                    }
                )
                Log.e("Interpret Service", "Starting translation with input: $inputLanguage, output1: $outputLanguage1, output2: $outputLanguage2");
                //_status.value = "Translation Active"
            } catch (e: Exception) {
                _status.value = "Error: ${e.message}"
            }
        }
    }

    fun stopTranslation() {
        speechService.stopTranslation()
        _status.value = "Translation stopped"
    }

    override fun onCleared() {
        super.onCleared()
        stopTranslation()
    }
}