package com.example.interpret.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import javax.inject.Inject

class BluetoothAudioService @Inject constructor() {
    fun routeAudioToEarbud(language: String, audioData: ByteArray) {
        val audioManager = BluetoothAdapter.getDefaultAdapter()
        if (audioManager.isEnabled) {
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
                        .setChannelMask(if (language == "fr-FR") AudioFormat.CHANNEL_OUT_LEFT else AudioFormat.CHANNEL_OUT_RIGHT)
                        .build()
                )
                .setBufferSizeInBytes(audioData.size)
                .build()

            track.play()
            track.write(audioData, 0, audioData.size)
            track.stop()
            track.release()
        }
    }
}