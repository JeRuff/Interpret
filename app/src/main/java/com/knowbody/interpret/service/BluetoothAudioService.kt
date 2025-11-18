package com.knowbody.interpret.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BluetoothAudioService @Inject constructor(
    private val context: Context
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val TAG = "BluetoothAudioService"
    private val activeTracks = mutableListOf<AudioTrack>()

    // Track which language goes to which earbud
    private var leftEarbudLanguage: String = ""
    private var rightEarbudLanguage: String = ""

    fun setLanguageRouting(outputLanguage1: String, outputLanguage2: String) {
        leftEarbudLanguage = outputLanguage1
        rightEarbudLanguage = outputLanguage2
        Log.d(TAG, "Language routing set - Left: $outputLanguage1, Right: $outputLanguage2")
    }

    suspend fun routeAudioToEarbud(language: String, audioData: ByteArray) = withContext(Dispatchers.IO) {
        var audioTrack: AudioTrack? = null

        try {
            // Azure Speech Service returns 16kHz, 16-bit, mono PCM by default
            val sampleRate = 16000
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            // Determine which channel(s) to use based on language routing
            val channelConfig = when (language) {
                leftEarbudLanguage -> {
                    Log.d(TAG, "Routing $language to LEFT earbud")
                    AudioFormat.CHANNEL_OUT_FRONT_LEFT
                }
                rightEarbudLanguage -> {
                    Log.d(TAG, "Routing $language to RIGHT earbud")
                    AudioFormat.CHANNEL_OUT_FRONT_RIGHT
                }
                else -> {
                    Log.w(TAG, "Language $language not mapped, using MONO output")
                    AudioFormat.CHANNEL_OUT_MONO
                }
            }

            // Calculate buffer size
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(audioData.size * 2, minBufferSize) // *2 for stereo conversion

            Log.d(TAG, "Playing audio for $language: ${audioData.size} bytes, buffer: $bufferSize")

            // Convert mono to stereo with proper channel routing
            val stereoData = convertMonoToStereoWithRouting(audioData, channelConfig)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO) // Always use stereo for routing
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            synchronized(activeTracks) {
                activeTracks.add(audioTrack)
            }

            // Start playback
            audioTrack.play()

            // Write audio data
            val bytesWritten = audioTrack.write(stereoData, 0, stereoData.size)
            Log.d(TAG, "Wrote $bytesWritten bytes to audio track")

            // Wait for playback to complete
            val durationMs = ((audioData.size / 2.0) / sampleRate * 1000).toLong()
            Thread.sleep(durationMs + 100) // Add small buffer

        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio for $language: ${e.message}", e)
        } finally {
            try {
                audioTrack?.let { track ->
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.stop()
                    }
                    track.release()
                    synchronized(activeTracks) {
                        activeTracks.remove(track)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up audio track: ${e.message}")
            }
        }
    }

    /**
     * Converts mono PCM16 audio to stereo, routing to specified channel
     * @param monoData Original mono audio data
     * @param channelConfig Which channel to route to (LEFT, RIGHT, or MONO for both)
     * @return Stereo audio data
     */
    private fun convertMonoToStereoWithRouting(monoData: ByteArray, channelConfig: Int): ByteArray {
        val stereoData = ByteArray(monoData.size * 2)

        // PCM16 is 2 bytes per sample
        for (i in 0 until monoData.size step 2) {
            val sample = ((monoData[i + 1].toInt() shl 8) or (monoData[i].toInt() and 0xFF)).toShort()

            val stereoIndex = i * 2

            when (channelConfig) {
                AudioFormat.CHANNEL_OUT_FRONT_LEFT -> {
                    // Left channel: original audio
                    stereoData[stereoIndex] = monoData[i]
                    stereoData[stereoIndex + 1] = monoData[i + 1]
                    // Right channel: silence (0)
                    stereoData[stereoIndex + 2] = 0
                    stereoData[stereoIndex + 3] = 0
                }
                AudioFormat.CHANNEL_OUT_FRONT_RIGHT -> {
                    // Left channel: silence (0)
                    stereoData[stereoIndex] = 0
                    stereoData[stereoIndex + 1] = 0
                    // Right channel: original audio
                    stereoData[stereoIndex + 2] = monoData[i]
                    stereoData[stereoIndex + 3] = monoData[i + 1]
                }
                else -> {
                    // Both channels: original audio (MONO/fallback)
                    stereoData[stereoIndex] = monoData[i]
                    stereoData[stereoIndex + 1] = monoData[i + 1]
                    stereoData[stereoIndex + 2] = monoData[i]
                    stereoData[stereoIndex + 3] = monoData[i + 1]
                }
            }
        }

        return stereoData
    }

    fun cleanup() {
        synchronized(activeTracks) {
            activeTracks.forEach { track ->
                try {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.stop()
                    }
                    track.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during cleanup: ${e.message}")
                }
            }
            activeTracks.clear()
        }
        leftEarbudLanguage = ""
        rightEarbudLanguage = ""
        Log.d(TAG, "Audio service cleaned up")
    }

    fun isBluetoothAudioConnected(): Boolean {
        return audioManager.isBluetoothScoOn ||
                audioManager.isBluetoothA2dpOn ||
                audioManager.isWiredHeadsetOn
    }

    fun getAudioDeviceInfo(): String {
        return when {
            audioManager.isBluetoothScoOn -> "Bluetooth SCO"
            audioManager.isBluetoothA2dpOn -> "Bluetooth A2DP"
            audioManager.isWiredHeadsetOn -> "Wired Headset"
            audioManager.isSpeakerphoneOn -> "Speakerphone"
            else -> "Unknown/Default"
        }
    }
}