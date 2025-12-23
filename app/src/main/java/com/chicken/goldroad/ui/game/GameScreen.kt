package com.chicken.goldroad.ui.game

import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay

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

    val basketBitmap =
            remember(selectedBasket.id) {
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

    val infiniteTransition = rememberInfiniteTransition(label = "honeyWave")
    val wavePhase by
            infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                            ),
                    label = "wavePhase"
            )

    // Quantize phase to reduce path recalculations (roughly "every other frame")
    val optimizedPhase = (wavePhase * 15f).toInt() / 15f

    var showFinalResults by remember { mutableStateOf(false) }
    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.WON || gameState.status == GameStatus.LOST) {
            delay(2000) // Match honey expansion duration
            showFinalResults = true
        } else {
            showFinalResults = false
        }
    }

    var processedStatus by remember { mutableStateOf<GameStatus?>(null) }
    LaunchedEffect(gameState.status) {
        val status = gameState.status
        if (status == GameStatus.PLAYING) {
            processedStatus = null
        }
        if (status != processedStatus && (status == GameStatus.WON || status == GameStatus.LOST)) {
            val reward =
                    if (status == GameStatus.WON) {
                        max(gameState.score, gameState.targetScore / 2)
                    } else {
                        max(gameState.score / 2, 8)
                    }
            playerViewModel.addCoins(reward)
            processedStatus = status
        }
    }

    val honeyExpansion by
            animateFloatAsState(
                    targetValue =
                            if (gameState.status == GameStatus.WON ||
                                            gameState.status == GameStatus.LOST
                            )
                                    1f
                            else 0.12f,
                    animationSpec = tween(durationMillis = 2000),
                    label = "honeyExpansion"
            )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
                modifier =
                        Modifier.fillMaxSize()
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
                                        val end = change.position + Offset(0f, cameraY)
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

        HoneyOverlay(expansion = honeyExpansion, wavePhase = optimizedPhase)

        TopHud(
                score = gameState.collectedEggs,
                target = gameState.targetScore,
                coins = playerPreferences.coins,
                onPause = {
                    viewModel.pauseGame()
                    playerViewModel.pauseAudio()
                }
        )

        gameState.countdownSeconds?.let { seconds ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                        color = Color(0xAA000000),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 200.dp)
                ) {
                    Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StrokedText(text = "TIME LEFT", fontSize = 20.sp, color = Color.White)
                        StrokedText(text = "$seconds", fontSize = 64.sp, color = Color(0xFFF7D26B))
                    }
                }
            }
        }

        if (gameState.status == GameStatus.PAUSED) {
            PauseOverlay(
                    musicEnabled = playerPreferences.musicEnabled,
                    soundEnabled = playerPreferences.soundEnabled,
                    onToggleMusic = {
                        playerViewModel.setMusicEnabled(!playerPreferences.musicEnabled)
                    },
                    onToggleSound = {
                        playerViewModel.setSoundEnabled(!playerPreferences.soundEnabled)
                    },
                    onResume = {
                        viewModel.resumeGame()
                        playerViewModel.resumeAudio()
                    },
                    onHome = {
                        viewModel.resumeGame()
                        onBack()
                    },
                    onRestart = {},
            )
        }

        if (showFinalResults && gameState.status == GameStatus.WON) {
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
        } else if (showFinalResults && gameState.status == GameStatus.LOST) {
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
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(16.dp)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = Color(0x99000000), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StrokedText(
                        text = "Score",
                        color = Color.White,
                        strokeColor = Color.Black,
                        strokeWidth = 4f,
                        fontSize = 14.sp
                )
                StrokedText(
                        text = "$score / $target",
                        color = Color.White,
                        strokeColor = Color.Black,
                        strokeWidth = 4f,
                        fontSize = 16.sp
                )
            }
        }
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoundIconButton(
                    icon = rememberVectorPainter(Icons.Default.Pause),
                    modifier = Modifier.size(56.dp),
                    onClick = onPause
            )
        }
    }
}

@Composable
fun HoneyOverlay(expansion: Float, wavePhase: Float) {
    val context = LocalContext.current
    val drips = remember {
        listOf(
                0.15f to 1.2f, // widthRatio to depthMultiplier
                0.25f to 0.8f,
                0.20f to 1.5f,
                0.22f to 1.1f,
                0.18f to 0.9f
        )
    }

    val honeyColor = Color(0xffebb236)
    val honeyEdgeColor = Color(0xffc9922f)
    val edgeThickness = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }

    // Reuse Path objects to avoid allocations during drawing
    val bodyPath = remember { androidx.compose.ui.graphics.Path() }
    val edgePath = remember { androidx.compose.ui.graphics.Path() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val h = height * expansion

        fun updateHoneyPath(path: androidx.compose.ui.graphics.Path, offsetY: Float) {
            path.reset()
            path.moveTo(0f, 0f)
            path.lineTo(width, 0f)
            path.lineTo(width, h + offsetY)

            var currentX = width
            drips.forEachIndexed { i, (wRatio, dMult) ->
                val waveSegmentWidth = widцth * wRatio
                val targetX = currentX - waveSegmentWidth

                if (expansion < 0.95f) {
                    val phaseOffset = i * 0.7f
                    val waveDepth =
                            (30.dp.toPx() * dMult) +
                                    (kotlin.math.sin(wavePhase + phaseOffset) * 12.dp.toPx())

                    path.relativeQuadraticBezierTo(
                            -waveSegmentWidth / 4f,
                            waveDepth,
                            -waveSegmentWidth / 2f,
                            0f
                    )
                    path.relativeQuadraticBezierTo(
                            -waveSegmentWidth / 4f,
                            -waveDepth / 2f,
                            -waveSegmentWidth / 2f,
                            0f
                    )
                } else {
                    path.lineTo(targetX, h + offsetY)
                }
                currentX = targetX
            }
            path.lineTo(0f, 0f)
            path.close()
        }

        // 1. Draw edge (shadow) first (only if needed)
        if (expansion < 0.98f) {
            updateHoneyPath(edgePath, edgeThickness)
            drawPath(path = edgePath, color = honeyEdgeColor)
        }

        // 2. Draw main body once
        updateHoneyPath(bodyPath, 0f)
        drawPath(path = bodyPath, color = honeyColor)
    }
}
