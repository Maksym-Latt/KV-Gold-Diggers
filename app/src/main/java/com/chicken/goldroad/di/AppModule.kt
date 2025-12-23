package com.chicken.goldroad.di

import android.content.Context
import com.chicken.goldroad.data.AudioEngine
import com.chicken.goldroad.data.PlayerDataRepository
import com.chicken.goldroad.data.SoundManager
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
    fun provideSoundManager(@ApplicationContext context: Context): SoundManager {
        return SoundManager(context)
    }

    @Provides
    @Singleton
    fun providePlayerRepository(@ApplicationContext context: Context): PlayerDataRepository {
        return PlayerDataRepository(context)
    }

    @Provides
    @Singleton
    fun provideAudioController(
        @ApplicationContext context: Context,
        soundManager: SoundManager
    ): AudioEngine {
        return AudioEngine(context, soundManager)
    }
}
