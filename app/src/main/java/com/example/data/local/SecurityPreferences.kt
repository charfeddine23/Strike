package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppThemeMode
import org.json.JSONObject

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("strikepulse_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FINGERPRINT_LOCK_ENABLED = "fingerprint_lock_enabled"
        private const val LEGACY_KEY_PATTERN_LOCK_ENABLED = "pattern_lock_enabled"
        private const val LEGACY_KEY_SAVED_PATTERN = "saved_pattern"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_HAPTIC_ENABLED = "haptic_feedback_enabled"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        private const val KEY_THEME_MODE = "app_theme_mode"
    }

    init {
        if (prefs.contains(LEGACY_KEY_SAVED_PATTERN) || prefs.contains(LEGACY_KEY_PATTERN_LOCK_ENABLED)) {
            prefs.edit()
                .remove(LEGACY_KEY_SAVED_PATTERN)
                .remove(LEGACY_KEY_PATTERN_LOCK_ENABLED)
                .apply()
        }
    }

    var isFingerprintLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_FINGERPRINT_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FINGERPRINT_LOCK_ENABLED, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "ar") ?: "ar"
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()

    var isAutoUpdateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE_ENABLED, value).apply()

    var themeMode: AppThemeMode
        get() = AppThemeMode.fromPref(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.prefValue))
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.prefValue).apply()

    fun exportSnapshot(): JSONObject {
        return JSONObject().apply {
            put("isFingerprintLockEnabled", isFingerprintLockEnabled)
            put("selectedLanguage", selectedLanguage)
            put("isHapticFeedbackEnabled", isHapticFeedbackEnabled)
            put("isAutoUpdateEnabled", isAutoUpdateEnabled)
            put("themeMode", themeMode.prefValue)
        }
    }

    fun restoreSnapshot(json: JSONObject) {
        val rawLang = json.optString("selectedLanguage", selectedLanguage)
        val lang = if (rawLang in setOf("ar", "fr", "en")) rawLang else selectedLanguage
        val fingerprintEnabled = json.optBoolean("isFingerprintLockEnabled", isFingerprintLockEnabled)
        val haptic = json.optBoolean("isHapticFeedbackEnabled", isHapticFeedbackEnabled)
        val autoUpdate = json.optBoolean("isAutoUpdateEnabled", isAutoUpdateEnabled)
        val restoredThemeMode = AppThemeMode.fromPref(json.optString("themeMode", themeMode.prefValue))

        prefs.edit().apply {
            putString(KEY_APP_LANGUAGE, lang)
            putBoolean(KEY_FINGERPRINT_LOCK_ENABLED, fingerprintEnabled)
            remove(LEGACY_KEY_SAVED_PATTERN)
            remove(LEGACY_KEY_PATTERN_LOCK_ENABLED)
            putBoolean(KEY_HAPTIC_ENABLED, haptic)
            putBoolean(KEY_AUTO_UPDATE_ENABLED, autoUpdate)
            putString(KEY_THEME_MODE, restoredThemeMode.prefValue)
            apply()
        }
    }
}
