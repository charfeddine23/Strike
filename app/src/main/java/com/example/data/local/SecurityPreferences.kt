package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("strikepulse_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PATTERN_LOCK_ENABLED = "pattern_lock_enabled"
        private const val KEY_SAVED_PATTERN = "saved_pattern"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_HAPTIC_ENABLED = "haptic_feedback_enabled"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
    }

    var isPatternLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_PATTERN_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PATTERN_LOCK_ENABLED, value).apply()

    var savedPattern: String?
        get() = prefs.getString(KEY_SAVED_PATTERN, null)
        set(value) = prefs.edit().putString(KEY_SAVED_PATTERN, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "ar") ?: "ar"
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()

    var isAutoUpdateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE_ENABLED, value).apply()

    fun verifyPattern(pattern: String): Boolean {
        val stored = savedPattern ?: return false
        return stored == pattern
    }

    fun clearPattern() {
        prefs.edit()
            .remove(KEY_SAVED_PATTERN)
            .putBoolean(KEY_PATTERN_LOCK_ENABLED, false)
            .apply()
    }
}
