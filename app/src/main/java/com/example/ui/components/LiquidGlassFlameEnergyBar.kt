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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sleek, luxurious, dynamic, and smooth "Liquid Glass" neon container placed
 * directly under the top header area to display the "Total Flame Energy Points" value (e.g., "3.7")
 * with smooth gradients matching the app's overall cyberpunk dark-mode aesthetic.
 */
@Composable
fun LiquidGlassFlameEnergyBar(
    flameScore: Double,
    onOpenFlame: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val stage = getFlameStage(flameScore)
    val colors = getFlameColors(flameScore)

    val infiniteTransition = rememberInfiniteTransition(label = "energy_bar_anims")

    // Dynamic rotating neon border gradient angle
    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_gradient_angle"
    )

    // Breathing pulse for ambient radial background glow
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_pulse_alpha"
    )

    // Flame shimmer/time for kinetic micro-flame
    val flameTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mini_flame_time"
    )

    // Compute animated rotating border brush
    val angleRad = Math.toRadians(borderAngle.toDouble())
    val dirX = cos(angleRad).toFloat()
    val dirY = sin(angleRad).toFloat()

    val dynamicBorderBrush = Brush.linearGradient(
        colors = listOf(
            colors.first.copy(alpha = if (isDarkTheme) 0.95f else 0.8f),
            colors.second.copy(alpha = if (isDarkTheme) 0.45f else 0.35f),
            NeonViolet.copy(alpha = if (isDarkTheme) 0.65f else 0.4f),
            NeonCyan.copy(alpha = if (isDarkTheme) 0.85f else 0.6f),
            colors.first.copy(alpha = if (isDarkTheme) 0.95f else 0.8f)
        ),
        start = Offset(250f * (1f - dirX), 120f * (1f - dirY)),
        end = Offset(250f * (1f + dirX), 120f * (1f + dirY))
    )

    val glassBgBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                Color(0xE60F172A), // Deep cosmic slate
                Color(0xF0080D18)  // Obsidian bottom
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xF5FFFFFF),
                Color(0xEAF0F7FA)
            )
        )
    }

    val stageLabel = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> "FROZEN"
        FlameVisualStage.ICE_CRACKING -> "THAWING"
        FlameVisualStage.COLOR_GRADIENT -> "GRADIENT"
        FlameVisualStage.FULLY_IGNITED -> "SUPERCHARGED"
        FlameVisualStage.SUPERNOVA_OVERLOAD -> "SUPERNOVA"
    }

    val formattedScore = String.format(Locale.US, "%.1f", flameScore)

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = if (isDarkTheme) 6.dp else 4.dp,
                shape = shape,
                ambientColor = if (isDarkTheme) colors.second.copy(alpha = 0.3f) else Color(0x140284C7),
                spotColor = if (isDarkTheme) colors.first.copy(alpha = 0.4f) else Color(0x1A0F172A)
            )
            .clip(shape)
            .background(glassBgBrush)
            .drawBehind {
                // Dynamic ambient radial glow centered behind score/flame
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.first.copy(alpha = pulseAlpha),
                            colors.second.copy(alpha = pulseAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.5f),
                        radius = size.width * 0.45f
                    )
                )

                // Secondary soft aura on left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = pulseAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.1f, size.height * 0.5f),
                        radius = size.width * 0.35f
                    )
                )

                // Crystalline specular highlight on top edge
                drawLine(
                    color = if (isDarkTheme) Color(0x35FFFFFF) else Color(0x80FFFFFF),
                    start = Offset(16.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 16.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            .border(1.2.dp, dynamicBorderBrush, shape)
            .clickable(onClick = onOpenFlame)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("total_flame_energy_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Kinetic Flame Orb + Labels
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature animated flame canvas
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDarkTheme) Color(0x33101827) else Color(0x1A0284C7)
                        )
                        .border(
                            1.dp,
                            colors.second.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f + 1f

                        val motion = if (stage == FlameVisualStage.FROZEN_STATIC) 0f else 1f
                        val sway = sin(flameTime * 3f) * 1.8f * motion

                        val flameWidth = w * 0.52f
                        val flameHeight = h * 0.72f
                        val tipX = cx + sway
                        val tipY = cy - flameHeight * 0.52f
                        val baseY = cy + flameHeight * 0.40f

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

                        // White glowing core
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = 1.4.dp.toPx(),
                            center = Offset(tipX, tipY + 3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = stringResource(R.string.flame_total_points),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        letterSpacing = 0.8.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Tiny glowing status beacon dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.first)
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        // Stage Badge Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.second.copy(alpha = if (isDarkTheme) 0.2f else 0.15f))
                                .border(
                                    0.75.dp,
                                    colors.first.copy(alpha = if (isDarkTheme) 0.6f else 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stageLabel,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                                letterSpacing = 0.4.sp,
                                color = colors.first
                            )
                        }
                    }
                }
            }

            // Right Side: Prominent Score Numeric Value + Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formattedScore,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        letterSpacing = (-0.5).sp,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "PTS",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        color = colors.first,
                        modifier = Modifier.padding(bottom = 2.5.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.first.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
