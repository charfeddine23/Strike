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
            .addMigrations(AppDatabase.MIGRATION_5_6)
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
        composeRule.onNodeWithTag("heatmap_bottom_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_know_location_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("heatmap_swipe_up_graphics").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodes(hasText("Know My Location", substring = true)).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodes(hasText("موقعي الآن", substring = true)).fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("heatmap_layers_btn").assertIsDisplayed()
        // Enter button must be removed
        assertEquals(0, composeRule.onAllNodesWithTag("heatmap_enter_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("top_bar_outer_container").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("bottom_bar_outer_container").fetchSemanticsNodes().size)

        // Open floating Layers switcher and verify Know My Location sits directly below Map Style & Layers
        composeRule.onNodeWithTag("heatmap_layers_btn").performClick()
        composeRule.waitForIdle()
        val layersPanelBounds = composeRule.onNodeWithTag("heatmap_layers_panel").assertIsDisplayed().getBoundsInRoot()
        val knowLocBelowPanelBounds = composeRule.onNodeWithTag("heatmap_know_location_btn").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Know My Location (${knowLocBelowPanelBounds.top}) must sit below Map Style & Layers (${layersPanelBounds.bottom})",
            knowLocBelowPanelBounds.top >= layersPanelBounds.bottom
        )
        composeRule.onNodeWithTag("heatmap_theme_light_btn").performClick()
        composeRule.onNodeWithTag("heatmap_3d_buildings_toggle").performClick()
        composeRule.onNodeWithTag("heatmap_mode_pins_btn").performClick()
        composeRule.waitForIdle()

        // Swipe/Tap Swipe Up beacon -> navigates smoothly into Main App and displays TopBar & BottomBar
        composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").performClick()
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

        composeRule.onNodeWithTag("add_record_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("input_fullname").performTextInput("يوسف بن خدة")

        composeRule.onNodeWithTag("data_entry_location_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("btn_pick_first_strike_location").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("first_strike_picker_dialog").assertIsDisplayed()
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
        composeRule.waitForIdle()

        val swipeBtn = composeRule.onNodeWithTag("heatmap_swipe_up_hide_btn").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("heatmap_swipe_up_graphics").fetchSemanticsNodes().size)
        val layersBtn = composeRule.onNodeWithTag("heatmap_layers_btn").assertIsDisplayed()
        val knowLocBtn = composeRule.onNodeWithTag("heatmap_know_location_btn").assertIsDisplayed()
        val bottomBar = composeRule.onNodeWithTag("heatmap_bottom_bar").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("heatmap_enter_btn").fetchSemanticsNodes().size)

        val layersBounds = layersBtn.getBoundsInRoot()
        val knowLocBounds = knowLocBtn.getBoundsInRoot()
        val bottomBarBounds = bottomBar.getBoundsInRoot()

        assertTrue(
            "Know My Location (${knowLocBounds.top}) must sit below top-right Layers button (${layersBounds.bottom})",
            knowLocBounds.top >= layersBounds.bottom
        )
        assertTrue(
            "Know My Location (${knowLocBounds.bottom}) must sit cleanly above the bottom Swipe Up bar (${bottomBarBounds.top})",
            knowLocBounds.bottom <= bottomBarBounds.top
        )

        // 1. Swipe up gesture on the handle hides the map & reveals TopBar/BottomBar
        swipeBtn.performTouchInput { swipeUp(startY = centerY, endY = centerY - 220f) }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("first_strike_heatmap_screen").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("top_bar_outer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_outer_container").assertIsDisplayed()

        // 2. Re-open map from Settings drawer and tap the button -> also hides map cleanly
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
        composeRule.onNodeWithTag("top_bar_pull_heatmap_handle").assertIsDisplayed()

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
        composeRule.onNodeWithTag("nav_tab_dashboard").performClick()
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
}
