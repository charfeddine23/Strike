package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.model.StrikeEvent
import com.example.data.profile.ProfileManager
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.TaskStatus
import com.example.data.roadmap.XpCalculationEngine
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

        context.getSharedPreferences("strikepulse_roadmap_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("strikepulse_profile_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("strikepulse_xp_engine_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        ProfileManager.resetForTesting(context)
        XpCalculationEngine.resetForTesting(context)
        RoadmapManager.resetForTesting(context)
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

    @Test
    fun `test monthly girl and deflame presses award XP without mutating task step counters`() {
        val startDate = getDateDaysAgo(60) // 60 days = 2 months -> 180 inactivity pts
        val baselineXp = RoadmapManager.totalXp.value
        val baselineCompleted = RoadmapManager.completedTaskIds.value.size

        // 5 Girl/month over 60 days = 10 Girl presses (-30 pts flame, +500 XP)
        // 10 Deflame/month over 60 days = 20 Deflame presses (-60 pts flame, +400 XP)
        // Final Flame score = 180 - 0 - 30 - 60 = 90.0 pts
        val result = FlameManager.recalculateFromStartDate(
            context = context,
            startDateStr = startDate,
            strikeEvents = emptyList(),
            lang = "ar",
            girlPerMonth = 5,
            deflamePerMonth = 10
        )

        assertEquals(60, result.daysElapsed)
        assertEquals(180.0, result.inactivityPointsAdded, 0.001)
        assertEquals(10, result.girlPressesCount)
        assertEquals(30.0, result.girlPointsDeducted, 0.001)
        assertEquals(20, result.deflamePressesCount)
        assertEquals(60.0, result.deflamePointsDeducted, 0.001)
        assertEquals(90.0, result.resultingScore, 0.001)
        assertEquals(90.0, FlameManager.getScore(context), 0.001)

        // Monthly rates are saved
        assertEquals(5, FlameManager.getSavedGirlPerMonth(context))
        assertEquals(10, FlameManager.getSavedDeflamePerMonth(context))

        // Recorded in existing Flame logs with isRetroactiveEstimate = true & actionCount
        val logs = FlameManager.flameLogs.value
        assertEquals("START_DATE_RECALCULATION", logs[0].reason)
        val girlLog = logs.firstOrNull { it.reason == "GIRL" }
        val deflameLog = logs.firstOrNull { it.reason == "DEFLAME" }
        assertNotNull(girlLog)
        assertNotNull(deflameLog)
        assertEquals(-30.0, girlLog!!.change, 0.001)
        assertEquals(-60.0, deflameLog!!.change, 0.001)
        assertEquals(10, girlLog.actionCount)
        assertEquals(20, deflameLog.actionCount)
        assertTrue(girlLog.isRetroactiveEstimate)
        assertTrue(deflameLog.isRetroactiveEstimate)

        // Task step counters MUST NOT advance or complete from monthly retroactive estimates
        val allActiveTasks = RoadmapManager.dailyTasks.value +
            RoadmapManager.weeklyTasks.value +
            RoadmapManager.monthlyTasks.value +
            RoadmapManager.yearlyTasks.value
        assertTrue(allActiveTasks.all { it.currentCount == 0 && it.status == TaskStatus.PENDING })
        assertEquals(baselineCompleted, RoadmapManager.completedTaskIds.value.size)

        // Total XP from Girl (10 * 50 = 500) + Deflame (20 * 20 = 400) = 900 XP MUST count in progression
        val breakdown = XpCalculationEngine.syncRetroactiveTelemetry(
            context = context,
            trackingStartDate = startDate,
            flameLogs = logs,
            strikeEvents = emptyList(),
            individuals = emptyList(),
            recalculateExact = true
        )
        assertEquals(10, breakdown.girlCount)
        assertEquals(20, breakdown.deflameCount)
        assertEquals(500L, breakdown.girlXp)
        assertEquals(400L, breakdown.deflameXp)
        assertEquals(900L, breakdown.totalTelemetryXp)
        assertEquals(baselineXp + 900L, RoadmapManager.totalXp.value)

        // Re-running with updated rates replaces estimate logs cleanly without doubling XP
        FlameManager.recalculateFromStartDate(
            context = context,
            startDateStr = startDate,
            strikeEvents = emptyList(),
            lang = "ar",
            girlPerMonth = 2,
            deflamePerMonth = 0
        )
        val updatedBreakdown = XpCalculationEngine.syncRetroactiveTelemetry(
            context = context,
            trackingStartDate = startDate,
            flameLogs = FlameManager.flameLogs.value,
            strikeEvents = emptyList(),
            individuals = emptyList(),
            recalculateExact = true
        )
        assertEquals(4, updatedBreakdown.girlCount)
        assertEquals(0, updatedBreakdown.deflameCount)
        assertEquals(200L, updatedBreakdown.totalTelemetryXp)
        assertEquals(baselineXp + 200L, RoadmapManager.totalXp.value)

        // Total reset clears saved monthly rates
        FlameManager.resetAllLevelsToZero(context)
        assertEquals(0, FlameManager.getSavedGirlPerMonth(context))
        assertEquals(0, FlameManager.getSavedDeflamePerMonth(context))
    }

    @Test
    fun `test flame score exceeds 20000 without upper limit and persists accurately`() {
        FlameManager.setScore(context, 75000.0)
        assertEquals(75000.0, FlameManager.getScore(context), 0.001)
        assertEquals(75000.0, FlameManager.peakScore.value, 0.001)

        // Adding points beyond 20,000 continues cleanly
        FlameManager.addPoints(context, 50000.0, reason = "INACTIVITY", details = "Huge streak bonus")
        assertEquals(125000.0, FlameManager.getScore(context), 0.001)

        // Survives full reload from SharedPreferences
        FlameManager.resetForTesting(context)
        assertEquals(125000.0, FlameManager.getScore(context), 0.001)

        // Start-date recalculation over 10,000 days (~27.4 years -> 30,000 pts) exceeds 20,000 without clamp
        val longStartDate = getDateDaysAgo(10000)
        val result = FlameManager.recalculateFromStartDate(context, longStartDate, emptyList())
        assertEquals(30000.0, result.resultingScore, 0.001)
        assertEquals(30000.0, FlameManager.getScore(context), 0.001)

        // DeviationsManager handles > 20,000 scores smoothly at top Rank & Tier
        val rank = com.example.data.deviations.DeviationsManager.getLightRank(125000.0)
        val tier = com.example.data.deviations.DeviationsManager.getBiologicalTier(125000.0)
        assertEquals(20, rank.index)
        assertEquals(50, tier.index)
    }
}
