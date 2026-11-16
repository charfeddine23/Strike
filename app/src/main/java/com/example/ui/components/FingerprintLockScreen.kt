package com.example.ui.components

import android.view.SoundEffectConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CaveatHandwriting
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.util.BiometricAvailability
import com.example.util.FingerprintAuthHelper
import com.example.util.HapticHelper
import com.example.util.rememberFragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class BiometricUiStatus {
    IDLE,
    SUCCESS,
    ERROR
}

/**
 * Fullscreen Fingerprint / Biometric Lock Screen replacing the legacy Pattern Lock screen.
 * Automatically triggers system BiometricPrompt upon entry and supports manual retry.
 */
@Composable
fun FullscreenFingerprintUnlockScreen(
    onUnlockSuccess: () -> Unit,
    isHapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hostActivity = rememberFragmentActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    var uiStatus by remember { mutableStateOf(BiometricUiStatus.IDLE) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var availability by remember(context, hostActivity) {
        mutableStateOf(FingerprintAuthHelper.checkAvailability(hostActivity ?: context))
    }

    val promptTitle = stringResource(R.string.fingerprint_prompt_title)
    val promptSubtitle = stringResource(R.string.fingerprint_prompt_subtitle)
    val cancelLabel = stringResource(R.string.cancel)
    val authFailedMsg = stringResource(R.string.fingerprint_auth_failed)
    val notEnrolledMsg = stringResource(R.string.fingerprint_not_enrolled)
    val unavailableMsg = stringResource(R.string.fingerprint_unavailable)

    DisposableEffect(lifecycleOwner, context, hostActivity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                availability = FingerprintAuthHelper.checkAvailability(hostActivity ?: context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun launchBiometricPrompt() {
        val checkContext = hostActivity ?: context
        val currentAvailability = FingerprintAuthHelper.checkAvailability(checkContext)
        availability = currentAvailability
        if (currentAvailability == BiometricAvailability.NONE_ENROLLED) {
            uiStatus = BiometricUiStatus.ERROR
            statusMessage = notEnrolledMsg
            return
        }
        if (hostActivity == null) {
            uiStatus = BiometricUiStatus.ERROR
            statusMessage = unavailableMsg
            return
        }
        uiStatus = BiometricUiStatus.IDLE
        statusMessage = null
        FingerprintAuthHelper.showFingerprintPrompt(
            activity = hostActivity,
            title = promptTitle,
            subtitle = promptSubtitle,
            cancelButtonText = cancelLabel,
            onSuccess = {
                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                uiStatus = BiometricUiStatus.SUCCESS
                statusMessage = null
                scope.launch {
                    delay(220)
                    onUnlockSuccess()
                }
            },
            onError = { errMsg ->
                uiStatus = BiometricUiStatus.ERROR
                statusMessage = errMsg.ifBlank { authFailedMsg }
            },
            onCancel = {
                uiStatus = BiometricUiStatus.IDLE
                statusMessage = null
            }
        )
    }

    LaunchedEffect(Unit) {
        if (availability == BiometricAvailability.AVAILABLE) {
            launchBiometricPrompt()
        } else if (availability == BiometricAvailability.NONE_ENROLLED) {
            statusMessage = notEnrolledMsg
        } else {
            statusMessage = unavailableMsg
        }
    }

    val overlayBg = if (isDarkTheme) Color(0xEC050B14) else Color(0xF5F1F5F9)
    val primaryColor = if (isDarkTheme) NeonCyan else DayCyan
    val emeraldColor = if (isDarkTheme) NeonEmerald else DayEmerald
    val errorColor = if (isDarkTheme) NeonRose else Color(0xFFDC2626)

    val activeAccent = when (uiStatus) {
        BiometricUiStatus.SUCCESS -> emeraldColor
        BiometricUiStatus.ERROR -> errorColor
        BiometricUiStatus.IDLE -> primaryColor
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fp_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fp_ring_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayBg)
            .padding(24.dp)
            .testTag("fingerprint_unlock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Shield Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.14f))
                    .border(1.dp, primaryColor.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusMessage ?: stringResource(R.string.fingerprint_unlock_prompt),
                fontSize = 15.sp,
                fontFamily = if (statusMessage != null) PlusJakartaSans else CaveatHandwriting,
                color = if (statusMessage != null) errorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = if (statusMessage != null) FontWeight.SemiBold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(34.dp))

            // Interactive Concentric Fingerprint Scanner Orb
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .clickable {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        launchBiometricPrompt()
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(ringScale)
                ) {
                    val outerRadius = size.minDimension / 2f - 4.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeAccent.copy(alpha = 0.22f),
                                NeonViolet.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        radius = outerRadius
                    )
                    drawCircle(
                        color = activeAccent.copy(alpha = 0.35f),
                        radius = outerRadius - 6.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = activeAccent.copy(alpha = 0.58f),
                        radius = outerRadius * 0.78f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    activeAccent.copy(alpha = 0.25f),
                                    Color(0xFF0B1324)
                                )
                            )
                        )
                        .border(2.dp, activeAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiStatus == BiometricUiStatus.SUCCESS) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Fingerprint
                        },
                        contentDescription = stringResource(R.string.fingerprint_unlock_btn),
                        tint = activeAccent,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    launchBiometricPrompt()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color(0xFF06101F)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("fingerprint_unlock_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.fingerprint_unlock_btn),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (availability == BiometricAvailability.NONE_ENROLLED) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { FingerprintAuthHelper.openSecuritySettings(context) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("fingerprint_open_settings_btn")
                ) {
                    Text(
                        text = stringResource(R.string.fingerprint_open_settings),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.fingerprint_lock_desc),
                fontSize = 11.sp,
                color = Color(0x8094A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}
