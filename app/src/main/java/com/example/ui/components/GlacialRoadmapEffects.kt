package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.roadmap.TaskPools
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.PlusJakartaSans
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Night & Day Glacial Palettes (strictly separated for high contrast in both modes)
val GlacialNightTop = Color(0xFF03101D)
val GlacialNightMid = Color(0xFF061E34)
val GlacialNightBottom = Color(0xFF0B2D4C)
val GlacialNightCyan = Color(0xFF38BDF8)
val GlacialNightIce = Color(0xFFBAE6FD)
val GlacialNightCrystal = Color(0xFFE0F2FE)
val GlacialNightAurora = Color(0xFF22D3EE)

val GlacialDayTop = Color(0xFFF0F9FF)
val GlacialDayMid = Color(0xFFDDF1FE)
val GlacialDayBottom = Color(0xFFC3E6FD)
val GlacialDayCyan = Color(0xFF0284C7)
val GlacialDayDeepBlue = Color(0xFF0369A1)
val GlacialDayIceBorder = Color(0xFF7DD3FC)
val GlacialDayText = Color(0xFF0C1E38)

/**
 * Returns progressive glacial submersion intensity in `[0f..1f]` for [stationId].
 * - Stations `1..500`: `0f` (classic cyber roadmap)
 * - Station `501`: `~0.005f` (entering permafrost threshold)
 * - Station `700`: `1.0f` (deeply submerged in eternal ice)
 */
fun glacialDepthForStation(stationId: Int): Float {
    if (stationId <= TaskPools.BASE_STATIONS_COUNT) return 0f
    val span = (TaskPools.TOTAL_STATIONS_COUNT - TaskPools.BASE_STATIONS_COUNT).coerceAtLeast(1).toFloat()
    return ((stationId - TaskPools.BASE_STATIONS_COUNT).toFloat() / span).coerceIn(0f, 1f)
}

private data class SnowParticleSpec(
    val xFrac: Float,
    val yOffset: Float,
    val radiusDp: Float,
    val fallSpeed: Float,
    val swayPhase: Float,
    val swayAmpDp: Float,
    val isCrystalStar: Boolean,
    val thresholdDepth: Float
)

/**
 * Full-screen polar arctic atmosphere layer rendered behind the LazyColumn.
 * As [glacialDepth] grows (`501 -> 700`), polar auroras, crystalline glacier ridges,
 * and swirling snowfall smoothly take over the entire roadmap canvas.
 */
