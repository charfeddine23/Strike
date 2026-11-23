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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
import com.example.ui.theme.LocalAppDarkTheme
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
    darkTheme: Boolean = LocalAppDarkTheme.current,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_liquid_glass")
    val minAlpha = if (darkTheme) 0.25f else 0.16f
    val maxAlpha = if (darkTheme) 0.45f else 0.28f
    val pulseAlphaState = infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ambient"
    )

    val baseBg = if (darkTheme) CosmicDarkBg else DayGlassBg
    val cyanColor = if (darkTheme) NeonCyan else DayCyanGlow
    val violetColor = if (darkTheme) NeonViolet else DayPurpleGlow
    val orangeColor = if (darkTheme) NeonEmerald else DayOrangeGlow

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawWithCache {
                val canvasW = size.width
                val canvasH = size.height
                val c1 = Offset(canvasW * 0.85f, canvasH * 0.15f)
                val r1 = canvasW * 0.8f
                val b1 = Brush.radialGradient(
                    colors = listOf(cyanColor, Color.Transparent),
                    center = c1,
                    radius = r1
                )
                val c2 = Offset(canvasW * 0.15f, canvasH * 0.82f)
                val r2 = canvasW * 0.85f
                val b2 = Brush.radialGradient(
                    colors = listOf(violetColor, Color.Transparent),
                    center = c2,
                    radius = r2
                )
                val c3 = Offset(canvasW * 0.5f, canvasH * 0.45f)
                val r3 = canvasW * 0.6f
                val b3 = Brush.radialGradient(
                    colors = listOf(orangeColor, Color.Transparent),
                    center = c3,
                    radius = r3
                )
                onDrawBehind {
                    val alpha = pulseAlphaState.value
                    drawCircle(brush = b1, radius = r1, center = c1, alpha = alpha)
                    drawCircle(brush = b2, radius = r2, center = c2, alpha = alpha * 0.8f)
                    drawCircle(
                        brush = b3,
                        radius = r3,
                        center = c3,
                        alpha = alpha * (if (darkTheme) 0.6f else 0.65f)
                    )
                }
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
    darkTheme: Boolean = LocalAppDarkTheme.current,
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
    darkTheme: Boolean = LocalAppDarkTheme.current,
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
    darkTheme: Boolean = LocalAppDarkTheme.current
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
    darkTheme: Boolean = LocalAppDarkTheme.current
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
    darkTheme: Boolean = LocalAppDarkTheme.current
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
                    fontSize = 19.sp,
                    modifier = Modifier.testTag("strike_button_total_count")
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
    darkTheme: Boolean = LocalAppDarkTheme.current
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
        darkTheme = darkTheme,
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
    darkTheme: Boolean = LocalAppDarkTheme.current
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
                    WinnerBadge(label = "$labelA +${((valueA - valueB) * 10.0).toLong() / 10.0}", darkTheme = darkTheme)
                } else if (isBWinner) {
                    Spacer(modifier = Modifier.width(6.dp))
                    WinnerBadge(label = "$labelB +${((valueB - valueA) * 10.0).toLong() / 10.0}", darkTheme = darkTheme)
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
    darkTheme: Boolean = LocalAppDarkTheme.current
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

/**
 * Full-bleed edge Fade-in-Black atmospheric scrim + floating iPhone-style Liquid Glass capsule
 * for Top Header and Bottom Navigation Dock.
 */
@Composable
fun IphoneLiquidGlassBarSurface(
    isTopBar: Boolean,
    cornerRadius: Dp,
    outerModifier: Modifier = Modifier,
    capsulePaddingModifier: Modifier = Modifier,
    innerPaddingModifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val fadeScrimBrush = remember(isTopBar, isDarkTheme) {
        if (isTopBar) {
            if (isDarkTheme) {
                Brush.verticalGradient(
                    0.00f to Color(0xF5000000),
                    0.36f to Color(0xCC000000),
                    0.68f to Color(0x7A000000),
                    0.88f to Color(0x2E000000),
                    1.00f to Color.Transparent
                )
            } else {
                Brush.verticalGradient(
                    0.00f to Color(0x94000000),
                    0.36f to Color(0x58000000),
                    0.72f to Color(0x22000000),
                    1.00f to Color.Transparent
                )
            }
        } else {
            if (isDarkTheme) {
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.16f to Color(0x2E000000),
                    0.44f to Color(0x82000000),
                    0.76f to Color(0xD8000000),
                    1.00f to Color(0xFA000000)
                )
            } else {
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.18f to Color(0x1C000000),
                    0.48f to Color(0x52000000),
                    0.80f to Color(0x86000000),
                    1.00f to Color(0xAD000000)
                )
            }
        }
    }

    val glassFillBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                0.00f to Color(0x24FFFFFF),
                0.28f to Color(0x0DFFFFFF),
                0.72f to Color(0x07FFFFFF),
                1.00f to Color(0x16FFFFFF)
            )
        } else {
            Brush.verticalGradient(
                0.00f to Color(0x9EFFFFFF),
                0.36f to Color(0x68FFFFFF),
                0.76f to Color(0x5CEFF6FF),
                1.00f to Color(0x82FFFFFF)
            )
        }
    }

    val fresnelBorderBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0x9EFFFFFF),
                    Color(0x4838BDF8),
                    Color(0x22FFFFFF),
                    Color(0x5CFFFFFF)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xEEFFFFFF),
                    Color(0x660284C7),
                    Color(0x44FFFFFF),
                    Color(0x9EFFFFFF)
                )
            )
        }
    }

    Box(
        modifier = outerModifier
            .fillMaxWidth()
            .background(fadeScrimBrush)
    ) {
        Box(
            modifier = capsulePaddingModifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDarkTheme) 12.dp else 14.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color(0x55000000),
                    spotColor = Color(0x66000000)
                )
                .clip(shape)
                .background(glassFillBrush)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    // Soft upper-left liquid light glare
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isDarkTheme) Color(0x1EFFFFFF) else Color(0x44FFFFFF),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.16f, h * 0.08f),
                            radius = w * 0.42f
                        ),
                        center = Offset(w * 0.16f, h * 0.08f),
                        radius = w * 0.42f
                    )
                    // Subtle prismatic cyan-violet caustic whisper on the right arc
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isDarkTheme) Color(0x1838BDF8) else Color(0x180284C7),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.86f, h * 0.18f),
                            radius = w * 0.34f
                        ),
                        center = Offset(w * 0.86f, h * 0.18f),
                        radius = w * 0.34f
                    )
                    // Top curved specular meniscus sheen
                    val inset = cornerRadius.toPx().coerceAtMost(w * 0.25f) * 0.55f
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x70FFFFFF),
                                if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xF5FFFFFF),
                                Color(0x70FFFFFF),
                                Color.Transparent
                            )
                        ),
                        start = Offset(inset, 1.1.dp.toPx()),
                        end = Offset(w - inset, 1.1.dp.toPx()),
                        strokeWidth = 1.25.dp.toPx()
                    )
                    // Subtle bottom rim bounce reflection
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (isDarkTheme) Color(0x38FFFFFF) else Color(0x66FFFFFF),
                                Color.Transparent
                            )
                        ),
                        start = Offset(inset * 1.2f, h - 1.dp.toPx()),
                        end = Offset(w - inset * 1.2f, h - 1.dp.toPx()),
                        strokeWidth = 0.85.dp.toPx()
                    )
                }
                .border(1.15.dp, fresnelBorderBrush, shape)
                .then(innerPaddingModifier),
            content = content
        )
    }
}

