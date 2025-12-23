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
    private val loopingStreams = mutableMapOf<Int, Int>()
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

    private fun loadSound(resId: Int) {
        if (!soundMap.containsKey(resId)) {
            soundMap[resId] = soundPool.load(context, resId, 1)
        }
    }

    fun playSound(resId: Int) {
        playSoundInternal(resId, loop = 0)
    }

    fun playLooping(resId: Int) {
        val streamId = playSoundInternal(resId, loop = -1)
        if (streamId != 0) {
            loopingStreams[resId] = streamId
        }
    }

    fun stopLooping(resId: Int) {
        loopingStreams.remove(resId)?.let { soundPool.stop(it) }
    }

    private fun playSoundInternal(resId: Int, loop: Int): Int {
        if (isMuted) return 0
        loadSound(resId)
        val soundId = soundMap[resId] ?: return 0
        return soundPool.play(soundId, 1f, 1f, 1, loop, 1f)
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            loopingStreams.values.forEach { soundPool.stop(it) }
            loopingStreams.clear()
        }
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
