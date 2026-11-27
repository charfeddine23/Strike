package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.profile.ProfileManager
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.StationState
import com.example.data.roadmap.TaskItem
import com.example.data.roadmap.TaskPeriod
import com.example.data.roadmap.TaskPools
import com.example.data.roadmap.XpCalculationEngine
import com.example.ui.components.RoadmapBiome
import com.example.ui.components.roadmapBiomeStateForStation
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class RoadmapAndThemeEnhancementsTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(object : TestWatcher() {
        override fun starting(description: Description) {
            resetAllState()
        }

        override fun finished(description: Description) {
            resetAllState()
        }

        private fun resetAllState() {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            ctx.getSharedPreferences("strikepulse_security_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            ctx.getSharedPreferences("strikepulse_flame_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            ctx.getSharedPreferences("strikepulse_roadmap_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            ctx.getSharedPreferences("strikepulse_profile_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            ctx.getSharedPreferences("strikepulse_xp_engine_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            FlameManager.resetForTesting(ctx)
            ProfileManager.resetForTesting(ctx)
            XpCalculationEngine.resetForTesting(ctx)
            RoadmapManager.resetForTesting(ctx)
            runBlocking(Dispatchers.IO) {
                AppDatabase.getDatabase(ctx).clearAllTables()
            }
        }
    }).around(composeRule)

    @Test
    fun taskCodeFormatsPeriodNumberAndDifficultyCorrectlyAcrossAll1000Tasks() {
        val sampleDaily23Easy = TaskItem(
            id = "daily_23",
            period = TaskPeriod.DAILY,
            title = "اختبار",
            description = "وصف",
            xpBounty = 50,
            difficultyTier = 1
        )
        assertEquals("D23E", sampleDaily23Easy.taskCode)

        val sampleWeekly5Med = TaskItem(
            id = "weekly_5",
            period = TaskPeriod.WEEKLY,
            title = "اختبار",
            description = "وصف",
            xpBounty = 120,
            difficultyTier = 2
        )
        assertEquals("W5M", sampleWeekly5Med.taskCode)

        val sampleMonthly10Hard = TaskItem(
            id = "monthly_10",
            period = TaskPeriod.MONTHLY,
            title = "اختبار",
            description = "وصف",
            xpBounty = 300,
            difficultyTier = 3
        )
        assertEquals("M10H", sampleMonthly10Hard.taskCode)

        val sampleEternal100Legend = TaskItem(
            id = "everlasting_100",
            period = TaskPeriod.EVERLASTING,
            title = "اختبار",
            description = "وصف",
            xpBounty = 900,
            difficultyTier = 4
        )
        assertEquals("E100L", sampleEternal100Legend.taskCode)

        // All 1,000 tasks in catalog must have unique valid task codes
        val codes = TaskPools.ALL_1000_TASKS.map { it.taskCode }
        assertEquals(1000, codes.distinct().size)
        val pattern = Regex("^[DWME]\\d+[EMHL]$")
        assertTrue(codes.all { pattern.matches(it) })
    }

    private fun enterMainAppFromHeatmap() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("heatmap_swipe_up_hide_btn").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun settingsDrawer_controlsDayNightAndSystemThemeModesWithPersistence() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurityPreferences(ctx)
        assertEquals(AppThemeMode.SYSTEM, prefs.themeMode)

        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("top_bar_settings_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("settings_drawer_panel").fetchSemanticsNodes().isNotEmpty()
        }

        // Switch to Day (Light) Mode
        composeRule.onNodeWithTag("theme_mode_light").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(AppThemeMode.LIGHT, SecurityPreferences(ctx).themeMode)

        // Switch to Night (Dark) Mode
        composeRule.onNodeWithTag("theme_mode_dark").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(AppThemeMode.DARK, SecurityPreferences(ctx).themeMode)

        // Switch back to System Mode
        composeRule.onNodeWithTag("theme_mode_system").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(AppThemeMode.SYSTEM, SecurityPreferences(ctx).themeMode)
    }

    @Test
    fun tasksScreen_separatesFromRoadmap_hidesOldKeysAndTitle_showsTaskIdAndShapeSkip_andOpensLedgerAndXpLogs() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        XpCalculationEngine.onDeflameAction(ctx, bonusNote = "تحكم يدوي بالشعلة")
        XpCalculationEngine.onStrikeRecorded(ctx, strikeCount = 1, personName = "طارق")

        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()

        // 1. Removed header texts, Level pill/popup, Tasks tab & hub switcher must not be present on Roadmap
        assertEquals(0, composeRule.onAllNodesWithText("Cyber Roadmap & Tasks", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("500 Stations, XP Progression & Egg Rewards", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_back_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_header_progress_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_hub_menu_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_tab_tasks").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_progress_popup").fetchSemanticsNodes().size)

        // 2. Navigate to dedicated Tasks tab from Bottom Bar -> verify 3-key inventory boxes are gone, task ID badge & key-shaped Skip appear
        composeRule.onNodeWithTag("nav_tab_tasks").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasks_screen").assertIsDisplayed()

        assertEquals(0, composeRule.onAllNodesWithText("Wooden Key", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Bronze Key", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Silver Key", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Skip w/", substring = true).fetchSemanticsNodes().size)

        val firstDailyTask = RoadmapManager.dailyTasks.value.first()
        composeRule.onNode(
            hasTestTag("task_id_badge_${firstDailyTask.id}") and hasAnyChild(hasText(firstDailyTask.taskCode)),
            useUnmergedTree = true
        ).assertIsDisplayed()

        val skippableDaily = RoadmapManager.dailyTasks.value.first { it.canBeSkipped && !it.isLocked() }
        val keyShapeTag = "skip_key_shape_${skippableDaily.requiredSkipKey.name.lowercase()}_${skippableDaily.id}"
        composeRule.onNodeWithTag("btn_skip_task_${skippableDaily.id}").assertIsDisplayed()
        composeRule.onNodeWithTag(keyShapeTag, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("btn_skip_task_${skippableDaily.id}")
            .assertTextContains("تجاوز", substring = false)

        // Check Eternal filter shows compact "can't skip" badge and hides long Eternal Milestone text
        composeRule.onNodeWithText("الأبدية", substring = true).performClick()
        composeRule.waitForIdle()
        val firstEternal = RoadmapManager.yearlyTasks.value.first()
        composeRule.onNode(
            hasTestTag("task_cant_skip_badge_${firstEternal.id}") and hasAnyChild(hasText("can't skip")),
            useUnmergedTree = true
        ).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Cannot be skipped with keys", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("غير قابلة للتجاوز بالمفاتيح", substring = true).fetchSemanticsNodes().size)

        // 3. Switch cleanly inside Tasks Hub: Tasks -> Tasks Ledger -> Xp Logs -> Tasks
        composeRule.onNodeWithTag("tasks_hub_tab_ledger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_bar_settings_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_ledger_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tasks_hub_tab_xp_logs").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_bar_settings_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("xp_logs_screen").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("xp_logs_title").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("xp_logs_back_btn").fetchSemanticsNodes().size)
        composeRule.onNode(
            hasTestTag("xp_logs_stat_total") and hasAnyChild(hasText("120", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("xp_logs_list").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("السبب:", substring = true).fetchSemanticsNodes().isNotEmpty())
        composeRule.onAllNodesWithText("السبب:", substring = true)[0].assertIsDisplayed()

        composeRule.onNodeWithTag("tasks_hub_tab_tasks").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasks_lazy_list").assertIsDisplayed()
    }

    @Test
    fun roadmapBiomes_transitionSmoothlyAcrossAll4StagesFromIce1ToLava700() {
        // 1. Continuous mathematical biome interpolation verification across all 4 stages
        val s1 = roadmapBiomeStateForStation(1f)
        assertEquals(RoadmapBiome.GLACIAL, s1.dominantBiome)
        assertEquals(1.0f, s1.iceSubmersion, 0.01f)
        assertEquals(0.0f, s1.lavaSubmersion, 0.01f)

        val mid12 = roadmapBiomeStateForStation(175.5f)
        assertTrue(mid12.iceWeight in 0.45f..0.55f && mid12.rockWeight in 0.45f..0.55f)

        val s260 = roadmapBiomeStateForStation(260f)
        assertEquals(RoadmapBiome.ROCKY, s260.dominantBiome)
        assertTrue(s260.rockIntensity > 0.95f)

        val s440 = roadmapBiomeStateForStation(440f)
        assertEquals(RoadmapBiome.FOREST, s440.dominantBiome)
        assertTrue(s440.forestIntensity > 0.95f)

        val s526 = roadmapBiomeStateForStation(526f)
        assertEquals(RoadmapBiome.VOLCANIC, s526.dominantBiome)
        assertTrue(s526.lavaSubmersion > 0f && s526.lavaSubmersion < 0.35f)

        val s700 = roadmapBiomeStateForStation(700f)
        assertEquals(RoadmapBiome.VOLCANIC, s700.dominantBiome)
        assertEquals(1.0f, s700.lavaSubmersion, 0.01f)

        // 2. UI verification across all 4 stages on RoadmapScreen
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()

        // Level 1 starts deeply submerged in Glacial ice
        composeRule.onNodeWithTag("roadmap_biome_backdrop").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_biome_submersion_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("stage_banner_1").assertIsDisplayed()

        // Scroll to Stage 2: Rocky (176)
        composeRule.onNodeWithTag("roadmap_stations_list").performScrollToIndex(175)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("station_176").assertIsDisplayed()
        composeRule.onNodeWithTag("stage_banner_176").assertIsDisplayed()

        // Scroll to Stage 3: Forest (351)
        composeRule.onNodeWithTag("roadmap_stations_list").performScrollToIndex(350)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("station_351").assertIsDisplayed()
        composeRule.onNodeWithTag("stage_banner_351").assertIsDisplayed()

        // Scroll to Stage 4: Cataclysm & Volcanic (526)
        composeRule.onNodeWithTag("roadmap_stations_list").performScrollToIndex(525)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("station_526").assertIsDisplayed()
        composeRule.onNodeWithTag("stage_banner_526").assertIsDisplayed()

        // Scroll to Level 700: Deepest Volcanic Magma Abyss
        composeRule.onNodeWithTag("roadmap_stations_list").performScrollToIndex(699)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("station_700").assertIsDisplayed()
        composeRule.onNodeWithTag("stage_banner_700").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_biome_backdrop").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_biome_submersion_overlay").assertIsDisplayed()
    }

    @Test
    fun roadmapLevelProgression_andManualClaimBehavior_advanceFrontierAndKeepShieldsInInventory() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val s1Cost = TaskPools.getRequiredXpForStation(1)
        // Earn Station 1 + 500 partial XP toward Station 2
        RoadmapManager.addXp(ctx, s1Cost + 500L)

        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()

        // Station 1 is unlocked, shows Claim button and YOU badge, and Station 2 shows partial progress bar
        composeRule.onNodeWithTag("btn_claim_station_1").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_you_badge_1", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("station_progress_bar_2", useUnmergedTree = true).assertIsDisplayed()

        // Tapping Station 1 card body does NOT auto-claim
        composeRule.onNodeWithTag("station_1").performClick()
        composeRule.waitForIdle()
        assertEquals(StationState.UNLOCKED, RoadmapManager.getStationState(1))
        assertFalse(RoadmapManager.claimedStations.value.contains(1))

        // Tapping explicit Claim button DOES claim Station 1, adds 24h shield to inventory WITHOUT activating it,
        // and moves YOU progression badge forward onto Station 2
        composeRule.onNodeWithTag("btn_claim_station_1").performClick()
        composeRule.waitForIdle()

        assertEquals(StationState.CLAIMED, RoadmapManager.getStationState(1))
        assertTrue(RoadmapManager.claimedStations.value.contains(1))
        assertEquals(1, FlameManager.available1DayShields.value)
        assertFalse("Shield reward must NOT auto-activate on claim", FlameManager.isStreakShieldActive.value)
        composeRule.onNodeWithTag("roadmap_you_badge_2", useUnmergedTree = true).assertIsDisplayed()
    }
}
