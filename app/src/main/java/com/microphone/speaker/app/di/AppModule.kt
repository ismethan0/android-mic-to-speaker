package com.microphone.speaker.app.di

import com.microphone.speaker.app.repository.AudioRepository
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
    fun provideAudioRepository(): AudioRepository {
        return AudioRepository()
    }
}