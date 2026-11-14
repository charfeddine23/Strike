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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.roadmap.RewardType
import com.example.data.roadmap.RoadmapStation
import com.example.data.roadmap.StationState
import com.example.ui.theme.PlusJakartaSans
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Exact Visual Design System Palette for the Wavy Snake Roadmap
val RoadmapBgDarkTop = Color(0xFF0B0F19)
val RoadmapBgDarkMid = Color(0xFF0E1422)
val RoadmapBgDarkBottom = Color(0xFF121826)

val NeonAqua = Color(0xFF00F0FF)
val ElectricMint = Color(0xFF00FF88)
val WarmFireOrange = Color(0xFFFF8800)
val MilestoneGold = Color(0xFFFFC107)

private data class GlowDotSpec(
    val xFrac: Float,
    val yFrac: Float,
    val radiusDp: Float,
    val color: Color,
    val phaseOffset: Float,
    val hasHalo: Boolean
)

/**
 * Ambient deep OLED dark gradient (#0B0F19 -> #121826) with subtle floating glowing orbs
 * matching the futuristic cyber roadmap atmosphere.
 */
@Composable
fun RoadmapAmbientBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow_dots")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_phase"
    )

    val dots = remember {
        listOf(
            GlowDotSpec(0.12f, 0.08f, 3.5f, NeonAqua, 0.2f, true),
            GlowDotSpec(0.86f, 0.11f, 3.0f, ElectricMint, 1.1f, true),
            GlowDotSpec(0.06f, 0.24f, 4.0f, Color(0xFF8B5CF6), 2.3f, false),
            GlowDotSpec(0.42f, 0.22f, 4.5f, WarmFireOrange, 0.8f, true),
            GlowDotSpec(0.75f, 0.30f, 3.0f, NeonAqua, 3.4f, false),
            GlowDotSpec(0.18f, 0.42f, 5.0f, NeonAqua, 1.7f, true),
            GlowDotSpec(0.92f, 0.46f, 4.0f, ElectricMint, 2.9f, true),
            GlowDotSpec(0.58f, 0.54f, 3.5f, WarmFireOrange, 4.1f, true),
            GlowDotSpec(0.28f, 0.62f, 4.0f, Color(0xFF8B5CF6), 0.5f, true),
            GlowDotSpec(0.88f, 0.68f, 3.5f, ElectricMint, 3.8f, true),
            GlowDotSpec(0.14f, 0.77f, 4.5f, NeonAqua, 2.1f, true),
            GlowDotSpec(0.52f, 0.80f, 3.5f, MilestoneGold, 4.8f, true),
            GlowDotSpec(0.66f, 0.89f, 5.5f, WarmFireOrange, 1.4f, true),
            GlowDotSpec(0.95f, 0.86f, 4.0f, NeonAqua, 5.2f, false),
            GlowDotSpec(0.32f, 0.94f, 3.5f, ElectricMint, 2.7f, true),
            GlowDotSpec(0.87f, 0.96f, 4.0f, ElectricMint, 0.9f, true)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(RoadmapBgDarkTop, RoadmapBgDarkMid, RoadmapBgDarkBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle atmospheric corner radial glows
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonAqua.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.12f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.25f, size.height * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricMint.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.80f, size.height * 0.55f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.80f, size.height * 0.55f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WarmFireOrange.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.20f, size.height * 0.82f),
                    radius = size.width * 0.50f
                ),
                radius = size.width * 0.50f,
                center = Offset(size.width * 0.20f, size.height * 0.82f)
            )

            // Subtle ambient floating glow dots
            dots.forEach { dot ->
                val dx = sin(driftPhase + dot.phaseOffset) * 6.dp.toPx()
                val dy = cos(driftPhase + dot.phaseOffset * 1.3f) * 8.dp.toPx()
                val pulse = 0.65f + 0.35f * sin(driftPhase * 2f + dot.phaseOffset)
                val center = Offset(size.width * dot.xFrac + dx, size.height * dot.yFrac + dy)
                val r = dot.radiusDp.dp.toPx()

                if (dot.hasHalo) {
                    drawCircle(
                        color = dot.color.copy(alpha = 0.14f * pulse),
                        radius = r * 3.0f,
                        center = center
                    )
                }
                drawCircle(
                    color = dot.color.copy(alpha = 0.58f * pulse),
                    radius = r,
                    center = center
                )
            }
        }
    }
}

