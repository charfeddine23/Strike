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
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Park
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
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.PlusJakartaSans
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class BiomeParticleSpec(
    val xFrac: Float,
    val yOffset: Float,
    val radiusDp: Float,
    val speed: Float,
    val swayPhase: Float,
    val swayAmpDp: Float,
    val variant: Int
)

/**
 * Full-screen 4-stage continuous atmospheric backdrop behind the Roadmap LazyColumn.
 * Smoothly blends sky gradients, mountain/glacier/canopy/caldera silhouettes, and
 * animated environmental particles across Glacial -> Rocky -> Forest -> Volcanic.
 */
@Composable
fun RoadmapBiomeBackdrop(
    biomeState: RoadmapBiomeState,
    driftPhase: Float,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val particles = remember {
        List(24) { idx ->
            val h1 = ((idx * 37 + 13) % 100) / 100f
            val h2 = ((idx * 53 + 29) % 100) / 100f
            val h3 = ((idx * 71 + 7) % 100) / 100f
            BiomeParticleSpec(
                xFrac = h1,
                yOffset = h2,
                radiusDp = 1.5f + (idx % 4) * 0.9f,
                speed = 0.60f + (idx % 5) * 0.28f,
                swayPhase = h3 * (2f * PI.toFloat()),
                swayAmpDp = 5f + (idx % 3) * 4f,
                variant = idx % 6
            )
        }
    }
    val reusablePath = remember { Path() }

    val topCol = if (isDarkTheme) {
        blendBiomeColor(GlacialNightTop, RockyNightTop, ForestNightTop, VolcanicNightTop, biomeState)
    } else {
        blendBiomeColor(GlacialDayTop, RockyDayTop, ForestDayTop, VolcanicDayTop, biomeState)
    }
    val midCol = if (isDarkTheme) {
        blendBiomeColor(GlacialNightMid, RockyNightMid, ForestNightMid, VolcanicNightMid, biomeState)
    } else {
        blendBiomeColor(GlacialDayMid, RockyDayMid, ForestDayMid, VolcanicDayMid, biomeState)
    }
    val botCol = if (isDarkTheme) {
        blendBiomeColor(GlacialNightBottom, RockyNightBottom, ForestNightBottom, VolcanicNightBottom, biomeState)
    } else {
        blendBiomeColor(GlacialDayBottom, RockyDayBottom, ForestDayBottom, VolcanicDayBottom, biomeState)
    }

    val normalizedCycle = driftPhase / (2f * PI.toFloat())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(topCol, midCol, botCol))
            )
            .testTag("roadmap_biome_backdrop")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. STAGE 1 — GLACIAL: Aurora curtains, jagged glacier peaks & falling crystal snow
            if (biomeState.iceWeight > 0.01f) {
                val iw = biomeState.iceWeight
                val icePrimary = if (isDarkTheme) GlacialNightAurora else GlacialDayCyan
                val iceEdge = if (isDarkTheme) GlacialNightCyan else GlacialDayDeepBlue
                val flakeCol = if (isDarkTheme) GlacialNightCrystal else Color.White

                val swayX = sin(driftPhase) * (w * 0.08f)
                val auroraPath = Path().apply {
                    moveTo(0f, h * 0.14f)
                    cubicTo(w * 0.32f + swayX, h * 0.04f, w * 0.68f - swayX, h * 0.24f, w, h * 0.12f)
                }
                drawPath(
                    path = auroraPath,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            icePrimary.copy(alpha = (0.20f + 0.14f * biomeState.iceSubmersion) * iw * (if (isDarkTheme) 1f else 0.7f)),
                            iceEdge.copy(alpha = 0.14f * iw),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 38.dp.toPx(), cap = StrokeCap.Round)
                )

                val glacierH = h * (0.10f + 0.12f * biomeState.iceSubmersion)
                drawSilhouettePeaks(
                    width = w,
                    height = h,
                    peakHeight = glacierH,
                    fillTop = iceEdge.copy(alpha = 0.16f * iw),
                    fillBot = topCol.copy(alpha = 0.36f * iw)
                )

                particles.forEach { p ->
                    val yFrac = ((p.yOffset + normalizedCycle * p.speed) % 1f + 1f) % 1f
                    val px = w * p.xFrac + sin(driftPhase * 1.4f + p.swayPhase) * p.swayAmpDp.dp.toPx()
                    val py = h * yFrac
                    val r = p.radiusDp.dp.toPx() * (0.85f + 0.25f * biomeState.iceSubmersion)
                    val alpha = (0.36f + 0.36f * biomeState.iceSubmersion) * iw
                    if (p.variant == 0) {
                        drawMiniSnowflake(Offset(px, py), r * 1.8f, flakeCol.copy(alpha = alpha * 0.85f), 0.95.dp.toPx(), driftPhase + p.swayPhase)
                    } else {
                        drawCircle(
                            color = icePrimary.copy(alpha = alpha * 0.22f),
                            radius = r * 2.1f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = flakeCol.copy(alpha = alpha * 0.82f),
                            radius = r * 0.85f,
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // 2. STAGE 2 — ROCKY: Canyon cliff silhouettes, mineral strata & drifting dust/stone shards
            if (biomeState.rockWeight > 0.01f) {
                val rw = biomeState.rockWeight
                val amberCol = if (isDarkTheme) RockyNightAmber else RockyDayAmber
                val slateCol = if (isDarkTheme) Color(0xFF475569) else Color(0xFF94A3B8)

                // Stratified mineral horizon glows
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, amberCol.copy(alpha = 0.08f * rw), Color.Transparent),
                        startY = h * 0.30f,
                        endY = h * 0.72f
                    )
                )

                // Terraced canyon silhouettes at lower perimeter
                val canyonH = h * 0.15f
                val mesaPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h - canyonH * 0.58f)
                    lineTo(w * 0.16f, h - canyonH * 0.58f)
                    lineTo(w * 0.22f, h - canyonH * 0.92f)
                    lineTo(w * 0.40f, h - canyonH * 0.92f)
                    lineTo(w * 0.48f, h - canyonH * 0.44f)
                    lineTo(w * 0.66f, h - canyonH * 0.78f)
                    lineTo(w * 0.84f, h - canyonH * 0.78f)
                    lineTo(w, h - canyonH * 0.48f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = mesaPath,
                    brush = Brush.verticalGradient(
                        listOf(amberCol.copy(alpha = 0.14f * rw), slateCol.copy(alpha = 0.24f * rw)),
                        startY = h - canyonH,
                        endY = h
                    )
                )

                particles.forEachIndexed { idx, p ->
                    val yFrac = ((p.yOffset + normalizedCycle * p.speed * 0.55f) % 1f + 1f) % 1f
                    val px = (w * p.xFrac + driftPhase * 12f * (if (idx % 2 == 0) 1f else -1f)) % w
                    val safeX = if (px < 0f) px + w else px
                    val py = h * yFrac
                    val r = p.radiusDp.dp.toPx()
                    if (p.variant == 0 || p.variant == 3) {
                        reusablePath.reset()
                        reusablePath.moveTo(safeX, py - r * 1.35f)
                        reusablePath.lineTo(safeX + r * 1.05f, py)
                        reusablePath.lineTo(safeX, py + r * 1.35f)
                        reusablePath.lineTo(safeX - r * 1.05f, py)
                        reusablePath.close()
                        drawPath(reusablePath, color = amberCol.copy(alpha = 0.40f * rw))
                    } else {
                        drawCircle(
                            color = slateCol.copy(alpha = 0.34f * rw),
                            radius = r * 0.85f,
                            center = Offset(safeX, py)
                        )
                    }
                }
            }

            // 3. STAGE 3 — FOREST: Canopy sunbeams, treetop silhouettes & glowing fireflies + falling leaves
            if (biomeState.forestWeight > 0.01f) {
                val fw = biomeState.forestWeight
                val emeraldCol = if (isDarkTheme) ForestNightEmerald else ForestDayEmerald
                val limeCol = if (isDarkTheme) ForestNightLime else ForestDayLeaf

                // Diagonal forest canopy light rays
                val beamShift = sin(driftPhase * 0.7f) * w * 0.05f
                val beamPath = Path().apply {
                    moveTo(w * 0.15f + beamShift, 0f)
                    lineTo(w * 0.36f + beamShift, 0f)
                    lineTo(w * 0.68f + beamShift, h)
                    lineTo(w * 0.34f + beamShift, h)
                    close()
                }
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        listOf(
                            limeCol.copy(alpha = (if (isDarkTheme) 0.11f else 0.14f) * fw),
                            emeraldCol.copy(alpha = 0.04f * fw),
                            Color.Transparent
                        )
                    )
                )

                // Soft forest canopy hills along bottom
                val canopyH = h * 0.14f
                val canopyPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h - canopyH * 0.55f)
                    cubicTo(w * 0.18f, h - canopyH * 1.05f, w * 0.35f, h - canopyH * 0.35f, w * 0.52f, h - canopyH * 0.85f)
                    cubicTo(w * 0.72f, h - canopyH * 1.15f, w * 0.88f, h - canopyH * 0.45f, w, h - canopyH * 0.72f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = canopyPath,
                    brush = Brush.verticalGradient(
                        listOf(emeraldCol.copy(alpha = 0.18f * fw), Color.Transparent),
                        startY = h - canopyH,
                        endY = h
                    )
                )

                particles.forEach { p ->
                    val yFrac = ((p.yOffset + normalizedCycle * p.speed * 0.62f) % 1f + 1f) % 1f
                    val px = w * p.xFrac + sin(driftPhase * 1.8f + p.swayPhase) * p.swayAmpDp.dp.toPx()
                    val py = h * yFrac
                    val r = p.radiusDp.dp.toPx()
                    val pulse = 0.55f + 0.45f * sin(driftPhase * 2.5f + p.swayPhase)
                    if (p.variant % 2 == 0) {
                        // Bioluminescent firefly orb (2 layered solid circles — 0 shader allocs)
                        drawCircle(
                            color = emeraldCol.copy(alpha = 0.18f * pulse * fw),
                            radius = r * 2.5f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = limeCol.copy(alpha = 0.78f * pulse * fw),
                            radius = r * 0.95f,
                            center = Offset(px, py)
                        )
                    } else {
                        // Drifting leaf silhouette
                        drawOval(
                            color = emeraldCol.copy(alpha = 0.42f * fw),
                            topLeft = Offset(px - r * 1.25f, py - r * 0.62f),
                            size = Size(r * 2.5f, r * 1.24f)
                        )
                    }
                }
            }

            // 4. STAGE 4 — CATACLYSM & VOLCANIC LAVA: Rising volcanic glow, cinder ash & soaring embers
            if (biomeState.volcanicWeight > 0.01f) {
                val vw = biomeState.volcanicWeight
                val lavaDepth = biomeState.lavaSubmersion
                val magmaCol = if (isDarkTheme) VolcanicNightMagma else VolcanicDayMagma
                val goldCol = if (isDarkTheme) VolcanicNightGold else VolcanicDayOrange

                // Rising molten caldera glow from floor
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            magmaCol.copy(alpha = (0.10f + 0.28f * lavaDepth) * vw),
                            goldCol.copy(alpha = (0.16f + 0.34f * lavaDepth) * vw)
                        ),
                        startY = h * (0.58f - 0.24f * lavaDepth),
                        endY = h
                    )
                )

                // Craggy volcanic peaks
                drawSilhouettePeaks(
                    width = w,
                    height = h,
                    peakHeight = h * (0.11f + 0.11f * lavaDepth),
                    fillTop = magmaCol.copy(alpha = 0.20f * vw),
                    fillBot = Color(0xFF1A0402).copy(alpha = 0.45f * vw)
                )

                // Soaring upward magma sparks & cataclysm ash (2 layered solid circles — 0 shader allocs)
                particles.forEach { p ->
                    val riseFrac = (((p.yOffset - normalizedCycle * (p.speed * 1.15f)) % 1f) + 1f) % 1f
                    val px = w * p.xFrac + sin(driftPhase * 2.0f + p.swayPhase) * (p.swayAmpDp * 1.2f).dp.toPx()
                    val py = h * riseFrac
                    val r = p.radiusDp.dp.toPx() * (0.85f + 0.50f * lavaDepth)
                    val emberAlpha = (0.40f + 0.52f * lavaDepth) * vw
                    val coreColor = if (p.variant % 2 == 0) goldCol else magmaCol
                    drawCircle(
                        color = magmaCol.copy(alpha = emberAlpha * 0.24f),
                        radius = r * 2.4f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = coreColor.copy(alpha = emberAlpha * 0.90f),
                        radius = r * 0.9f,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

/**
 * Foreground non-blocking viewport submersion overlay:
 * Realistic luxury atmospheric lighting & rim depth across the 4 stages without spiky edge feathers:
 * - Glacial (1..175): Frosted sapphire ambient veil & delicate polar sheen.
 * - Rocky (176..350): Warm chiseled bronze/obsidian vignette.
 * - Forest (351..525): Tranquil deep emerald canopy ambient mist.
 * - Cataclysm -> Level 700: Controlled molten caldera floor glow rising smoothly at Level 700.
 */
@Composable
fun RoadmapBiomeSubmersionOverlay(
    biomeState: RoadmapBiomeState,
    driftPhase: Float,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val pulse = 0.88f + 0.12f * sin(driftPhase * 1.6f)
    val poolPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("roadmap_biome_submersion_overlay")
    ) {
        val w = size.width
        val h = size.height

        // 1. GLACIAL SUBMERSION (smooth frosted glass atmosphere, zero spiky edge feathers)
        if (biomeState.iceSubmersion > 0.01f) {
            val d = biomeState.iceSubmersion
            val rimeEdge = if (isDarkTheme) GlacialNightIce else Color.White
            val rimeGlow = if (isDarkTheme) GlacialNightCyan else GlacialDayCyan
            drawEdgeVignettes(w, h, rimeEdge, rimeGlow, d * 0.68f, pulse)
        }

        // 2. ROCKY / STONE FRAME
        if (biomeState.rockIntensity > 0.01f) {
            val r = biomeState.rockIntensity
            val stoneEdge = if (isDarkTheme) Color(0xFF3F3835) else Color(0xFFD6C3A5)
            val amberGlow = if (isDarkTheme) RockyNightAmber else RockyDayAmber
            drawEdgeVignettes(w, h, stoneEdge, amberGlow, r * 0.36f, pulse)
        }

        // 3. FOREST / CANOPY ATMOSPHERE
        if (biomeState.forestIntensity > 0.01f) {
            val f = biomeState.forestIntensity
            val mossEdge = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFA7F3D0)
            val vineGlow = if (isDarkTheme) ForestNightEmerald else ForestDayEmerald
            drawEdgeVignettes(w, h, mossEdge, vineGlow, f * 0.40f, pulse)
        }

        // 4. VOLCANIC LAVA SUBMERSION (controlled caldera floor pool & subtle rim glow)
        if (biomeState.lavaSubmersion > 0.01f) {
            val v = biomeState.lavaSubmersion
            val lavaCore = if (isDarkTheme) VolcanicNightGold else VolcanicDayOrange
            val lavaRim = if (isDarkTheme) VolcanicNightMagma else VolcanicDayMagma
            drawEdgeVignettes(w, h, lavaCore, lavaRim, v * 0.78f, pulse)

            val poolHeight = (14f + 44f * v).dp.toPx()
            val waveShift = sin(driftPhase * 1.8f) * 5.dp.toPx()
            poolPath.reset()
            poolPath.moveTo(0f, h)
            poolPath.lineTo(0f, h - poolHeight * 0.64f)
            poolPath.cubicTo(
                w * 0.33f, h - poolHeight * (1.02f + 0.06f * sin(driftPhase)),
                w * 0.67f, h - poolHeight * 0.56f + waveShift,
                w, h - poolHeight * 0.74f
            )
            poolPath.lineTo(w, h)
            poolPath.close()
            drawPath(
                path = poolPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lavaRim.copy(alpha = 0.26f * v), lavaCore.copy(alpha = 0.44f * v)),
                    startY = h - poolHeight,
                    endY = h
                )
            )
        }
    }
}

