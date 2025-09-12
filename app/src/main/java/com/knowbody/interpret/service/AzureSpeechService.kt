package com.knowbody.interpret.service

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

class AzureSpeechService @Inject constructor(
    private val context: Context,
    private val speechKey: String,
    private val speechRegion: String
) {
    companion object {
        private const val TAG = "AzureSpeechService"
        private const val SILENCE_TIMEOUT_SECONDS = 15L // Timeout for no speech
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
        private val FALLBACK_REGIONS = listOf("westeurope", "eastus") // Fallback regions
    }

    private var speechConfig: SpeechTranslationConfig? = null
    private var audioConfig: AudioConfig? = null
    private var recognizer: TranslationRecognizer? = null
    private var isRunning = false
    private val lock = ReentrantLock()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var currentRegion = speechRegion

    init {
        val logFile = File(context.filesDir, "speech_sdk.log").absolutePath

        try {
            // Initialize SpeechTranslationConfig
            speechConfig = SpeechTranslationConfig.fromSubscription(speechKey, currentRegion).apply {
                setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "15000")
                setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "15000")
                // Set log file to app-specific directory
                setProperty(PropertyId.Speech_LogFilename, logFile)
            }
            Log.d(TAG, "SpeechTranslationConfig initialized successfully, log file: $logFile, region: $currentRegion")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SpeechTranslationConfig", e)
            throw e
        }
    }

    suspend fun startTranslation(
        leftEarbudLanguage: String,
        rightEarbudLanguage: String,
        context: Context,
        onAudioOutput: (String, ByteArray) -> Unit
    ) {
        if (!hasAudioPermission(context)) {
            Log.w(TAG, "Audio permission not granted")
            throw SecurityException("Audio permission not granted")
        }
        if (!hasAppOpsAudioPermission(context)) {
            Log.w(TAG, "AppOps RECORD_AUDIO permission not granted")
            throw SecurityException("AppOps RECORD_AUDIO permission not granted")
        }

        lock.lock()
        try {
            if (isRunning) {
                stopTranslation()
            }

            isRunning = true
            setupRecognizersWithRetry(leftEarbudLanguage, rightEarbudLanguage, onAudioOutput)

            coroutineScope.launch {
                startAudioProcessing()
            }
            Log.d(TAG, "Translation started for left: $leftEarbudLanguage, right: $rightEarbudLanguage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start translation", e)
            throw e
        } finally {
            lock.unlock()
        }
    }

    fun stopTranslation() {
        lock.lock()
        try {
            isRunning = false
            recognizer?.stopContinuousRecognitionAsync()?.get()
            recognizer?.close()
            recognizer = null
            Log.d(TAG, "Translation stopped")
        } finally {
            lock.unlock()
        }
    }

    private fun hasAudioPermission(context: Context): Boolean {
        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Audio permission granted: $granted")
        return granted
    }

    private fun hasAppOpsAudioPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_RECORD_AUDIO,
                android.os.Process.myUid(),
                context.packageName
            )
            val granted = mode == android.app.AppOpsManager.MODE_ALLOWED
            Log.d(TAG, "AppOps RECORD_AUDIO permission mode: $mode, granted: $granted")
            return granted
        }
        return true // AppOps not required below API 29
    }

    private suspend fun setupRecognizersWithRetry(
        leftEarbudLanguage: String,
        rightEarbudLanguage: String,
        onAudioOutput: (String, ByteArray) -> Unit
    ) {
        var attempt = 0
        var regions = listOf(currentRegion) + FALLBACK_REGIONS
        var lastException: Exception? = null

        while (attempt < MAX_RETRIES && regions.isNotEmpty()) {
            try {
                Log.d(TAG, "Attempting to setup recognizer with region: $currentRegion, attempt: ${attempt + 1}")
                speechConfig = SpeechTranslationConfig.fromSubscription(speechKey, currentRegion).apply {
                    speechRecognitionLanguage = leftEarbudLanguage // Default to left earbud language
                    addTargetLanguage(leftEarbudLanguage)
                    addTargetLanguage(rightEarbudLanguage)
                    setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "15000")
                    setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "15000")
                    val logFile = File(context.filesDir, "speech_sdk.log").absolutePath
                    setProperty(PropertyId.Speech_LogFilename, logFile)
                }
                audioConfig = AudioConfig.fromDefaultMicrophoneInput()
                recognizer = TranslationRecognizer(speechConfig, audioConfig)
                var lastSpeechDetectedTime = System.currentTimeMillis()

                recognizer?.recognized?.addEventListener { _, event ->
                    if (event.result.reason == ResultReason.TranslatedSpeech) {
                        val translations = event.result.translations
                        translations.forEach { (lang, text) ->
                            Log.d(TAG, "Recognized translation - Language: $lang, Text: $text")
                            if (text.isNotEmpty()) {
                                lastSpeechDetectedTime = System.currentTimeMillis() // Update on recognized speech
                                synthesizeSpeech(text, lang, onAudioOutput)
                            } else {
                                Log.w(TAG, "Empty translation text detected for language: $lang")
                            }
                        }
                    } else {
                        Log.w(TAG, "Translation not recognized, reason: ${event.result.reason}")
                    }
                }
                recognizer?.recognizing?.addEventListener { _, event ->
                    val translations = event.result.translations
                    translations.forEach { (lang, text) ->
                        Log.d(TAG, "Recognizing translation - Language: $lang, Text: $text")
                        if (text.isNotEmpty()) {
                            lastSpeechDetectedTime = System.currentTimeMillis() // Update on recognizing speech
                        }
                    }
                }
                recognizer?.sessionStarted?.addEventListener { _, e ->
                    Log.d(TAG, "Recognition session started, ConnectionId: ${e.sessionId}")
                }
                recognizer?.sessionStopped?.addEventListener { _, e ->
                    Log.d(TAG, "Recognition session stopped, ConnectionId: ${e.sessionId}")
                }
                recognizer?.canceled?.addEventListener { _, e ->
                    Log.e(TAG, "Recognition canceled: ${e.reason}, error: ${e.errorDetails}")
                    if (e.errorDetails.contains("WS_OPEN_ERROR_UNDERLYING_IO_OPEN_FAILED")) {
                        Log.w(TAG, "WebSocket connection failed, consider switching region or checking network")
                    }
                }
                recognizer?.startContinuousRecognitionAsync()?.get()
                Log.d(TAG, "TranslationRecognizer started with region: $currentRegion")
                return // Success, exit retry loop
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup TranslationRecognizer with region: $currentRegion, attempt: ${attempt + 1}", e)
                lastException = e
                attempt++
                if (attempt < MAX_RETRIES && regions.isNotEmpty()) {
                    currentRegion = regions[1] // Move to next region
                    regions = regions.drop(1)
                    Log.d(TAG, "Retrying with new region: $currentRegion")
                    delay(RETRY_DELAY_MS) // Wait before retry
                }
            }
        }
        if (lastException != null) {
            Log.e(TAG, "All retries failed for TranslationRecognizer setup", lastException)
            throw lastException
        }
    }

    private fun synthesizeSpeech(text: String, language: String, onAudioOutput: (String, ByteArray) -> Unit) {
        coroutineScope.launch {
            try {
                val synthesisConfig = SpeechConfig.fromSubscription(speechKey, currentRegion).apply {
                    speechSynthesisVoiceName = if (language == "fr-FR") "fr-FR-DeniseNeural" else "lt-LT-OnaNeural"
                }
                val synthesizer = com.microsoft.cognitiveservices.speech.SpeechSynthesizer(synthesisConfig)
                val result = withContext(Dispatchers.IO) {
                    synthesizer.SpeakTextAsync(text).get()
                }
                if (result.reason == ResultReason.SynthesizingAudioCompleted) {
                    val earbud = if (language == "fr-FR") "left" else "right"
                    Log.d(TAG, "Synthesis complete: audioData size=${result.audioData.size} bytes for language=$language, earbud=$earbud")
                    onAudioOutput(earbud, result.audioData)
                } else {
                    val cancellationDetails = com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails.fromResult(result)
                    Log.e(TAG, "Synthesis failed: ${cancellationDetails.errorDetails}")
                }
                result.close()
                synthesizer.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to synthesize audio for text: $text, language: $language", e)
            }
        }
    }

    private suspend fun startAudioProcessing() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Audio processing started")
            var lastSpeechDetectedTime = System.currentTimeMillis()
            while (isRunning) {
                try {
                    if (System.currentTimeMillis() - lastSpeechDetectedTime > SILENCE_TIMEOUT_SECONDS * 1000) {
                        Log.w(TAG, "No speech detected for $SILENCE_TIMEOUT_SECONDS seconds, stopping processing")
                        isRunning = false
                        break
                    }
                    delay(10) // Small delay to prevent tight loop
                } catch (e: Exception) {
                    Log.e(TAG, "Audio processing error", e)
                    break
                }
            }
        } finally {
            recognizer?.stopContinuousRecognitionAsync()?.get()
            Log.d(TAG, "Audio processing stopped")
        }
    }

    fun testEarbudChannel(earbud: String, context: Context) {
        Log.d(TAG, "Testing $earbud earbud")
        BluetoothAudioService().testEarbudChannel(earbud, context)
    }
}