package com.example

import android.content.Context
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class NavigationAndRoadmapTabsTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(object : TestWatcher() {
        override fun starting(description: Description) {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            runBlocking(Dispatchers.IO) {
                AppDatabase.getDatabase(ctx).clearAllTables()
            }
        }

        override fun finished(description: Description) {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            runBlocking(Dispatchers.IO) {
                AppDatabase.getDatabase(ctx).clearAllTables()
            }
        }
    }).around(composeRule)

    private fun enterMainAppFromHeatmap() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("heatmap_swipe_up_hide_btn").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun mainNavigationSwitchesCleanlyWithoutOverlappingScreensOrStuckBanners() {
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("records_dashboard_banner").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("sort_button").assertIsDisplayed()
        composeRule.onNodeWithTag("records_dashboard_btn").assertIsDisplayed()

        // Tap the new Dashboard button next to Sort/Filter -> opens Dashboard panel
        composeRule.onNodeWithTag("records_dashboard_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("records_dashboard_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("close_dashboard_dialog_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("records_dashboard_dialog").fetchSemanticsNodes().size)

        // Navigate Dashboard -> Inventory
        composeRule.onNodeWithTag("nav_tab_inventory").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inventory_screen").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("records_dashboard_btn").fetchSemanticsNodes().size)

        // Open Inventory sub-page and verify hub disappears cleanly
        composeRule.onNodeWithTag("inv_cat_avatars").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inv_subpage_avatars").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inventory_hub").fetchSemanticsNodes().size)

        // Navigate Inventory -> Comparison (Smart Search & header card are removed cleanly)
        composeRule.onNodeWithTag("nav_tab_compare").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selector_person_a").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("smart_search_banner").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Smart Analytical Comparison", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("مقارنة تحليلية ذكية", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Head-to-head metrics analysis", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("inventory_screen").fetchSemanticsNodes().size)

        // Navigate Comparison -> Home -> Records (Dashboard button returns)
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("records_dashboard_btn").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("smart_search_banner").fetchSemanticsNodes().size)
    }

    @Test
    fun roadmapTabsRenamedAndEliteMissionsShowsSoonPage() {
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("roadmap_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_tab_stations").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_tab_tasks").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_tab_elite_missions").assertIsDisplayed()

        composeRule.onNodeWithTag("roadmap_tab_stations").assertTextEquals("المسار")
        composeRule.onNodeWithTag("roadmap_tab_tasks").assertTextEquals("المهام")
        composeRule.onNodeWithTag("roadmap_tab_elite_missions").assertTextEquals("مهام النخبة")

        // Verify English resource strings match requested labels
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Roadmap", ctx.getString(R.string.roadmap_tab_stations))
        assertEquals("Tasks", ctx.getString(R.string.roadmap_tab_tasks))
        assertEquals("Elite Missions", ctx.getString(R.string.roadmap_tab_elite_missions))
        assertEquals("Soon", ctx.getString(R.string.elite_missions_soon))

        // Tap Elite Missions tab -> opens Elite Missions page showing قريباً / Soon
        composeRule.onNodeWithTag("roadmap_tab_elite_missions").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("elite_missions_page").assertIsDisplayed()
        composeRule.onNodeWithTag("elite_missions_soon_badge").assertIsDisplayed()
        composeRule.onNodeWithText("قريباً").assertIsDisplayed()
    }

    @Test
    fun dataEntryDialogHistoricalStrikesScrollsAndAcceptsEntryCleanly() {
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_record_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("data_entry_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("historical_strikes_section").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("input_historical_year").performScrollTo().performTextReplacement("2024")
        composeRule.onNodeWithTag("input_historical_count").performScrollTo().performTextReplacement("12")
        composeRule.onNodeWithTag("add_historical_entry_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("سنة 2024", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("12 ضربة", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deviationsScreen_hidesHeaderTitleAndBackArrow_andPlacesTrackBadgeBelowDna() {
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("dna_header_icon").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("deviations_screen").assertIsDisplayed()

        // In-screen back arrow & subtitle must be removed
        assertEquals(0, composeRule.onAllNodesWithTag("deviations_back_btn").fetchSemanticsNodes().size)
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val subtitleText = ctx.getString(R.string.deviations_subtitle)
        assertEquals(0, composeRule.onAllNodesWithText(subtitleText, substring = false).fetchSemanticsNodes().size)

        // Track identification badge sits strictly below the 3D DNA canvas
        val dnaBounds = composeRule.onNodeWithTag("deviations_dna_canvas").assertIsDisplayed().getBoundsInRoot()
        val trackBounds = composeRule.onNodeWithTag("deviations_track_badge").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Expected Track badge (${trackBounds.top}) to sit below DNA canvas (${dnaBounds.bottom})",
            trackBounds.top >= dnaBounds.bottom
        )

        // Sanctified / Warning status bar and Info button sit strictly below the 3D DNA canvas
        val warningBounds = composeRule.onNodeWithTag("deviations_warning_bar").assertIsDisplayed().getBoundsInRoot()
        val infoBounds = composeRule.onNodeWithTag("deviations_info_btn").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Expected warning bar (${warningBounds.top}) to sit below DNA canvas (${dnaBounds.bottom})",
            warningBounds.top >= dnaBounds.bottom
        )
        assertTrue(
            "Expected info button (${infoBounds.top}) to sit below DNA canvas (${dnaBounds.bottom})",
            infoBounds.top >= dnaBounds.bottom
        )
    }

    @Test
    fun eternalGoalsHeader_hidesOnUpwardSwipe_andReappearsOnDownwardPullAtFirstTask() {
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("roadmap_tab_tasks").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("period_chip_eternal").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("eternal_goals_header").assertIsDisplayed()

        // Swipe up to browse Eternal tasks -> Eternal Goals header collapses smoothly
        composeRule.onNodeWithTag("tasks_lazy_list").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("eternal_goals_header").fetchSemanticsNodes().size)

        // Return to the top (first task) and swipe down -> Eternal Goals header reappears smoothly
        composeRule.onNodeWithTag("tasks_lazy_list").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasks_lazy_list").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("eternal_goals_header").assertIsDisplayed()
    }

    @Test
    fun screenshotBlockingToggle_inSettings_updatesFlagSecureAndPersists() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        SecurityPreferences(ctx).isScreenshotBlockingEnabled = true

        enterMainAppFromHeatmap()
        assertTrue(
            "Expected FLAG_SECURE enabled by default",
            (composeRule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        )

        // Open Settings side drawer and disable Non-Screenshot toggle
        composeRule.onNodeWithTag("top_bar_settings_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screenshot_blocking_switch").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertFalse(SecurityPreferences(ctx).isScreenshotBlockingEnabled)
        assertEquals(
            0,
            composeRule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE
        )

        // Re-enable Non-Screenshot toggle -> FLAG_SECURE restored
        composeRule.onNodeWithTag("screenshot_blocking_switch").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(SecurityPreferences(ctx).isScreenshotBlockingEnabled)
        assertTrue(
            "Expected FLAG_SECURE re-enabled when switch is turned back on",
            (composeRule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        )
    }

    @Test
    fun bottomNavBar_showsIconsOnlyAndKeepsFixedTabBoundsAcrossSwitches() {
        enterMainAppFromHeatmap()
        val navTags = listOf(
            "nav_tab_home",
            "nav_tab_inventory",
            "nav_tab_compare",
            "nav_tab_roadmap"
        )
        val initialBounds = navTags.associateWith { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed().getBoundsInRoot()
        }

        // Old Records and Strike tabs are moved out of BottomBar into Home
        assertEquals(0, composeRule.onAllNodesWithTag("nav_tab_dashboard").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("nav_tab_strike").fetchSemanticsNodes().size)

        // Bottom bar tab label for Strike Recording ("تسجيل ضربة") is not rendered anywhere in the bar
        assertEquals(0, composeRule.onAllNodesWithText("تسجيل ضربة", substring = false).fetchSemanticsNodes().size)

        // Switch across multiple tabs and confirm every bottom bar tab stays in its exact fixed X slot
        listOf("nav_tab_inventory", "nav_tab_compare", "nav_tab_roadmap", "nav_tab_home").forEach { targetTab ->
            composeRule.onNodeWithTag(targetTab).performClick()
            composeRule.waitForIdle()
            navTags.forEach { tag ->
                val updated = composeRule.onNodeWithTag(tag).assertIsDisplayed().getBoundsInRoot()
                val base = initialBounds.getValue(tag)
                assertEquals("Left X shifted for $tag after clicking $targetTab", base.left.value, updated.left.value, 0.5f)
                assertEquals("Right X shifted for $tag after clicking $targetTab", base.right.value, updated.right.value, 0.5f)
            }
        }
    }

    @Test
    fun homeScreen_displaysRoadmapLevelHeatmapRecordsAndAddStrike_andNavigatesToEachCorrectly() {
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()

        val topBarBounds = composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed().getBoundsInRoot()
        val roadmapBounds = composeRule.onNodeWithTag("home_roadmap_level_card").assertIsDisplayed().getBoundsInRoot()
        val heatmapBounds = composeRule.onNodeWithTag("home_heatmap_card").assertIsDisplayed().getBoundsInRoot()
        val recordsBounds = composeRule.onNodeWithTag("home_records_btn").assertIsDisplayed().getBoundsInRoot()
        val addStrikeBounds = composeRule.onNodeWithTag("home_add_strike_btn").assertIsDisplayed().getBoundsInRoot()

        // Vertical order: TopBar -> Roadmap Level -> (Heatmap & Records side-by-side) -> Add Strike
        assertTrue(roadmapBounds.top >= topBarBounds.bottom)
        assertTrue(heatmapBounds.top >= roadmapBounds.bottom)
        assertTrue(recordsBounds.top >= roadmapBounds.bottom)
        assertTrue(addStrikeBounds.top >= heatmapBounds.bottom)
        assertTrue(addStrikeBounds.top >= recordsBounds.bottom)
        assertTrue(
            "Heatmap and Records must sit side-by-side horizontally",
            heatmapBounds.right <= recordsBounds.left || recordsBounds.right <= heatmapBounds.left
        )

        // 1. Tap Heatmap tile -> opens First-Strike Heatmap, then swipe up back to Home
        composeRule.onNodeWithTag("home_heatmap_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("first_strike_heatmap_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()

        // 2. Tap Records button -> opens old Records (DashboardScreen), then Back returns to Home
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("add_record_fab").assertIsDisplayed()
        composeRule.onNodeWithTag("records_back_to_home_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()

        // 3. Tap Add Strike button -> opens moved StrikeScreen, then Back returns to Home
        composeRule.onNodeWithTag("home_add_strike_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("strike_button").assertIsDisplayed()
        composeRule.onNodeWithTag("strike_back_to_home_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()

        // 4. Tap Roadmap Level card -> opens RoadmapScreen
        composeRule.onNodeWithTag("home_roadmap_level_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("roadmap_screen").assertIsDisplayed()
    }
}
