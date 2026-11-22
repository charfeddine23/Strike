package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.profile.ProfileManager
import com.example.data.profile.ProfileMasterInventory
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.TaskPools
import com.example.data.roadmap.XpCalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class ProfileTitleResetRegrindTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(object : TestWatcher() {
        override fun starting(description: Description) = resetAll()
        override fun finished(description: Description) = resetAll()

        private fun resetAll() {
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
    fun profileManager_resetTitleForRegrind_tracksXpAndPreventsStationBypass() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()

        // Unlock Title #2 (Station 10) and accumulate XP
        val station10CumXp = TaskPools.getCumulativeXpForStation(10)
        RoadmapManager.addXp(ctx, station10CumXp)
        ProfileManager.onStationUnlocked(ctx, 10)
        ProfileManager.equipTitle(ctx, 2)
        assertTrue(ProfileManager.unlockedTitleIds.value.contains(2))
        assertEquals(2, ProfileManager.equippedTitle.value.id)

        // Reset Title #2 for re-grind
        val target = ProfileManager.resetTitleForReGrind(ctx, 2)
        assertNotNull(target)
        assertFalse(ProfileManager.unlockedTitleIds.value.contains(2))
        // Equipped title safely falls back to Title #1
        assertEquals(1, ProfileManager.equippedTitle.value.id)
        assertTrue(ProfileManager.titleRegrindTargets.value.containsKey(2))

        // Unlocking higher stations directly does NOT bypass the active re-grind challenge for Title #2
        ProfileManager.onStationUnlocked(ctx, 20)
        assertFalse(ProfileManager.unlockedTitleIds.value.contains(2))

        // Earning partial XP does not unlock early
        val reqGain = target!!.requiredGain
        RoadmapManager.addXp(ctx, reqGain - 100L)
        assertFalse(ProfileManager.unlockedTitleIds.value.contains(2))

        // Completing remaining XP unlocks Title #2 again and clears the challenge
        RoadmapManager.addXp(ctx, 100L)
        assertTrue(ProfileManager.unlockedTitleIds.value.contains(2))
        assertFalse(ProfileManager.titleRegrindTargets.value.containsKey(2))

        // Resetting all unlocked titles (3, 2, 1) falls back first to #3 when remaining, then to NO_TITLE_PLACEHOLDER (0)
        ProfileManager.resetTitleForReGrind(ctx, 2)
        ProfileManager.resetTitleForReGrind(ctx, 1)
        assertEquals(3, ProfileManager.equippedTitle.value.id)
        ProfileManager.resetTitleForReGrind(ctx, 3)
        assertEquals(0, ProfileManager.equippedTitle.value.id)
        assertFalse(ProfileManager.unlockedTitleIds.value.contains(1))

        // Also test Milestone Peak Title (#100) re-grind via Flame points
        FlameManager.setScore(ctx, 20_000.0)
        assertTrue(ProfileManager.unlockedTitleIds.value.contains(ProfileMasterInventory.MILESTONE_PEAK_TITLE.id))
        ProfileManager.resetTitleForReGrind(ctx, ProfileMasterInventory.MILESTONE_PEAK_TITLE.id)
        assertFalse(ProfileManager.unlockedTitleIds.value.contains(ProfileMasterInventory.MILESTONE_PEAK_TITLE.id))
        FlameManager.addPoints(ctx, 4_900.0, "TEST", "Partial flame regrind")
        assertFalse(ProfileManager.unlockedTitleIds.value.contains(ProfileMasterInventory.MILESTONE_PEAK_TITLE.id))
        FlameManager.addPoints(ctx, 200.0, "TEST", "Finish flame regrind")
        assertTrue(ProfileManager.unlockedTitleIds.value.contains(ProfileMasterInventory.MILESTONE_PEAK_TITLE.id))
    }

    @Test
    fun inventoryTitles_hold10sDoesNotResetTitle() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_inventory").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inv_cat_titles").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("inv_title_card_1").performScrollTo().performTouchInput {
            down(center)
        }
        composeRule.mainClock.advanceTimeBy(10_200L)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inv_title_card_1").performTouchInput {
            up()
        }
        composeRule.waitForIdle()

        assertTrue(
            "Holding title inside Inventory must NEVER reset it",
            ProfileManager.unlockedTitleIds.value.contains(1)
        )
        assertEquals(0, composeRule.onAllNodesWithTag("profile_title_reset_countdown_1").fetchSemanticsNodes().size)
    }

    @Test
    fun profileScreen_hold10sResetsTitleAndReunlocksAfterEarningXp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_avatar_header_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_tab_titles").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_titles_hold_reset_hint").assertIsDisplayed()

        composeRule.onNodeWithTag("profile_lazy_list").performScrollToNode(hasTestTag("profile_title_card_5"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_title_card_1").assertIsDisplayed()

        // Short hold (~3 seconds) must NOT reset Title #1
        composeRule.onNodeWithTag("profile_title_card_1").performTouchInput {
            down(center)
        }
        composeRule.mainClock.advanceTimeBy(3_000L)
        composeRule.waitForIdle()
        val countdownNodes = composeRule.onAllNodesWithTag("profile_title_reset_countdown_1", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue(
            "Expected countdown node after 3s hold, found=${countdownNodes.size}, cardBounds=${composeRule.onNodeWithTag("profile_title_card_1").fetchSemanticsNode().boundsInRoot}",
            countdownNodes.isNotEmpty()
        )
        composeRule.onNodeWithTag("profile_title_reset_countdown_1", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("profile_title_reset_progress_1", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("profile_title_card_1").performTouchInput {
            up()
        }
        composeRule.waitForIdle()
        assertTrue("Releasing before 10 seconds must keep title unlocked", ProfileManager.unlockedTitleIds.value.contains(1))

        // Full 10-second continuous hold MUST reset Title #1 and show re-grind progress
        composeRule.onNodeWithTag("profile_title_card_1").performTouchInput {
            down(center)
        }
        composeRule.mainClock.advanceTimeBy(10_200L)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_title_card_1").performTouchInput {
            up()
        }
        composeRule.waitForIdle()

        assertFalse("10-second hold in Profile must lock Title #1", ProfileManager.unlockedTitleIds.value.contains(1))
        assertEquals(0, ProfileManager.equippedTitle.value.id)
        composeRule.onNodeWithTag("profile_lazy_list").performScrollToNode(hasTestTag("profile_equipped_title_text"))
        composeRule.onNodeWithTag("profile_equipped_title_text").assertTextContains("بدون لقب", substring = false)
        composeRule.onNodeWithTag("profile_lazy_list").performScrollToNode(hasTestTag("profile_title_regrind_1"))
        composeRule.onNodeWithTag("profile_title_regrind_1").assertIsDisplayed()

        // Earning the full re-grind XP automatically re-unlocks Title #1
        RoadmapManager.addXp(ctx, TaskPools.ROADMAP_LEVEL_1_XP)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()

        assertTrue(ProfileManager.unlockedTitleIds.value.contains(1))
        assertEquals(1, ProfileManager.equippedTitle.value.id)
        composeRule.onNodeWithTag("profile_lazy_list").performScrollToNode(hasTestTag("profile_title_card_1"))
        assertEquals(0, composeRule.onAllNodesWithTag("profile_title_regrind_1").fetchSemanticsNodes().size)
    }
}
