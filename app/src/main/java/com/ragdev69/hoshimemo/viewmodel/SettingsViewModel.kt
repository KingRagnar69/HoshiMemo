package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ragdev69.hoshimemo.ui.theme.AppTheme
import com.ragdev69.hoshimemo.utils.PrefsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PrefsManager(app)

    val currentTheme: StateFlow<AppTheme> = prefs.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.DARK)

    val currentLanguage: StateFlow<String> = prefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    fun setTheme(theme: AppTheme) = viewModelScope.launch {
        prefs.setTheme(theme)
    }

    fun setLanguage(code: String) = viewModelScope.launch {
        prefs.setLanguage(code)
    }
}