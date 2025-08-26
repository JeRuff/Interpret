package com.example.interpret.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import kotlin.math.sin
import android.os.Build


class BluetoothAudioService @Inject constructor() {
    private val TAG = "BluetoothAudioService"
    private val AUDIO_ATTRIBUTION_TAG = "interpreter_audio"


    @SuppressLint("ServiceCast")
    fun routeAudioToEarbud(earbud: String, audioData: ByteArray, context: Context? = null) {

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled or not supported")
            return
        }

        // Check A2DP profile
        var isA2dpConnected = false
        context?.let {
            bluetoothAdapter.getProfileProxy(it, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        isA2dpConnected = proxy.connectedDevices.isNotEmpty()
                        Log.d(TAG, "A2DP connected: $isA2dpConnected, Devices: ${proxy.connectedDevices}")
                    }
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {
                    Log.d(TAG, "A2DP disconnected")
                }
            }, BluetoothProfile.A2DP)
        }

        if (!isA2dpConnected) {
            Log.w(TAG, "A2DP profile not connected. Attempting to route audio anyway.")
        }

        // Request audio focus
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            Log.e(TAG, "AudioManager is null, cannot request audio focus")
            return
        }

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> Log.d(TAG, "Audio focus gained")
                    AudioManager.AUDIOFOCUS_LOSS -> Log.w(TAG, "Audio focus lost")
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> Log.w(TAG, "Audio focus lost transiently")
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Log.d(TAG, "Audio focus can duck")
                    else -> Log.w(TAG, "Unknown audio focus change: $focusChange")
                }
            }
            .build()

        val focusResult = audioManager.requestAudioFocus(focusRequest)
        when (focusResult) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> Log.d(TAG, "Audio focus granted for $earbud")
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Log.e(TAG, "Audio focus request failed for $earbud")
                return
            }
            else -> Log.w(TAG, "Unexpected audio focus result: $focusResult for $earbud")
        }

        try {

            val sampleRate = 24000 // Matches Azure Speech SDK output
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()

            // Manipulate audio buffer for left or right channel
            val stereoData = when (earbud) {
                "left" -> createStereoBuffer(audioData, muteRight = true)
                "right" -> createStereoBuffer(audioData, muteLeft = true)
                else -> audioData // Fallback to original data
            }


            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(audioData.size * 2) // 2 bytes per sample for PCM_16BIT
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            Log.d(TAG, "AudioTrack configured for $earbud: ChannelMask=CHANNEL_OUT_STEREO, SampleRate=$sampleRate, Encoding=PCM_16BIT, BufferSize=${stereoData.size}")

            // Wait for playback to complete
            val latch = CountDownLatch(1)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    Log.d(TAG, "Playback marker reached for $earbud")
                    latch.countDown()
                }

                override fun onPeriodicNotification(track: AudioTrack?) {
                    // Not used
                }
            })

            // Set marker at the end of the audio data
            val frameCount = stereoData.size / 4 // 4 bytes per stereo frame (16-bit, 2 channels)
            track.setNotificationMarkerPosition(frameCount)

            track.play()
            track.write(stereoData, 0, stereoData.size)
            Log.d(TAG, "Writing ${stereoData.size} bytes to AudioTrack for $earbud")

            // Wait for playback to complete (with timeout)
            val durationMs = (stereoData.size / (sampleRate * 4.0) * 1000).toLong() + 500 // Add 500ms buffer
            latch.await(durationMs, TimeUnit.MILLISECONDS)

            track.write(stereoData, 0, stereoData.size)
            track.flush()
            track.stop()
            track.release()

            // Abandon audio focus
            audioManager.abandonAudioFocusRequest(focusRequest)
            Log.d(TAG, "Audio routed to $earbud earbud successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error routing audio to $earbud earbud: ${e.message}", e)
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
    }

    // Test function to play a sample tone to a specific earbud
    fun testEarbudChannel(earbud: String, context: Context? = null) {
        val sampleRate = 24000
        val duration = 1.0 // 1 second
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        // Generate a simple tone (440Hz sine wave)
        for (i in buffer.indices) {
            buffer[i] = (sin(2.0 * Math.PI * i / (sampleRate / 440.0)) * 32767).toInt().toShort()
        }
        val audioData = buffer.toByteArray()

        routeAudioToEarbud(earbud, audioData, context)
        Log.d(TAG, "Test tone sent to $earbud earbud")
    }

    // Create a stereo buffer, muting one channel if needed

    // Create a stereo buffer, muting one channel if needed
    private fun createStereoBuffer(monoData: ByteArray, muteLeft: Boolean = false, muteRight: Boolean = false): ByteArray {
        if (monoData.isEmpty()) {
            Log.e(TAG, "Mono data is empty, returning empty stereo buffer")
            return ByteArray(0)
        }

        if (monoData.size % 2 != 0) {
            Log.w(TAG, "Mono data size ${monoData.size} is not even, padding with zero")
            val paddedData = monoData.copyOf(monoData.size + 1)
            paddedData[monoData.size] = 0
            return createStereoBuffer(paddedData, muteLeft, muteRight)
        }

        val stereoData = ByteArray(monoData.size * 2)
        for (i in monoData.indices step 2) {
            val sample = (monoData[i].toInt() and 0xFF) or (monoData[i + 1].toInt() shl 8)
            val sampleLeft = if (muteLeft) 0 else sample
            val sampleRight = if (muteRight) 0 else sample
            // Left channel
            stereoData[i * 2] = (sampleLeft and 0xFF).toByte()
            stereoData[i * 2 + 1] = (sampleLeft shr 8).toByte()
            // Right channel
            stereoData[i * 2 + 2] = (sampleRight and 0xFF).toByte()
            stereoData[i * 2 + 3] = (sampleRight shr 8).toByte()
        }

        Log.d(TAG, "Created stereo buffer: inputSize=${monoData.size}, outputSize=${stereoData.size}, muteLeft=$muteLeft, muteRight=$muteRight")
        // Validate buffer contents
        val sampleCount = stereoData.size / 4
        if (sampleCount > 0) {
            val leftSample = (stereoData[0].toInt() and 0xFF) or (stereoData[1].toInt() shl 8)
            val rightSample = (stereoData[2].toInt() and 0xFF) or (stereoData[3].toInt() shl 8)
            Log.d(TAG, "First sample: left=$leftSample (should be ${if (muteLeft) 0 else "non-zero"}), right=$rightSample (should be ${if (muteRight) 0 else "non-zero"})")
        }

        return stereoData
    }
}

// Extension to convert ShortArray to ByteArray for PCM_16BIT
private fun ShortArray.toByteArray(): ByteArray {
    val byteArray = ByteArray(size * 2)
    for (i in indices) {
        byteArray[i * 2] = (this[i].toInt() and 0xff).toByte()
        byteArray[i * 2 + 1] = (this[i].toInt() shr 8).toByte()
    }
    return byteArray
}

        /*if (audioManager.isEnabled) {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(24000)
                        .setChannelMask(if (language == "fr-FR") AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(audioData.size)
                .build()

            track.play()
            track.write(audioData, 0, audioData.size)
            track.stop()
            track.release()*/