package com.ragdev69.hoshimemo.ui.theme

import androidx.compose.ui.graphics.Color

// ── Secret Theme Enum ─────────────────────────────────────────
enum class SecretTheme(val displayName: String, val emoji: String, val description: String) {
    NONE("None", "", ""),
    GOTHIC("Gothic Noir", "🕸️", "Pure black & white — stark and silent"),
    DRAGON("Dragon's Hoard", "🐉", "Obsidian crypts & blood-red sigils"),
    SCIFI_FANTASY("Void Rift", "🛸", "Neon plasma meets ember & molten gold")
}

// ── GOTHIC NOIR — true black & white, zero color ─────────────
val Gothic_Background    = Color(0xFF000000)   // pure black
val Gothic_Surface       = Color(0xFF111111)
val Gothic_Card          = Color(0xFF1A1A1A)
val Gothic_Primary       = Color(0xFFFFFFFF)   // pure white as primary
val Gothic_Accent        = Color(0xFFAAAAAA)   // mid grey accent
val Gothic_Text          = Color(0xFFFFFFFF)
val Gothic_TextSecondary = Color(0xFF888888)
val Gothic_Border        = Color(0xFF333333)
val Gothic_NavBar        = Color(0xFF000000)

// ── DRAGON'S HOARD — was Gothic: obsidian, blood red, bone ───
val Dragon_Background    = Color(0xFF0D0A0E)
val Dragon_Surface       = Color(0xFF1A1320)
val Dragon_Card          = Color(0xFF231829)
val Dragon_Primary       = Color(0xFF8B0000)   // deep blood red
val Dragon_Accent        = Color(0xFFB5936B)   // aged bone
val Dragon_Text          = Color(0xFFE8DDD0)
val Dragon_TextSecondary = Color(0xFF9A8A7E)
val Dragon_Border        = Color(0xFF4A1020)
val Dragon_NavBar        = Color(0xFF0D0A0E)

// ── VOID RIFT — neon cyan/purple merged with ember orange/gold ─
val SciFi_Background     = Color(0xFF060508)   // near black with warm tint
val SciFi_Surface        = Color(0xFF0E0A14)
val SciFi_Card           = Color(0xFF16101E)
val SciFi_Primary        = Color(0xFF00E5FF)   // neon cyan (dominant)
val SciFi_Accent         = Color(0xFFFFB830)   // molten gold from Dragon
val SciFi_Text           = Color(0xFFE8E0FF)
val SciFi_TextSecondary  = Color(0xFF7060AA)
val SciFi_Border         = Color(0xFF2A1A3A)
val SciFi_NavBar         = Color(0xFF060508)

// ── Color mappings ────────────────────────────────────────────
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