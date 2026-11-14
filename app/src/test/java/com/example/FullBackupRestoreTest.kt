package com.example

import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.flame.ActiveShieldType
import com.example.data.flame.FlameManager
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.profile.PotionType
import com.example.data.profile.ProfileManager
import com.example.data.profile.SkipKeyType
import com.example.data.repository.IndividualRepository
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FullBackupRestoreTest {

    private lateinit var app: Application
    private lateinit var database: AppDatabase
    private lateinit var repository: IndividualRepository
    private lateinit var backupManager: BackupManager
    private lateinit var backupFile: File

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        clearAllPrefs()

        database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = IndividualRepository(database.individualDao(), database.strikeEventDao())
        backupManager = BackupManager(repository)
        backupFile = File(app.cacheDir, "strikepulse_literal_backup_test.json")
        if (backupFile.exists()) backupFile.delete()
    }

    @After
    fun tearDown() {
        if (backupFile.exists()) backupFile.delete()
        database.close()
    }

    private fun clearAllPrefs() {
        app.getSharedPreferences("strikepulse_roadmap_prefs", Application.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("strikepulse_flame_prefs", Application.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("strikepulse_profile_prefs", Application.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("strikepulse_security_prefs", Application.MODE_PRIVATE).edit().clear().commit()

        ProfileManager.resetForTesting(app)
        FlameManager.resetForTesting(app)
        RoadmapManager.resetForTesting(app)
    }

    @Test
    fun testLiteralCompleteBackupExportAndRestore() = runBlocking {
        // 1. Populate Room DB: Individuals & StrikeEvents
        val person1 = Individual(
            id = 1L,
            fullName = "Atlas Cyber",
            type = "M",
            length = 22.0,
            width = 11.0,
            size = 242.0,
            sScore = 88.0,
            fScore = 91.0,
            cScore = 85.0,
            boost = 270.0,
            strikeCount = 12,
            phoneNumber = "+15550199"
        )
        val person2 = Individual(
            id = 2L,
            fullName = "Nova Pulse",
            type = "W",
            length = 18.0,
            width = 9.0,
            size = 162.0,
            sScore = 76.0,
            fScore = 80.0,
            cScore = 79.0,
            boost = 238.0,
            strikeCount = 4,
            phoneNumber = "+15550200"
        )
        database.individualDao().insert(person1)
        database.individualDao().insert(person2)

        val event1 = StrikeEvent(
            id = 501L,
            individualId = 1L,
            individualName = "Atlas Cyber",
            year = 2026,
            date = "2026-11-10",
            time = "11:20:00",
            count = 1,
            strikeType = "Automatic",
            performanceValue = 270.0,
            notes = "Critical precision"
        )
        val event2 = StrikeEvent(
            id = 502L,
            individualId = 2L,
            individualName = "Nova Pulse",
            year = 2026,
            date = "2026-11-11",
            time = "19:45:00",
            count = 2,
            strikeType = "Manual",
            performanceValue = 238.0,
            notes = "Secondary entry"
        )
        database.strikeEventDao().insertEvent(event1)
        database.strikeEventDao().insertEvent(event2)

        // 2. Populate FlameManager with score, shields, and 210+ infinite strike/flame logs (> old 150 cap)
        FlameManager.setScore(app, 14250.0)
        FlameManager.grant1DayShield(app, 2)
        FlameManager.grant48hShield(app, 3)
        FlameManager.grantManualShield(app, days = 3)
        FlameManager.activate48hShield(app)
        for (i in 1..210) {
            FlameManager.addPoints(
                context = app,
                delta = 1.0,
                reason = "INACTIVITY",
                details = "Infinite log entry #$i",
                title = "Log #$i"
            )
        }
        assertTrue("Expected > 200 infinite flame logs, got ${FlameManager.flameLogs.value.size}", FlameManager.flameLogs.value.size >= 210)

        // 3. Populate RoadmapManager (XP, claimed stations, D/W/M partial + completed, Annual partial + archive)
        RoadmapManager.addXp(app, 8400)
        assertTrue(RoadmapManager.claimStationReward(app, 1) != null)
        assertTrue(RoadmapManager.claimStationReward(app, 2) != null)

        val firstDailyId = RoadmapManager.dailyTasks.value.first().id
        assertTrue(RoadmapManager.completeTask(app, firstDailyId))

        val multiStepAnnual = RoadmapManager.yearlyTasks.value.first { it.actionTag == "STRIKE" && it.targetCount > 1 }
        val completedAnnual = RoadmapManager.yearlyTasks.value.first { it.id != multiStepAnnual.id }
        RoadmapManager.onActionOccurred(app, "STRIKE")
        assertTrue(RoadmapManager.completeTask(app, completedAnnual.id))

        val expectedTotalXp = RoadmapManager.totalXp.value
        val expectedClaimedStations = RoadmapManager.claimedStations.value
        val expectedCompletedTaskIds = RoadmapManager.completedTaskIds.value
        val expectedDailyIds = RoadmapManager.dailyTasks.value.map { it.id }
        val expectedWeeklySnapshot = RoadmapManager.weeklyTasks.value.map { it.id to it.currentCount }

        // 4. Populate ProfileManager (custom username, potions incl. Eternal Resurgence, keys, permanent unlocks)
        ProfileManager.setUserName(app, "Sovereign Striker")
        ProfileManager.unlockAvatar(app, "av_gabriel_wings")
        ProfileManager.equipAvatar(app, "av_gabriel_wings")
        ProfileManager.unlockTitle(app, 21)
        ProfileManager.equipTitle(app, 21)
        ProfileManager.grantSkipKeys(app, SkipKeyType.WOODEN, 7)
        ProfileManager.grantSkipKeys(app, SkipKeyType.BRONZE, 5)
        ProfileManager.grantSkipKeys(app, SkipKeyType.SILVER, 2)
        ProfileManager.grantPotion(app, PotionType.PULSE_ELIXIR, 2)
        ProfileManager.grantPotion(app, PotionType.SHADOW_GRIP_SERUM, 1)
        ProfileManager.grantPotion(app, PotionType.ABSOLUTE_CRYO_INFUSION, 2)
        ProfileManager.grantPotion(app, PotionType.ETERNAL_REBIRTH_ELIXIR, 2)

        // 5. Populate SecurityPreferences
        val secPrefs = SecurityPreferences(app)
        secPrefs.selectedLanguage = "en"
        secPrefs.isHapticFeedbackEnabled = false
        secPrefs.isAutoUpdateEnabled = false
        secPrefs.savedPattern = "1-2-3-6-9"
        secPrefs.isPatternLockEnabled = true

        // Snapshot expected flame state after all mutations (including station claim shield activations)
        val expectedFlameScore = FlameManager.flameScore.value
        val expectedFlamePeak = FlameManager.peakScore.value
        val expectedFlameLogsCount = FlameManager.flameLogs.value.size
        val expectedActiveShieldType = FlameManager.activeShieldType.value
        val expected1DayShields = FlameManager.available1DayShields.value
        val expected48hShields = FlameManager.available48hShields.value
        val expectedManualShields = FlameManager.availableManualShields.value
        val expectedWoodenKeys = ProfileManager.woodenKeysCount.value
        val expectedBronzeKeys = ProfileManager.bronzeKeysCount.value
        val expectedSilverKeys = ProfileManager.silverKeysCount.value
        val expectedPulseElixir = ProfileManager.pulseElixirCount.value
        val expectedShadowSerum = ProfileManager.shadowSerumCount.value
        val expectedCryoInfusion = ProfileManager.cryoInfusionCount.value
        val expectedEternalRebirth = ProfileManager.eternalRebirthCount.value
        assertEquals(2, expectedEternalRebirth)

        // 6. Export full literal backup to Uri
        val fileUri = Uri.fromFile(backupFile)
        val exportRes = backupManager.exportBackup(app, fileUri, lang = "en")
        assertTrue("Export failed: ${exportRes.message}", exportRes.success)
        assertTrue(backupFile.exists() && backupFile.length() > 1000L)

        // 7. Completely wipe Room DB + SharedPreferences to simulate fresh device install
        database.individualDao().clearAll()
        database.strikeEventDao().clearAll()
        clearAllPrefs()

        assertTrue(repository.getAllIndividualsList().isEmpty())
        assertTrue(repository.getAllStrikeEventsList().isEmpty())
        assertEquals(0, FlameManager.flameLogs.value.size)
        assertEquals("Cyber Striker", ProfileManager.userName.value)
        assertEquals(0, ProfileManager.eternalRebirthCount.value)
        assertEquals("ar", SecurityPreferences(app).selectedLanguage)

        // 8. Import & Restore with replaceExisting = true
        val importRes = backupManager.importBackup(app, fileUri, replaceExisting = true, lang = "en")
        assertTrue("Import failed: ${importRes.message}", importRes.success)

        // 9. Assert 100% literal reconstruction across all subsystems
        val restoredIndividuals = repository.getAllIndividualsList()
        val restoredEvents = repository.getAllStrikeEventsList()
        assertEquals(2, restoredIndividuals.size)
        assertEquals(2, restoredEvents.size)
        assertEquals("Atlas Cyber", restoredIndividuals.first { it.id == 1L }.fullName)
        assertEquals("+15550200", restoredIndividuals.first { it.id == 2L }.phoneNumber)
        assertEquals("Critical precision", restoredEvents.first { it.id == 501L }.notes)

        assertEquals(expectedFlameScore, FlameManager.flameScore.value, 0.5)
        assertEquals(expectedFlamePeak, FlameManager.peakScore.value, 0.5)
        assertEquals(expectedFlameLogsCount, FlameManager.flameLogs.value.size)
        assertTrue(FlameManager.isStreakShieldActive.value)
        assertTrue(expectedActiveShieldType != ActiveShieldType.NONE)
        assertEquals(expectedActiveShieldType, FlameManager.activeShieldType.value)
        assertEquals(expected1DayShields, FlameManager.available1DayShields.value)
        assertEquals(expected48hShields, FlameManager.available48hShields.value)
        assertEquals(expectedManualShields, FlameManager.availableManualShields.value)

        assertEquals(expectedTotalXp, RoadmapManager.totalXp.value)
        assertEquals(expectedClaimedStations, RoadmapManager.claimedStations.value)
        assertEquals(expectedCompletedTaskIds, RoadmapManager.completedTaskIds.value)
        assertEquals(expectedDailyIds, RoadmapManager.dailyTasks.value.map { it.id })
        assertEquals(expectedWeeklySnapshot, RoadmapManager.weeklyTasks.value.map { it.id to it.currentCount })

        val restoredYearly = RoadmapManager.yearlyTasks.value
        assertEquals(250, restoredYearly.size)
        assertEquals(1, restoredYearly.first { it.id == multiStepAnnual.id }.currentCount)
        assertEquals(TaskStatus.COMPLETED, restoredYearly.last().status)
        assertEquals(completedAnnual.id, restoredYearly.last().id)

        assertEquals("Sovereign Striker", ProfileManager.userName.value)
        assertEquals("av_gabriel_wings", ProfileManager.equippedAvatar.value.id)
        assertEquals(21, ProfileManager.equippedTitle.value.id)
        assertTrue(ProfileManager.unlockedAvatarIds.value.contains("av_gabriel_wings"))
        assertTrue(ProfileManager.unlockedTitleIds.value.contains(21))
        assertEquals(expectedWoodenKeys, ProfileManager.woodenKeysCount.value)
        assertEquals(expectedBronzeKeys, ProfileManager.bronzeKeysCount.value)
        assertEquals(expectedSilverKeys, ProfileManager.silverKeysCount.value)
        assertEquals(expectedPulseElixir, ProfileManager.pulseElixirCount.value)
        assertEquals(expectedShadowSerum, ProfileManager.shadowSerumCount.value)
        assertEquals(expectedCryoInfusion, ProfileManager.cryoInfusionCount.value)
        assertEquals(expectedEternalRebirth, ProfileManager.eternalRebirthCount.value)

        val restoredSec = SecurityPreferences(app)
        assertEquals("en", restoredSec.selectedLanguage)
        assertFalse(restoredSec.isHapticFeedbackEnabled)
        assertFalse(restoredSec.isAutoUpdateEnabled)
        assertTrue(restoredSec.isPatternLockEnabled)
        assertEquals("1-2-3-6-9", restoredSec.savedPattern)
    }
}
