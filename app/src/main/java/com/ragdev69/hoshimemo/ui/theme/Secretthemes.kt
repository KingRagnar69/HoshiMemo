package com.ragdev69.hoshimemo.ui.theme

import androidx.compose.ui.graphics.Color

// ── Secret Theme Enum ─────────────────────────────────────────
// These are HIDDEN themes — only toggleable from the Secret Screen.
// Do NOT add these to any public settings UI.
enum class SecretTheme(val displayName: String, val emoji: String, val description: String) {
    NONE("None", "", ""),
    GOTHIC("Gothic Noir", "🕸️", "Obsidian crypts & blood-red sigils"),
    DRAGON("Dragon's Hoard", "🐉", "Ember gold & scorched black scales"),
    SCIFI_FANTASY("Void Rift", "🛸", "Neon plasma & deep-space indigo")
}

// ── Secret Theme Colors ───────────────────────────────────────

// GOTHIC — deep charcoal, blood red, bone white
val Gothic_Background    = Color(0xFF0D0A0E)
val Gothic_Surface       = Color(0xFF1A1320)
val Gothic_Card          = Color(0xFF231829)
val Gothic_Primary       = Color(0xFF8B0000)   // deep blood red
val Gothic_Accent        = Color(0xFFB5936B)   // aged bone
val Gothic_Text          = Color(0xFFE8DDD0)
val Gothic_TextSecondary = Color(0xFF9A8A7E)
val Gothic_Border        = Color(0xFF4A1020)
val Gothic_NavBar        = Color(0xFF0D0A0E)

// DRAGON — scorched black, ember orange, molten gold
val Dragon_Background    = Color(0xFF0C0805)
val Dragon_Surface       = Color(0xFF1A110A)
val Dragon_Card          = Color(0xFF251710)
val Dragon_Primary       = Color(0xFFD4500A)   // ember orange
val Dragon_Accent        = Color(0xFFFFB830)   // molten gold
val Dragon_Text          = Color(0xFFF5E6D0)
val Dragon_TextSecondary = Color(0xFFAA8060)
val Dragon_Border        = Color(0xFF4A2210)
val Dragon_NavBar        = Color(0xFF0C0805)

// SCIFI_FANTASY — void black, neon cyan, plasma purple
val SciFi_Background     = Color(0xFF050810)
val SciFi_Surface        = Color(0xFF0A0F1E)
val SciFi_Card           = Color(0xFF0F1628)
val SciFi_Primary        = Color(0xFF00E5FF)   // neon cyan
val SciFi_Accent         = Color(0xFFAA44FF)   // plasma purple
val SciFi_Text           = Color(0xFFD0E8FF)
val SciFi_TextSecondary  = Color(0xFF6080AA)
val SciFi_Border         = Color(0xFF1A2A4A)
val SciFi_NavBar         = Color(0xFF050810)

// ── Color mappings for getHoshiColors ────────────────────────
fun getSecretHoshiColors(theme: SecretTheme): HoshiColors? = when (theme) {
    SecretTheme.NONE -> null
    SecretTheme.GOTHIC -> HoshiColors(
        background    = Gothic_Background,
        surface       = Gothic_Surface,
        card          = Gothic_Card,
        primary       = Gothic_Primary,
        accent        = Gothic_Accent,
        text          = Gothic_Text,
        textSecondary = Gothic_TextSecondary,
        border        = Gothic_Border,
        navBar        = Gothic_NavBar,
        isDark        = true
    )
    SecretTheme.DRAGON -> HoshiColors(
        background    = Dragon_Background,
        surface       = Dragon_Surface,
        card          = Dragon_Card,
        primary       = Dragon_Primary,
        accent        = Dragon_Accent,
        text          = Dragon_Text,
        textSecondary = Dragon_TextSecondary,
        border        = Dragon_Border,
        navBar        = Dragon_NavBar,
        isDark        = true
    )
    SecretTheme.SCIFI_FANTASY -> HoshiColors(
        background    = SciFi_Background,
        surface       = SciFi_Surface,
        card          = SciFi_Card,
        primary       = SciFi_Primary,
        accent        = SciFi_Accent,
        text          = SciFi_Text,
        textSecondary = SciFi_TextSecondary,
        border        = SciFi_Border,
        navBar        = SciFi_NavBar,
        isDark        = true
    )
}