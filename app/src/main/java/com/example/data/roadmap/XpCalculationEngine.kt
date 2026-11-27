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
    const val LEVEL_700_THRESHOLD_XP = TaskPools.ROADMAP_LEVEL_700_XP

    private const val PREFS_NAME = "strikepulse_xp_engine_prefs"
    private const val KEY_CREDITED_TELEMETRY_XP = "key_credited_telemetry_xp"
    private const val KEY_LAST_SYNCED_START_DATE = "key_last_synced_start_date"
    private const val KEY_BASELINE_RESET_AT_MILLIS = "key_baseline_reset_at_millis"

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
            .putLong(KEY_BASELINE_RESET_AT_MILLIS, System.currentTimeMillis())
            .remove(KEY_LAST_SYNCED_START_DATE)
            .apply()
    }

    fun isBaselineResetActive(context: Context): Boolean {
        return getPrefs(context).getLong(KEY_BASELINE_RESET_AT_MILLIS, 0L) > 0L
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
    private fun recordLiveTelemetryXp(
        context: Context,
        xpAmount: Long,
        category: XpSourceCategory = XpSourceCategory.SYSTEM,
        concernedParty: String = "نقاط اللعبة المباشرة",
        concernedPartyEn: String = "Live Game Telemetry",
        detail: String = "",
        detailEn: String = detail
    ): Long {
        if (xpAmount <= 0L) return 0L
        init(context)
        val updatedCredited = (_creditedTelemetryXp.value + xpAmount).coerceAtLeast(0L)
        _creditedTelemetryXp.value = updatedCredited
        getPrefs(context).edit().putLong(KEY_CREDITED_TELEMETRY_XP, updatedCredited).apply()
        RoadmapManager.addXp(
            context = context,
            amount = xpAmount,
            category = category,
            concernedParty = concernedParty,
            concernedPartyEn = concernedPartyEn,
            detail = detail,
            detailEn = detailEn
        )
        return xpAmount
    }

    /**
     * Awards +20 XP for a single Deflame button tap.
     */
    @Synchronized
    fun onDeflameAction(context: Context, bonusNote: String = ""): Long {
        val detailAr = if (bonusNote.isNotBlank()) "ضغطة زر التبريد • $bonusNote" else "ضغطة زر التبريد (Deflame)"
        val detailEn = if (bonusNote.isNotBlank()) "Deflame tap • $bonusNote" else "Deflame button tap"
        return recordLiveTelemetryXp(
            context = context,
            xpAmount = XP_PER_DEFLAME,
            category = XpSourceCategory.DEFLAME,
            concernedParty = "زر التبريد (Deflame)",
            concernedPartyEn = "Deflame Action",
            detail = detailAr,
            detailEn = detailEn
        )
    }

    /**
     * Awards +50 XP for a single Girl button activation.
     */
    @Synchronized
    fun onGirlAction(context: Context): Long {
        return recordLiveTelemetryXp(
            context = context,
            xpAmount = XP_PER_GIRL,
            category = XpSourceCategory.GIRL,
            concernedParty = "تفعيل (Girl)",
            concernedPartyEn = "Girl Action",
            detail = "تبريد عبر زر Girl",
            detailEn = "Cooldown via Girl action"
        )
    }

    /**
     * Awards +100 XP per recorded Strike (+100 * strikeCount).
     */
    @Synchronized
    fun onStrikeRecorded(
        context: Context,
        strikeCount: Int = 1,
        personName: String? = null
    ): Long {
        val count = strikeCount.coerceAtLeast(1)
        val cleanName = personName?.trim()?.takeIf { it.isNotEmpty() }
        val partyAr = if (cleanName != null) "سترايك: $cleanName" else "تسجيل سترايك"
        val partyEn = if (cleanName != null) "Strike: $cleanName" else "Strike Recorded"
        val detailAr = if (count > 1) "تسجيل $count سترايك" else "تسجيل سترايك واحد (+100 XP)"
        val detailEn = if (count > 1) "$count strikes recorded" else "Single strike recorded (+100 XP)"
        return recordLiveTelemetryXp(
            context = context,
            xpAmount = XP_PER_STRIKE * count,
            category = XpSourceCategory.STRIKE,
            concernedParty = partyAr,
            concernedPartyEn = partyEn,
            detail = detailAr,
            detailEn = detailEn
        )
    }

    /**
     * Awards +500 XP for adding a new person, plus an additional +1,000 XP
     * if their total Size is strictly greater than 200 (1,500 XP total).
     */
    @Synchronized
    fun onNewPersonAdded(
        context: Context,
        size: Double,
        personName: String? = null
    ): Long {
        val cleanName = personName?.trim()?.takeIf { it.isNotEmpty() }
        val hasSizeBonus = size > PERSON_SIZE_BONUS_THRESHOLD
        val partyAr = if (cleanName != null) "فرد جديد: $cleanName" else "إضافة فرد جديد"
        val partyEn = if (cleanName != null) "New Person: $cleanName" else "New Person Added"
        val detailAr = if (hasSizeBonus) {
            "إضافة فرد (+500 XP) + مكافأة الحجم Size > 200 (+1,000 XP)"
        } else {
            "إضافة فرد جديد (+500 XP)"
        }
        val detailEn = if (hasSizeBonus) {
            "New person (+500 XP) + Size > 200 bonus (+1,000 XP)"
        } else {
            "New person added (+500 XP)"
        }
        return recordLiveTelemetryXp(
            context = context,
            xpAmount = calculatePersonAdditionXp(size),
            category = XpSourceCategory.PERSON,
            concernedParty = partyAr,
            concernedPartyEn = partyEn,
            detail = detailAr,
            detailEn = detailEn
        )
    }

    /**
     * Computes exact all-time XP earned from all Flame logs (DEFLAME, GIRL), Strike events
     * (including any legacy Individual.strikeCount excess without event rows), and added Individuals.
     * Used when no Flame trackingStartDate filter is configured so all real player data counts
     * immediately toward Roadmap Level.
     */
    fun calculateAllTimeTelemetryFromLogs(
        flameLogs: List<FlameLogEntry>,
        strikeEvents: List<StrikeEvent>,
        individuals: List<Individual>,
        minTimestampExclusiveMillis: Long = 0L
    ): RetroactiveXpBreakdown {
        val afterResetLogs = if (minTimestampExclusiveMillis > 0L) {
            flameLogs.filter { it.timestamp > minTimestampExclusiveMillis }
        } else {
            flameLogs
        }
        val deflameCount = afterResetLogs
            .filter { it.reason == "DEFLAME" }
            .sumOf { it.actionCount.coerceAtLeast(1) }
        val girlCount = afterResetLogs
            .filter { it.reason == "GIRL" }
            .sumOf { it.actionCount.coerceAtLeast(1) }

        val relevantEvents = if (minTimestampExclusiveMillis > 0L) {
            strikeEvents.filter { it.timestamp > minTimestampExclusiveMillis }
        } else {
            strikeEvents
        }
        val eventStrikeCount = relevantEvents.sumOf { it.count.coerceAtLeast(1) }
        val legacyUntrackedStrikes = if (minTimestampExclusiveMillis == 0L) {
            val eventUnitsByPerson = strikeEvents.groupBy { it.individualId }
                .mapValues { (_, list) -> list.sumOf { it.count.coerceAtLeast(1) } }
            individuals.sumOf { person ->
                val tracked = eventUnitsByPerson[person.id] ?: 0
                (person.strikeCount - tracked).coerceAtLeast(0)
            }
        } else {
            0
        }
        val strikeCount = eventStrikeCount + legacyUntrackedStrikes

        val relevantPersons = if (minTimestampExclusiveMillis > 0L) {
            individuals.filter { it.updatedAt > minTimestampExclusiveMillis }
        } else {
            individuals
        }
        val personCount = relevantPersons.size
        val largePersonBonusCount = relevantPersons.count { it.size > PERSON_SIZE_BONUS_THRESHOLD }

        val deflameXp = deflameCount * XP_PER_DEFLAME
        val girlXp = girlCount * XP_PER_GIRL
        val strikeXp = strikeCount * XP_PER_STRIKE
        val personBaseXp = personCount * XP_PER_PERSON_BASE
        val personSizeBonusXp = largePersonBonusCount * XP_PER_PERSON_SIZE_BONUS
        val total = deflameXp + girlXp + strikeXp + personBaseXp + personSizeBonusXp

        return RetroactiveXpBreakdown(
            trackingStartDate = null,
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
     * Scans all-time user telemetry (when no start-date filter is active) and idempotently
     * credits any uncredited XP up to [maxOf(prevCredited, computed.totalTelemetryXp)].
     */
    @Synchronized
    fun syncAllTimeTelemetry(
        context: Context,
        flameLogs: List<FlameLogEntry>,
        strikeEvents: List<StrikeEvent>,
        individuals: List<Individual>
    ): RetroactiveXpBreakdown {
        init(context)
        RoadmapManager.init(context)

        val baselineResetAt = getPrefs(context).getLong(KEY_BASELINE_RESET_AT_MILLIS, 0L)
        val computed = calculateAllTimeTelemetryFromLogs(
            flameLogs = flameLogs,
            strikeEvents = strikeEvents,
            individuals = individuals,
            minTimestampExclusiveMillis = baselineResetAt
        )

        val prevCredited = _creditedTelemetryXp.value
        val targetCredited = maxOf(prevCredited, computed.totalTelemetryXp)
        val delta = targetCredited - prevCredited

        if (delta > 0L) {
            val partsAr = buildList {
                if (computed.strikeCount > 0) add("${computed.strikeCount} سترايك")
                if (computed.deflameCount > 0) add("${computed.deflameCount} Deflame")
                if (computed.girlCount > 0) add("${computed.girlCount} Girl")
                if (computed.personCount > 0) add("${computed.personCount} أفراد")
            }.joinToString(" • ").ifBlank { "مزامنة السجلات الفعلية" }
            val partsEn = buildList {
                if (computed.strikeCount > 0) add("${computed.strikeCount} strikes")
                if (computed.deflameCount > 0) add("${computed.deflameCount} Deflame")
                if (computed.girlCount > 0) add("${computed.girlCount} Girl")
                if (computed.personCount > 0) add("${computed.personCount} persons")
            }.joinToString(" • ").ifBlank { "Actual telemetry sync" }
            RoadmapManager.addXp(
                context = context,
                amount = delta,
                category = XpSourceCategory.RETROACTIVE,
                concernedParty = "مزامنة التقدم الفعلي",
                concernedPartyEn = "Actual Telemetry Sync",
                detail = partsAr,
                detailEn = partsEn
            )
            _creditedTelemetryXp.value = targetCredited
            getPrefs(context).edit()
                .putLong(KEY_CREDITED_TELEMETRY_XP, targetCredited)
                .apply()
        }

        val result = computed.copy(deltaCreditedXp = delta)
        _lastBreakdown.value = result
        return result
    }

    /**
     * Computes exact XP earned from Flame logs (DEFLAME, GIRL), Strike events, and added Individuals
     * starting strictly at 00:00:00 local time on each element's start date up to 23:59:59.999 on
     * its end date (unbounded at end when end date is null).
     */
    fun calculateTelemetryFromLogs(
        trackingStartDate: String?,
        flameLogs: List<FlameLogEntry>,
        strikeEvents: List<StrikeEvent>,
        individuals: List<Individual>,
        trackingEndDate: String? = null,
        strikesStartDate: String? = trackingStartDate,
        strikesEndDate: String? = trackingEndDate,
        girlStartDate: String? = trackingStartDate,
        girlEndDate: String? = trackingEndDate,
        deflameStartDate: String? = trackingStartDate,
        deflameEndDate: String? = trackingEndDate,
        includeAllHistoricalStrikes: Boolean = false,
        includeAllIndividuals: Boolean = false
    ): RetroactiveXpBreakdown {
        if (trackingStartDate.isNullOrBlank()) {
            return RetroactiveXpBreakdown(trackingStartDate = null)
        }
        val startMillis = parseStartOfDayMillis(trackingStartDate)
            ?: return RetroactiveXpBreakdown(trackingStartDate = trackingStartDate)
        val endMillis = trackingEndDate?.takeIf { it.isNotBlank() }?.let { parseEndOfDayMillis(it) }

        val effDeflameStart = deflameStartDate?.takeIf { it.isNotBlank() }?.let { parseStartOfDayMillis(it) } ?: startMillis
        val effDeflameEnd = deflameEndDate?.takeIf { it.isNotBlank() }?.let { parseEndOfDayMillis(it) } ?: endMillis

        val effGirlStart = girlStartDate?.takeIf { it.isNotBlank() }?.let { parseStartOfDayMillis(it) } ?: startMillis
        val effGirlEnd = girlEndDate?.takeIf { it.isNotBlank() }?.let { parseEndOfDayMillis(it) } ?: endMillis

        val effStrikesStartStr = strikesStartDate?.takeIf { it.isNotBlank() } ?: trackingStartDate
        val effStrikesEndStr = strikesEndDate?.takeIf { it.isNotBlank() } ?: trackingEndDate

        val deflameCount = flameLogs
            .filter {
                it.reason == "DEFLAME" &&
                    it.timestamp >= effDeflameStart &&
                    (effDeflameEnd == null || it.timestamp <= effDeflameEnd)
            }
            .sumOf { it.actionCount.coerceAtLeast(1) }
        val girlCount = flameLogs
            .filter {
                it.reason == "GIRL" &&
                    it.timestamp >= effGirlStart &&
                    (effGirlEnd == null || it.timestamp <= effGirlEnd)
            }
            .sumOf { it.actionCount.coerceAtLeast(1) }

        val relevantStrikes = strikeEvents.filter { ev ->
            val inDateWindow = ev.date >= effStrikesStartStr && (effStrikesEndStr.isNullOrBlank() || ev.date <= effStrikesEndStr)
            val isHistoricalEntry = ev.strikeType.startsWith("Historical", ignoreCase = true)
            inDateWindow || (includeAllHistoricalStrikes && isHistoricalEntry)
        }
        val strikeCount = relevantStrikes.sumOf { it.count.coerceAtLeast(1) }

        val relevantPersons = if (includeAllIndividuals) {
            individuals
        } else {
            individuals.filter {
                it.updatedAt >= startMillis && (endMillis == null || it.updatedAt <= endMillis)
            }
        }
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
     * Scans all historical user logs/records within the configured start/end bounds
     * and credits any missing retroactive XP without double-counting previously credited actions.
     *
     * When [recalculateExact] is true (e.g., user explicitly updates date ranges),
     * reconciles exact telemetry delta (positive or negative) relative to previous credited telemetry XP.
     */
    @Synchronized
    fun syncRetroactiveTelemetry(
        context: Context,
        trackingStartDate: String?,
        flameLogs: List<FlameLogEntry>,
        strikeEvents: List<StrikeEvent>,
        individuals: List<Individual>,
        recalculateExact: Boolean = false,
        trackingEndDate: String? = null,
        strikesStartDate: String? = trackingStartDate,
        strikesEndDate: String? = trackingEndDate,
        girlStartDate: String? = trackingStartDate,
        girlEndDate: String? = trackingEndDate,
        deflameStartDate: String? = trackingStartDate,
        deflameEndDate: String? = trackingEndDate,
        includeAllHistoricalStrikes: Boolean = false,
        includeAllIndividuals: Boolean = false
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
            individuals = individuals,
            trackingEndDate = trackingEndDate,
            strikesStartDate = strikesStartDate,
            strikesEndDate = strikesEndDate,
            girlStartDate = girlStartDate,
            girlEndDate = girlEndDate,
            deflameStartDate = deflameStartDate,
            deflameEndDate = deflameEndDate,
            includeAllHistoricalStrikes = includeAllHistoricalStrikes,
            includeAllIndividuals = includeAllIndividuals
        )

        val prevCredited = _creditedTelemetryXp.value
        val targetCredited = if (recalculateExact) {
            computed.totalTelemetryXp
        } else {
            maxOf(prevCredited, computed.totalTelemetryXp)
        }
        val delta = targetCredited - prevCredited

        if (delta != 0L) {
            val partsAr = buildList {
                if (computed.strikeCount > 0) add("${computed.strikeCount} سترايك")
                if (computed.deflameCount > 0) add("${computed.deflameCount} Deflame")
                if (computed.girlCount > 0) add("${computed.girlCount} Girl")
                if (computed.personCount > 0) add("${computed.personCount} أفراد")
            }.joinToString(" • ").ifBlank { "فحص السجلات التاريخية" }
            val partsEn = buildList {
                if (computed.strikeCount > 0) add("${computed.strikeCount} strikes")
                if (computed.deflameCount > 0) add("${computed.deflameCount} Deflame")
                if (computed.girlCount > 0) add("${computed.girlCount} Girl")
                if (computed.personCount > 0) add("${computed.personCount} persons")
            }.joinToString(" • ").ifBlank { "Historical log scan" }
            val rangeLabel = if (!trackingEndDate.isNullOrBlank()) "$trackingStartDate → $trackingEndDate" else trackingStartDate
            RoadmapManager.addXp(
                context = context,
                amount = delta,
                category = XpSourceCategory.RETROACTIVE,
                concernedParty = "مزامنة السجل التاريخي ($rangeLabel)",
                concernedPartyEn = "Retroactive Sync ($rangeLabel)",
                detail = partsAr,
                detailEn = partsEn
            )
        }

        _creditedTelemetryXp.value = targetCredited
        getPrefs(context).edit()
            .putLong(KEY_CREDITED_TELEMETRY_XP, targetCredited)
            .putString(KEY_LAST_SYNCED_START_DATE, trackingStartDate)
            .remove(KEY_BASELINE_RESET_AT_MILLIS)
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

    private fun parseEndOfDayMillis(dateStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = sdf.parse(dateStr.trim()) ?: return null
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
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
            remove(KEY_BASELINE_RESET_AT_MILLIS)
            apply()
        }
        if (importedDate.isNotEmpty()) {
            _lastBreakdown.value = _lastBreakdown.value.copy(trackingStartDate = importedDate)
        }
        isInitialized = true
    }
}
