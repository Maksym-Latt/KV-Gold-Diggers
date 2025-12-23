package com.chicken.goldroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chicken.goldroad.data.AudioController
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
    private val audioController: AudioController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerPreferences())
    val uiState: StateFlow<PlayerPreferences> = _uiState

    init {
        viewModelScope.launch {
            repository.playerState.collectLatest { prefs ->
                _uiState.value = prefs
                audioController.applyToggles(prefs.musicEnabled, prefs.soundEnabled)
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
            audioController.setMusicEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
            audioController.setSoundEnabled(enabled)
        }
    }

    fun setCurrentLevel(level: Int) {
        viewModelScope.launch { repository.setCurrentLevel(level) }
    }

    fun playMenuMusic() {
        audioController.playMenuMusic()
    }

    fun playGameMusic() {
        audioController.playGameMusic()
    }

    fun pauseAudio() {
        audioController.pause()
    }

    fun resumeAudio() {
        audioController.resume()
    }

    fun playClick() {
        audioController.playClick()
    }

    fun startDiggingSound() {
        audioController.startDiggingSound()
    }

    fun stopDiggingSound() {
        audioController.stopDiggingSound()
    }

    fun playWinSound() {
        audioController.playWinSound()
    }

    fun playLoseSound() {
        audioController.playLoseSound()
    }
}
