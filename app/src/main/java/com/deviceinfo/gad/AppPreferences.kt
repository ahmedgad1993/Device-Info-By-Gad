package com.deviceinfo.gad

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode

    private val _language = MutableStateFlow(prefs.getString("language", "en") ?: "en")
    val language: StateFlow<String> = _language

    private val _accentColor = MutableStateFlow(prefs.getString("accent_color", "blue") ?: "blue")
    val accentColor: StateFlow<String> = _accentColor

    private val _animationsEnabled = MutableStateFlow(prefs.getBoolean("animations_enabled", true))
    val animationsEnabled: StateFlow<Boolean> = _animationsEnabled

    private val _refreshInterval = MutableStateFlow(prefs.getInt("refresh_interval", 1000))
    val refreshInterval: StateFlow<Int> = _refreshInterval

    fun setThemePreference(theme: String) {
        prefs.edit().putString("theme", theme).apply()
        _themeMode.value = theme
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _language.value = lang
    }

    fun setAccentColor(color: String) {
        prefs.edit().putString("accent_color", color).apply()
        _accentColor.value = color
    }
    
    fun setAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("animations_enabled", enabled).apply()
        _animationsEnabled.value = enabled
    }

    fun setRefreshInterval(intervalMs: Int) {
        prefs.edit().putInt("refresh_interval", intervalMs).apply()
        _refreshInterval.value = intervalMs
    }
}
