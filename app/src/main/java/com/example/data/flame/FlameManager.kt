package com.example.data.flame

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FlameLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val change: Double,
    val resultingScore: Double,
    val reason: String,
    val details: String = ""
)

object FlameManager {
    private const val PREFS_NAME = "strikepulse_flame_prefs"
    private const val KEY_SCORE = "key_flame_score"
    private const val KEY_LAST_INACTIVITY_DATE = "key_last_inactivity_date"
    private const val KEY_LAST_STRIKE_DATE = "key_last_strike_date"
    private const val KEY_FLAME_LOGS = "key_flame_logs_json"
    private const val KEY_STREAK_SHIELD = "key_streak_shield_active"
    private const val KEY_PEAK_SCORE = "key_peak_flame_score"
    private const val KEY_TOTAL_POINTS_GAINED = "key_total_points_gained"
    private const val KEY_TOTAL_POINTS_EXTINGUISHED = "key_total_points_extinguished"
    private const val MAX_LOGS_CAPACITY = 150

    private val _flameScore = MutableStateFlow(45.0) // Initial starting score
    val flameScore: StateFlow<Double> = _flameScore.asStateFlow()

    private val _flameLogs = MutableStateFlow<List<FlameLogEntry>>(emptyList())
    val flameLogs: StateFlow<List<FlameLogEntry>> = _flameLogs.asStateFlow()

    private val _isStreakShieldActive = MutableStateFlow(false)
    val isStreakShieldActive: StateFlow<Boolean> = _isStreakShieldActive.asStateFlow()

    private val _peakScore = MutableStateFlow(45.0)
    val peakScore: StateFlow<Double> = _peakScore.asStateFlow()

    private val _totalPointsGained = MutableStateFlow(0.0)
    val totalPointsGained: StateFlow<Double> = _totalPointsGained.asStateFlow()

    private val _totalPointsExtinguished = MutableStateFlow(0.0)
    val totalPointsExtinguished: StateFlow<Double> = _totalPointsExtinguished.asStateFlow()

    private var isInitialized = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun todayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        val savedScore = prefs.getFloat(KEY_SCORE, 45.0f).toDouble().coerceAtLeast(0.0)
        _flameScore.value = savedScore

        _isStreakShieldActive.value = prefs.getBoolean(KEY_STREAK_SHIELD, false)
        val savedPeak = prefs.getFloat(KEY_PEAK_SCORE, savedScore.toFloat()).toDouble()
        _peakScore.value = maxOf(savedScore, savedPeak)
        _totalPointsGained.value = prefs.getFloat(KEY_TOTAL_POINTS_GAINED, 0.0f).toDouble()
        _totalPointsExtinguished.value = prefs.getFloat(KEY_TOTAL_POINTS_EXTINGUISHED, 0.0f).toDouble()

