package com.chicken.goldroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chicken.goldroad.data.AudioEngine
import com.chicken.goldroad.data.PlayerDataRepository
import com.chicken.goldroad.data.PlayerPreferences
import com.chicken.goldroad.domain.model.BasketType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerDataRepository,
    private val audioEngine: AudioEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerPreferences())
    val uiState: StateFlow<PlayerPreferences> = _uiState

    init {
        viewModelScope.launch {
            repository.playerState.collectLatest { prefs ->
                _uiState.value = prefs
                audioEngine.applyToggles(prefs.musicEnabled, prefs.soundEnabled)
            }
        }
    }

    fun addCoins(amount: Int) {
        viewModelScope.launch { repository.addCoins(amount) }
    }

    fun selectBasket(type: BasketType) {
        viewModelScope.launch { repository.selectBasket(type) }
    }

    fun buyAndEquip(type: BasketType) {
        viewModelScope.launch {
            val unlocked = repository.unlockBasket(type)
            if (unlocked) {
                repository.selectBasket(type)
            }
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMusicEnabled(enabled)
            audioEngine.setMusicEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
            audioEngine.setSoundEnabled(enabled)
        }
    }

    fun setCurrentLevel(level: Int) {
        viewModelScope.launch { repository.setCurrentLevel(level) }
    }

    fun playMenuMusic() {
        audioEngine.playMenuMusic()
    }

    fun playGameMusic() {
        audioEngine.playGameMusic()
    }

    fun pauseAudio() {
        audioEngine.pause()
    }

    fun resumeAudio() {
        audioEngine.resume()
    }

    fun playClick() {
        audioEngine.playClick()
    }

    fun startDiggingSound() {
        audioEngine.startDiggingSound()
    }

    fun stopDiggingSound() {
        audioEngine.stopDiggingSound()
    }

    fun playWinSound() {
        audioEngine.playWinSound()
    }

    fun playLoseSound() {
        audioEngine.playLoseSound()
    }
}
