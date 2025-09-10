package com.knowbody.interpret.service

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.microsoft.cognitiveservices.speech.*
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AzureSpeechService(private val context: Context) {
    companion object {
        private const val TAG = "AzureSpeechService"
        private const val SAMPLE_RATE = 24000 // 24kHz for Raw24Khz16BitMonoPcm
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val BUFFER_SIZE_FACTOR = 2
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
        // Initialize SpeechConfig with your Azure Speech Service subscription key and region
        speechConfig = SpeechConfig.fromSubscription("Bp98ummrHrp32jao0zfJ45KsjDbdkunpTmTliWNeDjBXgWrwX9ZGJQQJ99BHACi5YpzXJ3w3AAAYACOG3eno", "northeurope")
        speechConfig?.speechRecognitionLanguage = "auto" // Auto-detect language
        speechConfig?.setProperty("SpeechSegmentBoundary", "true") // Enable segment boundary for real-time
    }

    fun startTranslation(leftEarbudLanguage: String, rightEarbudLanguage: String, context: Context) {
        if (!hasAudioPermission(context)) {
            Log.w(TAG, "Audio permission not granted. Requesting permission.")
            // Permission request should be handled by FTUEScreen; log and return if not granted yet
            return
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
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
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
            audioRecord?.startRecording()
            Log.d(TAG, "AudioRecord started with buffer size: $bufferSize")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Audio recording permission denied", e)
            isRunning = false
            // Notify UI or user via callback (e.g., through ViewModel)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup AudioRecord", e)
            isRunning = false
        }
    }

    private fun setupRecognizers(leftEarbudLanguage: String, rightEarbudLanguage: String) {
        audioConfig = AudioConfig.fromWavFileInput(null) // Will use push stream
        recognizer = SpeechRecognizer(speechConfig, audioConfig)
        recognizer?.recognized?.addEventListener { _, e ->
            if (e.result.reason == ResultReason.RecognizedSpeech) {
                val detectedLanguage = e.result.properties.getProperty("Language") ?: "unknown"
                val text = e.result.text
                Log.d(TAG, "Detected language: $detectedLanguage, Text: $text")
                routeTranslation(text, detectedLanguage, leftEarbudLanguage, rightEarbudLanguage)
            }
        }
        recognizer?.startContinuousRecognitionAsync()?.get()
    }

    private suspend fun startAudioProcessing() = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
        val audioBuffer = ByteArray(bufferSize)
        val pushStream = AudioInputStream.createPushStream()

        audioConfig = AudioConfig.fromStreamInput(pushStream)
        recognizer = SpeechRecognizer(speechConfig, audioConfig)
        recognizer?.startContinuousRecognitionAsync()?.get()

        while (isRunning) {
            try {
                val read = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    pushStream.write(audioBuffer)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio processing error", e)
                break
            }
        }

        recognizer?.stopContinuousRecognitionAsync()?.get()
    }

    private fun routeTranslation(text: String, detectedLanguage: String, leftEarbudLanguage: String, rightEarbudLanguage: String) {
        coroutineScope.launch {
            val targetLanguage = when (detectedLanguage) {
                leftEarbudLanguage -> rightEarbudLanguage
                rightEarbudLanguage -> leftEarbudLanguage
                else -> return@launch // Ignore if not a configured language
            }
            val earbud = if (detectedLanguage == leftEarbudLanguage) "right" else "left"
            Log.d(TAG, "Translating: detected=$detectedLanguage, target=$targetLanguage, earbud=$earbud, text='$text'")

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
                        // Route audio to the appropriate earbud via BluetoothAudioService
                        // (Implementation assumed to be handled elsewhere)
                    } else if (e.result.reason == ResultReason.Canceled) {
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
                synthesizer.SpeakTextAsync(text).get() // Blocking call, handled in IO dispatcher
            }

            if (result.reason == ResultReason.SynthesizingAudioCompleted) {
                Log.d(TAG, "Synthesis complete (alternate check): audioData size=${result.audioData.size} bytes for earbud=$earbud")
                // Route audio to the appropriate earbud via BluetoothAudioService
                // (Implementation assumed to be handled elsewhere)
            }else if (result.reason == ResultReason.Canceled) {
                val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(result)
                Log.e(TAG, "Synthesis canceled: ${cancellationDetails.errorDetails}")
            }
        }
    }

    fun testEarbudChannel(earbud: String, context: Context) {
        Log.d(TAG, "Testing $earbud earbud")
        // Implementation for testing earbud audio (assumed handled by BluetoothAudioService)
    }
}