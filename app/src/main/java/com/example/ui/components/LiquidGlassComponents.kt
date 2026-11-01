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
import com.example.ui.theme.CosmicDarkBg
import com.example.ui.theme.CosmicLightBg
import com.example.ui.theme.GlassBorderBrushDark
import com.example.ui.theme.GlassBorderBrushLight
import com.example.ui.theme.GlassDarkPanel
import com.example.ui.theme.GlassLightPanel
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonSky
import com.example.ui.theme.NeonViolet

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_ambient")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ambient"
    )

    val baseBg = if (darkTheme) CosmicDarkBg else CosmicLightBg
    val cyanGlow = if (darkTheme) NeonCyan.copy(alpha = pulseAlpha) else Color(0xFF38BDF8).copy(alpha = 0.15f)
    val violetGlow = if (darkTheme) NeonViolet.copy(alpha = pulseAlpha * 0.8f) else Color(0xFFA855F7).copy(alpha = 0.12f)
    val emeraldGlow = if (darkTheme) NeonEmerald.copy(alpha = pulseAlpha * 0.6f) else Color(0xFF10B981).copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                val canvasW = size.width
                val canvasH = size.height

                // Top-right Cyan Cosmic Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cyanGlow, Color.Transparent),
                        center = Offset(canvasW * 0.85f, canvasH * 0.15f),
                        radius = canvasW * 0.75f
                    ),
                    radius = canvasW * 0.75f,
                    center = Offset(canvasW * 0.85f, canvasH * 0.15f)
                )

                // Bottom-left Violet Cosmic Nebula
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(violetGlow, Color.Transparent),
                        center = Offset(canvasW * 0.15f, canvasH * 0.8f),
                        radius = canvasW * 0.85f
                    ),
                    radius = canvasW * 0.85f,
                    center = Offset(canvasW * 0.15f, canvasH * 0.8f)
                )

                // Middle-edge subtle emerald highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(emeraldGlow, Color.Transparent),
                        center = Offset(canvasW * 0.5f, canvasH * 0.45f),
                        radius = canvasW * 0.5f
                    ),
                    radius = canvasW * 0.5f,
                    center = Offset(canvasW * 0.5f, canvasH * 0.45f)
                )
            }
    ) {
        content()
    }
}

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
    val panelColor = if (darkTheme) GlassDarkPanel else GlassLightPanel
    val borderBrush = if (darkTheme) GlassBorderBrushDark else GlassBorderBrushLight

    val baseModifier = modifier
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .clip(shape)
        .background(panelColor)
        .border(borderWidth, borderBrush, shape)

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

@Composable
fun GlassTypeBadge(
    type: String,
    modifier: Modifier = Modifier
) {
    val (badgeColor, textColor) = when (type) {
        "M" -> Pair(NeonCyan, Color(0xFF032230))
        "G" -> Pair(NeonViolet, Color(0xFF280749))
        "TS" -> Pair(NeonEmerald, Color(0xFF022B1D))
        "FG" -> Pair(NeonAmber, Color(0xFF331B00))
        else -> Pair(NeonSky, Color(0xFF0A2033))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.22f))
            .border(1.dp, badgeColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
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
    accentColor: Color = NeonCyan
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CosmicStrikeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    strikeCount: Int,
    subtitle: String = "TAP TO STRIKE",
    testTag: String = "strike_button"
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
                        colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent)
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
                    Brush.sweepGradient(
                        listOf(NeonCyan, NeonViolet, NeonEmerald, NeonCyan)
                    ),
                    CircleShape
                )
        )

        // Core tactile Liquid Glass button
        Box(
            modifier = Modifier
                .size(165.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0F1E36),
                            Color(0xFF070E1A)
                        )
                    )
                )
                .border(2.dp, NeonCyan.copy(alpha = 0.85f), CircleShape)
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
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "$strikeCount",
                    color = NeonAmber,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
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
    testTag: String? = null
) {
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
                    .background(accentColor.copy(alpha = 0.20f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
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
    modifier: Modifier = Modifier
) {
    val total = (valueA + valueB).coerceAtLeast(0.001)
    val fractionA = (valueA / total).toFloat().coerceIn(0.05f, 0.95f)
    val isAWinner = valueA > valueB
    val isBWinner = valueB > valueA
    val isTie = valueA == valueB

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
                    fontWeight = FontWeight.SemiBold
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
                color = if (isTie) MaterialTheme.colorScheme.onSurfaceVariant else if (isAWinner) NeonCyan else NeonViolet,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Value numbers row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${((valueA * 100.0).toLong() / 100.0)}",
                color = if (isAWinner) NeonCyan else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isAWinner) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = "${((valueB * 100.0).toLong() / 100.0)}",
                color = if (isBWinner) NeonViolet else MaterialTheme.colorScheme.onSurface,
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
                .background(Color(0x331E293B))
        ) {
            Box(
                modifier = Modifier
                    .weight(fractionA)
                    .fillMaxSize()
                    .background(
                        if (isAWinner) NeonCyan else Color(0xFF0284C7).copy(alpha = 0.5f)
                    )
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .weight(1f - fractionA)
                    .fillMaxSize()
                    .background(
                        if (isBWinner) NeonViolet else Color(0xFF7C3AED).copy(alpha = 0.5f)
                    )
            )
        }
    }
}

@Composable
fun WinnerBadge(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NeonEmerald.copy(alpha = 0.2f))
            .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = NeonEmerald,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = NeonEmerald,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
