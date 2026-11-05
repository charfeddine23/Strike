package com.example

import com.example.ui.components.StrikerStatus
import com.example.ui.components.StrikerStatusEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test

class StrikerStatusTest {

    private val oneYearMillis = (365.25 * 24 * 60 * 60 * 1000).toLong()

    @Test
    fun testActiveStatus_whenLastStrikeWithinTwoYears() {
        val now = System.currentTimeMillis()
        // Strike recorded 6 months ago (0.5 years)
        val strikeTimeRecent = now - (0.5 * oneYearMillis).toLong()
        val status = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = strikeTimeRecent,
            strikeCount = 10
        )
        assertEquals(StrikerStatus.ACTIVE, status)

        // Strike recorded 1.5 years ago
        val strikeTime1_5Years = now - (1.5 * oneYearMillis).toLong()
        val status1_5 = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = strikeTime1_5Years,
            strikeCount = 5
        )
        assertEquals(StrikerStatus.ACTIVE, status1_5)

        // Strike recorded exactly 2 years minus 1 day
        val strikeTimeJustUnderTwoYears = now - (2 * oneYearMillis) + 86400000L
        val statusBoundary = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = strikeTimeJustUnderTwoYears,
            strikeCount = 1
        )
        assertEquals(StrikerStatus.ACTIVE, statusBoundary)
    }

    @Test
    fun testStandByStatus_whenLastStrikeBetweenTwoAndThreeYears() {
        val now = System.currentTimeMillis()
        // Strike recorded 2.5 years ago (> 2 years and <= 3 years)
        val strikeTime2_5Years = now - (2.5 * oneYearMillis).toLong()
        val status = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = strikeTime2_5Years,
            strikeCount = 15
        )
        assertEquals(StrikerStatus.STAND_BY, status)

        // Strike recorded 2 years + 1 day ago
        val strikeTimeJustOverTwoYears = now - (2 * oneYearMillis) - 86400000L
        val status2 = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = strikeTimeJustOverTwoYears,
            strikeCount = 8
        )
        assertEquals(StrikerStatus.STAND_BY, status2)
    }

    @Test
    fun testRetiredStatus_whenLastStrikeMoreThanThreeYears() {
        val now = System.currentTimeMillis()
        // Strike recorded 3.5 years ago (> 3 years)
        val strikeTime3_5Years = now - (3.5 * oneYearMillis).toLong()
        val status = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = strikeTime3_5Years,
            strikeCount = 40
        )
        assertEquals(StrikerStatus.RETIRED, status)

        // Strike recorded 5 years ago
        val strikeTime5Years = now - (5 * oneYearMillis).toLong()
        val status5Years = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = strikeTime5Years,
            strikeCount = 20
        )
        assertEquals(StrikerStatus.RETIRED, status5Years)
    }

    @Test
    fun testPriority_moreThanThreeYearsMustBeRetiredNotStandBy() {
        val now = System.currentTimeMillis()
        // When > 3 years, condition for > 2 years is also true, but priority must ensure RETIRED
        val strikeTime4Years = now - (4 * oneYearMillis).toLong()
        val status = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = strikeTime4Years,
            strikeCount = 25
        )
        assertEquals(StrikerStatus.RETIRED, status)
    }

    @Test
    fun testNoStrikes_evaluatesToRetired() {
        val status = StrikerStatusEvaluator.evaluate(
            lastStrikeTimestamp = null,
            strikeCount = 0,
            updatedAt = 0L
        )
        assertEquals(StrikerStatus.RETIRED, status)
    }
}
