package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.ActiveShieldType
import com.example.data.flame.FlameManager
import com.example.data.model.StrikeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiStrikeFlameTest {

    private lateinit var context: Context
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun todayDateStr(): String = dateFormat.format(Date())

    private fun offsetDateStr(daysOffset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return dateFormat.format(cal.time)
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        FlameManager.init(context)
        FlameManager.setScore(context, 1000.0, "TEST_RESET")
        FlameManager.setStreakShieldActive(context, false)
        FlameManager.clearLogs(context)
    }

    @Test
    fun `verify double strike penalty rule and 3-day manual shield grant on live today`() {
        val strikeDate = todayDateStr()
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

        val result = FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = strikeDate,
            boost = 30.0,
            personName = "Target B",
            latestDateBeforeInsert = offsetDateStr(-1),
            strikesToday = listOf(event1, event2)
        )

        // Expected penalty on live today: (20.0 + 30.0) * 3 = 150.0
        assertEquals(150.0, result.penaltyDeducted, 0.001)
        assertEquals("DOUBLE_STRIKE", result.ruleApplied)
        assertEquals("3_DAYS_MANUAL", result.shieldGranted)
        assertEquals(850.0, FlameManager.flameScore.value, 0.001)
        assertEquals(initialManualShields + 1, FlameManager.availableManualShields.value)
    }

    @Test
    fun `verify multi strike combo penalty rule and 7-day auto shield grant on live today`() {
        val strikeDate = todayDateStr()
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
            latestDateBeforeInsert = offsetDateStr(-1),
            strikesToday = strikes
        )

        // Expected on live today: Sum(10 + 15 + 25 = 50) * 3 strikes = 150.0
        assertEquals(150.0, result.penaltyDeducted, 0.001)
        assertEquals("MULTI_STRIKE_COMBO", result.ruleApplied)
        assertEquals("7_DAYS_AUTO", result.shieldGranted)
        assertTrue(FlameManager.isShieldActiveNow())
        assertTrue(FlameManager.getActiveShieldRemainingMillis() > 0)
    }

    @Test
    fun `verify extreme overload reset rule on 6 or more strikes on live today`() {
        val strikeDate = todayDateStr()
        val strikes = (1..6).map {
            StrikeEvent(id = it.toLong(), individualId = it.toLong(), individualName = "Target $it", date = strikeDate, performanceValue = 10.0)
        }

        val result = FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = strikeDate,
            boost = 10.0,
            personName = "Target 6",
            latestDateBeforeInsert = offsetDateStr(-1),
            strikesToday = strikes
        )

        assertEquals("EXTREME_OVERLOAD", result.ruleApplied)
        assertEquals(0.0, FlameManager.flameScore.value, 0.001)
    }

    @Test
    fun `past and future dates strictly isolated from double strike combo and overload`() {
        val pastDate = offsetDateStr(-5)
        val futureDate = offsetDateStr(5)
        val initialShields = FlameManager.availableManualShields.value
        val initialAutoCount = FlameManager.sevenDayAutoShieldCount.value

        // 1. Past date with 2, 3, and 6 strikes (even with latestDateBeforeInsert = null) MUST NOT trigger combo/overload
        val pastOverloadStrikes = (1..6).map {
            StrikeEvent(id = it.toLong(), individualId = it.toLong(), individualName = "Past $it", date = pastDate, performanceValue = 25.0)
        }
        val pastResult = FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = pastDate,
            boost = 25.0,
            personName = "Past 6",
            latestDateBeforeInsert = null,
            strikesToday = pastOverloadStrikes
        )
        assertEquals("RETROACTIVE", pastResult.ruleApplied)
        assertEquals(0.0, pastResult.penaltyDeducted, 0.001)
        assertEquals(1000.0, FlameManager.flameScore.value, 0.001)

        // 2. Future date with 2 or 6 strikes MUST NOT trigger double strike, combo, or overload
        val futureDoubleStrikes = listOf(
            StrikeEvent(id = 101L, individualId = 1L, individualName = "Future A", date = futureDate, performanceValue = 20.0),
            StrikeEvent(id = 102L, individualId = 2L, individualName = "Future B", date = futureDate, performanceValue = 30.0)
        )
        val futureResult = FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = futureDate,
            boost = 30.0,
            personName = "Future B",
            latestDateBeforeInsert = null,
            strikesToday = futureDoubleStrikes
        )
        assertEquals("RETROACTIVE", futureResult.ruleApplied)
        assertEquals(0.0, futureResult.penaltyDeducted, 0.001)
        assertEquals(1000.0, FlameManager.flameScore.value, 0.001)
        assertEquals(initialShields, FlameManager.availableManualShields.value)
        assertEquals(initialAutoCount, FlameManager.sevenDayAutoShieldCount.value)
        assertFalse(FlameManager.isShieldActiveNow())
    }

    @Test
    fun `historical strikes passed alongside live today strike are ignored by combo counter`() {
        val today = todayDateStr()
        val yesterday = offsetDateStr(-1)
        val mixedStrikes = listOf(
            StrikeEvent(id = 1L, individualId = 1L, individualName = "Live", date = today, strikeType = "Strike +1", performanceValue = 20.0),
            StrikeEvent(id = 2L, individualId = 1L, individualName = "Historical", date = today, strikeType = "Historical Entry", performanceValue = 50.0),
            StrikeEvent(id = 3L, individualId = 2L, individualName = "Yesterday", date = yesterday, strikeType = "Strike +1", performanceValue = 40.0)
        )

        val result = FlameManager.onStrikeRegistered(
            context = context,
            strikeDate = today,
            boost = 20.0,
            personName = "Live",
            latestDateBeforeInsert = yesterday,
            strikesToday = mixedStrikes
        )

        assertEquals("SINGLE_STRIKE", result.ruleApplied)
        assertEquals(20.0, result.penaltyDeducted, 0.001)
        assertEquals(980.0, FlameManager.flameScore.value, 0.001)
    }

    @Test
    fun `verify strike deleted refund with effective penalty`() {
        FlameManager.setScore(context, 500.0, "TEST")
        val event = StrikeEvent(
            id = 55L,
            individualId = 1L,
            individualName = "Target A",
            date = todayDateStr(),
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

        FlameManager.grant48hShield(context, 1)
        assertEquals(1, FlameManager.available48hShields.value)
        assertTrue(FlameManager.activate48hShield(context))
        assertEquals(0, FlameManager.available48hShields.value)
        assertEquals(ActiveShieldType.SHIELD_48H, FlameManager.activeShieldType.value)

        FlameManager.grantManualShield(context, 1)
        assertTrue(FlameManager.activateManualShield(context))
        assertEquals(ActiveShieldType.SHIELD_72H, FlameManager.activeShieldType.value)

        // 7-day auto shield trigger strictly requires 3..5 strikes on LIVE TODAY
        val strikeDate = todayDateStr()
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
            latestDateBeforeInsert = offsetDateStr(-1),
            strikesToday = strikes
        )
        assertEquals(1, FlameManager.sevenDayAutoShieldCount.value)
        assertEquals(ActiveShieldType.SHIELD_7D_AUTO, FlameManager.activeShieldType.value)
    }
}
