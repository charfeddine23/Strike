package com.example.data.repository

import com.example.data.local.IndividualDao
import com.example.data.local.StrikeEventDao
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import kotlinx.coroutines.flow.Flow

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

    suspend fun incrementStrike(id: Long, delta: Int = 1) {
        dao.incrementStrike(id, delta)
    }

    suspend fun setStrikeCount(id: Long, count: Int) {
        dao.setStrikeCount(id, count.coerceAtLeast(0))
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

        // Increment individual strike count
        dao.incrementStrike(individualId, 1)

        // Record strike event
        val event = StrikeEvent(
            individualId = individualId,
            individualName = individualName,
            date = date,
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

        // Seed some initial strike events
        val currentDate = StrikeEvent.currentDateString()
        val allCurrent = dao.getIndividualById(1)
        if (allCurrent != null) {
            val sampleEvents = listOf(
                StrikeEvent(
                    individualId = 1,
                    individualName = "Tariq Al-Mansoor",
                    date = currentDate,
                    strikeType = "Power",
                    performanceValue = 94.5,
                    notes = "High-velocity impact test"
                ),
                StrikeEvent(
                    individualId = 3,
                    individualName = "Fatima Zahra",
                    date = currentDate,
                    strikeType = "Precision",
                    performanceValue = 88.0,
                    notes = "Rapid succession strike"
                )
            )
            strikeEventDao.insertAll(sampleEvents)
        }
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
