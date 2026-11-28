package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.map.NominatimGeocoder
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.profile.ProfileManager
import com.example.data.repository.IndividualRepository
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.XpCalculationEngine
import com.example.ui.map.MapStyleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
class FirstStrikeHeatmapTest {

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

    @Test
    fun nominatimGeocoder_stripTifinagh_removesTifinaghRangeCleanly() {
        val mixed = "Tizi Ouzou ⵜⵉⵣⵉ ⵓⵣⵓ, Algeria"
        val cleaned = NominatimGeocoder.stripTifinagh(mixed)
        assertFalse(cleaned.any { it in '\u2D30'..'\u2D7F' })
        assertEquals("Tizi Ouzou, Algeria", cleaned)

        val arabicMixed = "الجزائر العاصمة • ⴷⵣⴰⵢⴻⵔ • Hydra"
        val cleanedArabic = NominatimGeocoder.stripTifinagh(arabicMixed)
        assertFalse(cleanedArabic.any { it in '\u2D30'..'\u2D7F' })
        assertTrue(cleanedArabic.contains("الجزائر العاصمة"))
        assertTrue(cleanedArabic.contains("Hydra"))

        val pureTifinagh = "ⵜⵉⴼⵉⵏⴰⵖ ⴰⵣⵓⵍ"
        assertEquals("", NominatimGeocoder.stripTifinagh(pureTifinagh))
    }

    @Test
    fun mapStyleHelper_buildGeoJsonAndGroupHotspots_clustersValidLocations() {
        val p1 = Individual(
            id = 1L,
            fullName = "أحمد",
            type = "M",
            length = 20.0,
            width = 10.0,
            size = 200.0,
            sScore = 70.0,
            fScore = 70.0,
            cScore = 70.0,
            boost = 210.0,
            strikeCount = 3,
            firstStrikeLat = 36.7538,
            firstStrikeLng = 3.0588,
            firstStrikePlaceName = "Algiers Center"
        )
        val p2 = Individual(
            id = 2L,
            fullName = "سارة",
            type = "TS",
            length = 22.0,
            width = 10.0,
            size = 220.0,
            sScore = 80.0,
            fScore = 80.0,
            cScore = 80.0,
            boost = 260.0,
            strikeCount = 1,
            firstStrikeLat = 36.7544,
            firstStrikeLng = 3.0592,
            firstStrikePlaceName = "Algiers Center"
        )
        val p3 = Individual(
            id = 3L,
            fullName = "طارق",
            type = "FG",
            length = 18.0,
            width = 9.0,
            size = 162.0,
            sScore = 60.0,
            fScore = 60.0,
            cScore = 60.0,
            boost = 180.0,
            strikeCount = 2,
            firstStrikeLat = 35.6971,
            firstStrikeLng = -0.6308,
            firstStrikePlaceName = "Oran"
        )
        val noLocation = Individual(
            id = 4L,
            fullName = "بدون موقع",
            type = "M",
            length = 18.0,
            width = 9.0,
            size = 162.0,
            sScore = 50.0,
            fScore = 50.0,
            cScore = 50.0,
            boost = 150.0
        )

        val geoJson = JSONObject(MapStyleHelper.buildFirstStrikeGeoJson(listOf(p1, p2, p3, noLocation)))
        assertEquals("FeatureCollection", geoJson.getString("type"))
        val features = geoJson.getJSONArray("features")
        assertEquals(3, features.length())

        val firstCoords = features.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(3.0588, firstCoords.getDouble(0), 0.0001)
        assertEquals(36.7538, firstCoords.getDouble(1), 0.0001)

        val hotspots = MapStyleHelper.groupHotspots(listOf(p1, p2, p3, noLocation))
        assertEquals(2, hotspots.size)
        assertEquals(2, hotspots[0].individuals.size)
        assertEquals(2L, hotspots[0].individuals.first().id) // higher boost first
        assertEquals(1, hotspots[1].individuals.size)
        assertEquals("Oran", hotspots[1].placeName)
    }

    @Test
    fun roomMigration5To6_addsFirstStrikeColumnsAndPersistsLocation() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_5_6_test.db"
        ctx.deleteDatabase(dbName)

