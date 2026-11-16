package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricAvailability
import com.example.util.FingerprintAuthHelper
import com.example.util.findFragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FingerprintAuthHelperTest {

    @Test
    fun `localized ContextWrapper preserves FragmentActivity and localized resources`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val locale = Locale("ar")
        val config = Configuration(activity.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val resContext = activity.createConfigurationContext(config)
        val wrapped = object : ContextWrapper(activity) {
            override fun getResources(): Resources = resContext.resources
            override fun getAssets(): AssetManager = resContext.assets
            override fun createConfigurationContext(
                overrideConfiguration: Configuration
            ): Context = resContext.createConfigurationContext(overrideConfiguration)
        }

        val resolved = wrapped.findFragmentActivity()
        assertNotNull(resolved)
        assertSame(activity, resolved)
        assertEquals("قفل التطبيق بالبصمة", wrapped.getString(R.string.fingerprint_lock))
    }

    @Test
    fun `checkAvailability returns valid status without crashing`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val status = FingerprintAuthHelper.checkAvailability(activity)
        // Robolectric default without enrolled biometrics reports NONE_ENROLLED or UNAVAILABLE safely
        assert(
            status == BiometricAvailability.AVAILABLE ||
                status == BiometricAvailability.NONE_ENROLLED ||
                status == BiometricAvailability.UNAVAILABLE
        )
    }
}
