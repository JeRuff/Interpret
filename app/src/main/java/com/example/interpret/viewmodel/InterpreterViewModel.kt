package com.example.interpret.viewmodel

import android.content.Context
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
    private val TAG = "InterpreterViewModel"


    fun startContinuousTranslation(
        leftEarbudLanguage: String,
        rightEarbudLanguage: String,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                _status.value = "Listening for speech..."
                speechService.startContinuousTranslation(
                    leftEarbudLanguage = leftEarbudLanguage,
                    rightEarbudLanguage = rightEarbudLanguage,
                    earbudLeft = "left",
                    earbudRight = "right",
                    onAudioOutput = { earbud, audio ->
                        Log.d(TAG, "Routing audio to earbud=$earbud, audioSize=${audio.size} bytes")
                        bluetoothService.routeAudioToEarbud(earbud, audio, context)

                    }
                )
                _status.value = "Translation Active"
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