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
        dao.delete(individual)
        strikeEventDao.deleteEventsForIndividual(individual.id)
    }

    /**
     * Increments the individual's strike count and AUTOMATICALLY creates an audit StrikeEvent record
     * with the current date, time, year, and individual details.
     */
    suspend fun incrementStrike(id: Long, delta: Int = 1) {
        val countDelta = delta.coerceAtLeast(1)
        dao.incrementStrike(id, countDelta)

        val individual = dao.getIndividualById(id)
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now.time)

        val typeLabel = when (countDelta) {
            1 -> "Strike +1"
            5 -> "Quick +5"
            10 -> "Burst +10"
            else -> "Batch +$countDelta"
        }

        val event = StrikeEvent(
            individualId = id,
            individualName = individual?.fullName ?: "Target #$id",
            year = year,
            date = dateStr,
            time = timeStr,
            count = countDelta,
            strikeType = typeLabel,
            performanceValue = individual?.boost ?: 0.0,
            notes = "Auto-recorded strike event",
            timestamp = System.currentTimeMillis()
        )
        strikeEventDao.insertEvent(event)
    }

    suspend fun setStrikeCount(id: Long, count: Int) {
        dao.setStrikeCount(id, count.coerceAtLeast(0))
    }

    /**
     * Stores past/historical strikes per year for an individual.
     */
    suspend fun recordHistoricalStrikes(individualId: Long, historicalEntries: List<Pair<Int, Int>>) {
        val individual = dao.getIndividualById(individualId)
        val name = individual?.fullName ?: "Target #$individualId"
        val events = historicalEntries.filter { it.second > 0 }.map { (year, count) ->
            StrikeEvent(
                individualId = individualId,
                individualName = name,
                year = year,
                date = "$year-06-15",
                time = "12:00:00",
                count = count,
                strikeType = "Historical Entry",
                performanceValue = individual?.boost ?: 0.0,
                notes = "سجلات سابقة لسنة $year ($count ضربة)",
                timestamp = System.currentTimeMillis() - ((Calendar.getInstance().get(Calendar.YEAR) - year).coerceAtLeast(0) * 31536000000L)
            )
        }
        if (events.isNotEmpty()) {
            strikeEventDao.insertAll(events)
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
            createSample("Tariq Al-Mansoor", "M", 24.5, 12.0, 88.0, 76.5, 92.0, 42),
            createSample("Alexandre Moreau", "TS", 30.0, 15.5, 94.0, 85.0, 89.5, 38),
            createSample("Fatima Zahra", "FG", 18.0, 14.2, 82.5, 91.0, 78.0, 55),
            createSample("Karim Ben Salah", "G", 28.0, 16.0, 79.0, 83.5, 87.0, 29),
            createSample("Sarah Jenkins", "M", 22.0, 11.5, 90.0, 88.0, 93.0, 47),
            createSample("Yassine Trabelsi", "FG", 26.5, 13.8, 86.0, 79.5, 84.0, 31)
        )
        dao.insertAll(samples)

        // Seed realistic multi-year strike events for the Yearly Dashboard
        val sampleEvents = listOf(
            StrikeEvent(
                individualId = 1,
                individualName = "Tariq Al-Mansoor",
                year = 2026,
                date = "2026-10-15",
                time = "14:22:10",
                count = 18,
                strikeType = "Auto-Recorded",
                performanceValue = 94.5,
                notes = "Current season strikes"
            ),
            StrikeEvent(
                individualId = 1,
                individualName = "Tariq Al-Mansoor",
                year = 2025,
                date = "2025-08-20",
                time = "11:15:30",
                count = 14,
                strikeType = "Historical Entry",
                performanceValue = 90.0,
                notes = "2025 tournament strikes"
            ),
            StrikeEvent(
                individualId = 1,
                individualName = "Tariq Al-Mansoor",
                year = 2024,
                date = "2024-05-12",
                time = "16:45:00",
                count = 10,
                strikeType = "Historical Entry",
                performanceValue = 88.0,
                notes = "2024 qualification strikes"
            ),
            StrikeEvent(
                individualId = 3,
                individualName = "Fatima Zahra",
                year = 2026,
                date = "2026-09-10",
                time = "09:30:15",
                count = 25,
                strikeType = "Auto-Recorded",
                performanceValue = 91.0,
                notes = "Championship strikes"
            ),
            StrikeEvent(
                individualId = 3,
                individualName = "Fatima Zahra",
                year = 2025,
                date = "2025-11-04",
                time = "17:10:00",
                count = 20,
                strikeType = "Historical Entry",
                performanceValue = 87.5,
                notes = "2025 league strikes"
            ),
            StrikeEvent(
                individualId = 3,
                individualName = "Fatima Zahra",
                year = 2024,
                date = "2024-07-22",
                time = "13:20:45",
                count = 10,
                strikeType = "Historical Entry",
                performanceValue = 82.5,
                notes = "2024 debut strikes"
            ),
            StrikeEvent(
                individualId = 2,
                individualName = "Alexandre Moreau",
                year = 2026,
                date = "2026-06-18",
                time = "15:00:20",
                count = 22,
                strikeType = "Auto-Recorded",
                performanceValue = 89.5,
                notes = "Power strikes"
            ),
            StrikeEvent(
                individualId = 2,
                individualName = "Alexandre Moreau",
                year = 2025,
                date = "2025-04-10",
                time = "10:14:00",
                count = 16,
                strikeType = "Historical Entry",
                performanceValue = 85.0,
                notes = "Mid-season strikes"
            )
        )
        strikeEventDao.insertAll(sampleEvents)
    }

    private fun createSample(
        name: String,
        type: String,
        length: Double,
        width: Double,
        sScore: Double,
        fScore: Double,
        cScore: Double,
        strikes: Int
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
            strikeCount = strikes
        )
    }
}
