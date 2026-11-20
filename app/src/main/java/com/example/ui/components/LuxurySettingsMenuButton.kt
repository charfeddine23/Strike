package com.example.ui.components

import android.view.SoundEffectConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.util.HapticHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Luxurious 3-stacked-horizontal-lines Settings Menu Button for the Top Banner.
 * Renders three stepped metallic-neon bars inside a frosted Liquid Glass squircle
 * with animated aura glow, 48dp accessible interactive touch target, ripple & haptic feedback.
 */
@Composable
fun LuxurySettingsMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    isHapticEnabled: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current
    val cdText = stringResource(R.string.settings)

    val infiniteTransition = rememberInfiniteTransition(label = "luxury_menu_anim")
    val angleDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "luxury_menu_angle"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "luxury_menu_pulse"
    )

    val rad = Math.toRadians(angleDeg.toDouble())
    val dirX = cos(rad).toFloat()
    val dirY = sin(rad).toFloat()
    val pulseFactor = (sin(glowPulse) + 1f) * 0.5f

    val borderBrush = Brush.linearGradient(
        colors = if (isDarkTheme) {
            listOf(
                NeonCyan,
                NeonViolet.copy(alpha = 0.85f),
                NeonAmber.copy(alpha = 0.8f),
                NeonCyan
            )
        } else {
            listOf(
                Color(0xFF0284C7),
                Color(0xFF6366F1),
                Color(0xFF0EA5E9),
                Color(0xFF0284C7)
            )
        },
        start = Offset(42f * (1f - dirX), 42f * (1f - dirY)),
        end = Offset(42f * (1f + dirX), 42f * (1f + dirY))
    )

    val containerBg = if (isDarkTheme) Color(0xE00A1428) else Color(0xEBF1F5F9)
    val shape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(38.dp)
            .drawBehind {
                val radius = size.maxDimension * (0.68f + pulseFactor * 0.1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = if (isDarkTheme) 0.24f + pulseFactor * 0.12f else 0.16f),
                            NeonViolet.copy(alpha = if (isDarkTheme) 0.10f else 0.06f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )
            }
            .clip(shape)
            .background(containerBg)
            .border(1.3.dp, borderBrush, shape)
            .semantics {
                contentDescription = cdText
            }
            .clickable(role = Role.Button) {
                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onClick()
            }
            .testTag("top_bar_settings_btn"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val barHeight = 2.4.dp.toPx()
            val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

            val topY = h * 0.18f - barHeight / 2f
            val midY = h * 0.50f - barHeight / 2f
            val botY = h * 0.82f - barHeight / 2f

            // Top line: executive full-width glowing bar + subtle gold highlight dot
            val topWidth = w * 0.80f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = if (isDarkTheme) {
                        listOf(NeonCyan, Color(0xFFA5F3FC))
                    } else {
                        listOf(Color(0xFF0369A1), Color(0xFF0EA5E9))
                    }
                ),
                topLeft = Offset(0f, topY),
                size = Size(topWidth, barHeight),
                cornerRadius = cornerRadius
            )
            drawCircle(
                color = if (isDarkTheme) NeonAmber else Color(0xFFF59E0B),
                radius = barHeight * 0.62f,
                center = Offset(w - barHeight * 0.7f, topY + barHeight / 2f)
            )

            // Middle line: sleekly indented/offset cyber-violet accent bar
            val midWidth = w * 0.76f
            val midStartX = w - midWidth
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF818CF8), NeonCyan)
                    } else {
                        listOf(Color(0xFF4F46E5), Color(0xFF0284C7))
                    }
                ),
                topLeft = Offset(midStartX, midY),
                size = Size(midWidth, barHeight),
                cornerRadius = cornerRadius
            )

            // Bottom line: strong anchor bar with luminous tip
            val botWidth = w * 0.90f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF34D399), NeonCyan)
                    } else {
                        listOf(Color(0xFF0F766E), Color(0xFF0EA5E9))
                    }
                ),
                topLeft = Offset(0f, botY),
                size = Size(botWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
