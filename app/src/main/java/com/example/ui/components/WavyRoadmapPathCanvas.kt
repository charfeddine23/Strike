package com.example.ui.components

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
import kotlin.math.sin

/**
 * Dedicated, reusable Compose Canvas component that draws a glowing, smooth wavy path
 * visualizing the station roadmap using the Neon Cyan (#00F0FF), Electric Mint (#00FF88),
 * and Warm Fire Orange (#FF8800) visual design system gradients.
 *
 * Connects any ordered list of [stations] using C1-continuous cubic Bezier S-curves,
 * multi-pass neon glow blooms along unlocked/claimed segments, glowing energy beads,
 * and a dashed slate conduit for upcoming locked stations.
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
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavy_flow_phase"
    )
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavy_beacon_pulse"
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

        // Compute smooth sinusoidal node coordinates for each station
        val nodeOffsets = stations.mapIndexed { index, station ->
            val phase = (PI / 6.0) + (station.id - 1) * (PI / 3.0)
            val wave = (9.0 / 8.0) * sin(phase) + (1.0 / 8.0) * sin(3.0 * phase)
            val x = centerX + amp * wave.toFloat()
            val y = if (count == 1) size.height * 0.5f else verticalPadding + index * stepY
            Offset(x, y)
        }

        // Draw smooth cubic Bezier segments between consecutive stations
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
                // Pass 1: Wide outer cyan-to-mint bloom
                drawPath(
                    path = segmentPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonAqua.copy(alpha = 0.22f * beaconPulse),
                            ElectricMint.copy(alpha = 0.24f * beaconPulse)
                        ),
                        startY = start.y,
                        endY = end.y
                    ),
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Pass 2: Mid-intensity neon cyan/mint halo
                drawPath(
                    path = segmentPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonAqua.copy(alpha = 0.55f),
                            ElectricMint.copy(alpha = 0.55f)
                        ),
                        startY = start.y,
                        endY = end.y
                    ),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Pass 3: Luminous core filament
                drawPath(
                    path = segmentPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFBDFBFF), NeonAqua, ElectricMint),
                        startY = start.y,
                        endY = end.y
                    ),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Traveling glowing energy bead along the cubic Bezier
                val t = (flowPhase + i * 0.17f) % 1f
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
                    color = NeonAqua.copy(alpha = 0.35f),
                    radius = 7.dp.toPx(),
                    center = Offset(bx, by)
                )
                drawCircle(
                    color = Color(0xFFE6FFFF),
                    radius = 3.dp.toPx(),
                    center = Offset(bx, by)
                )
            } else {
                // Upcoming dark dashed track
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
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 9f), 0f)
                    )
                )

                // Frontier warm fire-orange tail bleeding softly out of the current active milestone
                if (isFrontier) {
                    val midX = 0.125f * start.x + 0.375f * start.x + 0.375f * end.x + 0.125f * end.x
                    val midY = 0.125f * start.y + 0.375f * controlY1 + 0.375f * controlY2 + 0.125f * end.y
                    val tailPath = Path().apply {
                        moveTo(start.x, start.y)
                        quadraticTo(start.x, (start.y + midY) * 0.5f, midX, midY)
                    }
                    drawPath(
                        path = tailPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WarmFireOrange.copy(alpha = 0.9f),
                                MilestoneGold.copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            startY = start.y,
                            endY = midY
                        ),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Optional glowing node markers directly on the Canvas curve
        if (showNodeMarkers) {
            nodeOffsets.forEachIndexed { index, pt ->
                val st = stations[index]
                val isCurrent = st.id == currentStationId || st.state == StationState.UNLOCKED
                when {
                    isCurrent -> {
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
                            color = ElectricMint.copy(alpha = 0.25f),
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
 * of the 500-station LazyColumn so consecutive rows connect seamlessly with zero seams or kinks.
 */
@Composable
fun GlowingWavyRoadmapSegmentCanvas(
    stationId: Int,
    upperPathActive: Boolean,
    lowerPathActive: Boolean,
    isFrontierTail: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stepsPerHalf = 20
        val midY = size.height * 0.5f

        fun drawTrackHalf(startStep: Int, endStep: Int, isGlowing: Boolean, isTailFade: Boolean) {
            val path = Path()
            val samplePoints = mutableListOf<Offset>()
            for (s in startStep..endStep) {
                val yFrac = s.toFloat() / (stepsPerHalf * 2).toFloat()
                val t = (stationId - 0.5f) + yFrac
                val x = size.width * wavyNodeNormalizedX(t)
                val y = size.height * yFrac
                samplePoints.add(Offset(x, y))
                if (s == startStep) path.moveTo(x, y) else path.lineTo(x, y)
            }

            if (isGlowing) {
                // Wide atmospheric cyan-to-mint bloom
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonAqua.copy(alpha = 0.22f), ElectricMint.copy(alpha = 0.24f)),
                        startY = 0f,
                        endY = size.height
                    ),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Mid neon glow halo
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonAqua.copy(alpha = 0.50f), ElectricMint.copy(alpha = 0.50f)),
                        startY = 0f,
                        endY = size.height
                    ),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Crisp glowing core conduit
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFBDFBFF), NeonAqua, ElectricMint),
                        startY = 0f,
                        endY = size.height
                    ),
                    style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Subtle glowing light beads along the curve
                val beadIndices = listOf(stepsPerHalf / 2, endStep - stepsPerHalf / 3).distinct()
                beadIndices.forEach { idx ->
                    val localIdx = idx - startStep
                    if (localIdx in samplePoints.indices) {
                        val pt = samplePoints[localIdx]
                        drawCircle(
                            color = NeonAqua.copy(alpha = 0.35f),
                            radius = 7.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color(0xFFE0FFFF),
                            radius = 3.2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            } else {
                // Dark underlying track shadow + dashed conduit
                drawPath(
                    path = path,
                    color = Color(0x66161F33),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = Color(0xFF475569),
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 9f), 0f)
                    )
                )

                // Warm fire-orange energy tail fading below the active frontier node
                if (isTailFade && samplePoints.size >= 7) {
                    val tailPath = Path()
                    tailPath.moveTo(samplePoints[0].x, samplePoints[0].y)
                    for (k in 1..6) {
                        tailPath.lineTo(samplePoints[k].x, samplePoints[k].y)
                    }
                    val tailEndY = samplePoints[6].y
                    drawPath(
                        path = tailPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WarmFireOrange.copy(alpha = 0.85f),
                                MilestoneGold.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            startY = midY,
                            endY = tailEndY
                        ),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
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
                endStep = stepsPerHalf * 2,
                isGlowing = lowerPathActive,
                isTailFade = isFrontierTail
            )
        }
    }
}
