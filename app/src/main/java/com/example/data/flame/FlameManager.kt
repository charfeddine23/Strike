package com.example.data.flame

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FlameLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val change: Double,
    val resultingScore: Double,
    val reason: String
)

object FlameManager {
    private const val PREFS_NAME = "strikepulse_flame_prefs"
    private const val KEY_SCORE = "key_flame_score"
    private const val KEY_LAST_INACTIVITY_DATE = "key_last_inactivity_date"
    private const val KEY_LAST_STRIKE_DATE = "key_last_strike_date"
    private const val KEY_FLAME_LOGS = "key_flame_logs_json"

    private val _flameScore = MutableStateFlow(45.0) // initial warm starting score for good initial state
    val flameScore: StateFlow<Double> = _flameScore.asStateFlow()

    private val _flameLogs = MutableStateFlow<List<FlameLogEntry>>(emptyList())
    val flameLogs: StateFlow<List<FlameLogEntry>> = _flameLogs.asStateFlow()

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
        val savedScore = prefs.getFloat(KEY_SCORE, 45.0f).toDouble()
        _flameScore.value = savedScore.coerceAtLeast(0.0)

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
                            reason = obj.optString("reason", "")
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
    fun setScore(context: Context, newScore: Double, reason: String = "MANUAL", details: String = ""): Double {
        init(context)
        val clamped = newScore.coerceAtLeast(0.0)
        val diff = clamped - _flameScore.value
        _flameScore.value = clamped

        val prefs = getPrefs(context)
        prefs.edit().putFloat(KEY_SCORE, clamped.toFloat()).apply()

        if (diff != 0.0) {
            logEvent(context, details.ifBlank { "Score Adjustment" }, diff, clamped, reason)
        }
        return clamped
    }

    @Synchronized
    fun addPoints(context: Context, delta: Double, reason: String, details: String): Double {
        init(context)
        val old = _flameScore.value
        val updated = (old + delta).coerceAtLeast(0.0)
        _flameScore.value = updated

        getPrefs(context).edit().putFloat(KEY_SCORE, updated.toFloat()).apply()
        logEvent(context, details, delta, updated, reason)
        return updated
    }

    @Synchronized
    fun deductPoints(context: Context, delta: Double, reason: String, details: String): Double {
        init(context)
        val old = _flameScore.value
        val updated = (old - delta).coerceAtLeast(0.0)
        val actualDeducted = old - updated
        _flameScore.value = updated

        getPrefs(context).edit().putFloat(KEY_SCORE, updated.toFloat()).apply()
        logEvent(context, details, -actualDeducted, updated, reason)
        return updated
    }

    /**
     * "Deflame" Button: Each tap instantly decreases the total Flame score by 2 points (ensuring score >= 0).
     */
    fun deflame(context: Context): Double {
        return deductPoints(context, 2.0, "DEFLAME", "Deflame Action (-2 pts)")
    }

    /**
     * "Girl" Button: Each tap instantly decreases the total Flame score by 3 points (ensuring score >= 0).
     */
    fun girl(context: Context): Double {
        return deductPoints(context, 3.0, "GIRL", "Girl Cooldown Action (-3 pts)")
    }

    /**
     * Handles incoming contact call: +5 points automatically
     */
    fun onContactCallDetected(context: Context, contactName: String): Double {
        return addPoints(context, 5.0, "CONTACT_CALL", "Contact Call: $contactName (+5 pts)")
    }

    /**
     * Daily Inactivity Accrual:
     * Automatically adds 3 points to the total Flame score for every single day
     * that passes since the last recorded strike.
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
        if (days > 0) {
            val addedPoints = days * 3.0
            addPoints(
                context,
                addedPoints,
                "INACTIVITY",
                "Daily Inactivity ($days day${if (days > 1) "s" else ""}: +${addedPoints.toInt()} pts)"
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

        // Check retroactive rule
        if (!latestDateBeforeInsert.isNullOrBlank() && strikeDate < latestDateBeforeInsert) {
            // Retroactive entry: Must NOT trigger any Flame points or deductions!
            logEvent(
                context,
                "Retroactive Strike ($personName, $strikeDate) - Protected (0 pts)",
                0.0,
                _flameScore.value,
                "RETROACTIVE"
            )
            return false
        }

        // Current / Latest strike: Deduct BOOST value
        deductPoints(
            context,
            boost,
            "STRIKE_DEDUCTION",
            "Strike Registered: $personName (-${String.format(Locale.US, "%.1f", boost)} BOOST)"
        )

        // Reset inactivity tracking from this new latest strike date
        prefs.edit()
            .putString(KEY_LAST_INACTIVITY_DATE, strikeDate)
            .putString(KEY_LAST_STRIKE_DATE, strikeDate)
            .apply()

        return true
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

    private fun logEvent(context: Context, title: String, change: Double, resultingScore: Double, reason: String) {
        val entry = FlameLogEntry(
            id = System.currentTimeMillis() + (0..999).random(),
            timestamp = System.currentTimeMillis(),
            title = title,
            change = change,
            resultingScore = resultingScore,
            reason = reason
        )
        val current = _flameLogs.value.toMutableList()
        current.add(0, entry)
        val trimmed = current.take(50)
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
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_FLAME_LOGS, array.toString()).apply()
        } catch (_: Exception) {
        }
    }
}
