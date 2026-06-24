package com.ragdev69.hoshimemo.ui.secret

import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.ui.theme.SecretTheme
import kotlinx.coroutines.launch

// ── Icon alias component names — must match AndroidManifest.xml exactly ───────
// ALL entries are activity-aliases. MainActivity itself has no launcher filter.
// Index 0 = default alias (enabled by default), 1-3 = secret aliases (start disabled)
private val ICON_ALIASES = listOf(
    "com.ragdev69.hoshimemo.MainActivityDefaultIcon",   // 0 = default
    "com.ragdev69.hoshimemo.MainActivityGothicIcon",    // 1 = gothic
    "com.ragdev69.hoshimemo.MainActivityDragonIcon",    // 2 = dragon
    "com.ragdev69.hoshimemo.MainActivitySciFiIcon"      // 3 = sci-fi
)

private val ICON_LABELS = listOf(
    Triple("Default",   "🌙", "The original Hoshi icon"),
    Triple("Gothic",    "🕸️", "Dark & arcane"),
    Triple("Dragon",    "🐉", "Ember & scale"),
    Triple("Void Rift", "🛸", "Neon plasma")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretScreen(
    code: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors  = LocalHoshiColors.current
    val scope   = rememberCoroutineScope()

    val activeSecretTheme by SecretThemeManager.getSecretThemeFlow(context)
        .collectAsState(initial = SecretTheme.NONE)
    val activeIconIndex by SecretThemeManager.getActiveIconFlow(context)
        .collectAsState(initial = 0)

    var currentSection by remember { mutableStateOf(Section.HUB) }

    when (currentSection) {
        Section.STAR_CATCHER  -> StarCatcherGame(onDismiss = { currentSection = Section.HUB })
        Section.FUTURE_LETTER -> FutureLetterScreen(onBack = { currentSection = Section.HUB })
        Section.HUB -> {
            Scaffold(
                containerColor = colors.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✦", color = colors.primary, fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Secret Vault", color = colors.text, fontWeight = FontWeight.Bold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.text)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SecretHeader(code = code, accentColor = colors.primary)

                    // ── Section 1: Secret Themes ──────────────────────────────
                    SectionCard(
                        title      = "Hidden Themes",
                        subtitle   = "Only switchable from here. Invisible everywhere else.",
                        emoji      = "🎨",
                        background = colors.card,
                        border     = colors.border
                    ) {
                        ThemeSelector(
                            activeTheme = activeSecretTheme,
                            accentColor = colors.primary,
                            onSelect    = { theme ->
                                scope.launch { SecretThemeManager.setSecretTheme(context, theme) }
                            }
                        )
                    }

                    // ── Section 2: App Icons ──────────────────────────────────
                    SectionCard(
                        title      = "Secret Icons",
                        subtitle   = "Changes how the app looks on your home screen.",
                        emoji      = "📱",
                        background = colors.card,
                        border     = colors.border
                    ) {
                        IconSelector(
                            activeIndex = activeIconIndex,
                            accentColor = colors.primary,
                            onSelect    = { idx ->
                                scope.launch {
                                    SecretThemeManager.setActiveIcon(context, idx)
                                    switchAppIcon(context as? Activity, idx)
                                }
                            }
                        )
                    }

                    // ── Section 3: Star Catcher ───────────────────────────────
                    SectionCard(
                        title      = "Star Catcher",
                        subtitle   = "Catch falling stars. They get faster. Don't miss.",
                        emoji      = "⭐",
                        background = colors.card,
                        border     = colors.border,
                        onClick    = { currentSection = Section.STAR_CATCHER }
                    ) {
                        Text("Tap to play →", color = colors.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    // ── Section 4: Future Letters ─────────────────────────────
                    SectionCard(
                        title      = "Letters to Future Me",
                        subtitle   = "Write something. Lock it. Read it when the time comes.",
                        emoji      = "✉️",
                        background = colors.card,
                        border     = colors.border,
                        onClick    = { currentSection = Section.FUTURE_LETTER }
                    ) {
                        Text("Open letterbox →", color = colors.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ── Secret Header ─────────────────────────────────────────────────────────────
@Composable
private fun SecretHeader(code: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accentColor.copy(alpha = 0.15f), accentColor.copy(alpha = 0.05f))
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("✦ ✦ ✦", color = accentColor, fontSize = 14.sp, letterSpacing = 6.sp)
            Spacer(Modifier.height(6.dp))
            Text("You found it.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("Code: $code", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
    }
}

// ── Section Card ──────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    emoji: String,
    background: Color,
    border: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val mod = if (onClick != null)
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    else
        Modifier.fillMaxWidth()

    Box(
        modifier = mod
            .background(background, RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

// ── Theme Selector ────────────────────────────────────────────────────────────
@Composable
private fun ThemeSelector(
    activeTheme: SecretTheme,
    accentColor: Color,
    onSelect: (SecretTheme) -> Unit
) {
    val themes = listOf(
        Triple(SecretTheme.GOTHIC,        "Gothic Noir",    "🕸️"),
        Triple(SecretTheme.DRAGON,        "Dragon's Hoard", "🐉"),
        Triple(SecretTheme.SCIFI_FANTASY, "Void Rift",      "🛸")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // "Off" — restores whatever theme is set in Settings
        val noneSelected = activeTheme == SecretTheme.NONE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (noneSelected) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                .border(1.dp, if (noneSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .clickable { onSelect(SecretTheme.NONE) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎨", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Use my Settings theme",
                color      = if (noneSelected) accentColor else Color.White.copy(alpha = 0.8f),
                fontWeight = if (noneSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize   = 14.sp,
                modifier   = Modifier.weight(1f)
            )
            if (noneSelected) Box(Modifier.size(8.dp).background(accentColor, CircleShape))
        }

        themes.forEach { (theme, label, emoji) ->
            val selected = theme == activeTheme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) accentColor.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (selected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(theme) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    color      = if (selected) accentColor else Color.White.copy(alpha = 0.8f),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize   = 14.sp,
                    modifier   = Modifier.weight(1f)
                )
                if (selected) {
                    Box(Modifier.size(8.dp).background(accentColor, CircleShape))
                }
            }
        }
    }
}

// ── Icon Selector ─────────────────────────────────────────────────────────────
@Composable
private fun IconSelector(
    activeIndex: Int,
    accentColor: Color,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ICON_LABELS.forEachIndexed { idx, (label, emoji, desc) ->
            val selected = idx == activeIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) accentColor.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (selected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(idx) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        color      = if (selected) accentColor else Color.White.copy(alpha = 0.85f),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize   = 14.sp
                    )
                    Text(desc, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                }
                if (selected) {
                    Box(Modifier.size(8.dp).background(accentColor, CircleShape))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "⚠ Drop icon images into mipmap in Android Studio. See AndroidManifest for alias names.",
            color    = Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp
        )
    }
}

// ── Icon switching ────────────────────────────────────────────────────────────
// Index 0 = default (real MainActivity). For default we ENABLE MainActivity and
// DISABLE all aliases. For any other index we ENABLE that alias and DISABLE
// everything else INCLUDING MainActivity itself.
private fun switchAppIcon(activity: Activity?, newIndex: Int) {
    activity ?: return
    val pm = activity.packageManager
    // All entries are aliases — enable the selected one, disable the rest.
    // MainActivity itself has no launcher intent-filter so it's never touched.
    ICON_ALIASES.forEachIndexed { idx, alias ->
        val newState = if (idx == newIndex)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(activity, alias),
                newState,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

private enum class Section { HUB, STAR_CATCHER, FUTURE_LETTER }