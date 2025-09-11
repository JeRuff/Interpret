package com.knowbody.interpret.service

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.microsoft.cognitiveservices.speech.*
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import java.io.File

class AzureSpeechService(private val context: Context) {
    companion object {
        private const val TAG = "AzureSpeechService"
        private const val SAMPLE_RATE = 16000 // Changed to 16kHz for broader compatibility
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val BUFFER_SIZE_FACTOR = 2
        private const val SILENCE_TIMEOUT_SECONDS = 15L // Timeout for no speech
        private const val SILENCE_THRESHOLD = 300 // Lowered for sensitivity
    }

    private var speechConfig: SpeechConfig? = null
    private var audioConfig: AudioConfig? = null
    private var recognizer: SpeechRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var isRunning = false
    private val lock = ReentrantLock()
    private val speechSynthesisTasks = mutableMapOf<String, SpeechSynthesizer>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        try {
            // Initialize SpeechConfig with your Azure Speech Service subscription key and region
            speechConfig = SpeechConfig.fromSubscription("5xnphLc8VYj1yKTzdBQJ8Xz3g3ILltUHnOos5dJTMbLTwqMD0MlhJQQJ99BHACi5YpzXJ3w3AAAYACOGgrJH", "northeurope")
            speechConfig?.speechRecognitionLanguage = "fr-FR" // Single language for testing
            speechConfig?.setProperty("SpeechSegmentBoundary", "true")
            speechConfig?.setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "15000")
            speechConfig?.setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "15000")

            val logFile = File(context.filesDir, "speech_sdk.log").absolutePath
            speechConfig?.setProperty(PropertyId.Speech_LogFilename, logFile)

            speechConfig?.setProperty("subscriptionkey", "5xnphLc8VYj1yKTzdBQJ8Xz3g3ILltUHnOos5dJTMbLTwqMD0MlhJQQJ99BHACi5YpzXJ3w3AAAYACOGgrJH")
            speechConfig?.setProperty("region", "northeurope")
            Log.d(TAG, "SpeechConfig initialized successfully, log file: $logFile")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SpeechConfig", e)
            throw e
        }
    }

    fun startTranslation(leftEarbudLanguage: String, rightEarbudLanguage: String, context: Context) {
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
            setupAudioRecord()
            setupRecognizers(leftEarbudLanguage, rightEarbudLanguage)

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
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recognizer?.stopContinuousRecognitionAsync()?.get()
            recognizer?.close()
            recognizer = null
            speechSynthesisTasks.values.forEach { it.close() }
            speechSynthesisTasks.clear()
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

    private fun setupAudioRecord() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_FACTOR
            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                Log.d(TAG, "AudioRecord started with buffer size: $bufferSize")
            } else {
                Log.e(TAG, "AudioRecord failed to initialize")
                throw IllegalStateException("AudioRecord not initialized")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Audio recording permission denied", e)
            isRunning = false
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup AudioRecord", e)
            isRunning = false
            throw e
        }
    }

    private fun setupRecognizers(leftEarbudLanguage: String, rightEarbudLanguage: String) {
        try {
            val format = AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE.toLong(), 16, 1)
            val pushStream = AudioInputStream.createPushStream(format)
            audioConfig = AudioConfig.fromStreamInput(pushStream)
            recognizer = SpeechRecognizer(speechConfig, audioConfig)
            var lastSpeechDetectedTime = System.currentTimeMillis()
            recognizer?.recognized?.addEventListener { _, e ->
                if (e.result.reason == ResultReason.RecognizedSpeech) {
                    val rawLanguage = e.result.properties.getProperty("SpeechServiceConnection.RecoLanguage", "unknown")
                    Log.d(TAG, "Raw detected language: $rawLanguage")
                    val detectedLanguage = when {
                        rawLanguage.contains("fr-FR", ignoreCase = true) -> "fr-FR"
                        rawLanguage.contains("lt-LT", ignoreCase = true) -> "lt-LT"
                        else -> "unknown"
                    }
                    val text = e.result.text
                    Log.d(TAG, "Recognized speech - Language: $detectedLanguage, Text: $text")
                    if (text.isNotEmpty()) {
                        lastSpeechDetectedTime = System.currentTimeMillis() // Update on recognized speech
                        routeTranslation(text, detectedLanguage, leftEarbudLanguage, rightEarbudLanguage)
                    } else {
                        Log.w(TAG, "Empty text detected")
                    }
                } else {
                    Log.w(TAG, "Speech not recognized, reason: ${e.result.reason}")
                }
            }
            recognizer?.recognizing?.addEventListener { _, e ->
                Log.d(TAG, "Recognizing speech: ${e.result.text}")
                if (e.result.text.isNotEmpty()) {
                    lastSpeechDetectedTime = System.currentTimeMillis() // Update on recognizing speech
                }
            }
            recognizer?.sessionStarted?.addEventListener { _, _ ->
                Log.d(TAG, "Recognition session started")
            }
            recognizer?.sessionStopped?.addEventListener { _, _ ->
                Log.d(TAG, "Recognition session stopped")
            }
            recognizer?.canceled?.addEventListener { _, e ->
                Log.e(TAG, "Recognition canceled: ${e.reason}, error: ${e.errorDetails}")
            }
            recognizer?.startContinuousRecognitionAsync()?.get()
            Log.d(TAG, "SpeechRecognizer started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup SpeechRecognizer", e)
            throw e
        }
    }

    private suspend fun startAudioProcessing() = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
        val audioBuffer = ByteArray(bufferSize)
        val format = AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE.toLong(), 16, 1)
        val pushStream = AudioInputStream.createPushStream(format)

        try {
            Log.d(TAG, "Audio processing started")
            var lastSpeechDetectedTime = System.currentTimeMillis()
            while (isRunning) {
                try {
                    val read = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        val isSilence = isAudioSilent(audioBuffer, read)
                        Log.v(TAG, "Audio data read: $read bytes, isSilence: $isSilence")
                        pushStream.write(audioBuffer.copyOfRange(0, read))
                    } else {
                        Log.w(TAG, "No audio data read: $read")
                    }
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
            pushStream.close()
            Log.d(TAG, "Audio processing stopped")
        }
    }

    private fun isAudioSilent(buffer: ByteArray, length: Int): Boolean {
        for (i in 0 until length step 2) {
            if (i + 1 < length) {
                val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                if (abs(sample) > SILENCE_THRESHOLD) {
                    return false // Audio is not silent
                }
            }
        }
        return true // Audio is silent
    }

    private fun routeTranslation(text: String, detectedLanguage: String, leftEarbudLanguage: String, rightEarbudLanguage: String) {
        coroutineScope.launch {
            val targetLanguage = when (detectedLanguage) {
                leftEarbudLanguage -> rightEarbudLanguage
                rightEarbudLanguage -> leftEarbudLanguage
                else -> {
                    Log.w(TAG, "Unknown detected language: $detectedLanguage")
                    return@launch
                }
            }
            val earbud = if (detectedLanguage == leftEarbudLanguage) "right" else "left"
            Log.d(TAG, "Translating: detected=$detectedLanguage, target=$targetLanguage, earbud=$earbud, text='$text'")

            try {
                val synthesizer = speechSynthesisTasks.getOrPut(targetLanguage) {
                    val synthesisConfig = SpeechConfig.fromSubscription(speechConfig?.getProperty("subscriptionkey"), speechConfig?.getProperty("region"))
                    synthesisConfig.speechSynthesisLanguage = targetLanguage
                    val synthesizer = SpeechSynthesizer(synthesisConfig)
                    synthesizer.Synthesizing.addEventListener { _, e ->
                        Log.d(TAG, "Synthesizing: audioData size=${e.result.audioData.size} bytes")
                    }
                    synthesizer.SynthesisCompleted.addEventListener { _, e ->
                        if (e.result.reason == ResultReason.SynthesizingAudioCompleted) {
                            Log.d(TAG, "Synthesis complete: audioData size=${e.result.audioData.size} bytes for earbud=$earbud")
                            BluetoothAudioService(context).routeAudioToEarbud(earbud, e.result.audioData, context)
                        } else {
                            val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(e.result)
                            Log.e(TAG, "Synthesis canceled: ${cancellationDetails.errorDetails}")
                        }
                    }
                    synthesizer.SynthesisCanceled.addEventListener { _, e ->
                        val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(e.result)
                        Log.e(TAG, "Synthesis canceled: ${cancellationDetails.errorDetails}")
                    }
                    synthesizer
                }

                val result = withContext(Dispatchers.IO) {
                    synthesizer.SpeakTextAsync(text).get()
                }

                if (result.reason == ResultReason.SynthesizingAudioCompleted) {
                    Log.d(TAG, "Synthesis complete (alternate check): audioData size=${result.audioData.size} bytes for earbud=$earbud")
                    BluetoothAudioService(context).routeAudioToEarbud(earbud, result.audioData, context)
                } else {
                    val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(result)
                    Log.e(TAG, "Synthesis failed: ${cancellationDetails.errorDetails}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to synthesize audio for text: $text", e)
            }
        }
    }

    fun testEarbudChannel(earbud: String, context: Context) {
        Log.d(TAG, "Testing $earbud earbud")
        BluetoothAudioService(context).testEarbudChannel(earbud, context)
    }
}