/**
 * Computes C-infinity continuous horizontal normalized X coordinate [0.16..0.84]
 * for continuous station parameter [t].
 *
 * At every integer t = 1, 2, 3, ..., sin(PI/6 + (t-1)*(PI/3)) evaluates exclusively to
 * {-1.0, -0.5, +0.5, +1.0}, guaranteeing zero center collisions with side glass cards.
 */
fun wavyNodeNormalizedX(continuousStationIndex: Float): Float {
    val phase = (PI / 6.0) + (continuousStationIndex - 1.0) * (PI / 3.0)
    val shaped = (9.0 / 8.0) * sin(phase) + (1.0 / 8.0) * sin(3.0 * phase)
    return (0.5f + 0.34f * shaped.toFloat()).coerceIn(0.14f, 0.86f)
}

/**
 * Top Frosted Glass Roadmap Progress HUD Card with quick-jump milestone chips.
 */
@Composable
fun RoadmapProgressHudCard(
    currentStationId: Int,
    totalXp: Long,
    isAr: Boolean,
    onJumpToStation: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeStation = currentStationId.coerceIn(0, 500)
    val stagesLeft = (500 - safeStation).coerceAtLeast(0)
    val progressFraction = (safeStation / 500f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xD6141C2E), Color(0xC80D1322))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonAqua.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.14f),
                        ElectricMint.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glowing cyan circular emblem
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NeonAqua.copy(alpha = 0.14f))
                        .border(1.4.dp, NeonAqua.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = NeonAqua,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) {
                            "تقدم المسار: المحطة $safeStation / 500"
                        } else {
                            "Roadmap Progress: LVL $safeStation / 500"
                        },
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isAr) {
                            "XP المهام: $totalXp / 434,725 • متبقي $stagesLeft محطة"
                        } else {
                            "Tasks XP: $totalXp / 434,725 • $stagesLeft stages left"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sleek horizontal glowing progress track with endpoint glow dots
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceAtLeast(0.02f))
                            .height(4.dp)
                            .background(Brush.horizontalGradient(listOf(NeonAqua, ElectricMint)))
                    )
                }

                // Glowing start & end dots on the progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(NeonAqua)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (progressFraction >= 0.99f) ElectricMint else NeonAqua.copy(alpha = 0.55f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick-Jump Milestones Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jump to Me
                QuickJumpChip(
                    label = if (isAr) "موقعي ($safeStation)" else "Me ($safeStation)",
                    icon = Icons.Default.MyLocation,
                    isHighlighted = true,
                    accentColor = NeonAqua,
                    onClick = { onJumpToStation(safeStation.coerceAtLeast(1)) },
                    modifier = Modifier.testTag("btn_jump_me")
                )
                QuickJumpChip(
                    label = if (isAr) "م 1" else "LVL 1",
                    onClick = { onJumpToStation(1) }
                )
                QuickJumpChip(
                    label = if (isAr) "م 100" else "LVL 100",
                    onClick = { onJumpToStation(100) }
                )
                QuickJumpChip(
                    label = if (isAr) "م 250" else "LVL 250",
                    onClick = { onJumpToStation(250) }
                )
                QuickJumpChip(
                    label = if (isAr) "م 400" else "LVL 400",
                    onClick = { onJumpToStation(400) }
                )
                QuickJumpChip(
                    label = "500",
                    icon = Icons.Default.EmojiEvents,
                    isHighlighted = false,
                    accentColor = MilestoneGold,
                    isCrown = true,
                    onClick = { onJumpToStation(500) }
                )
            }
        }
    }
}

