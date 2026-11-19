package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupResult
import com.example.data.flame.FlameLogEntry
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.profile.ProfileManager
import com.example.data.repository.IndividualRepository
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.RewardType
import com.example.data.roadmap.TaskItem
import com.example.data.roadmap.XpCalculationEngine
import com.example.data.roadmap.XpHistoryEntry
import com.example.util.SmartSearchMatcher
import com.example.util.SmartSearchResult
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateState
import com.example.data.update.GitHubReleaseInfo
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.components.StrikerStatus
import com.example.ui.components.StrikerStatusEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class SortOrder {
    NAME_ASC,
    STRIKES_DESC,
    BOOST_DESC,
    SIZE_DESC,
    SCORE_DESC // BOOST * strikes
}

data class YearlyStrikeStat(
    val year: Int,
    val totalStrikes: Int,
    val eventCount: Int,
    val topIndividualName: String,
    val topIndividualStrikes: Int,
    val percentageOfAllTime: Float
)

sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
    data class ShowUndoSnackbar(
        val message: String,
        val actionLabel: String,
        val strikeEvent: StrikeEvent
    ) : UiEvent()
}

class IndividualViewModel(
    application: Application,
    private val repository: IndividualRepository
) : AndroidViewModel(application) {

    private val securityPrefs = SecurityPreferences(application)
    val backupManager = BackupManager(repository)

    // Security, Fingerprint Authentication, Haptics & Auto-Update
    val isFingerprintLockEnabled = mutableStateOf(securityPrefs.isFingerprintLockEnabled)
    val isHapticFeedbackEnabled = mutableStateOf(securityPrefs.isHapticFeedbackEnabled)
    val isAutoUpdateEnabled = mutableStateOf(securityPrefs.isAutoUpdateEnabled)
    val appUpdateState: StateFlow<AppUpdateState> = AppUpdateManager.state
    private val _isAppUnlocked = MutableStateFlow(!securityPrefs.isFingerprintLockEnabled)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Events flow for snackbars/toasts
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private suspend fun emitMessage(msg: String) {
        _eventFlow.emit(msg)
        _uiEvents.emit(UiEvent.ShowMessage(msg))
    }

    // Strike button animation trigger
    private val _strikeAnimCount = MutableStateFlow(0)
    val strikeAnimCount: StateFlow<Int> = _strikeAnimCount.asStateFlow()

    // Individuals stream
    val allIndividuals: StateFlow<List<Individual>> = repository.allIndividuals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Strike events stream
    val allStrikeEvents: StateFlow<List<StrikeEvent>> = repository.allStrikeEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All-Time Strikes Count directly from unfiltered SQL aggregate query (no limits or date restrictions)
    val allTimeStrikesCount: StateFlow<Int> = repository.allTimeStrikesCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Yearly Strike Dashboard Analytics computed directly from unfiltered SQL aggregation
    val yearlyStrikeStats: StateFlow<List<YearlyStrikeStat>> = combine(
        repository.yearlyPersonAggregatesFlow,
        allTimeStrikesCount
    ) { rows, allTimeRaw ->
        if (rows.isEmpty()) return@combine emptyList()

        val allTimeTotal = maxOf(allTimeRaw, rows.sumOf { it.totalStrikes }).coerceAtLeast(1)

        rows.groupBy { it.year }.map { (year, yearRows) ->
            val yearTotal = yearRows.sumOf { it.totalStrikes }
            val eventTotal = yearRows.sumOf { it.eventCount }
            val topPerson = yearRows.maxByOrNull { it.totalStrikes }

            YearlyStrikeStat(
                year = year,
                totalStrikes = yearTotal,
                eventCount = eventTotal,
                topIndividualName = topPerson?.individualName ?: "—",
                topIndividualStrikes = topPerson?.totalStrikes ?: 0,
                percentageOfAllTime = (yearTotal.toFloat() / allTimeTotal.toFloat()).coerceIn(0f, 1f)
            )
        }.sortedByDescending { it.year }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Map of individualId to timestamp of latest recorded strike event via SQL aggregation
    val individualLastStrikeMap: StateFlow<Map<Long, Long>> =
        repository.latestStrikeDateByIndividualFlow.map { rows ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            rows.associate { tuple ->
                val ts = tuple.latestDate?.let { dateStr ->
                    try {
                        dateFormat.parse(dateStr)?.time
                    } catch (_: Exception) {
                        null
                    }
                } ?: 0L
                tuple.individualId to ts
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun getStatusForIndividual(
        individual: Individual,
        lastStrikeMap: Map<Long, Long>? = null
    ): StrikerStatus {
        val lastTimestamp = lastStrikeMap?.get(individual.id)
        return StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = lastTimestamp,
            strikeCount = individual.strikeCount,
            updatedAt = individual.updatedAt
        )
    }

    // Search & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("ALL")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.SCORE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val filteredIndividuals: StateFlow<List<Individual>> = combine(
        allIndividuals,
        _searchQuery,
        _filterType,
        _sortOrder
    ) { list, query, filter, sort ->
        val typeFiltered = list.filter { item ->
            filter == "ALL" || item.type.equals(filter, ignoreCase = true)
        }
        if (query.isBlank()) {
            when (sort) {
                SortOrder.NAME_ASC -> typeFiltered.sortedBy { it.fullName.lowercase() }
                SortOrder.STRIKES_DESC -> typeFiltered.sortedByDescending { it.strikeCount }
                SortOrder.BOOST_DESC -> typeFiltered.sortedByDescending { it.boost }
                SortOrder.SIZE_DESC -> typeFiltered.sortedByDescending { it.size }
                SortOrder.SCORE_DESC -> typeFiltered.sortedByDescending { it.totalScore }
            }
        } else {
            SmartSearchMatcher.filterAndRank(typeFiltered, query).map { it.individual }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Instant Smart Search Engine State (replaces the old FlameEnergyBanner header slot)
    private val _strikeSearchQuery = MutableStateFlow("")
    val strikeSearchQuery: StateFlow<String> = _strikeSearchQuery.asStateFlow()

    val filteredStrikeIndividuals: StateFlow<List<SmartSearchResult>> = combine(
        allIndividuals,
        _strikeSearchQuery
    ) { list, query ->
        SmartSearchMatcher.filterAndRank(list, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Strike Screen Selection
    private val _selectedStrikeId = MutableStateFlow<Long?>(null)
    val selectedStrikeId: StateFlow<Long?> = _selectedStrikeId.asStateFlow()

    val currentStrikeIndividual: StateFlow<Individual?> = combine(
        allIndividuals,
        _selectedStrikeId
    ) { list, id ->
        if (id == null) {
            list.firstOrNull()
        } else {
            list.find { it.id == id } ?: list.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Comparison Screen Selections
    private val _personAId = MutableStateFlow<Long?>(null)
    val personAId: StateFlow<Long?> = _personAId.asStateFlow()

    private val _personBId = MutableStateFlow<Long?>(null)
    val personBId: StateFlow<Long?> = _personBId.asStateFlow()

    val personA: StateFlow<Individual?> = combine(
        allIndividuals,
        _personAId
    ) { list, id ->
        if (id != null) list.find { it.id == id }
        else list.getOrNull(0)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val personB: StateFlow<Individual?> = combine(
        allIndividuals,
        _personBId
    ) { list, id ->
        if (id != null) list.find { it.id == id }
        else if (list.size > 1) list[1]
        else list.getOrNull(0)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Data Entry Modal & Saving State
    private val _isDataEntryOpen = MutableStateFlow(false)
    val isDataEntryOpen: StateFlow<Boolean> = _isDataEntryOpen.asStateFlow()

    private val _isSavingForm = MutableStateFlow(false)
    val isSavingForm: StateFlow<Boolean> = _isSavingForm.asStateFlow()

    // Data Entry Form State
    var editingId: Long? = null
        private set

    val formName = MutableStateFlow("")
    val formPhoneNumber = MutableStateFlow("")
    val formType = MutableStateFlow("M")
    val formLength = MutableStateFlow("20.0")
    val formWidth = MutableStateFlow("10.0")
    val formSScore = MutableStateFlow("85.0")
    val formFScore = MutableStateFlow("80.0")
    val formCScore = MutableStateFlow("90.0")
    val formError = MutableStateFlow<String?>(null)

    // Historical strikes per year entries for the individual: (Year, StrikesCount)
    val formHistoricalEntries = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())

    // Live Auto-Calculations
    val liveCalculatedSize: StateFlow<Double> = combine(formLength, formWidth) { l, w ->
        val lengthVal = parseLocalizedDouble(l) ?: 0.0
        val widthVal = parseLocalizedDouble(w) ?: 0.0
        Individual.calculateSize(lengthVal, widthVal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 200.0
    )

    val liveCalculatedBoost: StateFlow<Double> = combine(
        formSScore,
        formFScore,
        formCScore,
        liveCalculatedSize
    ) { s, f, c, sizeVal ->
        val sVal = parseLocalizedDouble(s) ?: 0.0
        val fVal = parseLocalizedDouble(f) ?: 0.0
        val cVal = parseLocalizedDouble(c) ?: 0.0
        Individual.calculateBoost(sVal, fVal, cVal, sizeVal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 257.0
    )

    // Language state
    private val _currentLanguage = MutableStateFlow(securityPrefs.selectedLanguage)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Flame System State
    val flameScore: StateFlow<Double> = FlameManager.flameScore
    val flameLogs: StateFlow<List<FlameLogEntry>> = FlameManager.flameLogs
    val isStreakShieldActive: StateFlow<Boolean> = FlameManager.isStreakShieldActive
    val activeShieldExpiresAt: StateFlow<Long?> = FlameManager.activeShieldExpiresAt
    val activeShieldType: StateFlow<com.example.data.flame.ActiveShieldType> = FlameManager.activeShieldType
    val availableManualShields: StateFlow<Int> = FlameManager.availableManualShields
    val available48hShields: StateFlow<Int> = FlameManager.available48hShields
    val available1DayShields: StateFlow<Int> = FlameManager.available1DayShields
    val sevenDayAutoShieldCount: StateFlow<Int> = FlameManager.sevenDayAutoShieldCount
    val peakFlameScore: StateFlow<Double> = FlameManager.peakScore
    val totalPointsGained: StateFlow<Double> = FlameManager.totalPointsGained
    val totalPointsExtinguished: StateFlow<Double> = FlameManager.totalPointsExtinguished

    // 500-Station Wavy Roadmap & XP Progression Engine State
    val totalXp: StateFlow<Long> = RoadmapManager.totalXp
    val xpHistory: StateFlow<List<XpHistoryEntry>> = RoadmapManager.xpHistory
    val claimedStations: StateFlow<Set<Int>> = RoadmapManager.claimedStations
    val dailyTasks: StateFlow<List<TaskItem>> = RoadmapManager.dailyTasks
    val weeklyTasks: StateFlow<List<TaskItem>> = RoadmapManager.weeklyTasks
    val monthlyTasks: StateFlow<List<TaskItem>> = RoadmapManager.monthlyTasks
    val yearlyTasks: StateFlow<List<TaskItem>> = RoadmapManager.yearlyTasks
    val everlastingTasks: StateFlow<List<TaskItem>> = RoadmapManager.everlastingTasks
    val completedTaskIds: StateFlow<Set<String>> = RoadmapManager.completedTaskIds
    val completedTaskTimestamps: StateFlow<Map<String, Long>> = RoadmapManager.completedTaskTimestamps

    fun getRemainingPoolCount(period: com.example.data.roadmap.TaskPeriod): Int {
        return RoadmapManager.getRemainingPoolCount(period)
    }

    val woodenKeys: StateFlow<Int> = ProfileManager.woodenKeysCount
    val bronzeKeys: StateFlow<Int> = ProfileManager.bronzeKeysCount
    val silverKeys: StateFlow<Int> = ProfileManager.silverKeysCount

    val shields24h: StateFlow<Int> = RoadmapManager.shields24h
    val shields48h: StateFlow<Int> = RoadmapManager.shields48h
    val shields72h: StateFlow<Int> = RoadmapManager.shields72h

    val girlDeductionPoints: StateFlow<Double> = FlameManager.girlDeductionPoints
    val pulseElixirUsesLeft: StateFlow<Int> = ProfileManager.pulseElixirUsesLeft


    init {
        FlameManager.init(application)
        ProfileManager.init(application)
        RoadmapManager.init(application)
        XpCalculationEngine.init(application)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.normalizeBatchStrikeEventsToLiteral()
                val latestStrikeDate = repository.getLatestStrikeDate()
                FlameManager.evaluateDailyInactivity(application, latestStrikeDate)
                val trackingStartDate = FlameManager.getStreakStartDate(application)
            if (!trackingStartDate.isNullOrBlank()) {
                val cfg = FlameManager.getSavedRecalculationConfig(application)
                val endDate = FlameManager.getStreakEndDate(application)
                XpCalculationEngine.syncRetroactiveTelemetry(
                    context = application,
                    trackingStartDate = trackingStartDate,
                    flameLogs = FlameManager.flameLogs.value,
                    strikeEvents = repository.getAllStrikeEventsList(),
                    individuals = repository.getAllIndividualsList(),
                    recalculateExact = false,
                    trackingEndDate = endDate,
                    strikesStartDate = cfg.strikesRange?.takeIf { it.isCustom }?.startDate ?: trackingStartDate,
                    strikesEndDate = cfg.strikesRange?.takeIf { it.isCustom }?.endDate ?: endDate,
                    girlStartDate = cfg.girlRange?.takeIf { it.isCustom }?.startDate ?: trackingStartDate,
                    girlEndDate = cfg.girlRange?.takeIf { it.isCustom }?.endDate ?: endDate,
                    deflameStartDate = cfg.deflameRange?.takeIf { it.isCustom }?.startDate ?: trackingStartDate,
                    deflameEndDate = cfg.deflameRange?.takeIf { it.isCustom }?.endDate ?: endDate
                )
            }
            }
        }
    }

    /**
     * "Deflame" Button: Decreases the total Flame score by [points] (default 3.0) plus any active Pulse Elixir (+5.0).
     */
    fun deflame(points: Double = 3.0) {
        val pulseBonus = if (ProfileManager.pulseElixirUsesLeft.value > 0) 5.0 else 0.0
        val effectivePoints = (points + pulseBonus).coerceAtLeast(0.1)
        val newScore = FlameManager.deflame(getApplication(), points)
        viewModelScope.launch {
            val formatted = if (effectivePoints % 1.0 == 0.0) {
                "${effectivePoints.toInt()}"
            } else {
                String.format(Locale.US, "%.1f", effectivePoints)
            }
            val msg = when (currentLanguage.value) {
                "ar" -> "تم خفض الشعلة (-$formatted): ${String.format(Locale.US, "%.1f", newScore)}"
                "fr" -> "Déflamme (-$formatted pts) : ${String.format(Locale.US, "%.1f", newScore)}"
                else -> "Deflame (-$formatted pts): ${String.format(Locale.US, "%.1f", newScore)}"
            }
            emitMessage(msg)
        }
    }

    /**
     * Saves the deduction value used on subsequent taps of the "Girl" button without immediately deducting points.
     */
    fun setGirlDeductionPoints(points: Double) {
        val saved = FlameManager.setGirlDeductionPoints(getApplication(), points)
        viewModelScope.launch {
            val formatted = if (saved % 1.0 == 0.0) "${saved.toInt()}" else String.format(Locale.US, "%.1f", saved)
            val msg = when (currentLanguage.value) {
                "ar" -> "تم تحديث خصم Girl إلى (-$formatted) للضغطة القادمة"
                "fr" -> "Déduction Girl mise à jour (-$formatted pts) pour le prochain appui"
                else -> "Girl deduction updated (-$formatted pts) for next tap"
            }
            emitMessage(msg)
        }
    }

    /**
     * "Girl" Button: Decreases the total Flame score by [points] (defaults to saved girlDeductionPoints).
     */
    fun girl(points: Double? = null) {
        val resolvedPoints = (points ?: FlameManager.girlDeductionPoints.value).coerceAtLeast(0.1)
        val newScore = FlameManager.girl(getApplication(), resolvedPoints)
        viewModelScope.launch {
            val formatted = if (resolvedPoints % 1.0 == 0.0) {
                "${resolvedPoints.toInt()}"
            } else {
                String.format(Locale.US, "%.1f", resolvedPoints)
            }
            val msg = when (currentLanguage.value) {
                "ar" -> "تبريد Girl (-$formatted): ${String.format(Locale.US, "%.1f", newScore)}"
                "fr" -> "Action Girl (-$formatted pts) : ${String.format(Locale.US, "%.1f", newScore)}"
                else -> "Girl cooldown (-$formatted pts): ${String.format(Locale.US, "%.1f", newScore)}"
            }
            emitMessage(msg)
        }
    }

    fun setFlameScore(score: Double) {
        val label = if (currentLanguage.value == "ar") "محاكي النقاط التفاعلي" else "Interactive Score Simulator"
        FlameManager.setScore(getApplication(), score, "SIMULATION", label)
    }

    fun clearFlameLogs() {
        FlameManager.clearLogs(getApplication())
    }

    fun getStreakStartDate(): String? {
        return FlameManager.getStreakStartDate(getApplication())
    }

    fun getStreakEndDate(): String? {
        return FlameManager.getStreakEndDate(getApplication())
    }

    fun getSavedGirlPerMonth(): Int {
        return FlameManager.getSavedGirlPerMonth(getApplication())
    }

    fun getSavedDeflamePerMonth(): Int {
        return FlameManager.getSavedDeflamePerMonth(getApplication())
    }

    fun getSavedRecalculationConfig(): com.example.data.flame.StreakRecalculationConfig {
        return FlameManager.getSavedRecalculationConfig(getApplication())
    }

    fun previewStartDateRecalculation(
        startDateStr: String,
        girlPerMonth: Int = 0,
        deflamePerMonth: Int = 0,
        endDateStr: String? = null,
        inactivityRange: com.example.data.flame.DateRangeConfig? = null,
        strikesRange: com.example.data.flame.DateRangeConfig? = null,
        girlRange: com.example.data.flame.DateRangeConfig? = null,
        deflameRange: com.example.data.flame.DateRangeConfig? = null
    ): com.example.data.flame.FlameRecalculationResult {
        return FlameManager.previewRecalculation(
            startDateStr = startDateStr,
            strikeEvents = allStrikeEvents.value,
            girlPerMonth = girlPerMonth,
            deflamePerMonth = deflamePerMonth,
            endDateStr = endDateStr,
            inactivityRange = inactivityRange,
            strikesRange = strikesRange,
            girlRange = girlRange,
            deflameRange = deflameRange
        )
    }

    fun recalculateFlameScoreFromStartDate(
        startDateStr: String,
        girlPerMonth: Int = 0,
        deflamePerMonth: Int = 0,
        endDateStr: String? = null,
        inactivityRange: com.example.data.flame.DateRangeConfig? = null,
        strikesRange: com.example.data.flame.DateRangeConfig? = null,
        girlRange: com.example.data.flame.DateRangeConfig? = null,
        deflameRange: com.example.data.flame.DateRangeConfig? = null
    ): com.example.data.flame.FlameRecalculationResult {
        val result = FlameManager.recalculateFromStartDate(
            context = getApplication(),
            startDateStr = startDateStr,
            strikeEvents = allStrikeEvents.value,
            lang = currentLanguage.value,
            girlPerMonth = girlPerMonth,
            deflamePerMonth = deflamePerMonth,
            endDateStr = endDateStr,
            inactivityRange = inactivityRange,
            strikesRange = strikesRange,
            girlRange = girlRange,
            deflameRange = deflameRange
        )
        viewModelScope.launch {
            XpCalculationEngine.syncRetroactiveTelemetry(
                context = getApplication(),
                trackingStartDate = result.startDate,
                flameLogs = FlameManager.flameLogs.value,
                strikeEvents = repository.getAllStrikeEventsList(),
                individuals = repository.getAllIndividualsList(),
                recalculateExact = true,
                trackingEndDate = result.endDate,
                strikesStartDate = result.strikesStartDate,
                strikesEndDate = result.strikesEndDate,
                girlStartDate = result.girlStartDate,
                girlEndDate = result.girlEndDate,
                deflameStartDate = result.deflameStartDate,
                deflameEndDate = result.deflameEndDate
            )
            val formattedScore = if (result.resultingScore % 1.0 == 0.0) "${result.resultingScore.toInt()}" else String.format(Locale.US, "%.1f", result.resultingScore)
            val msg = when (currentLanguage.value) {
                "ar" -> buildString {
                    append("تمت إعادة حساب نقاط الشعلة بنجاح: $formattedScore نقطة (+${result.inactivityPointsAdded.toInt()} خمول، -${String.format(Locale.US, "%.1f", result.totalDeductions)} سترايك")
                    if (result.girlPressesCount > 0) append("، -${result.girlPointsDeducted.toInt()} Girl [×${result.girlPressesCount}]")
                    if (result.deflamePressesCount > 0) append("، -${result.deflamePointsDeducted.toInt()} Deflame [×${result.deflamePressesCount}]")
                    append(")")
                }
                "fr" -> "Score recalculé avec succès : $formattedScore pts"
                else -> buildString {
                    append("Flame score recalculated: $formattedScore pts (+${result.inactivityPointsAdded.toInt()} inactivity, -${String.format(Locale.US, "%.1f", result.totalDeductions)} strikes")
                    if (result.girlPressesCount > 0) append(", -${result.girlPointsDeducted.toInt()} Girl [×${result.girlPressesCount}]")
                    if (result.deflamePressesCount > 0) append(", -${result.deflamePointsDeducted.toInt()} Deflame [×${result.deflamePressesCount}]")
                    append(")")
                }
            }
            emitMessage(msg)
        }
        return result
    }

    fun activateManualShield() {
        val success = FlameManager.activateManualShield(getApplication(), days = 3)
        viewModelScope.launch {
            if (success) {
                val msg = when (currentLanguage.value) {
                    "ar" -> "تم تفعيل درع السلسلة بنجاح (3 أيام حماية)"
                    "fr" -> "Bouclier de série activé avec succès (3 jours)"
                    else -> "Streak Shield activated successfully (3 days protection)"
                }
                emitMessage(msg)
            }
        }
    }

    fun activate1DayShield() {
        val success = FlameManager.activate1DayShield(getApplication())
        viewModelScope.launch {
            if (success) {
                val msg = when (currentLanguage.value) {
                    "ar" -> "تم تفعيل درع 24 ساعة بنجاح"
                    "fr" -> "Bouclier 24h activé avec succès"
                    else -> "24h Shield activated successfully"
                }
                emitMessage(msg)
            }
        }
    }

    fun activate48hShield() {
        val success = FlameManager.activate48hShield(getApplication())
        viewModelScope.launch {
            if (success) {
                val msg = when (currentLanguage.value) {
                    "ar" -> "تم تفعيل درع 48 ساعة بنجاح"
                    "fr" -> "Bouclier 48h activé avec succès"
                    else -> "48h Shield activated successfully"
                }
                emitMessage(msg)
            }
        }
    }

    fun hardResetAllTaskCompletionStates() {
        RoadmapManager.hardResetAllTaskCompletionStates(getApplication())
        viewModelScope.launch {
            val msg = when (currentLanguage.value) {
                "ar" -> "تم تصفير وإعادة ضبط كافة حالات إنجاز المهام بنجاح! ⚡"
                "fr" -> "Toutes les missions ont été réinitialisées avec succès ! ⚡"
                else -> "All task completion states have been hard-reset! ⚡"
            }
            emitMessage(msg)
        }
    }

    fun grant1DayShield(count: Int = 1) {
        val newCount = FlameManager.grant1DayShield(getApplication(), count)
        viewModelScope.launch {
            val msg = when (currentLanguage.value) {
                "ar" -> "تمت إضافة درع 1 يوم إلى مخزونك (المتوفر: $newCount)"
                "fr" -> "Bouclier 1 jour ajouté à votre inventaire (Dispo : $newCount)"
                else -> "1-Day Shield added to your inventory (Available: $newCount)"
            }
            emitMessage(msg)
        }
    }

    fun setStreakShieldActive(active: Boolean) {
        FlameManager.setStreakShieldActive(getApplication(), active)
        viewModelScope.launch {
            val msg = when (currentLanguage.value) {
                "ar" -> if (active) "تم تفعيل درع السلسلة (إيقاف الخمول مؤقتاً)" else "تم إلغاء درع السلسلة (استئناف الخمول)"
                "fr" -> if (active) "Bouclier de série activé (pause inactivité)" else "Bouclier de série désactivé"
                else -> if (active) "Streak Shield Activated (Inactivity paused)" else "Streak Shield Deactivated"
            }
            emitMessage(msg)
        }
    }

    fun refreshTaskCycles() {
        RoadmapManager.refreshCyclesIfNeeded(getApplication())
    }

    // 500-Station Wavy Roadmap & Task Actions
    fun claimStation(stationId: Int) {
        val reward = RoadmapManager.claimStationReward(getApplication(), stationId)
        if (reward != null) {
            val lang = currentLanguage.value
            val localizedTitle = reward.getLocalizedTitle(lang)
            viewModelScope.launch {
                val msg = when (lang) {
                    "ar" -> "تم جني مكافأة المحطة $stationId: $localizedTitle"
                    "fr" -> "Récompense de la station $stationId réclamée : $localizedTitle"
                    else -> "Station $stationId Reward Claimed: $localizedTitle"
                }
                emitMessage(msg)
            }
        }
    }

    fun claimTaskReward(taskId: String) {
        val success = RoadmapManager.claimTaskReward(getApplication(), taskId)
        if (success) {
            viewModelScope.launch {
                val msg = when (currentLanguage.value) {
                    "ar" -> "تم جمع مكافأة المهمة وإضافة نقاط الـ XP بنجاح! ⚡"
                    "fr" -> "Récompense collectée et XP ajouté avec succès ! ⚡"
                    else -> "Task reward collected and XP awarded! ⚡"
                }
                emitMessage(msg)
            }
        }
    }

    fun completeTask(taskId: String) {
        val success = RoadmapManager.completeTask(getApplication(), taskId)
        if (success) {
            viewModelScope.launch {
                val msg = when (currentLanguage.value) {
                    "ar" -> "تم إنجاز المهمة وحصد نقاط الـ XP بنجاح! ⚡"
                    "fr" -> "Mission accomplie et XP récolté avec succès ! ⚡"
                    else -> "Task Completed and XP awarded successfully! ⚡"
                }
                emitMessage(msg)
            }
        }
    }

    fun toggleTaskCompletionLedger(taskId: String) {
        val newCompletedState = RoadmapManager.toggleTaskCompletionPreservingXp(getApplication(), taskId)
        if (newCompletedState != null) {
            viewModelScope.launch {
                val msg = when (currentLanguage.value) {
                    "ar" -> if (newCompletedState) {
                        "تم تعيين المهمة كمنجزة مع الحفاظ الكامل على رصيد XP المكتسب ✓"
                    } else {
                        "تمت إعادة المهمة إلى غير منجزة مع بقاء كامل XP المكتسب محفوظاً ↺"
                    }
                    "fr" -> if (newCompletedState) {
                        "Mission marquée terminée (XP acquis conservé intact) ✓"
                    } else {
                        "Mission remise en attente (XP acquis conservé intact) ↺"
                    }
                    else -> if (newCompletedState) {
                        "Task marked completed (all earned XP kept intact) ✓"
                    } else {
                        "Task marked uncompleted (all earned XP kept intact) ↺"
                    }
                }
                emitMessage(msg)
            }
        }
    }

    fun useSkipKeyForTask(taskId: String) {
        val allTasks = RoadmapManager.dailyTasks.value + RoadmapManager.weeklyTasks.value + RoadmapManager.monthlyTasks.value + RoadmapManager.yearlyTasks.value
        val task = allTasks.find { it.id == taskId }
        if (task != null && !task.canBeSkipped) {
            viewModelScope.launch {
                val msg = when (currentLanguage.value) {
                    "ar" -> "مهام الأبدية لا تقبل التجاوز بالمفاتيح! تتطلب إنجازاً فعلياً."
                    "fr" -> "Les missions éternelles ne peuvent pas être passées avec des clés !"
                    else -> "Eternal tasks cannot be skipped using keys! Real completion is required."
                }
                emitMessage(msg)
            }
            return
        }
        if (task != null && task.isLocked()) {
            viewModelScope.launch {
                val msg = when (currentLanguage.value) {
                    "ar" -> "هذه المهمة قيد الانتظار حتى موعد منح المهام الجديد"
                    "fr" -> "Cette mission attend le prochain cycle d'attribution"
                    else -> "This task is waiting for its next cycle unlock window"
                }
                emitMessage(msg)
            }
            return
        }

        val success = RoadmapManager.useSkipKeyForTask(getApplication(), taskId)
        viewModelScope.launch {
            if (success) {
                val msg = when (currentLanguage.value) {
                    "ar" -> "تم استخدام مفتاح التجاوز وحصد نقاط الـ XP فورياً! 🔑"
                    "fr" -> "Clé de passe utilisée et XP récolté immédiatement ! 🔑"
                    else -> "Skip Key used and XP awarded instantly! 🔑"
                }
                emitMessage(msg)
            } else {
                val msg = when (currentLanguage.value) {
                    "ar" -> "لا تملك مفتاح تجاوز مناسباً لهذه المهمة"
                    "fr" -> "Aucune clé de passe correspondante disponible"
                    else -> "No suitable skip key available for this task"
                }
                emitMessage(msg)
            }
        }
    }

    fun addXp(amount: Int) {
        RoadmapManager.addXp(getApplication(), amount)
    }

    // Security & Fingerprint Authentication Operations
    fun unlockWithFingerprint() {
        _isAppUnlocked.value = true
    }

    fun setFingerprintLockEnabled(enabled: Boolean) {
        securityPrefs.isFingerprintLockEnabled = enabled
        isFingerprintLockEnabled.value = enabled
        _isAppUnlocked.value = true
        viewModelScope.launch {
            val msg = if (enabled) {
                when (currentLanguage.value) {
                    "ar" -> "تم تفعيل قفل البصمة بنجاح"
                    "fr" -> "Verrouillage par empreinte activé avec succès"
                    else -> "Fingerprint authentication enabled"
                }
            } else {
                when (currentLanguage.value) {
                    "ar" -> "تم إلغاء قفل البصمة"
                    "fr" -> "Verrouillage par empreinte désactivé"
                    else -> "Fingerprint authentication disabled"
                }
            }
            emitMessage(msg)
        }
    }

    fun notifyMessage(msg: String) {
        viewModelScope.launch {
            emitMessage(msg)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        isHapticFeedbackEnabled.value = enabled
        securityPrefs.isHapticFeedbackEnabled = enabled
        val msg = if (enabled) {
            when (currentLanguage.value) {
                "ar" -> "تم تفعيل الاهتزاز اللمسي"
                "fr" -> "Retour haptique activé"
                else -> "Haptic feedback enabled"
            }
        } else {
            when (currentLanguage.value) {
                "ar" -> "تم تعطيل الاهتزاز اللمسي"
                "fr" -> "Retour haptique désactivé"
                else -> "Haptic feedback disabled"
            }
        }
        viewModelScope.launch {
            emitMessage(msg)
        }
    }

    // In-App Auto-Update Operations (GitHub Releases: charfeddine23/Strike)
    fun setAutoUpdateEnabled(enabled: Boolean) {
        isAutoUpdateEnabled.value = enabled
        securityPrefs.isAutoUpdateEnabled = enabled
        viewModelScope.launch {
            val msg = if (enabled) {
                when (currentLanguage.value) {
                    "ar" -> "تم تفعيل التحديث التلقائي من داخل التطبيق"
                    "fr" -> "Mise à jour automatique activée"
                    else -> "Automatic in-app updates enabled"
                }
            } else {
                when (currentLanguage.value) {
                    "ar" -> "تم تعطيل التحقق التلقائي من التحديثات"
                    "fr" -> "Mise à jour automatique désactivée"
                    else -> "Automatic update checks disabled"
                }
            }
            emitMessage(msg)
        }
    }

    fun checkForAppUpdate(isAutoCheck: Boolean = false) {
        if (isAutoCheck && !isAutoUpdateEnabled.value) return
        viewModelScope.launch {
            AppUpdateManager.checkForUpdates(
                currentVersion = BuildConfig.VERSION_NAME,
                isAutoCheck = isAutoCheck
            )
        }
    }

    fun downloadAppUpdate(release: GitHubReleaseInfo) {
        viewModelScope.launch {
            AppUpdateManager.downloadUpdateInApp(getApplication(), release)
        }
    }

    fun cancelAppUpdateDownload() {
        AppUpdateManager.cancelDownload()
    }

    fun installAppUpdate(apkFile: File) {
        val launched = AppUpdateManager.installDownloadedApk(getApplication(), apkFile)
        if (!launched) {
            viewModelScope.launch {
                val msg = when (currentLanguage.value) {
                    "ar" -> "يرجى تفعيل إذن تثبيت التطبيقات ثم الضغط على «تثبيت التحديث الآن»"
                    "fr" -> "Veuillez autoriser l'installation d'applications puis réessayer"
                    else -> "Please allow installing apps from this source, then tap Install again"
                }
                emitMessage(msg)
            }
        }
    }

    fun dismissUpdatePrompt() {
        AppUpdateManager.dismissPrompt()
    }

    fun notifyContactImported(name: String, phone: String = "") {
        viewModelScope.launch {
            val msg = if (phone.isNotBlank()) {
                when (currentLanguage.value) {
                    "ar" -> "تم ربط جهة الاتصال: $name ($phone)"
                    "fr" -> "Contact lié : $name ($phone)"
                    else -> "Contact linked: $name ($phone)"
                }
            } else {
                when (currentLanguage.value) {
                    "ar" -> "تم استيراد جهة الاتصال: $name"
                    "fr" -> "Contact importé : $name"
                    else -> "Contact imported: $name"
                }
            }
            emitMessage(msg)
        }
    }

    fun lockApp() {
        if (isFingerprintLockEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setStrikeSearchQuery(query: String) {
        _strikeSearchQuery.value = query
    }

    fun clearStrikeSearch() {
        _strikeSearchQuery.value = ""
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _filterType.value = "ALL"
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun selectStrikeIndividual(id: Long) {
        _selectedStrikeId.value = id
    }

    fun selectPersonA(id: Long) {
        _personAId.value = id
    }

    fun selectPersonB(id: Long) {
        _personBId.value = id
    }

    fun swapComparisonPersons() {
        val a = _personAId.value
        val b = _personBId.value
        _personAId.value = b
        _personBId.value = a
    }

    /**
     * Increment strike count - AUTOMATICALLY stores to database with current date, time, year!
     */
    fun triggerStrike(delta: Int = 1) {
        viewModelScope.launch {
            val target = currentStrikeIndividual.value ?: return@launch
            val latestBefore = repository.getLatestStrikeDate()
            val eventId = repository.incrementStrike(target.id, delta)
            _strikeAnimCount.value += 1

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val strikesToday = repository.getEventsForDate(today)

            val result = FlameManager.onStrikeRegistered(
                context = getApplication(),
                strikeDate = today,
                boost = target.boost,
                personName = target.fullName,
                latestDateBeforeInsert = latestBefore,
                strikesToday = strikesToday,
                strikeCount = delta
            )

            if (result.penaltyDeducted > 0.0) {
                repository.updateEventNotes(eventId, "PENALTY:${result.penaltyDeducted}|RULE:${result.ruleApplied}")
            }

            val msg = when (result.ruleApplied) {
                "DOUBLE_STRIKE" -> {
                    when (currentLanguage.value) {
                        "ar" -> "عقوبة الضربة المزدوجة! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} نقطة) وفتح درع 3 أيام"
                        "fr" -> "Pénalité Double Strike ! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} pts) & Bouclier 3j débloqué"
                        else -> "Double Strike Penalty! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} pts) & 3-Day Shield unlocked"
                    }
                }
                "MULTI_STRIKE_COMBO" -> {
                    when (currentLanguage.value) {
                        "ar" -> "كومبو ضربات متعددة! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} نقطة) وتفعيل درع 7 أيام تلقائياً"
                        "fr" -> "Combo Multi-Strikes ! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} pts) & Bouclier 7j activé"
                        else -> "Multi-Strike Combo! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} pts) & 7-Day Shield auto-activated"
                    }
                }
                "EXTREME_OVERLOAD" -> {
                    when (currentLanguage.value) {
                        "ar" -> "تحميل زائد أقصى! تم تصفير نقاط الشعلة إلى 0 وتجميد النار"
                        "fr" -> "Surcharge Extrême ! Score réinitialisé à 0 et flamme gelée"
                        else -> "Extreme System Overload! Flame score reset to 0 & flame frozen"
                    }
                }
                else -> {
                    when (currentLanguage.value) {
                        "ar" -> "تم تسجيل ضربة +$delta (${target.fullName})"
                        "fr" -> "Strike enregistré +$delta (${target.fullName})"
                        else -> "Strike registered +$delta (${target.fullName})"
                    }
                }
            }
            emitMessage(msg)
        }
    }

    fun incrementStrikeForId(id: Long, delta: Int = 1) {
        viewModelScope.launch {
            val target = repository.getIndividualById(id)
            val latestBefore = repository.getLatestStrikeDate()
            val eventId = repository.incrementStrike(id, delta)
            _strikeAnimCount.value += 1

            if (target != null) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val strikesToday = repository.getEventsForDate(today)

                val result = FlameManager.onStrikeRegistered(
                    context = getApplication(),
                    strikeDate = today,
                    boost = target.boost,
                    personName = target.fullName,
                    latestDateBeforeInsert = latestBefore,
                    strikesToday = strikesToday,
                    strikeCount = delta
                )

                if (result.penaltyDeducted > 0.0) {
                    repository.updateEventNotes(eventId, "PENALTY:${result.penaltyDeducted}|RULE:${result.ruleApplied}")
                }

                val msg = when (result.ruleApplied) {
                    "DOUBLE_STRIKE" -> {
                        when (currentLanguage.value) {
                            "ar" -> "عقوبة الضربة المزدوجة! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} نقطة) وفتح درع 3 أيام"
                            "fr" -> "Pénalité Double Strike ! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} pts) & Bouclier 3j débloqué"
                            else -> "Double Strike Penalty! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} pts) & 3-Day Shield unlocked"
                        }
                    }
                    "MULTI_STRIKE_COMBO" -> {
                        when (currentLanguage.value) {
                            "ar" -> "كومبو ضربات متعددة! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} نقطة) وتفعيل درع 7 أيام تلقائياً"
                            "fr" -> "Combo Multi-Strikes ! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} pts) & Bouclier 7j activé"
                            else -> "Multi-Strike Combo! (-${String.format(Locale.US, "%.1f", result.penaltyDeducted)} pts) & 7-Day Shield auto-activated"
                        }
                    }
                    "EXTREME_OVERLOAD" -> {
                        when (currentLanguage.value) {
                            "ar" -> "تحميل زائد أقصى! تم تصفير نقاط الشعلة إلى 0 وتجميد النار"
                            "fr" -> "Surcharge Extrême ! Score réinitialisé à 0 et flamme gelée"
                            else -> "Extreme System Overload! Flame score reset to 0 & flame frozen"
                        }
                    }
                    else -> when (currentLanguage.value) {
                        "ar" -> "تم تسجيل ضربة +$delta (${target.fullName})"
                        "fr" -> "Strike enregistré +$delta (${target.fullName})"
                        else -> "Strike registered +$delta (${target.fullName})"
                    }
                }
                emitMessage(msg)
            }
        }
    }

    fun resetStrikes(id: Long) {
        viewModelScope.launch {
            repository.setStrikeCount(id, 0)
            val msg = when (currentLanguage.value) {
                "ar" -> "تم تصفير عداد الضربات"
                "fr" -> "Compteur de strikes réinitialisé"
                else -> "Strikes reset"
            }
            emitMessage(msg)
        }
    }

    fun addHistoricalYearEntry(year: Int, count: Int) {
        if (count <= 0) return
        val current = formHistoricalEntries.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.first == year }
        if (existingIndex >= 0) {
            val updated = current[existingIndex].second + count
            current[existingIndex] = Pair(year, updated)
        } else {
            current.add(Pair(year, count))
        }
        formHistoricalEntries.value = current.sortedByDescending { it.first }
    }

    fun removeHistoricalYearEntry(index: Int) {
        val current = formHistoricalEntries.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            formHistoricalEntries.value = current
        }
    }

    fun openDataEntryForNew() {
        initFormForNew()
        _isDataEntryOpen.value = true
    }

    fun openDataEntryForEdit(individual: Individual) {
        initFormForEdit(individual)
        _isDataEntryOpen.value = true
    }

    fun closeDataEntry() {
        _isDataEntryOpen.value = false
    }

    fun initFormForNew() {
        editingId = null
        formName.value = ""
        formPhoneNumber.value = ""
        formType.value = "M"
        formLength.value = "25.0"
        formWidth.value = "12.0"
        formSScore.value = "85.0"
        formFScore.value = "80.0"
        formCScore.value = "90.0"
        formError.value = null
        formHistoricalEntries.value = emptyList()
    }

    fun initFormForEdit(individual: Individual) {
        editingId = individual.id
        formName.value = individual.fullName
        formPhoneNumber.value = individual.phoneNumber
        formType.value = individual.type
        formLength.value = individual.length.toString()
        formWidth.value = individual.width.toString()
        formSScore.value = individual.sScore.toString()
        formFScore.value = individual.fScore.toString()
        formCScore.value = individual.cScore.toString()
        formError.value = null
        formHistoricalEntries.value = emptyList()
    }

    fun saveCurrentForm(onSuccess: () -> Unit) {
        if (_isSavingForm.value) return

        val lang = currentLanguage.value
        val name = formName.value.trim()
        val phone = formPhoneNumber.value.trim()
        if (name.isEmpty()) {
            formError.value = when (lang) {
                "ar" -> "الاسم واللقب حقل إلزامي."
                "fr" -> "Le nom complet est obligatoire."
                else -> "Full name is required."
            }
            return
        }

        val length = parseLocalizedDouble(formLength.value)
        val width = parseLocalizedDouble(formWidth.value)
        val s = parseLocalizedDouble(formSScore.value)
        val f = parseLocalizedDouble(formFScore.value)
        val c = parseLocalizedDouble(formCScore.value)

        if (length == null || length <= 0 || !length.isFinite() || width == null || width <= 0 || !width.isFinite()) {
            formError.value = when (lang) {
                "ar" -> "يجب إدخال أبعاد موجبة صحيحة."
                "fr" -> "Veuillez entrer des dimensions valides et positives."
                else -> "Please enter valid positive dimensions."
            }
            return
        }
        if (s == null || !s.isFinite() || f == null || !f.isFinite() || c == null || !c.isFinite()) {
            formError.value = when (lang) {
                "ar" -> "يرجى إدخال قيم صحيحة للدرجات (S, F, C)."
                "fr" -> "Veuillez entrer des scores valides (S, F, C)."
                else -> "Please enter valid score values (S, F, C)."
            }
            return
        }

        formError.value = null
        _isSavingForm.value = true

        val currentEditingId = editingId
        val isNewRecord = currentEditingId == null || currentEditingId == 0L
        val historicalSnapshot = formHistoricalEntries.value.toList()
        val historicalStrikesTotal = historicalSnapshot.sumOf { it.second.toLong().coerceAtLeast(0L) }
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val safeType = formType.value.takeIf { it in Individual.VALID_TYPES } ?: "M"
        val calculatedSize = Individual.calculateSize(length, width)
        val calculatedBoost = Individual.calculateBoost(s, f, c, calculatedSize)

        val existingStrikeCount = if (!isNewRecord) {
            allIndividuals.value.find { it.id == currentEditingId }?.strikeCount ?: 0
        } else {
            0
        }
        val effectiveStrikeCount = if (!isNewRecord) {
            (existingStrikeCount.toLong() + historicalStrikesTotal.toLong())
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else {
            historicalStrikesTotal
        }

        val individualToSave = Individual(
            id = currentEditingId ?: 0L,
            fullName = name,
            phoneNumber = phone,
            type = safeType,
            length = length,
            width = width,
            size = calculatedSize,
            sScore = s,
            fScore = f,
            cScore = c,
            boost = calculatedBoost,
            strikeCount = effectiveStrikeCount
        )

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val savedId = repository.save(individualToSave)
                    val effectiveId = if (!isNewRecord) currentEditingId!! else savedId

                    if (historicalSnapshot.isNotEmpty()) {
                        repository.recordHistoricalStrikes(effectiveId, historicalSnapshot, lang)
                    }

                    val appCtx = getApplication<Application>()
                    if (isNewRecord) {
                        RoadmapManager.onActionOccurred(appCtx, "PERSON")
                        XpCalculationEngine.onNewPersonAdded(
                            context = appCtx,
                            size = individualToSave.size,
                            personName = individualToSave.fullName
                        )
                    }

                    if (historicalSnapshot.isNotEmpty()) {
                        val startDate = FlameManager.getStreakStartDate(appCtx)
                        if (!startDate.isNullOrBlank()) {
                            XpCalculationEngine.syncRetroactiveTelemetry(
                                context = appCtx,
                                trackingStartDate = startDate,
                                flameLogs = FlameManager.flameLogs.value,
                                strikeEvents = repository.getAllStrikeEventsList(),
                                individuals = repository.getAllIndividualsList(),
                                recalculateExact = false
                            )
                        }
                    }
                }

                val savedMsg = when (lang) {
                    "ar" -> "تم حفظ السجل بنجاح"
                    "fr" -> "Enregistrement sauvegardé avec succès"
                    else -> "Record saved successfully"
                }
                emitMessage(savedMsg)
                _isDataEntryOpen.value = false
                onSuccess()
            } catch (e: Exception) {
                formError.value = when (lang) {
                    "ar" -> "تعذر حفظ السجل: ${e.localizedMessage ?: "خطأ غير متوقع"}"
                    "fr" -> "Impossible d'enregistrer : ${e.localizedMessage ?: "Erreur inattendue"}"
                    else -> "Could not save record: ${e.localizedMessage ?: "Unexpected error"}"
                }
            } finally {
                _isSavingForm.value = false
            }
        }
    }

    fun deleteIndividual(individual: Individual) {
        viewModelScope.launch {
            repository.delete(individual)
            if (_selectedStrikeId.value == individual.id) {
                _selectedStrikeId.value = null
            }
            if (_personAId.value == individual.id) {
                _personAId.value = null
            }
            if (_personBId.value == individual.id) {
                _personBId.value = null
            }
            val msg = when (currentLanguage.value) {
                "ar" -> "تم حذف السجل وكافة الضربات المرتبطة به (${individual.fullName})"
                "fr" -> "Enregistrement et tous les strikes associés supprimés (${individual.fullName})"
                else -> "Record and all associated strikes deleted (${individual.fullName})"
            }
            emitMessage(msg)
        }
    }

    /**
     * When a strike is deleted, it automatically retrieves its penalty value,
     * adds it back to the current Flame Score (Flame Score + penaltyValue),
     * and updates both the database and the UI instantly.
     */
    fun deleteStrike(event: StrikeEvent) = deleteStrikeEvent(event)

    fun deleteStrikeEvent(event: StrikeEvent) {
        viewModelScope.launch {
            val effective = event.getEffectivePenalty()
            val penaltyValue = if (effective > 0.0) {
                effective
            } else if (event.performanceValue > 0.0) {
                event.performanceValue
            } else {
                repository.getIndividualById(event.individualId)?.boost ?: 0.0
            }

            // 1. Update database
            repository.deleteStrikeEvent(event)

            // 2. Automatically add penalty value back to the Flame Score (Flame Score + penaltyValue) & update UI
            if (penaltyValue > 0.0) {
                FlameManager.onStrikeDeleted(
                    context = getApplication(),
                    penaltyValue = penaltyValue,
                    personName = event.individualName,
                    strikeDate = event.date
                )
            }

            val msg = when (currentLanguage.value) {
                "ar" -> "تم حذف الضربة واستعادة نقاط الشعلة (+${String.format(Locale.US, "%.1f", penaltyValue)})"
                "fr" -> "Strike supprimé et points de flamme restitués (+${String.format(Locale.US, "%.1f", penaltyValue)})"
                else -> "Strike deleted & flame points restored (+${String.format(Locale.US, "%.1f", penaltyValue)})"
            }
            val undoLabel = when (currentLanguage.value) {
                "ar" -> "تراجع"
                "fr" -> "Annuler"
                else -> "Undo"
            }
            _eventFlow.emit(msg)
            _uiEvents.emit(
                UiEvent.ShowUndoSnackbar(
                    message = msg,
                    actionLabel = undoLabel,
                    strikeEvent = event
                )
            )
        }
    }

    fun undoDeleteStrikeEvent(event: StrikeEvent) {
        viewModelScope.launch {
            val effective = event.getEffectivePenalty()
            val penaltyValue = if (effective > 0.0) {
                effective
            } else if (event.performanceValue > 0.0) {
                event.performanceValue
            } else {
                repository.getIndividualById(event.individualId)?.boost ?: 0.0
            }

            // 1. Restore to database
            repository.restoreStrikeEvent(event)

            // 2. Re-apply penalty deduction to Flame Score
            if (penaltyValue > 0.0) {
                FlameManager.onStrikeRestoredAfterUndo(
                    context = getApplication(),
                    penaltyValue = penaltyValue,
                    personName = event.individualName
                )
            }

            val msg = when (currentLanguage.value) {
                "ar" -> "تمت استعادة الضربة بنجاح (+${event.count} - ${event.individualName})"
                "fr" -> "Strike restauré avec succès (+${event.count} - ${event.individualName})"
                else -> "Strike restored successfully (+${event.count} - ${event.individualName})"
            }
            emitMessage(msg)
        }
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        securityPrefs.selectedLanguage = lang
        viewModelScope.launch {
            val msg = when (lang) {
                "ar" -> "تم تغيير اللغة إلى العربية"
                "fr" -> "Langue changée en Français"
                else -> "Language changed to English"
            }
            emitMessage(msg)
        }
    }

    /**
     * Literally resets all game levels, consumables, unlocked titles, and unlocked avatars
     * back to their initial base state WITHOUT touching individuals or strike records.
     */
    fun resetAllGameLevelsToZero() {
        val app = getApplication<android.app.Application>()
        viewModelScope.launch {
            RoadmapManager.resetAllLevelsToZero(app)
            FlameManager.resetAllLevelsToZero(app)
            ProfileManager.resetAllProgressToZero(app)
            val msg = when (currentLanguage.value) {
                "ar" -> "تم تصفير كافة المستويات والمخزون وإعادة قفل الألقاب والأفاتارات إلى حالتها الأولية (مع الحفاظ على الأفراد وسجلات السترايك)!"
                "fr" -> "Tous les niveaux, consommables, titres et avatars ont été réinitialisés à l'état initial (individus et strikes conservés) !"
                else -> "All game levels, consumables, titles & avatars reset to initial state (individuals & strike logs preserved)!"
            }
            emitMessage(msg)
        }
    }

    fun exportBackup(context: Context, destinationUri: Uri, onResult: ((BackupResult) -> Unit)? = null) {
        viewModelScope.launch {
            val lang = currentLanguage.value
            val result = backupManager.exportBackup(context, destinationUri, lang)
            emitMessage(result.message)
            onResult?.invoke(result)
        }
    }

    fun importBackup(
        context: Context,
        sourceUri: Uri,
        replaceExisting: Boolean,
        onResult: ((BackupResult) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val lang = currentLanguage.value
            val result = backupManager.importBackup(context, sourceUri, replaceExisting, lang)
            if (result.success) {
                _currentLanguage.value = securityPrefs.selectedLanguage
                isFingerprintLockEnabled.value = securityPrefs.isFingerprintLockEnabled
                isHapticFeedbackEnabled.value = securityPrefs.isHapticFeedbackEnabled
                isAutoUpdateEnabled.value = securityPrefs.isAutoUpdateEnabled
                _isAppUnlocked.value = true
            }
            emitMessage(result.message)
            onResult?.invoke(result)
        }
    }

    companion object {
        fun normalizeNumericInput(raw: String): String {
            if (raw.isEmpty()) return ""
            val sb = StringBuilder(raw.length)
            for (ch in raw.trim()) {
                when (ch) {
                    in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                    in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                    '٫', '،', ',' -> sb.append('.')
                    '−', '–' -> sb.append('-')
                    else -> if (!ch.isWhitespace()) sb.append(ch)
                }
            }
            return sb.toString()
        }

        fun parseLocalizedDouble(raw: String): Double? {
            return normalizeNumericInput(raw).toDoubleOrNull()
        }

        fun parseLocalizedInt(raw: String): Int? {
            return normalizeNumericInput(raw).toIntOrNull()
        }

        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repository = IndividualRepository(db.individualDao(), db.strikeEventDao())
                    return IndividualViewModel(application, repository) as T
                }
            }
    }
}
