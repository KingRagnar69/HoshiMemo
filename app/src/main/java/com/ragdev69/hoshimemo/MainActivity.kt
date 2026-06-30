package com.ragdev69.hoshimemo

import com.ragdev69.hoshimemo.BuildConfig
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
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
import com.ragdev69.hoshimemo.ui.secret.SecretThemeManager
import com.ragdev69.hoshimemo.ui.sound.LocalSoundConfig
import com.ragdev69.hoshimemo.ui.sound.SoundConfig
import com.ragdev69.hoshimemo.ui.sound.SoundManager
import com.ragdev69.hoshimemo.ui.sound.SoundPrefs
import com.ragdev69.hoshimemo.ui.theme.AppTheme
import com.ragdev69.hoshimemo.ui.theme.HoshiColors
import com.ragdev69.hoshimemo.ui.theme.HoshiMemoThemeWithColors
import com.ragdev69.hoshimemo.ui.theme.SecretTheme
import com.ragdev69.hoshimemo.ui.theme.getHoshiColors
import com.ragdev69.hoshimemo.ui.theme.getSecretHoshiColors
import com.ragdev69.hoshimemo.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var interstitialAd: InterstitialAd? = null
    private var isAdShowing    = false

    private var secondsElapsed = 0
    private val handler        = android.os.Handler(android.os.Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private val firstAdAt   = 60
    private val repeatEvery = 1200
    private var nextAdAt    = firstAdAt

    // ── FIX: attachBaseContext wraps the locale on every cold start.
    // This is the key hook that makes saved language survive process death
    // and work on any device — it runs before setContent, so the correct
    // locale is in place before any string resource is resolved.
    override fun attachBaseContext(newBase: Context) {
        val savedLang = newBase
            .getSharedPreferences("hoshi_locale", Context.MODE_PRIVATE)
            .getString("lang", "en") ?: "en"
        super.attachBaseContext(HoshiApp.wrapLocale(newBase, savedLang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(
                    if (BuildConfig.DEBUG)
                        listOf(AdRequest.DEVICE_ID_EMULATOR, "YOUR_DEVICE_TEST_ID")
                    else
                        listOf("YOUR_DEVICE_TEST_ID")
                )
                .build()
        )

        MobileAds.initialize(this) {
            loadInterstitialAd()
            startAdTimer()
        }

        SoundManager.init(this)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val currentTheme    by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
            val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()

            val secretTheme by SecretThemeManager
                .getSecretThemeFlow(this@MainActivity)
                .collectAsState(initial = SecretTheme.NONE)

            LaunchedEffect(currentTheme) {
                if (secretTheme != SecretTheme.NONE) {
                    SecretThemeManager.setSecretTheme(this@MainActivity, SecretTheme.NONE)
                }
            }

            val hoshiColors = getSecretHoshiColors(secretTheme) ?: getHoshiColors(currentTheme)

            val soundMuted by SoundPrefs.getMutedFlow(this@MainActivity)
                .collectAsState(initial = false)

            // ── FIX: Language change handler.
            // Problem on other devices: SettingsViewModel saves to DataStore, but
            // HoshiApp.persistLocale() must write to SharedPrefs ("hoshi_locale")
            // so attachBaseContext can read it on the NEXT cold start.
            // On Android 13+ we ALSO call AppCompatDelegate.setApplicationLocales()
            // which uses the system's per-app language API — this is what makes it
            // work reliably across all Android versions without needing recreate().
            var lastAppliedLang by remember { mutableStateOf("") }
            LaunchedEffect(currentLanguage) {
                if (lastAppliedLang.isNotEmpty() && currentLanguage != lastAppliedLang) {
                    // 1. Persist to SharedPrefs for attachBaseContext on cold start
                    HoshiApp.persistLocale(this@MainActivity, currentLanguage)

                    // 2. Android 13+ per-app language API (works without recreate)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(currentLanguage)
                        )
                    } else {
                        // Android 12 and below: recreate() is required
                        recreate()
                    }
                }
                lastAppliedLang = currentLanguage
            }

            HoshiMemoThemeWithColors(hoshiColors = hoshiColors) {
                CompositionLocalProvider(
                    LocalSoundConfig provides SoundConfig(
                        secretTheme = secretTheme,
                        muted       = soundMuted
                    )
                ) {
                    val systemUi = rememberSystemUiController()
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
                            SplashContent(colors = hoshiColors)
                        } else {
                            val navController = rememberNavController()
                            HoshiNavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable  = null
        secondsElapsed = 0
        nextAdAt       = firstAdAt
        SoundManager.release()
    }

    private fun startAdTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                secondsElapsed++
                if (secondsElapsed >= nextAdAt) {
                    showAdSafely()
                    nextAdAt = secondsElapsed + repeatEvery
                    loadInterstitialAd()
                }
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(timerRunnable!!, 1000L)
    }

    private fun loadInterstitialAd() {
        val adUnitId = if (BuildConfig.DEBUG)
            "ca-app-pub-3940256099942544/1033173712"
        else
            "ca-app-pub-7539583464292210/9125843250"

        InterstitialAd.load(
            this,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdShowing    = false
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
fun SplashContent(colors: HoshiColors) {
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
                text  = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color      = colors.text
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textSecondary
                )
            )
        }
    }
}