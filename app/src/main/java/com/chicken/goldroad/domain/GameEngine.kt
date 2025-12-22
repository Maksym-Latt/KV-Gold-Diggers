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
import kotlin.math.max
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
    val type: Int, // 1..6
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

    // ---------------------------
    // Physics tuning (realistic-ish)
    // ---------------------------
    private val gravityPxPerSec2 = 2600f          // px/s^2 (подбирай под размеры)
    private val airDragPerSec = 0.35f             // 0..1 (сопротивление воздуха)
    private val restitution = 0.06f               // 0..1 (почти без отскока)
    private val groundFriction = 5.5f             // (скольжение по земле)
    private val rollingFriction = 1.8f            // (затухание катания)
    private val maxSpeed = 2400f                  // px/s safety clamp
    private val wallRestitution = 0.08f

    // ---------------------------
    // Terrain
    // ---------------------------
    var terrainBitmap: Bitmap? = null
        private set

    private var terrainCanvas: Canvas? = null

    private val eraserPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeWidth = 80f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    // ---------------------------
    // Entities
    // ---------------------------
    private val movingEggs = CopyOnWriteArrayList<Egg>()
    private val obstacles = CopyOnWriteArrayList<Obstacle>()

    // ---------------------------
    // Dimensions
    // ---------------------------
    private var screenWidth = 0
    private var screenHeight = 0
    var basketRect = android.graphics.RectF()

    // ---------------------------
    // Config
    // ---------------------------
    val eggRadius = 15f

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
        spawnEggs(count = 25, startX = width / 2f, startY = 150f)

        obstacles.clear()
        generateObstacles(level, width, height)

        _gameState.value = GameState(
            level = level,
            score = 0,
            targetScore = 20 + (level * 10),
            status = GameStatus.PLAYING
        )
    }

    private fun spawnEggs(count: Int, startX: Float, startY: Float) {
        repeat(count) { i ->
            movingEggs.add(
                Egg(
                    id = System.nanoTime() + i,
                    x = startX + Random.nextFloat() * 300f - 150f,
                    y = startY + Random.nextFloat() * 100f - 50f,
                    vx = Random.nextFloat() * 80f - 40f,     // px/s
                    vy = Random.nextFloat() * 30f,           // px/s
                    angle = Random.nextFloat() * 360f,
                    angularVelocity = Random.nextFloat() * 60f - 30f, // deg/s
                    type = Random.nextInt(1, 7)
                )
            )
        }
    }

    private fun generateObstacles(level: Int, w: Int, h: Int) {
        val count = 2 + level
        repeat(count) {
            obstacles.add(
                Obstacle(
                    x = Random.nextFloat() * (w - 100f) + 50f,
                    y = Random.nextFloat() * (h - 600f) + 300f,
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

    /**
     * deltaTime: миллисекунды между кадрами
     */
    fun update(deltaTime: Long) {
        if (_gameState.value.status != GameStatus.PLAYING) return

        val dt = (deltaTime.coerceIn(1L, 33L)).toFloat() / 1000f // сек (стабилизация)
        var score = _gameState.value.score

        movingEggs.forEach { egg ->
            if (!egg.isActive) return@forEach

            // ---------------------------
            // Forces (air)
            // ---------------------------
            egg.vy += gravityPxPerSec2 * dt

            val dragFactor = expDecay(airDragPerSec, dt) // ~e^(-k*dt)
            egg.vx *= dragFactor
            egg.vy *= dragFactor

            // speed clamp (safety)
            clampSpeed(egg)

            // ---------------------------
            // Integrate (predict)
            // ---------------------------
            var newX = egg.x + egg.vx * dt
            var newY = egg.y + egg.vy * dt

            // ---------------------------
            // Wall collisions (left/right/top)
            // ---------------------------
            if (newX < eggRadius) {
                newX = eggRadius
                egg.vx = -egg.vx * wallRestitution
            } else if (newX > screenWidth - eggRadius) {
                newX = screenWidth - eggRadius
                egg.vx = -egg.vx * wallRestitution
            }
            if (newY < eggRadius) {
                newY = eggRadius
                egg.vy = -egg.vy * wallRestitution
            }

            // ---------------------------
            // Terrain collision + rolling/sliding
            // ---------------------------
            val contact = resolveTerrainContact(
                x = newX,
                y = newY,
                radius = eggRadius,
                maxPushOutIters = 6
            )

            if (contact.isTouchingGround) {
                // Place egg on corrected position
                newX = contact.correctedX
                newY = contact.correctedY

                // Split velocity into normal/tangent
                val nx = contact.normalX
                val ny = contact.normalY

                val vn = egg.vx * nx + egg.vy * ny
                val tx = -ny
                val ty = nx
                val vt = egg.vx * tx + egg.vy * ty

                // If moving into ground => remove penetration velocity + small restitution
                val newVn = if (vn < 0f) -vn * restitution else vn
                // Tangential friction (Coulomb-like, but stable)
                val frictionDecel = groundFriction * gravityPxPerSec2 * dt * 0.0008f * 1000f
                val vtAfter =
                    if (abs(vt) <= frictionDecel) 0f else (vt - sign(vt) * frictionDecel)

                // Recompose velocity
                egg.vx = tx * vtAfter + nx * newVn
                egg.vy = ty * vtAfter + ny * newVn

                // Rolling: angular vel tends to match ground tangential speed
                val desiredOmega = -(vtAfter / max(eggRadius, 1f)) * (180f / Math.PI.toFloat()) // deg/s
                egg.angularVelocity = lerp(egg.angularVelocity, desiredOmega, 0.18f)

                // Rolling friction: gradually reduce angular velocity when almost stopped
                val rollDamp = expDecay(rollingFriction, dt)
                egg.angularVelocity *= rollDamp
            } else {
                // In air: keep spin (slightly damp)
                egg.angularVelocity *= expDecay(0.5f, dt)
            }

            // ---------------------------
            // Obstacles
            // ---------------------------
            obstacles.forEach { obs ->
                if (!egg.isActive) return@forEach

                val dx = (newX - obs.x)
                val dy = (newY - obs.y)
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < obs.radius + eggRadius) {
                    when (obs.type) {
                        ObstacleType.SPIKE, ObstacleType.HOLE -> {
                            egg.isActive = false
                        }
                        ObstacleType.STONE -> {
                            // Push out + reflect a bit along normal
                            val nx = if (dist > 0.001f) dx / dist else 1f
                            val ny = if (dist > 0.001f) dy / dist else 0f
                            val penetration = (obs.radius + eggRadius) - dist
                            newX += nx * penetration
                            newY += ny * penetration

                            val vn = egg.vx * nx + egg.vy * ny
                            if (vn < 0f) {
                                egg.vx -= (1f + restitution) * vn * nx
                                egg.vy -= (1f + restitution) * vn * ny
                            }
                        }
                    }
                }
            }

            // ---------------------------
            // Basket
            // ---------------------------
            if (egg.isActive && basketRect.contains(newX, newY)) {
                egg.isActive = false
                score++
            }

            // ---------------------------
            // Commit position + rotation
            // ---------------------------
            egg.x = newX
            egg.y = newY
            egg.angle = wrapAngle(egg.angle + egg.angularVelocity * dt)

            // ---------------------------
            // Death (fell out)
            // ---------------------------
            if (egg.y > screenHeight + 200f) {
                egg.isActive = false
            }
        }

        val newStatus =
            if (score >= _gameState.value.targetScore) GameStatus.WON else GameStatus.PLAYING

        _gameState.value = _gameState.value.copy(
            score = score,
            eggs = movingEggs.filter { it.isActive },
            status = newStatus,
            frameTick = _gameState.value.frameTick + 1
        )
    }

    // ---------------------------
    // Terrain helpers
    // ---------------------------

    private data class TerrainContact(
        val isTouchingGround: Boolean,
        val correctedX: Float,
        val correctedY: Float,
        val normalX: Float,
        val normalY: Float
    )

    /**
     * 1) Быстро проверяем касание (по окружности),
     * 2) если есть контакт — ищем нормаль по градиенту "плотности" вокруг точки,
     * 3) выталкиваем яйцо по нормали несколькими итерациями (стабильнее, чем "newY=oldY").
     */
    private fun resolveTerrainContact(
        x: Float,
        y: Float,
        radius: Float,
        maxPushOutIters: Int
    ): TerrainContact {
        val bmp = terrainBitmap ?: return TerrainContact(false, x, y, 0f, -1f)

        // Quick contact test (multi-sample)
        if (!checkTerrainCollision(x, y, radius)) {
            return TerrainContact(false, x, y, 0f, -1f)
        }

        // Compute normal by sampling density around center (like a tiny SDF-ish gradient)
        val (nx0, ny0) = estimateTerrainNormal(bmp, x, y)
        var nx = nx0
        var ny = ny0
        val nLen = sqrt(nx * nx + ny * ny)
        if (nLen < 0.0001f) {
            // Fallback: push up
            nx = 0f
            ny = -1f
        } else {
            nx /= nLen
            ny /= nLen
        }

        var cx = x
        var cy = y

        // Push out iteratively
        repeat(maxPushOutIters) {
            if (!checkTerrainCollision(cx, cy, radius)) return@repeat
            cx += nx * 2.2f
            cy += ny * 2.2f
        }

        val touching = checkTerrainCollision(cx, cy, radius)
        // If still colliding, brute push up as last resort
        if (touching) {
            var py = cy
            repeat(18) {
                if (!checkTerrainCollision(cx, py, radius)) {
                    cy = py
                    return@repeat
                }
                py -= 2f
            }
        }

        return TerrainContact(
            isTouchingGround = true,
            correctedX = cx,
            correctedY = cy,
            normalX = nx,
            normalY = ny
        )
    }

    private fun estimateTerrainNormal(bmp: Bitmap, x: Float, y: Float): Pair<Float, Float> {
        // Sample "solidness" around point; solid => 1, empty => 0
        val step = 6
        val cx = x.toInt()
        val cy = y.toInt()

        val left = solid01(bmp, cx - step, cy)
        val right = solid01(bmp, cx + step, cy)
        val up = solid01(bmp, cx, cy - step)
        val down = solid01(bmp, cx, cy + step)

        // Gradient points toward increasing solidness, we want push OUT of solid => invert
        val gx = (right - left)
        val gy = (down - up)

        return Pair(-gx, -gy)
    }

    private fun solid01(bmp: Bitmap, x: Int, y: Int): Float {
        if (x < 0 || x >= screenWidth || y < 0 || y >= screenHeight) return 0f
        return try {
            val pixel = bmp.getPixel(x, y)
            if ((pixel ushr 24) > 0) 1f else 0f
        } catch (_: Throwable) {
            0f
        }
    }

    private fun checkTerrainCollision(x: Float, y: Float, radius: Float): Boolean {
        val bmp = terrainBitmap ?: return false

        val points = listOf(
            Offset(0f, radius),
            Offset(radius * 0.85f, radius * 0.35f),
            Offset(-radius * 0.85f, radius * 0.35f),
            Offset(radius, 0f),
            Offset(-radius, 0f),
            Offset(0f, 0f)
        )

        for (p in points) {
            val px = (x + p.x).toInt()
            val py = (y + p.y).toInt()
            if (px < 0 || px >= screenWidth || py < 0 || py >= screenHeight) continue

            try {
                val pixel = bmp.getPixel(px, py)
                if ((pixel ushr 24) > 0) return true
            } catch (_: Throwable) {
                // ignore
            }
        }
        return false
    }

    // ---------------------------
    // Small math helpers
    // ---------------------------

    private fun clampSpeed(egg: Egg) {
        val v2 = egg.vx * egg.vx + egg.vy * egg.vy
        val maxV2 = maxSpeed * maxSpeed
        if (v2 > maxV2) {
            val k = maxSpeed / sqrt(v2)
            egg.vx *= k
            egg.vy *= k
        }
    }

    private fun expDecay(k: Float, dt: Float): Float {
        // ~ e^(-k*dt) but cheap and stable
        return 1f / (1f + k * dt)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun sign(v: Float): Float = if (v >= 0f) 1f else -1f

    private fun wrapAngle(a: Float): Float {
        var v = a % 360f
        if (v < 0f) v += 360f
        return v
    }
}