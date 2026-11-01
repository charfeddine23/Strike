package com.example

import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun filter_by_type_and_name_logic() {
        val individuals = listOf(
            Individual(id = 1, fullName = "Tariq Al-Mansoor", type = "M", length = 20.0, width = 10.0, size = 200.0, sScore = 80.0, fScore = 80.0, cScore = 80.0, boost = 242.0),
            Individual(id = 2, fullName = "Fatima Zahra", type = "FG", length = 15.0, width = 10.0, size = 150.0, sScore = 90.0, fScore = 85.0, cScore = 85.0, boost = 261.5),
            Individual(id = 3, fullName = "Alexandre Moreau", type = "TS", length = 25.0, width = 12.0, size = 300.0, sScore = 70.0, fScore = 75.0, cScore = 80.0, boost = 228.0)
        )

        // Filter by type "FG"
        val fgList = individuals.filter { it.type.equals("FG", ignoreCase = true) }
        assertEquals(1, fgList.size)
        assertEquals("Fatima Zahra", fgList[0].fullName)

        // Search by query "tariq"
        val query = "tariq"
        val searchResults = individuals.filter {
            it.fullName.lowercase().contains(query) || it.type.lowercase().contains(query)
        }
        assertEquals(1, searchResults.size)
        assertEquals("Tariq Al-Mansoor", searchResults[0].fullName)

        // Search by type in search bar "ts"
        val searchTypeResults = individuals.filter {
            it.fullName.lowercase().contains("ts") || it.type.lowercase().contains("ts")
        }
        assertTrue(searchTypeResults.any { it.type == "TS" })
    }

    @Test
    fun strike_event_creation_and_defaults() {
        val event = StrikeEvent(
            individualId = 10L,
            individualName = "Karim Ben Salah",
            year = 2026,
            date = "2026-11-01",
            time = "14:30:00",
            count = 5,
            strikeType = "Quick +5",
            performanceValue = 98.5,
            notes = "Maximum impact performance"
        )

        assertEquals(10L, event.individualId)
        assertEquals("Karim Ben Salah", event.individualName)
        assertEquals(2026, event.year)
        assertEquals("2026-11-01", event.date)
        assertEquals("14:30:00", event.time)
        assertEquals(5, event.count)
        assertEquals("Quick +5", event.strikeType)
        assertEquals(98.5, event.performanceValue, 0.001)
        assertEquals("Maximum impact performance", event.notes)
        assertNotNull(StrikeEvent.currentDateString())
    }

    @Test
    fun test_yearly_strike_aggregation() {
        val events = listOf(
            StrikeEvent(individualId = 1, individualName = "Tariq", year = 2026, count = 10),
            StrikeEvent(individualId = 1, individualName = "Tariq", year = 2026, count = 5),
            StrikeEvent(individualId = 2, individualName = "Fatima", year = 2025, count = 25),
            StrikeEvent(individualId = 2, individualName = "Fatima", year = 2024, count = 15)
        )

        val grouped = events.groupBy { it.year }
        assertEquals(15, grouped[2026]?.sumOf { it.count })
        assertEquals(25, grouped[2025]?.sumOf { it.count })
        assertEquals(15, grouped[2024]?.sumOf { it.count })
        assertEquals(55, events.sumOf { it.count })
    }

    @Test
    fun test_pattern_lock_matching_logic() {
        val savedPattern = "0-1-2-5-8"
        val correctAttempt = "0-1-2-5-8"
        val wrongAttempt = "0-1-2-4-7"

        assertTrue(savedPattern == correctAttempt)
        assertFalse(savedPattern == wrongAttempt)
    }

    @Test
    fun test_score_calculation_boost_times_strike() {
        val boost = 250.0
        val strikes = 15
        val expectedScore = 3750.0 // 250.0 * 15
        val actualScore = Individual.calculateTotalScore(boost, strikes)
        assertEquals(expectedScore, actualScore, 0.001)

        val individual = Individual(
            id = 1,
            fullName = "Tariq",
            type = "M",
            length = 20.0,
            width = 10.0,
            size = 200.0,
            sScore = 80.0,
            fScore = 80.0,
            cScore = 80.0,
            boost = 242.0,
            strikeCount = 10
        )
        // individual.totalScore = 242.0 * 10 = 2420.0
        assertEquals(2420.0, individual.totalScore, 0.001)
    }
}
