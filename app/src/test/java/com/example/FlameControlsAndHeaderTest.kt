package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.profile.PotionType
import com.example.data.profile.ProfileManager
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.XpCalculationEngine
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class FlameControlsAndHeaderTest {

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

    private fun openFlameScreen() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("header_flame_btn").performTouchInput { click() }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("flame_hero_card").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollToManualControls() {
        composeRule.onNodeWithTag("flame_lazy_column")
            .performScrollToNode(hasTestTag("btn_deflame"))
        composeRule.waitForIdle()
    }

    @Test
    fun flameScreen_hidesBackArrow_shieldTimer_andSupernovaPillAboveFlame() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        FlameManager.setScore(ctx, 750.0, reason = "SIMULATION")
        FlameManager.grant1DayShield(ctx, 1)
        FlameManager.activate1DayShield(ctx)

        openFlameScreen()

        composeRule.onNodeWithTag("flame_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("flame_hero_canvas").assertIsDisplayed()

        // Back arrow, Shield remaining timer badge, and SUPERNOVA / OVERLOAD (> 600) pill are removed
        assertEquals(0, composeRule.onAllNodesWithTag("flame_back_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("flame_shield_status_badge").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("SUPERNOVA / OVERLOAD (> 600)", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("مستعر أعظم / حمل فائق", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun deflameButton_displaysRealEffectivePointsWithPulseElixirAndDeductsCorrectly() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        FlameManager.setScore(ctx, 100.0, reason = "SIMULATION")

        openFlameScreen()
        scrollToManualControls()

        // Without Pulse Elixir -> shows -3
        composeRule.onNodeWithTag("deflame_points_badge", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("deflame_points_badge", useUnmergedTree = true).assertTextContains("-3", substring = true)

        // Activate Pulse Elixir (+5 bonus for 3 uses -> total -8)
        ProfileManager.grantPotion(ctx, PotionType.PULSE_ELIXIR, 1, notifyRoadmapTask = false)
        ProfileManager.usePulseElixir(ctx)
        composeRule.waitForIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeRule.onNodeWithTag("deflame_points_badge", useUnmergedTree = true).assertTextContains("-8", substring = true)

        // 3 boosted taps: 100 -> 92 -> 84 -> 76
        composeRule.onNodeWithTag("btn_deflame").performClick()
        composeRule.waitForIdle()
        assertEquals(92.0, FlameManager.flameScore.value, 0.001)

        composeRule.onNodeWithTag("btn_deflame").performClick()
        composeRule.waitForIdle()
        assertEquals(84.0, FlameManager.flameScore.value, 0.001)

        composeRule.onNodeWithTag("btn_deflame").performClick()
        composeRule.waitForIdle()
        assertEquals(76.0, FlameManager.flameScore.value, 0.001)

        // Once 3 uses expire, badge reverts to -3
        assertEquals(0, ProfileManager.pulseElixirUsesLeft.value)
        composeRule.onNodeWithTag("deflame_points_badge", useUnmergedTree = true).assertTextContains("-3", substring = true)
    }

    @Test
    fun girlAdjustment_updatesDisplayedPointsWithoutImmediateDeduction_andNextTapDeductsNewValue() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        FlameManager.setScore(ctx, 100.0, reason = "SIMULATION")

        openFlameScreen()
        scrollToManualControls()

        // Default is -3
        composeRule.onNodeWithTag("girl_points_badge", useUnmergedTree = true).assertTextContains("-3", substring = true)

        // Updating Girl deduction amount (as invoked by Apply Deduction in DeflameAdjustmentDialog)
        FlameManager.setGirlDeductionPoints(ctx, 10.0)
        composeRule.waitForIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Must NOT deduct immediately; score stays 100.0 while Girl deduction becomes 10.0
        assertEquals(100.0, FlameManager.flameScore.value, 0.001)
        assertEquals(10.0, FlameManager.girlDeductionPoints.value, 0.001)
        composeRule.onNodeWithTag("girl_points_badge", useUnmergedTree = true).assertTextContains("-10", substring = true)

        // Next tap deducts the newly configured 10 points (100 -> 90 -> 80)
        composeRule.onNodeWithTag("btn_girl").performClick()
        composeRule.waitForIdle()
        assertEquals(90.0, FlameManager.flameScore.value, 0.001)

        composeRule.onNodeWithTag("btn_girl").performClick()
        composeRule.waitForIdle()
        assertEquals(80.0, FlameManager.flameScore.value, 0.001)

        // Verify persistence across re-initialization
        FlameManager.resetForTesting(ctx)
        assertEquals(10.0, FlameManager.girlDeductionPoints.value, 0.001)
    }
}
