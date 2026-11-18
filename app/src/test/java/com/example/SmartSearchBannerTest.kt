package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.ui.components.SmartSearchBanner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class SmartSearchBannerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun smartSearchBannerSizeRemainsStrictlyFixedWhileTypingAndClearing() {
        var query by mutableStateOf("")

        composeRule.setContent {
            SmartSearchBanner(
                query = query,
                onQueryChange = { query = it },
                onClearQuery = { query = "" },
                matchedCount = if (query.isBlank()) 5 else 2,
                totalCount = 5,
                currentLang = "ar"
            )
        }
        composeRule.waitForIdle()

        val initialBounds = composeRule.onNodeWithTag("smart_search_banner").getBoundsInRoot()
        val initialWidth = initialBounds.right - initialBounds.left
        val initialHeight = initialBounds.bottom - initialBounds.top

        // Type short text
        composeRule.onNodeWithTag("smart_top_search_input").performTextInput("أحمد")
        composeRule.waitForIdle()

        val afterShortBounds = composeRule.onNodeWithTag("smart_search_banner").getBoundsInRoot()
        assertEquals(initialWidth.value, (afterShortBounds.right - afterShortBounds.left).value, 0.5f)
        assertEquals(initialHeight.value, (afterShortBounds.bottom - afterShortBounds.top).value, 0.5f)

        // Type long multi-word text to verify no wrapping or vertical expansion
        composeRule.onNodeWithTag("smart_top_search_input")
            .performTextInput(" اسم طويل جدا جدا لاختبار ثبات حجم حاوية البحث الذكي الفوري")
        composeRule.waitForIdle()

        val afterLongBounds = composeRule.onNodeWithTag("smart_search_banner").getBoundsInRoot()
        assertEquals(initialWidth.value, (afterLongBounds.right - afterLongBounds.left).value, 0.5f)
        assertEquals(initialHeight.value, (afterLongBounds.bottom - afterLongBounds.top).value, 0.5f)

        // Clear via clear button
        composeRule.onNodeWithTag("smart_top_search_clear").performClick()
        composeRule.waitForIdle()

        val afterClearBounds = composeRule.onNodeWithTag("smart_search_banner").getBoundsInRoot()
        assertEquals(initialWidth.value, (afterClearBounds.right - afterClearBounds.left).value, 0.5f)
        assertEquals(initialHeight.value, (afterClearBounds.bottom - afterClearBounds.top).value, 0.5f)
    }
}
