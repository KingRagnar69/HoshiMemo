package com.ragdev69.hoshimemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ── App Theme Enum ────────────────────────────────────────────
enum class AppTheme(val displayName: String, val emoji: String) {
    DARK("Dark", "🌙"),
    FANTASY("Fantasy", "🧚"),
    ANIME("Lightning", "⚡"),
    SAKURA("Sakura", "🌸"),
    COSMIC("Cosmic", "🌌"),
    GREIGE("Greige", "🍂")
}

// ── Theme Colors Data Class ───────────────────────────────────
data class HoshiColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val primary: Color,
    val accent: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val navBar: Color,
    val isDark: Boolean
)

fun getHoshiColors(theme: AppTheme): HoshiColors = when (theme) {
    AppTheme.DARK -> HoshiColors(
        background    = Dark_Background,
        surface       = Dark_Surface,
        card          = Dark_Card,
        primary       = Dark_Primary,
        accent        = Dark_Accent,
        text          = Dark_Text,
        textSecondary = Dark_TextSecondary,
        border        = Dark_Border,
        navBar        = Dark_NavBar,
        isDark        = true
    )
    AppTheme.FANTASY -> HoshiColors(
        background    = Fantasy_Background,
        surface       = Fantasy_Surface,
        card          = Fantasy_Card,
        primary       = Fantasy_Primary,
        accent        = Fantasy_Accent,
        text          = Fantasy_Text,
        textSecondary = Fantasy_TextSecondary,
        border        = Fantasy_Border,
        navBar        = Fantasy_NavBar,
        isDark        = true
    )
    AppTheme.ANIME -> HoshiColors(
        background    = Lightning_Background,
        surface       = Lightning_Surface,
        card          = Lightning_Card,
        primary       = Lightning_Primary,
        accent        = Lightning_Accent,
        text          = Lightning_Text,
        textSecondary = Lightning_TextSecondary,
        border        = Lightning_Border,
        navBar        = Lightning_NavBar,
        isDark        = true
    )
    AppTheme.SAKURA -> HoshiColors(
        background    = Sakura_Background,
        surface       = Sakura_Surface,
        card          = Sakura_Card,
        primary       = Sakura_Primary,
        accent        = Sakura_Accent,
        text          = Sakura_Text,
        textSecondary = Sakura_TextSecondary,
        border        = Sakura_Border,
        navBar        = Sakura_NavBar,
        isDark        = false
    )
    AppTheme.COSMIC -> HoshiColors(
        background    = Cosmic_Background,
        surface       = Cosmic_Surface,
        card          = Cosmic_Card,
        primary       = Cosmic_Primary,
        accent        = Cosmic_Accent,
        text          = Cosmic_Text,
        textSecondary = Cosmic_TextSecondary,
        border        = Cosmic_Border,
        navBar        = Cosmic_NavBar,
        isDark        = true
    )
    AppTheme.GREIGE -> HoshiColors(
        background    = Greige_Background,
        surface       = Greige_Surface,
        card          = Greige_Card,
        primary       = Greige_Primary,
        accent        = Greige_Accent,
        text          = Greige_Text,
        textSecondary = Greige_TextSecondary,
        border        = Greige_Border,
        navBar        = Greige_NavBar,
        isDark        = false
    )
}

// ── Composition Local ─────────────────────────────────────────
val LocalHoshiColors = compositionLocalOf { getHoshiColors(AppTheme.DARK) }
val LocalAppTheme    = compositionLocalOf { AppTheme.DARK }

// ── Main Theme Composable ─────────────────────────────────────
@Composable
fun HoshiMemoTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val hoshiColors = getHoshiColors(appTheme)

    val colorScheme = if (hoshiColors.isDark) {
        darkColorScheme(
            primary      = hoshiColors.primary,
            secondary    = hoshiColors.accent,
            background   = hoshiColors.background,
            surface      = hoshiColors.surface,
            onPrimary    = Color.White,
            onBackground = hoshiColors.text,
            onSurface    = hoshiColors.text
        )
    } else {
        lightColorScheme(
            primary      = hoshiColors.primary,
            secondary    = hoshiColors.accent,
            background   = hoshiColors.background,
            surface      = hoshiColors.surface,
            onPrimary    = Color.White,
            onBackground = hoshiColors.text,
            onSurface    = hoshiColors.text
        )
    }

    CompositionLocalProvider(
        LocalHoshiColors provides hoshiColors,
        LocalAppTheme    provides appTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = HoshiTypography,
            content     = content
        )
    }
}