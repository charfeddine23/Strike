package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.model.StrikeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class StreakStartDateRecalculationTest {

    private lateinit var context: Context
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("strikepulse_flame_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        FlameManager.resetForTesting(context)
    }

    private fun getDateDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateFormat.format(cal.time)
    }

    @Test
    fun `test recalculation with 30 elapsed days and no strikes`() {
        val startDate = getDateDaysAgo(30)
        val result = FlameManager.previewRecalculation(startDate, emptyList())

        assertEquals(30, result.daysElapsed)
        assertEquals(90.0, result.inactivityPointsAdded, 0.001) // 30 * 3.0
        assertEquals(0, result.totalStrikesCount)
        assertEquals(0.0, result.totalDeductions, 0.001)
        assertEquals(90.0, result.resultingScore, 0.001)
    }

    @Test
    fun `test recalculation with strikes inside and outside range`() {
        val startDate = getDateDaysAgo(30)
        val today = dateFormat.format(Date())

        val strikes = listOf(
            // Strike 45 days ago (before start date -> MUST BE IGNORED)
            StrikeEvent(
                id = 1,
                individualId = 10,
                individualName = "Target Alpha",
                date = getDateDaysAgo(45),
                year = 2026,
                count = 1,
                performanceValue = 12.0
            ),
            // Strike 10 days ago (single strike -> -8.0 pts)
            StrikeEvent(
                id = 2,
                individualId = 11,
                individualName = "Target Beta",
                date = getDateDaysAgo(10),
                year = 2026,
                count = 1,
                performanceValue = 8.0
            ),
            // Double strike 5 days ago (2 strikes on same date -> (5.0 + 7.0) * 3 = 36.0 pts)
            StrikeEvent(
                id = 3,
                individualId = 12,
                individualName = "Target Gamma",
                date = getDateDaysAgo(5),
                year = 2026,
                count = 1,
                performanceValue = 5.0
            ),
            StrikeEvent(
                id = 4,
                individualId = 13,
                individualName = "Target Delta",
                date = getDateDaysAgo(5),
                year = 2026,
                count = 1,
                performanceValue = 7.0
            )
        )

        val result = FlameManager.recalculateFromStartDate(context, startDate, strikes)

        assertEquals(30, result.daysElapsed)
        assertEquals(90.0, result.inactivityPointsAdded, 0.001)
        assertEquals(3, result.totalStrikesCount) // Strikes 2, 3, and 4
        // Total deductions = 8.0 (single) + (5.0 + 7.0) * 3.0 (double) = 8.0 + 36.0 = 44.0
        assertEquals(44.0, result.totalDeductions, 0.001)
        // Resulting score = 90.0 - 44.0 = 46.0
        assertEquals(46.0, result.resultingScore, 0.001)

        // Verify score is persisted in FlameManager
        assertEquals(46.0, FlameManager.getScore(context), 0.001)
        assertEquals(startDate, FlameManager.getStreakStartDate(context))

        // Verify log entry was created
        val logs = FlameManager.flameLogs.value
        assertTrue(logs.isNotEmpty())
        assertEquals("START_DATE_RECALCULATION", logs[0].reason)
        assertEquals("إعادة حساب تاريخ بداية العد", logs[0].title)
    }

    @Test
    fun `test recalculation clamped at zero if deductions exceed inactivity points`() {
        val startDate = getDateDaysAgo(10) // 10 days = 30 pts

        val strikes = listOf(
            // Triple strike 2 days ago: (10 + 10 + 10) * 3 = 90 pts deduction
            StrikeEvent(id = 1, individualId = 1, individualName = "P1", date = getDateDaysAgo(2), year = 2026, count = 1, performanceValue = 10.0),
            StrikeEvent(id = 2, individualId = 2, individualName = "P2", date = getDateDaysAgo(2), year = 2026, count = 1, performanceValue = 10.0),
            StrikeEvent(id = 3, individualId = 3, individualName = "P3", date = getDateDaysAgo(2), year = 2026, count = 1, performanceValue = 10.0)
        )

        val result = FlameManager.recalculateFromStartDate(context, startDate, strikes)
        assertEquals(10, result.daysElapsed)
        assertEquals(30.0, result.inactivityPointsAdded, 0.001)
        assertEquals(90.0, result.totalDeductions, 0.001)
        assertEquals(0.0, result.resultingScore, 0.001) // Clamped to 0.0 minimum
        assertEquals(0.0, FlameManager.getScore(context), 0.001)
    }
}
