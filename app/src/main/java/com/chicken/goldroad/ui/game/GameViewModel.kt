package com.chicken.goldroad.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chicken.goldroad.domain.GameEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.graphics.BitmapFactory
import android.content.Context
import com.chicken.goldroad.R
import com.chicken.goldroad.domain.model.BasketType
import com.chicken.goldroad.data.PlayerPreferences
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class GameViewModel @Inject constructor(val gameEngine: GameEngine) : ViewModel() {

    val gameState = gameEngine.gameState
    private var gameLoopRunning = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _assets = MutableStateFlow<GameAssets?>(null)
    val assets = _assets.asStateFlow()

    data class GameAssets(
        val eggBitmaps: List<android.graphics.Bitmap>,
        val basketBitmap: android.graphics.Bitmap,
        val bgGroundBitmaps: List<android.graphics.Bitmap>
    )

    fun startLevel(
            context: Context,
            playerPreferences: PlayerPreferences,
            width: Int,
            height: Int,
            next: Boolean = false,
            startPaused: Boolean = true
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            
            // 1. Decode assets if not already loaded or if basket changed
            val basketType = BasketType.fromId(playerPreferences.selectedBasketId)
            if (_assets.value == null || _assets.value?.basketBitmap == null) {
                val eggs = listOf(R.drawable.egg_1, R.drawable.egg_2, R.drawable.egg_3, R.drawable.egg_4, R.drawable.egg_5)
                        .map { BitmapFactory.decodeResource(context.resources, it) }
                
                val basket = BitmapFactory.decodeResource(context.resources, basketType.imageRes)
                
                val bgs = listOf(R.drawable.bg_ground_1, R.drawable.bg_ground_2, R.drawable.bg_ground_3, R.drawable.bg_ground_4, R.drawable.bg_ground_5, R.drawable.bg_ground_6)
                        .map { BitmapFactory.decodeResource(context.resources, it) }
                
                _assets.value = GameAssets(eggs, basket, bgs)
            }

            // 2. Initialize level
            val currentLevel = gameEngine.gameState.value.level
            val targetLevel = if (next) currentLevel + 1 else currentLevel
            gameEngine.initLevel(width, height, targetLevel, _assets.value?.bgGroundBitmaps ?: emptyList())
            if (startPaused) {
                gameEngine.pause()
            } else {
                gameEngine.resume()
            }
            
            _isLoading.value = false
            startGameLoop()
        }
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
