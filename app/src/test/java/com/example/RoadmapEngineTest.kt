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
        RoadmapManager.resetForTesting(app)

        val flamePrefs = app.getSharedPreferences("strike_pulse_flame_prefs", Application.MODE_PRIVATE)
        flamePrefs.edit().clear().commit()
        FlameManager.resetForTesting(app)
    }

    @Test
    fun test1000TaskMatrixAndExactTotalXp() {
        val daily = TaskPools.ALL_DAILY_TASKS
        val weekly = TaskPools.ALL_WEEKLY_TASKS
        val monthly = TaskPools.ALL_MONTHLY_TASKS
        val everlasting = TaskPools.ALL_EVERLASTING_TASKS

        assertEquals(250, daily.size)
        assertEquals(250, weekly.size)
        assertEquals(250, monthly.size)
        assertEquals(250, everlasting.size)
        assertEquals(1000, TaskPools.ALL_1000_TASKS.size)

        // Exact XP ranges
        assertEquals(50, daily.minOf { it.xpBounty })
        assertEquals(240, daily.maxOf { it.xpBounty })

        assertEquals(100, weekly.minOf { it.xpBounty })
        assertEquals(290, weekly.maxOf { it.xpBounty })

        assertEquals(200, monthly.minOf { it.xpBounty })
        assertEquals(635, monthly.maxOf { it.xpBounty })

        assertEquals(500, everlasting.minOf { it.xpBounty })
        assertEquals(1480, everlasting.maxOf { it.xpBounty })

        // Category sums
        assertEquals(36250, daily.sumOf { it.xpBounty })
        assertEquals(48750, weekly.sumOf { it.xpBounty })
        assertEquals(104375, monthly.sumOf { it.xpBounty })
        assertEquals(245350, everlasting.sumOf { it.xpBounty })

        // Grand Total XP Pool across all 1000 tasks must equal EXACTLY 434,725 XP
        val totalXp = TaskPools.ALL_1000_TASKS.sumOf { it.xpBounty.toLong() }
        assertEquals(434725L, totalXp)
        assertEquals(TaskPools.TOTAL_XP_POOL, totalXp)
    }

    @Test
    fun testExponentialRoadmapCurve() {
        val s1Xp = TaskPools.getRequiredXpForStation(1)
        val s500Xp = TaskPools.getRequiredXpForStation(500)

        // Station 1: 50 XP
        assertEquals(50L, s1Xp)

        // Station 500: exactly 434,725 XP (matches total XP pool)
        assertEquals(434725L, s500Xp)

        // Monotonic check across all 500 stations
        var prevXp = 0L
        for (s in 1..500) {
            val req = TaskPools.getRequiredXpForStation(s)
            assertTrue("Station $s required XP ($req) should be >= prev ($prevXp)", req >= prevXp)
            prevXp = req
        }
    }

    @Test
    fun testGenerate500Stations() {
        val stations = RoadmapManager.generateStations(totalXp = 50L, claimedStations = emptySet())
        assertEquals(500, stations.size)

        // Station 1 requires 50 XP, with 50 XP it is UNLOCKED
        assertEquals(1, stations[0].id)
        assertEquals(StationState.UNLOCKED, stations[0].state)

        // Station 2 requires 51 XP, should be LOCKED at 50 XP
        assertEquals(2, stations[1].id)
        assertEquals(StationState.LOCKED, stations[1].state)

        // Station 500 requires 434,725 XP
        assertEquals(500, stations[499].id)
        assertEquals(434725L, stations[499].requiredTotalXp)
        assertEquals(StationState.LOCKED, stations[499].state)
    }

    @Test
    fun testNonRepeatingConsumptionRotation() {
        val allDaily = TaskPools.ALL_DAILY_TASKS
        val firstBatch = TaskPools.getRotatedDailyTasks(emptySet(), "2026-11-11")
        assertEquals(5, firstBatch.size)

        // Simulate completing the first task
        val completedId = firstBatch[0].id
        val secondBatch = TaskPools.getRotatedDailyTasks(setOf(completedId), "2026-11-11")
        assertEquals(5, secondBatch.size)

        // The completed task MUST NEVER appear in subsequent rotations
        assertFalse(secondBatch.any { it.id == completedId })
    }

    @Test
    fun testEverlastingMasterGoalsDisplayAll250() {
        val completedSet = setOf("everlasting_1", "everlasting_5")
        val goals = TaskPools.getEverlastingTasks(completedSet)

        assertEquals(250, goals.size)
        val task1 = goals.first { it.id == "everlasting_1" }
        assertEquals(TaskStatus.COMPLETED, task1.status)

        val task2 = goals.first { it.id == "everlasting_2" }
        assertEquals(TaskStatus.PENDING, task2.status)
    }

    @Test
    fun testMilestoneStrategicRewards() {
        val reward20 = TaskPools.getRewardForStation(20)
        val reward100 = TaskPools.getRewardForStation(100)
        val reward500 = TaskPools.getRewardForStation(500)

        assertNotNull(reward20)
        assertNotNull(reward100)
        assertNotNull(reward500)

        // Station 20 and 100 have extra legendary rewards
        assertEquals(RewardType.KEY_LEGENDARY, reward20.type)
        assertEquals(RewardType.KEY_LEGENDARY, reward100.type)

        // Station 500 should be the ultimate Tier 5 Eternal Rebirth Elixir
        assertEquals(RewardType.POTION_ETERNAL_REBIRTH, reward500.type)
        assertEquals(1, reward500.quantity)
    }

    @Test
    fun testCompleteTaskAndXpProgression() {
        val initialXp = RoadmapManager.totalXp.value
        RoadmapManager.addXp(app, 250)
        assertEquals(initialXp + 250L, RoadmapManager.totalXp.value)
    }

    @Test
    fun test1000TasksMatrixDistribution() {
        assertEquals(250, TaskPools.ALL_DAILY_TASKS.size)
        assertEquals(250, TaskPools.ALL_WEEKLY_TASKS.size)
        assertEquals(250, TaskPools.ALL_MONTHLY_TASKS.size)
        assertEquals(250, TaskPools.ALL_EVERLASTING_TASKS.size)
        assertEquals(1000, TaskPools.ALL_1000_TASKS.size)
    }

    @Test
    fun testAnnualTasksStrictlyDisallowSkippingWithKeys() {
        val everlastingTasks = RoadmapManager.yearlyTasks.value
        assertFalse(everlastingTasks.isEmpty())

        for (task in everlastingTasks) {
            assertFalse("Annual tasks must have canBeSkipped == false", task.canBeSkipped)
        }

        // Give player abundant keys of all rarities
        RoadmapManager.addKey(app, KeyRarity.COMMON, 10)
        RoadmapManager.addKey(app, KeyRarity.NORMAL, 10)
        RoadmapManager.addKey(app, KeyRarity.RARE, 10)
        RoadmapManager.addKey(app, KeyRarity.LEGENDARY, 10)

        val targetAnnualTask = everlastingTasks.first()
        val skipAttempt = RoadmapManager.useSkipKeyForTask(app, targetAnnualTask.id)

        assertFalse("Skipping an annual task using keys must be strictly forbidden", skipAttempt)
        assertEquals(TaskStatus.PENDING, targetAnnualTask.status)
    }

    @Test
    fun testInitialInventoryIsZero() {
        assertEquals(0, RoadmapManager.commonKeys.value)
        assertEquals(0, RoadmapManager.normalKeys.value)
        assertEquals(0, RoadmapManager.rareKeys.value)
        assertEquals(0, RoadmapManager.legendaryKeys.value)

        assertEquals(0, RoadmapManager.shields24h.value)
        assertEquals(0, RoadmapManager.shields48h.value)
        assertEquals(0, RoadmapManager.shields72h.value)
        assertEquals(0, FlameManager.available1DayShields.value)
        assertEquals(0, FlameManager.availableManualShields.value)
    }

    @Test
    fun testSkipKeyConsumption() {
        val daily = RoadmapManager.dailyTasks.value
        val targetTask = daily.first()

        RoadmapManager.addKey(app, targetTask.requiredKeyRarity, 1)

        val used = RoadmapManager.useSkipKeyForTask(app, targetTask.id)
        assertTrue(used)

        val updatedDaily = RoadmapManager.dailyTasks.value
        val updatedTask = updatedDaily.first { it.id == targetTask.id }
        assertEquals(TaskStatus.SKIPPED, updatedTask.status)
        assertTrue(RoadmapManager.completedTaskIds.value.contains(targetTask.id))
    }

    @Test
    fun testHardResetAllTaskCompletionStates() {
        val dailyTask = RoadmapManager.dailyTasks.value.first()
        val weeklyTask = RoadmapManager.weeklyTasks.value.first()

        // Mark tasks completed / skipped
        RoadmapManager.addKey(app, dailyTask.requiredKeyRarity, 2)
        RoadmapManager.addKey(app, weeklyTask.requiredKeyRarity, 2)
        assertTrue(RoadmapManager.useSkipKeyForTask(app, dailyTask.id))
        assertTrue(RoadmapManager.useSkipKeyForTask(app, weeklyTask.id))
        assertFalse(RoadmapManager.completedTaskIds.value.isEmpty())

        // Execute 5s Profile long-press hard reset
        RoadmapManager.hardResetAllTaskCompletionStates(app)

        assertTrue(RoadmapManager.completedTaskIds.value.isEmpty())
        assertTrue(RoadmapManager.dailyTasks.value.all { it.status == TaskStatus.PENDING && it.currentCount == 0 })
        assertTrue(RoadmapManager.weeklyTasks.value.all { it.status == TaskStatus.PENDING && it.currentCount == 0 })
        assertTrue(RoadmapManager.monthlyTasks.value.all { it.status == TaskStatus.PENDING && it.currentCount == 0 })
        assertTrue(RoadmapManager.yearlyTasks.value.all { it.status == TaskStatus.PENDING && it.currentCount == 0 })
    }

    @Test
    fun testDailyTasksEliminateImpossibleQuotasAndScaleLogically() {
        val daily = TaskPools.ALL_DAILY_TASKS
        assertEquals(250, daily.size)

        // All daily titles must be unique
        assertEquals(250, daily.map { it.title }.distinct().size)

        // No daily task asks to activate/spam multiple shields in 24h
        val shieldTasks = daily.filter { it.actionTag == "SHIELD" }
        assertTrue(shieldTasks.isNotEmpty())
        assertTrue(shieldTasks.all { it.targetCount == 1 })

        // No daily task asks to spam multiple skip-keys in 24h
        val keyTasks = daily.filter { it.actionTag == "KEY" }
        assertTrue(keyTasks.isNotEmpty())
        assertTrue(keyTasks.all { it.targetCount == 1 })

        // Daily strike tasks never exceed 3 strikes (avoiding 6+ EXTREME_OVERLOAD contradiction)
        val strikeTasks = daily.filter { it.actionTag == "STRIKE" }
        assertTrue(strikeTasks.isNotEmpty())
        assertTrue(strikeTasks.all { it.targetCount in 1..3 })
    }

    @Test
    fun testProgressiveDifficultyAndXpOrderingAcrossAllPools() {
        val pools = listOf(
            TaskPools.ALL_DAILY_TASKS,
            TaskPools.ALL_WEEKLY_TASKS,
            TaskPools.ALL_MONTHLY_TASKS,
            TaskPools.ALL_EVERLASTING_TASKS
        )
        for (pool in pools) {
            for (i in 1 until pool.size) {
                assertTrue(
                    "Difficulty tier must be non-decreasing at index $i",
                    pool[i].difficultyTier >= pool[i - 1].difficultyTier
                )
                assertTrue(
                    "XP bounty must be non-decreasing at index $i",
                    pool[i].xpBounty >= pool[i - 1].xpBounty
                )
            }
        }

        // Rotated slices must also appear in ascending progressive order
        val rotatedDaily = TaskPools.getRotatedDailyTasks(emptySet(), "2026-11-13")
        val rotatedWeekly = TaskPools.getRotatedWeeklyTasks(emptySet(), "2026_W46")
        val rotatedMonthly = TaskPools.getRotatedMonthlyTasks(emptySet(), "2026_M11")

        listOf(rotatedDaily, rotatedWeekly, rotatedMonthly).forEach { slice ->
            for (i in 1 until slice.size) {
                assertTrue(slice[i].difficultyTier >= slice[i - 1].difficultyTier)
                if (slice[i].difficultyTier == slice[i - 1].difficultyTier) {
                    assertTrue(slice[i].xpBounty >= slice[i - 1].xpBounty)
                }
            }
        }
    }

    @Test
    fun testActionTagExactIncrementPreventsFalseMatches() {
        RoadmapManager.onActionOccurred(app, "STRIKE")
        val allActive = RoadmapManager.dailyTasks.value +
            RoadmapManager.weeklyTasks.value +
            RoadmapManager.monthlyTasks.value

        allActive.forEach { task ->
            if (task.actionTag == "STRIKE") {
                assertTrue(
                    "STRIKE task ${task.id} should have incremented",
                    task.currentCount >= 1 || task.isCompleted
                )
            } else {
                assertEquals(
                    "Non-STRIKE task ${task.id} (${task.actionTag}) must NOT falsely increment on STRIKE",
                    0,
                    task.currentCount
                )
            }
        }
    }
}
