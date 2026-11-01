package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder {
    SCORE_DESC,
    BOOST_DESC,
    STRIKES_DESC,
    SIZE_DESC,
    NAME_ASC
}

class IndividualViewModel(
    application: Application,
    private val repository: IndividualRepository = run {
        val db = AppDatabase.getDatabase(application)
        IndividualRepository(db.individualDao(), db.strikeEventDao())
    }
) : AndroidViewModel(application) {

    val allIndividuals: StateFlow<List<Individual>> = repository.allIndividuals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allStrikeEvents: StateFlow<List<StrikeEvent>> = repository.allStrikeEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search, Filter & Sort
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("ALL")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.BOOST_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Filtered and Sorted list
    val filteredIndividuals: StateFlow<List<Individual>> = combine(
        allIndividuals,
        _searchQuery,
        _filterType,
        _sortOrder
    ) { list, query, type, sort ->
        var result = list

        if (type != "ALL") {
            result = result.filter { it.type.equals(type, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.fullName.lowercase().contains(q) || it.type.lowercase().contains(q)
            }
        }

        when (sort) {
            SortOrder.SCORE_DESC -> result.sortedByDescending { it.totalScore }
            SortOrder.BOOST_DESC -> result.sortedByDescending { it.boost }
            SortOrder.STRIKES_DESC -> result.sortedByDescending { it.strikeCount }
            SortOrder.SIZE_DESC -> result.sortedByDescending { it.size }
            SortOrder.NAME_ASC -> result.sortedBy { it.fullName.lowercase() }
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

    // Live Auto-Calculations
    val liveCalculatedSize: StateFlow<Double> = combine(
        formLength,
        formWidth
    ) { lStr, wStr ->
        val l = lStr.toDoubleOrNull() ?: 0.0
        val w = wStr.toDoubleOrNull() ?: 0.0
        Individual.calculateSize(l, w)
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
    ) { sStr, fStr, cStr, sz ->
        val s = sStr.toDoubleOrNull() ?: 0.0
        val f = fStr.toDoubleOrNull() ?: 0.0
        val c = cStr.toDoubleOrNull() ?: 0.0
        Individual.calculateBoost(s, f, c, sz)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 257.0
    )

    // Events
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    // Strike trigger animation counter
    private val _strikeAnimCount = MutableStateFlow(0)
    val strikeAnimCount: StateFlow<Int> = _strikeAnimCount.asStateFlow()

    // Language setting ("en", "ar", "fr")
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Theme mode (null: system, true: dark, false: light)
    private val _isDarkThemeOverride = MutableStateFlow<Boolean?>(null)
    val isDarkThemeOverride: StateFlow<Boolean?> = _isDarkThemeOverride.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
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

    fun triggerStrike(delta: Int = 1) {
        viewModelScope.launch {
            val target = currentStrikeIndividual.value ?: return@launch
            repository.incrementStrike(target.id, delta)
            _strikeAnimCount.value += 1
            _eventFlow.emit("Strike +$delta (${target.fullName})")
        }
    }

    fun incrementStrikeForId(id: Long, delta: Int = 1) {
        viewModelScope.launch {
            repository.incrementStrike(id, delta)
            _strikeAnimCount.value += 1
            _eventFlow.emit("Strike +$delta")
        }
    }

    fun resetStrikes(id: Long) {
        viewModelScope.launch {
            repository.setStrikeCount(id, 0)
            _eventFlow.emit("Strikes reset")
        }
    }

    fun logManualStrike(
        individualId: Long,
        date: String,
        strikeType: String,
        performanceValue: Double,
        notes: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.logStrikeEvent(individualId, date, strikeType, performanceValue, notes)
            _strikeAnimCount.value += 1
            _eventFlow.emit("Logged $strikeType strike event ($performanceValue)")
            onSuccess()
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
    }

    fun saveCurrentForm(onSuccess: () -> Unit) {
        val name = formName.value.trim()
        if (name.isEmpty()) {
            formError.value = "Full name is required."
            return
        }

        val length = formLength.value.toDoubleOrNull()
        val width = formWidth.value.toDoubleOrNull()
        val s = formSScore.value.toDoubleOrNull()
        val f = formFScore.value.toDoubleOrNull()
        val c = formCScore.value.toDoubleOrNull()

        if (length == null || length <= 0 || width == null || width <= 0) {
            formError.value = "Length and Width must be valid positive numbers."
            return
        }

        if (s == null || f == null || c == null) {
            formError.value = "Scores must be valid numbers."
            return
        }

        viewModelScope.launch {
            val currentExisting = if (editingId != null) {
                repository.getIndividualById(editingId!!)
            } else null

            val strikes = currentExisting?.strikeCount ?: 0

            val individual = Individual(
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
                strikeCount = strikes
            )

            repository.save(individual)
            _eventFlow.emit(if (editingId != null) "Record updated" else "Record created")
            onSuccess()
        }
    }

    fun deleteIndividual(individual: Individual) {
        viewModelScope.launch {
            repository.delete(individual)
            _eventFlow.emit("Record deleted")
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            _eventFlow.emit("Sample data loaded")
        }
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    fun toggleTheme() {
        _isDarkThemeOverride.value = when (_isDarkThemeOverride.value) {
            null -> true
            true -> false
            false -> true
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return IndividualViewModel(application) as T
                }
            }
    }
}
