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

data class StrikeRegistrationResult(
    val penaltyDeducted: Double,
    val ruleApplied: String, // "RETROACTIVE", "SINGLE_STRIKE", "DOUBLE_STRIKE", "MULTI_STRIKE_COMBO", "EXTREME_OVERLOAD"
    val shieldGranted: String = "" // "3_DAYS_MANUAL", "7_DAYS_AUTO", ""
)

data class FlameRecalculationResult(
    val startDate: String,
    val todayDate: String,
    val daysElapsed: Int,
    val inactivityPointsAdded: Double,
    val totalStrikesCount: Int,
    val totalDeductions: Double,
    val resultingScore: Double
)

object FlameManager {
    private const val PREFS_NAME = "strikepulse_flame_prefs"
    private const val KEY_SCORE = "key_flame_score"
    private const val KEY_START_COUNT_DATE = "key_start_count_date"
    private const val KEY_LAST_INACTIVITY_DATE = "key_last_inactivity_date"
    private const val KEY_LAST_STRIKE_DATE = "key_last_strike_date"
    private const val KEY_FLAME_LOGS = "key_flame_logs_json"
    private const val KEY_STREAK_SHIELD = "key_streak_shield_active"
    private const val KEY_ACTIVE_SHIELD_EXPIRES_AT = "key_active_shield_expires_at"
    private const val KEY_AVAILABLE_MANUAL_SHIELDS = "key_available_manual_shields"
    private const val KEY_AVAILABLE_1DAY_SHIELDS = "key_available_1day_shields"
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

    private val _activeShieldExpiresAt = MutableStateFlow<Long?>(null)
    val activeShieldExpiresAt: StateFlow<Long?> = _activeShieldExpiresAt.asStateFlow()

    private val _availableManualShields = MutableStateFlow(0) // 3-Day Shields from Double Strike
    val availableManualShields: StateFlow<Int> = _availableManualShields.asStateFlow()

    private val _available1DayShields = MutableStateFlow(0) // 1-Day Shields (starts at 0, earned via tasks & rewards)
    val available1DayShields: StateFlow<Int> = _available1DayShields.asStateFlow()

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

    private fun format(value: Double): String {
        return if (value % 1.0 == 0.0) "${value.toInt()}" else String.format(Locale.US, "%.1f", value)
    }

    fun isShieldActiveNow(): Boolean {
        val expires = _activeShieldExpiresAt.value
        if (expires != null) {
            val now = System.currentTimeMillis()
            if (now < expires) return true
        }
        return _isStreakShieldActive.value
    }

