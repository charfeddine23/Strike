package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Individual
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StrikePulse", appName)
    }

    @Test
    fun `verify size auto calculation`() {
        val length = 20.0
        val width = 15.0
        val expectedSize = 300.0
        val actualSize = Individual.calculateSize(length, width)
        assertEquals(expectedSize, actualSize, 0.001)
    }

    @Test
    fun `verify boost auto calculation formula`() {
        val s = 80.0
        val f = 75.0
        val c = 95.0
        val size = 200.0
        // Formula: (s + f + c) + (size / 100) = (80 + 75 + 95) + (200 / 100) = 250 + 2 = 252.0
        val expectedBoost = 252.0
        val actualBoost = Individual.calculateBoost(s, f, c, size)
        assertEquals(expectedBoost, actualBoost, 0.001)
    }
}