@Composable
fun GlacialViewportBackdrop(
    glacialDepth: Float,
    driftPhase: Float,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val depth = glacialDepth.coerceIn(0f, 1f)
    if (depth <= 0.005f) return

    val particles = remember {
        List(44) { idx ->
            val h1 = ((idx * 37 + 13) % 100) / 100f
            val h2 = ((idx * 53 + 29) % 100) / 100f
            val h3 = ((idx * 71 + 7) % 100) / 100f
            SnowParticleSpec(
                xFrac = h1,
                yOffset = h2,
                radiusDp = 1.6f + (idx % 4) * 1.1f,
                fallSpeed = 0.7f + (idx % 5) * 0.35f,
                swayPhase = h3 * (2f * PI.toFloat()),
                swayAmpDp = 6f + (idx % 3) * 5f,
                isCrystalStar = idx % 6 == 0,
                thresholdDepth = (idx / 44f) * 0.72f
            )
        }
    }

    val topCol = if (isDarkTheme) GlacialNightTop else GlacialDayTop
    val midCol = if (isDarkTheme) GlacialNightMid else GlacialDayMid
    val botCol = if (isDarkTheme) GlacialNightBottom else GlacialDayBottom
    val auroraPrimary = if (isDarkTheme) GlacialNightAurora else GlacialDayCyan
    val auroraSecondary = if (isDarkTheme) Color(0xFF818CF8) else Color(0xFF0EA5E9)
    val flakeColor = if (isDarkTheme) GlacialNightCrystal else Color.White
    val crystalEdgeColor = if (isDarkTheme) GlacialNightCyan else GlacialDayDeepBlue

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        topCol.copy(alpha = (0.35f + 0.65f * depth).coerceIn(0f, 0.96f)),
                        midCol.copy(alpha = (0.38f + 0.62f * depth).coerceIn(0f, 0.96f)),
                        botCol.copy(alpha = (0.42f + 0.58f * depth).coerceIn(0f, 0.98f))
                    )
                )
            )
            .testTag("glacial_viewport_backdrop")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Drifting Polar Aurora Borealis Curtains
            val swayX = sin(driftPhase) * (w * 0.08f)
            val auroraPath = Path().apply {
                moveTo(0f, h * 0.14f)
                cubicTo(
                    w * 0.32f + swayX, h * 0.04f,
                    w * 0.68f - swayX, h * 0.25f,
                    w, h * 0.12f
                )
            }
            drawPath(
                path = auroraPath,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        auroraPrimary.copy(alpha = (0.14f + 0.18f * depth) * (if (isDarkTheme) 1f else 0.7f)),
                        auroraSecondary.copy(alpha = (0.12f + 0.14f * depth) * (if (isDarkTheme) 1f else 0.6f)),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = (38.dp.toPx() * (0.6f + 0.6f * depth)), cap = StrokeCap.Round)
            )

            // 2. Jagged Crystalline Glacier Peaks along lower perimeter (rising deeper with depth)
            val glacierHeight = h * (0.08f + 0.14f * depth)
            val ridgePath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h - glacierHeight * 0.65f)
                lineTo(w * 0.14f, h - glacierHeight * 1.05f)
                lineTo(w * 0.28f, h - glacierHeight * 0.52f)
                lineTo(w * 0.45f, h - glacierHeight * 0.88f)
                lineTo(w * 0.62f, h - glacierHeight * 0.45f)
                lineTo(w * 0.81f, h - glacierHeight * 1.12f)
                lineTo(w, h - glacierHeight * 0.60f)
                lineTo(w, h)
                close()
            }
            drawPath(
                path = ridgePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        crystalEdgeColor.copy(alpha = 0.16f * depth),
                        topCol.copy(alpha = 0.36f * depth)
                    ),
                    startY = h - glacierHeight,
                    endY = h
                )
            )

            // 3. Continuous Falling Snowflakes & Diamond Ice Shards
            val normalizedCycle = driftPhase / (2f * PI.toFloat())
            particles.forEach { p ->
                if (depth >= p.thresholdDepth) {
                    val visFactor = ((depth - p.thresholdDepth) / 0.25f).coerceIn(0f, 1f)
                    val yFrac = ((p.yOffset + normalizedCycle * p.fallSpeed) % 1f + 1f) % 1f
                    val px = w * p.xFrac + sin(driftPhase * 1.5f + p.swayPhase) * p.swayAmpDp.dp.toPx()
                    val py = h * yFrac
                    val r = p.radiusDp.dp.toPx() * (0.75f + 0.35f * depth)

                    if (p.isCrystalStar) {
                        drawMiniSnowflake(
                            center = Offset(px, py),
                            radius = r * 1.8f,
                            color = flakeColor.copy(alpha = 0.65f * visFactor),
                            strokeWidth = 1.dp.toPx(),
                            rotationRad = driftPhase + p.swayPhase
                        )
                    } else {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    flakeColor.copy(alpha = 0.72f * visFactor),
                                    auroraPrimary.copy(alpha = 0.20f * visFactor),
                                    Color.Transparent
                                ),
                                center = Offset(px, py),
                                radius = r * 2.1f
                            ),
                            radius = r * 2.1f,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Foreground non-blocking glacial submersion overlay:
 * Progressively freezes screen edges and branches crystalline frost dendrites inward from
 * all 4 corners as the player goes deeper past Station 500 toward Station 700 (`glacialDepth -> 1f`).
 */
@Composable
fun GlacialSubmersionOverlay(
    glacialDepth: Float,
    driftPhase: Float,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val depth = glacialDepth.coerceIn(0f, 1f)
    if (depth <= 0.01f) return

    val rimeEdgeColor = if (isDarkTheme) GlacialNightIce else Color.White
    val rimeGlowColor = if (isDarkTheme) GlacialNightCyan else GlacialDayCyan
    val veinColor = if (isDarkTheme) GlacialNightCrystal else GlacialDayDeepBlue
    val pulse = 0.84f + 0.16f * sin(driftPhase * 2f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("glacial_submersion_overlay")
    ) {
        val w = size.width
        val h = size.height

        // 1. Encroaching Frosted Ice Vignettes on all 4 Screen Borders
        val topRimH = (18f + 54f * depth).dp.toPx()
        val bottomRimH = (22f + 68f * depth).dp.toPx()
        val sideRimW = (10f + 38f * depth).dp.toPx()
        val rimAlpha = (0.14f + 0.34f * depth) * pulse

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(rimeEdgeColor.copy(alpha = rimAlpha * 0.75f), rimeGlowColor.copy(alpha = rimAlpha * 0.35f), Color.Transparent),
                startY = 0f,
                endY = topRimH
            ),
            topLeft = Offset.Zero,
            size = Size(w, topRimH)
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, rimeGlowColor.copy(alpha = rimAlpha * 0.42f), rimeEdgeColor.copy(alpha = rimAlpha * 0.85f)),
                startY = h - bottomRimH,
                endY = h
            ),
            topLeft = Offset(0f, h - bottomRimH),
            size = Size(w, bottomRimH)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(rimeEdgeColor.copy(alpha = rimAlpha * 0.65f), rimeGlowColor.copy(alpha = rimAlpha * 0.22f), Color.Transparent),
                startX = 0f,
                endX = sideRimW
            ),
            topLeft = Offset.Zero,
            size = Size(sideRimW, h)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, rimeGlowColor.copy(alpha = rimAlpha * 0.22f), rimeEdgeColor.copy(alpha = rimAlpha * 0.65f)),
                startX = w - sideRimW,
                endX = w
            ),
            topLeft = Offset(w - sideRimW, 0f),
            size = Size(sideRimW, h)
        )

        // 2. Branching Dendritic Ice Crystals from all 4 Corners + Mid-Sides
        val cornerReach = (26f + 98f * depth).dp.toPx()
        val veinAlpha = (0.20f + 0.50f * depth) * pulse
        val primaryVeinColor = veinColor.copy(alpha = veinAlpha)
        val rimHighlightColor = rimeEdgeColor.copy(alpha = veinAlpha * 0.75f)

        // Top-Left, Top-Right, Bottom-Left, Bottom-Right dendrites
        drawFrostDendrite(Offset(0f, 0f), baseAngleDeg = 42f, reach = cornerReach, color = primaryVeinColor, highlight = rimHighlightColor, depth = depth)
        drawFrostDendrite(Offset(w, 0f), baseAngleDeg = 138f, reach = cornerReach, color = primaryVeinColor, highlight = rimHighlightColor, depth = depth)
        drawFrostDendrite(Offset(0f, h), baseAngleDeg = -40f, reach = cornerReach * 1.15f, color = primaryVeinColor, highlight = rimHighlightColor, depth = depth)
        drawFrostDendrite(Offset(w, h), baseAngleDeg = -140f, reach = cornerReach * 1.15f, color = primaryVeinColor, highlight = rimHighlightColor, depth = depth)

        // Side crystalline needles appear once submerged deeper past Station 550 (depth > 0.25)
        if (depth > 0.25f) {
            val sideReach = cornerReach * 0.62f * ((depth - 0.25f) / 0.75f).coerceIn(0f, 1f)
            drawFrostDendrite(Offset(0f, h * 0.42f), baseAngleDeg = 12f, reach = sideReach, color = primaryVeinColor, highlight = rimHighlightColor, depth = depth)
            drawFrostDendrite(Offset(w, h * 0.58f), baseAngleDeg = 168f, reach = sideReach, color = primaryVeinColor, highlight = rimHighlightColor, depth = depth)
        }
    }
}

