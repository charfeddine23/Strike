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
    val details: String = "",
    val isRetroactiveEstimate: Boolean = false,
    val actionCount: Int = 1
)

data class StrikeRegistrationResult(
    val penaltyDeducted: Double,
    val ruleApplied: String, // "RETROACTIVE", "SINGLE_STRIKE", "DOUBLE_STRIKE", "MULTI_STRIKE_COMBO", "EXTREME_OVERLOAD"
    val shieldGranted: String = "" // "3_DAYS_MANUAL", "7_DAYS_AUTO", ""
)

data class DateRangeConfig(
    val startDate: String,
    val endDate: String,
    val isCustom: Boolean = false
)

data class StreakRecalculationConfig(
    val globalStartDate: String,
    val globalEndDate: String,
    val inactivityRange: DateRangeConfig? = null,
    val strikesRange: DateRangeConfig? = null,
    val girlRange: DateRangeConfig? = null,
    val deflameRange: DateRangeConfig? = null,
    val girlPerMonth: Int = 0,
    val deflamePerMonth: Int = 0
)

data class FlameRecalculationResult(
    val startDate: String,
    val todayDate: String,
    val endDate: String = todayDate,
    val daysElapsed: Int,
    val inactivityStartDate: String = startDate,
    val inactivityEndDate: String = endDate,
    val inactivityPointsAdded: Double,
    val strikesStartDate: String = startDate,
    val strikesEndDate: String = endDate,
    val totalStrikesCount: Int,
    val totalDeductions: Double,
    val girlStartDate: String = startDate,
    val girlEndDate: String = endDate,
    val girlDaysElapsed: Int = daysElapsed,
    val girlPerMonth: Int = 0,
    val girlPressesCount: Int = 0,
    val girlPointsDeducted: Double = 0.0,
    val deflameStartDate: String = startDate,
    val deflameEndDate: String = endDate,
    val deflameDaysElapsed: Int = daysElapsed,
    val deflamePerMonth: Int = 0,
    val deflamePressesCount: Int = 0,
    val deflamePointsDeducted: Double = 0.0,
    val resultingScore: Double,
    val estimatedPeakScore: Double = resultingScore,
    val totalExtinguishedPoints: Double = totalDeductions + girlPointsDeducted + deflamePointsDeducted
)

enum class ActiveShieldType(val labelAr: String, val labelEn: String) {
    NONE("لا يوجد درع نشط", "No Active Shield"),
    SHIELD_24H("درع الـ 24 ساعة", "24h Shield"),
    SHIELD_48H("درع الـ 48 ساعة", "48h Shield"),
    SHIELD_72H("درع الـ 72 ساعة", "72h Shield"),
    SHIELD_7D_AUTO("درع الـ 7 أيام التلقائي", "7-Day Auto Shield")
}

object FlameManager {
    private const val PREFS_NAME = "strikepulse_flame_prefs"
    private const val KEY_SCORE = "key_flame_score"
    private const val KEY_START_COUNT_DATE = "key_start_count_date"
    private const val KEY_END_COUNT_DATE = "key_end_count_date"
    private const val KEY_PER_ITEM_DATE_RANGES = "key_per_item_date_ranges_json"
    private const val KEY_LAST_INACTIVITY_DATE = "key_last_inactivity_date"
    private const val KEY_LAST_STRIKE_DATE = "key_last_strike_date"
    private const val KEY_FLAME_LOGS = "key_flame_logs_json"
    private const val KEY_STREAK_SHIELD = "key_streak_shield_active"
    private const val KEY_ACTIVE_SHIELD_EXPIRES_AT = "key_active_shield_expires_at"
    private const val KEY_ACTIVE_SHIELD_TYPE = "key_active_shield_type"
    private const val KEY_AVAILABLE_MANUAL_SHIELDS = "key_available_manual_shields"
    private const val KEY_AVAILABLE_48H_SHIELDS = "key_available_48h_shields"
    private const val KEY_AVAILABLE_1DAY_SHIELDS = "key_available_1day_shields"
    private const val KEY_SEVEN_DAY_AUTO_TRIGGERS = "key_seven_day_auto_triggers"
    private const val KEY_PEAK_SCORE = "key_peak_flame_score"
    private const val KEY_TOTAL_POINTS_GAINED = "key_total_points_gained"
    private const val KEY_TOTAL_POINTS_EXTINGUISHED = "key_total_points_extinguished"
    private const val KEY_GIRL_PER_MONTH = "key_start_date_girl_per_month"
    private const val KEY_DEFLAME_PER_MONTH = "key_start_date_deflame_per_month"
    private const val KEY_GIRL_DEDUCTION_POINTS = "key_girl_deduction_points"
    private const val KEY_GIRL_DEDUCTION_POINTS_BITS = "key_girl_deduction_points_bits"
    private const val KEY_SCORE_BITS = "key_flame_score_bits"
    private const val KEY_PEAK_SCORE_BITS = "key_peak_flame_score_bits"
    private const val KEY_TOTAL_POINTS_GAINED_BITS = "key_total_points_gained_bits"
    private const val KEY_TOTAL_POINTS_EXTINGUISHED_BITS = "key_total_points_extinguished_bits"

    private val _flameScore = MutableStateFlow(45.0) // Initial starting score
    val flameScore: StateFlow<Double> = _flameScore.asStateFlow()

    private val _girlDeductionPoints = MutableStateFlow(3.0)
    val girlDeductionPoints: StateFlow<Double> = _girlDeductionPoints.asStateFlow()

    private val _flameLogs = MutableStateFlow<List<FlameLogEntry>>(emptyList())
    val flameLogs: StateFlow<List<FlameLogEntry>> = _flameLogs.asStateFlow()

    private val _isStreakShieldActive = MutableStateFlow(false)
    val isStreakShieldActive: StateFlow<Boolean> = _isStreakShieldActive.asStateFlow()

    private val _activeShieldExpiresAt = MutableStateFlow<Long?>(null)
    val activeShieldExpiresAt: StateFlow<Long?> = _activeShieldExpiresAt.asStateFlow()

    private val _activeShieldType = MutableStateFlow(ActiveShieldType.NONE)
    val activeShieldType: StateFlow<ActiveShieldType> = _activeShieldType.asStateFlow()

    private val _availableManualShields = MutableStateFlow(0) // 72h (3-Day) Shields from Double Strike & Rewards
    val availableManualShields: StateFlow<Int> = _availableManualShields.asStateFlow()

    private val _available48hShields = MutableStateFlow(0) // 48h (2-Day) Shields earned via rewards
    val available48hShields: StateFlow<Int> = _available48hShields.asStateFlow()

    private val _available1DayShields = MutableStateFlow(0) // 24h (1-Day) Shields earned via tasks & rewards
    val available1DayShields: StateFlow<Int> = _available1DayShields.asStateFlow()

    private val _sevenDayAutoShieldCount = MutableStateFlow(0) // Total 7-day automated shields triggered
    val sevenDayAutoShieldCount: StateFlow<Int> = _sevenDayAutoShieldCount.asStateFlow()

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

    private fun sanitizeScore(value: Double): Double {
        return if (!value.isFinite()) 0.0 else value.coerceAtLeast(0.0)
    }

    private fun saveDouble(
        editor: SharedPreferences.Editor,
        floatKey: String,
        bitsKey: String,
        value: Double
    ): SharedPreferences.Editor {
        val safe = sanitizeScore(value)
        val floatSafe = safe.coerceAtMost(Float.MAX_VALUE.toDouble()).toFloat()
        return editor
            .putFloat(floatKey, floatSafe)
            .putLong(bitsKey, java.lang.Double.doubleToRawLongBits(safe))
    }

    private fun loadDouble(
        prefs: SharedPreferences,
        floatKey: String,
        bitsKey: String,
        defaultValue: Double
    ): Double {
        val raw = if (prefs.contains(bitsKey)) {
            java.lang.Double.longBitsToDouble(prefs.getLong(bitsKey, java.lang.Double.doubleToRawLongBits(defaultValue)))
        } else {
            prefs.getFloat(floatKey, defaultValue.toFloat()).toDouble()
        }
        return sanitizeScore(raw)
    }

