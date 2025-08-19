package com.example.interpret.service

import android.util.Log
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AzureSpeechService @Inject constructor(
    private val speechKey: String,
    private val speechRegion: String
) {
    private var recognizer: TranslationRecognizer? = null

    suspend fun startContinuousTranslation(
        inputLanguage: String,
        outputLanguage1: String,
        outputLanguage2: String,
        onAudioOutput: (String, ByteArray) -> Unit
    ) = withContext(Dispatchers.IO) {
        val speechConfig = SpeechTranslationConfig.fromSubscription(speechKey, speechRegion).apply {
            speechRecognitionLanguage = inputLanguage
            addTargetLanguage(outputLanguage1)
            addTargetLanguage(outputLanguage2)
            voiceName = if (outputLanguage1 == "fr-FR") "fr-FR-DeniseNeural" else "lt-LT-OnaNeural"
            // Faster utterance detection for real-time
            setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "200")
        }

        Log.i("Interpret Service", "Starting translation with input: $inputLanguage, output1: $outputLanguage1, output2: $outputLanguage2");


        val audioConfig = AudioConfig.fromDefaultMicrophoneInput()
        recognizer = TranslationRecognizer(speechConfig, audioConfig)

        recognizer?.recognizing?.addEventListener { _, event ->
            if (event.result.reason == ResultReason.RecognizingSpeech) {
                val originalText = event.result.text ?: ""
                // Translate only if enough info (3+ words)
                if (originalText.split(" ").filter { it.isNotBlank() }.size >= 3) {
                    val translations = event.result.translations
                    translations.forEach { (lang, text) ->
                        synthesizeSpeech(text, lang, onAudioOutput)
                    }
                }
            }
        }

        recognizer?.recognized?.addEventListener { _, event ->
            if (event.result.reason == ResultReason.RecognizedSpeech) {
                val originalText = event.result.text ?: ""
                // Translate only if enough info (3+ words)
                if (originalText.split(" ").filter { it.isNotBlank() }.size >= 3) {
                    val translations = event.result.translations
                    translations.forEach { (lang, text) ->
                        synthesizeSpeech(text, lang, onAudioOutput)
                    }
                }
            }
        }

        recognizer?.startContinuousRecognitionAsync()?.get()
    }

    private fun synthesizeSpeech(text: String, language: String, onAudioOutput: (String, ByteArray) -> Unit) {
        val speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion).apply {
            speechSynthesisVoiceName = if (language == "fr-FR") "fr-FR-DeniseNeural" else "lt-LT-OnaNeural"
        }
        val synthesizer = com.microsoft.cognitiveservices.speech.SpeechSynthesizer(speechConfig)
        val result = synthesizer.SpeakTextAsync(text).get()
        if (result.reason == ResultReason.SynthesizingAudioCompleted) {
            onAudioOutput(language, result.audioData)
        }
        result.close()
        synthesizer.close()
    }

    fun stopTranslation() {
        recognizer?.stopContinuousRecognitionAsync()?.get()
        recognizer?.close()
    }
}