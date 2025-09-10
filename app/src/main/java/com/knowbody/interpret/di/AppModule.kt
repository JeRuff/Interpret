package com.knowbody.interpret.di

import android.content.Context
import com.knowbody.interpret.service.AzureSpeechService
import com.knowbody.interpret.service.BluetoothAudioService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAzureSpeechService(@ApplicationContext context: Context): AzureSpeechService {
        return AzureSpeechService(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothAudioService(@ApplicationContext context: Context): BluetoothAudioService {
        return BluetoothAudioService(context)
    }
}