        val logsJson = prefs.getString(KEY_FLAME_LOGS, null)
        if (!logsJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<FlameLogEntry>()
                val array = JSONArray(logsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        FlameLogEntry(
                            id = obj.optLong("id", System.currentTimeMillis()),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            title = obj.optString("title", ""),
                            change = obj.optDouble("change", 0.0),
                            resultingScore = obj.optDouble("resultingScore", 0.0),
                            reason = obj.optString("reason", "EVENT"),
                            details = obj.optString("details", "")
                        )
                    )
                }
                _flameLogs.value = list
            } catch (_: Exception) {
            }
        }
        isInitialized = true
    }

    fun getScore(context: Context): Double {
        init(context)
        return _flameScore.value
    }

    @Synchronized
    fun setStreakShieldActive(context: Context, active: Boolean) {
        init(context)
        _isStreakShieldActive.value = active
        getPrefs(context).edit().putBoolean(KEY_STREAK_SHIELD, active).apply()

        val title = if (active) "Streak Shield Activated" else "Streak Shield Deactivated"
        val details = if (active) {
            "Inactivity accrual (+3 pts/day) paused during rest period."
        } else {
            "Inactivity accrual resumed."
        }
        logEvent(
            context = context,
            title = title,
            change = 0.0,
            resultingScore = _flameScore.value,
            reason = "STREAK_SHIELD",
            details = details
        )
    }

    @Synchronized
    fun setScore(context: Context, newScore: Double, reason: String = "SIMULATION", details: String = ""): Double {
        init(context)
        val clamped = newScore.coerceIn(0.0, 10000.0)
        val diff = clamped - _flameScore.value
        _flameScore.value = clamped

        if (clamped > _peakScore.value) {
            _peakScore.value = clamped
            getPrefs(context).edit().putFloat(KEY_PEAK_SCORE, clamped.toFloat()).apply()
        }

        if (diff > 0) {
            val updatedGained = _totalPointsGained.value + diff
            _totalPointsGained.value = updatedGained
            getPrefs(context).edit().putFloat(KEY_TOTAL_POINTS_GAINED, updatedGained.toFloat()).apply()
        } else if (diff < 0) {
            val updatedExt = _totalPointsExtinguished.value + (-diff)
            _totalPointsExtinguished.value = updatedExt
            getPrefs(context).edit().putFloat(KEY_TOTAL_POINTS_EXTINGUISHED, updatedExt.toFloat()).apply()
        }

        val prefs = getPrefs(context)
        prefs.edit().putFloat(KEY_SCORE, clamped.toFloat()).apply()

        if (diff != 0.0) {
            logEvent(
                context = context,
                title = "Interactive Score Simulator",
                change = diff,
                resultingScore = clamped,
                reason = reason,
                details = details.ifBlank { "Score manually set to ${String.format(Locale.US, "%.1f", clamped)} pts" }
            )
        }
        return clamped
    }

    @Synchronized
    fun addPoints(context: Context, delta: Double, reason: String, details: String, title: String = ""): Double {
        init(context)
        val old = _flameScore.value
        val updated = (old + delta).coerceIn(0.0, 10000.0)
        _flameScore.value = updated

        if (updated > _peakScore.value) {
            _peakScore.value = updated
            getPrefs(context).edit().putFloat(KEY_PEAK_SCORE, updated.toFloat()).apply()
        }

        val updatedGained = _totalPointsGained.value + delta
        _totalPointsGained.value = updatedGained
        getPrefs(context).edit()
            .putFloat(KEY_SCORE, updated.toFloat())
            .putFloat(KEY_TOTAL_POINTS_GAINED, updatedGained.toFloat())
            .apply()

        val eventTitle = title.ifBlank {
            when (reason) {
                "INACTIVITY" -> "Daily Inactivity Accrual"
                "CONTACT_CALL" -> "Contact Call Detected"
                "STRIKE_DELETED_REFUND" -> "Strike Deleted Refund"
                else -> "Flame Energy Accrual"
            }
        }
        logEvent(context, eventTitle, delta, updated, reason, details)
        return updated
    }

    @Synchronized
    fun deductPoints(context: Context, delta: Double, reason: String, details: String, title: String = ""): Double {
        init(context)
        val old = _flameScore.value
        val updated = (old - delta).coerceIn(0.0, 10000.0)
        val actualDeducted = old - updated
        _flameScore.value = updated

        val updatedExt = _totalPointsExtinguished.value + actualDeducted
        _totalPointsExtinguished.value = updatedExt
        getPrefs(context).edit()
            .putFloat(KEY_SCORE, updated.toFloat())
            .putFloat(KEY_TOTAL_POINTS_EXTINGUISHED, updatedExt.toFloat())
            .apply()

        val eventTitle = title.ifBlank {
            when (reason) {
                "DEFLAME" -> "Deflame Button Action"
                "GIRL" -> "Girl Button Action"
                "STRIKE_DEDUCTION" -> "New Strike Registered"
                "STRIKE_RESTORED_UNDO" -> "Undo Strike Delete"
                else -> "Flame Energy Deduction"
            }
        }
        logEvent(context, eventTitle, -actualDeducted, updated, reason, details)
        return updated
    }

    /**
     * "Deflame" Button: Each tap instantly decreases the total Flame score by 2 points (ensuring score >= 0).
     */
    fun deflame(context: Context): Double {
        return deductPoints(
            context = context,
            delta = 2.0,
            reason = "DEFLAME",
            details = "Manual instant decrease (-2.0 pts)",
            title = "Deflame Button Action"
        )
    }

    /**
     * "Girl" Button: Each tap instantly decreases the total Flame score by 3 points (ensuring score >= 0).
     */
    fun girl(context: Context): Double {
        return deductPoints(
            context = context,
            delta = 3.0,
            reason = "GIRL",
            details = "Manual cooldown decrease (-3.0 pts)",
            title = "Girl Button Action"
        )
    }

    /**
     * Handles incoming contact call: Automatically adds 5 points to total Flame score.
     */
    fun onContactCallDetected(context: Context, contactName: String): Double {
        return addPoints(
            context = context,
            delta = 5.0,
            reason = "CONTACT_CALL",
            details = "Incoming call from linked contact: $contactName (+5.0 pts)",
            title = "Contact Call Detected"
        )
    }

    /**
     * Daily Inactivity Accrual:
     * Automatically adds 3 points to the total Flame score for every single day
     * that passes since the last recorded strike.
     * If Streak Shield is active, accrual is paused and protected.
     */
    @Synchronized
    fun evaluateDailyInactivity(context: Context, latestRecordedStrikeDate: String?): Int {
        init(context)
        val prefs = getPrefs(context)
        val today = todayString()

        val baseDate = prefs.getString(KEY_LAST_INACTIVITY_DATE, null)
            ?: latestRecordedStrikeDate
            ?: today

        val days = daysBetween(baseDate, today).toInt()

        // Check if Streak Shield is currently active
        if (_isStreakShieldActive.value) {
            prefs.edit().putString(KEY_LAST_INACTIVITY_DATE, today).apply()
            if (days > 0) {
                logEvent(
                    context = context,
                    title = "Streak Shield Active",
                    change = 0.0,
                    resultingScore = _flameScore.value,
                    reason = "STREAK_SHIELD",
                    details = "Protected against $days day${if (days > 1) "s" else ""} inactivity accrual"
                )
            }
            return 0
        }

        if (days > 0) {
            val addedPoints = days * 3.0
            addPoints(
                context = context,
                delta = addedPoints,
                reason = "INACTIVITY",
                details = "$days calendar day${if (days > 1) "s" else ""} elapsed without strike (+${addedPoints.toInt()} pts)",
                title = "Daily Inactivity Accrual"
            )
            prefs.edit().putString(KEY_LAST_INACTIVITY_DATE, today).apply()
            return days
        } else {
            if (prefs.getString(KEY_LAST_INACTIVITY_DATE, null) == null) {
                prefs.edit().putString(KEY_LAST_INACTIVITY_DATE, today).apply()
            }
            return 0
        }
    }

    /**
     * Retroactive Entry Rule & Strike Deduction:
     * - If strikeDate < latestDateBeforeInsert: Retroactive entry, NO points or deductions.
     * - If strikeDate >= latestDateBeforeInsert: Deduct points equal to the current BOOST value.
     */
    @Synchronized
    fun onStrikeRegistered(
        context: Context,
        strikeDate: String,
        boost: Double,
        personName: String,
        latestDateBeforeInsert: String?
    ): Boolean {
        init(context)
        val prefs = getPrefs(context)

        // Evaluate whether this entry is retroactive
        val isRetroactive = if (latestDateBeforeInsert.isNullOrBlank()) {
            false
        } else {
            strikeDate < latestDateBeforeInsert
        }

        if (isRetroactive) {
            // Retroactive Entry Rule: DO NOT trigger any Flame points or deductions!
            logEvent(
                context = context,
                title = "Retroactive Strike Ignored",
                change = 0.0,
                resultingScore = _flameScore.value,
                reason = "RETROACTIVE",
                details = "Date ($strikeDate) is before latest ($latestDateBeforeInsert). Protected from flame deduction."
            )
            return false
        } else {
            // New latest strike registered: Deduct points equal to BOOST
            val deduction = boost.coerceAtLeast(0.0)
            deductPoints(
                context = context,
                delta = deduction,
                reason = "STRIKE_DEDUCTION",
                details = "Strike registered for $personName (-${String.format(Locale.US, "%.1f", deduction)} BOOST)",
                title = "New Strike Registered"
            )
            // Update last strike date & reset inactivity baseline date to this strike's date
            prefs.edit()
                .putString(KEY_LAST_STRIKE_DATE, strikeDate)
                .putString(KEY_LAST_INACTIVITY_DATE, strikeDate)
                .apply()
            return true
        }
    }

    /**
     * When a strike is deleted, automatically retrieves its penalty value (BOOST),
     * adds it back to the current Flame Score (Flame Score + penaltyValue),
     * and updates both the database and the UI instantly.
     */
    fun onStrikeDeleted(
        context: Context,
        penaltyValue: Double,
        strikeDate: String,
        personName: String
    ): Double {
        init(context)
        if (penaltyValue <= 0.0) return _flameScore.value
        return addPoints(
            context = context,
            delta = penaltyValue,
            reason = "STRIKE_DELETED_REFUND",
            details = "Penalty refunded for deleted strike ($strikeDate) of $personName (+${String.format(Locale.US, "%.1f", penaltyValue)} BOOST)",
            title = "Strike Deleted Refund"
        )
    }

    /**
     * When a deleted strike is undone/restored.
     */
    fun onStrikeRestoredAfterUndo(
        context: Context,
        penaltyValue: Double,
        personName: String
    ): Double {
        init(context)
        if (penaltyValue <= 0.0) return _flameScore.value
        return deductPoints(
            context = context,
            delta = penaltyValue,
            reason = "STRIKE_RESTORED_UNDO",
            details = "Undo Action: Re-applied Strike for $personName (-${String.format(Locale.US, "%.1f", penaltyValue)} BOOST)",
            title = "Undo Strike Delete"
        )
    }

    @Synchronized
    fun clearLogs(context: Context) {
        init(context)
        _flameLogs.value = emptyList()
        getPrefs(context).edit().remove(KEY_FLAME_LOGS).apply()
    }

    private fun daysBetween(fromDateStr: String, toDateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val from = sdf.parse(fromDateStr) ?: return 0
            val to = sdf.parse(toDateStr) ?: return 0
            val diff = to.time - from.time
            (diff / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
    }

    private fun logEvent(
        context: Context,
        title: String,
        change: Double,
        resultingScore: Double,
        reason: String,
        details: String = ""
    ) {
        val entry = FlameLogEntry(
            id = System.currentTimeMillis() + (0..999).random(),
            timestamp = System.currentTimeMillis(),
            title = title,
            change = change,
            resultingScore = resultingScore,
            reason = reason,
            details = details
        )
        val current = _flameLogs.value.toMutableList()
        current.add(0, entry)
        val trimmed = current.take(MAX_LOGS_CAPACITY)
        _flameLogs.value = trimmed

        try {
            val array = JSONArray()
            for (item in trimmed) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("title", item.title)
                    put("change", item.change)
                    put("resultingScore", item.resultingScore)
                    put("reason", item.reason)
                    put("details", item.details)
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_FLAME_LOGS, array.toString()).apply()
        } catch (_: Exception) {
        }
    }
}
