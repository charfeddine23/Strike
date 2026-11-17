package com.example.data.repository

import com.example.data.local.IndividualDao
import com.example.data.local.StrikeEventDao
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
    }

    val allIndividuals: Flow<List<Individual>> = dao.getAllIndividuals()
    val allStrikeEvents: Flow<List<StrikeEvent>> = strikeEventDao.getAllStrikeEvents()

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
        return dao.insert(prepared)
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
     * [StrikeEvent] records (each with count = 1) up to [MAX_LITERAL_RECENT_STRIKES] (10,000).
     */
    suspend fun incrementStrike(id: Long, delta: Int = 1): Long {
        val countDelta = delta.coerceAtLeast(1)
        dao.incrementStrike(id, countDelta)

        val individual = dao.getIndividualById(id)
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now.time)
        val baseTimestamp = System.currentTimeMillis()
        val personName = individual?.fullName ?: "Target #$id"
        val boostVal = individual?.boost ?: 0.0

        val currentCount = strikeEventDao.getCount()
        val availableSlots = (MAX_LITERAL_RECENT_STRIKES - currentCount).coerceAtLeast(0)
        val eventsToInsertCount = minOf(countDelta, availableSlots)

        if (eventsToInsertCount <= 0) {
            // If log already hit 10,000 ceiling, trim oldest excess so the newest strike is always recorded literally
            trimOldestStrikesBeyondLimit(MAX_LITERAL_RECENT_STRIKES - 1)
            val single = StrikeEvent(
                individualId = id,
                individualName = personName,
                year = year,
                date = dateStr,
                time = timeStr,
                count = 1,
                strikeType = "Strike +1",
                performanceValue = boostVal,
                notes = if (countDelta > 1) "Literal strike (batch +$countDelta)" else "Auto-recorded literal strike",
                timestamp = baseTimestamp
            )
            return strikeEventDao.insertEvent(single)
        }

        if (eventsToInsertCount == 1 && countDelta == 1) {
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

        val literalEvents = (0 until eventsToInsertCount).map { idx ->
            StrikeEvent(
                individualId = id,
                individualName = personName,
                year = year,
                date = dateStr,
                time = timeStr,
                count = 1,
                strikeType = "Strike +1",
                performanceValue = boostVal,
                notes = if (countDelta > 1) "Literal strike #${idx + 1}/$countDelta" else "Auto-recorded strike event",
                timestamp = baseTimestamp + idx
            )
        }

        val lastEvent = literalEvents.last()
        val bulk = literalEvents.dropLast(1)
        if (bulk.isNotEmpty()) {
            bulk.chunked(DB_INSERT_CHUNK_SIZE).forEach { chunk ->
                strikeEventDao.insertAll(chunk)
            }
        }
        return strikeEventDao.insertEvent(lastEvent)
    }

    private suspend fun trimOldestStrikesBeyondLimit(targetMaxCount: Int) {
        val all = strikeEventDao.getAllStrikeEventsList()
        if (all.size <= targetMaxCount) return
        val excess = all.drop(targetMaxCount.coerceAtLeast(0))
        excess.forEach { strikeEventDao.deleteEvent(it) }
    }

    suspend fun setStrikeCount(id: Long, count: Int) {
        dao.setStrikeCount(id, count.coerceAtLeast(0))
    }

    suspend fun getLatestStrikeDate(): String? = strikeEventDao.getLatestStrikeDate()

    suspend fun getEventsForDate(date: String): List<StrikeEvent> = strikeEventDao.getEventsForDate(date)

    suspend fun updateEventNotes(id: Long, notes: String) = strikeEventDao.updateEventNotes(id, notes)

    /**
     * Stores past/historical strikes per year for an individual, expanding every strike literally (count = 1)
     * up to [MAX_LITERAL_RECENT_STRIKES] (10,000).
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
        var remainingBudget = (MAX_LITERAL_RECENT_STRIKES - existingCount).coerceAtLeast(0)

        val expandedEvents = mutableListOf<StrikeEvent>()
        for ((year, rawCount) in historicalEntries.sortedByDescending { it.first }) {
            if (rawCount <= 0 || remainingBudget <= 0) continue
            val takeCount = minOf(rawCount, remainingBudget)
            val yearBaseMillis = nowMillis - ((currentYear - year).coerceAtLeast(0) * 31_536_000_000L)
            for (idx in 0 until takeCount) {
                expandedEvents.add(
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
            remainingBudget -= takeCount
        }
        if (expandedEvents.isNotEmpty()) {
            expandedEvents.chunked(DB_INSERT_CHUNK_SIZE).forEach { chunk ->
                strikeEventDao.insertAll(chunk)
            }
        }
    }

    /**
     * Expands any legacy grouped StrikeEvent rows (where count > 1) into literal individual rows (count = 1)
     * up to [MAX_LITERAL_RECENT_STRIKES] (10,000) so Recent Strikes literally displays every strike.
     */
    suspend fun normalizeBatchStrikeEventsToLiteral(maxTotal: Int = MAX_LITERAL_RECENT_STRIKES) {
        val all = strikeEventDao.getAllStrikeEventsList()
        val batchRows = all.filter { it.count > 1 }
        if (batchRows.isEmpty()) return

        val singleRowsCount = all.count { it.count <= 1 }
        var remainingBudget = (maxTotal - singleRowsCount).coerceAtLeast(0)
        val newLiterals = mutableListOf<StrikeEvent>()

        for (ev in batchRows) {
            strikeEventDao.deleteEvent(ev)
            val expandCount = ev.count.coerceAtLeast(1)
            val take = minOf(expandCount, remainingBudget)
            for (i in 0 until take) {
                newLiterals.add(
                    ev.copy(
                        id = 0L,
                        count = 1,
                        strikeType = if (ev.strikeType.startsWith("Historical")) "Historical Entry" else "Strike +1",
                        timestamp = ev.timestamp + i
                    )
                )
            }
            remainingBudget -= take
        }
        if (newLiterals.isNotEmpty()) {
            newLiterals.chunked(DB_INSERT_CHUNK_SIZE).forEach { chunk ->
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

    suspend fun seedSampleDataIfEmpty() {
        if (dao.getCount() == 0) {
            seedSampleData()
        }
    }

    suspend fun seedSampleData() {
        val samples = listOf(
            createSample("Tariq Al-Mansoor", "M", 24.5, 12.0, 88.0, 76.5, 92.0, 42, "+15550198"),
            createSample("Alexandre Moreau", "TS", 30.0, 15.5, 94.0, 85.0, 89.5, 38, "+33612345678"),
            createSample("Fatima Zahra", "FG", 18.0, 14.2, 82.5, 91.0, 78.0, 55, "+213555123456"),
            createSample("Karim Ben Salah", "G", 28.0, 16.0, 79.0, 83.5, 87.0, 29, "+213555987654"),
            createSample("Sarah Jenkins", "M", 22.0, 11.5, 90.0, 88.0, 93.0, 47, "+15550244"),
            createSample("Yassine Trabelsi", "FG", 26.5, 13.8, 86.0, 79.5, 84.0, 31, "+21698123456")
        )
        dao.insertAll(samples)

        // Seed realistic literal 1-by-1 multi-year strike events (each row has count = 1)
        val groupedSeeds = listOf(
            Triple(1L to "Tariq Al-Mansoor", 2026 to "2026-10-15", 18 to (94.5 to "Current season strike")),
            Triple(1L to "Tariq Al-Mansoor", 2025 to "2025-08-20", 14 to (90.0 to "2025 tournament strike")),
            Triple(1L to "Tariq Al-Mansoor", 2024 to "2024-05-12", 10 to (88.0 to "2024 qualification strike")),
            Triple(3L to "Fatima Zahra", 2026 to "2026-09-10", 25 to (91.0 to "Championship strike")),
            Triple(3L to "Fatima Zahra", 2025 to "2025-11-04", 20 to (87.5 to "2025 league strike")),
            Triple(3L to "Fatima Zahra", 2024 to "2024-07-22", 10 to (82.5 to "2024 debut strike")),
            Triple(2L to "Alexandre Moreau", 2026 to "2026-06-18", 22 to (89.5 to "Power strike")),
            Triple(2L to "Alexandre Moreau", 2025 to "2025-04-10", 16 to (85.0 to "Mid-season strike"))
        )
        val nowTs = System.currentTimeMillis()
        var tsOffset = 0L
        val sampleEvents = groupedSeeds.flatMap { (personPair, datePair, metaPair) ->
            val (indId, indName) = personPair
            val (year, dateStr) = datePair
            val (totalCount, perfNotes) = metaPair
            val (perfVal, noteText) = perfNotes
            val strikeType = if (year >= 2026) "Auto-Recorded" else "Historical Entry"
            (0 until totalCount).map {
                tsOffset++
                StrikeEvent(
                    individualId = indId,
                    individualName = indName,
                    year = year,
                    date = dateStr,
                    time = "14:22:10",
                    count = 1,
                    strikeType = strikeType,
                    performanceValue = perfVal,
                    notes = noteText,
                    timestamp = nowTs - ((groupedSeeds.sumOf { g -> g.third.first } - tsOffset) * 1000L)
                )
            }
        }
        sampleEvents.chunked(DB_INSERT_CHUNK_SIZE).forEach { chunk ->
            strikeEventDao.insertAll(chunk)
        }
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
        strikeEventDao.insertAll(strikeEvents)
        return Pair(individuals.size, strikeEvents.size)
    }

    suspend fun getIndividualsWithPhone(): List<Individual> = dao.getIndividualsWithPhone()

    private fun createSample(
        name: String,
        type: String,
        length: Double,
        width: Double,
        sScore: Double,
        fScore: Double,
        cScore: Double,
        strikes: Int,
        phone: String = ""
    ): Individual {
        val size = Individual.calculateSize(length, width)
        val boost = Individual.calculateBoost(sScore, fScore, cScore, size)
        return Individual(
            fullName = name,
            type = type,
            length = length,
            width = width,
            size = size,
            sScore = sScore,
            fScore = fScore,
            cScore = cScore,
            boost = boost,
            strikeCount = strikes,
            phoneNumber = phone
        )
    }
}