@Composable
private fun QuickJumpChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isHighlighted: Boolean = false,
    accentColor: Color = Color(0xFF94A3B8),
    isCrown: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isHighlighted -> accentColor.copy(alpha = 0.16f)
        isCrown -> MilestoneGold.copy(alpha = 0.14f)
        else -> Color(0x99161E31)
    }
    val borderColor = when {
        isHighlighted -> accentColor.copy(alpha = 0.85f)
        isCrown -> MilestoneGold.copy(alpha = 0.55f)
        else -> Color(0x33FFFFFF)
    }
    val textColor = when {
        isHighlighted -> NeonAqua
        isCrown -> MilestoneGold
        else -> Color(0xFFCBD5E1)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isHighlighted || isCrown) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

/**
 * Single continuous wavy snake-road segment + circular neon node + frosted glass reward card.
 * Positioned in physical LTR horizontal coordinates so the Bezier/sine Canvas curve aligns with
 * every orb pixel-for-pixel in both RTL ("ar") and LTR ("en"), while restoring layout direction inside the card.
 */
@Composable
fun WavySnakeStationRow(
    station: RoadmapStation,
    prevStationState: StationState?,
    nextStationState: StationState?,
    isCurrentMilestone: Boolean,
    currentLang: String,
    pulseScale: Float,
    orbitRotation: Float,
    onClaim: () -> Unit
) {
    val isAr = currentLang == "ar"
    val appDirection = if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr

    val isMajorMilestone = remember(station.id, station.reward.type) {
        station.id % 5 == 0 ||
            station.reward.type == RewardType.REWARD_AVATAR ||
            station.reward.type == RewardType.REWARD_TITLE ||
            station.reward.type == RewardType.POTION_ETERNAL_REBIRTH ||
            station.reward.type == RewardType.KEY_SILVER
    }

    val nodeNormX = remember(station.id) { wavyNodeNormalizedX(station.id.toFloat()) }
    val nodeOnLeft = nodeNormX < 0.5f

    val isCurrentActiveOrb = isCurrentMilestone || station.state == StationState.UNLOCKED
    val upperPathActive = station.state != StationState.LOCKED || (prevStationState != null && prevStationState != StationState.LOCKED)
    val lowerPathActive = nextStationState != null && nextStationState != StationState.LOCKED
    val isFrontierTail = station.state != StationState.LOCKED && nextStationState == StationState.LOCKED

    val rowHeight = 166.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalHeightPx = constraints.maxHeight.toFloat()
        val maxWidthDp = maxWidth
        val nodeCenterDp = maxWidthDp * nodeNormX

        // Layer 1: Continuous Seamless Glowing / Dashed Snake Track Canvas
        GlowingWavyRoadmapSegmentCanvas(
            stationId = station.id,
            upperPathActive = upperPathActive,
            lowerPathActive = lowerPathActive,
            isFrontierTail = isFrontierTail,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Physical LTR positioning for Node Orb + Side Frosted Card
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                val orbBoxSize = 74.dp
                val orbCenterX = (totalWidthPx * nodeNormX).roundToInt()
                val orbCenterY = (totalHeightPx * 0.5f).roundToInt()

                // 2A. Circular Station Orb + Animated Orbital Rings + "YOU" Tag
                Box(
                    modifier = Modifier
                        .offset {
                            val halfPx = orbBoxSize.toPx() * 0.5f
                            IntOffset((orbCenterX - halfPx).roundToInt(), (orbCenterY - halfPx).roundToInt())
                        }
                        .size(orbBoxSize),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrentActiveOrb) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            // Warm ambient radial halo
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(WarmFireOrange.copy(alpha = 0.35f), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension * 0.5f
                                ),
                                radius = size.minDimension * 0.5f,
                                center = center
                            )
                            // Rotating outer dashed gold ring
                            rotate(degrees = orbitRotation, pivot = center) {
                                drawCircle(
                                    color = MilestoneGold.copy(alpha = 0.72f),
                                    radius = size.minDimension * 0.45f,
                                    center = center,
                                    style = Stroke(
                                        width = 1.6.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
                                    )
                                )
                            }
                            // Inner crisp glowing ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.75f),
                                radius = size.minDimension * 0.37f,
                                center = center,
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                        }
                    }

                    val orbSize = if (isCurrentActiveOrb) 50.dp else 44.dp
                    val orbBrush = when {
                        isCurrentActiveOrb -> Brush.verticalGradient(listOf(MilestoneGold, WarmFireOrange))
                        station.state == StationState.CLAIMED -> Brush.verticalGradient(listOf(ElectricMint, Color(0xFF00C875)))
                        else -> Brush.verticalGradient(listOf(Color(0xFF1A2338), Color(0xFF111829)))
                    }
                    val orbBorder = when {
                        isCurrentActiveOrb -> Color.White.copy(alpha = 0.9f)
                        station.state == StationState.CLAIMED -> ElectricMint.copy(alpha = 0.9f)
                        else -> Color(0xFF3B4863)
                    }

                    Box(
                        modifier = Modifier
                            .size(orbSize)
                            .scale(if (station.state == StationState.UNLOCKED) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(orbBrush)
                            .border(1.6.dp, orbBorder, CircleShape)
                            .let { mod ->
                                if (station.state == StationState.UNLOCKED) mod.clickable(onClick = onClaim) else mod
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            when {
                                station.state == StationState.CLAIMED -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF071318),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                station.state == StationState.UNLOCKED -> {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF071318),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${station.id}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = if (station.state == StationState.LOCKED) Color(0xFFCBD5E1) else Color(0xFF071318),
                                lineHeight = 12.sp
                            )
                        }
                    }

                    // "YOU" / "أنت" neon badge floating cleanly above active node
                    if (isCurrentMilestone) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-8).dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(listOf(MilestoneGold, WarmFireOrange))
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isAr) "أنت 📍" else "YOU 📍",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0B0F19),
                                lineHeight = 10.sp
                            )
                        }
                    }
                }

                // 2B. Frosted Glass Station Card on Opposite Side of Curve
                val cardGapFromCenter = 36.dp
                val outerEdgePad = 12.dp
                val cardStartPad = if (nodeOnLeft) nodeCenterDp + cardGapFromCenter else outerEdgePad
                val cardEndPad = if (nodeOnLeft) outerEdgePad else (maxWidthDp - nodeCenterDp) + cardGapFromCenter

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = cardStartPad, end = cardEndPad),
                    contentAlignment = Alignment.Center
                ) {
                    // Restore user's RTL/LTR inside the card
                    CompositionLocalProvider(LocalLayoutDirection provides appDirection) {
                        StationGlassRewardCard(
                            station = station,
                            isMajorMilestone = isMajorMilestone,
                            isCurrentMilestone = isCurrentMilestone,
                            currentLang = currentLang,
                            pulseScale = pulseScale,
                            onClaim = onClaim
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StationGlassRewardCard(
    station: RoadmapStation,
    isMajorMilestone: Boolean,
    isCurrentMilestone: Boolean,
    currentLang: String,
    pulseScale: Float,
    onClaim: () -> Unit
) {
    val isAr = currentLang == "ar"

    val borderBrush = when {
        isCurrentMilestone || (isMajorMilestone && station.state != StationState.LOCKED) -> Brush.linearGradient(
            listOf(MilestoneGold, WarmFireOrange.copy(alpha = 0.85f), MilestoneGold.copy(alpha = 0.7f))
        )
        station.state == StationState.UNLOCKED -> Brush.linearGradient(
            listOf(NeonAqua, ElectricMint)
        )
        station.state == StationState.CLAIMED -> Brush.linearGradient(
            listOf(ElectricMint.copy(alpha = 0.55f), NeonAqua.copy(alpha = 0.35f))
        )
        else -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.14f), NeonAqua.copy(alpha = 0.12f))
        )
    }

    val cardFillBrush = when {
        isCurrentMilestone -> Brush.verticalGradient(listOf(Color(0xDC1B1E28), Color(0xCC111521)))
        station.state == StationState.UNLOCKED -> Brush.verticalGradient(listOf(Color(0xDC102331), Color(0xCC0C1725)))
        station.state == StationState.CLAIMED -> Brush.verticalGradient(listOf(Color(0xD6111B2B), Color(0xC80B1320)))
        else -> Brush.verticalGradient(listOf(Color(0xB8111726), Color(0xA60B101B)))
    }

    val borderWidth = if (isCurrentMilestone || station.state == StationState.UNLOCKED) 1.4.dp else 1.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardFillBrush)
            .border(borderWidth, borderBrush, RoundedCornerShape(16.dp))
            .let { mod ->
                if (station.state == StationState.UNLOCKED) mod.clickable(onClick = onClaim) else mod
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("station_${station.id}")
    ) {
        // Header: Reward Title + Reward Quantity Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = station.reward.getLocalizedTitle(currentLang),
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                color = if (station.state == StationState.LOCKED) Color(0xFFCBD5E1) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            RewardQuantityPill(station = station)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Reward Description
        Text(
            text = station.reward.getLocalizedDescription(currentLang),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = Color(0xFF94A3B8),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Status / Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (station.state) {
                StationState.UNLOCKED -> {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricMint),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .scale(pulseScale)
                            .testTag("btn_claim_station_${station.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF071318),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "جني المكافأة" else "Claim",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF071318)
                        )
                    }
                    Text(
                        text = if (isAr) "الحد: ${station.requiredTotalXp} XP" else "Req: ${station.requiredTotalXp} XP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAqua
                    )
                }

                StationState.CLAIMED -> {
                    Text(
                        text = if (isAr) "تم الاستلام ✓" else "Claimed ✓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint
                    )
                    if (isMajorMilestone) {
                        MajorBadgePill(isAr = isAr)
                    } else {
                        Text(
                            text = if (isAr) "الحد: ${station.requiredTotalXp} XP" else "Req: ${station.requiredTotalXp} XP",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                StationState.LOCKED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MilestoneGold.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) {
                                "م ${station.id} • يتطلب ${station.requiredTotalXp} XP"
                            } else {
                                "LVL ${station.id} • Needs ${station.requiredTotalXp} XP"
                            },
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (isMajorMilestone) {
                        MajorBadgePill(isAr = isAr)
                    }
                }
            }
        }
    }
}

