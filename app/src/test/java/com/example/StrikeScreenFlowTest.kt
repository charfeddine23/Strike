package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Individual
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class StrikeScreenFlowTest {

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
    fun strikeTabHasNoExtrasAndTotalStrikesUpdatesOnPickAndConfirm() {
        val activity = composeRule.activity
        val db = AppDatabase.getDatabase(activity)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        var id1 = 0L
        var latestEventId = 0L
        runBlocking(Dispatchers.IO) {
            id1 = repo.save(
                Individual(
                    fullName = "أحمد",
                    type = "M",
                    length = 20.0,
                    width = 10.0,
                    size = 200.0,
                    sScore = 80.0,
                    fScore = 80.0,
                    cScore = 80.0,
                    boost = 242.0,
                    strikeCount = 0
                )
            )
            val id2 = repo.save(
                Individual(
                    fullName = "سارة",
                    type = "G",
                    length = 18.0,
                    width = 9.0,
                    size = 162.0,
                    sScore = 70.0,
                    fScore = 75.0,
                    cScore = 80.0,
                    boost = 200.0,
                    strikeCount = 0
                )
            )
            repeat(2) { repo.incrementStrike(id1, 1) }
            repeat(3) { repo.incrementStrike(id2, 1) }
        }

        // Navigate to Strike screen via bottom bar
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("heatmap_swipe_up_hide_btn").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("nav_tab_strike").performClick()
        composeRule.waitForIdle()

        // 1. Verify extras (Select Individual + Smart Search) are gone on Strike tab
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("strike_button").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, composeRule.onAllNodesWithTag("select_individual_section").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("smart_search_banner").fetchSemanticsNodes().size)

        // 2. Verify center of Strike button displays total strikes (2 + 3 = 5)
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val nodes = composeRule.onAllNodesWithTag("strike_button_total_count", useUnmergedTree = true)
                .fetchSemanticsNodes()
            val text = nodes.firstOrNull()?.config?.getOrElse(SemanticsProperties.Text) { emptyList() }
                ?.joinToString("") { it.text }
            if (text == "5") true else {
                Thread.sleep(20)
                false
            }
        }
        composeRule.onNodeWithTag("strike_button_total_count", useUnmergedTree = true)
            .assertTextEquals("5")

        // 3. Tap Strike button -> opens picker dialog; Confirm disabled until individual chosen
        composeRule.onNodeWithTag("strike_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("strike_target_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_confirm_strike_target").assertIsNotEnabled()

        // 4. Pick first individual -> Confirm opens location picker -> Confirm registers +1 strike with coordinates
        composeRule.onNodeWithTag("strike_picker_item_$id1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_confirm_strike_target").assertIsEnabled().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("first_strike_picker_dialog").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("picker_confirm_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("first_strike_picker_dialog").fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithTag("strike_target_picker_dialog").fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val nodes = composeRule.onAllNodesWithTag("strike_button_total_count", useUnmergedTree = true)
                .fetchSemanticsNodes()
            val text = nodes.firstOrNull()?.config?.getOrElse(SemanticsProperties.Text) { emptyList() }
                ?.joinToString("") { it.text }
            if (text == "6") true else {
                Thread.sleep(25)
                false
            }
        }

        // Total strikes center counter updated from 5 -> 6 and location persisted on latest event
        composeRule.onNodeWithTag("strike_button_total_count", useUnmergedTree = true)
            .assertTextEquals("6")

        // 5. Verify deleting from Recent Strikes opens Biometric Confirmation dialog & blocks unverified delete
        var latestEvent: com.example.data.model.StrikeEvent? = null
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val candidate = runBlocking(Dispatchers.IO) { repo.allStrikeEvents.first().firstOrNull() }
            if (candidate != null && candidate.hasLocation &&
                composeRule.onAllNodesWithTag("delete_strike_btn_${candidate.id}").fetchSemanticsNodes().isNotEmpty()
            ) {
                latestEvent = candidate
                true
            } else {
                false
            }
        }
        latestEventId = latestEvent!!.id
        org.junit.Assert.assertTrue("New strike from flow must carry map coordinates", latestEvent!!.hasLocation)
        composeRule.onNodeWithTag("delete_strike_btn_$latestEventId").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("delete_strike_biometric_dialog").assertIsDisplayed()

        // Tapping verify never deletes without biometric success callback
        composeRule.onNodeWithTag("btn_verify_fingerprint_delete").performClick()
        composeRule.waitForIdle()
        assertEquals(6, runBlocking(Dispatchers.IO) { repo.allTimeStrikesCountFlow.first() })

        // Dismiss cancel -> dialog closes and total stays 6
        composeRule.onNodeWithTag("btn_cancel_delete_strike").performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("delete_strike_biometric_dialog").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("strike_button_total_count", useUnmergedTree = true)
            .assertTextEquals("6")
    }
}