private fun isRobolectricRuntime(): Boolean =
    Build.FINGERPRINT.contains("robolectric", ignoreCase = true) ||
        Build.HARDWARE.contains("robolectric", ignoreCase = true)

/**
 * High-FPS Progressive Top & Bottom Edge Fade-In Blur Modifier.
 * On Android 12+ (API 31+) real hardware devices, records the viewport once into a GPU
 * GraphicsLayer (zero Composable tree duplication) and renders two compact edge-height
 * blurred slices (top & bottom) masked with a smooth cubic vertical BlendMode.DstIn gradient.
 * Below API 31 (and in Robolectric JVM tests), acts as a pass-through paired cleanly with
 * [TopBottomEdgeFadeBlurOverlay] atmospheric frosted feathering.
 */
fun Modifier.edgeFadeInBlur(
    topHeight: Dp,
    bottomHeight: Dp,
    blurRadius: Dp = 18.dp
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || isRobolectricRuntime()) {
        return@composed this
    }

    val contentLayer = rememberGraphicsLayer()
    val topBlurLayer = rememberGraphicsLayer()
    val bottomBlurLayer = rememberGraphicsLayer()
    val offscreenPaint = remember { Paint() }

    this.drawWithContent {
        val totalW = size.width
        val totalH = size.height
        val topH = topHeight.toPx().coerceIn(0f, totalH * 0.35f)
        val bottomH = bottomHeight.toPx().coerceIn(0f, totalH * 0.35f)
        val radiusPx = blurRadius.toPx().coerceAtLeast(1f)

        if (totalW < 2f || totalH < 2f || (topH < 2f && bottomH < 2f)) {
            drawContent()
            return@drawWithContent
        }

        // Single-pass capture + crisp base pass
        contentLayer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(contentLayer)

        val renderBlur = AndroidRenderEffect.createBlurEffect(
            radiusPx,
            radiusPx,
            Shader.TileMode.CLAMP
        ).asComposeRenderEffect()

        val wInt = totalW.toInt().coerceAtLeast(1)

        if (topH >= 2f) {
            val topHInt = topH.toInt().coerceAtLeast(1)
            topBlurLayer.renderEffect = renderBlur
            topBlurLayer.record(size = IntSize(wInt, topHInt)) {
                drawLayer(contentLayer)
            }
            val topMaskBrush = Brush.verticalGradient(
                0.00f to Color.White,
                0.38f to Color.White.copy(alpha = 0.88f),
                0.68f to Color.White.copy(alpha = 0.42f),
                0.88f to Color.White.copy(alpha = 0.12f),
                1.00f to Color.Transparent,
                startY = 0f,
                endY = topH
            )
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(0f, 0f, totalW, topH), offscreenPaint)
                drawLayer(topBlurLayer)
                drawRect(
                    brush = topMaskBrush,
                    topLeft = Offset.Zero,
                    size = Size(totalW, topH),
                    blendMode = BlendMode.DstIn
                )
                canvas.restore()
            }
        }

        if (bottomH >= 2f) {
            val bottomHInt = bottomH.toInt().coerceAtLeast(1)
            val bottomStartY = (totalH - bottomH).coerceAtLeast(0f)
            bottomBlurLayer.renderEffect = renderBlur
            bottomBlurLayer.record(size = IntSize(wInt, bottomHInt)) {
                translate(left = 0f, top = -bottomStartY) {
                    drawLayer(contentLayer)
                }
            }
            val bottomMaskBrush = Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.12f to Color.White.copy(alpha = 0.12f),
                0.32f to Color.White.copy(alpha = 0.42f),
                0.62f to Color.White.copy(alpha = 0.88f),
                1.00f to Color.White,
                startY = bottomStartY,
                endY = totalH
            )
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(0f, bottomStartY, totalW, totalH), offscreenPaint)
                translate(left = 0f, top = bottomStartY) {
                    drawLayer(bottomBlurLayer)
                }
                drawRect(
                    brush = bottomMaskBrush,
                    topLeft = Offset(0f, bottomStartY),
                    size = Size(totalW, bottomH),
                    blendMode = BlendMode.DstIn
                )
                canvas.restore()
            }
        }
    }
}

