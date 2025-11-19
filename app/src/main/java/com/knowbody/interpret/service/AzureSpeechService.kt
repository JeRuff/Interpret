package com.knowbody.interpret.service

import android.util.Log
import com.knowbody.interpret.model.LanguageConfig
import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.text.get

class AzureSpeechService @Inject constructor(
    private val speechKey: String,
    private val speechRegion: String
) {
    private var recognizer: TranslationRecognizer? = null
    private val TAG = "AzureSpeechService"

    suspend fun startContinuousTranslation(
        outputLanguage1: String,
        outputLanguage2: String,
        onAudioOutput: (String, ByteArray) -> Unit,
        onError: (String) -> Unit,
        onTranslationText: (String, String) -> Unit = { _, _ -> },
        onLanguageDetected: (String) -> Unit = { }
    ) = withContext(Dispatchers.IO) {
        try {
            // Validate API key
            if (speechKey.isEmpty() || speechKey == "YOUR_AZURE_KEY_HERE") {
                throw IllegalStateException("Azure Speech API key not configured. Please add it to local.properties")
            }

            // Create language list from output languages for auto-detection
            val sourceLanguages = listOf(outputLanguage1, outputLanguage2).distinct()
            Log.d(TAG, "Auto-detecting languages: $sourceLanguages")

            val speechConfig =
                SpeechTranslationConfig.fromSubscription(speechKey, speechRegion).apply {
                    // Don't set speechRecognitionLanguage - we'll use auto-detection instead
                    addTargetLanguage(outputLanguage1)
                    addTargetLanguage(outputLanguage2)
                }

            val audioConfig = AudioConfig.fromDefaultMicrophoneInput()

            // Create recognizer with auto-detection
            fun createRecognizer(): TranslationRecognizer {
                val autoDetectConfig = AutoDetectSourceLanguageConfig.fromLanguages(sourceLanguages)
                return TranslationRecognizer(speechConfig, autoDetectConfig, audioConfig).apply {
                    // Handle successful recognition
                    recognized.addEventListener { _, event ->
                        try {
                            if (event.result.reason == com.microsoft.cognitiveservices.speech.ResultReason.TranslatedSpeech) {
                                // Get detected language
                                val detectedLanguage = event.result.properties.getProperty(
                                    com.microsoft.cognitiveservices.speech.PropertyId.SpeechServiceConnection_AutoDetectSourceLanguageResult
                                )

                                if (detectedLanguage != null) {
                                    Log.d(TAG, "Detected language: $detectedLanguage")
                                    onLanguageDetected(detectedLanguage)
                                }

                                val originalText = event.result.text
                                if (originalText.isNotBlank()) {
                                    Log.d(TAG, "Original text [$detectedLanguage]: $originalText")
                                }

                                val translations = event.result.translations
                                translations.forEach { (lang, text) ->
                                        Log.d(TAG, "Translation [$lang]: $text")
                                        onTranslationText(lang, text)
                                        synthesizeSpeech(text, lang, onAudioOutput, onError)
                                }

                                try {
                                    recognizer?.stopContinuousRecognitionAsync()?.get()
                                    recognizer?.close()
                                    recognizer = createRecognizer() // recreate with fresh auto-detect config
                                    recognizer?.startContinuousRecognitionAsync()?.get()
                                } catch (e: Exception) {
                                    Log.e(
                                        TAG,
                                        "Error restarting recognizer for re-detection: ${e.message}",
                                        e
                                    )
                                }

                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in recognized event: ${e.message}", e)
                            onError("Translation processing error: ${e.message}")
                        }
                    }

                    // Handle recognition events (for real-time feedback)
                    recognizing.addEventListener { _, event ->
                        if (event.result.reason == com.microsoft.cognitiveservices.speech.ResultReason.TranslatingSpeech) {
                            val detectedLanguage = event.result.properties.getProperty(
                                com.microsoft.cognitiveservices.speech.PropertyId.SpeechServiceConnection_AutoDetectSourceLanguageResult
                            )
                            if (detectedLanguage != null) {
                                onLanguageDetected(detectedLanguage)
                            }
                        }
                    }

                    // Handle errors
                    canceled.addEventListener { _, event ->
                        val errorMsg = "Recognition canceled: ${event.reason}"
                        Log.e(TAG, errorMsg)
                        onError(errorMsg)
                    }
                }
            }
            recognizer = createRecognizer()
            recognizer?.startContinuousRecognitionAsync()?.get()
            Log.d(TAG, "Continuous recognition started with auto language detection")
        } catch (e: Exception) {
            val errorMsg = "Failed to start translation: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onError(errorMsg)
            throw e
        }
    }
    private fun synthesizeSpeech(
        text: String,
        language: String,
        onAudioOutput: (String, ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        var synthesizer: SpeechSynthesizer? = null
        var result: SpeechSynthesisResult? = null

        try {
            val voiceName = LanguageConfig.getVoiceForLanguage(language)
            val speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion).apply {
                speechSynthesisVoiceName = voiceName
            }

            synthesizer = SpeechSynthesizer(speechConfig, null)
            result = synthesizer.SpeakTextAsync(text).get()

            when (result.reason) {
                ResultReason.SynthesizingAudioCompleted -> {
                    val audioData = result.audioData
                    if (audioData != null && audioData.isNotEmpty()) {
                        Log.d(TAG, "Synthesis completed for $language, ${audioData.size} bytes")
                        onAudioOutput(language, audioData)
                    } else {
                        Log.w(TAG, "Synthesis completed but no audio data")
                    }
                }
                ResultReason.Canceled -> {
                    val errorMsg = "Synthesis canceled for $language"
                    Log.e(TAG, errorMsg)
                    onError(errorMsg)
                }
                else -> {
                    val errorMsg = "Synthesis failed: ${result.reason}"
                    Log.e(TAG, errorMsg)
                    onError(errorMsg)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Synthesis error for $language: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onError(errorMsg)
        } finally {
            // Ensure resources are always cleaned up
            try {
                result?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing result: ${e.message}")
            }
            try {
                synthesizer?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing synthesizer: ${e.message}")
            }
        }
    }

    suspend fun stopTranslation() = withContext(Dispatchers.IO) {
        try {
            recognizer?.stopContinuousRecognitionAsync()?.get()
            Log.d(TAG, "Recognition stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition: ${e.message}", e)
        } finally {
            try {
                recognizer?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing recognizer: ${e.message}")
            }
            recognizer = null
        }
    }
}
