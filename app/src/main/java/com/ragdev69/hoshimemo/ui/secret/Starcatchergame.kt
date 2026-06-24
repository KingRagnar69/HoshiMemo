package com.ragdev69.hoshimemo.ui.secret

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// ── Data ──────────────────────────────────────────────────────────────────────
private data class Star(
    val id: Int,
    val x: Float,          // px
    var y: Float,          // px
    val radius: Float,     // px
    val speedPx: Float,    // px per frame
    val color: Color,
    val twinkleOffset: Float
)

private val STAR_COLORS = listOf(
    Color(0xFFFFE066), Color(0xFFFFFFFF), Color(0xFFAADDFF),
    Color(0xFFFFAACC), Color(0xFFCCAAFF), Color(0xFFAAFFCC)
)

private enum class GameState { IDLE, PLAYING, DEAD }

@Composable
fun StarCatcherGame(onDismiss: () -> Unit) {
    val context     = LocalContext.current
    val colors      = LocalHoshiColors.current
    val scope       = rememberCoroutineScope()

    // ── Mutable game state — stored in plain refs to avoid recomposition ──────
    var gameState   by remember { mutableStateOf(GameState.IDLE) }
    var score       by remember { mutableIntStateOf(0) }
    var highScore   by remember { mutableIntStateOf(0) }
    var lives       by remember { mutableIntStateOf(3) }

    // These are mutated inside the game loop without triggering recomposition
    val stars       = remember { mutableStateListOf<Star>() }
    val catcherXRef = remember { mutableFloatStateOf(0f) }   // px, set on first frame
    var canvasW     by remember { mutableFloatStateOf(0f) }
    var canvasH     by remember { mutableFloatStateOf(0f) }
    var frameTime   by remember { mutableLongStateOf(0L) }    // drives Canvas redraw cheaply
    var idCounter   by remember { mutableIntStateOf(0) }
    var frameCount  by remember { mutableIntStateOf(0) }

    // Load high score once
    LaunchedEffect(Unit) {
        SecretThemeManager.getStarHighScoreFlow(context).collect { highScore = it }
    }

    // ── Core game loop — one coroutine, awaitFrame for 60fps sync ─────────────
    LaunchedEffect(gameState) {
        if (gameState != GameState.PLAYING) return@LaunchedEffect

        score      = 0
        lives      = 3
        frameCount = 0
        stars.clear()

        while (gameState == GameState.PLAYING) {
            awaitFrame()                     // suspends until next vsync (~16ms)
            frameCount++

            if (canvasW == 0f || canvasH == 0f) continue

            // Init catcher in centre on first frame
            if (catcherXRef.floatValue == 0f) catcherXRef.floatValue = canvasW / 2f

            // ── Speed scaling: faster every 10 points ─────────────────────────
            val speedMult = 1f + (score / 10) * 0.3f          // +30% per 10pts
            val basePx    = canvasH * 0.006f * speedMult      // ~0.6% screen height/frame

            // ── Spawn: interval shrinks with score ────────────────────────────
            val spawnEvery = maxOf(18, 45 - score * 2)
            if (frameCount % spawnEvery == 0) {
                val r = canvasH * 0.022f + Random.nextFloat() * canvasH * 0.012f
                stars.add(
                    Star(
                        id           = idCounter++,
                        x            = r + Random.nextFloat() * (canvasW - r * 2f),
                        y            = -r,
                        radius       = r,
                        speedPx      = basePx + Random.nextFloat() * canvasH * 0.002f,
                        color        = STAR_COLORS.random(),
                        twinkleOffset = Random.nextFloat()
                    )
                )
            }

            // ── Move stars ────────────────────────────────────────────────────
            val catcherY      = canvasH * 0.88f
            val catcherHalfW  = canvasW * 0.09f
            val toRemove      = mutableListOf<Star>()
            var missedThisFrame = 0

            for (s in stars) {
                s.y += s.speedPx
                when {
                    // Caught
                    s.y >= catcherY - s.radius &&
                            s.y <= catcherY + s.radius &&
                            abs(s.x - catcherXRef.floatValue) <= catcherHalfW + s.radius -> {
                        toRemove.add(s)
                        score++
                    }
                    // Missed — fell off screen
                    s.y > canvasH + s.radius -> {
                        toRemove.add(s)
                        missedThisFrame++
                    }
                }
            }
            stars.removeAll(toRemove.toSet())

            if (missedThisFrame > 0) {
                val newLives = lives - missedThisFrame
                if (newLives <= 0) {
                    lives = 0
                    gameState = GameState.DEAD
                    scope.launch { SecretThemeManager.saveHighScore(context, score) }
                    return@LaunchedEffect
                }
                lives = newLives
            }

            // Trigger one Canvas redraw by bumping frameTime
            frameTime = System.nanoTime()
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050810))
    ) {
        when (gameState) {
            GameState.IDLE -> GameIdleScreen(
                highScore   = highScore,
                accentColor = colors.primary,
                onStart     = { gameState = GameState.PLAYING },
                onBack      = onDismiss
            )

            GameState.PLAYING -> {
                // Single Canvas — redraws each frame via frameTime read
                @Suppress("UNUSED_VARIABLE")
                val frameSnapshot = frameTime   // read so Compose tracks it as a dependency
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                catcherXRef.floatValue = offset.x.coerceIn(
                                    size.width * 0.08f,
                                    size.width * 0.92f
                                )
                            }
                        }
                ) {
                    canvasW = size.width
                    canvasH = size.height

                    val phase = (frameCount % 120) / 120f   // 0..1 twinkle cycle

                    // Background stars (static seed, cheap)
                    drawBgStars(phase)

                    // Falling stars
                    for (s in stars) {
                        val alpha = 0.65f + 0.35f * sin((phase + s.twinkleOffset) * 2f * Math.PI.toFloat()).coerceIn(-1f, 1f)
                        drawCircle(s.color.copy(alpha = alpha.coerceIn(0f,1f)),  radius = s.radius,        center = Offset(s.x, s.y))
                        drawCircle(s.color.copy(alpha = (alpha * 0.25f).coerceIn(0f,1f)), radius = s.radius * 2.4f, center = Offset(s.x, s.y))
                    }

                    // Catcher bar
                    val cx = catcherXRef.floatValue
                    val cy = size.height * 0.88f
                    val hw = size.width * 0.09f
                    drawLine(Color(0xFF00E5FF),                  Offset(cx - hw, cy), Offset(cx + hw, cy), strokeWidth = 7f)
                    drawLine(Color(0xFF00E5FF).copy(alpha = 0.25f), Offset(cx - hw, cy), Offset(cx + hw, cy), strokeWidth = 22f)
                }

                // HUD — only recomposes when score/lives change
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⭐ $score",  color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("❤️".repeat(lives.coerceAtLeast(0)), fontSize = 18.sp)
                }
                Text(
                    "Tap to move catcher",
                    color    = Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                )
            }

            GameState.DEAD -> GameOverScreen(
                score       = score,
                highScore   = highScore,
                accentColor = colors.primary,
                onRestart   = { stars.clear(); gameState = GameState.PLAYING },
                onBack      = onDismiss
            )
        }
    }
}

