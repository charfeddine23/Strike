package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    UNAVAILABLE
}

/**
 * Unwraps localized/configuration ContextWrappers to obtain the host FragmentActivity
 * required by androidx.biometric.BiometricPrompt.
 */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/**
 * Safely resolves the hosting FragmentActivity from either LocalContext or LocalActivityResultRegistryOwner.
 */
@Composable
fun rememberFragmentActivity(): FragmentActivity? {
    val context = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current
    return remember(context, registryOwner) {
        context.findFragmentActivity() ?: (registryOwner as? FragmentActivity)
    }
}

object FingerprintAuthHelper {

    fun checkAvailability(context: Context): BiometricAvailability {
        val hostContext = context.findFragmentActivity() ?: context
        val manager = BiometricManager.from(hostContext)

        val biometricWeakOrStrong = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

        val primaryStatus = manager.canAuthenticate(biometricWeakOrStrong)
        if (primaryStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricAvailability.AVAILABLE
        }
        if (primaryStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            return BiometricAvailability.NONE_ENROLLED
        }

        // Fallback for OEMs / API 28-29 builds where BIOMETRIC_WEAK reports BIOMETRIC_ERROR_UNSUPPORTED
        val strongStatus = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (strongStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricAvailability.AVAILABLE
        }
        if (strongStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            return BiometricAvailability.NONE_ENROLLED
        }

        // On Android 11+ (API 30+), if biometric hardware is absent/unsupported, allow device credential
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val withDeviceCredential = biometricWeakOrStrong or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            when (manager.canAuthenticate(withDeviceCredential)) {
                BiometricManager.BIOMETRIC_SUCCESS -> return BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> return BiometricAvailability.NONE_ENROLLED
            }
        }

        return BiometricAvailability.UNAVAILABLE
    }

    fun showFingerprintPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cancelButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> onCancel()
                        else -> onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // System dialog remains open and shows retry feedback automatically
                }
            }
        )

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            builder.setNegativeButtonText(cancelButtonText)
        }

        try {
            prompt.authenticate(builder.build())
        } catch (e: Exception) {
            // Fallback without DEVICE_CREDENTIAL if OEM ROM rejects the combination
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val fallbackInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setConfirmationRequired(false)
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                        )
                        .setNegativeButtonText(cancelButtonText)
                        .build()
                    prompt.authenticate(fallbackInfo)
                    return
                } catch (_: Exception) {
                    // Fall through to error callback
                }
            }
            onError(e.localizedMessage ?: "")
        }
    }

    fun openSecuritySettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
                // Ignore if settings activity cannot be resolved
            }
        }
    }
}
