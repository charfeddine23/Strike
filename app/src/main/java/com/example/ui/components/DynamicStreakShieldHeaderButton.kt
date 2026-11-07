package com.example.ui.components

import android.view.SoundEffectConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.util.HapticHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Luxurious, animated, dynamic shield element placed next to the flame in the header:
 * - If shield is active/available: Displays a dynamic animated shield icon with emerald neon pulse.
 * - If no shield is active: Displays a glowing, luxurious "+" icon with dynamic background & effects
 *   indicating that the streak is currently unprotected.
 */
@Composable
fun DynamicStreakShieldHeaderButton(
    isShieldActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isHapticEnabled: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "shield_button_anim")

    // Dynamic rotation angle for shimmer border
    val angleDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_angle"
    )

    // Breathing pulse for energetic aura
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_pulse"
    )

    val rad = Math.toRadians(angleDeg.toDouble())
    val dirX = cos(rad).toFloat()
    val dirY = sin(rad).toFloat()

    val scaleBreath = 1f + sin(pulse) * 0.035f

    if (isShieldActive) {
        // 1. ACTIVE SHIELD: Luxurious glowing animated emerald shield
        val emeraldBorderBrush = Brush.linearGradient(
            colors = listOf(
                NeonEmerald,
                Color(0xFF34D399),
                Color(0xFF059669),
                NeonEmerald
            ),
            start = Offset(40f * (1f - dirX), 40f * (1f - dirY)),
            end = Offset(40f * (1f + dirX), 40f * (1f + dirY))
        )

        Box(
            modifier = modifier
                .size(38.dp)
                .scale(scaleBreath)
                .clip(CircleShape)
                .drawBehind {
                    val radius = size.width * (0.65f + sin(pulse) * 0.08f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonEmerald.copy(alpha = 0.35f),
                                Color(0x15059669),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius
                    )
                }
                .background(Color(0xD9064E3B))
                .border(1.5.dp, emeraldBorderBrush, CircleShape)
                .clickable {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onClick()
                }
                .testTag("dynamic_shield_active_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Streak Shield Active (Protected)",
                tint = Color(0xFF6EE7B7),
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        // 2. UNPROTECTED STREAK: Glowing luxurious "+" icon indicating unprotected streak!
        val warningPulse = (sin(pulse * 1.5f) + 1f) / 2f
        val warningBorderBrush = Brush.linearGradient(
            colors = listOf(
                NeonAmber,
                NeonRose.copy(alpha = 0.85f),
                Color(0xFFF97316),
                NeonAmber
            ),
            start = Offset(40f * (1f - dirX), 40f * (1f - dirY)),
            end = Offset(40f * (1f + dirX), 40f * (1f + dirY))
        )

        Box(
            modifier = modifier
                .size(38.dp)
                .scale(scaleBreath)
                .clip(CircleShape)
                .drawBehind {
                    val radius = size.width * (0.68f + warningPulse * 0.12f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonAmber.copy(alpha = 0.38f + warningPulse * 0.20f),
                                NeonRose.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius
                    )
                }
                .background(Color(0xD91E1B18))
                .border(1.5.dp, warningBorderBrush, CircleShape)
                .clickable {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onClick()
                }
                .testTag("dynamic_shield_unprotected_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Streak Unprotected - Tap + to Activate Shield",
                tint = Color(0xFFFDE047),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
