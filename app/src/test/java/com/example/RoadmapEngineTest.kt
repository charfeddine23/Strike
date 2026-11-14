package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.profile.ProfileManager
import com.example.data.profile.SkipKeyType
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

        val flamePrefs = app.getSharedPreferences("strikepulse_flame_prefs", Application.MODE_PRIVATE)
        flamePrefs.edit().clear().commit()
        FlameManager.resetForTesting(app)

        val profilePrefs = app.getSharedPreferences("strikepulse_profile_prefs", Application.MODE_PRIVATE)
        profilePrefs.edit().clear().commit()
        ProfileManager.resetForTesting(app)
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

        // Station 20 and 100 have extra Silver Skip-Key rewards
        assertEquals(RewardType.KEY_SILVER, reward20.type)
        assertEquals(RewardType.KEY_SILVER, reward100.type)

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
    fun testSkipKeyConsumption() {
        val daily = RoadmapManager.dailyTasks.value
        val targetTask = daily.first()

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
        assertFalse("Skipped Daily task must disappear immediately from active list", updatedDaily.any { it.id == targetTask.id })
        assertTrue(RoadmapManager.completedTaskIds.value.contains(targetTask.id))
    }

    @Test
    fun testCarryOverPendingTasksAndDynamicRefillAcrossCycles() {
        val initialDaily = RoadmapManager.dailyTasks.value
        assertEquals(5, initialDaily.size)

        // 1. Complete 1 daily task and skip another
        val completedId = initialDaily.first().id
        assertTrue(RoadmapManager.completeTask(app, completedId))

        val skippedTask = RoadmapManager.dailyTasks.value.first()
        RoadmapManager.addKey(app, skippedTask.requiredSkipKey, 1)
        assertTrue(RoadmapManager.useSkipKeyForTask(app, skippedTask.id))

        // If any remaining daily task requires >1 step, increment it once so we verify partial progress carry-over too
        val partialCandidate = RoadmapManager.dailyTasks.value.firstOrNull { it.targetCount > 1 }
        if (partialCandidate != null) {
            RoadmapManager.onActionOccurred(app, partialCandidate.actionTag)
        }

        val carriedOverTasks = RoadmapManager.dailyTasks.value
        assertTrue("Expected pending tasks to carry over, size=${carriedOverTasks.size}", carriedOverTasks.size in 1..3)
        val carriedCounts = carriedOverTasks.associate { it.id to it.currentCount }

        // 2. Simulate next calendar day / week / month rollover
        val prefs = app.getSharedPreferences("strikepulse_roadmap_prefs", Application.MODE_PRIVATE)
        prefs.edit()
            .putString("key_last_daily_date", "2020-01-01")
            .putString("key_last_weekly_code", "2020_W01")
            .putString("key_last_monthly_code", "2020_M01")
            .apply()

        RoadmapManager.resetForTesting(app)

        val nextDayDaily = RoadmapManager.dailyTasks.value
        assertEquals(5, nextDayDaily.size)
        // Pending tasks from previous day must carry over intact with partial progress preserved
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
    fun testAnnualBatchPersistenceAndBottomArchive() {
        val initialYearly = RoadmapManager.yearlyTasks.value
        assertEquals(250, initialYearly.size)
        assertTrue(initialYearly.all { it.status == TaskStatus.PENDING })

        // Pick a STRIKE task with targetCount > 1 to test partial persistence, and complete another annual task
        val multiStepStrike = initialYearly.first { it.actionTag == "STRIKE" && it.targetCount > 1 }
        val completedAnnual = initialYearly.first { it.id != multiStepStrike.id }

        RoadmapManager.onActionOccurred(app, "STRIKE")
        assertTrue(RoadmapManager.completeTask(app, completedAnnual.id))

        // Re-initialize from SharedPreferences JSON
        RoadmapManager.resetForTesting(app)

        val reloadedYearly = RoadmapManager.yearlyTasks.value
        assertEquals(250, reloadedYearly.size)

        // Completed task remains visible and sits at the bottom archive (all pending before all completed)
        val firstCompletedIdx = reloadedYearly.indexOfFirst { it.isCompleted }
        assertTrue("At least one annual task should be archived as completed", firstCompletedIdx >= 0)
        assertTrue(reloadedYearly.drop(firstCompletedIdx).all { it.isCompleted })
        assertTrue(reloadedYearly.take(firstCompletedIdx).all { !it.isCompleted })
        assertEquals(completedAnnual.id, reloadedYearly.last().id)

        // Partial progress on pending multi-step annual task persisted across restart
        val reloadedStrike = reloadedYearly.first { it.id == multiStepStrike.id }
        assertEquals(1, reloadedStrike.currentCount)
        assertEquals(TaskStatus.PENDING, reloadedStrike.status)
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
        ProfileManager.unlockTitle(app, 21)

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

        // Permanent Unlock Persistence Rule: unlocked Titles & Avatars NEVER get wiped on reset
        assertTrue(ProfileManager.unlockedAvatarIds.value.containsAll(setOf("av_core_node", "av_gabriel_wings")))
        assertTrue(ProfileManager.unlockedTitleIds.value.containsAll(setOf(1, 21)))
    }

    @Test
    fun testWavySnakeTrajectoryZeroCenterCollisionsAndContinuity() {
        for (stationId in 1..500) {
            val nodeX = com.example.ui.components.wavyNodeNormalizedX(stationId.toFloat())
            // Nodes must always sit clearly on Left (<= 0.30) or Right (>= 0.70) so side glass cards never collide
            assertTrue(
                "Station $stationId nodeX=$nodeX too close to center",
                kotlin.math.abs(nodeX - 0.5f) >= 0.20f
            )
            assertTrue("Station $stationId nodeX=$nodeX out of safe bounds", nodeX in 0.14f..0.86f)

            if (stationId < 500) {
                val bottomOfRow = com.example.ui.components.wavyNodeNormalizedX(stationId + 0.5f)
                val topOfNextRow = com.example.ui.components.wavyNodeNormalizedX((stationId + 1) - 0.5f)
                assertEquals(bottomOfRow, topOfNextRow, 0.0001f)
            }
        }
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

        // 3. Tasks XP is strictly the sole source of XP progression
        val firstDaily = RoadmapManager.dailyTasks.value.first()
        val firstWeekly = RoadmapManager.weeklyTasks.value.first()
        assertTrue(RoadmapManager.completeTask(app, firstDaily.id))
        val xpAfterDaily = RoadmapManager.totalXp.value
        assertEquals(firstDaily.xpBounty.toLong(), xpAfterDaily)

        assertTrue(RoadmapManager.completeTask(app, firstWeekly.id))
        val expectedTasksXp = firstDaily.xpBounty.toLong() + firstWeekly.xpBounty.toLong()
        assertEquals(expectedTasksXp, RoadmapManager.totalXp.value)

        // With >= 150 Tasks XP, Station 1 (50 XP), Station 2 (51 XP), ... are unlocked
        val unlockedCount = RoadmapManager.calculateUnlockedStationId(expectedTasksXp)
        assertTrue("Expected Stations 1..2 unlocked from Tasks XP, got $unlockedCount", unlockedCount >= 2)

        // 4. Claiming Roadmap stations MUST NOT add station.requiredTotalXp (or any XP) to totalXp
        val completedCountBefore = RoadmapManager.completedTaskIds.value.size
        for (stationId in 1..unlockedCount.coerceAtMost(10)) {
            val reqXpBefore = TaskPools.getRequiredXpForStation(stationId)
            assertTrue(expectedTasksXp >= reqXpBefore)
            val claimed = RoadmapManager.claimStationReward(app, stationId)
            assertNotNull("Station $stationId should be claimable once Tasks XP reaches $reqXpBefore", claimed)
            assertEquals(
                "Claiming Roadmap Station $stationId ($reqXpBefore reqXP) MUST NOT alter Tasks XP!",
                expectedTasksXp,
                RoadmapManager.totalXp.value
            )
        }
        assertEquals(completedCountBefore, RoadmapManager.completedTaskIds.value.size)
    }
}
