package com.knowbody.interpret.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject

class BluetoothAudioService @Inject constructor() {
    companion object {
        private const val TAG = "BluetoothAudioService"
        private const val SAMPLE_RATE = 16000 // 16kHz for Azure audio
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO // Stereo for earbud routing
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var bluetoothHeadset: BluetoothHeadset? = null
    private var audioManager: AudioManager? = null
    private var isBluetoothInitialized = false
    private val audioLock = ReentrantLock()

    // Explicit implementation of BluetoothProfile.ServiceListener
    private class BluetoothServiceListener(
        private val onConnected: (BluetoothHeadset) -> Unit,
        private val onDisconnected: () -> Unit
    ) : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothHeadset.HEADSET && proxy is BluetoothHeadset) {
                onConnected(proxy)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothHeadset.HEADSET) {
                onDisconnected()
            }
        }
    }

    fun initializeBluetooth(context: Context) {
        try {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                bluetoothAdapter.getProfileProxy(
                    context,
                    BluetoothServiceListener(
                        onConnected = { headset ->
                            bluetoothHeadset = headset
                            isBluetoothInitialized = true
                            audioManager?.startBluetoothSco()
                            audioManager?.isBluetoothScoOn = true
                            Log.d(TAG, "BluetoothHeadset connected")
                        },
                        onDisconnected = {
                            bluetoothHeadset = null
                            isBluetoothInitialized = false
                            audioManager?.stopBluetoothSco()
                            audioManager?.isBluetoothScoOn = false
                            Log.d(TAG, "BluetoothHeadset disconnected")
                        }
                    ),
                    BluetoothHeadset.HEADSET
                )
            } else {
                Log.w(TAG, "Bluetooth not available or disabled")
                isBluetoothInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Bluetooth", e)
            isBluetoothInitialized = false
        }
    }

    fun routeAudioToEarbud(earbud: String, audioData: ByteArray, context: Context) {
        audioLock.lock()
        try {
            Log.d(TAG, "Routing audio to $earbud, size: ${audioData.size} bytes")
            initializeBluetooth(context)

            // Convert mono audio to stereo, placing audio in the correct channel
            val stereoAudio = convertMonoToStereo(audioData, earbud)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .setEncoding(AUDIO_FORMAT)
                .build()

            val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            // Ensure Bluetooth SCO is active if Bluetooth is connected
            if (isBluetoothInitialized && bluetoothHeadset != null) {
                audioManager?.startBluetoothSco()
                audioManager?.isBluetoothScoOn = true
                Log.d(TAG, "Bluetooth SCO enabled for $earbud")
            } else {
                Log.w(TAG, "Bluetooth not initialized, using device speaker for $earbud")
            }

            audioTrack.play()
            audioTrack.write(stereoAudio, 0, stereoAudio.size)
            audioTrack.stop()
            audioTrack.release()

            // Clean up Bluetooth SCO after playback
            if (isBluetoothInitialized && bluetoothHeadset != null) {
                audioManager?.stopBluetoothSco()
                audioManager?.isBluetoothScoOn = false
                Log.d(TAG, "Bluetooth SCO stopped after playback")
            }

            Log.d(TAG, "Audio played for $earbud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to route audio to $earbud", e)
            throw e
        } finally {
            audioLock.unlock()
        }
    }

    private fun convertMonoToStereo(monoAudio: ByteArray, earbud: String): ByteArray {
        // Azure audio is mono (16-bit PCM, 1 channel). Convert to stereo (2 channels).
        val stereoAudio = ByteArray(monoAudio.size * 2)
        for (i in monoAudio.indices step 2) {
            val sample = (monoAudio[i].toInt() and 0xFF) or (monoAudio[i + 1].toInt() shl 8)
            val leftSample = if (earbud == "left") sample else 0
            val rightSample = if (earbud == "right") sample else 0
            // Left channel
            stereoAudio[i * 2] = (leftSample and 0xFF).toByte()
            stereoAudio[i * 2 + 1] = ((leftSample shr 8) and 0xFF).toByte()
            // Right channel
            stereoAudio[i * 2 + 2] = (rightSample and 0xFF).toByte()
            stereoAudio[i * 2 + 3] = ((rightSample shr 8) and 0xFF).toByte()
        }
        Log.d(TAG, "Converted mono to stereo for $earbud: leftSample=${stereoAudio[0]}, rightSample=${stereoAudio[2]}")
        return stereoAudio
    }

    fun testEarbudChannel(earbud: String, context: Context) {
        audioLock.lock()
        try {
            Log.d(TAG, "Testing audio channel for $earbud")
            initializeBluetooth(context)

            // Generate a 1-second 440Hz sine wave for testing
            val sampleRate = SAMPLE_RATE
            val numSamples = sampleRate
            val monoAudio = ByteArray(numSamples * 2) // 16-bit PCM
            for (i in 0 until numSamples) {
                val sample = (Math.sin(2.0 * Math.PI * i / (sampleRate / 440.0)) * 0.5 * Short.MAX_VALUE).toInt()
                monoAudio[i * 2] = (sample and 0xFF).toByte()
                monoAudio[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
            }

            // Convert to stereo for the specified earbud
            val stereoAudio = convertMonoToStereo(monoAudio, earbud)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .setEncoding(AUDIO_FORMAT)
                .build()

            val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            // Ensure Bluetooth SCO is active if Bluetooth is connected
            if (isBluetoothInitialized && bluetoothHeadset != null) {
                audioManager?.startBluetoothSco()
                audioManager?.isBluetoothScoOn = true
                Log.d(TAG, "Bluetooth SCO enabled for test $earbud")
            } else {
                Log.w(TAG, "Bluetooth not initialized, using device speaker for test $earbud")
            }

            audioTrack.play()
            audioTrack.write(stereoAudio, 0, stereoAudio.size)
            audioTrack.stop()
            audioTrack.release()

            // Clean up Bluetooth SCO
            if (isBluetoothInitialized && bluetoothHeadset != null) {
                audioManager?.stopBluetoothSco()
                audioManager?.isBluetoothScoOn = false
                Log.d(TAG, "Bluetooth SCO stopped after test")
            }

            Log.d(TAG, "Test audio played for $earbud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to test audio channel for $earbud", e)
            throw e
        } finally {
            audioLock.unlock()
        }
    }

    fun cleanup() {
        try {
            bluetoothHeadset?.let {
                audioManager?.stopBluetoothSco()
                audioManager?.isBluetoothScoOn = false
                BluetoothAdapter.getDefaultAdapter()?.closeProfileProxy(BluetoothHeadset.HEADSET, it)
                bluetoothHeadset = null
                isBluetoothInitialized = false
                Log.d(TAG, "Bluetooth cleaned up")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up Bluetooth", e)
        }
    }
}