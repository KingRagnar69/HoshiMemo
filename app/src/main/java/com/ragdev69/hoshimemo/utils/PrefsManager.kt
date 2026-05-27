package com.ragdev69.hoshimemo.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ragdev69.hoshimemo.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hoshi_prefs")

class PrefsManager(private val context: Context) {

    companion object {
        val KEY_THEME       = stringPreferencesKey("theme")
        val KEY_LANGUAGE    = stringPreferencesKey("language")
        val KEY_GRID_LAYOUT = booleanPreferencesKey("grid_layout")
        val KEY_SORT_BY     = stringPreferencesKey("sort_by")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val key = prefs[KEY_THEME] ?: AppTheme.DARK.name
        try { AppTheme.valueOf(key) } catch (e: Exception) { AppTheme.DARK }
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "en"
    }

    val gridLayoutFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GRID_LAYOUT] ?: true
    }

    val sortByFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SORT_BY] ?: "updatedAt"
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setGridLayout(isGrid: Boolean) {
        context.dataStore.edit { it[KEY_GRID_LAYOUT] = isGrid }
    }

    suspend fun setSortBy(sortBy: String) {
        context.dataStore.edit { it[KEY_SORT_BY] = sortBy }
    }
}