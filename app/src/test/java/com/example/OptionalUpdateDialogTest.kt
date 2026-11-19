package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateState
import com.example.data.update.GitHubReleaseInfo
import com.example.ui.components.AppUpdateCardDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class OptionalUpdateDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleRelease = GitHubReleaseInfo(
        tagName = "v2.0.0",
        versionName = "2.0.0",
        releaseName = "Strike v2.0.0",
        releaseNotes = "Optional enhancements",
        htmlUrl = AppUpdateManager.GITHUB_RELEASES_URL,
        apkDownloadUrl = "https://github.com/charfeddine23/Strike/releases/download/v2.0.0/Strike.apk",
        apkFileName = "Strike-v2.0.0.apk",
        apkSizeBytes = 10L * 1024L * 1024L
    )

    @Test
    fun `optional UpdateAvailable dialog allows maybe later and close dismiss`() {
        composeRule.mainClock.autoAdvance = false
        var dismissCount = 0
        var updateClicked = false

        composeRule.setContent {
            AppUpdateCardDialog(
                state = AppUpdateState.UpdateAvailable(sampleRelease),
                currentLang = "ar",
                onUpdateClick = { updateClicked = true },
                onInstallClick = {},
                onRetryClick = {},
                onCancelDownload = {},
                onDismiss = { dismissCount++ }
            )
        }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("optional_update_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("modal_download_update_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("modal_maybe_later_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("modal_close_update_btn").assertIsDisplayed()

        composeRule.onNodeWithTag("modal_maybe_later_btn").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, dismissCount)
        assertFalse(updateClicked)

        composeRule.onNodeWithTag("modal_close_update_btn").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(2, dismissCount)
    }

    @Test
    fun `optional Downloading state shows cancel button and invokes callback`() {
        composeRule.mainClock.autoAdvance = false
        var cancelled = false

        composeRule.setContent {
            AppUpdateCardDialog(
                state = AppUpdateState.Downloading(
                    release = sampleRelease,
                    progress = 0.45f,
                    bytesDownloaded = 4_500_000L,
                    totalBytes = 10_000_000L
                ),
                currentLang = "ar",
                onUpdateClick = {},
                onInstallClick = {},
                onRetryClick = {},
                onCancelDownload = { cancelled = true },
                onDismiss = {}
            )
        }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("modal_cancel_download_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("modal_cancel_download_btn").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertTrue(cancelled)
    }

    @Test
    fun `optional ReadyToInstall state offers maybe later dismiss`() {
        composeRule.mainClock.autoAdvance = false
        var dismissedReady = false
        val fakeApk = File.createTempFile("optional_update_test", ".apk")

        try {
            composeRule.setContent {
                AppUpdateCardDialog(
                    state = AppUpdateState.ReadyToInstall(sampleRelease, fakeApk),
                    currentLang = "ar",
                    onUpdateClick = {},
                    onInstallClick = {},
                    onRetryClick = {},
                    onCancelDownload = {},
                    onDismiss = { dismissedReady = true }
                )
            }
            composeRule.mainClock.advanceTimeByFrame()

            composeRule.onNodeWithTag("modal_install_update_btn").assertIsDisplayed()
            composeRule.onNodeWithTag("modal_maybe_later_btn").assertIsDisplayed()
            composeRule.onNodeWithTag("modal_maybe_later_btn").performClick()
            composeRule.mainClock.advanceTimeByFrame()
            assertTrue(dismissedReady)
        } finally {
            fakeApk.delete()
        }
    }

    @Test
    fun `optional Error state offers maybe later without mandatory label`() {
        composeRule.mainClock.autoAdvance = false
        var dismissedError = false

        composeRule.setContent {
            AppUpdateCardDialog(
                state = AppUpdateState.Error(
                    messageAr = "انقطع التحميل",
                    messageEn = "Download interrupted"
                ),
                currentLang = "ar",
                onUpdateClick = {},
                onInstallClick = {},
                onRetryClick = {},
                onCancelDownload = {},
                onDismiss = { dismissedError = true }
            )
        }
        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(0, composeRule.onAllNodesWithText("تحديث إجباري").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("modal_maybe_later_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("modal_maybe_later_btn").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertTrue(dismissedError)
    }
}
