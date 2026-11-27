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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayGlassBg
import com.example.ui.theme.DayGlassSurface
import com.example.ui.theme.DayGlassSurfaceElevated
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
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
 * Ambient backdrop adapting cleanly to Night Mode (#0B0F19 -> #121826) and Day Mode
 * (crystal pearl glass gradient) with subtle floating orbs and zero color bleeding.
 */
@Composable
fun RoadmapAmbientBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = LocalAppDarkTheme.current
) {
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

    val dots = remember(darkTheme) {
        val aqua = if (darkTheme) NeonAqua else DayCyan
        val mint = if (darkTheme) ElectricMint else DayEmerald
        val violet = if (darkTheme) Color(0xFF8B5CF6) else DayPurple
        val orange = if (darkTheme) WarmFireOrange else DayOrange
        val gold = if (darkTheme) MilestoneGold else DayOrange
        listOf(
            GlowDotSpec(0.12f, 0.08f, 3.5f, aqua, 0.2f, true),
            GlowDotSpec(0.86f, 0.11f, 3.0f, mint, 1.1f, true),
            GlowDotSpec(0.06f, 0.24f, 4.0f, violet, 2.3f, false),
            GlowDotSpec(0.42f, 0.22f, 4.5f, orange, 0.8f, true),
            GlowDotSpec(0.75f, 0.30f, 3.0f, aqua, 3.4f, false),
            GlowDotSpec(0.18f, 0.42f, 5.0f, aqua, 1.7f, true),
            GlowDotSpec(0.92f, 0.46f, 4.0f, mint, 2.9f, true),
            GlowDotSpec(0.58f, 0.54f, 3.5f, orange, 4.1f, true),
            GlowDotSpec(0.28f, 0.62f, 4.0f, violet, 0.5f, true),
            GlowDotSpec(0.88f, 0.68f, 3.5f, mint, 3.8f, true),
            GlowDotSpec(0.14f, 0.77f, 4.5f, aqua, 2.1f, true),
            GlowDotSpec(0.52f, 0.80f, 3.5f, gold, 4.8f, true),
            GlowDotSpec(0.66f, 0.89f, 5.5f, orange, 1.4f, true),
            GlowDotSpec(0.95f, 0.86f, 4.0f, aqua, 5.2f, false),
            GlowDotSpec(0.32f, 0.94f, 3.5f, mint, 2.7f, true),
            GlowDotSpec(0.87f, 0.96f, 4.0f, mint, 0.9f, true)
        )
    }

    val bgBrush = if (darkTheme) {
        Brush.verticalGradient(colors = listOf(RoadmapBgDarkTop, RoadmapBgDarkMid, RoadmapBgDarkBottom))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFF6FAFF), DayGlassBg, DayGlassSurfaceElevated))
    }

    val cornerGlow1 = if (darkTheme) NeonAqua.copy(alpha = 0.08f) else DayCyan.copy(alpha = 0.06f)
    val cornerGlow2 = if (darkTheme) ElectricMint.copy(alpha = 0.06f) else DayEmerald.copy(alpha = 0.05f)
    val cornerGlow3 = if (darkTheme) WarmFireOrange.copy(alpha = 0.06f) else DayOrange.copy(alpha = 0.05f)
    val dotAlphaScale = if (darkTheme) 1f else 0.52f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle atmospheric corner radial glows
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(cornerGlow1, Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.12f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.25f, size.height * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(cornerGlow2, Color.Transparent),
                    center = Offset(size.width * 0.80f, size.height * 0.55f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.80f, size.height * 0.55f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(cornerGlow3, Color.Transparent),
                    center = Offset(size.width * 0.20f, size.height * 0.82f),
                    radius = size.width * 0.50f
                ),
                radius = size.width * 0.50f,
                center = Offset(size.width * 0.20f, size.height * 0.82f)
            )

            // Subtle ambient floating bokeh glow dots
            dots.forEach { dot ->
                val dx = sin(driftPhase + dot.phaseOffset) * 8.dp.toPx()
                val dy = cos(driftPhase + dot.phaseOffset * 1.3f) * 10.dp.toPx()
                val pulse = 0.62f + 0.38f * sin(driftPhase * 2f + dot.phaseOffset)
                val center = Offset(size.width * dot.xFrac + dx, size.height * dot.yFrac + dy)
                val r = dot.radiusDp.dp.toPx()

                if (dot.hasHalo) {
                    drawCircle(
                        color = dot.color.copy(alpha = 0.08f * pulse * dotAlphaScale),
                        radius = r * 3.4f,
                        center = center
                    )
                    drawCircle(
                        color = dot.color.copy(alpha = 0.16f * pulse * dotAlphaScale),
                        radius = r * 2.0f,
                        center = center
                    )
                }
                drawCircle(
                    color = dot.color.copy(alpha = 0.68f * pulse * dotAlphaScale),
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
 * Single continuous wavy snake-road segment + circular neon node + frosted glass reward card.
 * Positioned in physical LTR horizontal coordinates so the Bezier/sine Canvas curve aligns with
 * every orb pixel-for-pixel in both RTL ("ar") and LTR ("en"), while restoring layout direction inside the card.
 */
@Composable
fun WavySnakeStationRow(
    station: RoadmapStation,
    prevStationState: StationState?,
    nextStationState: StationState?,
    unlockedStationId: Int,
    currentLang: String,
    pulseScale: Float,
    orbitRotation: Float,
    onClaim: () -> Unit,
    flowPhase: Float = 0f,
    glowPulse: Float = 0.85f,
    rippleProgress: Float = 0f,
    badgeBobDp: Float = 0f,
    nextStationId: Int = 1,
    currentStageXp: Long = 0L,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
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

    val showLevelZeroOrigin = station.id == 1 && unlockedStationId <= 0
    val isCurrentMilestone = unlockedStationId >= 1 && station.id == unlockedStationId
    val isNextTargetWithProgress = station.id == nextStationId && station.state == StationState.LOCKED && currentStageXp > 0L
    val isCurrentActiveOrb = (isCurrentMilestone && station.state != StationState.LOCKED) || station.state == StationState.UNLOCKED
    val upperPathActive = station.state != StationState.LOCKED ||
        (prevStationState != null && prevStationState != StationState.LOCKED) ||
        (showLevelZeroOrigin && currentStageXp > 0L) ||
        (isNextTargetWithProgress && unlockedStationId >= 1)
    val lowerPathActive = nextStationState != null && nextStationState != StationState.LOCKED
    val isFrontierTail = station.state != StationState.LOCKED && nextStationState == StationState.LOCKED

    val biomeState = remember(station.id) { roadmapBiomeStateForStation(station.id.toFloat()) }
    val rowHeight = 166.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showLevelZeroOrigin) {
            LevelZeroOriginRowHeader(
                biomeState = biomeState,
                currentLang = currentLang,
                hasPartialXp = currentStageXp > 0L,
                flowPhase = flowPhase,
                glowPulse = glowPulse,
                badgeBobDp = badgeBobDp,
                isDarkTheme = isDarkTheme
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val totalHeightPx = constraints.maxHeight.toFloat()
            val maxWidthDp = maxWidth
            val nodeCenterDp = maxWidthDp * nodeNormX

            // Layer 0: Continuous 4-Stage Blended Biome Rim Encasement (Glacial -> Rocky -> Forest -> Volcanic)
            RoadmapRowBiomeDecoration(
                biomeState = biomeState,
                glowPulse = glowPulse,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 1: Continuous Seamless Glowing / Dashed Snake Track Canvas
            GlowingWavyRoadmapSegmentCanvas(
                stationId = station.id,
                upperPathActive = upperPathActive,
                lowerPathActive = lowerPathActive,
                isFrontierTail = isFrontierTail,
                flowPhase = flowPhase,
                glowPulse = glowPulse,
                darkTheme = isDarkTheme,
                showTopHalfForStation1 = showLevelZeroOrigin,
                modifier = Modifier.fillMaxSize()
            )

            // Stage Threshold & Peak Banners (Stations 1, 176, 351, 526, 700)
            if (station.id == 1 || station.id == 176 || station.id == 351 || station.id == 526 || station.id == 700) {
                RoadmapStageBannerPill(
                    stationId = station.id,
                    currentLang = currentLang,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                )
            }

            // Layer 2: Physical LTR positioning for Node Orb + Side Frosted Card
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val orbBoxSize = 84.dp
                    val orbCenterX = (totalWidthPx * nodeNormX).roundToInt()
                    val orbCenterY = (totalHeightPx * 0.5f).roundToInt()

                    // 2A. Circular Station Orb + Expanding Sonar Ripples + Animated Orbital Rings + "YOU" Tag
                    Box(
                        modifier = Modifier
                            .offset {
                                val halfPx = orbBoxSize.toPx() * 0.5f
                                IntOffset((orbCenterX - halfPx).roundToInt(), (orbCenterY - halfPx).roundToInt())
                            }
                            .size(orbBoxSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentActiveOrb || isNextTargetWithProgress) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val showCyanOnly = !isCurrentActiveOrb && isNextTargetWithProgress
                                val haloAccent = if (showCyanOnly) NeonAqua else WarmFireOrange
                                val ringAccent = if (showCyanOnly) NeonAqua else MilestoneGold

                            // Warm/Cyan ambient breathing radial halo
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(haloAccent.copy(alpha = 0.42f * glowPulse), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension * 0.50f
                                ),
                                radius = size.minDimension * 0.50f,
                                center = center
                            )

                            // Expanding sonar pulse wave ring around current frontier / claimable orb
                            val rippleRadius = size.minDimension * (0.31f + rippleProgress * 0.19f)
                            val rippleAlpha = (1f - rippleProgress) * 0.72f
                            drawCircle(
                                color = ringAccent.copy(alpha = rippleAlpha),
                                radius = rippleRadius,
                                center = center,
                                style = Stroke(width = 1.8.dp.toPx())
                            )

                            // Counter-rotating faint outer ring
                            rotate(degrees = -orbitRotation * 0.6f, pivot = center) {
                                drawCircle(
                                    color = haloAccent.copy(alpha = 0.36f * glowPulse),
                                    radius = size.minDimension * 0.46f,
                                    center = center,
                                    style = Stroke(
                                        width = 1.1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 9f), 0f)
                                    )
                                )
                            }

                            // Rotating crisp dashed outer gold/cyan ring
                            rotate(degrees = orbitRotation, pivot = center) {
                                drawCircle(
                                    color = ringAccent.copy(alpha = 0.82f),
                                    radius = size.minDimension * 0.40f,
                                    center = center,
                                    style = Stroke(
                                        width = 1.8.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 6f), 0f)
                                    )
                                )
                            }

                            // Inner crisp glowing ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f * glowPulse),
                                radius = size.minDimension * 0.33f,
                                center = center,
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                        }
                    } else if (station.state == StationState.CLAIMED) {
                        // Claimed nodes keep their iconic outer concentric aura ring (Gold rotating halo on MAJOR)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            if (isMajorMilestone) {
                                rotate(degrees = orbitRotation, pivot = center) {
                                    drawCircle(
                                        color = MilestoneGold.copy(alpha = 0.72f * glowPulse),
                                        radius = size.minDimension * 0.38f,
                                        center = center,
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                        )
                                    )
                                }
                            }
                            drawCircle(
                                color = (if (isMajorMilestone) MilestoneGold else ElectricMint).copy(alpha = 0.48f * glowPulse),
                                radius = size.minDimension * 0.32f,
                                center = center,
                                style = Stroke(width = 1.3.dp.toPx())
                            )
                        }
                    } else {
                        // Locked nodes display a clean, understated obsidian metallic aura ring
                        val biomeAccent = biomePrimaryAccent(biomeState, isDarkTheme)
                        val auraStrength = maxOf(
                            biomeState.iceSubmersion,
                            biomeState.rockIntensity * 0.68f,
                            biomeState.forestIntensity * 0.72f,
                            biomeState.lavaSubmersion
                        ).coerceIn(0.22f, 1f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(
                                color = biomeAccent.copy(alpha = (0.16f + 0.24f * auraStrength) * glowPulse),
                                radius = size.minDimension * 0.33f,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }

                    val biomePrimary = biomePrimaryAccent(biomeState, isDarkTheme)
                    val biomeSecondary = biomeSecondaryAccent(biomeState, isDarkTheme)
                    val biomeIcon = when (biomeState.dominantBiome) {
                        RoadmapBiome.GLACIAL -> Icons.Default.AcUnit
                        RoadmapBiome.ROCKY -> Icons.Default.Landscape
                        RoadmapBiome.FOREST -> Icons.Default.Park
                        RoadmapBiome.VOLCANIC -> Icons.Default.LocalFireDepartment
                    }
                    val orbSize = if (isCurrentActiveOrb) 50.dp else 44.dp
                    val orbBrush = when {
                        isCurrentActiveOrb -> Brush.verticalGradient(listOf(biomeSecondary, biomePrimary))
                        station.state == StationState.CLAIMED -> Brush.verticalGradient(
                            if (isDarkTheme) listOf(ElectricMint, Color(0xFF00C875))
                            else listOf(Color(0xFF10B981), DayEmerald)
                        )
                        else -> Brush.verticalGradient(
                            if (isDarkTheme) {
                                listOf(
                                    blendBiomeColor(Color(0xFF10324F), Color(0xFF282220), Color(0xFF0B2C1F), Color(0xFF3E1008), biomeState),
                                    blendBiomeColor(Color(0xFF071B2E), Color(0xFF171316), Color(0xFF061912), Color(0xFF220704), biomeState)
                                )
                            } else {
                                listOf(
                                    Color.White,
                                    blendBiomeColor(Color(0xFFD9F0FE), Color(0xFFF3E6D3), Color(0xFFD1FAE5), Color(0xFFFFD8C7), biomeState)
                                )
                            }
                        )
                    }
                    val orbBorder = when {
                        isCurrentActiveOrb -> Color.White.copy(alpha = 0.92f)
                        station.state == StationState.CLAIMED -> (if (isDarkTheme) ElectricMint else DayEmerald).copy(alpha = 0.9f)
                        else -> biomePrimary.copy(alpha = 0.72f)
                    }
                    val lockedIconTint = if (isDarkTheme) biomeSecondary else biomePrimary
                    val lockedTextTint = if (isDarkTheme) {
                        blendBiomeColor(GlacialNightCrystal, RockyNightSlate, ForestNightMint, Color(0xFFFFEDD5), biomeState)
                    } else {
                        blendBiomeColor(GlacialDayText, RockyDayText, ForestDayText, VolcanicDayText, biomeState)
                    }
                    val unlockedFgTint = if (biomeState.dominantBiome == RoadmapBiome.VOLCANIC ||
                        biomeState.dominantBiome == RoadmapBiome.FOREST
                    ) {
                        if (isDarkTheme) Color(0xFF071318) else Color.White
                    } else {
                        Color(0xFF071318)
                    }

                    Box(
                        modifier = Modifier
                            .size(orbSize)
                            .scale(if (station.state == StationState.UNLOCKED) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(orbBrush)
                            .border(1.6.dp, orbBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            when {
                                station.state == StationState.CLAIMED -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color(0xFF071318) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                station.state == StationState.UNLOCKED -> {
                                    Icon(
                                        imageVector = biomeIcon,
                                        contentDescription = null,
                                        tint = unlockedFgTint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                else -> {
                                    val showBiomeSymbol = biomeState.iceSubmersion > 0.35f || biomeState.lavaSubmersion > 0.35f
                                    Icon(
                                        imageVector = if (showBiomeSymbol) biomeIcon else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = lockedIconTint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${station.id}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = when (station.state) {
                                    StationState.LOCKED -> lockedTextTint
                                    StationState.CLAIMED -> if (isDarkTheme) Color(0xFF071318) else Color.White
                                    else -> unlockedFgTint
                                },
                                lineHeight = 12.sp
                            )
                        }
                    }

                    // "YOU" / "أنت" neon badge gently bobbing directly above true unlocked station node
                    if (isCurrentMilestone) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-8f + badgeBobDp).dp)
                                .wrapContentWidth(unbounded = true)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(listOf(MilestoneGold, WarmFireOrange))
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("roadmap_you_badge_${station.id}")
                        ) {
                            Text(
                                text = if (isAr) "أنت 📍 • م ${station.id}" else "YOU 📍 • LVL ${station.id}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0B0F19),
                                lineHeight = 10.sp,
                                maxLines = 1
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
                            nextStationId = nextStationId,
                            currentStageXp = currentStageXp,
                            isDarkTheme = isDarkTheme,
                            onClaim = onClaim
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun LevelZeroOriginRowHeader(
    biomeState: RoadmapBiomeState,
    currentLang: String,
    hasPartialXp: Boolean,
    flowPhase: Float,
    glowPulse: Float,
    badgeBobDp: Float,
    isDarkTheme: Boolean
) {
    val isAr = currentLang == "ar"
    val primaryAccent = biomePrimaryAccent(biomeState, isDarkTheme)
    val dashPeriod = 22f
    val animatedDashEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(12f, 10f),
        phase = -flowPhase * dashPeriod
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .testTag("roadmap_level_zero_origin"),
        contentAlignment = Alignment.Center
    ) {
        RoadmapRowBiomeDecoration(
            biomeState = biomeState,
            glowPulse = glowPulse,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val startPt = Offset(size.width * 0.5f, size.height * 0.5f)
            val endPt = Offset(size.width * 0.5f, size.height)
            if (hasPartialXp) {
                drawLine(
                    color = primaryAccent.copy(alpha = 0.28f * glowPulse),
                    start = startPt,
                    end = endPt,
                    strokeWidth = 14.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryAccent.copy(alpha = 0.85f),
                    start = startPt,
                    end = endPt,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            } else {
                drawLine(
                    color = primaryAccent.copy(alpha = 0.45f),
                    start = startPt,
                    end = endPt,
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = animatedDashEffect
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(y = (badgeBobDp * 0.4f).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(MilestoneGold, WarmFireOrange)))
                    .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .testTag("roadmap_you_badge_0")
            ) {
                Text(
                    text = if (isAr) "أنت 📍 • م 0" else "YOU 📍 • LVL 0",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0B0F19),
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isDarkTheme) Color(0xFF143652) else Color.White,
                                primaryAccent.copy(alpha = if (isDarkTheme) 0.55f else 0.30f)
                            )
                        )
                    )
                    .border(1.5.dp, MilestoneGold.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                    color = if (isDarkTheme) Color.White else DayTextPrimary
                )
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
    nextStationId: Int = 1,
    currentStageXp: Long = 0L,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onClaim: () -> Unit
) {
    val isAr = currentLang == "ar"
    val biomeState = remember(station.id) { roadmapBiomeStateForStation(station.id.toFloat()) }

    val primaryBiomeTone = biomePrimaryAccent(biomeState, isDarkTheme)
    val secondaryBiomeTone = biomeSecondaryAccent(biomeState, isDarkTheme)
    val cyanTone = primaryBiomeTone
    val mintTone = if (isDarkTheme) ElectricMint else DayEmerald
    val goldTone = if (isDarkTheme) MilestoneGold else DayOrange

    val borderBrush = when {
        isCurrentMilestone || (isMajorMilestone && station.state != StationState.LOCKED) -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (isDarkTheme) 0.56f else 0.90f), secondaryBiomeTone, goldTone)
        )
        station.state == StationState.UNLOCKED -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (isDarkTheme) 0.52f else 0.85f), primaryBiomeTone, mintTone)
        )
        station.state == StationState.CLAIMED -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (isDarkTheme) 0.32f else 0.80f), mintTone.copy(alpha = 0.68f), primaryBiomeTone.copy(alpha = 0.42f))
        )
        else -> Brush.verticalGradient(
            if (isDarkTheme) {
                listOf(
                    Color.White.copy(alpha = 0.24f),
                    secondaryBiomeTone.copy(alpha = 0.28f),
                    primaryBiomeTone.copy(alpha = 0.24f)
                )
            } else {
                listOf(
                    Color(0xFFFFFFFF),
                    secondaryBiomeTone.copy(alpha = 0.38f),
                    primaryBiomeTone.copy(alpha = 0.44f)
                )
            }
        )
    }

    val cardFillBrush = if (isDarkTheme) {
        val darkCardTop = blendBiomeColor(
            Color(0xD60C2036),
            Color(0xD6201A1D),
            Color(0xD6092218),
            Color(0xDA2E0B08),
            biomeState
        )
        val darkCardBot = blendBiomeColor(
            Color(0xDF06111F),
            Color(0xDF131015),
            Color(0xDF05140E),
            Color(0xDF180504),
            biomeState
        )
        when {
            station.state == StationState.UNLOCKED -> Brush.verticalGradient(listOf(Color(0xE2112B46), darkCardBot))
            station.state == StationState.CLAIMED -> Brush.verticalGradient(listOf(Color(0xDA0C2136), darkCardBot))
            else -> Brush.verticalGradient(listOf(darkCardTop, darkCardBot))
        }
    } else {
        val lightCardBot = blendBiomeColor(
            Color(0xF5DDF2FE),
            Color(0xF5F2E6D6),
            Color(0xF5DCFCE7),
            Color(0xF5FFE2D6),
            biomeState
        )
        when {
            station.state == StationState.UNLOCKED -> Brush.verticalGradient(listOf(Color(0xFAFFFFFF), lightCardBot))
            station.state == StationState.CLAIMED -> Brush.verticalGradient(listOf(Color(0xFAFFFFFF), Color(0xF2ECFDF5)))
            else -> Brush.verticalGradient(listOf(Color(0xFAFFFFFF), lightCardBot))
        }
    }

    val borderWidth = if (isCurrentMilestone || station.state == StationState.UNLOCKED) {
        1.25.dp
    } else {
        0.95.dp
    }
    val titleColor = if (isDarkTheme) {
        blendBiomeColor(GlacialNightCrystal, Color(0xFFFDE68A), ForestNightMint, Color(0xFFFFEDD5), biomeState)
    } else {
        blendBiomeColor(GlacialDayText, RockyDayText, ForestDayText, VolcanicDayText, biomeState)
    }
    val descColor = if (isDarkTheme) {
        secondaryBiomeTone.copy(alpha = 0.86f)
    } else {
        blendBiomeColor(Color(0xFF1E3A5F), RockyDaySlate, ForestDayLeaf, VolcanicDayText.copy(alpha = 0.8f), biomeState)
    }
    val mutedReqColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardFillBrush)
            .border(borderWidth, borderBrush, RoundedCornerShape(16.dp))
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
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            RewardQuantityPill(station = station, isDarkTheme = isDarkTheme)
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Reward Description
        Text(
            text = station.reward.getLocalizedDescription(currentLang),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = descColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Live stage XP bar on the active next-to-unlock locked frontier station
        if (station.state == StationState.LOCKED && station.id == nextStationId) {
            val reqXp = station.requiredTotalXp.coerceAtLeast(1L)
            val stageFraction = (currentStageXp.coerceIn(0L, reqXp).toFloat() / reqXp.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDarkTheme) Color(0x401E293B) else Color(0xFFE2E8F0))
                    .testTag("station_progress_bar_${station.id}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (currentStageXp > 0L) stageFraction.coerceAtLeast(0.04f) else 0f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(primaryBiomeTone, secondaryBiomeTone))
                        )
                )
            }
        }

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
                        colors = ButtonDefaults.buttonColors(containerColor = mintTone),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .scale(pulseScale)
                            .testTag("btn_claim_station_${station.id}")
                    ) {
                        val btnFg = if (isDarkTheme) Color(0xFF071318) else Color.White
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = btnFg,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "جني المكافأة" else "Claim",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = btnFg
                        )
                    }
                    Text(
                        text = if (isAr) "الحد: ${station.requiredTotalXp} XP" else "Req: ${station.requiredTotalXp} XP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = cyanTone
                    )
                }

                StationState.CLAIMED -> {
                    Text(
                        text = if (isAr) "تم الاستلام ✓" else "Claimed ✓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = mintTone
                    )
                    if (isMajorMilestone) {
                        MajorBadgePill(isAr = isAr, isDarkTheme = isDarkTheme)
                    } else {
                        Text(
                            text = if (isAr) "الحد: ${station.requiredTotalXp} XP" else "Req: ${station.requiredTotalXp} XP",
                            fontSize = 10.sp,
                            color = mutedReqColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                StationState.LOCKED -> {
                    val stageEarnedForThis = if (station.id == nextStationId) currentStageXp.coerceIn(0L, station.requiredTotalXp) else 0L
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = goldTone.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) {
                                "م ${station.id} • $stageEarnedForThis / ${station.requiredTotalXp} XP"
                            } else {
                                "LVL ${station.id} • $stageEarnedForThis / ${station.requiredTotalXp} XP"
                            },
                            fontSize = 10.sp,
                            color = descColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (isMajorMilestone) {
                        MajorBadgePill(isAr = isAr, isDarkTheme = isDarkTheme)
                    }
                }
            }
        }
    }
}

