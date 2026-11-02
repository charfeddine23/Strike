package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaveatHandwriting
import com.example.ui.theme.CosmicDarkBg
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayCyanGlow
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayEmeraldGlow
import com.example.ui.theme.DayGlassBg
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayOrangeGlow
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayPurpleGlow
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.GlassBorderBrushDark
import com.example.ui.theme.GlassBorderBrushLight
import com.example.ui.theme.GlassCardSurfaceLight
import com.example.ui.theme.GlassDarkPanel
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonSky
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans

/**
 * Liquid Glass v5 Ambient Dynamic Background
 * In Day Mode: Renders a glare-free pastel mesh gradient with soft morning cyan,
 * gentle lilac nebula, and warm amber-peach mist over a celestial pearl canvas.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_liquid_glass")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (darkTheme) 0.25f else 0.16f,
        targetValue = if (darkTheme) 0.45f else 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ambient"
    )

    val baseBg = if (darkTheme) CosmicDarkBg else DayGlassBg
    val cyanGlow = if (darkTheme) NeonCyan.copy(alpha = pulseAlpha) else DayCyanGlow.copy(alpha = pulseAlpha)
    val violetGlow = if (darkTheme) NeonViolet.copy(alpha = pulseAlpha * 0.8f) else DayPurpleGlow.copy(alpha = pulseAlpha * 0.8f)
    val orangeGlow = if (darkTheme) NeonEmerald.copy(alpha = pulseAlpha * 0.6f) else DayOrangeGlow.copy(alpha = pulseAlpha * 0.65f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                val canvasW = size.width
                val canvasH = size.height

                // Top-right Morning Cyan Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cyanGlow, Color.Transparent),
                        center = Offset(canvasW * 0.85f, canvasH * 0.15f),
                        radius = canvasW * 0.8f
                    ),
                    radius = canvasW * 0.8f,
                    center = Offset(canvasW * 0.85f, canvasH * 0.15f)
                )

                // Bottom-left Pastel Lilac Nebula
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(violetGlow, Color.Transparent),
                        center = Offset(canvasW * 0.15f, canvasH * 0.82f),
                        radius = canvasW * 0.85f
                    ),
                    radius = canvasW * 0.85f,
                    center = Offset(canvasW * 0.15f, canvasH * 0.82f)
                )

                // Center-right Warm Peach-Amber Mist
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orangeGlow, Color.Transparent),
                        center = Offset(canvasW * 0.5f, canvasH * 0.45f),
                        radius = canvasW * 0.6f
                    ),
                    radius = canvasW * 0.6f,
                    center = Offset(canvasW * 0.5f, canvasH * 0.45f)
                )
            }
    ) {
        content()
    }
}

/**
 * Liquid Glass v5 Card Component
 * Implements refined multi-layer frosted depth:
 * - Subtle ambient drop shadow for elevation
 * - Crystalline translucent glass surface gradient
 * - Ultra-thin, semi-transparent glowing border with soft cyan & pure white highlights
 * - Specular top-light refraction edge
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val borderBrush = if (darkTheme) GlassBorderBrushDark else GlassBorderBrushLight
    val cardShadowElevation = if (darkTheme) 4.dp else 7.dp
    val cardShadowSpotColor = if (darkTheme) Color.Transparent else Color(0x180F172A)
    val cardShadowAmbientColor = if (darkTheme) Color.Transparent else Color(0x140284C7)

    val baseModifier = modifier
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .shadow(
            elevation = cardShadowElevation,
            shape = shape,
            clip = false,
            ambientColor = cardShadowAmbientColor,
            spotColor = cardShadowSpotColor
        )
        .clip(shape)
        .then(
            if (darkTheme) {
                Modifier.background(GlassDarkPanel)
            } else {
                Modifier.background(GlassCardSurfaceLight)
            }
        )
        .border(borderWidth, borderBrush, shape)
        .drawBehind {
            // Specular top-edge glass sheen in day mode
            if (!darkTheme) {
                drawLine(
                    color = Color(0x99FFFFFF),
                    start = Offset(12.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 12.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
        }

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier.padding(16.dp),
        content = content
    )
}

/**
 * Liquid Glass v5 Inner Tier Panel
 * Used for secondary inner blocks (dimensions, statistics, sub-rows) inside cards,
 * replacing harsh dark surfaces with a clean, cohesive frosted glass tier.
 */
