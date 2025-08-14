package com.example.interpret.viewmodel

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
    private val _status = MutableStateFlow("Select languages and start")
    val status: StateFlow<String> = _status

    fun startTranslation(
        inputLanguage: String,
        outputLanguage1: String,
        outputLanguage2: String
    ) {
        viewModelScope.launch {
            try {
                _status.value = "Starting translation..."
                speechService.startContinuousTranslation(
                    inputLanguage = inputLanguage,
                    outputLanguage1 = outputLanguage1,
                    outputLanguage2 = outputLanguage2,
                    onAudioOutput = { language, audio ->
                        bluetoothService.routeAudioToEarbud(language, audio)
                    }
                )
                _status.value = "Translation in progress"
            } catch (e: Exception) {
                _status.value = "Error: ${e.message}"
            }
        }
    }

    fun stopTranslation() {
        speechService.stopTranslation()
        _status.value = "Translation stopped"
    }
}