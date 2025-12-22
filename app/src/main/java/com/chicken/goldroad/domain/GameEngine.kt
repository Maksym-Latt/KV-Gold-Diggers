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
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
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
    var isActive: Boolean = true
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
class GameEngine @Inject constructor(
    private val soundManager: SoundManager
) {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    var terrainBitmap: Bitmap? = null
    private var terrainCanvas: Canvas? = null

    private val eraserPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeWidth = 80f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
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

    private val collisionResolveIterations = 5
    private val maxPushOutPerFrame = eggRadius * 1.35f

    private val eggCollisionIterations = 5
    private val eggRestitution = 0.08f
    private val eggTangentialFriction = 0.90f
    private val eggMaxImpulse = 1.8f
    private val eggPositionSlop = 0.02f
    private val eggSeparationBias = 0.72f

    private val settleNormalVelEps = 0.02f
    private val sleepVelEps = 0.06f
    private val sleepSpinEps = 0.25f

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
        repeat(count) { i ->
            movingEggs.add(
                Egg(
                    id = System.nanoTime() + i,
                    x = startX + Random.nextFloat() * 300 - 150,
                    y = startY + Random.nextFloat() * 100 - 50,
                    vx = Random.nextFloat() * 2 - 1,
                    vy = 0f,
                    angle = Random.nextFloat() * 360f,
                    angularVelocity = Random.nextFloat() * 5 - 2.5f,
                    type = Random.nextInt(1, 6)
                )
            )
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
    }

    fun update(deltaTime: Long) {
        if (_gameState.value.status != GameStatus.PLAYING) return

        var score = _gameState.value.score

        movingEggs.forEach { egg ->
            if (!egg.isActive) return@forEach

            if (!egg.x.isFinite() || !egg.y.isFinite() || !egg.vx.isFinite() || !egg.vy.isFinite()) {
                egg.x = (screenWidth * 0.5f).coerceIn(eggRadius, screenWidth - eggRadius)
                egg.y = (eggRadius + 5f).coerceIn(eggRadius, screenHeight - eggRadius)
                egg.vx = 0f
                egg.vy = 0f
                egg.angularVelocity = 0f
            }

            egg.vy += gravity
            egg.vx *= airDrag
            egg.vy *= airDrag

            var nextX = egg.x + egg.vx
            var nextY = egg.y + egg.vy

            val wallHit = resolveScreenBounds(egg, nextX, nextY).also {
                nextX = it.first
                nextY = it.second
            }.third

            val terrainResult = resolveTerrainCollision(
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

            if (egg.y > screenHeight + 50) {
                egg.isActive = false
            }
        }

        resolveEggCollisions()

        movingEggs.forEach { egg ->
            if (!egg.isActive) return@forEach
            enforceNotInsideTerrain(egg)
            resolveScreenBounds(egg, egg.x, egg.y)
            settleIfOnSurface(egg)
        }

        movingEggs.forEach { egg ->
            if (!egg.isActive) return@forEach

            obstacles.forEach { obs ->
                if (!egg.isActive) return@forEach

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
                        }
                    }
                }
            }

            if (egg.isActive && basketRect.contains(egg.x, egg.y)) {
                egg.isActive = false
                score++
            }
        }

        val newStatus = if (score >= _gameState.value.targetScore) GameStatus.WON else GameStatus.PLAYING

        _gameState.value = _gameState.value.copy(
            score = score,
            eggs = movingEggs.filter { it.isActive },
            status = newStatus,
            frameTick = _gameState.value.frameTick + 1
        )
    }

    private fun resolveEggCollisions() {
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
                    }

                    val rvx = b.vx - a.vx
                    val rvy = b.vy - a.vy
                    val velAlongNormal = rvx * nx + rvy * ny

                    if (velAlongNormal > 0f) continue

                    val e = eggRestitution
                    var jImpulse = -(1f + e) * velAlongNormal * 0.5f
                    jImpulse = jImpulse.coerceIn(0f, eggMaxImpulse)

                    val ix = jImpulse * nx
                    val iy = jImpulse * ny

                    a.vx -= ix
                    a.vy -= iy
                    b.vx += ix
                    b.vy += iy

                    val tx = -ny
                    val ty = nx
                    val velAlongTangent = rvx * tx + rvy * ty

                    val jt = velAlongTangent * 0.5f * (1f - eggTangentialFriction)
                    a.vx += tx * jt
                    a.vy += ty * jt
                    b.vx -= tx * jt
                    b.vy -= ty * jt

                    val spin = -(velAlongTangent / max(eggRadius, 1f)) * 57.29578f * 0.25f
                    a.angularVelocity += spin * 0.5f
                    b.angularVelocity -= spin * 0.5f
                }
            }
        }
    }

    private fun enforceNotInsideTerrain(egg: Egg) {
        val bmp = terrainBitmap ?: return
        if (!checkTerrainCollision(egg.x, egg.y, eggRadius)) return

        var px = egg.x
        var py = egg.y
        var pushed = 0f

        repeat(collisionResolveIterations + 2) {
            if (!checkTerrainCollision(px, py, eggRadius)) return@repeat

            val n = estimateCollisionNormal(px, py, eggRadius)
            val nx = n.first
            val ny = n.second
            if (nx == 0f && ny == 0f) return@repeat

            val step = min(eggRadius * 0.30f, maxPushOutPerFrame - pushed)
            if (step <= 0f) return@repeat

            px += nx * step
            py += ny * step
            pushed += step
        }

        egg.x = px
        egg.y = py
        clampToScreen(egg)
    }

    private fun settleIfOnSurface(egg: Egg) {
        if (terrainBitmap == null) return

        val onSurface = isTerrainSolid(egg.x.toInt(), (egg.y + eggRadius + 1f).toInt()) ||
                isTerrainSolid((egg.x + eggRadius * 0.7f).toInt(), (egg.y + eggRadius + 1f).toInt()) ||
                isTerrainSolid((egg.x - eggRadius * 0.7f).toInt(), (egg.y + eggRadius + 1f).toInt())

        if (!onSurface) return

        if (abs(egg.vy) < settleNormalVelEps) egg.vy = 0f

        if (abs(egg.vx) < sleepVelEps && abs(egg.vy) < sleepVelEps) {
            egg.vx = 0f
            egg.vy = 0f
            if (abs(egg.angularVelocity) < sleepSpinEps) egg.angularVelocity = 0f
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
        } else if (nx > screenWidth - eggRadius) {
            nx = screenWidth - eggRadius
            egg.vx = -egg.vx * restitution
            hit = true
        }

        if (ny < eggRadius) {
            ny = eggRadius
            egg.vy = -egg.vy * restitution
        }

        egg.x = nx
        egg.y = ny

        return Triple(nx, ny, hit)
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

            val n = estimateCollisionNormal(px, py, radius)
            if (n.first == 0f && n.second == 0f) return@repeat

            normalX = n.first
            normalY = n.second

            val pushStep = min(radius * 0.33f, maxPushOutPerFrame - pushedTotal)
            if (pushStep <= 0f) return@repeat

            px += normalX * pushStep
            py += normalY * pushStep
            pushedTotal += pushStep
        }

        if (!collided) return TerrainResolveResult(px, py, cvx, cvy, 0f)

        val vn = cvx * normalX + cvy * normalY
        val tx = -normalY
        val ty = normalX
        val vt = cvx * tx + cvy * ty

        val frictionK = if (wallHit || abs(normalX) > 0.6f) wallFriction else surfaceFriction

        val finalVn = if (vn < 0f) -vn * restitution else vn
        val finalVt = vt * frictionK

        cvx = tx * finalVt + normalX * finalVn
        cvy = ty * finalVt + normalY * finalVn

        if (abs(vn) < settleNormalVelEps) cvy = min(cvy, 0f)

        val angularVel = -(finalVt / max(radius, 1f)) * 57.29578f * rollingFactor
        return TerrainResolveResult(px, py, cvx, cvy, angularVel)
    }

    private fun estimateCollisionNormal(x: Float, y: Float, radius: Float): Pair<Float, Float> {
        val dirs = listOf(
            1f to 0f,
            -1f to 0f,
            0f to 1f,
            0f to -1f,
            0.7071f to 0.7071f,
            -0.7071f to 0.7071f,
            0.7071f to -0.7071f,
            -0.7071f to -0.7071f
        )

        var ax = 0f
        var ay = 0f
        var hits = 0

        for (d in dirs) {
            val sx = (x + d.first * radius).toInt()
            val sy = (y + d.second * radius).toInt()
            if (isTerrainSolid(sx, sy)) {
                ax -= d.first
                ay -= d.second
                hits++
            }
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

        val points = listOf(
            Offset(0f, 0f),
            Offset(0f, radius),
            Offset(0f, -radius),
            Offset(radius, 0f),
            Offset(-radius, 0f),
            Offset(radius * 0.7f, radius * 0.7f),
            Offset(-radius * 0.7f, radius * 0.7f),
            Offset(radius * 0.7f, -radius * 0.7f),
            Offset(-radius * 0.7f, -radius * 0.7f)
        )

        for (p in points) {
            val cx = (x + p.x).toInt()
            val cy = (y + p.y).toInt()
            if (cx !in 0 until screenWidth || cy !in 0 until screenHeight) continue

            try {
                val pixel = bmp.getPixel(cx, cy)
                if ((pixel ushr 24) > 0) return true
            } catch (_: Exception) {
            }
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