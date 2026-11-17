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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddIndividualCrashTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshAppStartsEmptyAndAddingNewIndividualWorksWithoutCrash() {
        composeRule.waitForIdle()

        // 1. Verify no default individuals or sample-seed button appear on startup
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("empty_records_card")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("empty_records_card").assertIsDisplayed()
        composeRule.onNodeWithText("لا توجد سجلات بعد").assertIsDisplayed()
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
}
