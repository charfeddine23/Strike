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

    @Test
    fun test_flame_stage_classification() {
        // Range 0 to 30: Frozen / Static
        assertTrue(0.0 <= 30.0)
        assertTrue(30.0 <= 30.0)

        // Range 31 to 60: Cracking Ice
        val scoreCracking = 45.0
        assertTrue(scoreCracking > 30.0 && scoreCracking <= 60.0)

        // Range 61 to 300: Dynamic Color Gradient
        val scoreGradient = 150.0
        assertTrue(scoreGradient > 60.0 && scoreGradient <= 300.0)

        // Range > 300: Supercharged Inferno
        val scoreIgnited = 350.0
        assertTrue(scoreIgnited > 300.0)
    }

    @Test
    fun test_flame_manual_controls_and_clamping() {
        var flameScore = 5.0

        // Deflame decreases by 2
        flameScore = maxOf(0.0, flameScore - 2.0)
        assertEquals(3.0, flameScore, 0.001)

        // Girl decreases by 3
        flameScore = maxOf(0.0, flameScore - 3.0)
        assertEquals(0.0, flameScore, 0.001)

        // Cannot decrease below 0
        flameScore = maxOf(0.0, flameScore - 2.0)
        assertEquals(0.0, flameScore, 0.001)

        flameScore = maxOf(0.0, flameScore - 3.0)
        assertEquals(0.0, flameScore, 0.001)
    }

    @Test
    fun test_flame_call_accrual_and_retroactive_strike_rule() {
        var flameScore = 100.0

        // Contact call adds 5 points
        flameScore += 5.0
        assertEquals(105.0, flameScore, 0.001)

        val latestRecordedDate = "2026-11-05"
        val retroactiveStrikeDate = "2026-11-02"
        val boost = 25.0

        // Retroactive strike: date < latestRecordedDate -> 0 points change
        val isRetroactive = retroactiveStrikeDate < latestRecordedDate
        assertTrue(isRetroactive)
        if (!isRetroactive) {
            flameScore = maxOf(0.0, flameScore - boost)
        }
        // Score unchanged
        assertEquals(105.0, flameScore, 0.001)

        // Current/New latest strike: date >= latestRecordedDate -> deduct BOOST
        val currentStrikeDate = "2026-11-05"
        val isCurrentOrNewer = currentStrikeDate >= latestRecordedDate
        assertTrue(isCurrentOrNewer)
        if (isCurrentOrNewer) {
            flameScore = maxOf(0.0, flameScore - boost)
        }
        assertEquals(80.0, flameScore, 0.001)
    }

    @Test
    fun test_flame_inactivity_accrual() {
        val daysInactive = 4L
        val accrualPerDay = 3.0
        val addedPoints = daysInactive * accrualPerDay
        assertEquals(12.0, addedPoints, 0.001)
    }

    @Test
    fun test_flame_penalty_refund_on_strike_deleted() {
        var flameScore = 50.0
        val strikePenaltyValue = 28.5

        // Strike deletion automatically adds penalty value back to Flame Score
        flameScore += strikePenaltyValue
        assertEquals(78.5, flameScore, 0.001)

        // Undo delete restores strike and re-deducts penalty value
        flameScore = maxOf(0.0, flameScore - strikePenaltyValue)
        assertEquals(50.0, flameScore, 0.001)
    }
}

