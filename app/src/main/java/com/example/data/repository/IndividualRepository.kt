package com.example.data.repository

import com.example.data.local.IndividualDao
import com.example.data.local.IndividualLatestDateTuple
import com.example.data.local.StrikeEventDao
import com.example.data.local.YearlyPersonAggregate
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IndividualRepository(
    private val dao: IndividualDao,
    private val strikeEventDao: StrikeEventDao
) {
    companion object {
        const val MAX_LITERAL_RECENT_STRIKES = 10_000
        private const val DB_INSERT_CHUNK_SIZE = 500

        fun isHistoricalStrikeType(strikeType: String): Boolean {
            return strikeType.startsWith("Historical", ignoreCase = true)
        }
    }

    val allIndividuals: Flow<List<Individual>> = dao.getAllIndividuals()
    val allStrikeEvents: Flow<List<StrikeEvent>> = strikeEventDao.getAllStrikeEvents()
    val allTimeStrikesCountFlow: Flow<Int> = strikeEventDao.getAllTimeStrikesCountFlow()
    val yearlyPersonAggregatesFlow: Flow<List<YearlyPersonAggregate>> = strikeEventDao.getYearlyPersonAggregatesFlow()
    val latestStrikeDateByIndividualFlow: Flow<List<IndividualLatestDateTuple>> =
        strikeEventDao.getLatestStrikeDateByIndividualFlow()

    suspend fun getAllTimeStrikesCount(): Int = strikeEventDao.getAllTimeStrikesCount()

    suspend fun getYearlyPersonAggregates(): List<YearlyPersonAggregate> =
        strikeEventDao.getYearlyPersonAggregates()

    suspend fun getIndividualById(id: Long): Individual? = dao.getIndividualById(id)

    fun getIndividualFlow(id: Long): Flow<Individual?> = dao.getIndividualFlow(id)

    fun getStrikeEventsForIndividual(id: Long): Flow<List<StrikeEvent>> =
        strikeEventDao.getEventsForIndividual(id)

    suspend fun save(individual: Individual): Long {
        val calculatedSize = Individual.calculateSize(individual.length, individual.width)
        val calculatedBoost = Individual.calculateBoost(
            individual.sScore,
            individual.fScore,
            individual.cScore,
            calculatedSize
        )
        val prepared = individual.copy(
            size = calculatedSize,
            boost = calculatedBoost,
            updatedAt = System.currentTimeMillis()
        )
        return if (prepared.id > 0L) {
            dao.update(prepared)
            strikeEventDao.updateIndividualNameForEvents(prepared.id, prepared.fullName)
            prepared.id
        } else {
            dao.insert(prepared)
        }
    }

    suspend fun delete(individual: Individual) {
        dao.deleteWithCascade(individual)
    }

    suspend fun deleteStrikeEvent(event: StrikeEvent) {
        strikeEventDao.deleteEvent(event)
        dao.incrementStrike(event.individualId, -event.count)
    }

    suspend fun restoreStrikeEvent(event: StrikeEvent): Long {
        val insertedId = strikeEventDao.insertEvent(event)
        dao.incrementStrike(event.individualId, event.count)
        return insertedId
    }

    /**
     * Increments the individual's strike count and AUTOMATICALLY creates literal 1-by-1 audit
     * [StrikeEvent] records (each with count = 1) up to [MAX_LITERAL_RECENT_STRIKES] (10,000)
     * without deleting older historical records so All-Time and Yearly Analytics remain 100% intact.
     */
    suspend fun incrementStrike(id: Long, delta: Int = 1): Long {
        val countDelta = delta.coerceAtLeast(1)
        dao.incrementStrike(id, countDelta)

        val individual = dao.getIndividualById(id)
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(now.time)
        val baseTimestamp = System.currentTimeMillis()
        val personName = individual?.fullName ?: "Target #$id"
        val boostVal = individual?.boost ?: 0.0

        if (countDelta == 1) {
            val event = StrikeEvent(
                individualId = id,
                individualName = personName,
                year = year,
                date = dateStr,
                time = timeStr,
                count = 1,
                strikeType = "Strike +1",
                performanceValue = boostVal,
                notes = "Auto-recorded strike event",
                timestamp = baseTimestamp
            )
            return strikeEventDao.insertEvent(event)
        }

        val currentCount = strikeEventDao.getCount()
        val availableLiteralSlots = (MAX_LITERAL_RECENT_STRIKES - currentCount).coerceAtLeast(1)
        val literalCount = minOf(countDelta, availableLiteralSlots)
        val overflowCount = countDelta - literalCount

        val eventsToInsert = ArrayList<StrikeEvent>(literalCount + if (overflowCount > 0) 1 else 0)
        for (idx in 0 until literalCount) {
            eventsToInsert.add(
                StrikeEvent(
                    individualId = id,
                    individualName = personName,
                    year = year,
                    date = dateStr,
                    time = timeStr,
                    count = 1,
                    strikeType = "Strike +1",
                    performanceValue = boostVal,
                    notes = "Literal strike #${idx + 1}/$countDelta",
                    timestamp = baseTimestamp + idx
                )
            )
        }
        if (overflowCount > 0) {
            eventsToInsert.add(
                StrikeEvent(
                    individualId = id,
                    individualName = personName,
                    year = year,
                    date = dateStr,
                    time = timeStr,
                    count = overflowCount,
                    strikeType = "Batch +$overflowCount",
                    performanceValue = boostVal,
                    notes = "Historical overflow (+$overflowCount strikes)",
                    timestamp = baseTimestamp + literalCount
                )
            )
        }

        val lastEvent = eventsToInsert.last()
        val bulk = eventsToInsert.dropLast(1)
        if (bulk.isNotEmpty()) {
            bulk.chunked(DB_INSERT_CHUNK_SIZE).forEach { chunk ->
                strikeEventDao.insertAll(chunk)
            }
        }
        return strikeEventDao.insertEvent(lastEvent)
    }

    suspend fun setStrikeCount(id: Long, count: Int) {
        dao.setStrikeCount(id, count.coerceAtLeast(0))
    }

    suspend fun getLatestStrikeDate(): String? = strikeEventDao.getLatestStrikeDate()

    suspend fun getEventsForDate(date: String): List<StrikeEvent> = strikeEventDao.getEventsForDate(date)

    suspend fun updateEventNotes(id: Long, notes: String) = strikeEventDao.updateEventNotes(id, notes)

    /**
     * Stores past/historical strikes per year for an individual, expanding every strike literally (count = 1)
     * up to [MAX_LITERAL_RECENT_STRIKES] (10,000) and preserving any overflow above 10,000 in grouped
     * overflow records so no historical strikes are ever dropped from analytics.
     */
    suspend fun recordHistoricalStrikes(
        individualId: Long,
        historicalEntries: List<Pair<Int, Int>>,
        lang: String = "ar"
    ) {
        val individual = dao.getIndividualById(individualId)
        val name = individual?.fullName ?: "Target #$individualId"
        val boostVal = individual?.boost ?: 0.0
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val nowMillis = System.currentTimeMillis()
        val existingCount = strikeEventDao.getCount()
        var remainingLiteralBudget = (MAX_LITERAL_RECENT_STRIKES - existingCount).coerceAtLeast(0)

        val eventsToInsert = mutableListOf<StrikeEvent>()
        for ((year, rawCount) in historicalEntries.sortedByDescending { it.first }) {
            if (rawCount <= 0) continue
            val takeLiteral = minOf(rawCount, remainingLiteralBudget)
            val overflowCount = rawCount - takeLiteral
            val yearBaseMillis = nowMillis - ((currentYear - year).coerceAtLeast(0) * 31_536_000_000L)
            for (idx in 0 until takeLiteral) {
                eventsToInsert.add(
                    StrikeEvent(
                        individualId = individualId,
                        individualName = name,
                        year = year,
                        date = "$year-06-15",
                        time = "12:00:00",
                        count = 1,
                        strikeType = "Historical Entry",
                        performanceValue = boostVal,
                        notes = if (lang == "ar") {
                            "ضربة سابقة مسجلة حرفياً لسنة $year (${idx + 1}/$rawCount)"
                        } else {
                            "Historical literal strike $year (${idx + 1}/$rawCount)"
                        },
                        timestamp = yearBaseMillis + idx
                    )
                )
            }
            if (overflowCount > 0) {
                eventsToInsert.add(
                    StrikeEvent(
                        individualId = individualId,
                        individualName = name,
                        year = year,
                        date = "$year-06-15",
                        time = "12:00:00",
                        count = overflowCount,
                        strikeType = "Historical Batch +$overflowCount",
                        performanceValue = boostVal,
                        notes = if (lang == "ar") {
                            "أرشيف ضربات تاريخية إضافية لسنة $year (+$overflowCount)"
                        } else {
                            "Historical overflow archive $year (+$overflowCount)"
                        },
                        timestamp = yearBaseMillis + takeLiteral
                    )
                )
            }
            remainingLiteralBudget = (remainingLiteralBudget - takeLiteral).coerceAtLeast(0)
        }
        if (eventsToInsert.isNotEmpty()) {
            eventsToInsert.chunked(DB_INSERT_CHUNK_SIZE).forEach { chunk ->
                strikeEventDao.insertAll(chunk)
            }
        }
    }

    /**
     * Loads the individual's historical strikes grouped and summed by year (descending by year).
     */
    suspend fun getHistoricalStrikesByYear(individualId: Long): List<Pair<Int, Int>> {
        val events = strikeEventDao.getEventsForIndividualList(individualId)
        return events
            .filter { isHistoricalStrikeType(it.strikeType) }
            .groupBy { it.year }
            .mapValues { (_, list) -> list.sumOf { it.count.coerceAtLeast(1) } }
            .entries
            .filter { it.value > 0 }
            .sortedByDescending { it.key }
            .map { it.key to it.value }
    }

    /**
     * Atomically replaces all historical strikes by year for [individualId] while preserving
     * live (non-historical) strike events and recomputing Individual.strikeCount accurately.
     */
    suspend fun replaceHistoricalStrikesForIndividual(
        individualId: Long,
        historicalEntries: List<Pair<Int, Int>>,
        lang: String = "ar"
    ): Int {
        val mergedByYear = linkedMapOf<Int, Int>()
        for ((year, count) in historicalEntries) {
            if (year in 2000..2050 && count > 0) {
                mergedByYear[year] = (mergedByYear[year] ?: 0) + count
            }
        }
        val cleanEntries = mergedByYear.entries
            .sortedByDescending { it.key }
            .map { it.key to it.value }

        val existingEvents = strikeEventDao.getEventsForIndividualList(individualId)
        val liveSum = existingEvents
            .filterNot { isHistoricalStrikeType(it.strikeType) }
            .sumOf { it.count.coerceAtLeast(1).toLong() }
        val oldHistoricalSum = existingEvents
            .filter { isHistoricalStrikeType(it.strikeType) }
            .sumOf { it.count.coerceAtLeast(1).toLong() }
        val currentPersonCount = (dao.getIndividualById(individualId)?.strikeCount ?: 0).toLong()
        val legacyUntracked = (currentPersonCount - (liveSum + oldHistoricalSum)).coerceAtLeast(0L)

        strikeEventDao.deleteHistoricalEventsForIndividual(individualId)
        if (cleanEntries.isNotEmpty()) {
            recordHistoricalStrikes(individualId, cleanEntries, lang)
        }

        val newHistoricalSum = cleanEntries.sumOf { it.second.toLong() }
        val newTotal = (liveSum + legacyUntracked + newHistoricalSum)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        dao.setStrikeCount(individualId, newTotal)
        return newTotal
    }

    /**
     * Expands any legacy grouped StrikeEvent rows (where count > 1) into literal individual rows (count = 1)
     * up to [MAX_LITERAL_RECENT_STRIKES] (10,000) without ever losing or truncating overflow strikes.
     */
    suspend fun normalizeBatchStrikeEventsToLiteral(maxTotal: Int = MAX_LITERAL_RECENT_STRIKES) {
        val all = strikeEventDao.getAllStrikeEventsList()
        val batchRows = all.filter { it.count > 1 }
        if (batchRows.isEmpty()) return

        val singleRowsCount = all.count { it.count <= 1 }
        var remainingBudget = (maxTotal - singleRowsCount).coerceAtLeast(0)
        if (remainingBudget <= 0) return

        val newEvents = mutableListOf<StrikeEvent>()
        for (ev in batchRows) {
            if (remainingBudget <= 0) break
            strikeEventDao.deleteEvent(ev)
            val expandCount = ev.count.coerceAtLeast(1)
            val take = minOf(expandCount, remainingBudget)
            val remainder = expandCount - take
            for (i in 0 until take) {
                newEvents.add(
                    ev.copy(
                        id = 0L,
                        count = 1,
                        strikeType = if (ev.strikeType.startsWith("Historical")) "Historical Entry" else "Strike +1",
                        timestamp = ev.timestamp + i
                    )
                )
            }
            if (remainder > 0) {
                newEvents.add(
                    ev.copy(
                        id = 0L,
                        count = remainder,
                        timestamp = ev.timestamp + take
                    )
                )
            }
            remainingBudget -= take
        }
        if (newEvents.isNotEmpty()) {
            newEvents.chunked(DB_INSERT_CHUNK_SIZE).forEach { chunk ->
                strikeEventDao.insertAll(chunk)
            }
        }
    }

    suspend fun logStrikeEvent(
        individualId: Long,
        date: String,
        strikeType: String,
        performanceValue: Double,
        notes: String = ""
    ): Long {
        val individual = dao.getIndividualById(individualId)
        val individualName = individual?.fullName ?: "Unknown"

        dao.incrementStrike(individualId, 1)

        val now = Calendar.getInstance()
        val year = date.take(4).toIntOrNull() ?: now.get(Calendar.YEAR)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now.time)

        val event = StrikeEvent(
            individualId = individualId,
            individualName = individualName,
            year = year,
            date = date,
            time = timeStr,
            count = 1,
            strikeType = strikeType,
            performanceValue = performanceValue,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )
        return strikeEventDao.insertEvent(event)
    }

    suspend fun getAllIndividualsList(): List<Individual> = dao.getAllIndividualsList()

    suspend fun getAllStrikeEventsList(): List<StrikeEvent> = strikeEventDao.getAllStrikeEventsList()

    suspend fun restoreBackup(
        individuals: List<Individual>,
        strikeEvents: List<StrikeEvent>,
        replaceExisting: Boolean
    ): Pair<Int, Int> {
        if (replaceExisting) {
            dao.clearAll()
            strikeEventDao.clearAll()
        }
        dao.insertAll(individuals)
        strikeEvents.chunked(DB_INSERT_CHUNK_SIZE).forEach { chunk ->
            strikeEventDao.insertAll(chunk)
        }
        return Pair(individuals.size, strikeEvents.size)
    }

    suspend fun getIndividualsWithPhone(): List<Individual> = dao.getIndividualsWithPhone()

    suspend fun updateFirstStrikeLocation(
        individualId: Long,
        lat: Double?,
        lng: Double?,
        placeName: String
    ) {
        dao.updateFirstStrikeLocation(
            id = individualId,
            lat = lat,
            lng = lng,
            placeName = com.example.data.map.NominatimGeocoder.stripTifinagh(placeName)
        )
    }
}
