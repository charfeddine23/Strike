package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Adaptive animated "Fire" header icon:
 * - Clean, borderless, circular-background-free design
 * - Slightly enlarged glowing flame burning with rising glowing orange and pink sparks
 * - Smooth motion graphics and subtle radial aura glow on dark minimalist liquid glass background
 */
@Composable
fun GlowingFlameHeaderIcon(
    flameScore: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val stage = getFlameStage(flameScore)
    val colors = getFlameColors(flameScore)

    val infiniteTransition = rememberInfiniteTransition(label = "header_flame_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "header_time"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "header_pulse"
    )

    // Micro sparks precomputed for allocation-free drawing
    val microSparks = remember {
        List(14) { i ->
            Triple(
                (i * 0.071f) % 1f,
                0.7f + (i % 5) * 0.2f,
                (i % 2 == 0) // alternating orange / pink
            )
        }
    }

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("header_flame_btn"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f + 2f

            // Dynamic flame sway
            val motion = if (stage == FlameVisualStage.FROZEN_STATIC) 0f else 1f
            val sway = sin(time * 3f) * 2.5f * motion

            // 1. Soft underlying radial aura glow (No hard border/background)
            val auraColor = when (stage) {
                FlameVisualStage.FROZEN_STATIC -> Color(0x3338BDF8)
                FlameVisualStage.ICE_CRACKING -> Color(0x4000E5FF)
                FlameVisualStage.COLOR_GRADIENT -> colors.second.copy(alpha = 0.28f * pulseGlow)
                FlameVisualStage.FULLY_IGNITED -> Color(0x55F97316).copy(alpha = 0.35f * pulseGlow)
                FlameVisualStage.SUPERNOVA_OVERLOAD -> Color(0x77C084FC).copy(alpha = 0.45f * pulseGlow)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.65f
                )
            )

            // 2. Core Flame Body (Slightly enlarged for clean prominence)
            val flameWidth = w * 0.52f
            val flameHeight = h * 0.74f
            val tipX = cx + sway
            val tipY = cy - flameHeight * 0.54f
            val baseY = cy + flameHeight * 0.44f

            val flamePath = Path().apply {
                moveTo(cx, baseY)
                cubicTo(
                    cx - flameWidth * 0.55f, cy + flameHeight * 0.12f,
                    cx - flameWidth * 0.35f, cy - flameHeight * 0.22f,
                    tipX, tipY
                )
                cubicTo(
                    cx + flameWidth * 0.35f, cy - flameHeight * 0.22f,
                    cx + flameWidth * 0.55f, cy + flameHeight * 0.12f,
                    cx, baseY
                )
                close()
            }

            drawPath(
                path = flamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(colors.first, colors.second, colors.third),
                    startY = tipY,
                    endY = baseY
                )
            )

            // 3. Inner White-Hot Core / Flame Heart
            drawCircle(
                color = Color.White.copy(alpha = if (stage == FlameVisualStage.FROZEN_STATIC) 0.65f else 0.9f),
                radius = 2.2.dp.toPx(),
                center = Offset(tipX, tipY + 4.5.dp.toPx())
            )

            // 4. Rising Glowing Orange and Pink Sparks (only when thawing/igniting)
            if (stage != FlameVisualStage.FROZEN_STATIC) {
                for ((idx, spark) in microSparks.withIndex()) {
                    val (xRatio, speed, isPink) = spark
                    val sparkProgress = ((time * speed + idx * 0.45f) % (2f * PI.toFloat())) / (2f * PI.toFloat())

                    val startY = baseY
                    val endY = tipY - 10f
                    val py = startY - sparkProgress * (startY - endY)
                    val px = cx + (xRatio - 0.5f) * flameWidth * 0.95f + sin(sparkProgress * 6f + idx) * 3.5f

                    val alpha = (sin(sparkProgress * PI.toFloat())).coerceIn(0f, 1f)
                    val sparkColor = if (isPink) {
                        Color(0xFFFB7185).copy(alpha = alpha) // Glowing pink spark
                    } else {
                        Color(0xFFF97316).copy(alpha = alpha) // Glowing orange spark
                    }

                    drawCircle(
                        color = sparkColor,
                        radius = (1.2f + (idx % 3) * 0.5f).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            } else {
                // Frost crystal core dot
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = 1.8.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
