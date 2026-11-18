package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.ActiveShieldType
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.profile.PotionType
import com.example.data.profile.ProfileManager
import com.example.data.profile.ProfileMasterInventory
import com.example.data.profile.SkipKeyType
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
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class InventoryRedesignTest {

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

    private fun openInventoryTab() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_inventory").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inventory_hub").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun hubDisplaysProtectiveShieldsHeaderAndFourContainersInTwoByTwoGrid() {
        openInventoryTab()

        // 1. Protective Shields top section remains in its place
        composeRule.onNodeWithTag("inv_protective_shields_section").assertIsDisplayed()

        // 2. All 4 containers exist and are visible
        val shieldsNode = composeRule.onNodeWithTag("inv_cat_shields").assertIsDisplayed()
        val keysNode = composeRule.onNodeWithTag("inv_cat_keys").assertIsDisplayed()
        val potionsNode = composeRule.onNodeWithTag("inv_cat_potions").assertIsDisplayed()
        val titlesNode = composeRule.onNodeWithTag("inv_cat_titles").assertIsDisplayed()

        // 3. Verify 2x2 side-by-side grid geometry
        val headerBounds = composeRule.onNodeWithTag("inv_protective_shields_section").getUnclippedBoundsInRoot()
        val shieldsBounds = shieldsNode.getUnclippedBoundsInRoot()
        val keysBounds = keysNode.getUnclippedBoundsInRoot()
        val potionsBounds = potionsNode.getUnclippedBoundsInRoot()
        val titlesBounds = titlesNode.getUnclippedBoundsInRoot()

        assertTrue("Protective Shields header must sit above grid", headerBounds.bottom <= shieldsBounds.top)
        assertTrue(
            "Shields & Keys must be side-by-side on Row 1",
            abs(shieldsBounds.top.value - keysBounds.top.value) < 4f &&
                abs(shieldsBounds.left.value - keysBounds.left.value) > 40f
        )
        assertTrue(
            "Potions & Titles must be side-by-side on Row 2",
            abs(potionsBounds.top.value - titlesBounds.top.value) < 4f &&
                abs(potionsBounds.left.value - titlesBounds.left.value) > 40f
        )
        assertTrue("Row 2 must sit below Row 1", potionsBounds.top > shieldsBounds.bottom)

        // 4. Avatars container is visible in hub below Row 2
        composeRule.onNodeWithTag("inventory_hub").performScrollToNode(hasTestTag("inv_cat_avatars"))
        val avatarsBounds = composeRule.onNodeWithTag("inv_cat_avatars").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Avatars container sits below Row 2", avatarsBounds.top > potionsBounds.bottom)
    }

    @Test
    fun shieldsSubPageListsAllShieldsWithQuantitiesAndManualActivation() {
        val ctx = composeRule.activity
        FlameManager.grant1DayShield(ctx, 2)
        FlameManager.grant48hShield(ctx, 0)

        openInventoryTab()
        composeRule.onNodeWithTag("inv_cat_shields").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inv_subpage_shields").fetchSemanticsNodes().isNotEmpty()
        }

        // All shields appear even when some have 0 quantity
        composeRule.onNodeWithTag("inv_shield_7day_luxury").assertIsDisplayed()
        composeRule.onNodeWithTag("inv_shield_24h").assertIsDisplayed()
        composeRule.onNodeWithTag("inv_shield_48h").assertIsDisplayed()
        composeRule.onNodeWithTag("inv_subpage_shields").performScrollToNode(hasTestTag("inv_shield_72h"))
        composeRule.onNodeWithTag("inv_shield_72h").assertIsDisplayed()

        // 24h Shield has qty=2 -> Activate button enabled; 48h & 72h have qty=0 -> disabled
        composeRule.onNodeWithTag("inv_activate_shield_48h").assertIsNotEnabled()
        composeRule.onNodeWithTag("inv_activate_shield_72h").assertIsNotEnabled()
        composeRule.onNodeWithTag("inv_activate_shield_24h").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertTrue(FlameManager.isStreakShieldActive.value)
        assertEquals(ActiveShieldType.SHIELD_24H, FlameManager.activeShieldType.value)
        assertEquals(1, FlameManager.available1DayShields.value)

        // Back button returns to 4-container hub
        composeRule.onNodeWithTag("inv_subpage_back_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inventory_hub").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun keysSubPageShowsOnlyOwnedKeysAndEmptyStateWhenNoneOwned() {
        val ctx = composeRule.activity

        // 1. Empty state when user owns 0 keys
        openInventoryTab()
        composeRule.onNodeWithTag("inv_cat_keys").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inv_subpage_keys").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("inv_keys_empty_state").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_key_wooden").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("inv_key_bronze").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("inv_key_silver").fetchSemanticsNodes().size)

        // 2. Grant ONLY Bronze keys -> ONLY Bronze appears; Wooden & Silver remain hidden
        ProfileManager.grantSkipKeys(ctx, SkipKeyType.BRONZE, 3)
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("inv_keys_empty_state").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("inv_key_bronze").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_key_wooden").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("inv_key_silver").fetchSemanticsNodes().size)
    }

    @Test
    fun potionsSubPageShowsOnlyOwnedPotionsAndAllowsManualActivation() {
        val ctx = composeRule.activity

        // 1. Empty state when 0 potions owned
        openInventoryTab()
        composeRule.onNodeWithTag("inv_cat_potions").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inv_subpage_potions").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("inv_potions_empty_state").assertIsDisplayed()

        // 2. Grant Pulse Elixir only -> ONLY Pulse Elixir shows up with Activate button
        ProfileManager.grantPotion(ctx, PotionType.PULSE_ELIXIR, 1, notifyRoadmapTask = false)
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("inv_potions_empty_state").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("inv_potion_pulse").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_potion_shadow").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("inv_potion_cryo").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("inv_potion_eternal_resurgence").fetchSemanticsNodes().size)

        // Activate Pulse Elixir -> consumes bottle & activates 3 uses
        composeRule.onNodeWithTag("inv_activate_potion_pulse").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals(0, ProfileManager.pulseElixirCount.value)
        assertEquals(3, ProfileManager.pulseElixirUsesLeft.value)
        // Remains visible as active status, with activate button hidden
        composeRule.onNodeWithTag("inv_potion_pulse").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_activate_potion_pulse").fetchSemanticsNodes().size)
    }

    @Test
    fun titlesSubPageListsAllGameTitlesAndEquipsOnlyUnlockedTitles() {
        val ctx = composeRule.activity
        ProfileManager.unlockTitle(ctx, 25)

        openInventoryTab()
        composeRule.onNodeWithTag("inv_cat_titles").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inv_subpage_titles").fetchSemanticsNodes().isNotEmpty()
        }

        // Starter title #1 is equipped initially
        composeRule.onNodeWithTag("inv_title_card_1").assertIsDisplayed()
        composeRule.onNodeWithTag("inv_title_active_1").assertIsDisplayed()

        // Locked title (#2) shows Locked badge and NO Activate button
        composeRule.onNodeWithTag("inv_subpage_titles").performScrollToNode(hasTestTag("inv_title_card_2"))
        composeRule.onNodeWithTag("inv_title_locked_2").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_activate_title_2").fetchSemanticsNodes().size)

        // Unlocked title (#25) shows Activate button -> clicking equips it
        composeRule.onNodeWithTag("inv_subpage_titles").performScrollToNode(hasTestTag("inv_title_card_25"))
        composeRule.onNodeWithTag("inv_activate_title_25").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals(25, ProfileManager.equippedTitle.value.id)
        composeRule.onNodeWithTag("inv_title_active_25").assertIsDisplayed()

        // Final milestone title #100 is also present in the catalog
        composeRule.onNodeWithTag("inv_subpage_titles")
            .performScrollToNode(hasTestTag("inv_title_card_${ProfileMasterInventory.MILESTONE_PEAK_TITLE.id}"))
        composeRule.onNodeWithTag("inv_title_card_${ProfileMasterInventory.MILESTONE_PEAK_TITLE.id}").assertIsDisplayed()
    }

    @Test
    fun oneSecondLongPressOpensTooltipWithExplanationAndHowToObtain() {
        openInventoryTab()
        composeRule.onNodeWithTag("inv_cat_shields").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inv_subpage_shields").fetchSemanticsNodes().isNotEmpty()
        }

        // Pressing for < 1 second (600ms) must NOT open tooltip sheet
        composeRule.onNodeWithTag("inv_shield_24h").performTouchInput {
            down(center)
            advanceEventTime(600L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("inventory_tooltip_sheet").fetchSemanticsNodes().size)

        // Holding for 1 second (1050ms) MUST open tooltip sheet with How To Obtain section
        composeRule.onNodeWithTag("inv_shield_24h").performTouchInput {
            down(center)
            advanceEventTime(1050L)
        }
        composeRule.mainClock.advanceTimeBy(1100L)
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inventory_tooltip_sheet").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("inv_shield_24h").performTouchInput { up() }
        composeRule.onNodeWithTag("inventory_tooltip_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("inv_tooltip_how_to_obtain").assertIsDisplayed()
    }

    @Test
    fun avatarsSubPageListsAllGameAvatarsAndEquipsUnlockedAvatars() {
        val ctx = composeRule.activity
        ProfileManager.unlockAvatar(ctx, "av_gabriel_wings")

        openInventoryTab()
        composeRule.onNodeWithTag("inventory_hub").performScrollToNode(hasTestTag("inv_cat_avatars"))
        composeRule.onNodeWithTag("inv_cat_avatars").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inv_subpage_avatars").fetchSemanticsNodes().isNotEmpty()
        }

        // Starter avatar is equipped initially
        composeRule.onNodeWithTag("inv_avatar_card_av_core_node").assertIsDisplayed()
        composeRule.onNodeWithTag("inv_avatar_active_av_core_node").assertIsDisplayed()

        // Locked avatar shows Locked pill and NO Activate button
        composeRule.onNodeWithTag("inv_subpage_avatars").performScrollToNode(hasTestTag("inv_avatar_card_av_std_circuit"))
        composeRule.onNodeWithTag("inv_avatar_locked_av_std_circuit").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_activate_avatar_av_std_circuit").fetchSemanticsNodes().size)

        // Unlocked avatar shows Activate button -> clicking equips it
        composeRule.onNodeWithTag("inv_subpage_avatars").performScrollToNode(hasTestTag("inv_avatar_card_av_gabriel_wings"))
        composeRule.onNodeWithTag("inv_activate_avatar_av_gabriel_wings").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals("av_gabriel_wings", ProfileManager.equippedAvatar.value.id)
        composeRule.onNodeWithTag("inv_avatar_active_av_gabriel_wings").assertIsDisplayed()

        // Final legendary avatar in catalog is also listed
        composeRule.onNodeWithTag("inv_subpage_avatars").performScrollToNode(hasTestTag("inv_avatar_card_av_eternal_flame"))
        composeRule.onNodeWithTag("inv_avatar_card_av_eternal_flame").assertIsDisplayed()

        // 1-second long press on avatar opens tooltip with How to Obtain
        composeRule.onNodeWithTag("inv_avatar_card_av_eternal_flame").performTouchInput {
            down(center)
            advanceEventTime(1050L)
        }
        composeRule.mainClock.advanceTimeBy(1100L)
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inventory_tooltip_sheet").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("inv_avatar_card_av_eternal_flame").performTouchInput { up() }
        composeRule.onNodeWithTag("inv_tooltip_how_to_obtain").assertIsDisplayed()
    }
}
