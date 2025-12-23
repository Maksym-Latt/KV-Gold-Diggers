package com.chicken.goldroad.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Offset
import com.chicken.goldroad.data.SoundManager
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GameStatus {
    PLAYING,
    PAUSED,
    WON,
    LOST
}

data class TerrainChunk(val topY: Float, val height: Float, val bitmap: Bitmap, val canvas: Canvas)

data class Egg(
        val id: Long,
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var angle: Float = 0f,
        var angularVelocity: Float = 0f,
        val type: Int,
        var isActive: Boolean = true,
        var sleepFrames: Int = 0
)

data class Obstacle(val x: Float, val y: Float, val radius: Float, val type: ObstacleType)

enum class ObstacleType {
    STONE,
    SPIKE,
    HOLE
}

data class GameState(
        val score: Int = 0,
        val collectedEggs: Int = 0,
        val targetScore: Int = 100,
        val status: GameStatus = GameStatus.PLAYING,
        val eggs: List<Egg> = emptyList(),
        val level: Int = 1,
        val frameTick: Long = 0,
        val countdownSeconds: Int? = null
)

@Singleton
class GameEngine @Inject constructor(private val soundManager: SoundManager) {
    companion object {
        const val BASE_TARGET = 20
        const val EGGS_PER_LEVEL = 10
        const val MAX_TARGET_EGGS = 100
        const val MAX_LEVEL = (MAX_TARGET_EGGS - BASE_TARGET) / EGGS_PER_LEVEL
    }

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    var cameraY = 0f
    private var scrollSpeed = 6.0f
    var worldHeight = 0
    private var countdownStarted = false
    private var countdownFrames = 0
    private val countdownTotalFrames = 5 * 60 // 10 seconds at ~60fps

    private var lastDigTime = 0L
    private val digThrottleMs = 16L // Max 60 dig operations per second

    val terrainChunks = mutableListOf<TerrainChunk>()
    private val chunkSize = 1000f // Vertical height of one chunk

    // Optimization: Downsampled ByteArray collision mask
    private val collisionScale = 2
    private var collisionMask: ByteArray? = null
    private var maskWidth = 0
    private var maskHeight = 0

    private val eraserPaint =
            Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                style = Paint.Style.STROKE
                strokeWidth = 80f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

