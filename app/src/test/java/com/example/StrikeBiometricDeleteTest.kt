package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Individual
import com.example.data.repository.IndividualRepository
import com.example.ui.screens.StrikeScreen
import com.example.ui.viewmodel.IndividualViewModel
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
class StrikeBiometricDeleteTest {

    private val composeRule = createComposeRule()

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
    fun biometricDeleteBlocksOnFailureAndDeletesOnSuccess() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())
        val viewModel = IndividualViewModel(app, repo)

        var eventId1 = 0L
        runBlocking(Dispatchers.IO) {
            val personId = repo.save(
                Individual(
                    fullName = "خالد",
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
            eventId1 = repo.incrementStrike(personId, 1)
            repo.incrementStrike(personId, 1)
        }

        var shouldSucceed = false
        composeRule.setContent {
            StrikeScreen(
                viewModel = viewModel,
                onAuthenticateDelete = { _, _, _, onSuccess, onError, _ ->
                    if (shouldSucceed) {
                        onSuccess()
                    } else {
                        onError("بصمة غير مطابقة")
                    }
                }
            )
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val nodes = composeRule.onAllNodesWithTag("strike_button_total_count", useUnmergedTree = true)
                .fetchSemanticsNodes()
            val text = nodes.firstOrNull()?.config?.getOrElse(SemanticsProperties.Text) { emptyList() }
                ?.joinToString("") { it.text }
            if (text == "2") true else {
                Thread.sleep(20)
                false
            }
        }

        // Open biometric delete dialog
        composeRule.onNodeWithTag("delete_strike_btn_$eventId1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("delete_strike_biometric_dialog").assertIsDisplayed()

        // Failed biometric attempt shows error and does NOT delete
        composeRule.onNodeWithTag("btn_verify_fingerprint_delete").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_delete_error_msg").assertIsDisplayed()
        assertEquals(2, runBlocking(Dispatchers.IO) { repo.allTimeStrikesCountFlow.first() })

        // Successful biometric attempt deletes event and updates Strike button total count 2 -> 1
        shouldSucceed = true
        composeRule.onNodeWithTag("btn_verify_fingerprint_delete").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val count = runBlocking(Dispatchers.IO) { repo.allTimeStrikesCountFlow.first() }
            if (count == 1) true else {
                Thread.sleep(20)
                false
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val nodes = composeRule.onAllNodesWithTag("strike_button_total_count", useUnmergedTree = true)
                .fetchSemanticsNodes()
            val text = nodes.firstOrNull()?.config?.getOrElse(SemanticsProperties.Text) { emptyList() }
                ?.joinToString("") { it.text }
            if (text == "1") true else {
                Thread.sleep(20)
                false
            }
        }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("delete_strike_biometric_dialog").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("strike_button_total_count", useUnmergedTree = true)
            .assertTextEquals("1")
    }
}
