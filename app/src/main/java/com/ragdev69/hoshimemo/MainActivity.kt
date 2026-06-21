package com.ragdev69.hoshimemo

import com.ragdev69.hoshimemo.BuildConfig
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.ragdev69.hoshimemo.ui.navigation.HoshiNavGraph
import com.ragdev69.hoshimemo.ui.theme.AppTheme
import com.ragdev69.hoshimemo.ui.theme.HoshiMemoTheme
import com.ragdev69.hoshimemo.ui.theme.getHoshiColors
import com.ragdev69.hoshimemo.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var interstitialAd: InterstitialAd? = null
    private var isAdShowing    = false

    // Tracks elapsed seconds since app opened (resets on quit)
    private var secondsElapsed = 0
    private val handler        = android.os.Handler(android.os.Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    // Schedule: first ad at 60s, then every 1200s (20 min) after that
    private val firstAdAt      = 60      // seconds
    private val repeatEvery    = 1200    // seconds (20 min)
    private var nextAdAt       = firstAdAt

    override fun attachBaseContext(newBase: Context) {
        val savedLang = newBase
            .getSharedPreferences("hoshi_locale", Context.MODE_PRIVATE)
            .getString("lang", "en") ?: "en"
        super.attachBaseContext(HoshiApp.wrapLocale(newBase, savedLang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── AdMob init ────────────────────────────────────────────────────────
        // Register THIS device as a test device so real-install testing is safe.
        // Replace "YOUR_DEVICE_TEST_ID" with the ID from logcat (see comment below).
        // To find your ID: run the app once, search logcat for "Use RequestConfiguration"
        // it will say something like: Use RequestConfiguration.Builder().setTestDeviceIds(...)
        // Copy that hex string and paste it below.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(
                    if (BuildConfig.DEBUG)
                        listOf(AdRequest.DEVICE_ID_EMULATOR, "YOUR_DEVICE_TEST_ID")
                    else
                        listOf("YOUR_DEVICE_TEST_ID")   // keep your device safe in prod too
                )
                .build()
        )

        MobileAds.initialize(this) {
            loadInterstitialAd()
            startAdTimer()
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val currentTheme    by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
            val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()

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

    override fun onDestroy() {
        super.onDestroy()
        // Stop timer — resets when app is quit
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        secondsElapsed = 0
        nextAdAt = firstAdAt
    }

    // ── Timer: ticks every second, shows ad at the right moments ─────────────
    private fun startAdTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                secondsElapsed++
                if (secondsElapsed >= nextAdAt) {
                    showAdSafely()
                    nextAdAt = secondsElapsed + repeatEvery   // schedule next
                    loadInterstitialAd()                       // preload next one
                }
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(timerRunnable!!, 1000L)
    }

    // ── Interstitial load ─────────────────────────────────────────────────────
    private fun loadInterstitialAd() {
        val adUnitId = if (BuildConfig.DEBUG)
            "ca-app-pub-3940256099942544/1033173712"   // test interstitial
        else
            "ca-app-pub-7539583464292210/9125843250"   // real interstitial

        InterstitialAd.load(
            this,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdShowing    = false   // ready to show again
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdShowing    = false
                }
            }
        )
    }

    private fun showAdSafely() {
        if (!isAdShowing && interstitialAd != null) {
            isAdShowing = true
            interstitialAd?.show(this)
            interstitialAd = null
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