    fun getActiveShieldRemainingMillis(): Long {
        val expires = _activeShieldExpiresAt.value ?: return 0L
        return (expires - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun formatRemaining(millis: Long): String {
        if (millis <= 0L) return "0s"
        val totalSecs = millis / 1000
        val days = totalSecs / (24 * 3600)
        val hours = (totalSecs % (24 * 3600)) / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${mins}m"
            hours > 0 -> "${hours}h ${mins}m ${secs}s"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        val savedScore = prefs.getFloat(KEY_SCORE, 45.0f).toDouble().coerceAtLeast(0.0)
        _flameScore.value = savedScore

        val now = System.currentTimeMillis()
        val expiresAt = if (prefs.contains(KEY_ACTIVE_SHIELD_EXPIRES_AT)) {
            prefs.getLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, 0L)
        } else null

        if (expiresAt != null && expiresAt > now) {
            _activeShieldExpiresAt.value = expiresAt
            _isStreakShieldActive.value = true
        } else {
            _activeShieldExpiresAt.value = null
            _isStreakShieldActive.value = prefs.getBoolean(KEY_STREAK_SHIELD, false)
            if (expiresAt != null && expiresAt <= now) {
                _isStreakShieldActive.value = false
                prefs.edit().putBoolean(KEY_STREAK_SHIELD, false).remove(KEY_ACTIVE_SHIELD_EXPIRES_AT).apply()
            }
        }

        val hasResetShields = prefs.getBoolean("has_reset_shields_to_zero_v2", false)
        if (!hasResetShields) {
            _availableManualShields.value = 0
            _available1DayShields.value = 0
            prefs.edit()
                .putInt(KEY_AVAILABLE_MANUAL_SHIELDS, 0)
                .putInt(KEY_AVAILABLE_1DAY_SHIELDS, 0)
                .putBoolean("has_reset_shields_to_zero_v2", true)
                .apply()
        } else {
            _availableManualShields.value = prefs.getInt(KEY_AVAILABLE_MANUAL_SHIELDS, 0)
            _available1DayShields.value = prefs.getInt(KEY_AVAILABLE_1DAY_SHIELDS, 0)
        }
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

    @Synchronized
    fun resetForTesting(context: Context) {
        isInitialized = false
        init(context)
    }

    @Synchronized
    fun resetShieldsInventoryToZero(context: Context) {
        init(context)
        _availableManualShields.value = 0
        _available1DayShields.value = 0
        getPrefs(context).edit()
            .putInt(KEY_AVAILABLE_MANUAL_SHIELDS, 0)
            .putInt(KEY_AVAILABLE_1DAY_SHIELDS, 0)
            .apply()
    }

    fun getScore(context: Context): Double {
        init(context)
        return _flameScore.value
    }

    @Synchronized
    fun grantManualShield(context: Context, days: Int = 3, reason: String = "Double Strike Reward"): Int {
        init(context)
        val newCount = _availableManualShields.value + 1
        _availableManualShields.value = newCount
        getPrefs(context).edit().putInt(KEY_AVAILABLE_MANUAL_SHIELDS, newCount).apply()
        return newCount
    }

    @Synchronized
    fun activateManualShield(context: Context, days: Int = 3): Boolean {
        init(context)
        if (_availableManualShields.value <= 0) return false
        val now = System.currentTimeMillis()
        val currentExpires = _activeShieldExpiresAt.value
        val base = if (currentExpires != null && currentExpires > now) currentExpires else now
        val newExpires = base + days * 24 * 3600 * 1000L

        val newCount = (_availableManualShields.value - 1).coerceAtLeast(0)
        _availableManualShields.value = newCount
        _activeShieldExpiresAt.value = newExpires
        _isStreakShieldActive.value = true

        getPrefs(context).edit()
            .putInt(KEY_AVAILABLE_MANUAL_SHIELDS, newCount)
            .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, newExpires)
            .putBoolean(KEY_STREAK_SHIELD, true)
            .apply()

        val expiryStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(newExpires))
        val countdown = formatRemaining(newExpires - now)
        logEvent(
            context = context,
            title = "Manual Streak Shield Activated",
            change = 0.0,
            resultingScore = _flameScore.value,
            reason = "STREAK_SHIELD",
            details = "Manual ${days}-Day Shield activated. Protected until $expiryStr ($countdown countdown remaining)."
        )
        return true
    }

    @Synchronized
    fun grant1DayShield(context: Context, count: Int = 1): Int {
        init(context)
        val newCount = (_available1DayShields.value + count).coerceAtLeast(0)
        _available1DayShields.value = newCount
        getPrefs(context).edit().putInt(KEY_AVAILABLE_1DAY_SHIELDS, newCount).apply()
        return newCount
    }

    @Synchronized
    fun activate1DayShield(context: Context): Boolean {
        init(context)
        if (_available1DayShields.value <= 0) return false
        val now = System.currentTimeMillis()
        val currentExpires = _activeShieldExpiresAt.value
        val base = if (currentExpires != null && currentExpires > now) currentExpires else now
        val newExpires = base + 1 * 24 * 3600 * 1000L

        val newCount = (_available1DayShields.value - 1).coerceAtLeast(0)
        _available1DayShields.value = newCount
        _activeShieldExpiresAt.value = newExpires
        _isStreakShieldActive.value = true

        getPrefs(context).edit()
            .putInt(KEY_AVAILABLE_1DAY_SHIELDS, newCount)
            .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, newExpires)
            .putBoolean(KEY_STREAK_SHIELD, true)
            .apply()

