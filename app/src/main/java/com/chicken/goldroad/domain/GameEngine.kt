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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
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
        val targetScore: Int = 100,
        val status: GameStatus = GameStatus.PLAYING,
        val eggs: List<Egg> = emptyList(),
        val level: Int = 1,
        val frameTick: Long = 0
)

@Singleton
class GameEngine @Inject constructor(private val soundManager: SoundManager) {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    var terrainBitmap: Bitmap? = null
    private var terrainCanvas: Canvas? = null

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
    private val surfaceFriction = 0.88f
    private val wallFriction = 0.92f
    private val rollingFactor = 1.0f

    private val collisionResolveIterations = 9
    private val maxPushOutPerFrame = eggRadius * 3.5f

    private val eggCollisionIterations = 6
    private val eggRestitution = 0.08f
    private val eggTangentialFriction = 0.90f
    private val eggMaxImpulse = 1.8f
    private val eggPositionSlop = 0.35f
    private val eggSeparationBias = 0.65f

    private val solvePasses = 3

    private val sleepSpeed = 0.05f
    private val sleepAngular = 0.4f
    private val sleepFramesToLock = 10

    private val tinyVel = 0.02f
    private val tinyAng = 0.25f

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

    fun initLevel(width: Int, height: Int, level: Int) {
        screenWidth = width
        screenHeight = height

        terrainBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        terrainCanvas = Canvas(terrainBitmap!!)

        val dirtColor = android.graphics.Color.parseColor("#8D6E63")
        terrainCanvas?.drawColor(dirtColor)

        val basketW = 200f
        val basketH = 100f
        basketRect.set(
                width / 2f - basketW / 2f,
                height - basketH - 50f,
                width / 2f + basketW / 2f,
                height - 50f
        )

        movingEggs.clear()
        spawnEggs(25, width / 2f, 150f)

        obstacles.clear()
        generateObstacles(level, width, height)

        _gameState.value = GameState(level = level, score = 0, targetScore = 20 + (level * 10))
    }

