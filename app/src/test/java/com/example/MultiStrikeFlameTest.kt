package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.ActiveShieldType
import com.example.data.flame.FlameManager
import com.example.data.model.StrikeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiStrikeFlameTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        FlameManager.init(context)
        FlameManager.setScore(context, 1000.0, "TEST_RESET")
        FlameManager.setStreakShieldActive(context, false)
        FlameManager.clearLogs(context)
    }

    @Test
    fun `verify double strike penalty rule and 3-day manual shield grant`() {
        // Initial score 1000.0
        val strikeDate = "2026-11-08"
        val event1 = StrikeEvent(
            id = 1L,
            individualId = 1L,
            individualName = "Target A",
            date = strikeDate,
            performanceValue = 20.0
        )
        val event2 = StrikeEvent(
            id = 2L,
            individualId = 2L,
            individualName = "Target B",
            date = strikeDate,
            performanceValue = 30.0
        )

        val initialManualShields = FlameManager.availableManualShields.value

        // When 2 strikes are registered on the exact same day
        val result = FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = strikeDate,
            boost = 30.0,
            personName = "Target B",
            latestDateBeforeInsert = "2026-11-07",
            strikesToday = listOf(event1, event2)
        )

        // Expected penalty: (20.0 + 30.0) * 3 = 150.0
        assertEquals(150.0, result.penaltyDeducted, 0.001)
        assertEquals("DOUBLE_STRIKE", result.ruleApplied)
        assertEquals("3_DAYS_MANUAL", result.shieldGranted)

        // Score should be 1000.0 - 150.0 = 850.0
        assertEquals(850.0, FlameManager.flameScore.value, 0.001)

        // Manual shield inventory should increase by 1
        assertEquals(initialManualShields + 1, FlameManager.availableManualShields.value)
    }

    @Test
    fun `verify multi strike combo penalty rule and 7-day auto shield grant`() {
        val strikeDate = "2026-11-08"
        val strikes = listOf(
            StrikeEvent(id = 1L, individualId = 1L, individualName = "A", date = strikeDate, performanceValue = 10.0),
            StrikeEvent(id = 2L, individualId = 2L, individualName = "B", date = strikeDate, performanceValue = 15.0),
            StrikeEvent(id = 3L, individualId = 3L, individualName = "C", date = strikeDate, performanceValue = 25.0)
        )

        val result = FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = strikeDate,
            boost = 25.0,
            personName = "C",
            latestDateBeforeInsert = "2026-11-07",
            strikesToday = strikes
        )

        // Expected: Sum(10 + 15 + 25 = 50) * 3 strikes = 150.0
        assertEquals(150.0, result.penaltyDeducted, 0.001)
        assertEquals("MULTI_STRIKE_COMBO", result.ruleApplied)
        assertEquals("7_DAYS_AUTO", result.shieldGranted)

        // Auto shield should now be active
        assertTrue(FlameManager.isShieldActiveNow())
        assertTrue(FlameManager.getActiveShieldRemainingMillis() > 0)
    }

    @Test
    fun `verify extreme overload reset rule on 6 or more strikes`() {
        val strikeDate = "2026-11-08"
        val strikes = (1..6).map {
            StrikeEvent(id = it.toLong(), individualId = it.toLong(), individualName = "Target $it", date = strikeDate, performanceValue = 10.0)
        }

        val result = FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = strikeDate,
            boost = 10.0,
            personName = "Target 6",
            latestDateBeforeInsert = "2026-11-07",
            strikesToday = strikes
        )

        assertEquals("EXTREME_OVERLOAD", result.ruleApplied)
        // Score should be completely reset to 0.0
        assertEquals(0.0, FlameManager.flameScore.value, 0.001)
    }

    @Test
    fun `verify strike deleted refund with effective penalty`() {
        FlameManager.setScore(context, 500.0, "TEST")
        val event = StrikeEvent(
            id = 55L,
            individualId = 1L,
            individualName = "Target A",
            date = "2026-11-08",
            performanceValue = 20.0,
            notes = "PENALTY:150.0|RULE:DOUBLE_STRIKE"
        )

        val penalty = event.getEffectivePenalty()
        assertEquals(150.0, penalty, 0.001)

        val newScore = FlameManager.onStrikeDeleted(
            context = context,
            penaltyValue = penalty,
            strikeDate = event.date,
            personName = event.individualName
        )

        // 500.0 + 150.0 = 650.0
        assertEquals(650.0, newScore, 0.001)
        assertEquals(650.0, FlameManager.flameScore.value, 0.001)
    }

    @Test
    fun `verify 1-day shield grant and activation`() {
        val initialCount = FlameManager.available1DayShields.value
        FlameManager.grant1DayShield(context, count = 2)
        assertEquals(initialCount + 2, FlameManager.available1DayShields.value)

        val success = FlameManager.activate1DayShield(context)
        assertTrue(success)
        assertEquals(initialCount + 1, FlameManager.available1DayShields.value)
        assertTrue(FlameManager.isShieldActiveNow())
        assertTrue(FlameManager.getActiveShieldRemainingMillis() > 0)
        assertEquals(ActiveShieldType.SHIELD_24H, FlameManager.activeShieldType.value)
    }

    @Test
    fun `verify activeShieldType tracking for 48h and 7-day auto shield counts`() {
        FlameManager.resetShieldsInventoryToZero(context)
        assertEquals(0, FlameManager.available48hShields.value)
        assertEquals(0, FlameManager.sevenDayAutoShieldCount.value)
        assertEquals(ActiveShieldType.NONE, FlameManager.activeShieldType.value)

        // 48h shield grant & activation
        FlameManager.grant48hShield(context, 1)
        assertEquals(1, FlameManager.available48hShields.value)
        assertTrue(FlameManager.activate48hShield(context))
        assertEquals(0, FlameManager.available48hShields.value)
        assertEquals(ActiveShieldType.SHIELD_48H, FlameManager.activeShieldType.value)

        // 72h manual shield grant & activation
        FlameManager.grantManualShield(context, 1)
        assertTrue(FlameManager.activateManualShield(context))
        assertEquals(ActiveShieldType.SHIELD_72H, FlameManager.activeShieldType.value)

        // 7-day auto shield trigger (3 strikes in one day)
        val strikeDate = "2026-11-12"
        val strikes = listOf(
            StrikeEvent(id = 11L, individualId = 1L, individualName = "A", date = strikeDate, performanceValue = 10.0),
            StrikeEvent(id = 12L, individualId = 2L, individualName = "B", date = strikeDate, performanceValue = 10.0),
            StrikeEvent(id = 13L, individualId = 3L, individualName = "C", date = strikeDate, performanceValue = 10.0)
        )
        FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = strikeDate,
            boost = 10.0,
            personName = "C",
            latestDateBeforeInsert = "2026-11-11",
            strikesToday = strikes
        )
        assertEquals(1, FlameManager.sevenDayAutoShieldCount.value)
        assertEquals(ActiveShieldType.SHIELD_7D_AUTO, FlameManager.activeShieldType.value)
    }
}