private fun DrawScope.drawEdgeVignettes(
    w: Float,
    h: Float,
    outerCol: Color,
    glowCol: Color,
    strength: Float,
    pulse: Float
) {
    val topRimH = (16f + 44f * strength).dp.toPx()
    val botRimH = (18f + 54f * strength).dp.toPx()
    val sideRimW = (8f + 22f * strength).dp.toPx()
    val rimAlpha = (0.09f + 0.24f * strength) * pulse

    drawRect(
        brush = Brush.verticalGradient(
            listOf(outerCol.copy(alpha = rimAlpha * 0.62f), glowCol.copy(alpha = rimAlpha * 0.26f), Color.Transparent),
            startY = 0f,
            endY = topRimH
        ),
        topLeft = Offset.Zero,
        size = Size(w, topRimH)
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, glowCol.copy(alpha = rimAlpha * 0.34f), outerCol.copy(alpha = rimAlpha * 0.72f)),
            startY = h - botRimH,
            endY = h
        ),
        topLeft = Offset(0f, h - botRimH),
        size = Size(w, botRimH)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(outerCol.copy(alpha = rimAlpha * 0.45f), glowCol.copy(alpha = rimAlpha * 0.14f), Color.Transparent),
            startX = 0f,
            endX = sideRimW
        ),
        topLeft = Offset.Zero,
        size = Size(sideRimW, h)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, glowCol.copy(alpha = rimAlpha * 0.14f), outerCol.copy(alpha = rimAlpha * 0.45f)),
            startX = w - sideRimW,
            endX = w
        ),
        topLeft = Offset(w - sideRimW, 0f),
        size = Size(sideRimW, h)
    )
}

