package com.ragdev69.hoshimemo.ui.secret

import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

data class FallingStar(
    val id: Int,
    val x: Float,          // 0f..1f normalized
    var y: Float,          // 0f..1f normalized, increases downward
    val size: Float,       // radius in dp
    val speed: Float,      // normalized units per tick
    val color: Color,
    val twinkle: Float = Random.nextFloat() // phase offset for twinkle
)

private val STAR_COLORS = listOf(
    Color(0xFFFFE066), Color(0xFFFFFFFF), Color(0xFFAADDFF),
    Color(0xFFFFAACC), Color(0xFFCCAAFF), Color(0xFFAAFFCC)
)

@Composable
fun StarCatcherGame(
    onDismiss: () -> Unit
) {
    val context    = LocalContext.current
    val colors     = LocalHoshiColors.current
    val scope      = rememberCoroutineScope()

    // ── Game state ────────────────────────────────────────────
    var gameState  by remember { mutableStateOf(GameState.IDLE) }   // IDLE, PLAYING, DEAD
    var score      by remember { mutableIntStateOf(0) }
    var highScore  by remember { mutableIntStateOf(0) }
    var lives      by remember { mutableIntStateOf(3) }
    var stars      by remember { mutableStateOf<List<FallingStar>>(emptyList()) }
    var catcherX   by remember { mutableFloatStateOf(0.5f) }        // normalized 0..1
    var tickMs     by remember { mutableLongStateOf(50L) }
    var starIdCounter by remember { mutableIntStateOf(0) }

    // Load high score
    LaunchedEffect(Unit) {
        SecretThemeManager.getStarHighScoreFlow(context).collect { highScore = it }
    }

    // ── Twinkle animation ─────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "twinkle")
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "phase"
    )

    // ── Game loop ─────────────────────────────────────────────
    LaunchedEffect(gameState) {
        if (gameState != GameState.PLAYING) return@LaunchedEffect

        var tick = 0
        while (gameState == GameState.PLAYING) {
            delay(tickMs)
            tick++

            // Spawn a new star every ~20 ticks (scales down with difficulty)
            val spawnInterval = maxOf(8, 20 - score / 5)
            val newStars = if (tick % spawnInterval == 0) {
                val baseSpeed = 0.004f + score * 0.0003f   // gets faster as score rises
                listOf(
                    FallingStar(
                        id    = starIdCounter++,
                        x     = Random.nextFloat() * 0.85f + 0.075f,
                        y     = 0f,
                        size  = Random.nextFloat() * 10f + 10f,
                        speed = baseSpeed + Random.nextFloat() * 0.002f,
                        color = STAR_COLORS.random()
                    )
                )
            } else emptyList()

            // Move stars down
            val movedStars = (stars + newStars).map { it.copy(y = it.y + it.speed) }

            // Catch detection: star within catcher range at bottom
            val catcherWidth = 0.15f
            val catcherY     = 0.88f
            val caught       = movedStars.filter { s ->
                s.y >= catcherY && abs(s.x - catcherX) < catcherWidth / 2f + s.size / 800f
            }
            val missed = movedStars.filter { s -> s.y > 1.0f && s !in caught }

            if (caught.isNotEmpty()) score += caught.size

            val newLives = lives - missed.size
            if (newLives <= 0) {
                lives = 0
                gameState = GameState.DEAD
                scope.launch { SecretThemeManager.saveHighScore(context, score) }
                return@LaunchedEffect
            }
            if (missed.isNotEmpty()) lives = newLives

            stars = movedStars.filterNot { it in caught || it in missed }
        }
    }

    // ── UI ────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050810))
    ) {
        when (gameState) {
            GameState.IDLE -> IdleScreen(
                highScore = highScore,
                accentColor = colors.primary,
                onStart = {
                    score = 0; lives = 3; stars = emptyList(); tickMs = 50L
                    gameState = GameState.PLAYING
                },
                onBack = onDismiss
            )

            GameState.PLAYING -> {
                // Canvas game area
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                catcherX = (offset.x / size.width).coerceIn(0.08f, 0.92f)
                            }
                        }
                ) {
                    // Draw starfield background
                    drawStarfield(twinklePhase)

                    // Draw falling stars
                    stars.forEach { star ->
                        val alpha = 0.7f + 0.3f * kotlin.math.sin(
                            (twinklePhase + star.twinkle) * 2 * Math.PI.toFloat()
                        )
                        drawCircle(
                            color  = star.color.copy(alpha = alpha),
                            radius = star.size,
                            center = Offset(star.x * size.width, star.y * size.height)
                        )
                        // Glow
                        drawCircle(
                            color  = star.color.copy(alpha = alpha * 0.3f),
                            radius = star.size * 2.2f,
                            center = Offset(star.x * size.width, star.y * size.height)
                        )
                    }

                    // Draw catcher
                    val cx     = catcherX * size.width
                    val cy     = 0.88f * size.height
                    val cw     = size.width * 0.15f
                    drawLine(
                        color       = Color(0xFF00E5FF),
                        start       = Offset(cx - cw / 2f, cy),
                        end         = Offset(cx + cw / 2f, cy),
                        strokeWidth = 6f
                    )
                    drawLine(
                        color       = Color(0xFF00E5FF).copy(alpha = 0.3f),
                        start       = Offset(cx - cw / 2f, cy),
                        end         = Offset(cx + cw / 2f, cy),
                        strokeWidth = 18f
                    )
                }

                // HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⭐ $score", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("❤️".repeat(lives.coerceAtLeast(0)), fontSize = 18.sp)
                }

                Text(
                    text = "Tap to move catcher",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                )
            }

            GameState.DEAD -> GameOverScreen(
                score     = score,
                highScore = highScore,
                accentColor = colors.primary,
                onRestart = {
                    score = 0; lives = 3; stars = emptyList()
                    gameState = GameState.PLAYING
                },
                onBack = onDismiss
            )
        }
    }
}

@Composable
private fun IdleScreen(highScore: Int, accentColor: Color, onStart: () -> Unit, onBack: () -> Unit) {
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
            "Catch falling stars before they vanish.\nStars fall faster as your score grows.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (highScore > 0) {
            Spacer(Modifier.height(12.dp))
            Text("Best: $highScore ⭐", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Play", color = Color.White, fontSize = 16.sp) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Back", color = Color.White.copy(alpha = 0.7f)) }
    }
}

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
        if (score >= highScore && score > 0) {
            Text("New best! 🎉", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        } else if (highScore > 0) {
            Text("Best: $highScore ⭐", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Try again", color = Color.White) }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Back", color = Color.White.copy(alpha = 0.7f)) }
    }
}

private fun DrawScope.drawStarfield(phase: Float) {
    // Static background stars
    val rng = Random(42)
    repeat(80) {
        val x = rng.nextFloat() * size.width
        val y = rng.nextFloat() * size.height
        val r = rng.nextFloat() * 2.5f + 0.5f
        val a = 0.2f + 0.15f * kotlin.math.sin((phase + rng.nextFloat()) * 2 * Math.PI.toFloat())
        drawCircle(Color.White.copy(alpha = a), radius = r, center = Offset(x, y))
    }
}

private enum class GameState { IDLE, PLAYING, DEAD }