package com.chicken.goldroad.data

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.chicken.goldroad.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundManager: SoundManager
) {
    private var musicPlayer: MediaPlayer? = null
    private var currentTrack: Int? = null
    private var musicEnabled: Boolean = true
    private var soundEnabled: Boolean = true
    private var diggingActive = false

    fun applyToggles(musicOn: Boolean, soundOn: Boolean) {
        setMusicEnabled(musicOn)
        setSoundEnabled(soundOn)
    }

    fun playMenuMusic() {
        playMusic(R.raw.menu_music)
    }

    fun playGameMusic() {
        playMusic(R.raw.game_loop)
    }

    fun pause() {
        musicPlayer?.pause()
    }

    fun resume() {
        if (musicEnabled) {
            musicPlayer?.start()
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) {
            musicPlayer?.pause()
        } else if (currentTrack != null && musicPlayer?.isPlaying != true) {
            musicPlayer?.start()
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        soundManager.setMuted(!enabled)
        if (!enabled) {
            stopDiggingSound()
        }
    }

    fun playClick() {
        if (!soundEnabled) return
        soundManager.playSound(R.raw.sfx_digging_repeat_sound)
    }

    fun startDiggingSound() {
        if (!soundEnabled || diggingActive) return
        diggingActive = true
        soundManager.playLooping(R.raw.sfx_digging_repeat_sound)
    }

    fun stopDiggingSound() {
        diggingActive = false
        soundManager.stopLooping(R.raw.sfx_digging_repeat_sound)
    }

    fun playWinSound() {
        if (!soundEnabled) return
        soundManager.playSound(R.raw.sfx_win)
    }

    fun playLoseSound() {
        if (!soundEnabled) return
        soundManager.playSound(R.raw.sfx_lose)
    }

    fun release() {
        musicPlayer?.release()
        musicPlayer = null
        soundManager.stopLooping(R.raw.sfx_digging_repeat_sound)
    }

    private fun playMusic(@RawRes track: Int) {
        if (currentTrack == track && musicPlayer != null) {
            if (musicEnabled && musicPlayer?.isPlaying != true) {
                musicPlayer?.start()
            }
            return
        }
        musicPlayer?.release()
        currentTrack = track
        musicPlayer = MediaPlayer.create(context, track).apply {
            isLooping = true
            if (musicEnabled) {
                start()
            }
        }
    }
}