/**
 * Lightweight atmospheric frosted edge veil layered beneath the floating transparent Top & Bottom Bars.
 * Complements [edgeFadeInBlur] with multi-stop cubic-eased translucent tint + soft crystalline
 * luminance diffusion so scrolling cards melt seamlessly behind the transparent outer bars without
 * restoring opaque black scrims. Non-interactive (zero touch interception).
 */
@Composable
fun BoxScope.TopBottomEdgeFadeBlurOverlay(
    topHeight: Dp,
    bottomHeight: Dp,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val topFrostBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                0.00f to Color(0x88050A14),
                0.32f to Color(0x62070E1D),
                0.64f to Color(0x2E091326),
                0.86f to Color(0x0E0B182E),
                1.00f to Color.Transparent
            )
        } else {
            Brush.verticalGradient(
                0.00f to Color(0x9EF4F8FD),
                0.35f to Color(0x6CEEF4FB),
                0.68f to Color(0x30EAF1FA),
                0.88f to Color(0x10E8F1FA),
                1.00f to Color.Transparent
            )
        }
    }

    val bottomFrostBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.14f to Color(0x0E0B182E),
                0.36f to Color(0x30091326),
                0.68f to Color(0x68070E1D),
                1.00f to Color(0x92050A14)
            )
        } else {
            Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.12f to Color(0x10E8F1FA),
                0.32f to Color(0x32EAF1FA),
                0.65f to Color(0x70EEF4FB),
                1.00f to Color(0xA6F4F8FD)
            )
        }
    }

    if (topHeight > 0.dp) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topHeight)
                .background(topFrostBrush)
                .testTag("edge_fade_blur_top")
        )
    }

    if (bottomHeight > 0.dp) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomHeight)
                .background(bottomFrostBrush)
                .testTag("edge_fade_blur_bottom")
        )
    }
}


