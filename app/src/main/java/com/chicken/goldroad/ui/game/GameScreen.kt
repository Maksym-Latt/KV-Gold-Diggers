package com.chicken.goldroad.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chicken.goldroad.R
import com.chicken.goldroad.domain.GameStatus

@Composable
fun GameScreen(viewModel: GameViewModel = hiltViewModel(), onBack: () -> Unit) {
    val gameState by viewModel.gameState.collectAsState()
    var screenSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var redrawTrigger by remember { mutableStateOf(0L) }

    val context = LocalContext.current

    // Watch frameTick and trigger redraw
    LaunchedEffect(gameState.frameTick) { redrawTrigger = gameState.frameTick }

    // Load Egg Bitmaps once
    val eggBitmaps = remember {
        val eggs =
                listOf(
                        R.drawable.egg_1,
                        R.drawable.egg_2,
                        R.drawable.egg_3,
                        R.drawable.egg_4,
                        R.drawable.egg_5
                )
        eggs.map { id -> android.graphics.BitmapFactory.decodeResource(context.resources, id) }
    }

    // Load Basket Bitmap
    val basketBitmap = remember {
        android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.basket_1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
                modifier =
                        Modifier.fillMaxSize()
                                .onSizeChanged {
                                    if (screenSize == null) {
                                        screenSize = it.width to it.height
                                        viewModel.startGame(it.width, it.height)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val cameraY = viewModel.gameEngine.cameraY
                                        val start =
                                                change.position - dragAmount +
                                                        androidx.compose.ui.geometry.Offset(
                                                                0f,
                                                                cameraY
                                                        )
                                        val end =
                                                change.position +
                                                        androidx.compose.ui.geometry.Offset(
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

                viewModel.gameEngine.terrainBitmap?.let {
                    nativeCanvas.drawBitmap(it, 0f, 0f, null)
                }

                val basketRect = viewModel.gameEngine.basketRect
                nativeCanvas.drawBitmap(basketBitmap, null, basketRect, null)

                val eggs = gameState.eggs
                val eggRadius = viewModel.gameEngine.eggRadius

                eggs.forEach { egg ->
                    // Optimization: only draw if visible
                    if (egg.y + eggRadius < cameraY ||
                                    egg.y - eggRadius >
                                            cameraY + (screenSize?.second?.toFloat() ?: 0f)
                    )
                            return@forEach

                    val bitmap = eggBitmaps.getOrElse(egg.type - 1) { eggBitmaps[0] }

                    nativeCanvas.save()
                    nativeCanvas.rotate(egg.angle, egg.x, egg.y)

                    val destRect =
                            android.graphics.RectF(
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

        // HUD
        Text(
                text = "Score: ${gameState.score} / ${gameState.targetScore}",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )

        // Overlays
        if (gameState.status == GameStatus.WON) {
            Overlay(
                    title = "Perfect Flow!",
                    msg = "+${gameState.score} Points",
                    color = Color(0xFF4CAF50),
                    onMain = onBack,
                    onAction = { /* Next Level logic */}
            )
        } else if (gameState.status == GameStatus.LOST) {
            Overlay(
                    title = "Not this time!",
                    msg = "Try again",
                    color = Color(0xFFFF9800),
                    onMain = onBack,
                    onAction = { screenSize?.let { viewModel.startGame(it.first, it.second) } }
            )
        }
    }
}

@Composable
fun Overlay(title: String, msg: String, color: Color, onMain: () -> Unit, onAction: () -> Unit) {
    Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.background(color).padding(32.dp)
        ) {
            Text(title, fontSize = 32.sp, color = Color.White)
            Text(msg, fontSize = 24.sp, color = Color.White)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = onAction) { Text("Action") }
            Button(
                    onClick = onMain,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Home") }
        }
    }
}
