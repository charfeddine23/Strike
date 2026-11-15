package com.example.data.roadmap

import android.content.Context
import android.content.SharedPreferences
import com.example.data.flame.FlameLogEntry
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Breakdown of XP credited from real-time & retroactive user telemetry
 * starting from the player's trackingStartDate ("تاريخ بداية العد").
 */
data class RetroactiveXpBreakdown(
    val trackingStartDate: String? = null,
    val deflameCount: Int = 0,
    val girlCount: Int = 0,
    val strikeCount: Int = 0,
    val personCount: Int = 0,
    val largePersonBonusCount: Int = 0,
    val deflameXp: Long = 0L,
    val girlXp: Long = 0L,
    val strikeXp: Long = 0L,
    val personBaseXp: Long = 0L,
    val personSizeBonusXp: Long = 0L,
    val totalTelemetryXp: Long = 0L,
    val deltaCreditedXp: Long = 0L
)

/**
 * Unified XP Calculation Engine:
 * - Independent 500-Station level threshold formula (Level 1 = 1,345 XP -> Level 500 = 1,000,000 XP)
 * - Real-time user action XP hooks:
 *   * Deflame tap: +20 XP
 *   * Girl activation: +50 XP
 *   * Single Strike recorded: +100 XP
 *   * New Person added: +500 XP (+1,000 XP additional bonus if Size > 200 -> 1,500 XP total)
 * - Retroactive start-date telemetry scanner from `trackingStartDate` ("تاريخ بداية العد")
 *   with strict idempotency so historical entries are never double-counted.
 */
object XpCalculationEngine {

    const val XP_PER_DEFLAME = 20L
    const val XP_PER_GIRL = 50L
    const val XP_PER_STRIKE = 100L
    const val XP_PER_PERSON_BASE = 500L
    const val XP_PER_PERSON_SIZE_BONUS = 1_000L
    const val PERSON_SIZE_BONUS_THRESHOLD = 200.0

    const val LEVEL_1_THRESHOLD_XP = TaskPools.ROADMAP_LEVEL_1_XP
    const val LEVEL_500_THRESHOLD_XP = TaskPools.ROADMAP_LEVEL_500_XP

    private const val PREFS_NAME = "strikepulse_xp_engine_prefs"
    private const val KEY_CREDITED_TELEMETRY_XP = "key_credited_telemetry_xp"
    private const val KEY_LAST_SYNCED_START_DATE = "key_last_synced_start_date"

    private val _creditedTelemetryXp = MutableStateFlow(0L)
    val creditedTelemetryXp: StateFlow<Long> = _creditedTelemetryXp.asStateFlow()

    private val _lastBreakdown = MutableStateFlow(RetroactiveXpBreakdown())
    val lastBreakdown: StateFlow<RetroactiveXpBreakdown> = _lastBreakdown.asStateFlow()