        // Create a v5 schema database with a row lacking location columns
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(ctx)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `individuals` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `fullName` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `length` REAL NOT NULL,
                            `width` REAL NOT NULL,
                            `size` REAL NOT NULL,
                            `sScore` REAL NOT NULL,
                            `fScore` REAL NOT NULL,
                            `cScore` REAL NOT NULL,
                            `boost` REAL NOT NULL,
                            `strikeCount` INTEGER NOT NULL,
                            `phoneNumber` TEXT NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `strike_events` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `individualId` INTEGER NOT NULL,
                            `individualName` TEXT NOT NULL,
                            `year` INTEGER NOT NULL,
                            `date` TEXT NOT NULL,
                            `time` TEXT NOT NULL,
                            `count` INTEGER NOT NULL,
                            `strikeType` TEXT NOT NULL,
                            `performanceValue` REAL NOT NULL,
                            `notes` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            FOREIGN KEY(`individualId`) REFERENCES `individuals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_strike_events_individualId` ON `strike_events` (`individualId`)")
                    db.execSQL(
                        """
                        INSERT INTO `individuals`
                        (`id`, `fullName`, `type`, `length`, `width`, `size`, `sScore`, `fScore`, `cScore`, `boost`, `strikeCount`, `phoneNumber`, `updatedAt`)
                        VALUES (10, 'Migration User', 'M', 20.0, 10.0, 200.0, 70.0, 70.0, 70.0, 210.0, 1, '', 1000)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val rawHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        rawHelper.writableDatabase.close()
        rawHelper.close()

        val roomDb = Room.databaseBuilder(ctx, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val migratedBeforeUpdate = roomDb.individualDao().getIndividualById(10L)
                assertNotNull(migratedBeforeUpdate)
                assertEquals("Migration User", migratedBeforeUpdate!!.fullName)
                assertEquals(null, migratedBeforeUpdate.firstStrikeLat)
                assertEquals(null, migratedBeforeUpdate.firstStrikeLng)
                assertEquals("", migratedBeforeUpdate.firstStrikePlaceName)

                roomDb.individualDao().updateFirstStrikeLocation(10L, 36.7538, 3.0588, "Algiers")
                val updated = roomDb.individualDao().getIndividualById(10L)
                assertNotNull(updated)
                assertEquals(36.7538, updated!!.firstStrikeLat!!, 0.0001)
                assertEquals(3.0588, updated.firstStrikeLng!!, 0.0001)
                assertEquals("Algiers", updated.firstStrikePlaceName)
            }
        } finally {
            roomDb.close()
            ctx.deleteDatabase(dbName)
        }
    }

    @Test
    fun launchIntoFullScreenHeatmap_hidesTopAndBottomBarsUntilEnterOrSettingsReopen() {
        composeRule.waitForIdle()

        // Launch opens full-screen First-Strike Heatmap and completely hides TopBar & BottomBar
        composeRule.onNodeWithTag("first_strike_heatmap_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_lower_hud_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_summary_toggle_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_total_mapped_counter").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_hud_view_switcher").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_hud_mode_heatmap_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_hud_mode_pins_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_bottom_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_know_location_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_enter_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_layers_btn").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("heatmap_top_location_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_outer_container").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("bottom_bar_outer_container").fetchSemanticsNodes().size)

        // Ensure Summary bar does NOT duplicate "Hottest" (reserved for top-left card)
        assertEquals(
            0,
            composeRule.onAllNodes(
                hasTestTag("heatmap_summary_toggle_bar") and hasAnyDescendant(hasText("Hottest", substring = true)),
                useUnmergedTree = true
            ).fetchSemanticsNodes().size
        )

        // Open floating Layers switcher and inspect halo legend
        composeRule.onNodeWithTag("heatmap_layers_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_layers_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_halo_legend").assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("heatmap_halo_legend") and hasAnyDescendant(hasText("First Strike", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("heatmap_halo_legend") and hasAnyDescendant(
                hasText("المناطق الساخنة", substring = true) or hasText("Hot Zones", substring = true)
            ),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_theme_light_btn").performClick()
        composeRule.onNodeWithTag("heatmap_3d_buildings_toggle").performClick()
        composeRule.onNodeWithTag("heatmap_hud_mode_pins_btn").performClick()
        composeRule.onNodeWithTag("heatmap_hud_mode_heatmap_btn").performClick()
        composeRule.waitForIdle()

        // Tap Enter -> navigates smoothly into Main App and reveals TopBar & BottomBar
        composeRule.onNodeWithTag("heatmap_enter_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("first_strike_heatmap_screen").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed()

        // Re-open Heatmap from Settings Side Drawer -> TopBar hides again cleanly
        composeRule.onNodeWithTag("top_bar_settings_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("settings_open_heatmap_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_open_heatmap_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("first_strike_heatmap_screen").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_outer_container").fetchSemanticsNodes().size)
    }

    @Test
    fun heatmapHotspotInspection_opensBottomSheetAndNavigatesToPersonProfile() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val personId = runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "كريم الميداني",
                    type = "TS",
                    length = 24.0,
                    width = 10.0,
                    size = 240.0,
                    sScore = 82.0,
                    fScore = 78.0,
                    cScore = 85.0,
                    boost = 248.0,
                    strikeCount = 4,
                    firstStrikeLat = 36.7538,
                    firstStrikeLng = 3.0588,
                    firstStrikePlaceName = "Hydra, Algiers"
                )
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("heatmap_hotspot_marker_hotspot_0_$personId").fetchSemanticsNodes().isNotEmpty()
        }

        // Tap hotspot marker -> opens Liquid Glass Bottom Sheet with people at that hotspot
        composeRule.onNodeWithTag("heatmap_hotspot_marker_hotspot_0_$personId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_hotspot_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("hotspot_person_item_$personId").assertIsDisplayed()

        // Tap person card -> exits entry screen, displays TopBar, and opens expanded person profile with location
        composeRule.onNodeWithTag("hotspot_person_item_$personId").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("person_first_strike_location_panel_$personId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("person_first_strike_location_panel_$personId")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("person_first_strike_location_panel_$personId") and
                hasAnyDescendant(hasText("Hydra, Algiers", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun dataEntryDialog_canPickFirstStrikeLocationAndPersistToDatabase() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_record_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("input_fullname").performTextInput("يوسف بن خدة")

        composeRule.onNodeWithTag("data_entry_location_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("btn_pick_first_strike_location").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("first_strike_picker_dialog").assertIsDisplayed()
        val dialogBounds = composeRule.onNodeWithTag("first_strike_picker_dialog").getBoundsInRoot()
        listOf(
            "picker_close_btn",
            "picker_search_input",
            "picker_gps_btn",
            "picker_map_frame",
            "picker_resolved_place_name",
            "picker_confirm_btn"
        ).forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).assertIsDisplayed().getBoundsInRoot()
            assertTrue(
                "$tag ($bounds) must stay strictly inside screen frame ($dialogBounds)",
                bounds.left >= dialogBounds.left &&
                    bounds.right <= dialogBounds.right &&
                    bounds.top >= dialogBounds.top &&
                    bounds.bottom <= dialogBounds.bottom
            )
        }
        composeRule.onNodeWithTag("location_picker_pin").assertIsDisplayed()
        composeRule.onNodeWithTag("location_picker_pin_shadow").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_confirm_btn").performClick()
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("first_strike_picker_dialog").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("save_record_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("data_entry_overlay").fetchSemanticsNodes().isEmpty()
        }

        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val saved = runBlocking(Dispatchers.IO) {
            AppDatabase.getDatabase(ctx).individualDao().getAllIndividualsList().firstOrNull { it.fullName == "يوسف بن خدة" }
        }
        assertNotNull(saved)
        assertTrue(saved!!.hasFirstStrikeLocation)
        assertEquals(36.7538, saved.firstStrikeLat!!, 0.001)
        assertEquals(3.0588, saved.firstStrikeLng!!, 0.001)
        assertTrue(saved.firstStrikePlaceName.isNotBlank())
    }

    @Test
    fun swipeUpHideMapButton_sitsAboveActionButtonsAndHidesMapOnSwipeAndTap() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())
        runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "عمر الساحلي",
                    type = "M",
                    length = 21.0,
                    width = 10.0,
                    size = 210.0,
                    sScore = 70.0,
                    fScore = 70.0,
                    cScore = 70.0,
                    boost = 210.0,
                    strikeCount = 2,
                    firstStrikeLat = 36.7538,
                    firstStrikeLng = 3.0588,
                    firstStrikePlaceName = "Algiers"
                )
            )
            repo.save(
                Individual(
                    fullName = "بدر الدين",
                    type = "TS",
                    length = 22.0,
                    width = 10.0,
                    size = 220.0,
                    sScore = 75.0,
                    fScore = 75.0,
                    cScore = 75.0,
                    boost = 225.0,
                    strikeCount = 1,
                    firstStrikeLat = 35.6971,
                    firstStrikeLng = -0.6308,
                    firstStrikePlaceName = "Oran"
                )
            )
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodes(
                hasTestTag("heatmap_total_mapped_counter") and hasAnyDescendant(hasText("2", substring = true)),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }

        val swipeBtn = composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").assertIsDisplayed()
        val summaryBar = composeRule.onNodeWithTag("heatmap_summary_toggle_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_layers_btn").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("heatmap_top_location_btn").fetchSemanticsNodes().size)
        val bottomBar = composeRule.onNodeWithTag("heatmap_bottom_bar").assertIsDisplayed()
        val knowLocBtn = composeRule.onNodeWithTag("heatmap_know_location_btn").assertIsDisplayed()
        val enterBtn = composeRule.onNodeWithTag("heatmap_enter_btn").assertIsDisplayed()

        val swipeBounds = swipeBtn.getBoundsInRoot()
        val summaryBounds = summaryBar.getBoundsInRoot()
        val bottomBarBounds = bottomBar.getBoundsInRoot()
        val knowLocBounds = knowLocBtn.getBoundsInRoot()
        val enterBounds = enterBtn.getBoundsInRoot()

        assertTrue(
            "Elevated Swipe Up pill (${swipeBounds.bottom}) must sit above Summary/Toggle bar (${summaryBounds.top})",
            swipeBounds.bottom <= summaryBounds.top
        )
        assertTrue(
            "Summary/Toggle bar (${summaryBounds.bottom}) must sit cleanly above Dual-Action bar (${bottomBarBounds.top})",
            summaryBounds.bottom <= bottomBarBounds.top
        )
        assertTrue(
            "Know My Location and Enter must sit side-by-side inside Bottom Dual-Action Bar",
            knowLocBounds.right <= enterBounds.left || enterBounds.right <= knowLocBounds.left
        )

        // 1. Swipe up gesture on elevated pill hides the map & reveals TopBar/BottomBar
        swipeBtn.performTouchInput { swipeUp(startY = centerY, endY = centerY - 220f) }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("first_strike_heatmap_screen").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed()

        // 2. Re-open map from Settings drawer and tap the pill -> also hides map cleanly
        composeRule.onNodeWithTag("top_bar_settings_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("settings_open_heatmap_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_open_heatmap_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("first_strike_heatmap_screen").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
    }

    @Test
    fun findHottestRegion_picksHighestHeatClusterAndDisplaysItAsDefaultLaunchFocus() {
        // Cooler spot: Algiers (1 person, 1 strike -> heat 1.15)
        val cooler = Individual(
            id = 101L,
            fullName = "أمين",
            type = "M",
            length = 20.0,
            width = 10.0,
            size = 200.0,
            sScore = 60.0,
            fScore = 60.0,
            cScore = 60.0,
            boost = 180.0,
            strikeCount = 1,
            firstStrikeLat = 36.7538,
            firstStrikeLng = 3.0588,
            firstStrikePlaceName = "Algiers"
        )
        // Hottest region: Oran (2 nearby high-strike people -> cumulative heat ~5.80)
        val hot1 = Individual(
            id = 102L,
            fullName = "خالد",
            type = "TS",
            length = 22.0,
            width = 10.0,
            size = 220.0,
            sScore = 80.0,
            fScore = 80.0,
            cScore = 80.0,
            boost = 240.0,
            strikeCount = 15,
            firstStrikeLat = 35.6971,
            firstStrikeLng = -0.6308,
            firstStrikePlaceName = "Oran Center"
        )
        val hot2 = Individual(
            id = 103L,
            fullName = "مراد",
            type = "FG",
            length = 21.0,
            width = 10.0,
            size = 210.0,
            sScore = 75.0,
            fScore = 75.0,
            cScore = 75.0,
            boost = 225.0,
            strikeCount = 10,
            firstStrikeLat = 35.7020,
            firstStrikeLng = -0.6250,
            firstStrikePlaceName = "Oran East"
        )

        assertEquals(null, MapStyleHelper.findHottestRegion(emptyList()))

        val hottest = MapStyleHelper.findHottestRegion(listOf(cooler, hot1, hot2))
        assertNotNull(hottest)
        assertEquals(2, hottest!!.peopleCount)
        assertTrue(hottest.lat in 35.69..35.71)
        assertTrue(hottest.lng in -0.64..-0.62)
        assertEquals("Oran Center", hottest.placeName)

        // Seed Room and verify FirstStrikeHeatmapScreen highlights the hottest region on launch
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())
        runBlocking(Dispatchers.IO) {
            repo.save(cooler.copy(id = 0))
            repo.save(hot1.copy(id = 0))
            repo.save(hot2.copy(id = 0))
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodes(
                hasTestTag("heatmap_stats_pill") and hasAnyDescendant(hasText("Oran Center", substring = true)),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("heatmap_stats_pill").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_all_strikes_panel").assertIsDisplayed()
        // All 3 mapped individuals across different regions appear in the list (Oran + Algiers)
        composeRule.onNode(
            hasTestTag("heatmap_all_strikes_panel") and hasAnyDescendant(hasText("خالد", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("heatmap_all_strikes_panel") and hasAnyDescendant(hasText("أمين", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()

        // Filter list to Algiers ("أمين"), tap it -> closes list & focuses map automatically on that strike
        composeRule.onNodeWithTag("heatmap_strikes_search_input").performTextInput("أمين")
        composeRule.waitForIdle()
        val ameenId = runBlocking(Dispatchers.IO) {
            AppDatabase.getDatabase(ctx).individualDao().getAllIndividualsList().first { it.fullName == "أمين" }.id
        }
        composeRule.onNodeWithTag("heatmap_strike_list_item_$ameenId").performClick()
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("heatmap_all_strikes_panel").fetchSemanticsNodes().size)
        composeRule.onNode(
            hasTestTag("heatmap_stats_pill") and hasAnyDescendant(hasText("أمين", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun curtainDragUpFromHeatmapAndPullDownFromTopBar_tracksFingerAndTransitionsSmoothly() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("first_strike_heatmap_screen").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_outer_container").fetchSemanticsNodes().size)

        // 1. Slow partial drag up (hold finger down) -> reveals underlying app pages in sync with finger
        val bottomBar = composeRule.onNodeWithTag("heatmap_bottom_bar")
        bottomBar.performTouchInput {
            down(center)
            moveBy(Offset(0f, -110f), delayMillis = 450L)
        }
        composeRule.waitForIdle()
        assertTrue(
            "Both heatmap and revealed TopBar must coexist mid-drag",
            composeRule.onAllNodesWithTag("first_strike_heatmap_screen").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag("top_bar_outer_container").fetchSemanticsNodes().isNotEmpty()
        )

        // Gentle release below threshold snaps back cleanly to full-screen heatmap
        bottomBar.performTouchInput { up() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("first_strike_heatmap_screen").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_outer_container").fetchSemanticsNodes().size)

        // 2. Full upward swipe reveals main pages + Top Bar pull handle
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 260f)
        }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("first_strike_heatmap_screen").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_pull_heatmap_handle").fetchSemanticsNodes().size)

        // 3. Partial pull down on Top Bar shows heatmap sliding down smoothly mid-gesture
        val topBar = composeRule.onNodeWithTag("top_bar_outer_container")
        topBar.performTouchInput {
            down(center)
            moveBy(Offset(0f, 110f), delayMillis = 450L)
        }
        composeRule.waitForIdle()
        assertTrue(
            "Pulling down TopBar must progressively bring Heatmap curtain back into view mid-drag",
            composeRule.onAllNodesWithTag("first_strike_heatmap_screen").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag("top_bar_outer_container").fetchSemanticsNodes().isNotEmpty()
        )
        topBar.performTouchInput { up() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()

        // 4. Full downward swipe from Top Bar re-opens the full-screen Heatmap
        topBar.performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 340f)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("first_strike_heatmap_screen").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_outer_container").fetchSemanticsNodes().size)
    }

    @Test
    fun returningFromMainPagesToHeatmap_preservesAllHotspotsAndInteractiveMarkers() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val person1Id = runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "سفيان القسنطيني",
                    type = "TS",
                    length = 23.0,
                    width = 10.0,
                    size = 230.0,
                    sScore = 84.0,
                    fScore = 79.0,
                    cScore = 86.0,
                    boost = 249.0,
                    strikeCount = 6,
                    firstStrikeLat = 36.3650,
                    firstStrikeLng = 6.6147,
                    firstStrikePlaceName = "Constantine"
                )
            )
        }
        val person2Id = runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "ياسين الوهراني",
                    type = "FG",
                    length = 20.0,
                    width = 9.0,
                    size = 180.0,
                    sScore = 72.0,
                    fScore = 70.0,
                    cScore = 74.0,
                    boost = 216.0,
                    strikeCount = 2,
                    firstStrikeLat = 35.6971,
                    firstStrikeLng = -0.6308,
                    firstStrikePlaceName = "Oran Bay"
                )
            )
        }

        // Wait for both hotspots to be active on initial heatmap launch
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("heatmap_hotspot_marker_hotspot_0_$person1Id").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag("heatmap_hotspot_marker_hotspot_1_$person2Id").fetchSemanticsNodes().isNotEmpty()
        }

        // Enter main pages via Swipe Up
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 260f)
        }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("first_strike_heatmap_screen").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()

        // Navigate across tabs inside main pages
        composeRule.onNodeWithTag("nav_tab_inventory").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitForIdle()

        // Return to Heatmap by pulling down Top Bar -> hotspots MUST remain intact & clickable
        composeRule.onNodeWithTag("top_bar_outer_container").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 340f)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("first_strike_heatmap_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_hotspot_marker_hotspot_0_$person1Id").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_hotspot_marker_hotspot_1_$person2Id").assertIsDisplayed()

        // Tapping preserved hotspot still opens inspection sheet cleanly
        composeRule.onNodeWithTag("heatmap_hotspot_marker_hotspot_1_$person2Id").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_hotspot_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("hotspot_person_item_$person2Id").assertIsDisplayed()
    }

    @Test
    fun roomMigration6To7_addsStrikeEventLocationAndCategoryColumns() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_6_7_test.db"
        ctx.deleteDatabase(dbName)

        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(ctx)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `individuals` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `fullName` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `length` REAL NOT NULL,
                            `width` REAL NOT NULL,
                            `size` REAL NOT NULL,
                            `sScore` REAL NOT NULL,
                            `fScore` REAL NOT NULL,
                            `cScore` REAL NOT NULL,
                            `boost` REAL NOT NULL,
                            `strikeCount` INTEGER NOT NULL,
                            `phoneNumber` TEXT NOT NULL,
                            `firstStrikeLat` REAL,
                            `firstStrikeLng` REAL,
                            `firstStrikePlaceName` TEXT NOT NULL DEFAULT '',
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `strike_events` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `individualId` INTEGER NOT NULL,
                            `individualName` TEXT NOT NULL,
                            `year` INTEGER NOT NULL,
                            `date` TEXT NOT NULL,
                            `time` TEXT NOT NULL,
                            `count` INTEGER NOT NULL,
                            `strikeType` TEXT NOT NULL,
                            `performanceValue` REAL NOT NULL,
                            `notes` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            FOREIGN KEY(`individualId`) REFERENCES `individuals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_strike_events_individualId` ON `strike_events` (`individualId`)")
                    db.execSQL(
                        """
                        INSERT INTO `individuals`
                        (`id`, `fullName`, `type`, `length`, `width`, `size`, `sScore`, `fScore`, `cScore`, `boost`, `strikeCount`, `phoneNumber`, `firstStrikeLat`, `firstStrikeLng`, `firstStrikePlaceName`, `updatedAt`)
                        VALUES (20, 'Migration V6 User', 'M', 21.0, 10.0, 210.0, 72.0, 72.0, 72.0, 216.0, 1, '', 36.75, 3.05, 'Algiers', 1000)
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `strike_events`
                        (`id`, `individualId`, `individualName`, `year`, `date`, `time`, `count`, `strikeType`, `performanceValue`, `notes`, `timestamp`)
                        VALUES (5, 20, 'Migration V6 User', 2026, '2026-11-25', '12:00:00', 1, 'Routine', 216.0, '', 2000)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val rawHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        rawHelper.writableDatabase.close()
        rawHelper.close()

        val roomDb = Room.databaseBuilder(ctx, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_6_7)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val repo = IndividualRepository(roomDb.individualDao(), roomDb.strikeEventDao())
                val beforeUpdate = roomDb.strikeEventDao().getEventById(5L)
                assertNotNull(beforeUpdate)
                assertFalse(beforeUpdate!!.hasLocation)
                assertFalse(beforeUpdate.isFirstStrike)

                repo.updateStrikeEventLocationAndCategory(
                    eventId = 5L,
                    lat = 35.6971,
                    lng = -0.6308,
                    placeName = "Oran Harbor",
                    isFirstStrike = false
                )
                val updated = roomDb.strikeEventDao().getEventById(5L)
                assertNotNull(updated)
                assertTrue(updated!!.hasLocation)
                assertEquals(35.6971, updated.lat!!, 0.0001)
                assertEquals(-0.6308, updated.lng!!, 0.0001)
                assertEquals("Oran Harbor", updated.placeName)
                assertFalse(updated.isFirstStrike)
            }
        } finally {
            roomDb.close()
            ctx.deleteDatabase(dbName)
        }
    }

    @Test
    fun mapStyleHelper_separatesFirstStrikeAndSnapMapRoutineHeatGeoJson() {
        val person = Individual(
            id = 42L,
            fullName = "فريد",
            type = "TS",
            length = 22.0,
            width = 10.0,
            size = 220.0,
            sScore = 80.0,
            fScore = 80.0,
            cScore = 80.0,
            boost = 240.0,
            strikeCount = 3,
            firstStrikeLat = 36.7538,
            firstStrikeLng = 3.0588,
            firstStrikePlaceName = "Algiers Center"
        )
        val routineEvent = com.example.data.model.StrikeEvent(
            id = 201L,
            individualId = 42L,
            individualName = "فريد",
            year = 2026,
            date = "2026-11-25",
            time = "14:00:00",
            count = 2,
            strikeType = "Routine",
            performanceValue = 240.0,
            lat = 35.6971,
            lng = -0.6308,
            placeName = "Oran Corniche",
            isFirstStrike = false
        )

        val firstJson = JSONObject(MapStyleHelper.buildFirstStrikeGeoJson(listOf(person), listOf(routineEvent)))
        val routineJson = JSONObject(MapStyleHelper.buildRoutineStrikeGeoJson(listOf(person), listOf(routineEvent)))
        assertEquals(1, firstJson.getJSONArray("features").length())
        assertEquals(1, routineJson.getJSONArray("features").length())
        assertEquals(
            true,
            firstJson.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getBoolean("isFirstStrike")
        )
        assertEquals(
            false,
            routineJson.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getBoolean("isFirstStrike")
        )

        val hotspots = MapStyleHelper.groupHotspots(listOf(person), listOf(routineEvent))
        assertEquals(2, hotspots.size)
        val routineHotspot = hotspots.first { it.hasRoutineStrikes }
        assertEquals("Oran Corniche", routineHotspot.placeName)
        assertEquals(1, routineHotspot.routineStrikeCount)
        assertEquals(2, routineHotspot.points.sumOf { it.strikeCount })
        assertEquals(0, routineHotspot.firstStrikeCount)
    }

    @Test
    fun dashboardPersonProfile_canAddMultiStrikesAsRoutineOrFirstStrikeWithLocation() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val personId = runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "نسيم الباهي",
                    type = "M",
                    length = 21.0,
                    width = 10.0,
                    size = 210.0,
                    sScore = 75.0,
                    fScore = 76.0,
                    cScore = 74.0,
                    boost = 225.0,
                    strikeCount = 0
                )
            )
        }

        // Exit launch heatmap cleanly and verify top bar icons are not wrapped in a pill container
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_telemetry_pill_container").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("dna_header_icon").assertIsDisplayed()
        composeRule.onNodeWithTag("header_flame_btn").assertIsDisplayed()

        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitForIdle()

        // Expand person record card -> open Multi-Strike dialog
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("record_expand_btn_$personId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("record_expand_btn_$personId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("person_multi_strike_panel_$personId").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("btn_add_multi_strike_$personId").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Pick Routine Strike category, increment default 2 -> 3, and pin map location
        composeRule.onNodeWithTag("multi_strike_type_routine").performClick()
        composeRule.onNodeWithTag("multi_strike_inc_btn").performClick()
        composeRule.onNodeWithTag("btn_pick_multi_strike_location").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("first_strike_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_confirm_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("first_strike_picker_dialog").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag("btn_confirm_multi_strike").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val ind = runBlocking(Dispatchers.IO) { db.individualDao().getIndividualById(personId) }
            val currentEvents = runBlocking(Dispatchers.IO) { db.strikeEventDao().getEventsForIndividualList(personId) }
            ind?.strikeCount == 3 && currentEvents.size >= 3
        }

        val events = runBlocking(Dispatchers.IO) { db.strikeEventDao().getEventsForIndividualList(personId) }
        val routineMappedEvents = events.filter { !it.isFirstStrike && it.hasLocation }
        assertEquals(3, routineMappedEvents.sumOf { it.count })

        // Return to Heatmap -> open strikes list and verify Routine Strike appears live
        composeRule.onNodeWithTag("top_bar_settings_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("settings_open_heatmap_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_open_heatmap_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_stats_pill").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_all_strikes_panel").assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("heatmap_all_strikes_panel") and hasAnyDescendant(hasText("نسيم الباهي", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun mapStyleHelper_createsHighDefinitionRadialSpectralSprites() {
        val bands = (0..4).map { band -> MapStyleHelper.createHeatBandBitmap(band) }
        bands.forEachIndexed { band, bmp ->
            assertEquals(MapStyleHelper.HEAT_SPRITE_SIZE_PX, bmp.width)
            assertEquals(MapStyleHelper.HEAT_SPRITE_SIZE_PX, bmp.height)
            assertEquals(768, bmp.width)
            assertEquals(android.util.DisplayMetrics.DENSITY_XXXHIGH, bmp.density)

            val cornerAlpha = (bmp.getPixel(0, 0) ushr 24) and 0xFF
            assertEquals("Band $band corner must stay strictly transparent", 0, cornerAlpha)

            val centerPixel = bmp.getPixel(bmp.width / 2, bmp.height / 2)
            val centerAlpha = (centerPixel ushr 24) and 0xFF
            assertTrue("Band $band center alpha ($centerAlpha) must be visibly dense", centerAlpha in 80..220)

            val edgePixel = bmp.getPixel((bmp.width * 0.90).toInt(), bmp.height / 2)
            val edgeAlpha = (edgePixel ushr 24) and 0xFF
            assertTrue("Band $band must fall off smoothly toward edge ($edgeAlpha < $centerAlpha)", edgeAlpha < centerAlpha)
        }

        val redCenter = bands[4].getPixel(MapStyleHelper.HEAT_SPRITE_SIZE_PX / 2, MapStyleHelper.HEAT_SPRITE_SIZE_PX / 2)
        val redR = (redCenter ushr 16) and 0xFF
        val redB = redCenter and 0xFF
        assertTrue("Band 4 core should be Coral-Red dominant (R=$redR > B=$redB)", redR > redB + 120)
    }

    @Test
    fun oldHistoricalStrikeLocation_updatesPersonProfileAndShowsInDashboardCard() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val personId = runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "زياد التاريخي",
                    type = "TS",
                    length = 22.0,
                    width = 10.0,
                    size = 220.0,
                    sScore = 80.0,
                    fScore = 78.0,
                    cScore = 82.0,
                    boost = 240.0,
                    strikeCount = 1
                )
            )
        }
        val oldEventId = runBlocking(Dispatchers.IO) {
            db.strikeEventDao().insertEvent(
                StrikeEvent(
                    individualId = personId,
                    individualName = "زياد التاريخي",
                    year = 2024,
                    date = "2024-05-10",
                    time = "11:15",
                    count = 1,
                    strikeType = "Historical",
                    performanceValue = 240.0,
                    notes = "Old untagged strike",
                    timestamp = 1_715_339_700_000L
                )
            )
        }

        // Pin old historical event location and promote to First Strike
        runBlocking(Dispatchers.IO) {
            repo.updateStrikeEventLocationAndCategory(
                eventId = oldEventId,
                lat = 36.7538,
                lng = 3.0588,
                placeName = "Hydra Old Strike",
                isFirstStrike = true
            )
        }

        val updatedPerson = runBlocking(Dispatchers.IO) { db.individualDao().getIndividualById(personId) }
        assertNotNull(updatedPerson)
        assertTrue("First strike location must be recorded on Individual", updatedPerson!!.hasFirstStrikeLocation)
        assertEquals("Hydra Old Strike", updatedPerson.firstStrikePlaceName)

        // Also update the same event later (regression check: previously !ind.hasFirstStrikeLocation blocked updates)
        runBlocking(Dispatchers.IO) {
            repo.updateStrikeEventLocationAndCategory(
                eventId = oldEventId,
                lat = 36.7610,
                lng = 3.0510,
                placeName = "El Biar Summit",
                isFirstStrike = true
            )
        }
        val reUpdatedPerson = runBlocking(Dispatchers.IO) { db.individualDao().getIndividualById(personId) }
        assertEquals("El Biar Summit", reUpdatedPerson?.firstStrikePlaceName)

        // Verify inside striker profile card in DashboardScreen
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("record_expand_btn_$personId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("record_expand_btn_$personId").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("person_first_strike_location_panel_$personId").performScrollTo()
        composeRule.onNode(
            hasTestTag("person_first_strike_location_panel_$personId") and
                hasAnyDescendant(hasText("El Biar Summit", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()

        composeRule.onNodeWithTag("person_multi_strike_panel_$personId").performScrollTo()
        composeRule.onNode(
            hasTestTag("person_multi_strike_panel_$personId") and
                hasAnyDescendant(hasText("El Biar Summit", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun locationPicker_isCompactAndDisplaysAllRegisteredStrikesForPerson() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val personId = runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "نزار متعدد المواقع",
                    type = "FG",
                    length = 21.0,
                    width = 10.0,
                    size = 210.0,
                    sScore = 76.0,
                    fScore = 76.0,
                    cScore = 76.0,
                    boost = 228.0,
                    strikeCount = 3,
                    firstStrikeLat = 36.7480,
                    firstStrikeLng = 3.0400,
                    firstStrikePlaceName = "Algiers Hydra"
                )
            )
        }
        val ev1Id = runBlocking(Dispatchers.IO) {
            db.strikeEventDao().insertEvent(
                StrikeEvent(
                    individualId = personId,
                    individualName = "نزار متعدد المواقع",
                    year = 2026,
                    date = "2026-10-05",
                    time = "15:00",
                    count = 1,
                    strikeType = "Routine",
                    performanceValue = 228.0,
                    lat = 35.6971,
                    lng = -0.6308,
                    placeName = "Oran Corniche",
                    isFirstStrike = false,
                    timestamp = 1_791_212_400_000L
                )
            )
        }
        val ev2Id = runBlocking(Dispatchers.IO) {
            db.strikeEventDao().insertEvent(
                StrikeEvent(
                    individualId = personId,
                    individualName = "نزار متعدد المواقع",
                    year = 2026,
                    date = "2026-11-12",
                    time = "20:10",
                    count = 1,
                    strikeType = "Routine",
                    performanceValue = 228.0,
                    lat = 36.3650,
                    lng = 6.6147,
                    placeName = "Constantine Bridge",
                    isFirstStrike = false,
                    timestamp = 1_794_514_200_000L
                )
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_records_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("record_expand_btn_$personId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("record_expand_btn_$personId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_edit_person_location_$personId").performScrollTo().performClick()
        composeRule.waitForIdle()

        val dialogBounds = composeRule.onNodeWithTag("first_strike_picker_dialog").assertIsDisplayed().getBoundsInRoot()
        val mapBounds = composeRule.onNodeWithTag("picker_map_frame").assertIsDisplayed().getBoundsInRoot()
        val mapHeightDp = mapBounds.bottom - mapBounds.top
        assertTrue("Picker map height ($mapHeightDp) must be compact (<= 260.dp)", mapHeightDp.value <= 260f)
        assertTrue(
            "Compact picker map ($mapBounds) must stay strictly inside dialog ($dialogBounds)",
            mapBounds.top >= dialogBounds.top && mapBounds.bottom <= dialogBounds.bottom
        )

        // Verify all registered strikes for this person appear LIVE as plain red dots on the map (with NO place-name labels inside map frame)
        composeRule.onNodeWithTag("picker_fit_all_pins_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_live_dot_ind_$personId", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("picker_live_dot_ev_$ev1Id", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("picker_live_dot_ev_$ev2Id", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodes(
                hasTestTag("picker_map_frame") and hasAnyDescendant(hasText("Oran Corniche", substring = true)),
                useUnmergedTree = true
            ).fetchSemanticsNodes().size
        )
        assertEquals(
            0,
            composeRule.onAllNodes(
                hasTestTag("picker_map_frame") and hasAnyDescendant(hasText("Constantine Bridge", substring = true)),
                useUnmergedTree = true
            ).fetchSemanticsNodes().size
        )

        composeRule.onNodeWithTag("picker_person_locations_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_saved_loc_chip_ind_$personId").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_saved_loc_chip_ev_$ev1Id").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_saved_loc_chip_ev_$ev2Id").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_existing_pin_ev_$ev1Id").assertIsDisplayed()

        // Tapping a live red dot directly on the map jumps selected preview location cleanly
        composeRule.onNodeWithTag("picker_existing_pin_ev_$ev2Id").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(
            hasTestTag("first_strike_picker_dialog") and hasAnyDescendant(hasText("Constantine Bridge", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun tappingStrikeOnMap_showsSpecificSelectedStrikeDetailCard() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(ctx)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())

        val personId = runBlocking(Dispatchers.IO) {
            repo.save(
                Individual(
                    fullName = "طاهر قسنطينة",
                    type = "TS",
                    length = 23.0,
                    width = 10.5,
                    size = 241.5,
                    sScore = 88.0,
                    fScore = 84.0,
                    cScore = 86.0,
                    boost = 258.0,
                    strikeCount = 3
                )
            )
        }
        runBlocking(Dispatchers.IO) {
            db.strikeEventDao().insertEvent(
                StrikeEvent(
                    individualId = personId,
                    individualName = "طاهر قسنطينة",
                    year = 2026,
                    date = "2026-10-14",
                    time = "19:30",
                    count = 3,
                    strikeType = "Routine",
                    performanceValue = 258.0,
                    notes = "Evening tactical checkpoint",
                    lat = 36.3720,
                    lng = 6.6120,
                    placeName = "Sidi M'Cid",
                    isFirstStrike = false,
                    timestamp = 1_792_006_200_000L
                )
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("heatmap_hotspot_marker_hotspot_0_$personId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("heatmap_hotspot_marker_hotspot_0_$personId").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("heatmap_hotspot_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("selected_strike_detail_card").assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("selected_strike_detail_card") and hasAnyDescendant(hasText("طاهر قسنطينة", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("selected_strike_detail_card") and hasAnyDescendant(hasText("2026-10-14", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("selected_strike_detail_card") and hasAnyDescendant(hasText("Sidi M'Cid", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("selected_strike_detail_card") and hasAnyDescendant(hasText("Evening tactical checkpoint", substring = true)),
            useUnmergedTree = true
        ).assertIsDisplayed()

        // Navigate directly from the specific strike detail card to the striker profile
        composeRule.onNodeWithTag("btn_selected_strike_open_profile").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("person_first_strike_location_panel_$personId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("person_first_strike_location_panel_$personId").assertIsDisplayed()
    }

    @Test
    fun heatSpriteIconSizeAndPulseExpressions_useZoomOnlyAsTopLevelInterpolateInput() {
        val exprJson = MapStyleHelper.buildHeatSpriteIconSizeExpression(1.0f, 1.04f).toString()
        assertTrue(
            "Expected top-level interpolate expression with zoom input, got: $exprJson",
            exprJson.startsWith("[\"interpolate\", [\"linear\"], [\"zoom\"],")
        )
        val zoomOccurrences = "\\[\"zoom\"\\]".toRegex().findAll(exprJson).count()
        assertEquals("zoom() must only appear once as top-level interpolate input", 1, zoomOccurrences)

        val ringExprJson = MapStyleHelper.buildLivePulseRingRadiusExpression(1.6f).toString()
        assertTrue(
            "Expected top-level interpolate expression with zoom input, got: $ringExprJson",
            ringExprJson.startsWith("[\"interpolate\", [\"linear\"], [\"zoom\"],")
        )
        assertEquals(1, "\\[\"zoom\"\\]".toRegex().findAll(ringExprJson).count())

        val ringOpacityExprJson = MapStyleHelper.buildLivePulseRingOpacityExpression(0.75f).toString()
        assertTrue(
            "Expected top-level interpolate expression with zoom input, got: $ringOpacityExprJson",
            ringOpacityExprJson.startsWith("[\"interpolate\", [\"linear\"], [\"zoom\"],")
        )
        assertEquals(1, "\\[\"zoom\"\\]".toRegex().findAll(ringOpacityExprJson).count())

        val heatmapOpacityExprJson = MapStyleHelper.buildRoutineHeatmapOpacityExpression(
            com.example.ui.map.HeatmapDisplayMode.HYBRID,
            0.5f
        ).toString()
        assertTrue(
            "Expected top-level interpolate expression with zoom input, got: $heatmapOpacityExprJson",
            heatmapOpacityExprJson.startsWith("[\"interpolate\", [\"linear\"], [\"zoom\"],")
        )
        assertEquals(1, "\\[\"zoom\"\\]".toRegex().findAll(heatmapOpacityExprJson).count())
    }

    @Test
    fun heatmapAnimation_uses30SecondCycleAndClustersNearbyRoutineStrikesCalmly() {
        assertEquals(30_000L, MapStyleHelper.HEATMAP_ANIMATION_CYCLE_MS)
        assertEquals(0.0f, MapStyleHelper.computeSmoothAnimationPhase(0L), 0.0001f)
        assertEquals(0.5f, MapStyleHelper.computeSmoothAnimationPhase(15_000L), 0.0001f)
        assertEquals(0.0f, MapStyleHelper.computeSmoothAnimationPhase(30_000L), 0.0001f)
        assertEquals(0.25f, MapStyleHelper.computeSmoothAnimationPhase(67_500L), 0.0001f)

        val person = Individual(
            id = 55L,
            fullName = "سامي الهادئ",
            type = "TS",
            length = 22.0,
            width = 10.0,
            size = 220.0,
            sScore = 80.0,
            fScore = 80.0,
            cScore = 80.0,
            boost = 240.0,
            strikeCount = 4
        )
        val clusteredRoutineEvents = listOf(
            StrikeEvent(
                id = 1L,
                individualId = 55L,
                individualName = "سامي الهادئ",
                year = 2026,
                date = "2026-11-20",
                time = "10:00",
                count = 1,
                strikeType = "Routine",
                performanceValue = 240.0,
                lat = 36.7538,
                lng = 3.0588,
                placeName = "Hub Alpha",
                isFirstStrike = false
            ),
            StrikeEvent(
                id = 2L,
                individualId = 55L,
                individualName = "سامي الهادئ",
                year = 2026,
                date = "2026-11-21",
                time = "11:00",
                count = 2,
                strikeType = "Routine",
                performanceValue = 240.0,
                lat = 36.7545,
                lng = 3.0592,
                placeName = "Hub Alpha",
                isFirstStrike = false
            ),
            StrikeEvent(
                id = 3L,
                individualId = 55L,
                individualName = "سامي الهادئ",
                year = 2026,
                date = "2026-11-22",
                time = "12:00",
                count = 1,
                strikeType = "Routine",
                performanceValue = 240.0,
                lat = 35.6971,
                lng = -0.6308,
                placeName = "Hub Beta",
                isFirstStrike = false
            )
        )

        val rawRoutineJson = JSONObject(
            MapStyleHelper.buildRoutineStrikeGeoJson(listOf(person), clusteredRoutineEvents)
        )
        val clusterJson = JSONObject(
            MapStyleHelper.buildRoutineHotspotClusterGeoJson(listOf(person), clusteredRoutineEvents)
        )

        // All 3 points exist in source heatmap data, while cluster pulse centroids merge nearby points cleanly to 2 calm hubs
        assertEquals(3, rawRoutineJson.getJSONArray("features").length())
        assertEquals(2, clusterJson.getJSONArray("features").length())
    }
}
