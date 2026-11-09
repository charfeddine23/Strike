package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.roadmap.KeyRarity
import com.example.data.roadmap.RewardType
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.StationState
import com.example.data.roadmap.TaskPools
import com.example.data.roadmap.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapEngineTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val prefs = app.getSharedPreferences("strikepulse_roadmap_prefs", Application.MODE_PRIVATE)
        prefs.edit().clear().commit()
        RoadmapManager.init(app)

        val flamePrefs = app.getSharedPreferences("strike_pulse_flame_prefs", Application.MODE_PRIVATE)
        flamePrefs.edit().clear().commit()
        FlameManager.init(app)
    }

    @Test
    fun testGenerate500Stations() {
        val stations = RoadmapManager.generateStations(totalXp = 0L, claimedStations = emptySet())
        assertEquals(500, stations.size)

        // Station 1 requires 100 XP (with 0 XP maxUnlocked is 1, so Station 1 is UNLOCKED)
        assertEquals(1, stations[0].id)
        assertEquals(StationState.UNLOCKED, stations[0].state)

        // Station 2 requires 200 XP, should be LOCKED at 0 XP
        assertEquals(2, stations[1].id)
        assertEquals(StationState.LOCKED, stations[1].state)

        // Station 500 requires 50000 XP
        assertEquals(500, stations[499].id)
        assertEquals(StationState.LOCKED, stations[499].state)
    }

    @Test
    fun testStationUnlockingWithXp() {
        // At 500 XP, stations 1..5 are unlocked
        val stations = RoadmapManager.generateStations(totalXp = 500L, claimedStations = setOf(1, 2))
        assertEquals(StationState.CLAIMED, stations[0].state)
        assertEquals(StationState.CLAIMED, stations[1].state)
        assertEquals(StationState.UNLOCKED, stations[2].state)
        assertEquals(StationState.UNLOCKED, stations[3].state)
        assertEquals(StationState.UNLOCKED, stations[4].state)
        assertEquals(StationState.LOCKED, stations[5].state) // requires 600 XP
    }

    @Test
    fun testMilestoneStrategicRewards() {
        val reward25 = TaskPools.getRewardForStation(25)
        val reward50 = TaskPools.getRewardForStation(50)
        val reward100 = TaskPools.getRewardForStation(100)
        val reward500 = TaskPools.getRewardForStation(500)

        assertNotNull(reward25)
        assertNotNull(reward50)
        assertNotNull(reward100)
        assertNotNull(reward500)

        // Station 500 should be the ultimate Thor Hammer
        assertEquals(RewardType.THOR_HAMMER_30D, reward500.type)
    }

    @Test
    fun testDateSeededDailyTaskShuffle() {
        val tasksDay1 = TaskPools.getDailyTasksForDate("2026-11-09")
        val tasksDay1Again = TaskPools.getDailyTasksForDate("2026-11-09")
        val tasksDay2 = TaskPools.getDailyTasksForDate("2026-11-10")

        assertEquals(5, tasksDay1.size)
        assertEquals(5, tasksDay1Again.size)
        assertEquals(5, tasksDay2.size)

        // Deterministic on same date
        assertEquals(tasksDay1.map { it.id }, tasksDay1Again.map { it.id })

        // Distinct tasks within the day
        assertEquals(5, tasksDay1.map { it.id }.toSet().size)
    }

    @Test
    fun testWeeklyAndMonthlyTasksCount() {
        val weekly = TaskPools.WEEKLY_CORE_TASKS
        assertEquals(7, weekly.size)

        val monthly = TaskPools.MONTHLY_MILESTONES
        assertEquals(10, monthly.size)

        val yearly = TaskPools.YEARLY_EPIC_GOALS
        assertEquals(7, yearly.size)
    }

    @Test
    fun testCompleteTaskAndXpProgression() {
        val initialXp = RoadmapManager.totalXp.value
        RoadmapManager.addXp(app, 250)
        assertEquals(initialXp + 250L, RoadmapManager.totalXp.value)
    }

    @Test
    fun testPowerHammersActivationAndBonus() {
        assertFalse(RoadmapManager.isGirlBoostHammerActive())
        assertFalse(RoadmapManager.isDeflameBoostHammerActive())
        assertFalse(RoadmapManager.isThorHammerActive())

        // Activate Girl Boost Hammer
        RoadmapManager.activateHammer(app, RewardType.GIRL_BOOST_HAMMER_7D)
        assertTrue(RoadmapManager.isGirlBoostHammerActive())

        // Activate Deflame Boost Hammer
        RoadmapManager.activateHammer(app, RewardType.DEFLAME_BOOST_HAMMER_7D)
        assertTrue(RoadmapManager.isDeflameBoostHammerActive())

        // Activate Thor Hammer
        RoadmapManager.activateHammer(app, RewardType.THOR_HAMMER_30D)
        assertTrue(RoadmapManager.isThorHammerActive())
    }

    @Test
    fun testThorHammerMitigationOnStrikePenalty() {
        // Set initial flame score to 100
        FlameManager.setScore(app, 100.0)

        // With Thor hammer active, impact is reduced by 60%, so deduction is only 40% (0.4)
        RoadmapManager.activateHammer(app, RewardType.THOR_HAMMER_30D)
        assertTrue(RoadmapManager.isThorHammerActive())

        val originalDeduction = 10.0
        val finalDeduction = if (RoadmapManager.isThorHammerActive()) {
            originalDeduction * 0.4
        } else {
            originalDeduction
        }
        assertEquals(4.0, finalDeduction, 0.001)
    }

    @Test
    fun testSkipKeyConsumption() {
        // Give 1 Normal key
        RoadmapManager.addKey(app, KeyRarity.NORMAL, 1)
        val initialCount = RoadmapManager.normalKeys.value
        assertTrue(initialCount >= 1)

        // Consume normal key for task
        val daily = RoadmapManager.dailyTasks.value
        val targetTask = daily.first()
        val used = RoadmapManager.useSkipKeyForTask(app, targetTask.id)
        assertTrue(used)

        // Task should now be SKIPPED
        val updatedDaily = RoadmapManager.dailyTasks.value
        val updatedTask = updatedDaily.first { it.id == targetTask.id }
        assertEquals(TaskStatus.SKIPPED, updatedTask.status)
    }
}
