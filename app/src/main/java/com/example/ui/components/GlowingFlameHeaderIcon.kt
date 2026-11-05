package com.example.ui.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PlusJakartaSans
import kotlin.math.PI
import kotlin.math.sin

/**
 * Adaptive animated "Fire" header icon:
 * - A glowing animated flame igniting and burning with rising glowing orange and pink sparks
 * - Smooth motion graphics on dark minimalist liquid glass background
 * - Located immediately to the left of the settings icon in the top toolbar
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
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "header_time"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "header_pulse"
    )

    val buttonBg = if (isDarkTheme) Color(0x33101827) else Color(0xCCFFFFFF)
    val borderColor = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> Color(0x8038BDF8)
        FlameVisualStage.ICE_CRACKING -> Color(0x9900E5FF)
        FlameVisualStage.COLOR_GRADIENT -> colors.second.copy(alpha = 0.7f)
        FlameVisualStage.FULLY_IGNITED -> Color(0xFFF97316).copy(alpha = 0.85f)
    }

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
            .size(38.dp)
            .clip(CircleShape)
            .background(buttonBg)
            .border(1.2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .testTag("header_flame_btn"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f + 2f

            // Dynamic flame sway
            val motion = if (stage == FlameVisualStage.FROZEN_STATIC) 0f else 1f
            val sway = sin(time * 3f) * 2.2f * motion

            // 1. Core Flame Body
            val flameWidth = w * 0.44f
            val flameHeight = h * 0.62f
            val tipX = cx + sway
            val tipY = cy - flameHeight * 0.52f
            val baseY = cy + flameHeight * 0.42f

            val flamePath = Path().apply {
                moveTo(cx, baseY)
                cubicTo(
                    cx - flameWidth * 0.55f, cy + flameHeight * 0.1f,
                    cx - flameWidth * 0.35f, cy - flameHeight * 0.2f,
                    tipX, tipY
                )
                cubicTo(
                    cx + flameWidth * 0.35f, cy - flameHeight * 0.2f,
                    cx + flameWidth * 0.55f, cy + flameHeight * 0.1f,
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

            // 2. Inner Glow Tip
            drawCircle(
                color = Color.White.copy(alpha = if (stage == FlameVisualStage.FROZEN_STATIC) 0.6f else 0.85f),
                radius = 2.dp.toPx(),
                center = Offset(tipX, tipY + 4f)
            )

            // 3. Rising Glowing Orange and Pink Sparks (only when thawing/igniting)
            if (stage != FlameVisualStage.FROZEN_STATIC) {
                for ((idx, spark) in microSparks.withIndex()) {
                    val (xRatio, speed, isPink) = spark
                    val sparkProgress = ((time * speed + idx * 0.45f) % (2f * PI.toFloat())) / (2f * PI.toFloat())

                    val startY = baseY
                    val endY = tipY - 8f
                    val py = startY - sparkProgress * (startY - endY)
                    val px = cx + (xRatio - 0.5f) * flameWidth * 0.9f + sin(sparkProgress * 6f + idx) * 3f

                    val alpha = (sin(sparkProgress * PI.toFloat())).coerceIn(0f, 1f)
                    val sparkColor = if (isPink) {
                        Color(0xFFFB7185).copy(alpha = alpha) // Glowing pink spark
                    } else {
                        Color(0xFFF97316).copy(alpha = alpha) // Glowing orange spark
                    }

                    drawCircle(
                        color = sparkColor,
                        radius = (1.1f + (idx % 3) * 0.4f).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            } else {
                // Frost crystal dot
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        // Small notification / score badge on top-right if score > 0
        if (flameScore > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (stage == FlameVisualStage.FULLY_IGNITED) Color(0xFFEF4444)
                        else if (stage == FlameVisualStage.FROZEN_STATIC) Color(0xFF0284C7)
                        else colors.second
                    )
                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = flameScore.toInt().toString(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    color = Color.White
                )
            }
        }
    }
}
