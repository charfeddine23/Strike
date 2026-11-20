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

    @Test
    fun settingsDrawer_controlsDayNightAndSystemThemeModesWithPersistence() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurityPreferences(ctx)
        assertEquals(AppThemeMode.SYSTEM, prefs.themeMode)

        composeRule.waitForIdle()
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

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()

        // 1. Removed header texts & back arrow must not be present
        assertEquals(0, composeRule.onAllNodesWithText("Cyber Roadmap & Tasks", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("500 Stations, XP Progression & Egg Rewards", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_back_btn").fetchSemanticsNodes().size)

        // 2. Tasks Ledger sits strictly on physical LEFT, Xp Logs sits strictly on physical RIGHT
        val ledgerBounds = composeRule.onNodeWithTag("roadmap_header_ledger_btn").assertIsDisplayed().getBoundsInRoot()
        val xpLogsBounds = composeRule.onNodeWithTag("roadmap_header_xp_logs_btn").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Expected Tasks Ledger (left=${ledgerBounds.left}, right=${ledgerBounds.right}) to be on the physical left of Xp Logs (left=${xpLogsBounds.left})",
            ledgerBounds.right <= xpLogsBounds.left
        )

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

        // 4. Open Xp Logs screen from right header button & verify earned XP events + reasons
        composeRule.onNodeWithTag("roadmap_header_xp_logs_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("xp_logs_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("xp_logs_title").assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("xp_logs_stat_total") and hasAnyChild(hasText("120", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("xp_logs_list").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("السبب:", substring = true).fetchSemanticsNodes().isNotEmpty())
        composeRule.onAllNodesWithText("السبب:", substring = true)[0].assertIsDisplayed()

        // Back button returns to RoadmapScreen
        composeRule.onNodeWithTag("xp_logs_back_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("roadmap_screen").assertIsDisplayed()
    }
}