@Composable
private fun MajorBadgePill(isAr: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MilestoneGold,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = if (isAr) "كبرى" else "MAJOR",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = MilestoneGold
        )
    }
}

@Composable
private fun RewardQuantityPill(station: RoadmapStation) {
    val reward = station.reward
    val (icon, tint) = when (reward.type) {
        RewardType.SHIELD_24H, RewardType.SHIELD_48H, RewardType.SHIELD_72H -> Icons.Default.Shield to NeonAqua
        RewardType.KEY_WOODEN -> Icons.Default.VpnKey to Color(0xFFD97706)
        RewardType.KEY_BRONZE -> Icons.Default.VpnKey to WarmFireOrange
        RewardType.KEY_SILVER -> Icons.Default.VpnKey to Color(0xFFE2E8F0)
        RewardType.POTION_ETERNAL_REBIRTH -> Icons.Default.LocalDrink to MilestoneGold
        RewardType.POTION_PULSE_ELIXIR,
        RewardType.POTION_SHADOW_SERUM,
        RewardType.POTION_GABRIEL_DROPLET,
        RewardType.POTION_CRYO_INFUSION -> Icons.Default.LocalDrink to Color(0xFFA855F7)
        RewardType.REWARD_AVATAR -> Icons.Default.Person to ElectricMint
        RewardType.REWARD_TITLE -> Icons.Default.EmojiEvents to MilestoneGold
    }

    val isLocked = station.state == StationState.LOCKED
    val borderColor = if (isLocked) Color(0x3364748B) else tint.copy(alpha = 0.65f)
    val bgColor = if (isLocked) Color(0x55161E31) else tint.copy(alpha = 0.14f)
    val labelTint = if (isLocked) Color(0xFF94A3B8) else tint

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "+${reward.quantity}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = labelTint
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = labelTint,
            modifier = Modifier.size(11.dp)
        )
    }
}