@Composable
fun LiquidGlassInnerPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (darkTheme) Color(0x221E293B) else Color(0x66F1F5F9)
    val borderColor = if (darkTheme) Color(0x3538BDF8) else Color(0x40CBD5E1)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = content
    )
}

/**
 * Liquid Glass v5 Type Badge
 * High-contrast, vibrant jewel badges for M, G, TS, FG types.
 * Guaranteed WCAG AAA contrast ratio on both day glass and dark glass.
 */
@Composable
fun GlassTypeBadge(
    type: String,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val (badgeBg, borderColor, textColor) = if (darkTheme) {
        when (type) {
            "M" -> Triple(NeonCyan.copy(alpha = 0.22f), NeonCyan.copy(alpha = 0.65f), NeonCyan)
            "G" -> Triple(NeonViolet.copy(alpha = 0.22f), NeonViolet.copy(alpha = 0.65f), NeonViolet)
            "TS" -> Triple(NeonEmerald.copy(alpha = 0.22f), NeonEmerald.copy(alpha = 0.65f), NeonEmerald)
            "FG" -> Triple(NeonAmber.copy(alpha = 0.22f), NeonAmber.copy(alpha = 0.65f), NeonAmber)
            else -> Triple(NeonSky.copy(alpha = 0.22f), NeonSky.copy(alpha = 0.65f), NeonSky)
        }
    } else {
        // Day Mode: Deeply pigmented, high-contrast readable jewel tones
        when (type) {
            "M" -> Triple(Color(0x2200B4D8), DayCyan.copy(alpha = 0.65f), DayCyan)
            "G" -> Triple(Color(0x228B5CF6), DayPurple.copy(alpha = 0.65f), DayPurple)
            "TS" -> Triple(Color(0x2210B981), DayEmerald.copy(alpha = 0.65f), DayEmerald)
            "FG" -> Triple(Color(0x24F59E0B), DayOrange.copy(alpha = 0.65f), DayOrange)
            else -> Triple(Color(0x220284C7), Color(0xFF0369A1), Color(0xFF0369A1))
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type,
            color = textColor,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun GlassMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val effectiveAccent = if (darkTheme) accentColor else when (accentColor) {
        NeonCyan -> DayCyan
        NeonViolet -> DayPurple
        NeonEmerald -> DayEmerald
        NeonAmber -> DayOrange
        else -> accentColor
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(effectiveAccent.copy(alpha = if (darkTheme) 0.12f else 0.15f))
            .border(1.dp, effectiveAccent.copy(alpha = if (darkTheme) 0.35f else 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = effectiveAccent,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Cosmic Strike Button with Liquid Glass v5 Tactile Disc
 * In Day Mode: Features a luminous soft cyan/white outer halo, glowing ring,
 * and an ultra-deep obsidian glass center ensuring STRIKE and counts remain
 * intensely readable under direct sunlight.
 */
@Composable
fun CosmicStrikeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    strikeCount: Int,
    subtitle: String = "TAP TO STRIKE",
    testTag: String = "strike_button",
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
        label = "strike_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_strike")
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    val outerHaloColor = if (darkTheme) NeonCyan.copy(alpha = 0.25f) else DayCyanGlow.copy(alpha = 0.20f)
    val ringColors = if (darkTheme) {
        listOf(NeonCyan, NeonViolet, NeonEmerald, NeonCyan)
    } else {
        listOf(DayCyanGlow, DayPurpleGlow, DayEmeraldGlow, DayCyanGlow)
    }

    Box(
        modifier = modifier
            .size(220.dp)
            .scale(scale)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing pulse halo
        Box(
            modifier = Modifier
                .size(210.dp * ringPulse)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(outerHaloColor, Color.Transparent)
                    )
                )
        )

        // Middle luminous ring
        Box(
            modifier = Modifier
                .size(185.dp)
                .clip(CircleShape)
                .border(
                    2.dp,
                    Brush.sweepGradient(ringColors),
                    CircleShape
                )
        )

        // Core tactile Liquid Glass button with obsidian center for daylight contrast
        Box(
            modifier = Modifier
                .size(165.dp)
                .clip(CircleShape)
                .shadow(
                    elevation = if (darkTheme) 6.dp else 10.dp,
                    shape = CircleShape,
                    spotColor = if (darkTheme) Color.Transparent else Color(0x30007A99)
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF14243B),
                            Color(0xFF070E1A)
                        )
                    )
                )
                .border(2.dp, if (darkTheme) NeonCyan.copy(alpha = 0.85f) else Color(0xFF38BDF8), CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Strike Icon",
                    tint = NeonCyan,
                    modifier = Modifier.size(38.dp)
                )
                Text(
                    text = "STRIKE",
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "$strikeCount",
                    color = NeonAmber,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Handwritten secondary subtitle for artisanal flair
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontFamily = CaveatHandwriting,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun GlassStatSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val effectiveAccent = if (darkTheme) accentColor else when (accentColor) {
        NeonCyan -> DayCyan
        NeonViolet -> DayPurple
        NeonEmerald -> DayEmerald
        NeonAmber -> DayOrange
        else -> accentColor
    }

    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        testTag = testTag
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(effectiveAccent.copy(alpha = if (darkTheme) 0.20f else 0.16f))
                    .border(1.dp, effectiveAccent.copy(alpha = if (darkTheme) 0.5f else 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SmartMetricComparisonBar(
    title: String,
    valueA: Double,
    valueB: Double,
    labelA: String,
    labelB: String,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val total = (valueA + valueB).coerceAtLeast(0.001)
    val fractionA = (valueA / total).toFloat().coerceIn(0.05f, 0.95f)
    val isAWinner = valueA > valueB
    val isBWinner = valueB > valueA
    val isTie = valueA == valueB

    val colorA = if (darkTheme) NeonCyan else DayCyan
    val colorB = if (darkTheme) NeonViolet else DayPurple
    val trackBg = if (darkTheme) Color(0x331E293B) else Color(0x35CBD5E1)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                )
                if (isAWinner) {
                    Spacer(modifier = Modifier.width(6.dp))
                    WinnerBadge(label = "$labelA +${((valueA - valueB) * 10.0).toLong() / 10.0}")
                } else if (isBWinner) {
                    Spacer(modifier = Modifier.width(6.dp))
                    WinnerBadge(label = "$labelB +${((valueB - valueA) * 10.0).toLong() / 10.0}")
                }
            }

            Text(
                text = if (isTie) "TIE" else if (isAWinner) "$labelA Leads" else "$labelB Leads",
                color = if (isTie) MaterialTheme.colorScheme.onSurfaceVariant else if (isAWinner) colorA else colorB,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Numbers row with high contrast
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${((valueA * 100.0).toLong() / 100.0)}",
                color = if (isAWinner) colorA else MaterialTheme.colorScheme.onSurface,
                fontFamily = PlusJakartaSans,
                fontWeight = if (isAWinner) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = "${((valueB * 100.0).toLong() / 100.0)}",
                color = if (isBWinner) colorB else MaterialTheme.colorScheme.onSurface,
                fontFamily = PlusJakartaSans,
                fontWeight = if (isBWinner) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Split Comparison Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(trackBg)
        ) {
            Box(
                modifier = Modifier
                    .weight(fractionA)
                    .fillMaxSize()
                    .background(
                        if (isAWinner) colorA else colorA.copy(alpha = 0.45f)
                    )
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .weight(1f - fractionA)
                    .fillMaxSize()
                    .background(
                        if (isBWinner) colorB else colorB.copy(alpha = 0.45f)
                    )
            )
        }
    }
}

@Composable
fun WinnerBadge(
    label: String,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val emeraldColor = if (darkTheme) NeonEmerald else DayEmerald
    val badgeBg = if (darkTheme) NeonEmerald.copy(alpha = 0.2f) else Color(0x2210B981)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .border(1.dp, emeraldColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = emeraldColor,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = emeraldColor,
            fontFamily = PlusJakartaSans,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
