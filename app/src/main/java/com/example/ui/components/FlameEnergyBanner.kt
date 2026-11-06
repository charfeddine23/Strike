package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.PlusJakartaSans
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sleek, luxurious, dynamic, and smooth "Liquid Glass" neon container placed
 * directly under the top header area to display the "Total Flame Energy Points" value.
 */
@Composable
fun FlameEnergyBanner(
    flameScore: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val stage = getFlameStage(flameScore)
    val colors = getFlameColors(flameScore)

    val infiniteTransition = rememberInfiniteTransition(label = "banner_ambient_anim")

    // Dynamic rotating border angle
    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "banner_border_angle"
    )

    // Breathing glow intensity
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_glow_pulse"
    )

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "banner_flame_time"
    )

    // Stage label and primary accent color
    val (stageLabelRes, stageAccentColor) = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> Pair(R.string.flame_stage_frozen, Color(0xFF38BDF8))
        FlameVisualStage.ICE_CRACKING -> Pair(R.string.flame_stage_cracking, Color(0xFF00E5FF))
        FlameVisualStage.COLOR_GRADIENT -> Pair(R.string.flame_stage_gradient, colors.second)
        FlameVisualStage.FULLY_IGNITED -> Pair(R.string.flame_stage_ignited, Color(0xFFF97316))
        FlameVisualStage.SUPERNOVA_OVERLOAD -> Pair(R.string.flame_stage_supernova, Color(0xFFC084FC))
    }

    // Dynamic multi-stop neon gradient border
    val angleRad = Math.toRadians(borderAngle.toDouble())
    val dirX = cos(angleRad).toFloat()
    val dirY = sin(angleRad).toFloat()

    val dynamicBorderBrush = Brush.linearGradient(
        colors = listOf(
            colors.first.copy(alpha = glowPulse * 0.9f),
            NeonCyan.copy(alpha = 0.6f),
            NeonRose.copy(alpha = 0.75f),
            colors.second.copy(alpha = glowPulse)
        ),
        start = Offset(200f * (1f - dirX), 60f * (1f - dirY)),
        end = Offset(200f * (1f + dirX), 60f * (1f + dirY))
    )

    val shape = RoundedCornerShape(16.dp)
    val cardBgColor = if (isDarkTheme) Color(0xD90B1324) else Color(0xF2FFFFFF)
    val shadowColor = if (isDarkTheme) colors.second.copy(alpha = 0.25f * glowPulse) else Color(0x180284C7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = if (isDarkTheme) 6.dp else 4.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(cardBgColor)
            .drawBehind {
                // Living liquid glass dynamic radial illumination
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.second.copy(alpha = 0.18f * glowPulse),
                            colors.first.copy(alpha = 0.08f * glowPulse),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.5f),
                        radius = size.width * 0.65f
                    )
                )
                // Left flame aura reflection
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.second.copy(alpha = 0.15f * glowPulse),
                            Color.Transparent
                        ),
                        center = Offset(36.dp.toPx(), size.height * 0.5f),
                        radius = 48.dp.toPx()
                    )
                )
                // Specular top light refraction line
                drawLine(
                    color = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x80FFFFFF),
                    start = Offset(16.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 16.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            .border(1.2.dp, dynamicBorderBrush, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("flame_energy_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Micro Animated Flame & Labels
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Miniature Canvas Flame
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(30.dp)) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f + 1f

                        val motion = if (stage == FlameVisualStage.FROZEN_STATIC) 0f else 1f
                        val sway = sin(time * 3f) * 1.8f * motion

                        val flameW = w * 0.48f
                        val flameH = h * 0.68f
                        val tipX = cx + sway
                        val tipY = cy - flameH * 0.52f
                        val baseY = cy + flameH * 0.44f

                        val path = Path().apply {
                            moveTo(cx, baseY)
                            cubicTo(
                                cx - flameW * 0.55f, cy + flameH * 0.1f,
                                cx - flameW * 0.35f, cy - flameH * 0.2f,
                                tipX, tipY
                            )
                            cubicTo(
                                cx + flameW * 0.35f, cy - flameH * 0.2f,
                                cx + flameW * 0.55f, cy + flameH * 0.1f,
                                cx, baseY
                            )
                            close()
                        }

                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(colors.first, colors.second, colors.third),
                                startY = tipY,
                                endY = baseY
                            )
                        )

                        // Inner white-hot spark
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 1.8.dp.toPx(),
                            center = Offset(tipX, tipY + 3.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.flame_total_points),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            letterSpacing = 0.8.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing status dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(stageAccentColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(stageLabelRes),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            color = stageAccentColor
                        )
                    }
                }
            }

            // Right: Glowing Score Value & Chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Score Numeral (e.g. 3.7)
                Text(
                    text = String.format(Locale.US, "%.1f", flameScore),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                    letterSpacing = (-0.5).sp,
                    color = colors.second
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "PTS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.second.copy(alpha = 0.8f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
