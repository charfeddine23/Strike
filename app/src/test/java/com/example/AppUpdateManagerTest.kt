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
        assertTrue(AppUpdateManager.isNewerVersion("release-1.0.2", "1.0.0"))
        assertTrue(AppUpdateManager.isNewerVersion("Strike-v1.2.0-beta.1", "1.0.0"))
        assertTrue(AppUpdateManager.isNewerVersion("v2", "1.0.0"))
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
    fun `parseReleaseJson handles releases list array skips drafts and picks newest`() {
        val arrayJson = """
            [
              {
                "tag_name": "v9.9.9",
                "draft": true,
                "name": "Draft",
                "assets": []
              },
              {
                "tag_name": "release-1.5.0",
                "draft": false,
                "name": "Release 1.5.0",
                "body": "",
                "html_url": "https://github.com/charfeddine23/Strike/releases/tag/release-1.5.0",
                "assets": []
              },
              {
                "tag_name": "V2.1.0",
                "draft": false,
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
    fun `parseReleasesAtom and parseExpandedAssetsForApk resolve unthrottled releases`() {
        val atom = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <link rel="alternate" type="text/html" href="https://github.com/charfeddine23/Strike/releases/tag/Strike-v1.4.2"/>
                <title>Strike v1.4.2</title>
              </entry>
            </feed>
        """.trimIndent()
        val parsed = AppUpdateManager.parseReleasesAtom(atom)
        assertEquals(1, parsed.size)
        assertEquals("1.4.2", parsed[0].versionName)
        assertEquals("Strike-v1.4.2", parsed[0].tagName)

        val expandedHtml = """
            <a href="/charfeddine23/Strike/releases/download/Strike-v1.4.2/Strike-debug.apk">Strike-debug.apk</a>
            <a href="/charfeddine23/Strike/releases/download/Strike-v1.4.2/Strike-release-1.4.2.apk">Strike-release-1.4.2.apk</a>
        """.trimIndent()
        val apkPair = AppUpdateManager.parseExpandedAssetsForApk(expandedHtml)
        assertNotNull(apkPair)
        assertEquals("Strike-release-1.4.2.apk", apkPair!!.first)
        assertEquals(
            "https://github.com/charfeddine23/Strike/releases/download/Strike-v1.4.2/Strike-release-1.4.2.apk",
            apkPair.second
        )
    }

    @Test
    fun `parseTagsJson and parseRepoContentsForApk extract versioned fallbacks`() {
        val tagsJson = """[{"name": "v1.0.9"}, {"name": "v1.2.3"}]"""
        val tagInfo = AppUpdateManager.parseTagsJson(tagsJson)
        assertNotNull(tagInfo)
        assertEquals("1.2.3", tagInfo!!.versionName)
        assertEquals("https://github.com/charfeddine23/Strike/releases/tag/v1.2.3", tagInfo.htmlUrl)

        val contentsJson = """
            [
              {"type": "file", "name": "README.md", "download_url": "https://raw.githubusercontent.com/charfeddine23/Strike/main/README.md"},
              {"type": "file", "name": "StrikePulse-v1.3.0.apk", "size": 15000000, "download_url": "https://raw.githubusercontent.com/charfeddine23/Strike/main/StrikePulse-v1.3.0.apk", "html_url": "https://github.com/charfeddine23/Strike/blob/main/StrikePulse-v1.3.0.apk"}
            ]
        """.trimIndent()
        val apkContent = AppUpdateManager.parseRepoContentsForApk(contentsJson)
        assertNotNull(apkContent)
        assertEquals("1.3.0", apkContent!!.versionName)
        assertEquals("StrikePulse-v1.3.0.apk", apkContent.apkFileName)
        assertEquals(15000000L, apkContent.apkSizeBytes)
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

    @Test
    fun `dismissPrompt records dismissed release tag and resets state to Idle`() {
        val release = com.example.data.update.GitHubReleaseInfo(
            tagName = "v2.5.0",
            versionName = "2.5.0",
            releaseName = "v2.5.0",
            releaseNotes = "",
            htmlUrl = AppUpdateManager.GITHUB_RELEASES_URL,
            apkDownloadUrl = "https://example.com/app.apk",
            apkFileName = "app.apk",
            apkSizeBytes = 1024L
        )
        val stateField = AppUpdateManager::class.java.getDeclaredField("_state").apply {
            isAccessible = true
        }
        val dismissedField = AppUpdateManager::class.java.getDeclaredField("dismissedTagInSession").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val mutableState = stateField.get(AppUpdateManager) as kotlinx.coroutines.flow.MutableStateFlow<com.example.data.update.AppUpdateState>

        try {
            mutableState.value = com.example.data.update.AppUpdateState.UpdateAvailable(release)
            AppUpdateManager.dismissPrompt()

            assertEquals(com.example.data.update.AppUpdateState.Idle, AppUpdateManager.state.value)
            assertEquals("v2.5.0", dismissedField.get(AppUpdateManager))

            AppUpdateManager.resetToIdle()
            assertNull(dismissedField.get(AppUpdateManager))
        } finally {
            AppUpdateManager.resetToIdle()
        }
    }
}