private fun DrawScope.drawFrostDendrite(
    origin: Offset,
    baseAngleDeg: Float,
    reach: Float,
    color: Color,
    highlight: Color,
    depth: Float
) {
    val rad = baseAngleDeg * (PI / 180.0).toFloat()
    val cosA = cos(rad)
    val sinA = sin(rad)
    val tip = Offset(origin.x + cosA * reach, origin.y + sinA * reach)
    val mid = Offset(origin.x + cosA * reach * 0.52f, origin.y + sinA * reach * 0.52f)

    // Main ice fracture spine
    drawLine(
        color = highlight,
        start = origin,
        end = tip,
        strokeWidth = (1.1f + 1.1f * depth).dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = origin,
        end = tip,
        strokeWidth = 0.85.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Two pairs of symmetric frost side-branches
    val branchOffsets = listOf(0.32f to 0.50f, 0.64f to 0.34f)
    branchOffsets.forEach { (distFrac, lenScale) ->
        val anchor = Offset(origin.x + cosA * reach * distFrac, origin.y + sinA * reach * distFrac)
        val branchLen = reach * lenScale
        listOf(-34f, 34f).forEach { deltaDeg ->
            val bRad = (baseAngleDeg + deltaDeg) * (PI / 180.0).toFloat()
            val bEnd = Offset(anchor.x + cos(bRad) * branchLen, anchor.y + sin(bRad) * branchLen)
            drawLine(
                color = color,
                start = anchor,
                end = bEnd,
                strokeWidth = 0.9.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Secondary crystal fork when deeply frozen
            if (depth > 0.45f) {
                val subAnchor = Offset(
                    anchor.x + cos(bRad) * branchLen * 0.55f,
                    anchor.y + sin(bRad) * branchLen * 0.55f
                )
                val subRad = (baseAngleDeg + deltaDeg * 1.45f) * (PI / 180.0).toFloat()
                drawLine(
                    color = highlight,
                    start = subAnchor,
                    end = Offset(
                        subAnchor.x + cos(subRad) * branchLen * 0.38f,
                        subAnchor.y + sin(subRad) * branchLen * 0.38f
                    ),
                    strokeWidth = 0.7.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // Center crystal star glint
    drawCircle(
        color = highlight.copy(alpha = highlight.alpha * 0.55f),
        radius = (2.4f + 2f * depth).dp.toPx(),
        center = mid
    )
}

private fun DrawScope.drawMiniSnowflake(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
    rotationRad: Float
) {
    for (k in 0 until 3) {
        val angle = rotationRad + k * (PI.toFloat() / 3f)
        val dx = cos(angle) * radius
        val dy = sin(angle) * radius
        drawLine(
            color = color,
            start = Offset(center.x - dx, center.y - dy),
            end = Offset(center.x + dx, center.y + dy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Per-row permafrost framing drawn inside [WavySnakeStationRow] for stations `501..700`.
 * Gradually wraps each row in glowing icy mist and crystal rime spikes as [frostDepth] -> 1f.
 */
@Composable
fun GlacialRowFrostDecoration(
    frostDepth: Float,
    glowPulse: Float,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val d = frostDepth.coerceIn(0f, 1f)
    if (d <= 0f) return

    val mistColor = if (isDarkTheme) GlacialNightCyan else GlacialDayIceBorder
    val shardColor = if (isDarkTheme) GlacialNightCrystal else GlacialDayDeepBlue

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val mistAlpha = (0.04f + 0.14f * d) * glowPulse

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    mistColor.copy(alpha = mistAlpha),
                    Color.Transparent,
                    Color.Transparent,
                    mistColor.copy(alpha = mistAlpha)
                )
            )
        )

        // Crystal frost shards on alternating edges proportional to depth
        val spikeW = (10f + 26f * d).dp.toPx()
        val spikeAlpha = (0.14f + 0.36f * d) * glowPulse
        val leftSpike = Path().apply {
            moveTo(0f, h * 0.22f)
            lineTo(spikeW, h * 0.34f)
            lineTo(0f, h * 0.46f)
            close()
        }
        val rightSpike = Path().apply {
            moveTo(w, h * 0.54f)
            lineTo(w - spikeW, h * 0.66f)
            lineTo(w, h * 0.78f)
            close()
        }
        drawPath(leftSpike, color = shardColor.copy(alpha = spikeAlpha))
        drawPath(rightSpike, color = shardColor.copy(alpha = spikeAlpha))
        drawPath(
            leftSpike,
            color = mistColor.copy(alpha = spikeAlpha * 1.1f),
            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Miter)
        )
        drawPath(
            rightSpike,
            color = mistColor.copy(alpha = spikeAlpha * 1.1f),
            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Miter)
        )
    }
}

/**
 * Compact frost threshold banner embedded inside Station 501's row so LazyColumn item
 * indexing (`index = stationId - 1`) stays strictly intact without breaking quick-jump scrolling.
 */
@Composable
fun GlacialThresholdBannerPill(
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    modifier: Modifier = Modifier
) {
    val accent = if (isDarkTheme) GlacialNightCyan else GlacialDayDeepBlue
    val bgBrush = if (isDarkTheme) {
        Brush.horizontalGradient(listOf(Color(0xE606233C), Color(0xE60B3558)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xF5E0F2FE), Color(0xF5BAE6FD)))
    }
    val textColor = if (isDarkTheme) GlacialNightCrystal else GlacialDayText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgBrush)
            .border(1.1.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .testTag("glacial_threshold_banner_501")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = when (currentLang) {
                    "ar" -> "بداية العصر الجليدي • 501–700 (حد 20M XP)"
                    "fr" -> "Ère Glaciaire • 501–700 (Max 20M XP)"
                    else -> "Permafrost Zone • 501–700 (Max 20M XP)"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
