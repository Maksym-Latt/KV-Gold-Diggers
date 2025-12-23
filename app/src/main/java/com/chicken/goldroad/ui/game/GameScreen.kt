package com.chicken.goldroad.ui.game

import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chicken.goldroad.R
import com.chicken.goldroad.data.PlayerPreferences
import com.chicken.goldroad.domain.GameStatus
import com.chicken.goldroad.domain.model.BasketType
import com.chicken.goldroad.ui.PlayerViewModel
import com.chicken.goldroad.ui.components.RoundIconButton
import com.chicken.goldroad.ui.components.StrokedText
import com.chicken.goldroad.ui.overlay.PauseOverlay
import com.chicken.goldroad.ui.overlay.ResultOverlay
import kotlin.math.max

@Composable
fun GameScreen(
    playerPreferences: PlayerPreferences,
    playerViewModel: PlayerViewModel,
    viewModel: GameViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    var screenSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var redrawTrigger by remember { mutableStateOf(0L) }

    val context = LocalContext.current

    LaunchedEffect(Unit) { playerViewModel.playGameMusic() }

    val selectedBasket = BasketType.fromId(playerPreferences.selectedBasketId)

    val eggBitmaps = remember {
        val eggs =
            listOf(
                R.drawable.egg_1,
                R.drawable.egg_2,
                R.drawable.egg_3,
                R.drawable.egg_4,
                R.drawable.egg_5
            )
        eggs.map { id -> BitmapFactory.decodeResource(context.resources, id) }
    }

    val basketBitmap = remember(selectedBasket.id) {
        BitmapFactory.decodeResource(context.resources, selectedBasket.imageRes)
    }

    val bgGroundBitmaps = remember {
        listOf(
            R.drawable.bg_ground_1,
            R.drawable.bg_ground_2,
            R.drawable.bg_ground_3,
            R.drawable.bg_ground_4,
            R.drawable.bg_ground_5,
            R.drawable.bg_ground_6
        )
            .map { id -> BitmapFactory.decodeResource(context.resources, id) }
    }

    LaunchedEffect(gameState.frameTick) { redrawTrigger = gameState.frameTick }

    var processedStatus by remember { mutableStateOf<GameStatus?>(null) }
    LaunchedEffect(gameState.status) {
        val status = gameState.status
        if (status == GameStatus.PLAYING) {
            processedStatus = null
        }
        if (status != processedStatus && (status == GameStatus.WON || status == GameStatus.LOST)) {
            val reward = if (status == GameStatus.WON) {
                max(gameState.score, gameState.targetScore / 2)
            } else {
                max(gameState.score / 2, 8)
            }
            playerViewModel.addCoins(reward)
            processedStatus = status
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged {
                    if (screenSize == null) {
                        screenSize = it.width to it.height
                        viewModel.startLevel(
                            it.width,
                            it.height,
                            bgGroundBitmaps,
                            next = false
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val cameraY = viewModel.gameEngine.cameraY
                        val start =
                            change.position - dragAmount + Offset(0f, cameraY)
                        val end =
                            change.position +
                                    Offset(
                                        0f,
                                        cameraY
                                    )
                        viewModel.gameEngine.dig(start, end)
                    }
                }
        ) {
            val _key = redrawTrigger
            val cameraY = viewModel.gameEngine.cameraY

            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas

                nativeCanvas.save()
                nativeCanvas.translate(0f, -cameraY)

                val screenH = screenSize?.second?.toFloat() ?: 0f
                viewModel.gameEngine.terrainChunks.forEach { chunk ->
                    if (chunk.topY + chunk.height > cameraY && chunk.topY < cameraY + screenH) {
                        nativeCanvas.drawBitmap(chunk.bitmap, 0f, chunk.topY, null)
                    }
                }

                val basketRect = viewModel.gameEngine.basketRect
                nativeCanvas.drawBitmap(basketBitmap, null, basketRect, null)

                val eggs = gameState.eggs
                val eggRadius = viewModel.gameEngine.eggRadius

                eggs.forEach { egg ->
                    if (egg.y + eggRadius < cameraY ||
                        egg.y - eggRadius >
                        cameraY + (screenSize?.second?.toFloat() ?: 0f)
                    )
                        return@forEach

                    val bitmap = eggBitmaps.getOrElse(egg.type - 1) { eggBitmaps[0] }

                    nativeCanvas.save()
                    nativeCanvas.rotate(egg.angle, egg.x, egg.y)

                    val destRect =
                        RectF(
                            egg.x - eggRadius,
                            egg.y - eggRadius,
                            egg.x + eggRadius,
                            egg.y + eggRadius
                        )
                    nativeCanvas.drawBitmap(bitmap, null, destRect, null)

                    nativeCanvas.restore()
                }

                nativeCanvas.restore()
            }
        }

        TopHud(
            score = gameState.score,
            target = gameState.targetScore,
            coins = playerPreferences.coins,
            onPause = {
                viewModel.pauseGame()
                playerViewModel.pauseAudio()
            }
        )

        if (gameState.status == GameStatus.PAUSED) {
            PauseOverlay(
                musicEnabled = playerPreferences.musicEnabled,
                soundEnabled = playerPreferences.soundEnabled,
                onToggleMusic = { playerViewModel.setMusicEnabled(!playerPreferences.musicEnabled) },
                onToggleSound = { playerViewModel.setSoundEnabled(!playerPreferences.soundEnabled) },
                onResume = {
                    viewModel.resumeGame()
                    playerViewModel.resumeAudio()
                },
                onHome = {
                    viewModel.resumeGame()
                    onBack()
                },
                onRestart = {  },
            )
        }

        if (gameState.status == GameStatus.WON) {
            ResultOverlay(
                title = "Perfect Flow!",
                rewardText = "+${max(gameState.score, gameState.targetScore / 2)}",
                outerGradient = listOf(Color(0xFF7EDB6C), Color(0xFFECCF2A)),
                panelColor = Color(0xFFE7B735),
                characterImage = R.drawable.chicken_win,
                basketImage = selectedBasket.imageRes,
                onPrimary = {
                    screenSize?.let {
                        viewModel.startLevel(it.first, it.second, bgGroundBitmaps, next = true)
                    }
                },
                onSecondary = onBack,
                primaryLabel = "Next",
                secondaryLabel = "Home"
            )
        } else if (gameState.status == GameStatus.LOST) {
            ResultOverlay(
                title = "Not this time!",
                rewardText = "+${max(gameState.score / 2, 8)}",
                outerGradient = listOf(Color(0xFFF1A43D), Color(0xFFDE7A22)),
                panelColor = Color(0xFFEEB038),
                characterImage = R.drawable.chicken_lose,
                basketImage = selectedBasket.imageRes,
                onPrimary = {
                    screenSize?.let {
                        viewModel.startLevel(it.first, it.second, bgGroundBitmaps, next = false)
                    }
                },
                onSecondary = onBack,
                primaryLabel = "Try again",
                secondaryLabel = "Home"
            )
        }
    }
}

@Composable
private fun TopHud(score: Int, target: Int, coins: Int, onPause: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp).windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0x99000000),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StrokedText(text = "Score", color = Color.White, strokeColor = Color.Black, strokeWidth = 4f, fontSize = 14.sp)
                StrokedText(text = "$score / $target", color = Color.White, strokeColor = Color.Black, strokeWidth = 4f, fontSize = 16.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundIconButton(
                icon = rememberVectorPainter(Icons.Default.Pause),
                modifier = Modifier.size(56.dp),
                onClick = onPause
            )
        }
    }
}