    private var isInitialized = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        _creditedTelemetryXp.value = prefs.getLong(KEY_CREDITED_TELEMETRY_XP, 0L).coerceAtLeast(0L)
        val savedDate = prefs.getString(KEY_LAST_SYNCED_START_DATE, null)
        if (!savedDate.isNullOrBlank()) {
            _lastBreakdown.value = _lastBreakdown.value.copy(trackingStartDate = savedDate)
        }
        isInitialized = true
    }

    @Synchronized
    fun resetForTesting(context: Context) {
        isInitialized = false
        init(context)
    }

    @Synchronized
    fun resetAllToZero(context: Context) {
        init(context)
        _creditedTelemetryXp.value = 0L
        _lastBreakdown.value = RetroactiveXpBreakdown()
        getPrefs(context).edit()
            .putLong(KEY_CREDITED_TELEMETRY_XP, 0L)
            .remove(KEY_LAST_SYNCED_START_DATE)
            .apply()
    }

    /**
     * Independent smooth progression threshold for Station [level] (1..500),
     * scaling monotonically from 1,345 XP (Level 1) to 1,000,000 XP (Level 500).
     */
    fun getRequiredXpForLevel(level: Int): Long = TaskPools.getRequiredXpForStation(level)

    fun getCumulativeXpForLevel(level: Int): Long = TaskPools.getCumulativeXpForStation(level)

    fun calculateLevelForXp(totalXp: Long): Int = RoadmapManager.calculateUnlockedStationId(totalXp)

    fun getStageProgress(totalXp: Long): RoadmapManager.StageXpProgress = RoadmapManager.getStageProgress(totalXp)

    fun xpRemainingToNextLevel(totalXp: Long): Long {
        return RoadmapManager.getStageProgress(totalXp).remainingXpForNextStage
    }

    fun calculatePersonAdditionXp(size: Double): Long {
        return XP_PER_PERSON_BASE + if (size > PERSON_SIZE_BONUS_THRESHOLD) XP_PER_PERSON_SIZE_BONUS else 0L
    }

    @Synchronized
    private fun recordLiveTelemetryXp(context: Context, xpAmount: Long): Long {
        if (xpAmount <= 0L) return 0L
        init(context)
        val updatedCredited = (_creditedTelemetryXp.value + xpAmount).coerceAtLeast(0L)
        _creditedTelemetryXp.value = updatedCredited
        getPrefs(context).edit().putLong(KEY_CREDITED_TELEMETRY_XP, updatedCredited).apply()
        RoadmapManager.addXp(context, xpAmount)
        return xpAmount
    }

    /**
     * Awards +20 XP for a single Deflame button tap.
     */
    @Synchronized
    fun onDeflameAction(context: Context): Long {
        return recordLiveTelemetryXp(context, XP_PER_DEFLAME)
    }

    /**
     * Awards +50 XP for a single Girl button activation.
     */
    @Synchronized
    fun onGirlAction(context: Context): Long {
        return recordLiveTelemetryXp(context, XP_PER_GIRL)
    }

    /**
     * Awards +100 XP per recorded Strike (+100 * strikeCount).
     */
    @Synchronized
    fun onStrikeRecorded(context: Context, strikeCount: Int = 1): Long {
        val count = strikeCount.coerceAtLeast(1)
        return recordLiveTelemetryXp(context, XP_PER_STRIKE * count)
    }

    /**
     * Awards +500 XP for adding a new person, plus an additional +1,000 XP
     * if their total Size is strictly greater than 200 (1,500 XP total).
     */
    @Synchronized
    fun onNewPersonAdded(context: Context, size: Double): Long {
        return recordLiveTelemetryXp(context, calculatePersonAdditionXp(size))
    }

    /**
     * Computes exact XP earned from Flame logs (DEFLAME, GIRL), Strike events, and added Individuals
     * starting strictly at 00:00:00 local time on [trackingStartDate] (yyyy-MM-dd).
     */
    fun calculateTelemetryFromLogs(
        trackingStartDate: String?,
        flameLogs: List<FlameLogEntry>,
        strikeEvents: List<StrikeEvent>,
        individuals: List<Individual>
    ): RetroactiveXpBreakdown {
        if (trackingStartDate.isNullOrBlank()) {
            return RetroactiveXpBreakdown(trackingStartDate = null)
        }
        val startMillis = parseStartOfDayMillis(trackingStartDate)
            ?: return RetroactiveXpBreakdown(trackingStartDate = trackingStartDate)

        val deflameCount = flameLogs
            .filter { it.reason == "DEFLAME" && it.timestamp >= startMillis }
            .sumOf { it.actionCount.coerceAtLeast(1) }
        val girlCount = flameLogs
            .filter { it.reason == "GIRL" && it.timestamp >= startMillis }
            .sumOf { it.actionCount.coerceAtLeast(1) }

        val relevantStrikes = strikeEvents.filter { it.date >= trackingStartDate }
        val strikeCount = relevantStrikes.sumOf { it.count.coerceAtLeast(1) }

        val relevantPersons = individuals.filter { it.updatedAt >= startMillis }
        val personCount = relevantPersons.size
        val largePersonBonusCount = relevantPersons.count { it.size > PERSON_SIZE_BONUS_THRESHOLD }

        val deflameXp = deflameCount * XP_PER_DEFLAME
        val girlXp = girlCount * XP_PER_GIRL
        val strikeXp = strikeCount * XP_PER_STRIKE
        val personBaseXp = personCount * XP_PER_PERSON_BASE
        val personSizeBonusXp = largePersonBonusCount * XP_PER_PERSON_SIZE_BONUS
        val total = deflameXp + girlXp + strikeXp + personBaseXp + personSizeBonusXp

        return RetroactiveXpBreakdown(
            trackingStartDate = trackingStartDate,
            deflameCount = deflameCount,
            girlCount = girlCount,
            strikeCount = strikeCount,
            personCount = personCount,
            largePersonBonusCount = largePersonBonusCount,
            deflameXp = deflameXp,
            girlXp = girlXp,
            strikeXp = strikeXp,
            personBaseXp = personBaseXp,
            personSizeBonusXp = personSizeBonusXp,
            totalTelemetryXp = total,
            deltaCreditedXp = 0L
        )
    }

    /**
     * Scans all historical user logs/records starting strictly from [trackingStartDate]
     * and credits any missing retroactive XP without double-counting previously credited actions.
     *
     * When [recalculateExact] is true (e.g., user explicitly changes `trackingStartDate`),
     * reconciles exact telemetry delta (positive or negative) relative to previous credited telemetry XP.
     */
    @Synchronized
    fun syncRetroactiveTelemetry(
        context: Context,
        trackingStartDate: String?,
        flameLogs: List<FlameLogEntry>,
        strikeEvents: List<StrikeEvent>,
        individuals: List<Individual>,
        recalculateExact: Boolean = false
    ): RetroactiveXpBreakdown {
        init(context)
        RoadmapManager.init(context)

        if (trackingStartDate.isNullOrBlank()) {
            return RetroactiveXpBreakdown(trackingStartDate = null)
        }

        val computed = calculateTelemetryFromLogs(
            trackingStartDate = trackingStartDate,
            flameLogs = flameLogs,
            strikeEvents = strikeEvents,
            individuals = individuals
        )

        val prevCredited = _creditedTelemetryXp.value
        val targetCredited = if (recalculateExact) {
            computed.totalTelemetryXp
        } else {
            maxOf(prevCredited, computed.totalTelemetryXp)
        }
        val delta = targetCredited - prevCredited

        if (delta != 0L) {
            RoadmapManager.addXp(context, delta)
        }

        _creditedTelemetryXp.value = targetCredited
        getPrefs(context).edit()
            .putLong(KEY_CREDITED_TELEMETRY_XP, targetCredited)
            .putString(KEY_LAST_SYNCED_START_DATE, trackingStartDate)
            .apply()

        val result = computed.copy(deltaCreditedXp = delta)
        _lastBreakdown.value = result
        return result
    }

    private fun parseStartOfDayMillis(dateStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = sdf.parse(dateStr.trim()) ?: return null
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun exportSnapshot(context: Context): JSONObject {
        init(context)
        val prefs = getPrefs(context)
        return JSONObject().apply {
            put("creditedTelemetryXp", _creditedTelemetryXp.value)
            put("lastSyncedStartDate", prefs.getString(KEY_LAST_SYNCED_START_DATE, "") ?: "")
        }
    }

    @Synchronized
    fun restoreSnapshot(context: Context, json: JSONObject, replaceExisting: Boolean) {
        init(context)
        val importedCredited = json.optLong("creditedTelemetryXp", 0L).coerceAtLeast(0L)
        val finalCredited = if (replaceExisting) {
            importedCredited
        } else {
            (_creditedTelemetryXp.value + importedCredited).coerceAtLeast(0L)
        }
        val importedDate = json.optString("lastSyncedStartDate", "").trim()

        _creditedTelemetryXp.value = finalCredited
        getPrefs(context).edit().apply {
            putLong(KEY_CREDITED_TELEMETRY_XP, finalCredited)
            if (importedDate.isNotEmpty()) {
                putString(KEY_LAST_SYNCED_START_DATE, importedDate)
            } else if (replaceExisting) {
                remove(KEY_LAST_SYNCED_START_DATE)
            }
            apply()
        }
        if (importedDate.isNotEmpty()) {
            _lastBreakdown.value = _lastBreakdown.value.copy(trackingStartDate = importedDate)
        }
        isInitialized = true
    }
}
