package com.chicken.goldroad.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chicken.goldroad.domain.GameEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class GameViewModel @Inject constructor(val gameEngine: GameEngine) : ViewModel() {

    val gameState = gameEngine.gameState
    private var gameLoopRunning = false

    fun startLevel(
            width: Int,
            height: Int,
            bgBitmaps: List<android.graphics.Bitmap>,
            next: Boolean = false
    ) {
        val currentLevel = gameEngine.gameState.value.level
        val targetLevel = if (next) currentLevel + 1 else currentLevel
        gameEngine.initLevel(width, height, targetLevel, bgBitmaps)
        startGameLoop()
    }

    private fun startGameLoop() {
        if (gameLoopRunning) return
        gameLoopRunning = true
        viewModelScope.launch {
            while (isActive) {
                gameEngine.update() // ~60 FPS
                delay(16)
            }
        }
    }

    fun pauseGame() {
        gameEngine.pause()
    }

    fun resumeGame() {
        gameEngine.resume()
    }

    override fun onCleared() {
        super.onCleared()
        // clean up
    }
}
