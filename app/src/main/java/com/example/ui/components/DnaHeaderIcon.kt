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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.util.HapticHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sleek, vertical, 3D-styled DNA double helix icon placed on the left of the main flame icon.
 * Rotates smoothly in 3D perspective with neon particles and navigates to the Deviations screen.
 */
@Composable
fun DnaHeaderIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isHapticEnabled: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "dna_header_rot")
    val rotationRad by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dna_rot"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dna_pulse"
    )

    val auraColor = if (isDarkTheme) NeonCyan else Color(0xFF0284C7)
    val bg = if (isDarkTheme) Color(0xD90A1329) else Color(0xCCF0FDF4)
    val borderBrush = Brush.sweepGradient(
        listOf(
            NeonCyan,
            NeonViolet,
            NeonEmerald,
            NeonCyan
        )
    )

    Box(
        modifier = modifier
            .size(38.dp)
            .scale(pulse)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor.copy(alpha = 0.35f),
                            NeonViolet.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f
                )
            }
            .background(bg)
            .border(1.2.dp, borderBrush, CircleShape)
            .clickable {
                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onClick()
            }
            .testTag("dna_header_icon"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val radiusX = width * 0.36f

            val rungs = 7
            for (i in 0 until rungs) {
                val y = (height * 0.15f) + (i.toFloat() / (rungs - 1)) * (height * 0.70f)
                val phase = rotationRad + (i * 0.65f)

                val sinA = sin(phase)
                val cosA = cos(phase)

                val x1 = centerX + radiusX * cosA
                val x2 = centerX - radiusX * cosA

                val z1 = sinA // -1 to 1
                val z2 = -sinA

                // Draw connector rung
                val rungAlpha = (0.35f + (1f - kotlin.math.abs(sinA)) * 0.55f).coerceIn(0.2f, 1f)
                drawLine(
                    color = Color.White.copy(alpha = rungAlpha * 0.6f),
                    start = Offset(x1, y),
                    end = Offset(x2, y),
                    strokeWidth = 1.2f,
                    cap = StrokeCap.Round
                )

                // Draw node 1 (Strand A - Cyan/Emerald)
                val radius1 = if (z1 > 0) 2.2f else 1.4f
                val color1 = if (z1 > 0) NeonCyan else NeonCyan.copy(alpha = 0.45f)
                drawCircle(
                    color = color1,
                    radius = radius1,
                    center = Offset(x1, y)
                )

                // Draw node 2 (Strand B - Violet/Magenta)
                val radius2 = if (z2 > 0) 2.2f else 1.4f
                val color2 = if (z2 > 0) NeonViolet else NeonViolet.copy(alpha = 0.45f)
                drawCircle(
                    color = color2,
                    radius = radius2,
                    center = Offset(x2, y)
                )
            }
        }
    }
}
