package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.repository.IndividualRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    // Security & Pattern Lock
    val isPatternLockEnabled = mutableStateOf(securityPrefs.isPatternLockEnabled)
    private val _isAppUnlocked = MutableStateFlow(!securityPrefs.isPatternLockEnabled)
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

    // Yearly Strike Dashboard Analytics
    val yearlyStrikeStats: StateFlow<List<YearlyStrikeStat>> = allStrikeEvents.map { events ->
        if (events.isEmpty()) return@map emptyList()

        val allTimeTotal = events.sumOf { it.count }.coerceAtLeast(1)
        val groupedByYear = events.groupBy { it.year }

        groupedByYear.map { (year, yearEvents) ->
            val yearTotal = yearEvents.sumOf { it.count }
            val topPersonEntry = yearEvents.groupBy { it.individualName }
                .mapValues { entry -> entry.value.sumOf { it.count } }
                .maxByOrNull { it.value }

            YearlyStrikeStat(
                year = year,
                totalStrikes = yearTotal,
                eventCount = yearEvents.size,
                topIndividualName = topPersonEntry?.key ?: "—",
                topIndividualStrikes = topPersonEntry?.value ?: 0,
                percentageOfAllTime = (yearTotal.toFloat() / allTimeTotal.toFloat()).coerceIn(0f, 1f)
            )
        }.sortedByDescending { it.year }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTimeStrikesCount: StateFlow<Int> = allStrikeEvents.map { events ->
        events.sumOf { it.count }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

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
        val trimmedQuery = query.trim().lowercase()

        list.filter { item ->
            val matchesFilter = (filter == "ALL" || item.type.equals(filter, ignoreCase = true))
            val matchesSearch = trimmedQuery.isEmpty() ||
                item.fullName.lowercase().contains(trimmedQuery) ||
                item.type.lowercase().contains(trimmedQuery)
            matchesFilter && matchesSearch
        }.let { filtered ->
            when (sort) {
                SortOrder.NAME_ASC -> filtered.sortedBy { it.fullName.lowercase() }
                SortOrder.STRIKES_DESC -> filtered.sortedByDescending { it.strikeCount }
                SortOrder.BOOST_DESC -> filtered.sortedByDescending { it.boost }
                SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.size }
                SortOrder.SCORE_DESC -> filtered.sortedByDescending { it.totalScore }
            }
        }
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

    // Data Entry Form State
    var editingId: Long? = null
        private set

    val formName = MutableStateFlow("")
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
        val lengthVal = l.toDoubleOrNull() ?: 0.0
        val widthVal = w.toDoubleOrNull() ?: 0.0
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
        val sVal = s.toDoubleOrNull() ?: 0.0
        val fVal = f.toDoubleOrNull() ?: 0.0
        val cVal = c.toDoubleOrNull() ?: 0.0
        Individual.calculateBoost(sVal, fVal, cVal, sizeVal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 257.0
    )

    // Language state
    private val _currentLanguage = MutableStateFlow(securityPrefs.selectedLanguage)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // Security & Pattern Lock Operations
    fun verifyAndUnlock(pattern: String): Boolean {
        val matches = securityPrefs.verifyPattern(pattern)
        if (matches) {
            _isAppUnlocked.value = true
        }
        return matches
    }

    fun setPatternLock(newPattern: String) {
        securityPrefs.savedPattern = newPattern
        securityPrefs.isPatternLockEnabled = true
        isPatternLockEnabled.value = true
        _isAppUnlocked.value = true
        viewModelScope.launch {
            emitMessage("تم تفعيل قفل النقش بنجاح")
        }
    }

    fun disablePatternLock() {
        securityPrefs.clearPattern()
        isPatternLockEnabled.value = false
        _isAppUnlocked.value = true
        viewModelScope.launch {
            emitMessage("تم إلغاء قفل النقش")
        }
    }

    fun lockApp() {
        if (isPatternLockEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
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
            repository.incrementStrike(target.id, delta)
            _strikeAnimCount.value += 1
            emitMessage("تم تسجيل ضربة +$delta (${target.fullName})")
        }
    }

    fun incrementStrikeForId(id: Long, delta: Int = 1) {
        viewModelScope.launch {
            repository.incrementStrike(id, delta)
            _strikeAnimCount.value += 1
            emitMessage("Strike +$delta")
        }
    }

    fun resetStrikes(id: Long) {
        viewModelScope.launch {
            repository.setStrikeCount(id, 0)
            emitMessage("Strikes reset")
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

    fun initFormForNew() {
        editingId = null
        formName.value = ""
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
        val name = formName.value.trim()
        if (name.isEmpty()) {
            formError.value = "الاسم واللقب حقل إلزامي."
            return
        }

        val length = formLength.value.toDoubleOrNull()
        val width = formWidth.value.toDoubleOrNull()
        val s = formSScore.value.toDoubleOrNull()
        val f = formFScore.value.toDoubleOrNull()
        val c = formCScore.value.toDoubleOrNull()

        if (length == null || length <= 0 || width == null || width <= 0) {
            formError.value = "يجب إدخال أبعاد موجبة صحيحة."
            return
        }
        if (s == null || f == null || c == null) {
            formError.value = "يرجى إدخال قيم صحيحة للدرجات (S, F, C)."
            return
        }

        formError.value = null

        val historicalStrikesTotal = formHistoricalEntries.value.sumOf { it.second }

        val individualToSave = Individual(
            id = editingId ?: 0,
            fullName = name,
            type = formType.value,
            length = length,
            width = width,
            size = Individual.calculateSize(length, width),
            sScore = s,
            fScore = f,
            cScore = c,
            boost = Individual.calculateBoost(s, f, c, Individual.calculateSize(length, width)),
            strikeCount = if (editingId != null && historicalStrikesTotal == 0) {
                allIndividuals.value.find { it.id == editingId }?.strikeCount ?: 0
            } else {
                historicalStrikesTotal
            }
        )

        viewModelScope.launch {
            val savedId = repository.save(individualToSave)
            val effectiveId = if (editingId != null && editingId != 0L) editingId!! else savedId

            if (formHistoricalEntries.value.isNotEmpty()) {
                repository.recordHistoricalStrikes(effectiveId, formHistoricalEntries.value)
            }

            _eventFlow.emit("تم حفظ السجل بنجاح")
            onSuccess()
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

    fun deleteStrikeEvent(event: StrikeEvent) {
        viewModelScope.launch {
            repository.deleteStrikeEvent(event)
            val msg = when (currentLanguage.value) {
                "ar" -> "تم حذف ضربة (+${event.count} - ${event.individualName})"
                "fr" -> "Strike supprimé (+${event.count} - ${event.individualName})"
                else -> "Strike deleted (+${event.count} - ${event.individualName})"
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
            repository.restoreStrikeEvent(event)
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
            emitMessage("اللغة: ${lang.uppercase()}")
        }
    }

    fun reloadSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            emitMessage("تم إعادة تحميل البيانات النموذجية بنجاح")
        }
    }

    fun seedSampleData() = reloadSampleData()

    companion object {
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
