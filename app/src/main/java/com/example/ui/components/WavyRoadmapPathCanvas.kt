package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.roadmap.TaskPools
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.LocalAppDarkTheme
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Single-row C-infinity continuous wavy snake-path Canvas used inside each [WavySnakeStationRow]
 * of the 700-station LazyColumn. Driven by a single shared [flowPhase] and [glowPulse] so
 * glowing energy pulses glide seamlessly across row boundaries down the entire roadmap,
 * transitioning into a frozen crystalline permafrost conduit above Station 500.
 */
@Composable
fun GlowingWavyRoadmapSegmentCanvas(
    stationId: Int,
    upperPathActive: Boolean,
    lowerPathActive: Boolean,
    isFrontierTail: Boolean,
    modifier: Modifier = Modifier,
    flowPhase: Float = 0f,
    glowPulse: Float = 0.85f,
    darkTheme: Boolean = LocalAppDarkTheme.current
) {
    val frostDepth = glacialDepthForStation(stationId)
    val isFrozenZone = frostDepth > 0f

    Canvas(modifier = modifier.fillMaxSize()) {
        val stepsPerHalf = 22
        val totalSteps = stepsPerHalf * 2
        val midY = size.height * 0.5f

        val activeStartTone = when {
            isFrozenZone && darkTheme -> GlacialNightAurora
            isFrozenZone && !darkTheme -> GlacialDayCyan
            darkTheme -> NeonAqua
            else -> DayCyan
        }
        val activeEndTone = when {
            isFrozenZone && darkTheme -> GlacialNightIce
            isFrozenZone && !darkTheme -> GlacialDayDeepBlue
            darkTheme -> ElectricMint
            else -> DayEmerald
        }
        val coreTone = when {
            isFrozenZone && darkTheme -> GlacialNightCrystal
            isFrozenZone && !darkTheme -> Color.White
            darkTheme -> Color(0xFFC8FDFF)
            else -> Color(0xFF0EA5E9)
        }
        val orbHaloTone = if (darkTheme) Color(0xFFE6FFFF) else Color.White
        val orbCenterTone = if (darkTheme) Color(0xFFF2FFFF) else Color.White
        val lockedConduitBg = when {
            isFrozenZone && darkTheme -> Color(0x88103554)
            isFrozenZone && !darkTheme -> Color(0x557DD3FC)
            darkTheme -> Color(0x66161F33)
            else -> Color(0x3394A3B8)
        }
        val lockedDashColor = when {
            isFrozenZone && darkTheme -> GlacialNightIce.copy(alpha = 0.45f + 0.30f * frostDepth)
            isFrozenZone && !darkTheme -> GlacialDayCyan.copy(alpha = 0.55f + 0.28f * frostDepth)
            darkTheme -> Color(0xFF4A5873)
            else -> Color(0xFF94A3B8)
        }
        val frontierOuterTone = if (isFrozenZone) {
            if (darkTheme) GlacialNightCyan else GlacialDayDeepBlue
        } else {
            if (darkTheme) WarmFireOrange else DayOrange
        }
        val frontierInnerTone = if (isFrozenZone) {
            if (darkTheme) GlacialNightCrystal else Color.White
        } else {
            if (darkTheme) MilestoneGold else WarmFireOrange
        }

        fun pointAtFrac(yFrac: Float): Offset {
            val t = (stationId - 0.5f) + yFrac
            val x = size.width * wavyNodeNormalizedX(t)
            val y = size.height * yFrac
            return Offset(x, y)
        }

        // Exact dash period multiple so dashes flow smoothly across seamless row joins
        val dashPeriod = 22f
        val animatedDashEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(12f, 10f),
            phase = -flowPhase * dashPeriod
        )

        fun drawTrackHalf(startStep: Int, endStep: Int, isGlowing: Boolean, isTailFade: Boolean) {
            val path = Path()
            val samplePoints = mutableListOf<Offset>()
            for (s in startStep..endStep) {
                val yFrac = s.toFloat() / totalSteps.toFloat()
                val pt = pointAtFrac(yFrac)
                samplePoints.add(pt)
                if (s == startStep) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }

            if (isGlowing) {
                // Pass 1: Wide outer cyan-to-mint breathing neon bloom
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeStartTone.copy(alpha = (if (darkTheme) 0.24f else 0.16f) * glowPulse),
                            activeEndTone.copy(alpha = (if (darkTheme) 0.26f else 0.18f) * glowPulse)
                        ),
                        startY = 0f,
                        endY = size.height
                    ),
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Pass 2: Mid neon glow halo
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeStartTone.copy(alpha = 0.56f * glowPulse),
                            activeEndTone.copy(alpha = 0.56f * glowPulse)
                        ),
                        startY = 0f,
                        endY = size.height
                    ),
                    style = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Pass 3: Luminous core filament
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(coreTone, activeStartTone, activeEndTone),
                        startY = 0f,
                        endY = size.height
                    ),
                    style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Continuous traveling neon energy orbs flowing downward across row boundaries
                val minFrac = startStep.toFloat() / totalSteps.toFloat()
                val maxFrac = endStep.toFloat() / totalSteps.toFloat()
                val pulseCenters = listOf(flowPhase, (flowPhase + 0.5f) % 1f)
                val trailSteps = listOf(0f to 1.0f, -0.045f to 0.58f, -0.09f to 0.26f)

                pulseCenters.forEach { baseFrac ->
                    trailSteps.forEach { (delta, strength) ->
                        val f = ((baseFrac + delta) % 1f + 1f) % 1f
                        if (f in minFrac..maxFrac) {
                            val pt = pointAtFrac(f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        orbHaloTone.copy(alpha = 0.65f * strength),
                                        activeStartTone.copy(alpha = 0.34f * strength),
                                        Color.Transparent
                                    ),
                                    center = pt,
                                    radius = (11f * strength + 3f).dp.toPx()
                                ),
                                radius = (11f * strength + 3f).dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = orbCenterTone.copy(alpha = strength),
                                radius = (3.4f * strength + 0.6f).dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            } else {
                // Underlying shadow conduit + animated streaming dashed line (+ outer rime sheen in permafrost zone)
                if (isFrozenZone) {
                    drawPath(
                        path = path,
                        color = (if (darkTheme) GlacialNightCyan else Color.White).copy(alpha = (0.14f + 0.16f * frostDepth) * glowPulse),
                        style = Stroke(width = (8f + 4f * frostDepth).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                drawPath(
                    path = path,
                    color = lockedConduitBg,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = lockedDashColor,
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = animatedDashEffect
                    )
                )

                // Warm fire-orange / golden glowing tail fading smoothly below the frontier orb
                if (isTailFade && samplePoints.size >= 10) {
                    val dynamicCount = (7 + (glowPulse * 3f).roundToInt()).coerceIn(6, samplePoints.lastIndex)
                    val tailPath = Path()
                    tailPath.moveTo(samplePoints[0].x, samplePoints[0].y)
                    for (k in 1..dynamicCount) {
                        tailPath.lineTo(samplePoints[k].x, samplePoints[k].y)
                    }
                    val tailTip = samplePoints[dynamicCount]

                    // Outer soft orange/gold bloom on the frontier tail
                    drawPath(
                        path = tailPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                frontierOuterTone.copy(alpha = 0.35f * glowPulse),
                                frontierInnerTone.copy(alpha = 0.18f * glowPulse),
                                Color.Transparent
                            ),
                            startY = midY,
                            endY = tailTip.y
                        ),
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Inner fiery energy core tail
                    drawPath(
                        path = tailPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                frontierInnerTone.copy(alpha = 0.95f),
                                frontierOuterTone.copy(alpha = 0.78f),
                                Color.Transparent
                            ),
                            startY = midY,
                            endY = tailTip.y
                        ),
                        style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        if (stationId > 1) {
            drawTrackHalf(
                startStep = 0,
                endStep = stepsPerHalf,
                isGlowing = upperPathActive,
                isTailFade = false
            )
        }
        if (stationId < TaskPools.TOTAL_STATIONS_COUNT) {
            drawTrackHalf(
                startStep = stepsPerHalf,
                endStep = totalSteps,
                isGlowing = lowerPathActive,
                isTailFade = isFrontierTail
            )
        }
    }
}
