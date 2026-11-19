package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.profile.PotionType
import com.example.data.profile.ProfileManager
import com.example.data.repository.IndividualRepository
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.XpCalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GirlDialogWiringTest {

    @Before
    fun setUp() {
        resetAllState()
    }

    @After
    fun tearDown() {
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

    @Test
    fun setGirlDeductionPoints_updatesWithoutImmediateDeduction_appliesOnNextTap_andSurvivesBackupRestore() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        FlameManager.setScore(app, 100.0, reason = "SIMULATION")
        val db = AppDatabase.getDatabase(app)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())
        val viewModel = com.example.ui.viewmodel.IndividualViewModel(app, repo)

        // Default is 3.0
        assertEquals(3.0, viewModel.girlDeductionPoints.value, 0.001)

        // Saving custom Girl deduction (as invoked by Apply Deduction button) must NOT deduct immediately
        viewModel.setGirlDeductionPoints(25.0)
        assertEquals(100.0, FlameManager.flameScore.value, 0.001)
        assertEquals(25.0, viewModel.girlDeductionPoints.value, 0.001)

        // Subsequent Girl taps deduct the newly configured 25.0 points
        viewModel.girl()
        assertEquals(75.0, FlameManager.flameScore.value, 0.001)

        viewModel.girl()
        assertEquals(50.0, FlameManager.flameScore.value, 0.001)

        // Fractional deduction also saved cleanly without immediate subtraction
        viewModel.setGirlDeductionPoints(12.5)
        assertEquals(50.0, FlameManager.flameScore.value, 0.001)
        assertEquals(12.5, viewModel.girlDeductionPoints.value, 0.001)

        // Export -> Reset -> Restore preserves custom Girl deduction
        val snapshot = FlameManager.exportSnapshot(app)
        FlameManager.resetAllLevelsToZero(app)
        assertEquals(3.0, FlameManager.girlDeductionPoints.value, 0.001)

        FlameManager.restoreSnapshot(app, snapshot, replaceExisting = true)
        assertEquals(12.5, FlameManager.girlDeductionPoints.value, 0.001)

        viewModel.girl()
        assertEquals(37.5, FlameManager.flameScore.value, 0.001)
    }

    @Test
    fun deflameWithPulseElixir_tracksRealEffectiveDeductionAndRemainingUses() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        FlameManager.setScore(app, 60.0, reason = "SIMULATION")
        val db = AppDatabase.getDatabase(app)
        val repo = IndividualRepository(db.individualDao(), db.strikeEventDao())
        val viewModel = com.example.ui.viewmodel.IndividualViewModel(app, repo)

        assertEquals(0, viewModel.pulseElixirUsesLeft.value)
        ProfileManager.grantPotion(app, PotionType.PULSE_ELIXIR, 1, notifyRoadmapTask = false)
        ProfileManager.usePulseElixir(app)
        assertEquals(3, viewModel.pulseElixirUsesLeft.value)

        // Each Deflame tap under Pulse Elixir deducts 3 + 5 = 8 points
        viewModel.deflame()
        assertEquals(52.0, FlameManager.flameScore.value, 0.001)
        assertEquals(2, viewModel.pulseElixirUsesLeft.value)

        viewModel.deflame()
        assertEquals(44.0, FlameManager.flameScore.value, 0.001)
        assertEquals(1, viewModel.pulseElixirUsesLeft.value)

        viewModel.deflame()
        assertEquals(36.0, FlameManager.flameScore.value, 0.001)
        assertEquals(0, viewModel.pulseElixirUsesLeft.value)

        // Next tap deducts normal base 3 points
        viewModel.deflame()
        assertEquals(33.0, FlameManager.flameScore.value, 0.001)
    }
}
