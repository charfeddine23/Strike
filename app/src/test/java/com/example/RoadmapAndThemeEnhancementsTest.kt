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
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.profile.ProfileManager
import com.example.data.roadmap.RoadmapManager
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
        if (composeRule.onAllNodesWithTag("heatmap_enter_btn").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("heatmap_enter_btn").performClick()
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
    fun roadmapScreen_placesLedgerLeftXpLogsRight_hidesOldTitleAndKeys_showsTaskIdAndShapeSkip_andOpensXpLogs() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // Seed XP logs so XpLogs screen shows real entries and reasons
        XpCalculationEngine.onDeflameAction(ctx, bonusNote = "تحكم يدوي بالشعلة")
        XpCalculationEngine.onStrikeRecorded(ctx, strikeCount = 1, personName = "طارق")

        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()

        // 1. Removed header texts & back arrow must not be present
        assertEquals(0, composeRule.onAllNodesWithText("Cyber Roadmap & Tasks", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("500 Stations, XP Progression & Egg Rewards", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_back_btn").fetchSemanticsNodes().size)

        // 2. Living Level pill + Obsidian Portal Menu trigger
        val progressBtnBounds = composeRule.onNodeWithTag("roadmap_header_progress_btn").assertIsDisplayed().getBoundsInRoot()
        val portalMenuBtnBounds = composeRule.onNodeWithTag("roadmap_hub_menu_btn").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Expected Level Pill and Portal Switcher Trigger without overlap",
            progressBtnBounds.right <= portalMenuBtnBounds.left || portalMenuBtnBounds.right <= progressBtnBounds.left
        )
        // Progress card is NOT inline before opening the floating popup
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_progress_popup").fetchSemanticsNodes().size)

        // 3. Switch to Tasks tab -> verify 3-key inventory boxes are gone, task ID badge & key-shaped Skip appear
        composeRule.onNodeWithTag("roadmap_tab_tasks").performClick()
        composeRule.waitForIdle()

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

        // 4. Tap-to-Change cycles cleanly: Roadmap -> Tasks Ledger -> Xp Logs -> Roadmap
        composeRule.onNodeWithTag("roadmap_hub_menu_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_bar_settings_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_ledger_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("roadmap_hub_menu_btn").performClick()
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

        // Tap Switch View from Xp Logs -> returns straight to RoadmapScreen
        composeRule.onNodeWithTag("roadmap_hub_menu_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("roadmap_screen").assertIsDisplayed()

        // 5. Open Tasks Ledger via Tap-to-Change -> topBar also stays visible
        composeRule.onNodeWithTag("roadmap_hub_menu_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_bar_settings_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_ledger_screen").assertIsDisplayed()
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

        // Floating progress menu shows / 700
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_header_progress_title").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("roadmap_header_progress_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("roadmap_progress_popup").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_header_progress_title")
            .assertIsDisplayed()
            .assertTextContains("/ 700", substring = true)

        // Jump to Stage 2: Rocky (176)
        composeRule.onNodeWithTag("btn_jump_176").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("station_176").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("stage_banner_176").assertIsDisplayed()

        // Jump to Stage 3: Forest (351)
        composeRule.onNodeWithTag("roadmap_header_progress_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_jump_351").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("station_351").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("stage_banner_351").assertIsDisplayed()

        // Jump to Stage 4: Cataclysm & Volcanic (526)
        composeRule.onNodeWithTag("roadmap_header_progress_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_jump_526").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("station_526").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("stage_banner_526").assertIsDisplayed()

        // Jump to Level 700: Deepest Volcanic Magma Abyss
        composeRule.onNodeWithTag("roadmap_header_progress_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_jump_700").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("station_700").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("station_700").assertIsDisplayed()
        composeRule.onNodeWithTag("stage_banner_700").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_biome_backdrop").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_biome_submersion_overlay").assertIsDisplayed()
    }
}