// ── Background starfield ──────────────────────────────────────────────────────
private fun DrawScope.drawBgStars(phase: Float) {
    val rng = Random(42)
    repeat(90) {
        val x = rng.nextFloat() * size.width
        val y = rng.nextFloat() * size.height
        val r = rng.nextFloat() * 2.8f + 0.5f
        val a = 0.15f + 0.12f * sin((phase + rng.nextFloat()) * 2f * Math.PI.toFloat())
        drawCircle(Color.White.copy(alpha = a.coerceIn(0f, 1f)), radius = r, center = Offset(x, y))
    }
}

// ── Idle screen ───────────────────────────────────────────────────────────────
@Composable
private fun GameIdleScreen(
    highScore: Int, accentColor: Color,
    onStart: () -> Unit, onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌟", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Star Catcher", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Catch falling stars before they vanish.\nThey fall faster as your score grows.",
            color     = Color.White.copy(alpha = 0.6f),
            fontSize  = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (highScore > 0) {
            Spacer(Modifier.height(12.dp))
            Text("Best: $highScore ⭐", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            colors  = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape   = RoundedCornerShape(12.dp)
        ) { Text("Play", color = Color.White, fontSize = 16.sp) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onBack,
            colors  = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape   = RoundedCornerShape(12.dp)
        ) { Text("Back", color = Color.White.copy(alpha = 0.7f)) }
    }
}

// ── Game over screen ──────────────────────────────────────────────────────────
@Composable
private fun GameOverScreen(
    score: Int, highScore: Int, accentColor: Color,
    onRestart: () -> Unit, onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💫", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text("Stars gone", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Score: $score ⭐", color = Color.White, fontSize = 20.sp)
        if (score > 0 && score >= highScore) {
            Text("New best! 🎉", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        } else if (highScore > 0) {
            Text("Best: $highScore ⭐", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRestart,
            colors  = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape   = RoundedCornerShape(12.dp)
        ) { Text("Try again", color = Color.White) }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onBack,
            colors  = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape   = RoundedCornerShape(12.dp)
        ) { Text("Back", color = Color.White.copy(alpha = 0.7f)) }
    }
}