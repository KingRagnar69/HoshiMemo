package com.ragdev69.hoshimemo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ragdev69.hoshimemo.ui.navigation.HoshiNavGraph
import com.ragdev69.hoshimemo.ui.theme.AppTheme
import com.ragdev69.hoshimemo.ui.theme.HoshiMemoTheme
import com.ragdev69.hoshimemo.ui.theme.getHoshiColors
import com.ragdev69.hoshimemo.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    // Apply saved locale before Activity inflates any view
    override fun attachBaseContext(newBase: Context) {
        val savedLang = newBase
            .getSharedPreferences("hoshi_locale", Context.MODE_PRIVATE)
            .getString("lang", "en") ?: "en"
        super.attachBaseContext(HoshiApp.wrapLocale(newBase, savedLang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val currentTheme    by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
            val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()

            // When language changes → persist it then recreate so attachBaseContext re-runs
            var lastAppliedLang by remember { mutableStateOf("") }
            LaunchedEffect(currentLanguage) {
                if (lastAppliedLang.isNotEmpty() && currentLanguage != lastAppliedLang) {
                    HoshiApp.persistLocale(this@MainActivity, currentLanguage)
                    recreate()
                }
                lastAppliedLang = currentLanguage
            }

            HoshiMemoTheme(appTheme = currentTheme) {
                val hoshiColors = getHoshiColors(currentTheme)
                val systemUi    = rememberSystemUiController()

                SideEffect {
                    systemUi.setStatusBarColor(
                        color     = hoshiColors.background,
                        darkIcons = !hoshiColors.isDark
                    )
                    systemUi.setNavigationBarColor(
                        color     = hoshiColors.navBar,
                        darkIcons = !hoshiColors.isDark
                    )
                }

                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(tween(500)) togetherWith fadeOut(tween(400))
                    },
                    label = "splash_to_main"
                ) { splash ->
                    if (splash) {
                        SplashContent(theme = currentTheme)
                    } else {
                        val navController = rememberNavController()
                        HoshiNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashContent(theme: AppTheme) {
    val colors = getHoshiColors(theme)

    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 0.9f,
        targetValue   = 1.1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text     = "⭐",
                fontSize = 64.sp,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text  = "Hoshi Memo",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color      = colors.text
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "✨ Your thoughts, beautifully kept",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textSecondary
                )
            )
        }
    }
}