package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameLogEntry
import com.example.data.flame.FlameManager
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.profile.ProfileManager
import com.example.data.profile.SkipKeyType
import com.example.data.roadmap.RewardType
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.StationState
import com.example.data.roadmap.TaskPools
import com.example.data.roadmap.TaskStatus
import com.example.data.roadmap.XpCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

        val flamePrefs = app.getSharedPreferences("strikepulse_flame_prefs", Application.MODE_PRIVATE)
        flamePrefs.edit().clear().commit()
        FlameManager.resetForTesting(app)

        val profilePrefs = app.getSharedPreferences("strikepulse_profile_prefs", Application.MODE_PRIVATE)
        profilePrefs.edit().clear().commit()
        ProfileManager.resetForTesting(app)

        val xpEnginePrefs = app.getSharedPreferences("strikepulse_xp_engine_prefs", Application.MODE_PRIVATE)
        xpEnginePrefs.edit().clear().commit()
        XpCalculationEngine.resetForTesting(app)

        RoadmapManager.resetForTesting(app)
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
        val s700Xp = TaskPools.getRequiredXpForStation(700)

        // Station 1: exactly 1,345 XP
        assertEquals(1345L, s1Xp)

        // Station 500: exactly 1,000,000 XP
        assertEquals(1_000_000L, s500Xp)

        // Station 700 (Frozen Abyss Cap): exactly 20,000,000 XP
        assertEquals(20_000_000L, s700Xp)

        // Strictly increasing smooth difficulty progression across all 700 stations
        var prevXp = 0L
        var prevStepDelta = 0L
        for (s in 1..700) {
            val req = TaskPools.getRequiredXpForStation(s)
            assertTrue("Station $s required XP ($req) must strictly exceed prev ($prevXp)", req > prevXp)
            if (s > 1) {
                val delta = req - prevXp
                assertTrue("Step delta ($delta) at station $s should rise smoothly from prior ($prevStepDelta)", delta >= prevStepDelta - 1L)
                prevStepDelta = delta
            }
            prevXp = req
        }
    }

    @Test
    fun testGenerate700Stations() {
        val stations = RoadmapManager.generateStations(totalXp = 1345L, claimedStations = emptySet())
        assertEquals(700, stations.size)

        // Station 1 requires 1,345 XP, with 1,345 XP it is UNLOCKED
        assertEquals(1, stations[0].id)
        assertEquals(1345L, stations[0].requiredTotalXp)
        assertEquals(StationState.UNLOCKED, stations[0].state)

        // Station 2 requires > 1,345 XP, should be LOCKED at 1,345 XP
        assertEquals(2, stations[1].id)
        assertTrue(stations[1].requiredTotalXp > 1345L)
        assertEquals(StationState.LOCKED, stations[1].state)

        // Station 500 requires 1,000,000 XP
        assertEquals(500, stations[499].id)
        assertEquals(1_000_000L, stations[499].requiredTotalXp)
        assertEquals(StationState.LOCKED, stations[499].state)

        // Station 501 enters Frozen Zone (> 1,000,000 XP)
        assertEquals(501, stations[500].id)
        assertTrue(stations[500].requiredTotalXp > 1_000_000L)

        // Station 700 reaches 20,000,000 XP ceiling
        assertEquals(700, stations[699].id)
        assertEquals(20_000_000L, stations[699].requiredTotalXp)
        assertEquals(StationState.LOCKED, stations[699].state)
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
    fun testEverlastingMasterGoalsTransferOutUponCompletion() {
        val completedSet = setOf("everlasting_1", "everlasting_5")
        val activeGoals = TaskPools.getEverlastingTasks(completedSet)

        // Claimed Eternal tasks transfer out of the active Eternal list into Completed Tasks Ledger
        assertEquals(248, activeGoals.size)
        assertFalse(activeGoals.any { it.id == "everlasting_1" || it.id == "everlasting_5" })

        val task2 = activeGoals.first { it.id == "everlasting_2" }
        assertEquals(TaskStatus.PENDING, task2.status)
    }

    @Test
    fun testMilestoneStrategicRewards() {
        val reward20 = TaskPools.getRewardForStation(20)
        val reward100 = TaskPools.getRewardForStation(100)
        val reward500 = TaskPools.getRewardForStation(500)
        val reward600 = TaskPools.getRewardForStation(600)
        val reward700 = TaskPools.getRewardForStation(700)

        assertNotNull(reward20)
        assertNotNull(reward100)
        assertNotNull(reward500)
        assertNotNull(reward600)
        assertNotNull(reward700)

        // Station 20 and 100 have extra Silver Skip-Key rewards
        assertEquals(RewardType.KEY_SILVER, reward20.type)
        assertEquals(RewardType.KEY_SILVER, reward100.type)

        // Station 500 preserves Eternal Rebirth Elixir (1x)
        assertEquals(RewardType.POTION_ETERNAL_REBIRTH, reward500.type)
        assertEquals(1, reward500.quantity)

        // Glacial milestone rewards (600 & 700)
        assertEquals(RewardType.POTION_CRYO_INFUSION, reward600.type)
        assertEquals(3, reward600.quantity)
        assertEquals(RewardType.POTION_ETERNAL_REBIRTH, reward700.type)
        assertEquals(2, reward700.quantity)
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

        // Give player abundant Inventory skip-keys of all tiers
        RoadmapManager.addKey(app, SkipKeyType.WOODEN, 10)
        RoadmapManager.addKey(app, SkipKeyType.BRONZE, 10)
        RoadmapManager.addKey(app, SkipKeyType.SILVER, 10)

        val targetAnnualTask = everlastingTasks.first()
        val skipAttempt = RoadmapManager.useSkipKeyForTask(app, targetAnnualTask.id)

        assertFalse("Skipping an annual task using keys must be strictly forbidden", skipAttempt)
        assertEquals(TaskStatus.PENDING, targetAnnualTask.status)
    }

    @Test
    fun testInitialInventoryIsZero() {
        assertEquals(0, RoadmapManager.woodenKeysCount.value)
        assertEquals(0, RoadmapManager.bronzeKeysCount.value)
        assertEquals(0, RoadmapManager.silverKeysCount.value)
        assertEquals(0, ProfileManager.woodenKeysCount.value)
        assertEquals(0, ProfileManager.bronzeKeysCount.value)
        assertEquals(0, ProfileManager.silverKeysCount.value)

        assertEquals(0, RoadmapManager.shields24h.value)
        assertEquals(0, RoadmapManager.shields48h.value)
        assertEquals(0, RoadmapManager.shields72h.value)
        assertEquals(0, FlameManager.available1DayShields.value)
        assertEquals(0, FlameManager.availableManualShields.value)
    }

    @Test
    fun testSkipKeyConsumptionQueuesLockedReplacement() {
        val daily = RoadmapManager.dailyTasks.value
        val targetTask = daily.first()
        val now = System.currentTimeMillis()

        RoadmapManager.addKey(app, targetTask.requiredSkipKey, 1)

        val used = RoadmapManager.useSkipKeyForTask(app, targetTask.id)
        assertTrue(used)

        val remainingCount = when (targetTask.requiredSkipKey) {
            SkipKeyType.WOODEN -> ProfileManager.woodenKeysCount.value
            SkipKeyType.BRONZE -> ProfileManager.bronzeKeysCount.value
            SkipKeyType.SILVER -> ProfileManager.silverKeysCount.value
        }
        assertEquals(0, remainingCount)

        val updatedDaily = RoadmapManager.dailyTasks.value
        assertEquals(5, updatedDaily.size)
        assertFalse("Skipped Daily task must disappear immediately from active list", updatedDaily.any { it.id == targetTask.id })
        assertTrue(RoadmapManager.completedTaskIds.value.contains(targetTask.id))

        val lockedReplacement = updatedDaily.firstOrNull { it.isLocked(now) }
        assertNotNull("Skipped slot should be replaced by a task locked until midnight", lockedReplacement)
        assertFalse("Locked replacement cannot be skipped before midnight unlock", RoadmapManager.useSkipKeyForTask(app, lockedReplacement!!.id))
    }

    @Test
    fun testManualCollectHoldAndMidnightReplacementLifecycle() {
        val initialDaily = RoadmapManager.dailyTasks.value
        assertEquals(5, initialDaily.size)
        val xpBefore = RoadmapManager.totalXp.value

        // Trigger an action that completes at least one 1-step daily task (e.g. SHIELD or DEFLAME)
        val singleStepTask = initialDaily.firstOrNull { it.targetCount == 1 }
            ?: initialDaily.first().also { RoadmapManager.onActionOccurred(app, it.actionTag) }

        RoadmapManager.onActionOccurred(app, singleStepTask.actionTag)

        // Ensure single-step task reached READY_TO_CLAIM and stayed visible with 0 XP awarded yet
        val afterAction = RoadmapManager.dailyTasks.value
        val readyTask = afterAction.firstOrNull { it.id == singleStepTask.id && it.status == TaskStatus.READY_TO_CLAIM }
            ?: afterAction.firstOrNull { it.status == TaskStatus.READY_TO_CLAIM }

        if (readyTask != null) {
            assertEquals("XP MUST NOT be awarded before manual Collect click", xpBefore, RoadmapManager.totalXp.value)
            assertTrue("Ready task must stay visible until user clicks Collect", afterAction.any { it.id == readyTask.id })

            // Now tap Collect ("جمع")
            assertTrue(RoadmapManager.claimTaskReward(app, readyTask.id))
            assertEquals(xpBefore + readyTask.xpBounty, RoadmapManager.totalXp.value)
            assertTrue(RoadmapManager.completedTaskIds.value.contains(readyTask.id))
            assertTrue(RoadmapManager.completedTaskTimestamps.value.containsKey(readyTask.id))

            val afterClaim = RoadmapManager.dailyTasks.value
            assertEquals(5, afterClaim.size)
            assertFalse(afterClaim.any { it.id == readyTask.id })

            // Replacement task must be locked until next midnight and ignore actions until then
            val replacement = afterClaim.firstOrNull { it.isLocked() }
            assertNotNull("Claimed slot must draw a replacement waiting until 12:00 AM", replacement)
            RoadmapManager.onActionOccurred(app, replacement!!.actionTag)
            val afterLockedAction = RoadmapManager.dailyTasks.value.first { it.id == replacement.id }
            assertEquals("Locked replacement must not gain progress before unlock time", 0, afterLockedAction.currentCount)
        }
    }

    @Test
    fun testCarryOverPendingTasksAndDynamicRefillAcrossCycles() {
        val initialDaily = RoadmapManager.dailyTasks.value
        assertEquals(5, initialDaily.size)

        // 1. Complete 1 daily task and skip another
        val completedId = initialDaily.first().id
        assertTrue(RoadmapManager.completeTask(app, completedId))

        val skippedTask = RoadmapManager.dailyTasks.value.first { !it.isLocked() }
        RoadmapManager.addKey(app, skippedTask.requiredSkipKey, 1)
        assertTrue(RoadmapManager.useSkipKeyForTask(app, skippedTask.id))

        // Increment an unlocked pending multi-step task to verify partial progress carries over across cycles
        val partialCandidate = RoadmapManager.dailyTasks.value.firstOrNull { !it.isLocked() && it.targetCount > 1 }
        if (partialCandidate != null) {
            RoadmapManager.onActionOccurred(app, partialCandidate.actionTag)
        }

        val activeCarried = RoadmapManager.dailyTasks.value.filter { it.status == TaskStatus.PENDING || it.status == TaskStatus.READY_TO_CLAIM }
        assertEquals(5, activeCarried.size)
        val carriedCounts = activeCarried.associate { it.id to it.currentCount }

        // 2. Simulate next calendar day / week / month rollover
        val prefs = app.getSharedPreferences("strikepulse_roadmap_prefs", Application.MODE_PRIVATE)
        prefs.edit()
            .putString("key_last_daily_date", "2020-01-01")
            .putString("key_last_weekly_code", "SUN_2020-01-05")
            .putString("key_last_monthly_code", "2020_M01")
            .apply()

        RoadmapManager.resetForTesting(app)

        val nextDayDaily = RoadmapManager.dailyTasks.value
        assertEquals(5, nextDayDaily.size)
        carriedCounts.forEach { (cid, count) ->
            val reloaded = nextDayDaily.find { it.id == cid }
            assertNotNull("Carried-over task $cid must stay active across cycle", reloaded)
            assertEquals(count, reloaded!!.currentCount)
        }
        // Completed/skipped tasks must NEVER reappear
        assertFalse(nextDayDaily.any { it.id == completedId || it.id == skippedTask.id })
        // Weekly and Monthly capacities remain 7 and 10
        assertEquals(7, RoadmapManager.weeklyTasks.value.size)
        assertEquals(10, RoadmapManager.monthlyTasks.value.size)
    }

    @Test
    fun testEternalTasksHoldUntilClaimAndTransferToLedger() {
        val initialEternal = RoadmapManager.yearlyTasks.value
        assertEquals(250, initialEternal.size)
        assertTrue(initialEternal.all { it.status == TaskStatus.PENDING })

        // Pick a multi-step STRIKE task for partial persistence and a small POTION Eternal task (targetCount = 3)
        val multiStepStrike = initialEternal.first { it.actionTag == "STRIKE" && it.targetCount > 1 }
        val targetEternal = initialEternal.filter { it.actionTag == "POTION" }.minBy { it.targetCount }

        val xpStart = RoadmapManager.totalXp.value
        RoadmapManager.onActionOccurred(app, "STRIKE")
        repeat(targetEternal.targetCount) {
            RoadmapManager.onActionOccurred(app, "POTION")
        }

        // Completed Eternal task stays in yearlyTasks as READY_TO_CLAIM until user clicks Collect ("جمع")
        val heldTask = RoadmapManager.yearlyTasks.value.first { it.id == targetEternal.id }
        assertEquals(TaskStatus.READY_TO_CLAIM, heldTask.status)
        assertEquals(250, RoadmapManager.yearlyTasks.value.size)
        assertEquals(xpStart, RoadmapManager.totalXp.value)

        // Now user taps Collect ("جمع") -> XP awarded AND task transfers out of yearlyTasks into Ledger
        assertTrue(RoadmapManager.claimTaskReward(app, targetEternal.id))
        assertEquals(xpStart + targetEternal.xpBounty, RoadmapManager.totalXp.value)

        // Re-initialize from SharedPreferences JSON
        RoadmapManager.resetForTesting(app)

        val reloadedEternal = RoadmapManager.yearlyTasks.value
        assertEquals(249, reloadedEternal.size)
        assertFalse("Claimed Eternal task must leave active list upon Collect", reloadedEternal.any { it.id == targetEternal.id })

        // Partial progress on pending multi-step Eternal task persisted
        val reloadedStrike = reloadedEternal.first { it.id == multiStepStrike.id }
        assertEquals(1, reloadedStrike.currentCount)
        assertEquals(TaskStatus.PENDING, reloadedStrike.status)

        // Ledger snapshot contains ALL 1,000 tasks including completed Eternal task w/ timestamp and partial task progress
        val ledger = RoadmapManager.getTasksLedgerSnapshot()
        assertEquals(1000, ledger.size)
        val ledgerClaimed = ledger.first { it.task.id == targetEternal.id }
        assertEquals(TaskStatus.COMPLETED, ledgerClaimed.task.status)
        assertNotNull("Ledger entry must store completion timestamp", ledgerClaimed.completedAtMillis)
        assertEquals(ledgerClaimed.task.targetCount, ledgerClaimed.task.currentCount)

        val ledgerPartial = ledger.first { it.task.id == multiStepStrike.id }
        assertEquals(1, ledgerPartial.task.currentCount)
        assertEquals(TaskStatus.PENDING, ledgerPartial.task.status)
    }

    @Test
    fun testCycleGrantSchedulesDailyMidnightWeeklySundayMonthlyFirst() {
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 11, 15, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val sampleWednesday = cal.timeInMillis

        // Daily reset: Thursday Nov 12 at 00:00:00.000
        val dailyReset = java.util.Calendar.getInstance().apply {
            timeInMillis = TaskPools.nextDailyResetMillis(sampleWednesday)
        }
        assertEquals(12, dailyReset.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(0, dailyReset.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, dailyReset.get(java.util.Calendar.MINUTE))

        // Weekly reset: Upcoming Sunday Nov 15 at 00:00:00.000
        val weeklyReset = java.util.Calendar.getInstance().apply {
            timeInMillis = TaskPools.nextSundayResetMillis(sampleWednesday)
        }
        assertEquals(java.util.Calendar.SUNDAY, weeklyReset.get(java.util.Calendar.DAY_OF_WEEK))
        assertEquals(15, weeklyReset.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(0, weeklyReset.get(java.util.Calendar.HOUR_OF_DAY))

        // Week seed stays Sunday-anchored across week
        val sunWeek = TaskPools.currentWeekString(sampleWednesday)
        assertEquals("SUN_2026-11-08", sunWeek)

        // Monthly reset: December 1st at 00:00:00.000
        val monthlyReset = java.util.Calendar.getInstance().apply {
            timeInMillis = TaskPools.nextMonthlyResetMillis(sampleWednesday)
        }
        assertEquals(1, monthlyReset.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(java.util.Calendar.DECEMBER, monthlyReset.get(java.util.Calendar.MONTH))
        assertEquals(0, monthlyReset.get(java.util.Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testHardResetAllTaskCompletionStates() {
        val dailyTask = RoadmapManager.dailyTasks.value.first()
        val weeklyTask = RoadmapManager.weeklyTasks.value.first()

        // Mark tasks completed / skipped using Inventory skip keys
        RoadmapManager.addKey(app, dailyTask.requiredSkipKey, 2)
        RoadmapManager.addKey(app, weeklyTask.requiredSkipKey, 2)
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

    @Test
    fun testTotalGameResetLiterallyReturnsEverythingToZero() {
        // 1. Accumulate progress across Roadmap, Flame, and Profile
        RoadmapManager.addXp(app, 5000)
        assertNotNull(RoadmapManager.claimStationReward(app, 1))
        val dailyTask = RoadmapManager.dailyTasks.value.first()
        assertTrue(RoadmapManager.completeTask(app, dailyTask.id))

        FlameManager.setScore(app, 12500.0)
        FlameManager.grant1DayShield(app, 3)
        FlameManager.grant48hShield(app, 2)
        FlameManager.grantManualShield(app, days = 3)
        FlameManager.activate1DayShield(app)

        ProfileManager.grantSkipKeys(app, SkipKeyType.WOODEN, 5)
        ProfileManager.grantSkipKeys(app, SkipKeyType.BRONZE, 4)
        ProfileManager.grantSkipKeys(app, SkipKeyType.SILVER, 3)
        ProfileManager.grantPotion(app, com.example.data.profile.PotionType.PULSE_ELIXIR, 2)
        ProfileManager.grantPotion(app, com.example.data.profile.PotionType.SHADOW_GRIP_SERUM, 1)
        ProfileManager.usePulseElixir(app)
        ProfileManager.unlockAvatar(app, "av_gabriel_wings")
        ProfileManager.equipAvatar(app, "av_gabriel_wings")
        ProfileManager.unlockTitle(app, 21)
        ProfileManager.equipTitle(app, 21)

        // Sanity check that state is non-zero before reset
        assertTrue(RoadmapManager.totalXp.value > 0L)
        assertTrue(RoadmapManager.calculateUnlockedStationId(RoadmapManager.totalXp.value) > 0)
        assertFalse(RoadmapManager.claimedStations.value.isEmpty())
        assertFalse(RoadmapManager.completedTaskIds.value.isEmpty())
        assertTrue(FlameManager.flameScore.value > 0.0)
        assertTrue(FlameManager.isStreakShieldActive.value)

        // 2. Trigger Total Game Reset to Zero
        RoadmapManager.resetAllLevelsToZero(app)
        FlameManager.resetAllLevelsToZero(app)
        ProfileManager.resetAllProgressToZero(app)

        // 3. Assert literally EVERYTHING returned to zero / initial base
        assertEquals(0L, RoadmapManager.totalXp.value)
        assertEquals(0, RoadmapManager.calculateUnlockedStationId(RoadmapManager.totalXp.value))
        assertTrue(RoadmapManager.claimedStations.value.isEmpty())
        assertTrue(RoadmapManager.completedTaskIds.value.isEmpty())
        assertTrue(RoadmapManager.dailyTasks.value.all { it.status == TaskStatus.PENDING && it.currentCount == 0 })
        assertTrue(RoadmapManager.weeklyTasks.value.all { it.status == TaskStatus.PENDING && it.currentCount == 0 })
        assertTrue(RoadmapManager.monthlyTasks.value.all { it.status == TaskStatus.PENDING && it.currentCount == 0 })
        assertTrue(RoadmapManager.yearlyTasks.value.all { it.status == TaskStatus.PENDING && it.currentCount == 0 })
        assertEquals(0, RoadmapManager.shields24h.value)
        assertEquals(0, RoadmapManager.shields48h.value)
        assertEquals(0, RoadmapManager.shields72h.value)

        assertEquals(0.0, FlameManager.flameScore.value, 0.001)
        assertEquals(0.0, FlameManager.peakScore.value, 0.001)
        assertEquals(0.0, FlameManager.totalPointsGained.value, 0.001)
        assertEquals(0.0, FlameManager.totalPointsExtinguished.value, 0.001)
        assertTrue(FlameManager.flameLogs.value.isEmpty())
        assertFalse(FlameManager.isStreakShieldActive.value)
        assertEquals(0, FlameManager.available1DayShields.value)
        assertEquals(0, FlameManager.available48hShields.value)
        assertEquals(0, FlameManager.availableManualShields.value)
        assertEquals(0, FlameManager.sevenDayAutoShieldCount.value)

        assertEquals(0, ProfileManager.woodenKeysCount.value)
        assertEquals(0, ProfileManager.bronzeKeysCount.value)
        assertEquals(0, ProfileManager.silverKeysCount.value)
        assertEquals(0, ProfileManager.pulseElixirCount.value)
        assertEquals(0, ProfileManager.shadowSerumCount.value)
        assertEquals(0, ProfileManager.gabrielDropletCount.value)
        assertEquals(0, ProfileManager.cryoInfusionCount.value)
        assertEquals(0, ProfileManager.eternalRebirthCount.value)
        assertEquals(0, ProfileManager.pulseElixirUsesLeft.value)
        assertFalse(ProfileManager.hasCryoBuffArmed.value)
        assertFalse(ProfileManager.hasRebirthImmunity.value)

        // Reset All Levels to Zero clears & locks unlocked Titles and Avatars back to initial state
        assertEquals(setOf("av_core_node"), ProfileManager.unlockedAvatarIds.value)
        assertEquals(setOf(1), ProfileManager.unlockedTitleIds.value)
        assertEquals("av_core_node", ProfileManager.equippedAvatar.value.id)
        assertEquals(1, ProfileManager.equippedTitle.value.id)
    }

    @Test
    fun testWavySnakeTrajectoryZeroCenterCollisionsAndContinuity() {
        for (stationId in 1..700) {
            val nodeX = com.example.ui.components.wavyNodeNormalizedX(stationId.toFloat())
            // Nodes must always sit clearly on Left (<= 0.30) or Right (>= 0.70) so side glass cards never collide
            assertTrue(
                "Station $stationId nodeX=$nodeX too close to center",
                kotlin.math.abs(nodeX - 0.5f) >= 0.20f
            )
            assertTrue("Station $stationId nodeX=$nodeX out of safe bounds", nodeX in 0.14f..0.86f)

            if (stationId < 700) {
                val bottomOfRow = com.example.ui.components.wavyNodeNormalizedX(stationId + 0.5f)
                val topOfNextRow = com.example.ui.components.wavyNodeNormalizedX((stationId + 1) - 0.5f)
                assertEquals(bottomOfRow, topOfNextRow, 0.0001f)
            }
        }
    }

    @Test
    fun testGlacialDepthScalingAndSnapshotRestoreUpTo700() {
        // Level 1 starts submerged in glacial ice; 260 is Rocky, 440 is Forest, 700 is deeply submerged in volcanic magma
        assertEquals(1.0f, com.example.ui.components.glacialDepthForStation(1), 0.0001f)
        assertEquals(0.0f, com.example.ui.components.glacialDepthForStation(350), 0.0001f)
        assertTrue(com.example.ui.components.roadmapBiomeStateForStation(260f).rockIntensity > 0.95f)
        assertTrue(com.example.ui.components.roadmapBiomeStateForStation(440f).forestIntensity > 0.95f)
        assertEquals(1.0f, com.example.ui.components.roadmapBiomeStateForStation(700f).lavaSubmersion, 0.0001f)

        // Unlock all 700 stations using cumulative XP at station 700
        val maxCumulative = TaskPools.getCumulativeXpForStation(700)
        assertEquals(700, RoadmapManager.calculateUnlockedStationId(maxCumulative))
        val maxProg = RoadmapManager.getStageProgress(maxCumulative)
        assertEquals(700, maxProg.unlockedStationId)
        assertEquals(700, maxProg.nextStationId)
        assertEquals(1f, maxProg.progressFraction, 0.0001f)

        // Backup & Restore preserves claimed stations in frozen band 501..700
        RoadmapManager.addXp(app, maxCumulative)
        assertNotNull(RoadmapManager.claimStationReward(app, 550))
        assertNotNull(RoadmapManager.claimStationReward(app, 700))
        val snapshot = RoadmapManager.exportSnapshot(app)
        RoadmapManager.resetAllLevelsToZero(app)
        assertTrue(RoadmapManager.claimedStations.value.isEmpty())

        RoadmapManager.restoreSnapshot(app, snapshot, replaceExisting = true)
        assertTrue(RoadmapManager.claimedStations.value.contains(550))
        assertTrue(RoadmapManager.claimedStations.value.contains(700))
        assertEquals(700, RoadmapManager.calculateUnlockedStationId(RoadmapManager.totalXp.value))
    }

    @Test
    fun testRoadmapXpThresholdDoesNotAffectOrAddToTasksXp() {
        // 1. Fresh game must strictly start at 0 Tasks XP and Station 0 (Station 1 requires 50 XP from Tasks)
        assertEquals(0L, RoadmapManager.totalXp.value)
        assertEquals(0, RoadmapManager.calculateUnlockedStationId(RoadmapManager.totalXp.value))
        assertEquals(StationState.LOCKED, RoadmapManager.getStationState(1))
        assertEquals(null, RoadmapManager.claimStationReward(app, 1))

        // 2. Task catalog must contain ZERO circular "ROADMAP" tasks
        assertTrue(
            "TaskPools must not contain circular ROADMAP tasks",
            TaskPools.ALL_1000_TASKS.none { it.actionTag == "ROADMAP" }
        )

        // 3. Tasks XP is strictly the sole source of XP progression (Station 1 requires 1,345 XP, each stage resets from 0)
        var expectedTasksXp = 0L
        val targetUnlockXp = TaskPools.getCumulativeXpForStation(10)
        val annualCandidates = RoadmapManager.yearlyTasks.value.filter { it.status == TaskStatus.PENDING }
        for (task in annualCandidates) {
            if (expectedTasksXp >= targetUnlockXp) break
            assertTrue(RoadmapManager.completeTask(app, task.id))
            expectedTasksXp += task.xpBounty.toLong()
            assertEquals(expectedTasksXp, RoadmapManager.totalXp.value)
        }

        // Stations 1..10 unlocked purely from Tasks XP
        val unlockedCount = RoadmapManager.calculateUnlockedStationId(expectedTasksXp)
        assertTrue("Expected Stations 1..10 unlocked from Tasks XP ($expectedTasksXp), got $unlockedCount", unlockedCount >= 10)

        // 4. Claiming Roadmap stations MUST NOT add station.requiredTotalXp (or any XP) to totalXp
        val completedCountBefore = RoadmapManager.completedTaskIds.value.size
        for (stationId in 1..unlockedCount.coerceAtMost(10)) {
            val cumulativeBefore = TaskPools.getCumulativeXpForStation(stationId)
            assertTrue(expectedTasksXp >= cumulativeBefore)
            val claimed = RoadmapManager.claimStationReward(app, stationId)
            assertNotNull("Station $stationId should be claimable once cumulative XP reaches $cumulativeBefore", claimed)
            assertEquals(
                "Claiming Roadmap Station $stationId MUST NOT alter Tasks XP!",
                expectedTasksXp,
                RoadmapManager.totalXp.value
            )
        }
        assertEquals(completedCountBefore, RoadmapManager.completedTaskIds.value.size)
    }

    @Test
    fun testNonCumulativePerStageXpResetsFromZero() {
        val s1Cost = TaskPools.getRequiredXpForStation(1)
        val s2Cost = TaskPools.getRequiredXpForStation(2)
        val s3Cost = TaskPools.getRequiredXpForStation(3)
        assertEquals(1345L, s1Cost)
        assertTrue(s2Cost > s1Cost)

        // At 0 XP -> Station 0 unlocked, stage progress is 0 / s1Cost
        val prog0 = RoadmapManager.getStageProgress(0L)
        assertEquals(0, prog0.unlockedStationId)
        assertEquals(1, prog0.nextStationId)
        assertEquals(0L, prog0.currentStageXp)
        assertEquals(s1Cost, prog0.nextStageRequiredXp)

        // Reach exact Station 1 cost (1,345 XP) -> Station 1 unlocked, Station 2 resets back to 0 / s2Cost!
        RoadmapManager.addXp(app, s1Cost)
        val prog1 = RoadmapManager.getStageProgress(RoadmapManager.totalXp.value)
        assertEquals(1, prog1.unlockedStationId)
        assertEquals(2, prog1.nextStationId)
        assertEquals(0L, prog1.currentStageXp)
        assertEquals(s2Cost, prog1.nextStageRequiredXp)
        assertEquals(StationState.UNLOCKED, RoadmapManager.getStationState(1))
        assertEquals(StationState.LOCKED, RoadmapManager.getStationState(2))

        // Add +500 XP (total 1,845 XP > s2Cost): Station 2 must STILL be LOCKED at 500 / s2Cost!
        RoadmapManager.addXp(app, 500L)
        val progPartialS2 = RoadmapManager.getStageProgress(RoadmapManager.totalXp.value)
        assertEquals(1, progPartialS2.unlockedStationId)
        assertEquals(2, progPartialS2.nextStationId)
        assertEquals(500L, progPartialS2.currentStageXp)
        assertEquals(StationState.LOCKED, RoadmapManager.getStationState(2))

        // Earn remaining XP needed for Station 2 -> Station 2 unlocks, Station 3 starts from 0 / s3Cost
        RoadmapManager.addXp(app, s2Cost - 500L)
        val prog2 = RoadmapManager.getStageProgress(RoadmapManager.totalXp.value)
        assertEquals(2, prog2.unlockedStationId)
        assertEquals(3, prog2.nextStationId)
        assertEquals(0L, prog2.currentStageXp)
        assertEquals(s3Cost, prog2.nextStageRequiredXp)
        assertEquals(StationState.UNLOCKED, RoadmapManager.getStationState(2))
        assertEquals(StationState.LOCKED, RoadmapManager.getStationState(3))
    }

    @Test
    fun testXpCalculationEngineActionRulesAndSizeBonus() {
        assertEquals(0L, RoadmapManager.totalXp.value)
        assertEquals(0L, XpCalculationEngine.creditedTelemetryXp.value)

        // 1. Deflame Action: +20 XP per tap
        FlameManager.setScore(app, 500.0)
        FlameManager.deflame(app, 3.0)
        assertEquals(20L, RoadmapManager.totalXp.value)
        assertEquals(20L, XpCalculationEngine.creditedTelemetryXp.value)

        // 2. Girl Action: +50 XP per tap
        FlameManager.girl(app, 3.0)
        assertEquals(70L, RoadmapManager.totalXp.value)
        assertEquals(70L, XpCalculationEngine.creditedTelemetryXp.value)

        // 3. Strike Recorded: +100 XP per single strike (and scaled for batch strikes) on live today
        val liveToday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        FlameManager.onStrikeRegistered(
            context = app,
            strikeDate = liveToday,
            boost = 90.0,
            personName = "Target A",
            latestDateBeforeInsert = null,
            strikesToday = emptyList(),
            strikeCount = 1
        )
        assertEquals(170L, RoadmapManager.totalXp.value)

        FlameManager.onStrikeRegistered(
            context = app,
            strikeDate = liveToday,
            boost = 90.0,
            personName = "Target B",
            latestDateBeforeInsert = null,
            strikesToday = emptyList(),
            strikeCount = 3
        )
        assertEquals(470L, RoadmapManager.totalXp.value)

        // 4. New Person Added (Size <= 200 -> +500 XP strictly, NO bonus at exact 200.0)
        assertEquals(500L, XpCalculationEngine.calculatePersonAdditionXp(200.0))
        XpCalculationEngine.onNewPersonAdded(app, 200.0)
        assertEquals(970L, RoadmapManager.totalXp.value)

        // 5. New Person Added (Size > 200 -> +500 base + 1,000 bonus = +1,500 XP total)
        assertEquals(1500L, XpCalculationEngine.calculatePersonAdditionXp(200.1))
        XpCalculationEngine.onNewPersonAdded(app, 300.0)
        assertEquals(2470L, RoadmapManager.totalXp.value)
        assertEquals(2470L, XpCalculationEngine.creditedTelemetryXp.value)
    }

    @Test
    fun testXpCalculationEngineRetroactiveStartDateScanAndIdempotency() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val trackingStartDate = sdf.format(startCal.time) // 2026-11-10
        val beforeStartMillis = startCal.timeInMillis - 86_400_000L // 2026-11-09
        val onStartMillis = startCal.timeInMillis + 3_600_000L // 2026-11-10 01:00
        val afterStartMillis = startCal.timeInMillis + 2 * 86_400_000L // 2026-11-12

        val flameLogs = listOf(
            FlameLogEntry(id = 1, timestamp = beforeStartMillis, title = "Old Deflame", change = -3.0, resultingScore = 97.0, reason = "DEFLAME"),
            FlameLogEntry(id = 2, timestamp = onStartMillis, title = "Valid Deflame 1", change = -3.0, resultingScore = 94.0, reason = "DEFLAME"),
            FlameLogEntry(id = 3, timestamp = afterStartMillis, title = "Valid Deflame 2", change = -3.0, resultingScore = 91.0, reason = "DEFLAME"),
            FlameLogEntry(id = 4, timestamp = beforeStartMillis, title = "Old Girl", change = -3.0, resultingScore = 88.0, reason = "GIRL"),
            FlameLogEntry(id = 5, timestamp = afterStartMillis, title = "Valid Girl", change = -3.0, resultingScore = 85.0, reason = "GIRL"),
            FlameLogEntry(id = 6, timestamp = afterStartMillis, title = "Inactivity", change = 50.0, resultingScore = 135.0, reason = "INACTIVITY")
        )

        val strikeEvents = listOf(
            StrikeEvent(id = 10, individualId = 1, individualName = "A", year = 2026, date = "2026-11-08", time = "12:00:00", count = 5, strikeType = "Strike", performanceValue = 80.0),
            StrikeEvent(id = 11, individualId = 1, individualName = "A", year = 2026, date = "2026-11-10", time = "12:00:00", count = 1, strikeType = "Strike", performanceValue = 80.0),
            StrikeEvent(id = 12, individualId = 2, individualName = "B", year = 2026, date = "2026-11-14", time = "15:00:00", count = 2, strikeType = "Strike", performanceValue = 90.0)
        )

        val individuals = listOf(
            Individual(id = 1, fullName = "Old Person", type = "M", length = 25.0, width = 12.0, size = 300.0, sScore = 80.0, fScore = 80.0, cScore = 80.0, boost = 85.0, updatedAt = beforeStartMillis),
            Individual(id = 2, fullName = "Standard New Person", type = "M", length = 20.0, width = 10.0, size = 200.0, sScore = 80.0, fScore = 80.0, cScore = 80.0, boost = 85.0, updatedAt = onStartMillis),
            Individual(id = 3, fullName = "Bonus Size Person", type = "FG", length = 25.0, width = 10.0, size = 250.0, sScore = 80.0, fScore = 80.0, cScore = 80.0, boost = 85.0, updatedAt = afterStartMillis)
        )

        // Expected from >= 2026-11-10:
        // 2 Deflames (40) + 1 Girl (50) + 3 Strikes (300) + 2 Persons base (1000) + 1 Large Size Bonus >200 (1000) = 2,390 XP
        val breakdown1 = XpCalculationEngine.syncRetroactiveTelemetry(
            context = app,
            trackingStartDate = trackingStartDate,
            flameLogs = flameLogs,
            strikeEvents = strikeEvents,
            individuals = individuals,
            recalculateExact = false
        )
        assertEquals(2, breakdown1.deflameCount)
        assertEquals(1, breakdown1.girlCount)
        assertEquals(3, breakdown1.strikeCount)
        assertEquals(2, breakdown1.personCount)
        assertEquals(1, breakdown1.largePersonBonusCount)
        assertEquals(2390L, breakdown1.totalTelemetryXp)
        assertEquals(2390L, breakdown1.deltaCreditedXp)
        assertEquals(2390L, RoadmapManager.totalXp.value)
        val unlockedLevel = XpCalculationEngine.calculateLevelForXp(RoadmapManager.totalXp.value)
        assertEquals(1, unlockedLevel)
        assertTrue(RoadmapManager.totalXp.value >= XpCalculationEngine.getCumulativeXpForLevel(unlockedLevel))
        assertTrue(RoadmapManager.totalXp.value < XpCalculationEngine.getCumulativeXpForLevel(unlockedLevel + 1))

        // Idempotency check: repeating sync must award +0 delta and never double-count
        val breakdown2 = XpCalculationEngine.syncRetroactiveTelemetry(
            context = app,
            trackingStartDate = trackingStartDate,
            flameLogs = flameLogs,
            strikeEvents = strikeEvents,
            individuals = individuals,
            recalculateExact = false
        )
        assertEquals(0L, breakdown2.deltaCreditedXp)
        assertEquals(2390L, RoadmapManager.totalXp.value)

        // Moving start date forward to 2026-11-12 with recalculateExact = true reconciles cleanly:
        // Only: 1 Deflame (20) + 1 Girl (50) + 2 Strikes (200) + 1 Large Person (1500) = 1,770 XP
        val earlierAdjusted = XpCalculationEngine.syncRetroactiveTelemetry(
            context = app,
            trackingStartDate = "2026-11-12",
            flameLogs = flameLogs,
            strikeEvents = strikeEvents,
            individuals = individuals,
            recalculateExact = true
        )
        assertEquals(1770L, earlierAdjusted.totalTelemetryXp)
        assertEquals(-620L, earlierAdjusted.deltaCreditedXp)
        assertEquals(1770L, RoadmapManager.totalXp.value)
    }

    @Test
    fun testShieldStationRewardsDepositToInventoryWithoutAutoActivating() {
        val s7Cumulative = TaskPools.getCumulativeXpForStation(7)
        RoadmapManager.addXp(app, s7Cumulative)

        // Reaching Station 7 XP must NOT auto-claim any station or activate any shield
        assertTrue(RoadmapManager.claimedStations.value.isEmpty())
        assertFalse(FlameManager.isStreakShieldActive.value)
        assertEquals(0, FlameManager.available1DayShields.value)
        assertEquals(0, FlameManager.available48hShields.value)
        assertEquals(0, FlameManager.availableManualShields.value)

        // Claim Station 1 (24h shield), Station 4 (48h shield), Station 7 (72h shield)
        assertNotNull(RoadmapManager.claimStationReward(app, 1))
        assertEquals(1, FlameManager.available1DayShields.value)
        assertEquals(1, RoadmapManager.shields24h.value)
        assertFalse(FlameManager.isStreakShieldActive.value)

        assertNotNull(RoadmapManager.claimStationReward(app, 4))
        assertEquals(1, FlameManager.available48hShields.value)
        assertEquals(1, RoadmapManager.shields48h.value)
        assertFalse(FlameManager.isStreakShieldActive.value)

        assertNotNull(RoadmapManager.claimStationReward(app, 7))
        assertEquals(1, FlameManager.availableManualShields.value)
        assertEquals(1, RoadmapManager.shields72h.value)
        assertFalse(FlameManager.isStreakShieldActive.value)
    }
}