private fun DrawScope.drawSilhouettePeaks(
    width: Float,
    height: Float,
    peakHeight: Float,
    fillTop: Color,
    fillBot: Color
) {
    val path = Path().apply {
        moveTo(0f, height)
        lineTo(0f, height - peakHeight * 0.65f)
        lineTo(width * 0.14f, height - peakHeight * 1.05f)
        lineTo(width * 0.28f, height - peakHeight * 0.52f)
        lineTo(width * 0.45f, height - peakHeight * 0.88f)
        lineTo(width * 0.62f, height - peakHeight * 0.45f)
        lineTo(width * 0.81f, height - peakHeight * 1.12f)
        lineTo(width, height - peakHeight * 0.60f)
        lineTo(width, height)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(fillTop, fillBot),
            startY = height - peakHeight,
            endY = height
        )
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
        drawLine(color = color, start = Offset(center.x - dx, center.y - dy), end = Offset(center.x + dx, center.y + dy), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

/**
 * Clean Obsidian atmospheric edge framing inside [WavySnakeStationRow] for Levels 1..700
 * with subtle atmospheric mist and metallic glass hairlines (no edge spikes/feathers).
 */
@Composable
fun RoadmapRowBiomeDecoration(
    biomeState: RoadmapBiomeState,
    glowPulse: Float,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val edgeMist = biomePrimaryAccent(biomeState, isDarkTheme)
    val rimLine = biomeSecondaryAccent(biomeState, isDarkTheme)
    val prominence = maxOf(
        biomeState.iceSubmersion,
        biomeState.rockIntensity * 0.68f,
        biomeState.forestIntensity * 0.72f,
        biomeState.lavaSubmersion
    ).coerceIn(0.22f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val mistAlpha = (0.025f + 0.085f * prominence) * glowPulse

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(edgeMist.copy(alpha = mistAlpha), Color.Transparent, Color.Transparent, edgeMist.copy(alpha = mistAlpha))
            )
        )

        val hairlineAlpha = (0.08f + 0.14f * prominence) * glowPulse
        val strokeW = 0.85.dp.toPx()
        drawLine(
            color = rimLine.copy(alpha = hairlineAlpha),
            start = Offset(0.5.dp.toPx(), 0f),
            end = Offset(0.5.dp.toPx(), h),
            strokeWidth = strokeW
        )
        drawLine(
            color = rimLine.copy(alpha = hairlineAlpha),
            start = Offset(w - 0.5.dp.toPx(), 0f),
            end = Offset(w - 0.5.dp.toPx(), h),
            strokeWidth = strokeW
        )
    }
}

