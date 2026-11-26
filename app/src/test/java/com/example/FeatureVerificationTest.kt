package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.profile.ProfileManager
import com.example.data.profile.SkipKeyType
import com.example.data.repository.IndividualRepository
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.TaskStatus
import com.example.data.roadmap.XpCalculationEngine
import com.example.data.roadmap.XpSourceCategory
import com.example.util.SmartMatchReason
import com.example.util.SmartSearchMatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeatureVerificationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: IndividualRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("strikepulse_flame_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("strikepulse_roadmap_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("strikepulse_profile_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("strikepulse_xp_engine_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        FlameManager.resetForTesting(context)
        ProfileManager.resetForTesting(context)
        XpCalculationEngine.resetForTesting(context)
        RoadmapManager.resetForTesting(context)

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = IndividualRepository(database.individualDao(), database.strikeEventDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun dateDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateFormat.format(cal.time)
    }

    @Test
    fun xpHistoryTracksEverySourceAndConcernedPartyAndSurvivesSnapshot() {
        // 1. Deflame (+20)
        XpCalculationEngine.onDeflameAction(context, bonusNote = "Pulse Elixir +5")
        // 2. Girl (+50)
        XpCalculationEngine.onGirlAction(context)
        // 3. Strike with personName (+100)
        XpCalculationEngine.onStrikeRecorded(context, strikeCount = 1, personName = "طارق المنصور")
        // 4. New person with Size > 200 (+1500)
        XpCalculationEngine.onNewPersonAdded(context, size = 260.0, personName = "فاطمة الزهراء")
        // 5. Task completion
        val dailyTask = RoadmapManager.dailyTasks.value.first()
        assertTrue(RoadmapManager.completeTask(context, dailyTask.id))
        // 6. Skip Key completion
        val skippable = RoadmapManager.dailyTasks.value.first { it.status == TaskStatus.PENDING && it.canBeSkipped && !it.isLocked() }
        ProfileManager.grantSkipKeys(context, skippable.requiredSkipKey, 1)
        assertTrue(RoadmapManager.useSkipKeyForTask(context, skippable.id))

        val history = RoadmapManager.xpHistory.value
        assertEquals(6, history.size)

        assertTrue(history.any { it.category == XpSourceCategory.DEFLAME && it.amount == 20L })
        assertTrue(history.any { it.category == XpSourceCategory.GIRL && it.amount == 50L })
        assertTrue(history.any { it.category == XpSourceCategory.STRIKE && it.amount == 100L && it.concernedParty.contains("طارق المنصور") })
        assertTrue(history.any { it.category == XpSourceCategory.PERSON && it.amount == 1500L && it.concernedParty.contains("فاطمة الزهراء") })
        assertTrue(history.any { it.category == XpSourceCategory.TASK && it.amount == dailyTask.xpBounty.toLong() && it.concernedParty == dailyTask.title })
        assertTrue(history.any { it.category == XpSourceCategory.SKIP_KEY && it.amount == skippable.xpBounty.toLong() && it.concernedParty == skippable.title })

        // Export & Restore Snapshot preserves xpHistory
        val snapshot = RoadmapManager.exportSnapshot(context)
        RoadmapManager.resetAllLevelsToZero(context)
        assertTrue(RoadmapManager.xpHistory.value.isEmpty())

        RoadmapManager.restoreSnapshot(context, snapshot, replaceExisting = true)
        assertEquals(6, RoadmapManager.xpHistory.value.size)
    }

    @Test
    fun recentStrikesLogsEveryStrikeLiterallyUpTo10000() = runBlocking {
        val personId = repository.save(
            Individual(
                fullName = "Test Striker",
                type = "M",
                length = 25.0,
                width = 10.0,
                size = 250.0,
                sScore = 80.0,
                fScore = 85.0,
                cScore = 90.0,
                boost = 257.5
            )
        )

        // Increment by delta=5 creates 5 individual count=1 events
        repository.incrementStrike(personId, delta = 5)
        // Historical 12 strikes creates 12 individual count=1 events
        repository.recordHistoricalStrikes(personId, listOf(2025 to 12), lang = "ar")

        val events = repository.allStrikeEvents.first()
        assertEquals(17, events.size)
        assertTrue(events.all { it.count == 1 })

        // Verify legacy batch row normalization expands count > 1 into literal count = 1 rows
        database.strikeEventDao().insertEvent(
            StrikeEvent(
                individualId = personId,
                individualName = "Test Striker",
                year = 2024,
                date = "2024-03-10",
                time = "10:00:00",
                count = 4,
                strikeType = "Batch +4",
                performanceValue = 100.0
            )
        )
        repository.normalizeBatchStrikeEventsToLiteral(maxTotal = IndividualRepository.MAX_LITERAL_RECENT_STRIKES)

        val expanded = repository.allStrikeEvents.first()
        assertEquals(21, expanded.size)
        assertTrue(expanded.all { it.count == 1 })
    }

    @Test
    fun smartSearchMatcherHandlesArabicNormalizationTypesPhonesInitialsAndFuzzy() {
        val list = listOf(
            Individual(id = 1, fullName = "أحمد المنصور", type = "M", length = 21.0, width = 10.0, size = 210.0, sScore = 50.0, fScore = 50.0, cScore = 47.9, boost = 150.0, strikeCount = 12, phoneNumber = "+213555112233"),
            Individual(id = 2, fullName = "فاطمة الزهراء", type = "FG", length = 25.0, width = 10.0, size = 250.0, sScore = 70.0, fScore = 70.0, cScore = 77.5, boost = 220.0, strikeCount = 40, phoneNumber = "+213661998877"),
            Individual(id = 3, fullName = "Tariq Al-Mansoor", type = "TS", length = 18.0, width = 10.0, size = 180.0, sScore = 40.0, fScore = 40.0, cScore = 38.2, boost = 120.0, strikeCount = 7, phoneNumber = "+15550198")
        )

        // Arabic Alef + Ta Marbuta normalization
        val arMatch = SmartSearchMatcher.filterAndRank(list, "احمد")
        assertEquals(1L, arMatch.first().individual.id)

        val arTaMarbuta = SmartSearchMatcher.filterAndRank(list, "فاطمه")
        assertEquals(2L, arTaMarbuta.first().individual.id)

        // Type synonym ("ذكر" -> M)
        val typeMatch = SmartSearchMatcher.filterAndRank(list, "ذكر")
        assertEquals(1L, typeMatch.first().individual.id)
        assertEquals(SmartMatchReason.TYPE_MATCH, typeMatch.first().reason)

        // Phone digits match
        val phoneMatch = SmartSearchMatcher.filterAndRank(list, "998877")
        assertEquals(2L, phoneMatch.first().individual.id)
        assertEquals(SmartMatchReason.PHONE_MATCH, phoneMatch.first().reason)

        // Ordered initials match ("ta" -> Tariq Al-Mansoor)
        val initialsMatch = SmartSearchMatcher.filterAndRank(list, "tm")
        assertTrue(initialsMatch.any { it.individual.id == 3L })

        // Typo tolerance ("Tarriq" -> Tariq Al-Mansoor)
        val fuzzyMatch = SmartSearchMatcher.filterAndRank(list, "Tarriq")
        assertEquals(3L, fuzzyMatch.first().individual.id)
    }

    @Test
    fun flameAnalyticsAndCalculationPreviewStayStrictlySynchronized() {
        // Leave stale simulator values beforehand to verify recalculation cleans & syncs analytics 1:1
        FlameManager.setScore(context, 999.0)

        val startDate = dateDaysAgo(30) // +90.0 pts inactivity
        val strikes = listOf(
            // Single strike 10 days ago (-15.0 pts)
            StrikeEvent(id = 1, individualId = 1, individualName = "Target A", date = dateDaysAgo(10), year = 2026, count = 1, performanceValue = 15.0),
            // Double strike 5 days ago -> (10.0 + 10.0) * 3 = -60.0 pts (note intermediate peak before day 25 exceeded final score!)
            StrikeEvent(id = 2, individualId = 2, individualName = "Target B", date = dateDaysAgo(5), year = 2026, count = 1, performanceValue = 10.0),
            StrikeEvent(id = 3, individualId = 3, individualName = "Target C", date = dateDaysAgo(5), year = 2026, count = 1, performanceValue = 10.0)
        )

        val preview = FlameManager.previewRecalculation(
            startDateStr = startDate,
            strikeEvents = strikes,
            girlPerMonth = 2,    // 2 presses * 3 = -6.0 pts
            deflamePerMonth = 1  // 1 press * 3 = -3.0 pts
        )

        val applied = FlameManager.recalculateFromStartDate(
            context = context,
            startDateStr = startDate,
            strikeEvents = strikes,
            lang = "ar",
            girlPerMonth = 2,
            deflamePerMonth = 1
        )

        assertEquals(preview.resultingScore, applied.resultingScore, 0.001)
        assertEquals(preview.estimatedPeakScore, applied.estimatedPeakScore, 0.001)
        assertEquals(preview.totalExtinguishedPoints, applied.totalExtinguishedPoints, 0.001)

        // Analytics StateFlows must match Preview 1-for-1 (both immediately and after reload)
        assertEquals(preview.resultingScore, FlameManager.flameScore.value, 0.001)
        assertEquals(preview.estimatedPeakScore, FlameManager.peakScore.value, 0.001)
        assertEquals(preview.inactivityPointsAdded, FlameManager.totalPointsGained.value, 0.001)
        assertEquals(preview.totalExtinguishedPoints, FlameManager.totalPointsExtinguished.value, 0.001)
        assertTrue(FlameManager.peakScore.value > FlameManager.flameScore.value)

        FlameManager.resetForTesting(context)
        assertEquals(preview.resultingScore, FlameManager.flameScore.value, 0.001)
        assertEquals(preview.estimatedPeakScore, FlameManager.peakScore.value, 0.001)
        assertEquals(preview.inactivityPointsAdded, FlameManager.totalPointsGained.value, 0.001)
        assertEquals(preview.totalExtinguishedPoints, FlameManager.totalPointsExtinguished.value, 0.001)
    }

    @Test
    fun historicalLogHandles10000StrikesAndSqlAnalyticsCalculatesAllTimeWithoutFilters() = runBlocking {
        val alphaId = repository.save(
            Individual(
                fullName = "Alpha Striker",
                type = "M",
                length = 22.0,
                width = 10.0,
                size = 220.0,
                sScore = 70.0,
                fScore = 70.0,
                cScore = 70.0,
                boost = 212.2
            )
        )
        val betaId = repository.save(
            Individual(
                fullName = "Beta Striker",
                type = "FG",
                length = 20.0,
                width = 10.0,
                size = 200.0,
                sScore = 65.0,
                fScore = 65.0,
                cScore = 65.0,
                boost = 197.0
            )
        )

        // Store 10,000 historical strikes literally across multiple years (2023..2026)
        repository.recordHistoricalStrikes(
            alphaId,
            listOf(2023 to 1500, 2024 to 3500, 2025 to 5000),
            lang = "en"
        )
        val logged10k = repository.allStrikeEvents.first()
        assertEquals(10_000, logged10k.size)
        assertTrue(logged10k.all { it.count == 1 })

        // Add additional strikes beyond 10,000: historical log displays latest 10,000, while
        // unfiltered SQL DAO queries calculate ALL historical strikes (> 10,000) without truncation!
        repository.recordHistoricalStrikes(betaId, listOf(2022 to 250, 2026 to 300), lang = "en")
        repository.incrementStrike(betaId, delta = 5)

        val latestDisplayLog = repository.allStrikeEvents.first()
        assertEquals(10_000, latestDisplayLog.size)

        val sqlAllTimeTotal = repository.allTimeStrikesCountFlow.first()
        assertEquals(10_555, sqlAllTimeTotal)

        val sqlYearly = repository.yearlyPersonAggregatesFlow.first()
        val totalsByYear = sqlYearly.groupBy { it.year }.mapValues { (_, list) -> list.sumOf { it.totalStrikes } }
        assertEquals(1500, totalsByYear[2023])
        assertEquals(3500, totalsByYear[2024])
        assertEquals(5000, totalsByYear[2025])
        assertEquals(250, totalsByYear[2022])
        assertEquals(305, totalsByYear[2026])
    }

    @Test
    fun resetAllProgressLocksUnlockedTitlesAndAvatarsBackToInitialState() {
        ProfileManager.unlockAvatar(context, "av_gabriel_wings")
        ProfileManager.equipAvatar(context, "av_gabriel_wings")
        ProfileManager.unlockTitle(context, 25)
        ProfileManager.equipTitle(context, 25)

        assertTrue(ProfileManager.unlockedAvatarIds.value.contains("av_gabriel_wings"))
        assertTrue(ProfileManager.unlockedTitleIds.value.contains(25))
        assertEquals("av_gabriel_wings", ProfileManager.equippedAvatar.value.id)
        assertEquals(25, ProfileManager.equippedTitle.value.id)

        ProfileManager.resetAllProgressToZero(context)

        assertEquals(setOf("av_core_node"), ProfileManager.unlockedAvatarIds.value)
        assertEquals(setOf(1), ProfileManager.unlockedTitleIds.value)
        assertEquals("av_core_node", ProfileManager.equippedAvatar.value.id)
        assertEquals(1, ProfileManager.equippedTitle.value.id)

        // Verify persistence after reload
        ProfileManager.resetForTesting(context)
        assertEquals(setOf("av_core_node"), ProfileManager.unlockedAvatarIds.value)
        assertEquals(setOf(1), ProfileManager.unlockedTitleIds.value)
        assertEquals("av_core_node", ProfileManager.equippedAvatar.value.id)
        assertEquals(1, ProfileManager.equippedTitle.value.id)
    }

    @Test
    fun ledgerToggleChangesTaskCompletionBothWaysWhileKeepingEarnedXpIntact() {
        // Earn XP legitimately first
        val dailyTask = RoadmapManager.dailyTasks.value.first()
        assertTrue(RoadmapManager.completeTask(context, dailyTask.id))
        val expectedXp = RoadmapManager.totalXp.value
        val expectedHistoryCount = RoadmapManager.xpHistory.value.size
        assertTrue(expectedXp > 0L)
        assertEquals(1, expectedHistoryCount)

        // 1. Toggle COMPLETED -> UNCOMPLETED via ledger toggle: status reverts to PENDING, XP stays intact!
        val revertedState = RoadmapManager.toggleTaskCompletionPreservingXp(context, dailyTask.id)
        assertEquals(false, revertedState)
        assertEquals(expectedXp, RoadmapManager.totalXp.value)
        assertEquals(expectedHistoryCount, RoadmapManager.xpHistory.value.size)
        val afterRevert = RoadmapManager.getTasksLedgerSnapshot().first { it.task.id == dailyTask.id }
        assertFalse(afterRevert.task.isCompleted)
        assertEquals(TaskStatus.PENDING, afterRevert.task.status)

        // 2. Toggle UNCOMPLETED -> COMPLETED on an active pending task & an eternal task without changing XP!
        val eternalTask = RoadmapManager.yearlyTasks.value.first { !it.isCompleted }
        val completedState = RoadmapManager.toggleTaskCompletionPreservingXp(context, eternalTask.id)
        assertEquals(true, completedState)
        assertEquals(expectedXp, RoadmapManager.totalXp.value)
        assertEquals(expectedHistoryCount, RoadmapManager.xpHistory.value.size)
        val afterComplete = RoadmapManager.getTasksLedgerSnapshot().first { it.task.id == eternalTask.id }
        assertTrue(afterComplete.task.isCompleted)
        assertNotNull(afterComplete.completedAtMillis)

        // 3. Reload from SharedPreferences and verify both XP and toggled task states persisted cleanly
        RoadmapManager.resetForTesting(context)
        assertEquals(expectedXp, RoadmapManager.totalXp.value)
        assertEquals(expectedHistoryCount, RoadmapManager.xpHistory.value.size)
        val snapshotAfterReload = RoadmapManager.getTasksLedgerSnapshot()
        assertFalse(snapshotAfterReload.first { it.task.id == dailyTask.id }.task.isCompleted)
        assertTrue(snapshotAfterReload.first { it.task.id == eternalTask.id }.task.isCompleted)

        // Unknown task ID returns null cleanly without modifying state
        assertNull(RoadmapManager.toggleTaskCompletionPreservingXp(context, "non_existent_task_id"))
    }
}
