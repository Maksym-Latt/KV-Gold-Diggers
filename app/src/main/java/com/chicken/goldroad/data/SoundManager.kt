package com.chicken.goldroad.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<Int, Int>()
    private var isMuted = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    fun loadSound(resId: Int) {
        if (!soundMap.containsKey(resId)) {
            soundMap[resId] = soundPool.load(context, resId, 1)
        }
    }

    fun playSound(resId: Int) {
        if (isMuted) return
        val soundId = soundMap[resId] ?: return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun pauseForLifecycle() {
        soundPool.autoPause()
    }

    fun resumeAfterLifecycle() {
        soundPool.autoResume()
    }

    fun release() {
        soundPool.release()
    }
}