    private fun format(value: Double): String {
        return if (value % 1.0 == 0.0 && value <= Long.MAX_VALUE.toDouble()) {
            "${value.toLong()}"
        } else {
            String.format(Locale.US, "%.1f", value)
        }
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
        val savedScore = loadDouble(prefs, KEY_SCORE, KEY_SCORE_BITS, 45.0)
        _flameScore.value = savedScore

        val now = System.currentTimeMillis()
        val expiresAt = if (prefs.contains(KEY_ACTIVE_SHIELD_EXPIRES_AT)) {
            prefs.getLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, 0L)
        } else null

        if (expiresAt != null && expiresAt > now) {
            _activeShieldExpiresAt.value = expiresAt
            _isStreakShieldActive.value = true
            val savedTypeName = prefs.getString(KEY_ACTIVE_SHIELD_TYPE, ActiveShieldType.SHIELD_72H.name)
            _activeShieldType.value = try {
                ActiveShieldType.valueOf(savedTypeName ?: ActiveShieldType.SHIELD_72H.name)
            } catch (_: Exception) {
                ActiveShieldType.SHIELD_72H
            }
        } else {
            _activeShieldExpiresAt.value = null
            val wasActive = prefs.getBoolean(KEY_STREAK_SHIELD, false)
            if (expiresAt != null && expiresAt <= now) {
                _isStreakShieldActive.value = false
                _activeShieldType.value = ActiveShieldType.NONE
                prefs.edit()
                    .putBoolean(KEY_STREAK_SHIELD, false)
                    .remove(KEY_ACTIVE_SHIELD_EXPIRES_AT)
                    .remove(KEY_ACTIVE_SHIELD_TYPE)
                    .apply()
            } else {
                _isStreakShieldActive.value = wasActive
                _activeShieldType.value = if (wasActive) {
                    val savedTypeName = prefs.getString(KEY_ACTIVE_SHIELD_TYPE, ActiveShieldType.SHIELD_72H.name)
                    try {
                        ActiveShieldType.valueOf(savedTypeName ?: ActiveShieldType.SHIELD_72H.name)
                    } catch (_: Exception) {
                        ActiveShieldType.SHIELD_72H
                    }
                } else {
                    ActiveShieldType.NONE
                }
            }
        }

