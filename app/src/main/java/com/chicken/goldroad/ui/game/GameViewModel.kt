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

    fun startGame(width: Int, height: Int) {
        if (gameEngine.gameState.value.level == 1 && gameEngine.gameState.value.eggs.isEmpty()) {
            gameEngine.initLevel(width, height, 1)
        }
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

    override fun onCleared() {
        super.onCleared()
        // clean up
    }
}
