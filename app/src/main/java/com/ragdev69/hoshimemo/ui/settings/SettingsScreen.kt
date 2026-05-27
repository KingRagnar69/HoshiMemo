package com.ragdev69.hoshimemo.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragdev69.hoshimemo.ui.theme.*
import com.ragdev69.hoshimemo.utils.LocaleHelper
import com.ragdev69.hoshimemo.utils.Language
import com.ragdev69.hoshimemo.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val colors      = LocalHoshiColors.current
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val currentLang  by viewModel.currentLanguage.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Settings ⚙️",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold, color = colors.text
            ),
            modifier = Modifier.padding(start = 20.dp, top = 52.dp, bottom = 24.dp, end = 20.dp)
        )

        // ── Theme Section ─────────────────────────────────────
        SectionLabel("✨  THEME", colors)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppTheme.values().forEach { theme ->
                ThemeChip(
                    theme    = theme,
                    selected = theme == currentTheme,
                    colors   = colors,
                    onClick  = { viewModel.setTheme(theme) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Language Section ──────────────────────────────────
        SectionLabel("🌍  LANGUAGE", colors)

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LocaleHelper.supportedLanguages.forEach { lang ->
                LanguageRow(
                    language  = lang,
                    selected  = lang.code == currentLang,
                    colors    = colors,
                    onClick   = { viewModel.setLanguage(lang.code) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Version
        Text(
            "Hoshi Memo v1.0  ⭐",
            style    = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 100.dp)
        )
    }
}

@Composable
fun SectionLabel(
    text: String,
    colors: HoshiColors
) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall.copy(
            color         = colors.primary,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
    )
}

@Composable
fun ThemeChip(
    theme: AppTheme,
    selected: Boolean,
    colors: HoshiColors,
    onClick: () -> Unit
) {
    val themeColors = getHoshiColors(theme)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
            .width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(themeColors.primary)
                .then(
                    if (selected) Modifier.border(3.dp, colors.text, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(theme.emoji, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text  = theme.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = if (selected) colors.primary else colors.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        )

        if (selected) {
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
fun LanguageRow(
    language: Language,
    selected: Boolean,
    colors: HoshiColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(language.flag, fontSize = 24.sp)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = language.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color      = colors.text,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            )
            Text(
                text  = language.nativeName,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
        }

        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected",
                tint = colors.primary, modifier = Modifier.size(20.dp))
        }
    }
}