package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SecurityPreferences
import com.example.data.update.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppUpdateManagerTest {

    @Test
    fun `isNewerVersion detects semantic upgrades accurately`() {
        assertTrue(AppUpdateManager.isNewerVersion("v1.0.25", "1.0.0"))
        assertTrue(AppUpdateManager.isNewerVersion("2.0", "1.9.9"))
        assertTrue(AppUpdateManager.isNewerVersion("1.1.0", "1.0.99"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("v0.9.9", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("", "1.0.0"))
    }

    @Test
    fun `parseReleaseJson extracts release info and apk asset from latest object`() {
        val json = """
            {
              "tag_name": "v1.0.25",
              "name": "StrikePulse v1.0.25",
              "body": "Performance & auto-update release",
              "html_url": "https://github.com/charfeddine23/Strike/releases/tag/v1.0.25",
              "assets": [
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://github.com/charfeddine23/Strike/releases/download/v1.0.25/checksums.txt",
                  "size": 128
                },
                {
                  "name": "StrikePulse-v1.0.25.apk",
                  "browser_download_url": "https://github.com/charfeddine23/Strike/releases/download/v1.0.25/StrikePulse-v1.0.25.apk",
                  "size": 18432000
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateManager.parseReleaseJson(json)
        assertNotNull(info)
        assertEquals("v1.0.25", info!!.tagName)
        assertEquals("1.0.25", info.versionName)
        assertEquals("StrikePulse v1.0.25", info.releaseName)
        assertEquals("StrikePulse-v1.0.25.apk", info.apkFileName)
        assertEquals(
            "https://github.com/charfeddine23/Strike/releases/download/v1.0.25/StrikePulse-v1.0.25.apk",
            info.apkDownloadUrl
        )
        assertEquals(18432000L, info.apkSizeBytes)
    }

    @Test
    fun `parseReleaseJson handles releases list array and missing apk asset`() {
        val arrayJson = """
            [
              {
                "tag_name": "V2.1.0",
                "name": "",
                "body": "",
                "html_url": "https://github.com/charfeddine23/Strike/releases/tag/V2.1.0",
                "assets": []
              }
            ]
        """.trimIndent()

        val info = AppUpdateManager.parseReleaseJson(arrayJson)
        assertNotNull(info)
        assertEquals("2.1.0", info!!.versionName)
        assertEquals("V2.1.0", info.releaseName)
        assertNull(info.apkDownloadUrl)
        assertNull(info.apkFileName)
        assertEquals(0L, info.apkSizeBytes)
        assertNull(AppUpdateManager.parseReleaseJson("[]"))
        assertNull(AppUpdateManager.parseReleaseJson(""))
    }

    @Test
    fun `formatBytes formats KB and MB cleanly`() {
        assertEquals("—", AppUpdateManager.formatBytes(0L))
        assertEquals("512 KB", AppUpdateManager.formatBytes(512 * 1024L))
        assertEquals("12.5 MB", AppUpdateManager.formatBytes((12.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `autoUpdate preference defaults to true and persists changes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurityPreferences(context)
        assertTrue(prefs.isAutoUpdateEnabled)
        prefs.isAutoUpdateEnabled = false
        assertFalse(prefs.isAutoUpdateEnabled)
        prefs.isAutoUpdateEnabled = true
        assertTrue(prefs.isAutoUpdateEnabled)
    }

    @Test
    fun `checkForUpdates autoCheck preserves UpdateAvailable and ReadyToInstall states`() {
        kotlinx.coroutines.runBlocking {
            val release = com.example.data.update.GitHubReleaseInfo(
                tagName = "v9.9.9",
                versionName = "9.9.9",
                releaseName = "v9.9.9",
                releaseNotes = "",
                htmlUrl = AppUpdateManager.GITHUB_RELEASES_URL,
                apkDownloadUrl = null,
                apkFileName = null,
                apkSizeBytes = 0L
            )
            val stateField = AppUpdateManager::class.java.getDeclaredField("_state").apply {
                isAccessible = true
            }
            @Suppress("UNCHECKED_CAST")
            val mutableState = stateField.get(AppUpdateManager) as kotlinx.coroutines.flow.MutableStateFlow<com.example.data.update.AppUpdateState>

            try {
                val available = com.example.data.update.AppUpdateState.UpdateAvailable(release)
                mutableState.value = available
                val resultAvailable = AppUpdateManager.checkForUpdates(currentVersion = "1.0.0", isAutoCheck = true)
                assertEquals(available, resultAvailable)

                val fakeApk = java.io.File.createTempFile("test_update", ".apk")
                val ready = com.example.data.update.AppUpdateState.ReadyToInstall(release, fakeApk)
                mutableState.value = ready
                val resultReady = AppUpdateManager.checkForUpdates(currentVersion = "1.0.0", isAutoCheck = true)
                assertEquals(ready, resultReady)
                fakeApk.delete()
            } finally {
                AppUpdateManager.resetToIdle()
            }
        }
    }
}
