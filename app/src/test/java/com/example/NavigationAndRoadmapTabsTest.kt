package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    @Test
    fun mainNavigationSwitchesCleanlyWithoutOverlappingScreensOrStuckBanners() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("records_dashboard_banner").assertIsDisplayed()

        // Navigate Dashboard -> Inventory
        composeRule.onNodeWithTag("nav_tab_inventory").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inventory_screen").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("records_dashboard_banner").fetchSemanticsNodes().size)

        // Open Inventory sub-page and verify hub disappears cleanly
        composeRule.onNodeWithTag("inv_cat_avatars").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inv_subpage_avatars").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inventory_hub").fetchSemanticsNodes().size)

        // Navigate Inventory -> Comparison (Smart Search appears cleanly)
        composeRule.onNodeWithTag("nav_tab_compare").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("smart_search_banner").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inventory_screen").fetchSemanticsNodes().size)

        // Navigate Comparison -> Dashboard (Smart Search vanishes, Dashboard banner returns)
        composeRule.onNodeWithTag("nav_tab_dashboard").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("records_dashboard_banner").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("smart_search_banner").fetchSemanticsNodes().size)
    }

    @Test
    fun roadmapTabsRenamedAndEliteMissionsShowsSoonPage() {
        composeRule.waitForIdle()
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
}
