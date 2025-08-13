package com.example.interpret.di

import com.example.interpret.service.AzureSpeechService
import com.example.interpret.service.BluetoothAudioService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAzureSpeechService(): AzureSpeechService {
        return AzureSpeechService(
            speechKey = "Bp98ummrHrp32jao0zfJ45KsjDbdkunpTmTliWNeDjBXgWrwX9ZGJQQJ99BHACi5YpzXJ3w3AAAYACOG3eno", // Replace with your Azure Speech Key
            speechRegion = "northeurope" // Replace with your Azure region (e.g., westus)
        )
    }

    @Provides
    @Singleton
    fun provideBluetoothAudioService(): BluetoothAudioService {
        return BluetoothAudioService()
    }
}