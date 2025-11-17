package com.knowbody.interpret.di

import com.knowbody.interpret.BuildConfig
import com.knowbody.interpret.service.AzureSpeechService
import com.knowbody.interpret.service.BluetoothAudioService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAzureSpeechService(): AzureSpeechService {
        return AzureSpeechService(
            speechKey = BuildConfig.SPEECH_KEY,
            speechRegion = BuildConfig.SPEECH_REGION
        )
    }

    @Provides
    @Singleton
    fun provideBluetoothAudioService(@ApplicationContext context: Context): BluetoothAudioService {
        return BluetoothAudioService(context)
    }
}