    private val fillEraserPaint =
            Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                style = Paint.Style.FILL
                isAntiAlias = true
            }

    private val movingEggs = CopyOnWriteArrayList<Egg>()
    private val obstacles = CopyOnWriteArrayList<Obstacle>()

    private var screenWidth = 0
    private var screenHeight = 0

    var basketRect = android.graphics.RectF()

    val eggRadius = 15f

    private val gravity = 0.45f
    private val airDrag = 0.995f

    private val restitution = 0.05f
    private val surfaceFriction = 0.75f
    private val wallFriction = 0.75f
    private val rollingFactor = 1.0f

    private val collisionResolveIterations = 3
    private val maxPushOutPerFrame = eggRadius * 3.5f

    private val eggCollisionIterations = 2
    private val eggRestitution = 0.08f
    private val eggTangentialFriction = 0.90f
    private val eggMaxImpulse = 0.8f
    private val eggPositionSlop = 0.35f
    private val eggSeparationBias = 0.35f

    private val solvePasses = 1

    private val sleepSpeed = 0.05f
    private val sleepAngular = 0.4f
    private val sleepFramesToLock = 20

    private val tinyVel = 0.02f
    private val tinyAng = 0.25f

    // Spatial Hashing for Egg-Egg collisions
    private val gridCellSize = 60f // ~4x egg radius
    private var gridCols = 0
    private val spatialGrid = mutableMapOf<Int, MutableList<Egg>>()

    private val normalDirs =
            floatArrayOf(
                    1f,
                    0f,
                    -1f,
                    0f,
                    0f,
                    1f,
                    0f,
                    -1f,
                    0.7071f,
                    0.7071f,
                    -0.7071f,
                    0.7071f,
                    0.7071f,
                    -0.7071f,
                    -0.7071f,
                    -0.7071f,
                    0.3827f,
                    0.9239f,
                    -0.3827f,
                    0.9239f,
                    0.3827f,
                    -0.9239f,
                    -0.3827f,
                    -0.9239f,
                    0.9239f,
                    0.3827f,
                    -0.9239f,
                    0.3827f,
                    0.9239f,
                    -0.3827f,
                    -0.9239f,
                    -0.3827f
            )

    fun initLevel(width: Int, height: Int, level: Int, bgBitmaps: List<Bitmap>) {
        screenWidth = width
        screenHeight = height
        worldHeight = height * 5 + 1000 // Ensure enough terrain below the basket
        cameraY = 0f
        countdownStarted = false
        countdownFrames = 0

        val normalizedLevel = level.coerceIn(1, MAX_LEVEL)
        val targetScore = (BASE_TARGET + (normalizedLevel * EGGS_PER_LEVEL)).coerceAtMost(MAX_TARGET_EGGS)
        val totalEggs = targetScore * 2

        // Initialize chunks
        terrainChunks.forEach { it.bitmap.recycle() }
        terrainChunks.clear()

        val numChunks =
                (worldHeight / chunkSize.toInt()) +
                        (if (worldHeight % chunkSize.toInt() > 0) 1 else 0)

        for (i in 0 until numChunks) {
            val topY = i * chunkSize
            val chunkH =
                    if (i == numChunks - 1) {
                        (worldHeight - topY).coerceAtLeast(1f)
                    } else {
                        chunkSize
                    }

            val bmp = Bitmap.createBitmap(width, chunkH.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Draw random background from provided list
            val bg = if (bgBitmaps.isNotEmpty()) bgBitmaps.random() else null
            if (bg != null) {
                val src = android.graphics.Rect(0, 0, bg.width, bg.height)
                val dest = android.graphics.Rect(0, 0, width, chunkH.toInt())
                canvas.drawBitmap(bg, src, dest, null)
            } else {
                val dirtColor = android.graphics.Color.parseColor("#8D6E63")
                canvas.drawColor(dirtColor)
            }

            terrainChunks.add(TerrainChunk(topY, chunkH, bmp, canvas))
        }

        // Initialize collision mask (downsampled)
        maskWidth = width / collisionScale
        maskHeight = worldHeight / collisionScale
        collisionMask = ByteArray(maskWidth * maskHeight) { 1 } // 1 = solid

        val basketW = 200f
        val basketH = 100f
        basketRect.set(
                width / 2f - basketW / 2f,
                worldHeight - basketH - 500f,
                width / 2f + basketW / 2f,
                worldHeight - 500f
        )

        movingEggs.clear()
        spawnEggs(totalEggs)

        obstacles.clear()
        // generateObstacles(level, width, height)

        _gameState.value = GameState(level = normalizedLevel, score = 0, targetScore = targetScore)
    }

    private fun spawnEggs(totalCount: Int) {
        val topCount = (totalCount * 0.2f).toInt()
        val restCount = totalCount - topCount

        // 20% centered at the start (no eggs in the very first top part)
        spawnClustersInRegion(topCount, screenHeight * 0.4f, screenHeight * 0.9f)

        // 80% distributed through the rest
        spawnClustersInRegion(restCount, screenHeight * 1.1f, worldHeight - 600f)
    }

    private fun spawnClustersInRegion(count: Int, minY: Float, maxY: Float) {
        if (count <= 0) return
        val clusterCount = max(1, count / 5)
        val eggsPerCluster = count / clusterCount

        repeat(clusterCount) {
            val cx = Random.nextFloat() * (screenWidth - 200f) + 100f
            val cy = Random.nextFloat() * (maxY - minY) + minY

            repeat(eggsPerCluster) { i ->
                val egg =
                        Egg(
                                id = System.nanoTime() + i + Random.nextLong(),
                                x = cx + Random.nextFloat() * 120 - 60,
                                y = cy + Random.nextFloat() * 120 - 60,
                                vx = Random.nextFloat() * 2 - 1,
                                vy = 0f,
                                angle = Random.nextFloat() * 360f,
                                angularVelocity = Random.nextFloat() * 5 - 2.5f,
                                type = Random.nextInt(1, 6)
                        )
                movingEggs.add(egg)
            }

            // Draw holes in relevant chunks
            terrainChunks.forEach { chunk ->
                if (cy + 100f > chunk.topY && cy - 100f < chunk.topY + chunk.height) {
                    chunk.canvas.drawCircle(cx, cy - chunk.topY, 100f, fillEraserPaint)
                }
            }
        }
    }

    private fun generateObstacles(level: Int, w: Int, h: Int) {
        val count = 2 + level
        repeat(count) {
            obstacles.add(
                    Obstacle(
                            x = Random.nextFloat() * (w - 100) + 50,
                            y = Random.nextFloat() * (h - 600) + 300,
                            radius = 40f + Random.nextFloat() * 30f,
                            type = ObstacleType.values().random()
                    )
            )
        }
    }

    fun dig(start: Offset, end: Offset) {
        if (_gameState.value.status != GameStatus.PLAYING) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDigTime < digThrottleMs) return
        lastDigTime = currentTime

        // Update visual chunks
        val strokeHalf = eraserPaint.strokeWidth / 2f
        val minY = min(start.y, end.y) - strokeHalf
        val maxY = max(start.y, end.y) + strokeHalf

        terrainChunks.forEach { chunk ->
            if (maxY > chunk.topY && minY < chunk.topY + chunk.height) {
                chunk.canvas.save()
                chunk.canvas.translate(0f, -chunk.topY)
                chunk.canvas.drawLine(start.x, start.y, end.x, end.y, eraserPaint)
                chunk.canvas.restore()
            }
        }

        // Update collision mask
        updateMaskForLine(start, end, 40f) // eraser strokeWidth/2 = 40
    }

    private fun updateMaskForLine(start: Offset, end: Offset, radius: Float) {
        val mask = collisionMask ?: return
        val r = radius / collisionScale
        val rSq = r * r

        val x1 = start.x / collisionScale
        val y1 = start.y / collisionScale
        val x2 = end.x / collisionScale
        val y2 = end.y / collisionScale

        val minX = (min(x1, x2) - r).toInt().coerceIn(0, maskWidth - 1)
        val maxX = (max(x1, x2) + r).toInt().coerceIn(0, maskWidth - 1)
        val minY = (min(y1, y2) - r).toInt().coerceIn(0, maskHeight - 1)
        val maxY = (max(y1, y2) + r).toInt().coerceIn(0, maskHeight - 1)

        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy

        for (my in minY..maxY) {
            for (mx in minX..maxX) {
                val t = if (lenSq == 0f) 0f else ((mx - x1) * dx + (my - y1) * dy) / lenSq
                val closestX = x1 + t.coerceIn(0f, 1f) * dx
                val closestY = y1 + t.coerceIn(0f, 1f) * dy

                val distSq = (mx - closestX) * (mx - closestX) + (my - closestY) * (my - closestY)
                if (distSq < rSq) {
                    mask[my * maskWidth + mx] = 0
                }
            }
        }
    }

    fun update() {
        if (_gameState.value.status != GameStatus.PLAYING) return

        // Auto-scroll camera
        if (cameraY < worldHeight - screenHeight) {
            cameraY += scrollSpeed
        }

        var score = _gameState.value.score

        movingEggs.forEach { egg ->
            if (!egg.isActive) return@forEach

            val isSleeping = egg.sleepFrames >= sleepFramesToLock

            if (isSleeping) {
                // Check if sleeping egg is floating (terrain was dug away)
                if (!checkTerrainCollision(egg.x, egg.y, eggRadius)) {
                    egg.sleepFrames = 0
                    egg.vy = gravity // Start falling
                } else {
                    egg.vx = 0f
                    egg.vy = 0f
                    egg.angularVelocity = 0f
                }
            } else {
                egg.vy += gravity
                egg.vx *= airDrag
                egg.vy *= airDrag

                var nextX = egg.x + egg.vx
                var nextY = egg.y + egg.vy

                val wallHit =
                        resolveScreenBounds(egg, nextX, nextY)
                                .also {
                                    nextX = it.first
                                    nextY = it.second
                                }
                                .third

                val terrainResult =
                        resolveTerrainCollision(
                                startX = nextX,
                                startY = nextY,
                                radius = eggRadius,
                                vx = egg.vx,
                                vy = egg.vy,
                                wallHit = wallHit
                        )

                egg.x = terrainResult.x
                egg.y = terrainResult.y
                egg.vx = terrainResult.vx
                egg.vy = terrainResult.vy
                egg.angularVelocity = terrainResult.angularVelocity

                egg.angle = normalizeAngle(egg.angle + egg.angularVelocity)

                if (abs(egg.vx) < tinyVel) egg.vx = 0f
                if (abs(egg.vy) < tinyVel) egg.vy = 0f
                if (abs(egg.angularVelocity) < tinyAng) egg.angularVelocity = 0f

                if (egg.y > worldHeight + 200f) {
                    egg.y = cameraY - eggRadius // Reset to top of view
                    egg.vy = 0f
                    egg.vx = 0f
                    egg.angularVelocity = 0f
                    egg.sleepFrames = sleepFramesToLock
                }

                val postSpeedSq = egg.vx * egg.vx + egg.vy * egg.vy
                val postSleepBand =
                        postSpeedSq < (sleepSpeed * sleepSpeed) &&
                                abs(egg.angularVelocity) < sleepAngular
                egg.sleepFrames =
                        if (postSleepBand) min(egg.sleepFrames + 1, sleepFramesToLock) else 0
            }
        }

        repeat(solvePasses) {
            resolveEggCollisionsOnce()
            movingEggs.forEach { egg ->
                if (!egg.isActive) return@forEach
                val wallHit = resolveScreenBoundsNoBounce(egg)
                val settled =
                        resolveTerrainCollision(
                                startX = egg.x,
                                startY = egg.y,
                                radius = eggRadius,
                                vx = egg.vx,
                                vy = egg.vy,
                                wallHit = wallHit
                        )
                egg.x = settled.x
                egg.y = settled.y
                egg.vx = settled.vx
                egg.vy = settled.vy
                if (egg.sleepFrames >= sleepFramesToLock) {
                    egg.vx = 0f
                    egg.vy = 0f
                    egg.angularVelocity = 0f
                }
                clampToScreen(egg)
            }
        }

        movingEggs.forEach { egg ->
            if (!egg.isActive) return@forEach

            /*
            obstacles.forEach obstacleLoop@{ obs ->
                if (!egg.isActive) return@obstacleLoop

                val dx = egg.x - obs.x
                val dy = egg.y - obs.y
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < obs.radius + eggRadius) {
                    when (obs.type) {
                        ObstacleType.SPIKE, ObstacleType.HOLE -> egg.isActive = false
                        ObstacleType.STONE -> {
                            val ang = atan2(dy, dx)
                            val speed = sqrt(egg.vx * egg.vx + egg.vy * egg.vy)
                            val push = max(speed, 2f)
                            egg.vx = cos(ang) * push * 0.6f
                            egg.vy = sin(ang) * push * 0.6f
                            egg.sleepFrames = 0
                        }
                    }
                }
            }
            */

            if (egg.isActive && basketRect.contains(egg.x, egg.y)) {
                egg.isActive = false
                _gameState.value =
                        _gameState.value.copy(
                                collectedEggs =
                                        (_gameState.value.collectedEggs + 1).coerceAtMost(
                                                MAX_TARGET_EGGS
                                        )
                        )
            }
        }

        val activeEggs = movingEggs.filter { it.isActive }

        val isAtEnd = cameraY >= worldHeight - screenHeight
        var newStatus = _gameState.value.status
        var displayCountdown: Int? = null

        if (isAtEnd && !countdownStarted && newStatus == GameStatus.PLAYING) {
            countdownStarted = true
            countdownFrames = countdownTotalFrames
        }

        if (countdownStarted) {
            if (countdownFrames > 0) {
                countdownFrames--
                displayCountdown = (countdownFrames / 60) + 1
            } else {
                val collected = _gameState.value.collectedEggs
                val target = _gameState.value.targetScore
                newStatus = if (collected >= target) GameStatus.WON else GameStatus.LOST
            }
        }

        val currentCollected = _gameState.value.collectedEggs
        val target = _gameState.value.targetScore
        val finalScore =
                if (currentCollected >= target) {
                    target + (currentCollected - target) * 2
                } else {
                    currentCollected
                }

        _gameState.value =
                _gameState.value.copy(
                        score = finalScore,
                        eggs = activeEggs,
                        status = newStatus,
                        frameTick = _gameState.value.frameTick + 1,
                        countdownSeconds = displayCountdown
                )
    }

    private fun resolveEggCollisionsOnce() {
        val active = movingEggs.filter { it.isActive }
        if (active.size < 2) return

        // 1. Rebuild spatial grid
        spatialGrid.clear()
        active.forEach { egg ->
            val gx = (egg.x / gridCellSize).toInt()
            val gy = (egg.y / gridCellSize).toInt()
            val key = gx * 10000 + gy // Simple spatial key
            spatialGrid.getOrPut(key) { mutableListOf() }.add(egg)
        }

        val minDist = eggRadius * 2f
        val minDistSq = minDist * minDist

        repeat(eggCollisionIterations) {
            active.forEach { a ->
                val gx = (a.x / gridCellSize).toInt()
                val gy = (a.y / gridCellSize).toInt()

                // Check 9 neighboring cells
                for (ox in -1..1) {
                    for (oy in -1..1) {
                        val key = (gx + ox) * 10000 + (gy + oy)
                        val cellEggs = spatialGrid[key] ?: continue

                        cellEggs.forEach cellEggLoop@{ b ->
                            if (a.id >= b.id) return@cellEggLoop // Only check pairs once

                            val aSleep = a.sleepFrames >= sleepFramesToLock
                            val bSleep = b.sleepFrames >= sleepFramesToLock
                            if (aSleep && bSleep) return@forEach

                            val dx = b.x - a.x
                            val dy = b.y - a.y
                            val distSq = dx * dx + dy * dy

                            if (distSq >= minDistSq) return@forEach

                            val dist = sqrt(max(distSq, 0.0001f))
                            val nx = dx / dist
                            val ny = dy / dist

                            val penetration = (minDist - dist) - eggPositionSlop
                            if (penetration > 0f) {
                                val push = penetration * 0.5f * eggSeparationBias
                                if (!aSleep) {
                                    a.x -= nx * push
                                    a.y -= ny * push
                                }
                                if (!bSleep) {
                                    b.x += nx * push
                                    b.y += ny * push
                                }

                                clampToScreen(a)
                                clampToScreen(b)

                                if (penetration > 1.0f) {
                                    a.sleepFrames = 0
                                    b.sleepFrames = 0
                                }
                            }

                            if (aSleep || bSleep) {
                                val rvx = b.vx - a.vx
                                val rvy = b.vy - a.vy
                                val velAlongNormal = rvx * nx + rvy * ny
                                if (velAlongNormal < -0.5f) {
                                    a.sleepFrames = 0
                                    b.sleepFrames = 0
                                }
                                if (aSleep && !bSleep) {
                                    val vn = b.vx * nx + b.vy * ny
                                    if (vn < 0) {
                                        b.vx -= 2 * vn * nx * eggRestitution
                                        b.vy -= 2 * vn * ny * eggRestitution
                                    }
                                    return@forEach
                                } else if (!aSleep && bSleep) {
                                    val vn = a.vx * nx + a.vy * ny
                                    if (vn > 0) {
                                        a.vx -= 2 * vn * nx * eggRestitution
                                        a.vy -= 2 * vn * ny * eggRestitution
                                    }
                                    return@forEach
                                }
                            }

                            val rvx = b.vx - a.vx
                            val rvy = b.vy - a.vy
                            val velAlongNormal = rvx * nx + rvy * ny
                            if (velAlongNormal > 0f) return@forEach

                            val e = eggRestitution
                            var jImpulse = -(1f + e) * velAlongNormal / 2f
                            jImpulse = jImpulse.coerceIn(-eggMaxImpulse, eggMaxImpulse)

                            val ix = jImpulse * nx
                            val iy = jImpulse * ny
                            a.vx -= ix
                            a.vy -= iy
                            b.vx += ix
                            b.vy += iy

                            val tx = -ny
                            val ty = nx
                            val velAlongTangent = rvx * tx + rvy * ty

                            val jt = (velAlongTangent / 2f) * (1f - eggTangentialFriction)
                            a.vx += tx * jt
                            a.vy += ty * jt
                            b.vx -= tx * jt
                            b.vy -= ty * jt

                            val spin = -(velAlongTangent / max(eggRadius, 1f)) * 57.29578f * 0.25f
                            a.angularVelocity = a.angularVelocity + spin * 0.5f
                            b.angularVelocity = b.angularVelocity - spin * 0.5f
                        }
                    }
                }
            }
        }
    }

    private fun clampToScreen(egg: Egg) {
        egg.x = egg.x.coerceIn(eggRadius, screenWidth - eggRadius)
        egg.y = egg.y.coerceIn(eggRadius, worldHeight - eggRadius)
    }

    private data class TerrainResolveResult(
            val x: Float,
            val y: Float,
            val vx: Float,
            val vy: Float,
            val angularVelocity: Float
    )

    private fun resolveTerrainCollision(
            startX: Float,
            startY: Float,
            radius: Float,
            vx: Float,
            vy: Float,
            wallHit: Boolean
    ): TerrainResolveResult {

        var px = startX
        var py = startY
        var cvx = vx
        var cvy = vy

        var normalX = 0f
        var normalY = 0f
        var collided = false
        var pushedTotal = 0f

        repeat(collisionResolveIterations) {
            if (!checkTerrainCollision(px, py, radius)) return@repeat

            collided = true

            val n = estimateCollisionNormal(px, py, radius)
            if (n.first == 0f && n.second == 0f) return@repeat

            normalX = n.first
            normalY = n.second

            val pushStep = min(radius * 0.15f, maxPushOutPerFrame - pushedTotal)
            if (pushStep <= 0f) return@repeat

            px += normalX * pushStep
            py += normalY * pushStep
            pushedTotal += pushStep
        }

        if (!collided) {
            return TerrainResolveResult(px, py, cvx, cvy, 0f)
        }

        val vn = cvx * normalX + cvy * normalY
        val tx = -normalY
        val ty = normalX
        val vt = cvx * tx + cvy * ty

        val frictionK = if (wallHit || abs(normalX) > 0.6f) wallFriction else surfaceFriction

        if (vn < 0f) {
            val newVn = if (abs(vn) < 0.5f) 0f else -vn * restitution
            val newVt = vt * frictionK
            cvx = tx * newVt + normalX * newVn
            cvy = ty * newVt + normalY * newVn
        } else {
            val newVt = vt * frictionK
            cvx = tx * newVt + normalX * vn
            cvy = ty * newVt + normalY * vn
        }

        val angularVel = -(vt / max(radius, 1f)) * 57.29578f * rollingFactor
        return TerrainResolveResult(px, py, cvx, cvy, angularVel)
    }

    private fun estimateCollisionNormal(x: Float, y: Float, radius: Float): Pair<Float, Float> {
        var ax = 0f
        var ay = 0f
        var hits = 0

        var i = 0
        while (i < normalDirs.size) {
            val dx = normalDirs[i]
            val dy = normalDirs[i + 1]
            val sx = (x + dx * radius).toInt()
            val sy = (y + dy * radius).toInt()

            if (isTerrainSolid(sx, sy)) {
                ax -= dx
                ay -= dy
                hits++
            }
            i += 2
        }

        if (hits == 0) return 0f to 0f

        val len = sqrt(ax * ax + ay * ay)
        if (len < 0.0001f) return 0f to 0f

        return (ax / len) to (ay / len)
    }

    private fun normalizeAngle(a: Float): Float {
        var v = a % 360f
        if (v < 0f) v += 360f
        return v
    }

    private fun isTerrainSolid(x: Int, y: Int): Boolean {
        val mask = collisionMask ?: return false
        val mx = x / collisionScale
        val my = y / collisionScale
        if (mx !in 0 until maskWidth || my !in 0 until maskHeight) return false
        return mask[my * maskWidth + mx].toInt() == 1
    }

    private fun checkTerrainCollision(x: Float, y: Float, radius: Float): Boolean {
        val points =
                floatArrayOf(
                        0f,
                        0f,
                        0f,
                        radius,
                        0f,
                        -radius,
                        radius,
                        0f,
                        -radius,
                        0f,
                        radius * 0.7f,
                        radius * 0.7f,
                        -radius * 0.7f,
                        radius * 0.7f,
                        radius * 0.7f,
                        -radius * 0.7f,
                        -radius * 0.7f,
                        -radius * 0.7f
                )

        var i = 0
        while (i < points.size) {
            val cx = (x + points[i]).toInt()
            val cy = (y + points[i + 1]).toInt()
            if (isTerrainSolid(cx, cy)) return true
            i += 2
        }
        return false
    }

    private fun resolveScreenBounds(
            egg: Egg,
            nextX: Float,
            nextY: Float
    ): Triple<Float, Float, Boolean> {
        var nx = nextX
        var ny = nextY
        var hit = false

        if (nx < eggRadius) {
            nx = eggRadius
            egg.vx *= -wallFriction
            hit = true
        } else if (nx > screenWidth - eggRadius) {
            nx = screenWidth - eggRadius
            egg.vx *= -wallFriction
            hit = true
        }

        if (ny > worldHeight - eggRadius) {
            ny = worldHeight - eggRadius
            egg.vy *= -surfaceFriction
            hit = true
        }

        return Triple(nx, ny, hit)
    }

    private fun resolveScreenBoundsNoBounce(egg: Egg): Boolean {
        var hit = false
        if (egg.x < eggRadius) {
            egg.x = eggRadius
            hit = true
        } else if (egg.x > screenWidth - eggRadius) {
            egg.x = screenWidth - eggRadius
            hit = true
        }
        if (egg.y > worldHeight - eggRadius) {
            egg.y = worldHeight - eggRadius
            hit = true
        }
        return hit
    }

    fun pause() {
        if (_gameState.value.status == GameStatus.PLAYING) {
            _gameState.value = _gameState.value.copy(status = GameStatus.PAUSED)
        }
    }

    fun resume() {
        if (_gameState.value.status == GameStatus.PAUSED) {
            _gameState.value = _gameState.value.copy(status = GameStatus.PLAYING)
        }
    }
}
