package com.ragdev69.hoshimemo.ui.sound

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.soundDataStore by preferencesDataStore(name = "sound_prefs")

// ── Persists the mute toggle ──────────────────────────────────────────────────
object SoundPrefs {
    private val KEY_MUTED = booleanPreferencesKey("secret_sounds_muted")

    fun getMutedFlow(context: Context): Flow<Boolean> =
        context.soundDataStore.data.map { it[KEY_MUTED] ?: false }

    suspend fun setMuted(context: Context, muted: Boolean) {
        context.soundDataStore.edit { it[KEY_MUTED] = muted }
    }
}

// ── CompositionLocal so any composable can read current theme + mute state ───
data class SoundConfig(
    val secretTheme: com.ragdev69.hoshimemo.ui.theme.SecretTheme =
        com.ragdev69.hoshimemo.ui.theme.SecretTheme.NONE,
    val muted: Boolean = false
)

val LocalSoundConfig = compositionLocalOf { SoundConfig() }