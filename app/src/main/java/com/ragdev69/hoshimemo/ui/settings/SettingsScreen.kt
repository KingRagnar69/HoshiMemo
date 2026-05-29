package com.ragdev69.hoshimemo.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragdev69.hoshimemo.ui.theme.AppTheme
import com.ragdev69.hoshimemo.ui.theme.HoshiColors
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.ui.theme.getHoshiColors
import com.ragdev69.hoshimemo.utils.Language
import com.ragdev69.hoshimemo.utils.LocaleHelper
import com.ragdev69.hoshimemo.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val colors       = LocalHoshiColors.current
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val currentLang  by viewModel.currentLanguage.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(colors.surface, colors.background))
                )
                .padding(start = 20.dp, top = 52.dp, bottom = 20.dp, end = 20.dp)
        ) {
            Column {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = colors.text
                    )
                )
                Text(
                    "Personalize your experience",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Theme Section ─────────────────────────────────────
        SettingsSectionLabel("✨  APPEARANCE", colors)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppTheme.values().forEach { theme ->
                SettingsThemeChip(
                    theme    = theme,
                    selected = theme == currentTheme,
                    colors   = colors,
                    onClick  = { viewModel.setTheme(theme) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Language Section ──────────────────────────────────
        SettingsSectionLabel("🌍  LANGUAGE", colors)
        Spacer(modifier = Modifier.height(4.dp))

        // Currently selected language card
        val selectedLang = LocaleHelper.supportedLanguages.find { it.code == currentLang }
        if (selectedLang != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.20f),
                                colors.accent.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedLang.flag, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedLang.nativeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color      = colors.text,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            "Currently selected",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.primary
                            )
                        )
                    }
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint     = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LocaleHelper.supportedLanguages.forEach { lang ->
                SettingsLanguageRow(
                    language = lang,
                    selected = lang.code == currentLang,
                    colors   = colors,
                    onClick  = { viewModel.setLanguage(lang.code) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Version ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⭐", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Hoshi Memo v1.0",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textSecondary
                    )
                )
            }
        }
    }
}

// ── Private composables — prefixed to avoid any name conflicts ─

@Composable
private fun SettingsSectionLabel(text: String, colors: HoshiColors) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall.copy(
            color         = colors.primary,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        modifier = Modifier.padding(start = 20.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsThemeChip(
    theme: AppTheme,
    selected: Boolean,
    colors: HoshiColors,
    onClick: () -> Unit
) {
    val themeColors = getHoshiColors(theme)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) themeColors.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(10.dp)
            .width(76.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(themeColors.primary, themeColors.accent))
                )
                .then(
                    if (selected) Modifier.border(
                        3.dp,
                        Brush.linearGradient(listOf(colors.text, colors.primary)),
                        CircleShape
                    )
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(theme.emoji, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text  = theme.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = if (selected) colors.primary else colors.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        )

        AnimatedVisibility(
            visible = selected,
            enter   = scaleIn() + fadeIn(),
            exit    = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            )
        }
    }
}

@Composable
private fun SettingsLanguageRow(
    language: Language,
    selected: Boolean,
    colors: HoshiColors,
    onClick: () -> Unit
) {
    // lerp replaces animateColorAsState — no import issues
    val fraction by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label         = "lang_bg"
    )
    val bgColor = lerp(Color.Transparent, colors.primary.copy(alpha = 0.12f), fraction)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(language.flag, fontSize = 22.sp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Native script shown first so users recognise their own language
            Text(
                text  = language.nativeName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color      = colors.text,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                )
            )
            Text(
                text  = language.name,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
        }

        AnimatedVisibility(
            visible = selected,
            enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit    = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint     = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}