    private fun spawnEggs(count: Int, startX: Float, startY: Float) {
        val clusterCount = Random.nextInt(3, 6)
        val eggsPerCluster = count / clusterCount

        repeat(clusterCount) {
            // Cluster center: favor top (startY is 150f, screenHeight is available)
            // We'll distribute clusters across the width, and mostly in the top half
            val cx = Random.nextFloat() * (screenWidth - 200f) + 100f
            val cy = Random.nextFloat() * (screenHeight * 0.4f) + 50f // Favor top 40%

            // Dig void for cluster
            terrainCanvas?.drawCircle(cx, cy, 100f, fillEraserPaint)

            repeat(eggsPerCluster) { i ->
                movingEggs.add(
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
                )
            }
        }

        // Add any remaining eggs to a final random cluster
        val remaining = count % clusterCount
        if (remaining > 0) {
            val cx = Random.nextFloat() * (screenWidth - 200f) + 100f
            val cy = Random.nextFloat() * (screenHeight * 0.4f) + 50f

            // Dig void for cluster
            terrainCanvas?.drawCircle(cx, cy, 100f, fillEraserPaint)

            repeat(remaining) { i ->
                movingEggs.add(
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
                )
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
        terrainCanvas?.drawLine(start.x, start.y, end.x, end.y, eraserPaint)
        movingEggs.forEach { it.sleepFrames = 0 }
    }

    fun update(deltaTime: Long) {
        if (_gameState.value.status != GameStatus.PLAYING) return

        var score = _gameState.value.score

        movingEggs.forEach { egg ->
            if (!egg.isActive) return@forEach

            val speedSq = egg.vx * egg.vx + egg.vy * egg.vy
            val inSleepBand =
                    speedSq < (sleepSpeed * sleepSpeed) && abs(egg.angularVelocity) < sleepAngular

            if (egg.sleepFrames >= sleepFramesToLock && inSleepBand) {
                egg.vx = 0f
                egg.vy = 0f
                egg.angularVelocity = 0f
            } else {
                egg.vy += gravity
                egg.vx *= airDrag
                egg.vy *= airDrag
            }

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

            if (egg.y > screenHeight + 2000f) {
                egg.y = screenHeight - eggRadius
                egg.vy = 0f
                egg.vx = 0f
                egg.angularVelocity = 0f
                egg.sleepFrames = sleepFramesToLock
            }

            val postSpeedSq = egg.vx * egg.vx + egg.vy * egg.vy
            val postSleepBand =
                    postSpeedSq < (sleepSpeed * sleepSpeed) &&
                            abs(egg.angularVelocity) < sleepAngular
            egg.sleepFrames = if (postSleepBand) min(egg.sleepFrames + 1, sleepFramesToLock) else 0
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

            if (egg.isActive && basketRect.contains(egg.x, egg.y)) {
                egg.isActive = false
                score++
            }
        }

        val newStatus =
                if (score >= _gameState.value.targetScore) GameStatus.WON else GameStatus.PLAYING

        _gameState.value =
                _gameState.value.copy(
                        score = score,
                        eggs = movingEggs.filter { it.isActive },
                        status = newStatus,
                        frameTick = _gameState.value.frameTick + 1
                )
    }

    private fun resolveEggCollisionsOnce() {
        val active = movingEggs.filter { it.isActive }
        if (active.size < 2) return

        val minDist = eggRadius * 2f
        val minDistSq = minDist * minDist

        repeat(eggCollisionIterations) {
            for (i in 0 until active.size - 1) {
                val a = active[i]
                for (j in i + 1 until active.size) {
                    val b = active[j]

                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val distSq = dx * dx + dy * dy
                    if (distSq >= minDistSq) continue

                    val dist = sqrt(max(distSq, 0.0001f))
                    val nx = dx / dist
                    val ny = dy / dist

                    val penetration = (minDist - dist) - eggPositionSlop
                    if (penetration > 0f) {
                        val push = penetration * 0.5f * eggSeparationBias
                        a.x -= nx * push
                        a.y -= ny * push
                        b.x += nx * push
                        b.y += ny * push
                        clampToScreen(a)
                        clampToScreen(b)
                        a.sleepFrames = 0
                        b.sleepFrames = 0
                    }

                    val rvx = b.vx - a.vx
                    val rvy = b.vy - a.vy
                    val velAlongNormal = rvx * nx + rvy * ny
                    if (velAlongNormal > 0f) continue

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

    private fun clampToScreen(egg: Egg) {
        egg.x = egg.x.coerceIn(eggRadius, screenWidth - eggRadius)
        egg.y = egg.y.coerceIn(eggRadius, screenHeight - eggRadius)
    }

    private fun resolveScreenBounds(egg: Egg, x: Float, y: Float): Triple<Float, Float, Boolean> {
        var nx = x
        var ny = y
        var hit = false

        if (nx < eggRadius) {
            nx = eggRadius
            egg.vx = -egg.vx * restitution
            hit = true
            egg.sleepFrames = 0
        } else if (nx > screenWidth - eggRadius) {
            nx = screenWidth - eggRadius
            egg.vx = -egg.vx * restitution
            hit = true
            egg.sleepFrames = 0
        }

        if (ny < eggRadius) {
            ny = eggRadius
            egg.vy = -egg.vy * restitution
            egg.sleepFrames = 0
        }

        return Triple(nx, ny, hit)
    }

    private fun resolveScreenBoundsNoBounce(egg: Egg): Boolean {
        var hit = false
        if (egg.x < eggRadius) {
            egg.x = eggRadius
            if (egg.vx < 0f) egg.vx = 0f
            hit = true
        } else if (egg.x > screenWidth - eggRadius) {
            egg.x = screenWidth - eggRadius
            if (egg.vx > 0f) egg.vx = 0f
            hit = true
        }
        if (egg.y < eggRadius) {
            egg.y = eggRadius
            if (egg.vy < 0f) egg.vy = 0f
        }
        if (egg.y > screenHeight - eggRadius) {
            egg.y = screenHeight - eggRadius
            if (egg.vy > 0f) egg.vy = 0f
        }
        return hit
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
        val bmp = terrainBitmap ?: return TerrainResolveResult(startX, startY, vx, vy, 0f)

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

            val n = estimateCollisionNormal(px, py, radius, bmp)
            if (n.first == 0f && n.second == 0f) return@repeat

            normalX = n.first
            normalY = n.second

            val pushStep = min(radius * 0.55f, maxPushOutPerFrame - pushedTotal)
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
            val newVn = if (abs(vn) < 0.10f) 0f else -vn * restitution
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

    private fun estimateCollisionNormal(
            x: Float,
            y: Float,
            radius: Float,
            bmp: Bitmap
    ): Pair<Float, Float> {
        var ax = 0f
        var ay = 0f
        var hits = 0

        var i = 0
        while (i < normalDirs.size) {
            val dx = normalDirs[i]
            val dy = normalDirs[i + 1]
            val sx = (x + dx * radius).toInt()
            val sy = (y + dy * radius).toInt()

            if (sx in 0 until screenWidth && sy in 0 until screenHeight) {
                val solid =
                        try {
                            val pixel = bmp.getPixel(sx, sy)
                            (pixel ushr 24) > 0
                        } catch (_: Exception) {
                            false
                        }
                if (solid) {
                    ax -= dx
                    ay -= dy
                    hits++
                }
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
        val bmp = terrainBitmap ?: return false
        if (x < 0 || x >= screenWidth || y < 0 || y >= screenHeight) return false
        return try {
            val pixel = bmp.getPixel(x, y)
            (pixel ushr 24) > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun checkTerrainCollision(x: Float, y: Float, radius: Float): Boolean {
        val bmp = terrainBitmap ?: return false

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
                        -radius * 0.7f,
                        radius * 0.92f,
                        radius * 0.38f,
                        -radius * 0.92f,
                        radius * 0.38f,
                        radius * 0.92f,
                        -radius * 0.38f,
                        -radius * 0.92f,
                        -radius * 0.38f
                )

        var i = 0
        while (i < points.size) {
            val cx = (x + points[i]).toInt()
            val cy = (y + points[i + 1]).toInt()
            if (cx in 0 until screenWidth && cy in 0 until screenHeight) {
                val solid =
                        try {
                            val pixel = bmp.getPixel(cx, cy)
                            (pixel ushr 24) > 0
                        } catch (_: Exception) {
                            false
                        }
                if (solid) return true
            }
            i += 2
        }
        return false
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
