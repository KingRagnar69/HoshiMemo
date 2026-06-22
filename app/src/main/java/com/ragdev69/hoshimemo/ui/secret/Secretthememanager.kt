package com.ragdev69.hoshimemo.ui.secret

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ragdev69.hoshimemo.ui.theme.SecretTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.secretDataStore by preferencesDataStore(name = "secret_prefs")

object SecretThemeManager {

    private val KEY_SECRET_THEME   = stringPreferencesKey("secret_theme")
    private val KEY_ACTIVE_ICON    = intPreferencesKey("active_icon")       // 0 = default, 1 = gothic, 2 = dragon, 3 = scifi
    private val KEY_STAR_HIGHSCORE = intPreferencesKey("star_highscore")

    // ── Secret Theme ─────────────────────────────────────────
    fun getSecretThemeFlow(context: Context): Flow<SecretTheme> =
        context.secretDataStore.data.map { prefs ->
            val name = prefs[KEY_SECRET_THEME] ?: SecretTheme.NONE.name
            runCatching { SecretTheme.valueOf(name) }.getOrDefault(SecretTheme.NONE)
        }

    suspend fun setSecretTheme(context: Context, theme: SecretTheme) {
        context.secretDataStore.edit { it[KEY_SECRET_THEME] = theme.name }
    }

    // ── Active Icon ───────────────────────────────────────────
    fun getActiveIconFlow(context: Context): Flow<Int> =
        context.secretDataStore.data.map { prefs -> prefs[KEY_ACTIVE_ICON] ?: 0 }

    suspend fun setActiveIcon(context: Context, iconIndex: Int) {
        context.secretDataStore.edit { it[KEY_ACTIVE_ICON] = iconIndex }
    }

    // ── Star Catcher High Score ───────────────────────────────
    fun getStarHighScoreFlow(context: Context): Flow<Int> =
        context.secretDataStore.data.map { prefs -> prefs[KEY_STAR_HIGHSCORE] ?: 0 }

    suspend fun saveHighScore(context: Context, score: Int) {
        context.secretDataStore.edit { prefs ->
            val current = prefs[KEY_STAR_HIGHSCORE] ?: 0
            if (score > current) prefs[KEY_STAR_HIGHSCORE] = score
        }
    }
}