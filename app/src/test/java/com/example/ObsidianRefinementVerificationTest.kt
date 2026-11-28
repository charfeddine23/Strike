package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.model.Individual
import com.example.data.profile.ProfileManager
import com.example.data.repository.IndividualRepository
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.XpCalculationEngine
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
class ObsidianRefinementVerificationTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(object : TestWatcher() {
        override fun starting(description: Description) = resetAllState()
        override fun finished(description: Description) = resetAllState()

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

    private fun enterMainAppFromHeatmap() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("heatmap_swipe_up_hide_btn").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun personProfile_editHistoricalStrikesByYear_persistsChangesToDatabase() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val personId = runBlocking(Dispatchers.IO) {
            val id = repo.save(
                Individual(
                    fullName = "سليم المنصور",
                    type = "M",
                    length = 22.0,
                    width = 10.0,
                    size = 220.0,
                    sScore = 70.0,
                    fScore = 72.0,
                    cScore = 75.0,
                    boost = 220.0
                )
            )
            // Seed 2 live strikes + 2024(5) + 2023(3) -> total 10
            repo.incrementStrike(id, delta = 2)
            repo.recordHistoricalStrikes(id, listOf(2024 to 5, 2023 to 3), lang = "ar")
            id
        }

        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("record_expand_btn_$personId").fetchSemanticsNodes().isNotEmpty()
        }

        // Expand person record card & open Historical Strikes by Year editor
        composeRule.onNodeWithTag("record_expand_btn_$personId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("person_yearly_strikes_panel_$personId").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_edit_person_yearly_strikes_$personId").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("person_historical_strikes_dialog").assertIsDisplayed()

        // Increment 2024 (5 -> 6)
        composeRule.onNodeWithTag("inc_person_hist_year_2024").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Edit 2023 inline (3 -> 8)
        composeRule.onNodeWithTag("edit_person_hist_year_2023").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("person_hist_edit_count_2023").performScrollTo().performTextReplacement("8")
        composeRule.onNodeWithTag("person_hist_edit_apply_2023").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Add new year 2025 -> 4 strikes
        composeRule.onNodeWithTag("person_hist_add_year_input").performScrollTo().performTextReplacement("2025")
        composeRule.onNodeWithTag("person_hist_add_count_input").performScrollTo().performTextReplacement("4")
        composeRule.onNodeWithTag("person_hist_add_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Save changes cleanly to Room
        composeRule.onNodeWithTag("btn_save_person_yearly_strikes").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("person_historical_strikes_dialog").fetchSemanticsNodes().isEmpty()
        }

        // Verify persisted total (2 live + 6 + 8 + 4 = 20) & yearly breakdown in Room
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val updated = runBlocking(Dispatchers.IO) { db.individualDao().getIndividualById(personId) }
            updated?.strikeCount == 20
        }

        val yearlyMap = runBlocking(Dispatchers.IO) { repo.getHistoricalStrikesByYear(personId).toMap() }
        assertEquals(6, yearlyMap[2024])
        assertEquals(8, yearlyMap[2023])
        assertEquals(4, yearlyMap[2025])
    }

    @Test
    fun comparisonScreen_luxuryPersonPickerWithSearch_filtersAndSelectsSmoothly() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val (tariqId, yassinId, karimId) = runBlocking(Dispatchers.IO) {
            val a = repo.save(
                Individual(
                    fullName = "طارق المنصور",
                    type = "M",
                    length = 20.0,
                    width = 10.0,
                    size = 200.0,
                    sScore = 60.0,
                    fScore = 60.0,
                    cScore = 60.0,
                    boost = 190.0
                )
            )
            val b = repo.save(
                Individual(
                    fullName = "ياسين العتيبي",
                    type = "TS",
                    length = 24.0,
                    width = 10.0,
                    size = 240.0,
                    sScore = 75.0,
                    fScore = 70.0,
                    cScore = 72.0,
                    boost = 230.0
                )
            )
            val c = repo.save(
                Individual(
                    fullName = "كريم بن علي",
                    type = "FG",
                    length = 19.0,
                    width = 10.0,
                    size = 190.0,
                    sScore = 55.0,
                    fScore = 58.0,
                    cScore = 62.0,
                    boost = 175.0
                )
            )
            Triple(a, b, c)
        }

        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("nav_tab_compare").performClick()
        composeRule.waitForIdle()

        // Open luxury person A picker dialog
        composeRule.onNodeWithTag("selector_person_a").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("comparison_person_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("comparison_picker_list").assertIsDisplayed()

        // Search for "ياسين" -> only Yassin remains visible in filtered list
        composeRule.onNodeWithTag("comparison_picker_search_input").performTextReplacement("ياسين")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("comparison_picker_list").performScrollToNode(hasTestTag("comparison_picker_item_$yassinId"))
        composeRule.onNodeWithTag("comparison_picker_item_$yassinId").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("comparison_picker_item_$tariqId").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("comparison_picker_item_$karimId").fetchSemanticsNodes().size)

        // Select Yassin -> closes picker cleanly & updates Selector Person A
        composeRule.onNodeWithTag("comparison_picker_item_$yassinId").performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("comparison_person_picker_dialog").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("selector_person_a")
            .assertIsDisplayed()
            .assertTextContains("ياسين العتيبي", substring = true)
    }

    @Test
    fun tasksHub_transitionsCrisplyBetweenTasksTasksLedgerAndXpLogs_andRoadmapHasNoLevelOrTasks() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        XpCalculationEngine.onDeflameAction(ctx, bonusNote = "Obsidian Calibration")
        XpCalculationEngine.onStrikeRecorded(ctx, strikeCount = 1, personName = "طارق")

        enterMainAppFromHeatmap()

        // Roadmap must NOT contain Level pill, popup, hub switcher, or Tasks tab
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("roadmap_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_tab_stations").assertIsDisplayed()
        composeRule.onNodeWithTag("roadmap_tab_elite_missions").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("obsidian_roadmap_hub_switcher").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_header_progress_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_hub_menu_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("roadmap_tab_tasks").fetchSemanticsNodes().size)

        // Open dedicated Tasks tab from Bottom Bar -> Segmented Strip [ Tasks | Tasks Ledger | Xp Logs ]
        composeRule.onNodeWithTag("nav_tab_tasks").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasks_screen").assertIsDisplayed()
        val tTabBounds = composeRule.onNodeWithTag("tasks_hub_tab_tasks").assertIsDisplayed().getBoundsInRoot()
        val lTabBounds = composeRule.onNodeWithTag("tasks_hub_tab_ledger").assertIsDisplayed().getBoundsInRoot()
        val xTabBounds = composeRule.onNodeWithTag("tasks_hub_tab_xp_logs").assertIsDisplayed().getBoundsInRoot()
        assertTrue(tTabBounds.right <= lTabBounds.left || lTabBounds.right <= tTabBounds.left)
        assertTrue(lTabBounds.right <= xTabBounds.left || xTabBounds.right <= lTabBounds.left)

        // Switch smoothly across Tasks -> Tasks Ledger -> Xp Logs -> Tasks
        composeRule.onNodeWithTag("tasks_hub_tab_ledger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasks_ledger_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tasks_hub_tab_xp_logs").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("xp_logs_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tasks_hub_tab_tasks").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasks_lazy_list").assertIsDisplayed()
    }

    @Test
    fun topAndBottomBars_maintainTransparentOuterContainerAndFadeInBlurAcrossAllPages() {
        enterMainAppFromHeatmap()

        // Home -> Records (Dashboard)
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()
        val dashTopBarBounds = composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed().getBoundsInRoot()
        val dashBottomBarBounds = composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed().getBoundsInRoot()
        composeRule.onNodeWithTag("edge_fade_blur_top").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_bottom").assertIsDisplayed()
        val searchBounds = composeRule.onNodeWithTag("search_input").assertIsDisplayed().getBoundsInRoot()
        val fabBounds = composeRule.onNodeWithTag("add_record_fab").assertIsDisplayed().getBoundsInRoot()
        assertTrue("Dashboard top controls must start clear of TopBar", searchBounds.top >= dashTopBarBounds.bottom)
        assertTrue("Dashboard FAB must sit cleanly above BottomBar", fabBounds.bottom <= dashBottomBarBounds.top)

        // Inventory
        composeRule.onNodeWithTag("nav_tab_inventory").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_top").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_bottom").assertIsDisplayed()
        composeRule.onNodeWithTag("inventory_screen").assertIsDisplayed()

        // Home -> Add Strike
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_add_strike_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_top").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_bottom").assertIsDisplayed()

        // Comparison
        composeRule.onNodeWithTag("nav_tab_compare").performClick()
        composeRule.waitForIdle()
        val cmpTopBarBounds = composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed().getBoundsInRoot()
        composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_top").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_bottom").assertIsDisplayed()
        val selectorABounds = composeRule.onNodeWithTag("selector_person_a").assertIsDisplayed().getBoundsInRoot()
        assertTrue("Comparison top selectors must start clear of TopBar", selectorABounds.top >= cmpTopBarBounds.bottom)

        // Top Bar icons must NOT be wrapped in an enclosing pill container
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_telemetry_pill_container").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("dna_header_icon").assertIsDisplayed()
        composeRule.onNodeWithTag("header_flame_btn").assertIsDisplayed()

        // Tasks Hub & full-bleed Tasks viewport with expanded Fade-to-Liquid-Glass covering period chips (Daily/Weekly/Monthly/Eternal)
        composeRule.onNodeWithTag("nav_tab_tasks").performClick()
        composeRule.waitForIdle()
        val tasksTopBarBounds = composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed().getBoundsInRoot()
        val tasksBottomBarBounds = composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed().getBoundsInRoot()
        composeRule.onNodeWithTag("edge_fade_blur_top").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_bottom").assertIsDisplayed()
        val tasksTabBarBounds = composeRule.onNodeWithTag("tasks_hub_tab_tasks").assertIsDisplayed().getBoundsInRoot()
        assertTrue("Tasks tabs must start clear of TopBar", tasksTabBarBounds.top >= tasksTopBarBounds.bottom)

        val tasksBounds = composeRule.onNodeWithTag("tasks_lazy_list").assertIsDisplayed().getBoundsInRoot()
        assertTrue("Tasks list must extend behind TopBar (${tasksBounds.top} vs ${tasksTopBarBounds.bottom})", tasksBounds.top < tasksTopBarBounds.bottom)
        assertTrue("Tasks list must extend behind BottomBar (${tasksBounds.bottom} vs ${tasksBottomBarBounds.top})", tasksBounds.bottom > tasksBottomBarBounds.top)
        val eternalChipBounds = composeRule.onNodeWithTag("period_chip_eternal").performScrollTo().assertIsDisplayed().getBoundsInRoot()
        val roadmapFadeVeilBounds = composeRule.onNodeWithTag("roadmap_expanded_fade_veil").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Tasks expanded fade-to-liquid-glass (${roadmapFadeVeilBounds.bottom}) must extend below Daily/Weekly/Monthly/Eternal chips (${eternalChipBounds.bottom})",
            roadmapFadeVeilBounds.bottom >= eternalChipBounds.bottom + 30.dp
        )

        // Tasks -> Tasks Ledger: verify Completed/Uncompleted/All segments are removed & expanded fade veil covers period chips cleanly
        composeRule.onNodeWithTag("tasks_hub_tab_ledger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasks_ledger_screen").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("ledger_filter_completed").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("ledger_filter_uncompleted").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("ledger_filter_all").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("edge_fade_blur_top").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_bottom").assertIsDisplayed()
        composeRule.onNodeWithTag("ledger_expanded_fade_veil").assertIsDisplayed()
        val ledgerEternalBounds = composeRule.onNodeWithTag("ledger_period_eternal").performScrollTo().assertIsDisplayed().getBoundsInRoot()
        val ledgerFadeVeilBounds = composeRule.onNodeWithTag("ledger_expanded_fade_veil").getBoundsInRoot()
        assertTrue(
            "Ledger expanded fade-to-liquid-glass (${ledgerFadeVeilBounds.bottom}) must extend below Daily/Weekly/Monthly/Eternal chips (${ledgerEternalBounds.bottom})",
            ledgerFadeVeilBounds.bottom >= ledgerEternalBounds.bottom + 30.dp
        )
        assertEquals(0, composeRule.onAllNodesWithTag("ledger_back_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("ledger_screen_title").fetchSemanticsNodes().size)

        // Tasks -> Xp Logs: verify back button & Xp Logs title are removed
        composeRule.onNodeWithTag("tasks_hub_tab_xp_logs").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("xp_logs_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_top").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_bottom").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("xp_logs_back_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("xp_logs_title").fetchSemanticsNodes().size)

        // Roadmap screen starts cleanly clear of TopBar & keeps blur veils active
        composeRule.onNodeWithTag("nav_tab_roadmap").performClick()
        composeRule.waitForIdle()
        val rmTopBarBounds = composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed().getBoundsInRoot()
        composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_top").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_fade_blur_bottom").assertIsDisplayed()
        val rmStationsTabBounds = composeRule.onNodeWithTag("roadmap_tab_stations").assertIsDisplayed().getBoundsInRoot()
        assertTrue("Roadmap tabs must start clear of TopBar", rmStationsTabBounds.top >= rmTopBarBounds.bottom)
    }

    @Test
    fun dataEntryDialog_pendingHistoricalYearAndCount_autoSavesOnNewAndEdit() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_record_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("input_fullname").performTextReplacement("أمين التاريخي")
        composeRule.onNodeWithTag("input_historical_year").performScrollTo().performTextReplacement("2022")
        composeRule.onNodeWithTag("input_historical_count").performScrollTo().performTextReplacement("7")

        // Tap Save directly without tapping "+" first
        composeRule.onNodeWithTag("save_record_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("data_entry_overlay").fetchSemanticsNodes().isEmpty()
        }

        val savedPerson = runBlocking(Dispatchers.IO) {
            db.individualDao().getAllIndividualsList().firstOrNull { it.fullName == "أمين التاريخي" }
        }
        assertTrue("Person must be saved", savedPerson != null)
        val personId = savedPerson!!.id
        assertEquals(7, savedPerson.strikeCount)
        val initialHist = runBlocking(Dispatchers.IO) { repo.getHistoricalStrikesByYear(personId).toMap() }
        assertEquals(7, initialHist[2022])

        // Expand card & edit via DataEntryDialog, adding another pending historical year without tapping "+"
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("record_expand_btn_$personId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("record_expand_btn_$personId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("person_yearly_strikes_panel_$personId").assertIsDisplayed()
        composeRule.onNodeWithTag("record_edit_btn_$personId").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("data_entry_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("input_historical_year").performScrollTo().performTextReplacement("2021")
        composeRule.onNodeWithTag("input_historical_count").performScrollTo().performTextReplacement("3")
        composeRule.onNodeWithTag("save_record_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("data_entry_overlay").fetchSemanticsNodes().isEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking(Dispatchers.IO) { db.individualDao().getIndividualById(personId)?.strikeCount == 10 }
        }
        val editedHist = runBlocking(Dispatchers.IO) { repo.getHistoricalStrikesByYear(personId).toMap() }
        assertEquals(7, editedHist[2022])
        assertEquals(3, editedHist[2021])
    }

    @Test
    fun personHistoricalEditor_pendingInputWithoutAddTap_savesToDatabaseCleanly() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val personId = runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "خالد السنوي",
                    type = "M",
                    length = 24.0,
                    width = 11.0,
                    size = 264.0,
                    sScore = 75.0,
                    fScore = 75.0,
                    cScore = 75.0,
                    boost = 200.0
                )
            )
        }

        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("record_expand_btn_$personId").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("record_expand_btn_$personId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_edit_person_yearly_strikes_$personId").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("person_historical_strikes_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("person_hist_add_year_input").performScrollTo().performTextReplacement("2023")
        composeRule.onNodeWithTag("person_hist_add_count_input").performScrollTo().performTextReplacement("9")

        // Tap Save directly without clicking the "+" button
        composeRule.onNodeWithTag("btn_save_person_yearly_strikes").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("person_historical_strikes_dialog").fetchSemanticsNodes().isEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking(Dispatchers.IO) { db.individualDao().getIndividualById(personId)?.strikeCount == 9 }
        }
        val histMap = runBlocking(Dispatchers.IO) { repo.getHistoricalStrikesByYear(personId).toMap() }
        assertEquals(9, histMap[2023])
    }
}
