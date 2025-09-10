package com.knowbody.interpret.viewmodel

import android.content.Context
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
                    context = context
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

    fun testEarbudChannel(earbud: String, context: Context) {
        viewModelScope.launch {
            _status.value = "Testing $earbud earbud..."
            bluetoothService.testEarbudChannel(earbud, context)
            _status.value = "Test $earbud earbud complete"
        }
    }
}