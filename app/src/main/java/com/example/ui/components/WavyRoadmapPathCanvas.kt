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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.roadmap.RoadmapStation
import com.example.data.roadmap.StationState
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Dedicated, reusable Compose Canvas component that draws a glowing, smooth wavy path
 * visualizing the station roadmap using the Neon Cyan (#00F0FF), Electric Mint (#00FF88),
 * and Warm Fire Orange (#FF8800) visual design system gradients.
 *
 * Features fluid downward-traveling neon energy orbs with soft comet halos, breathing bloom,
 * expanding sonar ripple on the active milestone, and a flowing dashed conduit for locked nodes.
 */
@Composable
fun GlowingWavyRoadmapPath(
    stations: List<RoadmapStation>,
    currentStationId: Int,
    modifier: Modifier = Modifier,
    horizontalAmplitudeFraction: Float = 0.30f,
    showNodeMarkers: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavy_path_glow")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavy_flow_phase"
    )
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavy_beacon_pulse"
    )
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavy_ripple_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("wavy_roadmap_path_canvas")
    ) {
        if (stations.isEmpty()) return@Canvas

        val amp = size.width * horizontalAmplitudeFraction.coerceIn(0.10f, 0.40f)
        val centerX = size.width * 0.5f
        val count = stations.size
        val verticalPadding = 24.dp.toPx()
        val usableHeight = (size.height - verticalPadding * 2f).coerceAtLeast(1f)
        val stepY = if (count > 1) usableHeight / (count - 1).toFloat() else 0f

        val nodeOffsets = stations.mapIndexed { index, station ->
            val phase = (PI / 6.0) + (station.id - 1) * (PI / 3.0)
            val wave = (9.0 / 8.0) * sin(phase) + (1.0 / 8.0) * sin(3.0 * phase)
            val x = centerX + amp * wave.toFloat()
            val y = if (count == 1) size.height * 0.5f else verticalPadding + index * stepY
            Offset(x, y)
        }

        val dashPeriod = 22f
        val animatedDashEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(12f, 10f),
            phase = -flowPhase * dashPeriod
        )

        for (i in 0 until nodeOffsets.lastIndex) {
            val start = nodeOffsets[i]
            val end = nodeOffsets[i + 1]
            val currStation = stations[i]
            val nextStation = stations[i + 1]

            val segActive = currStation.state != StationState.LOCKED && nextStation.state != StationState.LOCKED
            val isFrontier = currStation.state != StationState.LOCKED && nextStation.state == StationState.LOCKED

            val controlY1 = start.y + (end.y - start.y) * 0.48f
            val controlY2 = start.y + (end.y - start.y) * 0.52f

            val segmentPath = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(
                    start.x, controlY1,
                    end.x, controlY2,
                    end.x, end.y
                )
            }

            if (segActive) {
                drawPath(
                    path = segmentPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonAqua.copy(alpha = 0.24f * beaconPulse),
                            ElectricMint.copy(alpha = 0.26f * beaconPulse)
                        ),
                        startY = start.y,
                        endY = end.y
                    ),
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                drawPath(
                    path = segmentPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonAqua.copy(alpha = 0.58f * beaconPulse),
                            ElectricMint.copy(alpha = 0.58f * beaconPulse)
                        ),
                        startY = start.y,
                        endY = end.y
                    ),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                drawPath(
                    path = segmentPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFC8FDFF), NeonAqua, ElectricMint),
                        startY = start.y,
                        endY = end.y
                    ),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Traveling energy orb with trailing glow along cubic Bezier
                val leadT = flowPhase
                val trailOffsets = listOf(0f to 1.0f, -0.06f to 0.55f, -0.12f to 0.25f)
                trailOffsets.forEach { (dt, strength) ->
                    val t = ((leadT + dt) % 1f + 1f) % 1f
                    val omt = 1f - t
                    val bx = omt * omt * omt * start.x +
                        3f * omt * omt * t * start.x +
                        3f * omt * t * t * end.x +
                        t * t * t * end.x
                    val by = omt * omt * omt * start.y +
                        3f * omt * omt * t * controlY1 +
                        3f * omt * t * t * controlY2 +
                        t * t * t * end.y
                    drawCircle(
                        color = NeonAqua.copy(alpha = 0.36f * strength),
                        radius = (8f * strength + 2f).dp.toPx(),
                        center = Offset(bx, by)
                    )
                    drawCircle(
                        color = Color(0xFFF0FFFF).copy(alpha = strength),
                        radius = (3.4f * strength + 0.6f).dp.toPx(),
                        center = Offset(bx, by)
                    )
                }
            } else {
                drawPath(
                    path = segmentPath,
                    color = Color(0x55161F33),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = segmentPath,
                    color = Color(0xFF475569),
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = animatedDashEffect
                    )
                )

                if (isFrontier) {
                    val tailT = 0.38f + 0.14f * beaconPulse
                    val omt = 1f - tailT
                    val tipX = omt * omt * omt * start.x +
                        3f * omt * omt * tailT * start.x +
                        3f * omt * tailT * tailT * end.x +
                        tailT * tailT * tailT * end.x
                    val tipY = omt * omt * omt * start.y +
                        3f * omt * omt * tailT * controlY1 +
                        3f * omt * tailT * tailT * controlY2 +
                        tailT * tailT * tailT * end.y
                    val tailPath = Path().apply {
                        moveTo(start.x, start.y)
                        quadraticTo(start.x, (start.y + tipY) * 0.5f, tipX, tipY)
                    }
                    drawPath(
                        path = tailPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WarmFireOrange.copy(alpha = 0.92f),
                                MilestoneGold.copy(alpha = 0.65f * beaconPulse),
                                Color.Transparent
                            ),
                            startY = start.y,
                            endY = tipY
                        ),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = MilestoneGold.copy(alpha = 0.7f * beaconPulse),
                        radius = 4.5.dp.toPx(),
                        center = Offset(tipX, tipY)
                    )
                }
            }
        }

        if (showNodeMarkers) {
            nodeOffsets.forEachIndexed { index, pt ->
                val st = stations[index]
                val isCurrent = st.id == currentStationId || st.state == StationState.UNLOCKED
                when {
                    isCurrent -> {
                        val rippleRadius = (12f + rippleProgress * 16f).dp.toPx()
                        val rippleAlpha = (1f - rippleProgress) * 0.65f
                        drawCircle(
                            color = WarmFireOrange.copy(alpha = rippleAlpha),
                            radius = rippleRadius,
                            center = pt,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(WarmFireOrange.copy(alpha = 0.45f * beaconPulse), Color.Transparent),
                                center = pt,
                                radius = 22.dp.toPx()
                            ),
                            radius = 22.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            brush = Brush.verticalGradient(listOf(MilestoneGold, WarmFireOrange)),
                            radius = 10.dp.toPx(),
                            center = pt
                        )
                    }
                    st.state == StationState.CLAIMED -> {
                        drawCircle(
                            color = ElectricMint.copy(alpha = 0.28f * beaconPulse),
                            radius = 13.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            brush = Brush.verticalGradient(listOf(NeonAqua, ElectricMint)),
                            radius = 7.5.dp.toPx(),
                            center = pt
                        )
                    }
                    else -> {
                        drawCircle(
                            color = Color(0xFF1A2338),
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color(0xFF475569),
                            radius = 6.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single-row C-infinity continuous wavy snake-path Canvas used inside each [WavySnakeStationRow]
 * of the 500-station LazyColumn. Driven by a single shared [flowPhase] and [glowPulse] so
 * glowing energy pulses glide seamlessly across row boundaries down the entire roadmap.
 */
@Composable
fun GlowingWavyRoadmapSegmentCanvas(
    stationId: Int,
    upperPathActive: Boolean,
    lowerPathActive: Boolean,
    isFrontierTail: Boolean,
    modifier: Modifier = Modifier,
    flowPhase: Float = 0f,
    glowPulse: Float = 0.85f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stepsPerHalf = 22
        val totalSteps = stepsPerHalf * 2
        val midY = size.height * 0.5f

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
                            NeonAqua.copy(alpha = 0.24f * glowPulse),
                            ElectricMint.copy(alpha = 0.26f * glowPulse)
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
                            NeonAqua.copy(alpha = 0.56f * glowPulse),
                            ElectricMint.copy(alpha = 0.56f * glowPulse)
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
                        colors = listOf(Color(0xFFC8FDFF), NeonAqua, ElectricMint),
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
                                        Color(0xFFE6FFFF).copy(alpha = 0.65f * strength),
                                        NeonAqua.copy(alpha = 0.34f * strength),
                                        Color.Transparent
                                    ),
                                    center = pt,
                                    radius = (11f * strength + 3f).dp.toPx()
                                ),
                                radius = (11f * strength + 3f).dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color(0xFFF2FFFF).copy(alpha = strength),
                                radius = (3.4f * strength + 0.6f).dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            } else {
                // Dark underlying shadow conduit + animated streaming dashed line
                drawPath(
                    path = path,
                    color = Color(0x66161F33),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = Color(0xFF4A5873),
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
                                WarmFireOrange.copy(alpha = 0.35f * glowPulse),
                                MilestoneGold.copy(alpha = 0.18f * glowPulse),
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
                                MilestoneGold.copy(alpha = 0.95f),
                                WarmFireOrange.copy(alpha = 0.78f),
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
        if (stationId < 500) {
            drawTrackHalf(
                startStep = stepsPerHalf,
                endStep = totalSteps,
                isGlowing = lowerPathActive,
                isTailFade = isFrontierTail
            )
        }
    }
}
