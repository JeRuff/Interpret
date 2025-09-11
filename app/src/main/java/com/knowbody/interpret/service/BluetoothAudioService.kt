package com.knowbody.interpret.service

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import androidx.core.content.ContextCompat
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer

class BluetoothAudioService(private val context: Context) {
    private val TAG = "BluetoothAudioService"

    fun testEarbudChannel(earbud: String, context: Context) {
        Log.d(TAG, "Testing $earbud earbud")
        // Play a test tone (440 Hz, 1 second) on the device speaker as a fallback
        val sampleRate = 24000
        val duration = 1.0 // 1 second
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        for (i in buffer.indices) {
            buffer[i] = (Math.sin(2 * Math.PI * i / (sampleRate / 440.0)) * Short.MAX_VALUE).toInt().toShort()
        }

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            numSamples * 2,
            AudioTrack.MODE_STATIC
        )
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        Log.d(TAG, "Test tone played for $earbud")
    }

    fun routeAudioToEarbud(earbud: String, audioData: ByteArray, context: Context) {
        Log.d(TAG, "Routing audio to $earbud, audioData size=${audioData.size} bytes")
        // Fallback: Play audio on device speaker
        try {
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                24000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioData.size,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(audioData, 0, audioData.size)
            audioTrack.play()
            Log.d(TAG, "Audio played for $earbud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio for $earbud", e)
        }
    }
}