/**
 * Biome stage threshold banner embedded inside key entry rows (`1`, `176`, `351`, `526`, `700`)
 * so LazyColumn 1:1 station indexing remains strictly preserved.
 */
@Composable
fun RoadmapStageBannerPill(
    stationId: Int,
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    modifier: Modifier = Modifier
) {
    val (accent, icon, bgColors, textColor, label) = when (stationId) {
        1 -> {
            val acc = if (isDarkTheme) GlacialNightCyan else GlacialDayDeepBlue
            val bg = if (isDarkTheme) listOf(Color(0xE606233C), Color(0xE60B3558)) else listOf(Color(0xF5E0F2FE), Color(0xF5BAE6FD))
            val txt = if (isDarkTheme) GlacialNightCrystal else GlacialDayText
            val lbl = when (currentLang) {
                "ar" -> "المرحلة الجليدية • 1–175 (أعماق الجليد)"
                "fr" -> "Ère Glaciaire • 1–175 (Givre Profond)"
                else -> "Glacial Era • 1–175 (Deep Ice)"
            }
            StagePillSpec(acc, Icons.Default.AcUnit, bg, txt, lbl)
        }
        176 -> {
            val acc = if (isDarkTheme) RockyNightAmber else RockyDayAmber
            val bg = if (isDarkTheme) listOf(Color(0xE6221C24), Color(0xE631251B)) else listOf(Color(0xF5FAF6F0), Color(0xF5EFE2CE))
            val txt = if (isDarkTheme) Color(0xFFFEF3C7) else RockyDayText
            val lbl = when (currentLang) {
                "ar" -> "المرحلة الصخرية • 176–350"
                "fr" -> "Ère Rocheuse • 176–350"
                else -> "Rocky Stage • 176–350"
            }
            StagePillSpec(acc, Icons.Default.Landscape, bg, txt, lbl)
        }
        351 -> {
            val acc = if (isDarkTheme) ForestNightEmerald else ForestDayEmerald
            val bg = if (isDarkTheme) listOf(Color(0xE6072217), Color(0xE60C3523)) else listOf(Color(0xF5ECFDF5), Color(0xF5D1FAE5))
            val txt = if (isDarkTheme) ForestNightMint else ForestDayText
            val lbl = when (currentLang) {
                "ar" -> "المرحلة الغابية • 351–525"
                "fr" -> "Ère Forestière • 351–525"
                else -> "Forest Stage • 351–525"
            }
            StagePillSpec(acc, Icons.Default.Park, bg, txt, lbl)
        }
        700 -> {
            val acc = if (isDarkTheme) VolcanicNightGold else VolcanicDayMagma
            val bg = if (isDarkTheme) listOf(Color(0xF0380B06), Color(0xF0541108)) else listOf(Color(0xF5FFE4D6), Color(0xF5FFC2A8))
            val txt = if (isDarkTheme) Color(0xFFFFEDD5) else VolcanicDayText
            val lbl = when (currentLang) {
                "ar" -> "أعماق الحمم البركانية • محطة 700 (20M XP)"
                "fr" -> "Abysse Volcanique • Station 700 (20M XP)"
                else -> "Volcanic Magma Abyss • LVL 700 (20M XP)"
            }
            StagePillSpec(acc, Icons.Default.LocalFireDepartment, bg, txt, lbl)
        }
        else -> { // 526
            val acc = if (isDarkTheme) VolcanicNightMagma else VolcanicDayMagma
            val bg = if (isDarkTheme) listOf(Color(0xE62A0A08), Color(0xE6421109)) else listOf(Color(0xF5FFF1EB), Color(0xF5FFD8C7))
            val txt = if (isDarkTheme) Color(0xFFFEE2E2) else VolcanicDayText
            val lbl = when (currentLang) {
                "ar" -> "بداية الكارثة والمرحلة البركانية • 526–700"
                "fr" -> "Cataclysme & Ère Volcanique • 526–700"
                else -> "Cataclysm & Volcanic Era • 526–700"
            }
            StagePillSpec(acc, Icons.Default.LocalFireDepartment, bg, txt, lbl)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(bgColors))
            .border(1.1.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .testTag("stage_banner_$stationId")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

private data class StagePillSpec(
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bgColors: List<Color>,
    val textColor: Color,
    val label: String
)