@Composable
private fun MajorBadgePill(
    isAr: Boolean,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val goldTint = if (isDarkTheme) MilestoneGold else DayOrange
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = goldTint,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = if (isAr) "كبرى" else "MAJOR",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = goldTint
        )
    }
}

@Composable
private fun RewardQuantityPill(
    station: RoadmapStation,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val reward = station.reward
    val (icon, tint) = when (reward.type) {
        RewardType.SHIELD_24H, RewardType.SHIELD_48H, RewardType.SHIELD_72H ->
            Icons.Default.Shield to (if (isDarkTheme) NeonAqua else DayCyan)
        RewardType.KEY_WOODEN ->
            Icons.Default.VpnKey to (if (isDarkTheme) Color(0xFFD97706) else Color(0xFFB45309))
        RewardType.KEY_BRONZE ->
            Icons.Default.VpnKey to (if (isDarkTheme) WarmFireOrange else DayOrange)
        RewardType.KEY_SILVER ->
            Icons.Default.VpnKey to (if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569))
        RewardType.POTION_ETERNAL_REBIRTH ->
            Icons.Default.LocalDrink to (if (isDarkTheme) MilestoneGold else DayOrange)
        RewardType.POTION_PULSE_ELIXIR,
        RewardType.POTION_SHADOW_SERUM,
        RewardType.POTION_GABRIEL_DROPLET,
        RewardType.POTION_CRYO_INFUSION ->
            Icons.Default.LocalDrink to (if (isDarkTheme) Color(0xFFA855F7) else DayPurple)
        RewardType.REWARD_AVATAR ->
            Icons.Default.Person to (if (isDarkTheme) ElectricMint else DayEmerald)
        RewardType.REWARD_TITLE ->
            Icons.Default.EmojiEvents to (if (isDarkTheme) MilestoneGold else DayOrange)
    }

    val isLocked = station.state == StationState.LOCKED
    val borderColor = if (isLocked) {
        if (isDarkTheme) Color(0x3364748B) else Color(0xFFCBD5E1)
    } else {
        tint.copy(alpha = 0.65f)
    }
    val bgColor = if (isLocked) {
        if (isDarkTheme) Color(0x55161E31) else Color(0xFFF1F5F9)
    } else {
        tint.copy(alpha = if (isDarkTheme) 0.14f else 0.12f)
    }
    val labelTint = if (isLocked) {
        if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary
    } else {
        tint
    }

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