        val expiryStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(newExpires))
        val countdown = formatRemaining(newExpires - now)
        logEvent(
            context = context,
            title = "1-Day Streak Shield Activated",
            change = 0.0,
            resultingScore = _flameScore.value,
            reason = "STREAK_SHIELD",
            details = "1-Day Shield activated. Protected until $expiryStr ($countdown countdown remaining)."
        )
        return true
    }

    @Synchronized
    fun grantAndActivateAutoShield(context: Context, days: Int = 7, reason: String = "Multi-Strike Combo Reward"): Long {
        init(context)
        val now = System.currentTimeMillis()
        val currentExpires = _activeShieldExpiresAt.value
        val base = if (currentExpires != null && currentExpires > now) currentExpires else now
        val newExpires = base + days * 24 * 3600 * 1000L

        _activeShieldExpiresAt.value = newExpires
        _isStreakShieldActive.value = true

        getPrefs(context).edit()
            .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, newExpires)
            .putBoolean(KEY_STREAK_SHIELD, true)
            .apply()

        return newExpires
    }

    @Synchronized
    fun setStreakShieldActive(context: Context, active: Boolean) {
        init(context)
        val now = System.currentTimeMillis()
        if (active) {
            val base = maxOf(now, _activeShieldExpiresAt.value ?: now)
            val expires = base + 3 * 24 * 3600 * 1000L
            _activeShieldExpiresAt.value = expires
            _isStreakShieldActive.value = true
            getPrefs(context).edit()
                .putBoolean(KEY_STREAK_SHIELD, true)
                .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, expires)
                .apply()

            val expiryStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(expires))
            val countdown = formatRemaining(expires - now)
            logEvent(
                context = context,
                title = "Streak Shield Activated",
                change = 0.0,
                resultingScore = _flameScore.value,
                reason = "STREAK_SHIELD",
                details = "Inactivity accrual (+3 pts/day) paused. Shield active until $expiryStr ($countdown remaining)."
            )
        } else {
            _isStreakShieldActive.value = false
            _activeShieldExpiresAt.value = null
            getPrefs(context).edit()
                .putBoolean(KEY_STREAK_SHIELD, false)
                .remove(KEY_ACTIVE_SHIELD_EXPIRES_AT)
                .apply()

            logEvent(
                context = context,
                title = "Streak Shield Deactivated",
                change = 0.0,
                resultingScore = _flameScore.value,
                reason = "STREAK_SHIELD",
                details = "Inactivity accrual resumed (+3 pts/day active)."
            )
        }
    }

    const val MAX_SCORE = 20000.0

    @Synchronized
    fun setScore(context: Context, newScore: Double, reason: String = "SIMULATION", details: String = ""): Double {
        init(context)
        val clamped = newScore.coerceIn(0.0, MAX_SCORE)
        val diff = clamped - _flameScore.value
        _flameScore.value = clamped

        if (clamped > _peakScore.value) {
            _peakScore.value = clamped
            getPrefs(context).edit().putFloat(KEY_PEAK_SCORE, clamped.toFloat()).apply()
        }

        // Check Milestone Peak Title unlock at 20,000 pts
        com.example.data.profile.ProfileManager.checkMilestonePeakUnlock(context, clamped)

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
        // If Shadow-Grip Serum is active, flame score growth rate is reduced by 50%
        val shadowFactor = if (com.example.data.profile.ProfileManager.isShadowSerumActive() && reason == "INACTIVITY") 0.5 else 1.0
        // If Gabriel's Purity Droplet is active, flame score growth is accelerated by 50%
        val gabrielFactor = if (com.example.data.profile.ProfileManager.isGabrielDropletActive() && reason == "INACTIVITY") 1.5 else 1.0
        val effectiveDelta = delta * shadowFactor * gabrielFactor

        val old = _flameScore.value
        val updated = (old + effectiveDelta).coerceIn(0.0, MAX_SCORE)
        _flameScore.value = updated

        if (updated > _peakScore.value) {
            _peakScore.value = updated
            getPrefs(context).edit().putFloat(KEY_PEAK_SCORE, updated.toFloat()).apply()
        }

        // Check Milestone Peak Title unlock at 20,000 pts
        com.example.data.profile.ProfileManager.checkMilestonePeakUnlock(context, updated)

        val updatedGained = _totalPointsGained.value + effectiveDelta
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
        logEvent(context, eventTitle, effectiveDelta, updated, reason, details)
        return updated
    }

    @Synchronized
    fun deductPoints(context: Context, delta: Double, reason: String, details: String, title: String = ""): Double {
        init(context)
        val old = _flameScore.value
        val updated = (old - delta).coerceIn(0.0, MAX_SCORE)
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
     * "Deflame" Button: Each tap instantly decreases the total Flame score by 3 points (ensuring score >= 0).
     * If Pulse Elixir is active, grants +5 extra points (consumes 1 of 3 uses).
     */
    fun deflame(context: Context, delta: Double = 3.0): Double {
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "DEFLAME")
        val pulseElixirBonus = com.example.data.profile.ProfileManager.consumePulseElixirUseIfActive(context)
        val safeDelta = (delta + pulseElixirBonus).coerceAtLeast(0.1)
        val formatted = if (safeDelta % 1.0 == 0.0) "${safeDelta.toInt()}" else String.format(Locale.US, "%.1f", safeDelta)
        val bonusNote = if (pulseElixirBonus > 0) " [Pulse Elixir: +5 pts]" else ""
        return deductPoints(
            context = context,
            delta = safeDelta,
            reason = "DEFLAME",
            details = "Manual instant decrease (-$formatted pts)$bonusNote",
            title = "Deflame Button Action"
        )
    }

    /**
     * "Girl" Button / Custom Points Adjustment: Each tap decreases the total Flame score by [delta] (default 3.0).
     */
    fun girl(context: Context, delta: Double = 3.0): Double {
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "GIRL")
        val safeDelta = delta.coerceAtLeast(0.1)
        val formatted = if (safeDelta % 1.0 == 0.0) "${safeDelta.toInt()}" else String.format(Locale.US, "%.1f", safeDelta)
        return deductPoints(
            context = context,
            delta = safeDelta,
            reason = "GIRL",
            details = "Manual cooldown decrease (-$formatted pts)",
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
        if (isShieldActiveNow()) {
            prefs.edit().putString(KEY_LAST_INACTIVITY_DATE, today).apply()
            val remainingStr = formatRemaining(getActiveShieldRemainingMillis())
            if (days > 0) {
                logEvent(
                    context = context,
                    title = "Streak Shield Active",
                    change = 0.0,
                    resultingScore = _flameScore.value,
                    reason = "STREAK_SHIELD",
                    details = "Protected against $days day${if (days > 1) "s" else ""} inactivity accrual (Countdown remaining: $remainingStr)"
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
     * Multi-Strike Penalty, Shield Rewards, and Extreme Reset Rules:
     * 1. Double Strike Penalty Rule (2 Strikes on exact same calendar day):
     *    - Take BOOST values of both individuals, add together, multiply by 3: (Boost1 + Boost2) * 3
     *    - Deduct directly from total Flame score (score >= 0)
     *    - Grant user a Streak Shield valid for 3 days with manual/optional activation.
     * 2. Multi-Strike Combo Penalty Rule (3 to 5 Strikes on exact same calendar day):
     *    - Add BOOST values of all individuals involved together: Sum(Boosts)
     *    - Multiply sum by exact number of strikes registered that day: Sum(Boosts) * Number of Strikes
     *    - Deduct directly from total Flame score (score >= 0)
     *    - Automatically grant user a Streak Shield valid for 1 week (7 days) with automatic activation.
     * 3. Extreme Overload Reset Rule (6 or more Strikes on exact same calendar day):
     *    - Trigger extreme system overload event
     *    - Completely reset Flame score to 0, freeze flame visuals back to initial static state, restart entirely from zero.
     * 4. Single Strike / Normal Latest Strike:
     *    - Deduct individual's BOOST value.
     */
    @Synchronized
    fun onStrikeRegistered(
        context: Context,
        strikeDate: String,
        boost: Double,
        personName: String,
        latestDateBeforeInsert: String?,
        strikesToday: List<com.example.data.model.StrikeEvent> = emptyList()
    ): StrikeRegistrationResult {
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
            return StrikeRegistrationResult(0.0, "RETROACTIVE")
        }

        val strikesCount = strikesToday.size.coerceAtLeast(1)

        val result = when {
            // Case 4: Extreme Overload Reset Rule (6 or more Strikes on exact same day)
            strikesCount >= 6 -> {
                val oldScore = _flameScore.value
                _flameScore.value = 0.0
                val updatedExt = _totalPointsExtinguished.value + oldScore
                _totalPointsExtinguished.value = updatedExt
                prefs.edit()
                    .putFloat(KEY_SCORE, 0.0f)
                    .putFloat(KEY_TOTAL_POINTS_EXTINGUISHED, updatedExt.toFloat())
                    .apply()

                val namesList = strikesToday.joinToString(", ") { it.individualName }
                val details = "Extreme system overload triggered! $strikesCount strikes registered today ($namesList). Flame score completely reset to 0.0 PTS. Visuals frozen to initial static state. Counting restarts from zero."
                logEvent(
                    context = context,
                    title = "Extreme System Overload (Score Reset to 0)",
                    change = -oldScore,
                    resultingScore = 0.0,
                    reason = "EXTREME_OVERLOAD",
                    details = details
                )
                StrikeRegistrationResult(oldScore, "EXTREME_OVERLOAD")
            }

            // Case 3: Multi-Strike Combo Penalty Rule (3 to 5 Strikes on exact same day)
            strikesCount in 3..5 -> {
                val sumBoosts = strikesToday.sumOf {
                    if (it.performanceValue > 0.0) it.performanceValue else boost.coerceAtLeast(0.1)
                }
                val penalty = sumBoosts * strikesCount
                deductPoints(
                    context = context,
                    delta = penalty,
                    reason = "MULTI_STRIKE_COMBO",
                    details = "",
                    title = "${strikesCount}x Multi-Strike Combo (-${format(penalty)} PTS)"
                )

                // Auto grant & activate 7-day shield
                val expiresAt = grantAndActivateAutoShield(context, days = 7, reason = "Multi-Strike Combo Reward")
                val expiryStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(expiresAt))
                val countdown = formatRemaining(expiresAt - System.currentTimeMillis())
                val namesList = strikesToday.joinToString(", ") {
                    "${it.individualName} (BOOST ${format(if (it.performanceValue > 0.0) it.performanceValue else boost)})"
                }

                val details = "${strikesCount} strikes today: $namesList. Formula: Sum of Boosts (${format(sumBoosts)}) × $strikesCount strikes = ${format(penalty)} PTS deducted. Reward: 7-Day Auto Streak Shield activated (Active until $expiryStr, countdown: $countdown)."

                // Update the log entry details
                val currentLogs = _flameLogs.value.toMutableList()
                if (currentLogs.isNotEmpty() && currentLogs[0].reason == "MULTI_STRIKE_COMBO") {
                    currentLogs[0] = currentLogs[0].copy(details = details)
                    _flameLogs.value = currentLogs
                }

                StrikeRegistrationResult(penalty, "MULTI_STRIKE_COMBO", "7_DAYS_AUTO")
            }

            // Case 2: Double Strike Penalty Rule (2 Strikes on exact same day)
            strikesCount == 2 -> {
                val s1 = strikesToday[0]
                val s2 = strikesToday[1]
                val b1 = if (s1.performanceValue > 0.0) s1.performanceValue else boost.coerceAtLeast(0.1)
                val b2 = if (s2.performanceValue > 0.0) s2.performanceValue else boost.coerceAtLeast(0.1)
                val sumBoosts = b1 + b2
                val penalty = sumBoosts * 3.0

                deductPoints(
                    context = context,
                    delta = penalty,
                    reason = "DOUBLE_STRIKE",
                    details = "",
                    title = "Double Strike Penalty (-${format(penalty)} PTS)"
                )

                // Grant 3-day manual shield
                val availableCount = grantManualShield(context, days = 3, reason = "Double Strike Reward")
                val details = "2 strikes today: ${s1.individualName} (BOOST ${format(b1)}) + ${s2.individualName} (BOOST ${format(b2)}). Formula: (${format(b1)} + ${format(b2)}) × 3 = ${format(penalty)} PTS deducted. Reward: 3-Day Manual Streak Shield unlocked (Available: $availableCount, tap Shield to activate)."

                // Update the log entry details
                val currentLogs = _flameLogs.value.toMutableList()
                if (currentLogs.isNotEmpty() && currentLogs[0].reason == "DOUBLE_STRIKE") {
                    currentLogs[0] = currentLogs[0].copy(details = details)
                    _flameLogs.value = currentLogs
                }

                StrikeRegistrationResult(penalty, "DOUBLE_STRIKE", "3_DAYS_MANUAL")
            }

            // Case 1: Normal Single Strike
            else -> {
                val rawDeduction = boost.coerceAtLeast(0.0)
                deductPoints(
                    context = context,
                    delta = rawDeduction,
                    reason = "STRIKE_DEDUCTION",
                    details = "Strike registered for $personName (-${format(rawDeduction)} BOOST)",
                    title = "New Strike Registered"
                )
                com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "STRIKE")
                StrikeRegistrationResult(rawDeduction, "SINGLE_STRIKE")
            }
        }

        // Update last strike date & reset inactivity baseline date to this strike's date
        prefs.edit()
            .putString(KEY_LAST_STRIKE_DATE, strikeDate)
            .putString(KEY_LAST_INACTIVITY_DATE, strikeDate)
            .apply()

        return result
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

    fun getStreakStartDate(context: Context): String? {
        init(context)
        return getPrefs(context).getString(KEY_START_COUNT_DATE, null)
    }

    /**
     * Preview recalculation from a given start date without modifying persistent state.
     */
    fun previewRecalculation(
        startDateStr: String,
        strikeEvents: List<com.example.data.model.StrikeEvent>
    ): FlameRecalculationResult {
        val today = todayString()
        val daysElapsed = daysBetween(startDateStr, today).toInt().coerceAtLeast(0)
        val inactivityPointsAdded = daysElapsed * 3.0

        // Filter strikes within the range [startDateStr, today]
        val relevantStrikes = strikeEvents.filter { it.date >= startDateStr && it.date <= today }
        val strikesByDate = relevantStrikes.groupBy { it.date }

        var totalDeductions = 0.0
        for ((_, dayStrikes) in strikesByDate) {
            val count = dayStrikes.size
            val dayPenalty = when {
                count >= 6 -> {
                    dayStrikes.sumOf { it.performanceValue.coerceAtLeast(0.1) } * count
                }
                count in 3..5 -> {
                    val sumBoosts = dayStrikes.sumOf { it.performanceValue.coerceAtLeast(0.1) }
                    sumBoosts * count
                }
                count == 2 -> {
                    val b1 = dayStrikes[0].performanceValue.coerceAtLeast(0.1)
                    val b2 = dayStrikes[1].performanceValue.coerceAtLeast(0.1)
                    (b1 + b2) * 3.0
                }
                count == 1 -> {
                    dayStrikes[0].performanceValue.coerceAtLeast(0.1)
                }
                else -> 0.0
            }
            totalDeductions += dayPenalty
        }

        val resultingScore = (inactivityPointsAdded - totalDeductions).coerceIn(0.0, MAX_SCORE)

        return FlameRecalculationResult(
            startDate = startDateStr,
            todayDate = today,
            daysElapsed = daysElapsed,
            inactivityPointsAdded = inactivityPointsAdded,
            totalStrikesCount = relevantStrikes.size,
            totalDeductions = totalDeductions,
            resultingScore = resultingScore
        )
    }

    /**
     * Recalculates Flame Score starting from [startDateStr] up to today:
     * - Adds +3.0 points daily inactivity for each calendar day elapsed since start date.
     * - Accounts for all strikes registered in that period, deducting their multi-strike / single-strike penalties.
     * - Updates database preferences, peak score, and logs the action in the Flame log.
     */
    @Synchronized
    fun recalculateFromStartDate(
        context: Context,
        startDateStr: String,
        strikeEvents: List<com.example.data.model.StrikeEvent>
    ): FlameRecalculationResult {
        init(context)
        val result = previewRecalculation(startDateStr, strikeEvents)
        val oldScore = _flameScore.value
        val newScore = result.resultingScore
        val diff = newScore - oldScore

        _flameScore.value = newScore
        if (newScore > _peakScore.value) {
            _peakScore.value = newScore
            getPrefs(context).edit().putFloat(KEY_PEAK_SCORE, newScore.toFloat()).apply()
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

        getPrefs(context).edit()
            .putFloat(KEY_SCORE, newScore.toFloat())
            .putString(KEY_START_COUNT_DATE, startDateStr)
            .putString(KEY_LAST_INACTIVITY_DATE, result.todayDate)
            .apply()

        val details = "تاريخ البداية: $startDateStr | الأيام المنقضية: ${result.daysElapsed} (+${result.inactivityPointsAdded.toInt()} نقطة) | السترايكات: ${result.totalStrikesCount} (-${format(result.totalDeductions)} نقطة)"
        logEvent(
            context = context,
            title = "إعادة حساب تاريخ بداية العد",
            change = diff,
            resultingScore = newScore,
            reason = "START_DATE_RECALCULATION",
            details = details
        )

        return result
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
