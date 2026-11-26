package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.repository.IndividualRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
class AddIndividualCrashTest {

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
    fun freshAppStartsEmptyAndAddingNewIndividualWorksWithoutCrash() {
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()

        // 1. Verify no default individuals or sample-seed button appear on startup
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("empty_records_card")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("empty_records_card").performScrollTo().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("seed_sample_btn").fetchSemanticsNodes().size)

        // 2. Open the in-tree Data Entry overlay via '+' FAB
        composeRule.onNodeWithTag("add_record_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("data_entry_overlay").assertIsDisplayed()

        // 3. Test contacts permission dialog gracefully handles denied permission without crashing
        composeRule.onNodeWithTag("btn_import_from_contacts").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("contacts_permission_dialog").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("btn_contacts_perm_cancel").performClick()
            composeRule.waitForIdle()
        }

        // 4. Fill form including Arabic localized decimal digits and historical strike entry
        composeRule.onNodeWithTag("input_fullname").performTextInput("أحمد علي")
        composeRule.onNodeWithTag("input_phone_number").performTextInput("+213555001122")
        composeRule.onNodeWithTag("input_length").performScrollTo().performTextReplacement("٢٤٫٥")
        composeRule.onNodeWithTag("input_width").performScrollTo().performTextReplacement("12.0")
        composeRule.onNodeWithTag("input_sscore").performScrollTo().performTextReplacement("85.0")
        composeRule.onNodeWithTag("input_fscore").performScrollTo().performTextReplacement("80.0")
        composeRule.onNodeWithTag("input_cscore").performScrollTo().performTextReplacement("90.0")

        composeRule.onNodeWithTag("add_historical_entry_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 5. Save and verify overlay dismisses cleanly and individual renders on Dashboard
        composeRule.onNodeWithTag("save_record_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("data_entry_overlay").fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("أحمد علي").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("أحمد علي").assertIsDisplayed()

        // 6. Expand the individual's card in Records and verify direct strike-addition button (+1) is removed
        val mainActivity = composeRule.activity
        val savedId = runBlocking(Dispatchers.IO) {
            AppDatabase.getDatabase(mainActivity).individualDao().getAllIndividualsList().first().id
        }
        composeRule.onNodeWithTag("record_expand_btn_$savedId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("record_strike_count_$savedId").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("quick_strike_$savedId").fetchSemanticsNodes().size)
    }

    @Test
    fun updatingExistingIndividualPreservesStrikeEvents() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val individualDao = db.individualDao()
            val strikeDao = db.strikeEventDao()
            val repo = IndividualRepository(individualDao, strikeDao)

            val insertedId = repo.save(
                Individual(
                    fullName = "Zayd",
                    type = "M",
                    length = 20.0,
                    width = 10.0,
                    size = 200.0,
                    sScore = 80.0,
                    fScore = 80.0,
                    cScore = 80.0,
                    boost = 242.0,
                    strikeCount = 1
                )
            )
            strikeDao.insertEvent(
                StrikeEvent(
                    individualId = insertedId,
                    individualName = "Zayd",
                    year = 2026,
                    date = "2026-11-17",
                    time = "10:00:00",
                    count = 1,
                    strikeType = "Manual",
                    performanceValue = 242.0
                )
            )

            // Update existing individual via save() -> must use UPDATE and NOT cascade-delete strike_events
            val existing = repo.getIndividualById(insertedId)!!
            repo.save(existing.copy(fullName = "Zayd Updated"))

            val eventsAfterUpdate = strikeDao.getEventsForIndividual(insertedId).first()
            assertEquals(1, eventsAfterUpdate.size)
            assertEquals("Zayd Updated", repo.getIndividualById(insertedId)?.fullName)
        } finally {
            db.close()
        }
    }

    @Test
    fun dataEntryDialog_validatesEmptyNameAndEditsExistingRecordCleanly() {
        enterMainAppFromHeatmap()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_record_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("data_entry_overlay").assertIsDisplayed()

        // Saving with empty name displays validation error & stays open
        composeRule.onNodeWithTag("save_record_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("data_entry_overlay").assertIsDisplayed()
        composeRule.waitForIdle()
        val errorNodes = composeRule.onAllNodesWithText("الاسم واللقب حقل إلزامي", substring = true)
        org.junit.Assert.assertTrue(errorNodes.fetchSemanticsNodes().isNotEmpty())
        errorNodes[0].performScrollTo().assertIsDisplayed()

        // Fill name and save
        composeRule.onNodeWithTag("input_fullname").performScrollTo().performTextInput("سارة")
        composeRule.onNodeWithTag("save_record_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("data_entry_overlay").fetchSemanticsNodes().isEmpty()
        }

        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val savedId = runBlocking(Dispatchers.IO) {
            AppDatabase.getDatabase(ctx).individualDao().getAllIndividualsList().first().id
        }

        // Expand record card and click Edit icon
        composeRule.onNodeWithTag("record_expand_btn_$savedId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("record_edit_btn_$savedId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("data_entry_overlay").assertIsDisplayed()

        // Edit name and confirm via bottom Save button (previously hidden/broken in edit mode)
        composeRule.onNodeWithTag("input_fullname").performScrollTo().performTextReplacement("سارة المعدلة")
        composeRule.onNodeWithTag("save_record_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("data_entry_overlay").fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("سارة المعدلة").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("سارة المعدلة").assertIsDisplayed()
    }
}