        val hasResetShields = prefs.getBoolean("has_reset_shields_to_zero_v2", false)
        if (!hasResetShields) {
            _availableManualShields.value = 0
            _available48hShields.value = 0
            _available1DayShields.value = 0
            _sevenDayAutoShieldCount.value = 0
            prefs.edit()
                .putInt(KEY_AVAILABLE_MANUAL_SHIELDS, 0)
                .putInt(KEY_AVAILABLE_48H_SHIELDS, 0)
                .putInt(KEY_AVAILABLE_1DAY_SHIELDS, 0)
                .putInt(KEY_SEVEN_DAY_AUTO_TRIGGERS, 0)
                .putBoolean("has_reset_shields_to_zero_v2", true)
                .apply()
        } else {
            _availableManualShields.value = prefs.getInt(KEY_AVAILABLE_MANUAL_SHIELDS, 0)
            _available48hShields.value = prefs.getInt(KEY_AVAILABLE_48H_SHIELDS, 0)
            _available1DayShields.value = prefs.getInt(KEY_AVAILABLE_1DAY_SHIELDS, 0)
            _sevenDayAutoShieldCount.value = prefs.getInt(KEY_SEVEN_DAY_AUTO_TRIGGERS, 0)
        }
        val savedPeak = loadDouble(prefs, KEY_PEAK_SCORE, KEY_PEAK_SCORE_BITS, savedScore)
        _peakScore.value = maxOf(savedScore, savedPeak)
        _totalPointsGained.value = loadDouble(prefs, KEY_TOTAL_POINTS_GAINED, KEY_TOTAL_POINTS_GAINED_BITS, 0.0)
        _totalPointsExtinguished.value = loadDouble(prefs, KEY_TOTAL_POINTS_EXTINGUISHED, KEY_TOTAL_POINTS_EXTINGUISHED_BITS, 0.0)
        _girlDeductionPoints.value = loadDouble(
            prefs,
            KEY_GIRL_DEDUCTION_POINTS,
            KEY_GIRL_DEDUCTION_POINTS_BITS,
            3.0
        ).coerceAtLeast(0.1)

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
                            details = obj.optString("details", ""),
                            isRetroactiveEstimate = obj.optBoolean("isRetroactiveEstimate", false),
                            actionCount = obj.optInt("actionCount", 1).coerceAtLeast(1)
                        )
                    )
                }
                _flameLogs.value = list
            } catch (_: Exception) {
                _flameLogs.value = emptyList()
            }
        } else {
            _flameLogs.value = emptyList()
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
        _available48hShields.value = 0
        _available1DayShields.value = 0
        _sevenDayAutoShieldCount.value = 0
        _activeShieldType.value = ActiveShieldType.NONE
        getPrefs(context).edit()
            .putInt(KEY_AVAILABLE_MANUAL_SHIELDS, 0)
            .putInt(KEY_AVAILABLE_48H_SHIELDS, 0)
            .putInt(KEY_AVAILABLE_1DAY_SHIELDS, 0)
            .putInt(KEY_SEVEN_DAY_AUTO_TRIGGERS, 0)
            .remove(KEY_ACTIVE_SHIELD_TYPE)
            .apply()
    }

    /**
     * Literally resets all Flame scores, shields, stats, dates, and history logs back to 0.
     */
    @Synchronized
    fun resetAllLevelsToZero(context: Context) {
        init(context)
        val today = todayString()
        _flameScore.value = 0.0
        _peakScore.value = 0.0
        _totalPointsGained.value = 0.0
        _totalPointsExtinguished.value = 0.0
        _flameLogs.value = emptyList()
        _isStreakShieldActive.value = false
        _activeShieldExpiresAt.value = null
        _activeShieldType.value = ActiveShieldType.NONE
        _availableManualShields.value = 0
        _available48hShields.value = 0
        _available1DayShields.value = 0
        _sevenDayAutoShieldCount.value = 0
        _girlDeductionPoints.value = 3.0

        getPrefs(context).edit().apply {
            saveDouble(this, KEY_SCORE, KEY_SCORE_BITS, 0.0)
            saveDouble(this, KEY_PEAK_SCORE, KEY_PEAK_SCORE_BITS, 0.0)
            saveDouble(this, KEY_TOTAL_POINTS_GAINED, KEY_TOTAL_POINTS_GAINED_BITS, 0.0)
            saveDouble(this, KEY_TOTAL_POINTS_EXTINGUISHED, KEY_TOTAL_POINTS_EXTINGUISHED_BITS, 0.0)
            remove(KEY_FLAME_LOGS)
            putBoolean(KEY_STREAK_SHIELD, false)
            remove(KEY_ACTIVE_SHIELD_EXPIRES_AT)
            remove(KEY_ACTIVE_SHIELD_TYPE)
            putInt(KEY_AVAILABLE_MANUAL_SHIELDS, 0)
            putInt(KEY_AVAILABLE_48H_SHIELDS, 0)
            putInt(KEY_AVAILABLE_1DAY_SHIELDS, 0)
            putInt(KEY_SEVEN_DAY_AUTO_TRIGGERS, 0)
            putBoolean("has_reset_shields_to_zero_v2", true)
            remove(KEY_START_COUNT_DATE)
            remove(KEY_END_COUNT_DATE)
            remove(KEY_PER_ITEM_DATE_RANGES)
            remove(KEY_LAST_STRIKE_DATE)
            remove(KEY_GIRL_PER_MONTH)
            remove(KEY_DEFLAME_PER_MONTH)
            remove(KEY_GIRL_DEDUCTION_POINTS)
            remove(KEY_GIRL_DEDUCTION_POINTS_BITS)
            putString(KEY_LAST_INACTIVITY_DATE, today)
            apply()
        }
    }

    fun getScore(context: Context): Double {
        init(context)
        return _flameScore.value
    }

    @Synchronized
    fun grantManualShield(
        context: Context,
        days: Int = 3,
        count: Int = 1,
        reason: String = "Double Strike Reward"
    ): Int {
        init(context)
        val newCount = (_availableManualShields.value + count.coerceAtLeast(1)).coerceAtLeast(0)
        _availableManualShields.value = newCount
        getPrefs(context).edit().putInt(KEY_AVAILABLE_MANUAL_SHIELDS, newCount).apply()
        return newCount
    }

    @Synchronized
    fun activateManualShield(context: Context, days: Int = 3, notifyRoadmapTask: Boolean = true): Boolean {
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
        _activeShieldType.value = ActiveShieldType.SHIELD_72H

        getPrefs(context).edit()
            .putInt(KEY_AVAILABLE_MANUAL_SHIELDS, newCount)
            .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, newExpires)
            .putBoolean(KEY_STREAK_SHIELD, true)
            .putString(KEY_ACTIVE_SHIELD_TYPE, ActiveShieldType.SHIELD_72H.name)
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
        if (notifyRoadmapTask) {
            com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "SHIELD")
        }
        return true
    }

    @Synchronized
    fun grant48hShield(context: Context, count: Int = 1): Int {
        init(context)
        val newCount = (_available48hShields.value + count).coerceAtLeast(0)
        _available48hShields.value = newCount
        getPrefs(context).edit().putInt(KEY_AVAILABLE_48H_SHIELDS, newCount).apply()
        return newCount
    }

    @Synchronized
    fun activate48hShield(context: Context, notifyRoadmapTask: Boolean = true): Boolean {
        init(context)
        if (_available48hShields.value <= 0) return false
        val now = System.currentTimeMillis()
        val currentExpires = _activeShieldExpiresAt.value
        val base = if (currentExpires != null && currentExpires > now) currentExpires else now
        val newExpires = base + 2 * 24 * 3600 * 1000L

        val newCount = (_available48hShields.value - 1).coerceAtLeast(0)
        _available48hShields.value = newCount
        _activeShieldExpiresAt.value = newExpires
        _isStreakShieldActive.value = true
        _activeShieldType.value = ActiveShieldType.SHIELD_48H

        getPrefs(context).edit()
            .putInt(KEY_AVAILABLE_48H_SHIELDS, newCount)
            .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, newExpires)
            .putBoolean(KEY_STREAK_SHIELD, true)
            .putString(KEY_ACTIVE_SHIELD_TYPE, ActiveShieldType.SHIELD_48H.name)
            .apply()

        val expiryStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(newExpires))
        val countdown = formatRemaining(newExpires - now)
        logEvent(
            context = context,
            title = "48-Hour Streak Shield Activated",
            change = 0.0,
            resultingScore = _flameScore.value,
            reason = "STREAK_SHIELD",
            details = "48-Hour Shield activated. Protected until $expiryStr ($countdown countdown remaining)."
        )
        if (notifyRoadmapTask) {
            com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "SHIELD")
        }
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
    fun activate1DayShield(context: Context, notifyRoadmapTask: Boolean = true): Boolean {
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
        _activeShieldType.value = ActiveShieldType.SHIELD_24H

        getPrefs(context).edit()
            .putInt(KEY_AVAILABLE_1DAY_SHIELDS, newCount)
            .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, newExpires)
            .putBoolean(KEY_STREAK_SHIELD, true)
            .putString(KEY_ACTIVE_SHIELD_TYPE, ActiveShieldType.SHIELD_24H.name)
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
        if (notifyRoadmapTask) {
            com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "SHIELD")
        }
        return true
    }

    @Synchronized
    fun grantAndActivateAutoShield(context: Context, days: Int = 7, reason: String = "Multi-Strike Combo Reward"): Long {
        init(context)
        val now = System.currentTimeMillis()
        val currentExpires = _activeShieldExpiresAt.value
        val base = if (currentExpires != null && currentExpires > now) currentExpires else now
        val newExpires = base + days * 24 * 3600 * 1000L

        val shieldType = if (days >= 7) ActiveShieldType.SHIELD_7D_AUTO else ActiveShieldType.SHIELD_72H
        _activeShieldExpiresAt.value = newExpires
        _isStreakShieldActive.value = true
        _activeShieldType.value = shieldType

        val editor = getPrefs(context).edit()
            .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, newExpires)
            .putBoolean(KEY_STREAK_SHIELD, true)
            .putString(KEY_ACTIVE_SHIELD_TYPE, shieldType.name)

        if (days >= 7) {
            val triggered = _sevenDayAutoShieldCount.value + 1
            _sevenDayAutoShieldCount.value = triggered
            editor.putInt(KEY_SEVEN_DAY_AUTO_TRIGGERS, triggered)
        }
        editor.apply()

        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "SHIELD")
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
            _activeShieldType.value = ActiveShieldType.SHIELD_72H
            getPrefs(context).edit()
                .putBoolean(KEY_STREAK_SHIELD, true)
                .putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, expires)
                .putString(KEY_ACTIVE_SHIELD_TYPE, ActiveShieldType.SHIELD_72H.name)
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
            _activeShieldType.value = ActiveShieldType.NONE
            getPrefs(context).edit()
                .putBoolean(KEY_STREAK_SHIELD, false)
                .remove(KEY_ACTIVE_SHIELD_EXPIRES_AT)
                .remove(KEY_ACTIVE_SHIELD_TYPE)
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

    const val MAX_SCORE = Double.MAX_VALUE

    @Synchronized
    fun setScore(context: Context, newScore: Double, reason: String = "SIMULATION", details: String = ""): Double {
        init(context)
        val clamped = sanitizeScore(newScore)
        val diff = clamped - _flameScore.value
        _flameScore.value = clamped

        if (clamped > _peakScore.value) {
            _peakScore.value = clamped
            getPrefs(context).edit().apply {
                saveDouble(this, KEY_PEAK_SCORE, KEY_PEAK_SCORE_BITS, clamped)
                apply()
            }
        }

        val projectedGained = _totalPointsGained.value + if (diff > 0) diff else 0.0
        // Check Milestone Peak Title unlock at 20,000+ pts
        com.example.data.profile.ProfileManager.checkMilestonePeakUnlock(
            context = context,
            flameScore = clamped,
            totalGainedPoints = projectedGained
        )

        if (diff > 0) {
            val updatedGained = _totalPointsGained.value + diff
            _totalPointsGained.value = updatedGained
            getPrefs(context).edit().apply {
                saveDouble(this, KEY_TOTAL_POINTS_GAINED, KEY_TOTAL_POINTS_GAINED_BITS, updatedGained)
                apply()
            }
        } else if (diff < 0) {
            val updatedExt = _totalPointsExtinguished.value + (-diff)
            _totalPointsExtinguished.value = updatedExt
            getPrefs(context).edit().apply {
                saveDouble(this, KEY_TOTAL_POINTS_EXTINGUISHED, KEY_TOTAL_POINTS_EXTINGUISHED_BITS, updatedExt)
                apply()
            }
        }

        val prefs = getPrefs(context)
        prefs.edit().apply {
            saveDouble(this, KEY_SCORE, KEY_SCORE_BITS, clamped)
            apply()
        }

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
        val updated = sanitizeScore(old + effectiveDelta)
        _flameScore.value = updated

        if (updated > _peakScore.value) {
            _peakScore.value = updated
            getPrefs(context).edit().apply {
                saveDouble(this, KEY_PEAK_SCORE, KEY_PEAK_SCORE_BITS, updated)
                apply()
            }
        }

        val updatedGained = _totalPointsGained.value + effectiveDelta
        _totalPointsGained.value = updatedGained
        getPrefs(context).edit().apply {
            saveDouble(this, KEY_SCORE, KEY_SCORE_BITS, updated)
            saveDouble(this, KEY_TOTAL_POINTS_GAINED, KEY_TOTAL_POINTS_GAINED_BITS, updatedGained)
            apply()
        }

        // Check Milestone Peak Title unlock at 20,000+ pts
        com.example.data.profile.ProfileManager.checkMilestonePeakUnlock(
            context = context,
            flameScore = updated,
            totalGainedPoints = updatedGained
        )

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
        val updated = sanitizeScore(old - delta)
        val actualDeducted = old - updated
        _flameScore.value = updated

        val updatedExt = _totalPointsExtinguished.value + actualDeducted
        _totalPointsExtinguished.value = updatedExt
        getPrefs(context).edit().apply {
            saveDouble(this, KEY_SCORE, KEY_SCORE_BITS, updated)
            saveDouble(this, KEY_TOTAL_POINTS_EXTINGUISHED, KEY_TOTAL_POINTS_EXTINGUISHED_BITS, updatedExt)
            apply()
        }

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

    @Synchronized
    fun setGirlDeductionPoints(context: Context, points: Double): Double {
        init(context)
        val safe = if (!points.isFinite()) 3.0 else points.coerceAtLeast(0.1)
        _girlDeductionPoints.value = safe
        getPrefs(context).edit().apply {
            saveDouble(this, KEY_GIRL_DEDUCTION_POINTS, KEY_GIRL_DEDUCTION_POINTS_BITS, safe)
            apply()
        }
        return safe
    }

    /**
     * "Deflame" Button: Each tap instantly decreases the total Flame score by 3 points (ensuring score >= 0).
     * If Pulse Elixir is active, grants +5 extra points (consumes 1 of 3 uses). Also awards +20 XP.
     */
    fun deflame(context: Context, delta: Double = 3.0): Double {
        init(context)
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "DEFLAME")
        val pulseElixirBonus = com.example.data.profile.ProfileManager.consumePulseElixirUseIfActive(context)
        val safeDelta = (delta + pulseElixirBonus).coerceAtLeast(0.1)
        val formatted = if (safeDelta % 1.0 == 0.0) "${safeDelta.toInt()}" else String.format(Locale.US, "%.1f", safeDelta)
        val bonusNote = if (pulseElixirBonus > 0) " [Pulse Elixir: +5 pts]" else ""
        val updated = deductPoints(
            context = context,
            delta = safeDelta,
            reason = "DEFLAME",
            details = "Manual instant decrease (-$formatted pts)$bonusNote",
            title = "Deflame Button Action"
        )
        val bonusArg = if (pulseElixirBonus > 0) "Pulse Elixir +5" else ""
        com.example.data.roadmap.XpCalculationEngine.onDeflameAction(context, bonusNote = bonusArg)
        return updated
    }

    /**
     * "Girl" Button / Custom Points Adjustment: Each tap decreases the total Flame score by [delta]
     * (defaulting to the persisted [_girlDeductionPoints] setting). Also awards +50 XP.
     */
    fun girl(context: Context, delta: Double? = null): Double {
        init(context)
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "GIRL")
        val safeDelta = (delta ?: _girlDeductionPoints.value).coerceAtLeast(0.1)
        val formatted = if (safeDelta % 1.0 == 0.0) "${safeDelta.toInt()}" else String.format(Locale.US, "%.1f", safeDelta)
        val updated = deductPoints(
            context = context,
            delta = safeDelta,
            reason = "GIRL",
            details = "Manual cooldown decrease (-$formatted pts)",
            title = "Girl Button Action"
        )
        com.example.data.roadmap.XpCalculationEngine.onGirlAction(context)
        return updated
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
        strikesToday: List<com.example.data.model.StrikeEvent> = emptyList(),
        strikeCount: Int = 1
    ): StrikeRegistrationResult {
        init(context)
        val prefs = getPrefs(context)
        val liveToday = todayString()
        val isLiveToday = (strikeDate == liveToday)
        val isFutureDate = (strikeDate > liveToday)
        val isPastDate = (strikeDate < liveToday)
        val effectiveLatestBefore = latestDateBeforeInsert?.takeIf { it.isNotBlank() && it <= liveToday }
        val isBeforeLatest = effectiveLatestBefore != null && strikeDate < effectiveLatestBefore

        // Strictly isolate Double Strike, Multi-Strike Combo, Extreme Overload, and live deductions
        // to the current live calendar day (Real-Time Today). Past and future days never trigger live penalties.
        if (!isLiveToday || isBeforeLatest) {
            val detailsMsg = when {
                isFutureDate -> "Future date ($strikeDate > today $liveToday). Isolated from live today flame penalties, combos, & overload triggers."
                isBeforeLatest -> "Date ($strikeDate) is before latest ($effectiveLatestBefore). Protected from flame deduction & combo triggers."
                isPastDate -> "Historical/past date ($strikeDate < today $liveToday). Isolated from live today combo/overload triggers."
                else -> "Date ($strikeDate != today $liveToday). Isolated from live today combo/overload triggers."
            }
            logEvent(
                context = context,
                title = if (isFutureDate) "Future Strike Isolated" else "Retroactive Strike Ignored",
                change = 0.0,
                resultingScore = _flameScore.value,
                reason = "RETROACTIVE",
                details = detailsMsg
            )
            return StrikeRegistrationResult(0.0, "RETROACTIVE")
        }

        // Only count real-time non-historical strikes matching live today's calendar date
        val liveTodayStrikes = strikesToday.filter {
            it.date == liveToday && !it.strikeType.startsWith("Historical", ignoreCase = true)
        }
        val expandedToday = liveTodayStrikes.flatMap { ev ->
            val n = ev.count.coerceAtLeast(1).coerceAtMost(20)
            if (n == 1) listOf(ev) else List(n) { ev }
        }
        val strikesCount = liveTodayStrikes.sumOf { it.count.coerceAtLeast(1) }.coerceAtLeast(1)

        val result = when {
            // Case 4: Extreme Overload Reset Rule (6 or more Strikes on live today)
            strikesCount >= 6 -> {
                val oldScore = _flameScore.value
                _flameScore.value = 0.0
                val updatedExt = _totalPointsExtinguished.value + oldScore
                _totalPointsExtinguished.value = updatedExt
                prefs.edit().apply {
                    saveDouble(this, KEY_SCORE, KEY_SCORE_BITS, 0.0)
                    saveDouble(this, KEY_TOTAL_POINTS_EXTINGUISHED, KEY_TOTAL_POINTS_EXTINGUISHED_BITS, updatedExt)
                    apply()
                }

                val namesList = expandedToday.distinctBy { it.individualId to it.id }.joinToString(", ") { it.individualName }
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

            // Case 3: Multi-Strike Combo Penalty Rule (3 to 5 Strikes on live today)
            strikesCount in 3..5 && expandedToday.size >= 3 -> {
                val sumBoosts = expandedToday.sumOf {
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
                val namesList = expandedToday.joinToString(", ") {
                    "${it.individualName} (BOOST ${format(if (it.performanceValue > 0.0) it.performanceValue else boost)})"
                }

                val details = "${strikesCount} strikes today: $namesList. Formula: Sum of Boosts (${format(sumBoosts)}) × $strikesCount strikes = ${format(penalty)} PTS deducted. Reward: 7-Day Auto Streak Shield activated (Active until $expiryStr, countdown: $countdown)."

                // Update the log entry details
                val currentLogs = _flameLogs.value.toMutableList()
                if (currentLogs.isNotEmpty() && currentLogs[0].reason == "MULTI_STRIKE_COMBO") {
                    currentLogs[0] = currentLogs[0].copy(details = details)
                    _flameLogs.value = currentLogs
                }

                com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "STRIKE")
                StrikeRegistrationResult(penalty, "MULTI_STRIKE_COMBO", "7_DAYS_AUTO")
            }

            // Case 2: Double Strike Penalty Rule (2 Strikes on live today)
            strikesCount == 2 && expandedToday.size >= 2 -> {
                val s1 = expandedToday[0]
                val s2 = expandedToday[1]
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

                com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "STRIKE")
                StrikeRegistrationResult(penalty, "DOUBLE_STRIKE", "3_DAYS_MANUAL")
            }

            // Case 1: Normal Single Strike on live today
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

        // Award +100 XP per recorded Strike with concerned individual's name
        com.example.data.roadmap.XpCalculationEngine.onStrikeRecorded(
            context = context,
            strikeCount = strikeCount,
            personName = personName
        )

        // Update last strike date & reset inactivity baseline date strictly to liveToday
        prefs.edit()
            .putString(KEY_LAST_STRIKE_DATE, liveToday)
            .putString(KEY_LAST_INACTIVITY_DATE, liveToday)
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

    fun getStreakEndDate(context: Context): String? {
        init(context)
        return getPrefs(context).getString(KEY_END_COUNT_DATE, null)
    }

    fun getSavedGirlPerMonth(context: Context): Int {
        init(context)
        return getPrefs(context).getInt(KEY_GIRL_PER_MONTH, 0).coerceAtLeast(0)
    }

    fun getSavedDeflamePerMonth(context: Context): Int {
        init(context)
        return getPrefs(context).getInt(KEY_DEFLAME_PER_MONTH, 0).coerceAtLeast(0)
    }

    fun getSavedRecalculationConfig(context: Context): StreakRecalculationConfig {
        init(context)
        val prefs = getPrefs(context)
        val today = todayString()
        val defaultStart = prefs.getString(KEY_START_COUNT_DATE, null)?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
                Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000L)
            )
        val defaultEnd = prefs.getString(KEY_END_COUNT_DATE, null)?.takeIf { it.isNotBlank() } ?: today
        val rangesJsonStr = prefs.getString(KEY_PER_ITEM_DATE_RANGES, null)
        val json = if (!rangesJsonStr.isNullOrBlank()) {
            try { JSONObject(rangesJsonStr) } catch (_: Exception) { null }
        } else null

        fun parseRange(key: String): DateRangeConfig? {
            val obj = json?.optJSONObject(key) ?: return null
            val isCustom = obj.optBoolean("isCustom", false)
            val s = obj.optString("startDate", defaultStart).ifBlank { defaultStart }
            val e = obj.optString("endDate", defaultEnd).ifBlank { defaultEnd }
            return DateRangeConfig(startDate = s, endDate = e, isCustom = isCustom)
        }

        return StreakRecalculationConfig(
            globalStartDate = defaultStart,
            globalEndDate = defaultEnd,
            inactivityRange = parseRange("inactivity"),
            strikesRange = parseRange("strikes"),
            girlRange = parseRange("girl"),
            deflameRange = parseRange("deflame"),
            girlPerMonth = getSavedGirlPerMonth(context),
            deflamePerMonth = getSavedDeflamePerMonth(context)
        )
    }

    private fun resolveEffectiveRange(
        globalStart: String,
        globalEnd: String,
        overrideRange: DateRangeConfig?,
        maxAllowedDate: String
    ): Pair<String, String> {
        val rawStart = if (overrideRange != null && overrideRange.isCustom) overrideRange.startDate else globalStart
        val rawEnd = if (overrideRange != null && overrideRange.isCustom) overrideRange.endDate else globalEnd
        val clampedEnd = minOf(rawEnd, maxAllowedDate)
        val clampedStart = minOf(rawStart, clampedEnd)
        return clampedStart to clampedEnd
    }

    /**
     * Preview recalculation using a single start date (up to today) or full per-item custom ranges
     * without modifying persistent state.
     * Historical, custom-range, and retroactive recalculations strictly disable daily combo/overload
     * multipliers (Double Strike, Multi-Strike Combo, Extreme Overload) and exclude future days.
     */
    fun previewRecalculation(
        startDateStr: String,
        strikeEvents: List<com.example.data.model.StrikeEvent>,
        girlPerMonth: Int = 0,
        deflamePerMonth: Int = 0,
        endDateStr: String? = null,
        inactivityRange: DateRangeConfig? = null,
        strikesRange: DateRangeConfig? = null,
        girlRange: DateRangeConfig? = null,
        deflameRange: DateRangeConfig? = null
    ): FlameRecalculationResult {
        val today = todayString()
        val effectiveGlobalEnd = endDateStr?.takeIf { it.isNotBlank() && it <= today } ?: today
        val effectiveGlobalStart = if (startDateStr <= effectiveGlobalEnd) startDateStr else effectiveGlobalEnd

        val (inactStart, inactEnd) = resolveEffectiveRange(effectiveGlobalStart, effectiveGlobalEnd, inactivityRange, today)
        val (strikesStart, strikesEnd) = resolveEffectiveRange(effectiveGlobalStart, effectiveGlobalEnd, strikesRange, today)
        val (girlStart, girlEnd) = resolveEffectiveRange(effectiveGlobalStart, effectiveGlobalEnd, girlRange, today)
        val (deflameStart, deflameEnd) = resolveEffectiveRange(effectiveGlobalStart, effectiveGlobalEnd, deflameRange, today)

        val daysElapsed = daysBetween(inactStart, inactEnd).toInt().coerceAtLeast(0)
        val inactivityPointsAdded = daysElapsed * 3.0

        // Filter strikes strictly within [strikesStart, strikesEnd] and exclude future dates (> today)
        val relevantStrikes = strikeEvents.filter {
            it.date >= strikesStart && it.date <= strikesEnd && it.date <= today
        }
        val strikesByDate = relevantStrikes.groupBy { it.date }

        val dailyStrikePenalties = mutableMapOf<String, Double>()
        var totalDeductions = 0.0
        var totalStrikeUnitsCount = 0
        for ((date, dayStrikesRaw) in strikesByDate) {
            var count = 0
            var dayPenalty = 0.0
            for (ev in dayStrikesRaw) {
                val n = ev.count.coerceAtLeast(1)
                count += n
                dayPenalty += ev.performanceValue.coerceAtLeast(0.1) * n
            }
            totalStrikeUnitsCount += count
            dailyStrikePenalties[date] = dayPenalty
            totalDeductions += dayPenalty
        }

        val girlDays = daysBetween(girlStart, girlEnd).toInt().coerceAtLeast(0)
        val deflameDays = daysBetween(deflameStart, deflameEnd).toInt().coerceAtLeast(0)

        val safeGirlPerMonth = girlPerMonth.coerceAtLeast(0)
        val safeDeflamePerMonth = deflamePerMonth.coerceAtLeast(0)
        val girlMonthsElapsed = girlDays / 30.0
        val deflameMonthsElapsed = deflameDays / 30.0

        val girlPressesCount = if (safeGirlPerMonth > 0 && girlDays > 0) {
            kotlin.math.round(safeGirlPerMonth * girlMonthsElapsed).toInt().coerceAtLeast(0)
        } else {
            0
        }
        val deflamePressesCount = if (safeDeflamePerMonth > 0 && deflameDays > 0) {
            kotlin.math.round(safeDeflamePerMonth * deflameMonthsElapsed).toInt().coerceAtLeast(0)
        } else {
            0
        }
        val girlPointsDeducted = girlPressesCount * 3.0
        val deflamePointsDeducted = deflamePressesCount * 3.0

        val totalExtinguishedPoints = sanitizeScore(totalDeductions + girlPointsDeducted + deflamePointsDeducted)
        val resultingScore = sanitizeScore(inactivityPointsAdded - totalExtinguishedPoints)

        // Compute chronological peak score across [effectiveGlobalStart..effectiveGlobalEnd]
        val timelineDates = sortedSetOf<String>().apply {
            add(effectiveGlobalStart)
            add(effectiveGlobalEnd)
            add(inactStart)
            add(inactEnd)
            add(girlStart)
            add(girlEnd)
            add(deflameStart)
            add(deflameEnd)
            addAll(dailyStrikePenalties.keys)
        }.toList()

        var runningScore = 0.0
        var peakAcrossPeriod = resultingScore
        for (idx in timelineDates.indices) {
            val d = timelineDates[idx]
            if (idx > 0) {
                val prev = timelineDates[idx - 1]
                // Inactivity accrued inside overlap of (prev..d] with [inactStart..inactEnd]
                val oInactStart = maxOf(prev, inactStart)
                val oInactEnd = minOf(d, inactEnd)
                if (oInactEnd > oInactStart) {
                    val addDays = daysBetween(oInactStart, oInactEnd).toDouble()
                    runningScore = sanitizeScore(runningScore + addDays * 3.0)
                }
                // Proportional Girl deductions inside overlap with [girlStart..girlEnd]
                if (girlDays > 0 && girlPointsDeducted > 0.0) {
                    val oGirlStart = maxOf(prev, girlStart)
                    val oGirlEnd = minOf(d, girlEnd)
                    if (oGirlEnd > oGirlStart) {
                        val frac = daysBetween(oGirlStart, oGirlEnd).toDouble() / girlDays.toDouble()
                        runningScore = sanitizeScore(runningScore - girlPointsDeducted * frac)
                    }
                }
                // Proportional Deflame deductions inside overlap with [deflameStart..deflameEnd]
                if (deflameDays > 0 && deflamePointsDeducted > 0.0) {
                    val oDefStart = maxOf(prev, deflameStart)
                    val oDefEnd = minOf(d, deflameEnd)
                    if (oDefEnd > oDefStart) {
                        val frac = daysBetween(oDefStart, oDefEnd).toDouble() / deflameDays.toDouble()
                        runningScore = sanitizeScore(runningScore - deflamePointsDeducted * frac)
                    }
                }
            }
            if (runningScore > peakAcrossPeriod) {
                peakAcrossPeriod = runningScore
            }
            val strikeDed = dailyStrikePenalties[d] ?: 0.0
            if (strikeDed > 0.0) {
                runningScore = sanitizeScore(runningScore - strikeDed)
            }
        }
        val estimatedPeakScore = maxOf(peakAcrossPeriod, resultingScore)

        return FlameRecalculationResult(
            startDate = effectiveGlobalStart,
            todayDate = effectiveGlobalEnd,
            endDate = effectiveGlobalEnd,
            daysElapsed = daysElapsed,
            inactivityStartDate = inactStart,
            inactivityEndDate = inactEnd,
            inactivityPointsAdded = inactivityPointsAdded,
            strikesStartDate = strikesStart,
            strikesEndDate = strikesEnd,
            totalStrikesCount = totalStrikeUnitsCount,
            totalDeductions = totalDeductions,
            girlStartDate = girlStart,
            girlEndDate = girlEnd,
            girlDaysElapsed = girlDays,
            girlPerMonth = safeGirlPerMonth,
            girlPressesCount = girlPressesCount,
            girlPointsDeducted = girlPointsDeducted,
            deflameStartDate = deflameStart,
            deflameEndDate = deflameEnd,
            deflameDaysElapsed = deflameDays,
            deflamePerMonth = safeDeflamePerMonth,
            deflamePressesCount = deflamePressesCount,
            deflamePointsDeducted = deflamePointsDeducted,
            resultingScore = resultingScore,
            estimatedPeakScore = estimatedPeakScore,
            totalExtinguishedPoints = totalExtinguishedPoints
        )
    }

    /**
     * Recalculates Flame Score across global [startDateStr]..[endDateStr] or independent
     * per-element custom date ranges:
     * - Adds +3.0 points daily inactivity for each calendar day in inactivity period.
     * - Accounts for all strikes within strikes period.
     * - Converts [girlPerMonth] & [deflamePerMonth] proportionally across their respective date ranges,
     *   logging them at their range end dates without mutating task step counters.
     */
    @Synchronized
    fun recalculateFromStartDate(
        context: Context,
        startDateStr: String,
        strikeEvents: List<com.example.data.model.StrikeEvent>,
        lang: String = "ar",
        girlPerMonth: Int = 0,
        deflamePerMonth: Int = 0,
        endDateStr: String? = null,
        inactivityRange: DateRangeConfig? = null,
        strikesRange: DateRangeConfig? = null,
        girlRange: DateRangeConfig? = null,
        deflameRange: DateRangeConfig? = null
    ): FlameRecalculationResult {
        init(context)
        val result = previewRecalculation(
            startDateStr = startDateStr,
            strikeEvents = strikeEvents,
            girlPerMonth = girlPerMonth,
            deflamePerMonth = deflamePerMonth,
            endDateStr = endDateStr,
            inactivityRange = inactivityRange,
            strikesRange = strikesRange,
            girlRange = girlRange,
            deflameRange = deflameRange
        )
        val oldScore = _flameScore.value
        val newScore = result.resultingScore
        val diff = newScore - oldScore
        val syncedPeak = sanitizeScore(maxOf(result.estimatedPeakScore, newScore))
        val syncedGained = sanitizeScore(result.inactivityPointsAdded)
        val syncedExtinguished = sanitizeScore(result.totalExtinguishedPoints)

        _flameScore.value = newScore
        _peakScore.value = syncedPeak
        _totalPointsGained.value = syncedGained
        _totalPointsExtinguished.value = syncedExtinguished

        getPrefs(context).edit().apply {
            saveDouble(this, KEY_PEAK_SCORE, KEY_PEAK_SCORE_BITS, syncedPeak)
            saveDouble(this, KEY_TOTAL_POINTS_GAINED, KEY_TOTAL_POINTS_GAINED_BITS, syncedGained)
            saveDouble(this, KEY_TOTAL_POINTS_EXTINGUISHED, KEY_TOTAL_POINTS_EXTINGUISHED_BITS, syncedExtinguished)
            apply()
        }

        // Check Milestone Peak Title unlock at 20,000+ pts
        com.example.data.profile.ProfileManager.checkMilestonePeakUnlock(
            context = context,
            flameScore = syncedPeak,
            totalGainedPoints = syncedGained
        )

        val rangesJson = JSONObject().apply {
            fun putRange(key: String, range: DateRangeConfig?) {
                if (range != null) {
                    put(key, JSONObject().apply {
                        put("isCustom", range.isCustom)
                        put("startDate", range.startDate)
                        put("endDate", range.endDate)
                    })
                }
            }
            putRange("inactivity", inactivityRange)
            putRange("strikes", strikesRange)
            putRange("girl", girlRange)
            putRange("deflame", deflameRange)
        }.toString()

        val hasExplicitEndDate = !endDateStr.isNullOrBlank()
        getPrefs(context).edit().apply {
            saveDouble(this, KEY_SCORE, KEY_SCORE_BITS, newScore)
            putString(KEY_START_COUNT_DATE, result.startDate)
            if (hasExplicitEndDate) {
                putString(KEY_END_COUNT_DATE, result.endDate)
            } else {
                remove(KEY_END_COUNT_DATE)
            }
            putString(KEY_PER_ITEM_DATE_RANGES, rangesJson)
            putString(KEY_LAST_INACTIVITY_DATE, todayString())
            putInt(KEY_GIRL_PER_MONTH, result.girlPerMonth)
            putInt(KEY_DEFLAME_PER_MONTH, result.deflamePerMonth)
            apply()
        }

        // Replace previous retroactive estimate logs cleanly so recalculating with new rates/dates never duplicates XP or logs
        val cleanedLogs = _flameLogs.value.filterNot { it.isRetroactiveEstimate }
        if (cleanedLogs.size != _flameLogs.value.size) {
            _flameLogs.value = cleanedLogs
            persistLogs(context, cleanedLogs)
        }

        val scoreAfterStrikes = sanitizeScore(result.inactivityPointsAdded - result.totalDeductions)
        val scoreAfterGirl = sanitizeScore(scoreAfterStrikes - result.girlPointsDeducted)

        // Log retroactive Girl presses if > 0 (timestamp anchored inside [girlStartDate..girlEndDate])
        if (result.girlPressesCount > 0) {
            val girlXp = result.girlPressesCount * 50L
            val girlTitle = when (lang) {
                "ar" -> "تسجيل Girl التلقائي (×${result.girlPressesCount})"
                "fr" -> "Action Girl rétroactive (×${result.girlPressesCount})"
                else -> "Retroactive Girl Action (×${result.girlPressesCount})"
            }
            val girlDetails = when (lang) {
                "ar" -> "${result.girlPerMonth}/شهر × ${result.girlDaysElapsed} يوم (${result.girlStartDate} → ${result.girlEndDate}) = ${result.girlPressesCount} مرة (+$girlXp XP، دون تعديل عداد المهام)"
                "fr" -> "${result.girlPerMonth}/mois × ${result.girlDaysElapsed} j (${result.girlStartDate} → ${result.girlEndDate}) = ${result.girlPressesCount} fois (+$girlXp XP)"
                else -> "${result.girlPerMonth}/mo × ${result.girlDaysElapsed} days (${result.girlStartDate} → ${result.girlEndDate}) = ${result.girlPressesCount} presses (+$girlXp XP)"
            }
            logEvent(
                context = context,
                title = girlTitle,
                change = -result.girlPointsDeducted,
                resultingScore = scoreAfterGirl,
                reason = "GIRL",
                details = girlDetails,
                isRetroactiveEstimate = true,
                actionCount = result.girlPressesCount,
                customTimestamp = dateMiddayMillis(result.girlEndDate)
            )
        }

        // Log retroactive Deflame presses if > 0 (timestamp anchored inside [deflameStartDate..deflameEndDate])
        if (result.deflamePressesCount > 0) {
            val deflameXp = result.deflamePressesCount * 20L
            val deflameTitle = when (lang) {
                "ar" -> "تبريد Deflame التلقائي (×${result.deflamePressesCount})"
                "fr" -> "Refroidissement Deflame rétroactif (×${result.deflamePressesCount})"
                else -> "Retroactive Deflame Action (×${result.deflamePressesCount})"
            }
            val deflameDetails = when (lang) {
                "ar" -> "${result.deflamePerMonth}/شهر × ${result.deflameDaysElapsed} يوم (${result.deflameStartDate} → ${result.deflameEndDate}) = ${result.deflamePressesCount} مرة (+$deflameXp XP، دون تعديل عداد المهام)"
                "fr" -> "${result.deflamePerMonth}/mois × ${result.deflameDaysElapsed} j (${result.deflameStartDate} → ${result.deflameEndDate}) = ${result.deflamePressesCount} fois (+$deflameXp XP)"
                else -> "${result.deflamePerMonth}/mo × ${result.deflameDaysElapsed} days (${result.deflameStartDate} → ${result.deflameEndDate}) = ${result.deflamePressesCount} presses (+$deflameXp XP)"
            }
            logEvent(
                context = context,
                title = deflameTitle,
                change = -result.deflamePointsDeducted,
                resultingScore = newScore,
                reason = "DEFLAME",
                details = deflameDetails,
                isRetroactiveEstimate = true,
                actionCount = result.deflamePressesCount,
                customTimestamp = dateMiddayMillis(result.deflameEndDate)
            )
        }

        val details = when (lang) {
            "ar" -> buildString {
                append("الفترة الرئيسية: ${result.startDate} → ${result.endDate}")
                append(" | الأيام: ${result.daysElapsed} (+${result.inactivityPointsAdded.toInt()} نقطة)")
                append(" | السترايكات: ${result.totalStrikesCount} (-${format(result.totalDeductions)} نقطة)")
                if (result.girlPressesCount > 0) append(" | Girl: ${result.girlPressesCount} (-${format(result.girlPointsDeducted)})")
                if (result.deflamePressesCount > 0) append(" | Deflame: ${result.deflamePressesCount} (-${format(result.deflamePointsDeducted)})")
            }
            "fr" -> buildString {
                append("Période : ${result.startDate} → ${result.endDate}")
                append(" | Jours : ${result.daysElapsed} (+${result.inactivityPointsAdded.toInt()} pts)")
                append(" | Strikes : ${result.totalStrikesCount} (-${format(result.totalDeductions)} pts)")
                if (result.girlPressesCount > 0) append(" | Girl : ${result.girlPressesCount} (-${format(result.girlPointsDeducted)})")
                if (result.deflamePressesCount > 0) append(" | Deflame : ${result.deflamePressesCount} (-${format(result.deflamePointsDeducted)})")
            }
            else -> buildString {
                append("Period: ${result.startDate} → ${result.endDate}")
                append(" | Days: ${result.daysElapsed} (+${result.inactivityPointsAdded.toInt()} pts)")
                append(" | Strikes: ${result.totalStrikesCount} (-${format(result.totalDeductions)} pts)")
                if (result.girlPressesCount > 0) append(" | Girl: ${result.girlPressesCount} (-${format(result.girlPointsDeducted)})")
                if (result.deflamePressesCount > 0) append(" | Deflame: ${result.deflamePressesCount} (-${format(result.deflamePointsDeducted)})")
            }
        }
        logEvent(
            context = context,
            title = when (lang) {
                "ar" -> "إعادة حساب تاريخ بداية العد"
                "fr" -> "Recalcul sur période sélectionnée"
                else -> "Streak Period Recalculation"
            },
            change = diff,
            resultingScore = newScore,
            reason = "START_DATE_RECALCULATION",
            details = details
        )

        return result
    }

    private fun dateMiddayMillis(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val parsed = sdf.parse(dateStr.trim()) ?: return System.currentTimeMillis()
            val midday = parsed.time + (12 * 3600 * 1000L)
            midday.coerceAtMost(System.currentTimeMillis())
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
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
        details: String = "",
        isRetroactiveEstimate: Boolean = false,
        actionCount: Int = 1,
        customTimestamp: Long? = null
    ) {
        val ts = customTimestamp ?: System.currentTimeMillis()
        val entry = FlameLogEntry(
            id = System.currentTimeMillis() + (0..999).random(),
            timestamp = ts,
            title = title,
            change = change,
            resultingScore = resultingScore,
            reason = reason,
            details = details,
            isRetroactiveEstimate = isRetroactiveEstimate,
            actionCount = actionCount.coerceAtLeast(1)
        )
        val current = _flameLogs.value.toMutableList()
        current.add(0, entry)
        _flameLogs.value = current
        persistLogs(context, current)
    }

    private fun persistLogs(context: Context, logs: List<FlameLogEntry>) {
        try {
            val array = logsToJsonArray(logs)
            getPrefs(context).edit().putString(KEY_FLAME_LOGS, array.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun logsToJsonArray(logs: List<FlameLogEntry>): JSONArray {
        val array = JSONArray()
        for (item in logs) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("title", item.title)
                put("change", item.change)
                put("resultingScore", item.resultingScore)
                put("reason", item.reason)
                put("details", item.details)
                if (item.isRetroactiveEstimate) put("isRetroactiveEstimate", true)
                if (item.actionCount != 1) put("actionCount", item.actionCount.coerceAtLeast(1))
            }
            array.put(obj)
        }
        return array
    }

    @Synchronized
    fun exportSnapshot(context: Context): JSONObject {
        init(context)
        val prefs = getPrefs(context)
        return JSONObject().apply {
            put("score", _flameScore.value)
            put("peakScore", _peakScore.value)
            put("totalPointsGained", _totalPointsGained.value)
            put("totalPointsExtinguished", _totalPointsExtinguished.value)
            put("isStreakShieldActive", _isStreakShieldActive.value)
            put("activeShieldExpiresAt", _activeShieldExpiresAt.value ?: 0L)
            put("activeShieldType", _activeShieldType.value.name)
            put("availableManualShields", _availableManualShields.value)
            put("available48hShields", _available48hShields.value)
            put("available1DayShields", _available1DayShields.value)
            put("sevenDayAutoShieldCount", _sevenDayAutoShieldCount.value)
            put("startCountDate", prefs.getString(KEY_START_COUNT_DATE, "") ?: "")
            put("endCountDate", prefs.getString(KEY_END_COUNT_DATE, "") ?: "")
            put("perItemDateRanges", prefs.getString(KEY_PER_ITEM_DATE_RANGES, "") ?: "")
            put("lastInactivityDate", prefs.getString(KEY_LAST_INACTIVITY_DATE, "") ?: "")
            put("lastStrikeDate", prefs.getString(KEY_LAST_STRIKE_DATE, "") ?: "")
            put("girlPerMonth", prefs.getInt(KEY_GIRL_PER_MONTH, 0))
            put("deflamePerMonth", prefs.getInt(KEY_DEFLAME_PER_MONTH, 0))
            put("girlDeductionPoints", _girlDeductionPoints.value)
            put("logs", logsToJsonArray(_flameLogs.value))
        }
    }

    @Synchronized
    fun restoreSnapshot(context: Context, json: JSONObject, replaceExisting: Boolean) {
        init(context)
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()

        val importedScore = sanitizeScore(json.optDouble("score", _flameScore.value))
        val importedPeak = sanitizeScore(json.optDouble("peakScore", importedScore))
        val importedGained = json.optDouble("totalPointsGained", 0.0).coerceAtLeast(0.0)
        val importedExt = json.optDouble("totalPointsExtinguished", 0.0).coerceAtLeast(0.0)

        val finalScore = if (replaceExisting) importedScore else maxOf(_flameScore.value, importedScore)
        val finalPeak = if (replaceExisting) maxOf(importedScore, importedPeak) else maxOf(_peakScore.value, importedPeak, finalScore)
        val finalGained = if (replaceExisting) importedGained else (_totalPointsGained.value + importedGained)
        val finalExt = if (replaceExisting) importedExt else (_totalPointsExtinguished.value + importedExt)

        val rawExpires = json.optLong("activeShieldExpiresAt", 0L)
        val finalExpires: Long? = if (replaceExisting) {
            if (rawExpires > now) rawExpires else null
        } else {
            val best = maxOf(_activeShieldExpiresAt.value ?: 0L, rawExpires)
            if (best > now) best else null
        }

        val importedShieldActive = json.optBoolean("isStreakShieldActive", false)
        val finalShieldActive = if (finalExpires != null) {
            true
        } else if (replaceExisting) {
            importedShieldActive && rawExpires == 0L
        } else {
            _isStreakShieldActive.value || (importedShieldActive && rawExpires == 0L)
        }

        val importedTypeName = json.optString("activeShieldType", ActiveShieldType.NONE.name)
        val importedType = ActiveShieldType.entries.find { it.name == importedTypeName } ?: ActiveShieldType.NONE
        val finalType = if (finalShieldActive) {
            if (importedType != ActiveShieldType.NONE) importedType else _activeShieldType.value
        } else {
            ActiveShieldType.NONE
        }

        val s72 = if (replaceExisting) json.optInt("availableManualShields", 0) else (_availableManualShields.value + json.optInt("availableManualShields", 0))
        val s48 = if (replaceExisting) json.optInt("available48hShields", 0) else (_available48hShields.value + json.optInt("available48hShields", 0))
        val s24 = if (replaceExisting) json.optInt("available1DayShields", 0) else (_available1DayShields.value + json.optInt("available1DayShields", 0))
        val autoCount = if (replaceExisting) json.optInt("sevenDayAutoShieldCount", 0) else (_sevenDayAutoShieldCount.value + json.optInt("sevenDayAutoShieldCount", 0))

        val importedLogs = mutableListOf<FlameLogEntry>()
        json.optJSONArray("logs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                importedLogs.add(
                    FlameLogEntry(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        title = obj.optString("title", ""),
                        change = obj.optDouble("change", 0.0),
                        resultingScore = obj.optDouble("resultingScore", 0.0),
                        reason = obj.optString("reason", "EVENT"),
                        details = obj.optString("details", ""),
                        isRetroactiveEstimate = obj.optBoolean("isRetroactiveEstimate", false),
                        actionCount = obj.optInt("actionCount", 1).coerceAtLeast(1)
                    )
                )
            }
        }
        val finalLogs = if (replaceExisting) {
            importedLogs
        } else {
            (importedLogs + _flameLogs.value)
                .distinctBy { "${it.id}_${it.timestamp}_${it.reason}" }
                .sortedByDescending { it.timestamp }
        }

        _flameScore.value = finalScore
        _peakScore.value = finalPeak
        _totalPointsGained.value = finalGained
        _totalPointsExtinguished.value = finalExt
        _isStreakShieldActive.value = finalShieldActive
        _activeShieldExpiresAt.value = finalExpires
        _activeShieldType.value = finalType
        _availableManualShields.value = s72.coerceAtLeast(0)
        _available48hShields.value = s48.coerceAtLeast(0)
        _available1DayShields.value = s24.coerceAtLeast(0)
        _sevenDayAutoShieldCount.value = autoCount.coerceAtLeast(0)
        _flameLogs.value = finalLogs

        val startDate = json.optString("startCountDate", "").trim()
        val endDate = json.optString("endCountDate", "").trim()
        val perItemRanges = json.optString("perItemDateRanges", "").trim()
        val lastInact = json.optString("lastInactivityDate", "").trim()
        val lastStrike = json.optString("lastStrikeDate", "").trim()
        val importedGirlPerMonth = json.optInt("girlPerMonth", 0).coerceAtLeast(0)
        val importedDeflamePerMonth = json.optInt("deflamePerMonth", 0).coerceAtLeast(0)
        val hasImportedGirlDeduction = json.has("girlDeductionPoints")
        val importedGirlDeduction = json.optDouble("girlDeductionPoints", 3.0)
            .takeIf { it.isFinite() && it >= 0.1 } ?: 3.0
        val finalGirlDeduction = if (replaceExisting || hasImportedGirlDeduction) {
            importedGirlDeduction
        } else {
            _girlDeductionPoints.value
        }
        _girlDeductionPoints.value = finalGirlDeduction

        prefs.edit().apply {
            saveDouble(this, KEY_SCORE, KEY_SCORE_BITS, finalScore)
            saveDouble(this, KEY_PEAK_SCORE, KEY_PEAK_SCORE_BITS, finalPeak)
            saveDouble(this, KEY_TOTAL_POINTS_GAINED, KEY_TOTAL_POINTS_GAINED_BITS, finalGained)
            saveDouble(this, KEY_TOTAL_POINTS_EXTINGUISHED, KEY_TOTAL_POINTS_EXTINGUISHED_BITS, finalExt)
            saveDouble(this, KEY_GIRL_DEDUCTION_POINTS, KEY_GIRL_DEDUCTION_POINTS_BITS, finalGirlDeduction)
            putBoolean(KEY_STREAK_SHIELD, finalShieldActive)
            if (finalExpires != null) putLong(KEY_ACTIVE_SHIELD_EXPIRES_AT, finalExpires) else remove(KEY_ACTIVE_SHIELD_EXPIRES_AT)
            if (finalShieldActive && finalType != ActiveShieldType.NONE) {
                putString(KEY_ACTIVE_SHIELD_TYPE, finalType.name)
            } else {
                remove(KEY_ACTIVE_SHIELD_TYPE)
            }
            putInt(KEY_AVAILABLE_MANUAL_SHIELDS, _availableManualShields.value)
            putInt(KEY_AVAILABLE_48H_SHIELDS, _available48hShields.value)
            putInt(KEY_AVAILABLE_1DAY_SHIELDS, _available1DayShields.value)
            putInt(KEY_SEVEN_DAY_AUTO_TRIGGERS, _sevenDayAutoShieldCount.value)
            putBoolean("has_reset_shields_to_zero_v2", true)
            putString(KEY_FLAME_LOGS, logsToJsonArray(finalLogs).toString())
            if (startDate.isNotEmpty()) putString(KEY_START_COUNT_DATE, startDate) else if (replaceExisting) remove(KEY_START_COUNT_DATE)
            if (endDate.isNotEmpty()) putString(KEY_END_COUNT_DATE, endDate) else if (replaceExisting) remove(KEY_END_COUNT_DATE)
            if (perItemRanges.isNotEmpty()) putString(KEY_PER_ITEM_DATE_RANGES, perItemRanges) else if (replaceExisting) remove(KEY_PER_ITEM_DATE_RANGES)
            if (lastInact.isNotEmpty()) putString(KEY_LAST_INACTIVITY_DATE, lastInact) else if (replaceExisting) remove(KEY_LAST_INACTIVITY_DATE)
            if (lastStrike.isNotEmpty()) putString(KEY_LAST_STRIKE_DATE, lastStrike) else if (replaceExisting) remove(KEY_LAST_STRIKE_DATE)
            if (replaceExisting || importedGirlPerMonth > 0) putInt(KEY_GIRL_PER_MONTH, importedGirlPerMonth)
            if (replaceExisting || importedDeflamePerMonth > 0) putInt(KEY_DEFLAME_PER_MONTH, importedDeflamePerMonth)
            apply()
        }
        isInitialized = true
    }
}
