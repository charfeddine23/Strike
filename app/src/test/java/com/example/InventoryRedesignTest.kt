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
        if (composeRule.onAllNodesWithTag("heatmap_enter_btn").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("heatmap_enter_btn").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("nav_tab_inventory").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inventory_hub").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun hubDisplaysFourContainersInTwoByTwoGridWithoutProtectiveShieldsHeader() {
        openInventoryTab()

        // 1. Protective Shields top header container + right finger hint button are completely removed
        assertEquals(0, composeRule.onAllNodesWithTag("inv_protective_shields_section").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("inventory_longpress_hint_btn").fetchSemanticsNodes().size)

        // 2. All 4 containers exist and are visible
        val shieldsNode = composeRule.onNodeWithTag("inv_cat_shields").assertIsDisplayed()
        val keysNode = composeRule.onNodeWithTag("inv_cat_keys").assertIsDisplayed()
        val potionsNode = composeRule.onNodeWithTag("inv_cat_potions").assertIsDisplayed()
        val titlesNode = composeRule.onNodeWithTag("inv_cat_titles").assertIsDisplayed()

        // 3. Verify 2x2 side-by-side grid geometry
        val shieldsBounds = shieldsNode.getUnclippedBoundsInRoot()
        val keysBounds = keysNode.getUnclippedBoundsInRoot()
        val potionsBounds = potionsNode.getUnclippedBoundsInRoot()
        val titlesBounds = titlesNode.getUnclippedBoundsInRoot()

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
    fun shieldsSubPageListsAllShieldsInTwoColumnGridWithManualActivation() {
        val ctx = composeRule.activity
        FlameManager.grant1DayShield(ctx, 2)
        FlameManager.grant48hShield(ctx, 0)

        openInventoryTab()
        composeRule.onNodeWithTag("inv_cat_shields").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inv_subpage_shields").fetchSemanticsNodes().isNotEmpty()
        }

        // All 4 shields appear in a 2-column grid (2 cards per row)
        val s7dBounds = composeRule.onNodeWithTag("inv_shield_7day_luxury").assertIsDisplayed().getUnclippedBoundsInRoot()
        val s24hBounds = composeRule.onNodeWithTag("inv_shield_24h").assertIsDisplayed().getUnclippedBoundsInRoot()
        val s48hBounds = composeRule.onNodeWithTag("inv_shield_48h").assertIsDisplayed().getUnclippedBoundsInRoot()
        val s72hBounds = composeRule.onNodeWithTag("inv_shield_72h").assertIsDisplayed().getUnclippedBoundsInRoot()

        assertTrue(
            "7-Day and 24h shields sit side-by-side in row 1",
            abs(s7dBounds.top.value - s24hBounds.top.value) < 4f &&
                abs(s7dBounds.left.value - s24hBounds.left.value) > 40f
        )
        assertTrue(
            "48h and 72h shields sit side-by-side in row 2",
            abs(s48hBounds.top.value - s72hBounds.top.value) < 4f &&
                abs(s48hBounds.left.value - s72hBounds.left.value) > 40f
        )
        assertTrue("Row 2 sits below Row 1", s48hBounds.top > s7dBounds.bottom)

        // 24h Shield has qty=2 -> Activate button enabled; 48h & 72h have qty=0 -> disabled
        composeRule.onNodeWithTag("inv_activate_shield_48h").assertIsNotEnabled()
        composeRule.onNodeWithTag("inv_activate_shield_72h").assertIsNotEnabled()
        composeRule.onNodeWithTag("inv_activate_shield_24h").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertTrue(FlameManager.isStreakShieldActive.value)
        assertEquals(ActiveShieldType.SHIELD_24H, FlameManager.activeShieldType.value)
        assertEquals(1, FlameManager.available1DayShields.value)

        // Back button returns to hub
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

        // 2. Grant Bronze keys -> ONLY Bronze appears; Wooden & Silver remain hidden
        ProfileManager.grantSkipKeys(ctx, SkipKeyType.BRONZE, 3)
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("inv_keys_empty_state").fetchSemanticsNodes().size)
        val bronzeBounds = composeRule.onNodeWithTag("inv_key_bronze").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_key_wooden").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("inv_key_silver").fetchSemanticsNodes().size)

        // 3. Grant Silver keys too -> Bronze and Silver sit side-by-side in 2-column grid
        ProfileManager.grantSkipKeys(ctx, SkipKeyType.SILVER, 2)
        composeRule.waitForIdle()
        val silverBounds = composeRule.onNodeWithTag("inv_key_silver").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(
            "Bronze & Silver keys sit side-by-side in 2-column grid",
            abs(bronzeBounds.top.value - silverBounds.top.value) < 4f &&
                abs(bronzeBounds.left.value - silverBounds.left.value) > 40f
        )
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

        // 2. Grant Pulse Elixir & Shadow Serum -> appear side-by-side in 2-column grid
        ProfileManager.grantPotion(ctx, PotionType.PULSE_ELIXIR, 1, notifyRoadmapTask = false)
        ProfileManager.grantPotion(ctx, PotionType.SHADOW_GRIP_SERUM, 1, notifyRoadmapTask = false)
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("inv_potions_empty_state").fetchSemanticsNodes().size)
        val pulseBounds = composeRule.onNodeWithTag("inv_potion_pulse").assertIsDisplayed().getUnclippedBoundsInRoot()
        val shadowBounds = composeRule.onNodeWithTag("inv_potion_shadow").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(
            "Pulse & Shadow potions sit side-by-side in 2-column grid",
            abs(pulseBounds.top.value - shadowBounds.top.value) < 4f &&
                abs(pulseBounds.left.value - shadowBounds.left.value) > 40f
        )
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
    fun titlesSubPageListsAllGameTitlesInTwoColumnGridAndEquipsOnlyUnlockedTitles() {
        val ctx = composeRule.activity
        ProfileManager.unlockTitle(ctx, 25)

        openInventoryTab()
        composeRule.onNodeWithTag("inv_cat_titles").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("inv_subpage_titles").fetchSemanticsNodes().isNotEmpty()
        }

        // Starter title #1 and locked title #2 sit side-by-side in 2-column grid
        val title1Bounds = composeRule.onNodeWithTag("inv_title_card_1").assertIsDisplayed().getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag("inv_title_active_1").assertIsDisplayed()
        val title2Bounds = composeRule.onNodeWithTag("inv_title_card_2").assertIsDisplayed().getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag("inv_title_locked_2").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_activate_title_2").fetchSemanticsNodes().size)
        assertTrue(
            "Title #1 & #2 sit side-by-side in 2-column grid",
            abs(title1Bounds.top.value - title2Bounds.top.value) < 4f &&
                abs(title1Bounds.left.value - title2Bounds.left.value) > 40f
        )

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

        // Starter avatar and locked avatar #2 sit side-by-side in 2-column grid
        val av1Bounds = composeRule.onNodeWithTag("inv_avatar_card_av_core_node").assertIsDisplayed().getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag("inv_avatar_active_av_core_node").assertIsDisplayed()
        val av2Bounds = composeRule.onNodeWithTag("inv_avatar_card_av_std_circuit").assertIsDisplayed().getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag("inv_avatar_locked_av_std_circuit").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("inv_activate_avatar_av_std_circuit").fetchSemanticsNodes().size)
        assertTrue(
            "Avatars #1 & #2 sit side-by-side in 2-column grid",
            abs(av1Bounds.top.value - av2Bounds.top.value) < 4f &&
                abs(av1Bounds.left.value - av2Bounds.left.value) > 40f
        )

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
