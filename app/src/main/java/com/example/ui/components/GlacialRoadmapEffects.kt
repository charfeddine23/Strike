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
    val depthLayer: Int, // 0 = far/small, 1 = mid, 2 = near/bokeh
    val variant: Int
)

/**
 * Full-screen 4-stage continuous realistic atmospheric backdrop behind the Roadmap LazyColumn.
 * Engineered with zero per-frame Shader/Path allocations inside draw loops to maintain buttery
 * 60-120fps scrolling smoothness across all 700 stations:
 * 1. Glacial (1..175): Silk aurora veil, distant alpine glacier silhouettes & 3-tier bokeh snow
 * 2. Rocky (176..350): Stratified canyon & basalt ridges with warm amber horizon haze & dust motes
 * 3. Forest (351..525): Layered pine canopy ridges, soft volumetric god-rays, mist & firefly bokeh
 * 4. Cataclysm & Volcanic (526..700): Obsidian caldera peaks, rising magma horizon & glowing sparks
 */
@Composable
fun RoadmapBiomeBackdrop(
    biomeState: RoadmapBiomeState,
    driftPhase: Float,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val particles = remember {
        List(40) { idx ->
            val h1 = ((idx * 37 + 13) % 100) / 100f
            val h2 = ((idx * 53 + 29) % 100) / 100f
            val h3 = ((idx * 71 + 7) % 100) / 100f
            val layer = idx % 3
            val baseRadius = when (layer) {
                0 -> 1.35f + (idx % 2) * 0.45f
                1 -> 2.15f + (idx % 3) * 0.6f
                else -> 3.6f + (idx % 3) * 1.15f // soft foreground depth-of-field bokeh
            }
            BiomeParticleSpec(
                xFrac = h1,
                yOffset = h2,
                radiusDp = baseRadius,
                speed = when (layer) {
                    0 -> 0.45f + (idx % 3) * 0.14f
                    1 -> 0.72f + (idx % 4) * 0.18f
                    else -> 1.05f + (idx % 3) * 0.22f
                },
                swayPhase = h3 * (2f * PI.toFloat()),
                swayAmpDp = 4f + layer * 4.5f,
                depthLayer = layer,
                variant = idx % 6
            )
        }
    }

    // Reusable Path instances so frame animation triggers zero GC pressure
    val auroraPath = remember { Path() }
    val farRidgePath = remember { Path() }
    val nearRidgePath = remember { Path() }
    val beamPath = remember { Path() }

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

            // 1. STAGE 1 — GLACIAL: Silk aurora ribbons, layered alpine ice ridges & depth-of-field snow bokeh
            if (biomeState.iceWeight > 0.01f) {
                val iw = biomeState.iceWeight
                val icePrimary = if (isDarkTheme) GlacialNightAurora else GlacialDayCyan
                val iceEdge = if (isDarkTheme) GlacialNightCyan else GlacialDayDeepBlue
                val flakeCol = if (isDarkTheme) GlacialNightCrystal else Color.White

                // Realistic multi-layered silk aurora curtains
                val swayX = sin(driftPhase) * (w * 0.07f)
                val swayY = cos(driftPhase * 0.8f) * (h * 0.018f)
                auroraPath.reset()
                auroraPath.moveTo(-w * 0.05f, h * 0.16f + swayY)
                auroraPath.cubicTo(
                    w * 0.28f + swayX, h * 0.05f,
                    w * 0.66f - swayX, h * 0.25f,
                    w * 1.05f, h * 0.12f - swayY
                )
                drawPath(
                    path = auroraPath,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            icePrimary.copy(alpha = (0.10f + 0.08f * biomeState.iceSubmersion) * iw * (if (isDarkTheme) 1f else 0.55f)),
                            iceEdge.copy(alpha = 0.09f * iw * (if (isDarkTheme) 1f else 0.55f)),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 62.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = auroraPath,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            flakeCol.copy(alpha = (0.11f + 0.07f * biomeState.iceSubmersion) * iw * (if (isDarkTheme) 1f else 0.6f)),
                            icePrimary.copy(alpha = 0.08f * iw),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )

                // Atmospheric horizon mist before mountains
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            iceEdge.copy(alpha = 0.08f * iw),
                            icePrimary.copy(alpha = 0.04f * iw)
                        ),
                        startY = h * 0.58f,
                        endY = h
                    )
                )

                // Distant glacier massif (smooth natural alpine profile)
                val farPeakH = h * (0.14f + 0.10f * biomeState.iceSubmersion)
                buildAlpineRidgePath(
                    path = farRidgePath,
                    width = w,
                    height = h,
                    peakHeight = farPeakH,
                    crests = floatArrayOf(0.55f, 0.94f, 0.62f, 1.04f, 0.72f, 0.88f, 0.50f)
                )
                drawPath(
                    path = farRidgePath,
                    brush = Brush.verticalGradient(
                        listOf(
                            iceEdge.copy(alpha = 0.14f * iw),
                            topCol.copy(alpha = 0.30f * iw)
                        ),
                        startY = h - farPeakH,
                        endY = h
                    )
                )

                // Closer glacial ridge silhouette with subtle moonlight crest rim
                val nearPeakH = h * (0.08f + 0.08f * biomeState.iceSubmersion)
                buildAlpineRidgePath(
                    path = nearRidgePath,
                    width = w,
                    height = h,
                    peakHeight = nearPeakH,
                    crests = floatArrayOf(0.75f, 0.48f, 0.98f, 0.56f, 0.86f, 0.64f, 0.78f)
                )
                drawPath(
                    path = nearRidgePath,
                    brush = Brush.verticalGradient(
                        listOf(
                            flakeCol.copy(alpha = 0.12f * iw),
                            topCol.copy(alpha = 0.42f * iw)
                        ),
                        startY = h - nearPeakH,
                        endY = h
                    )
                )

                // Zero-allocation depth-of-field realistic snowfall & bokeh
                particles.forEach { p ->
                    val yFrac = ((p.yOffset + normalizedCycle * p.speed) % 1f + 1f) % 1f
                    val px = w * p.xFrac + sin(driftPhase * 1.25f + p.swayPhase) * p.swayAmpDp.dp.toPx()
                    val py = h * yFrac
                    val r = p.radiusDp.dp.toPx() * (0.88f + 0.24f * biomeState.iceSubmersion)
                    val baseAlpha = (when (p.depthLayer) {
                        0 -> 0.28f
                        1 -> 0.52f
                        else -> 0.22f
                    } + 0.26f * biomeState.iceSubmersion) * iw
                    drawSoftAtmosphericOrb(
                        center = Offset(px, py),
                        radius = r,
                        coreColor = flakeCol,
                        haloColor = icePrimary,
                        alpha = baseAlpha,
                        drawCrispCore = p.depthLayer != 2
                    )
                }
            }

            // 2. STAGE 2 — ROCKY: Stratified canyon mesas, golden-hour horizon haze & mineral dust motes
            if (biomeState.rockWeight > 0.01f) {
                val rw = biomeState.rockWeight
                val amberCol = if (isDarkTheme) RockyNightAmber else RockyDayAmber
                val ochreCol = if (isDarkTheme) RockyNightOchre else RockyDayText
                val slateCol = if (isDarkTheme) Color(0xFF475569) else Color(0xFF94A3B8)

                // Warm amber canyon sunset haze band
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            amberCol.copy(alpha = 0.07f * rw),
                            ochreCol.copy(alpha = 0.09f * rw),
                            Color.Transparent
                        ),
                        startY = h * 0.28f,
                        endY = h * 0.85f
                    )
                )

                // Distant layered canyon silhouette
                val farMesaH = h * 0.17f
                buildTerracedMesaPath(farRidgePath, w, h, farMesaH, shiftX = 0f)
                drawPath(
                    path = farRidgePath,
                    brush = Brush.verticalGradient(
                        listOf(
                            amberCol.copy(alpha = 0.11f * rw),
                            slateCol.copy(alpha = 0.22f * rw)
                        ),
                        startY = h - farMesaH,
                        endY = h
                    )
                )

                // Foreground basalt ridge
                val nearMesaH = h * 0.105f
                buildTerracedMesaPath(nearRidgePath, w, h, nearMesaH, shiftX = w * 0.12f)
                drawPath(
                    path = nearRidgePath,
                    brush = Brush.verticalGradient(
                        listOf(
                            ochreCol.copy(alpha = 0.14f * rw),
                            topCol.copy(alpha = 0.36f * rw)
                        ),
                        startY = h - nearMesaH,
                        endY = h
                    )
                )

                // Fine suspended atmospheric mineral dust motes (zero allocation)
                particles.forEachIndexed { idx, p ->
                    val yFrac = ((p.yOffset + normalizedCycle * p.speed * 0.45f) % 1f + 1f) % 1f
                    val px = (w * p.xFrac + driftPhase * 9f * (if (idx % 2 == 0) 1f else -1f)) % w
                    val safeX = if (px < 0f) px + w else px
                    val py = h * yFrac
                    val r = p.radiusDp.dp.toPx() * 0.8f
                    val alpha = (if (p.variant % 3 == 0) 0.38f else 0.24f) * rw
                    drawSoftAtmosphericOrb(
                        center = Offset(safeX, py),
                        radius = r,
                        coreColor = if (p.variant % 2 == 0) amberCol else slateCol,
                        haloColor = amberCol,
                        alpha = alpha,
                        drawCrispCore = p.depthLayer != 2
                    )
                }
            }

            // 3. STAGE 3 — FOREST: Layered canopy hills, diagonal volumetric sunbeams, valley mist & fireflies
            if (biomeState.forestWeight > 0.01f) {
                val fw = biomeState.forestWeight
                val emeraldCol = if (isDarkTheme) ForestNightEmerald else ForestDayEmerald
                val limeCol = if (isDarkTheme) ForestNightLime else ForestDayLeaf
                val mintCol = if (isDarkTheme) ForestNightMint else Color.White

                // Dual soft volumetric canopy light shafts (god-rays)
                val beamShift = sin(driftPhase * 0.65f) * w * 0.045f
                beamPath.reset()
                beamPath.moveTo(w * 0.12f + beamShift, 0f)
                beamPath.lineTo(w * 0.30f + beamShift, 0f)
                beamPath.lineTo(w * 0.66f + beamShift, h)
                beamPath.lineTo(w * 0.36f + beamShift, h)
                beamPath.close()
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        listOf(
                            limeCol.copy(alpha = (if (isDarkTheme) 0.08f else 0.11f) * fw),
                            emeraldCol.copy(alpha = 0.03f * fw),
                            Color.Transparent
                        )
                    )
                )

                beamPath.reset()
                beamPath.moveTo(w * 0.56f - beamShift * 0.7f, 0f)
                beamPath.lineTo(w * 0.70f - beamShift * 0.7f, 0f)
                beamPath.lineTo(w * 0.98f - beamShift * 0.7f, h)
                beamPath.lineTo(w * 0.76f - beamShift * 0.7f, h)
                beamPath.close()
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        listOf(
                            emeraldCol.copy(alpha = (if (isDarkTheme) 0.06f else 0.08f) * fw),
                            Color.Transparent
                        )
                    )
                )

                // Soft organic distant forest rolling hills
                val canopyH = h * 0.145f
                buildCanopyRidgePath(farRidgePath, w, h, canopyH)
                drawPath(
                    path = farRidgePath,
                    brush = Brush.verticalGradient(
                        listOf(
                            emeraldCol.copy(alpha = 0.16f * fw),
                            topCol.copy(alpha = 0.25f * fw)
                        ),
                        startY = h - canopyH,
                        endY = h
                    )
                )

                // Soft mist layer above canopy floor
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, mintCol.copy(alpha = 0.07f * fw)),
                        startY = h * 0.74f,
                        endY = h
                    )
                )

                // Bioluminescent firefly bokeh & softly drifting pollen/leaves (zero allocation)
                particles.forEach { p ->
                    val yFrac = ((p.yOffset + normalizedCycle * p.speed * 0.52f) % 1f + 1f) % 1f
                    val px = w * p.xFrac + sin(driftPhase * 1.6f + p.swayPhase) * p.swayAmpDp.dp.toPx()
                    val py = h * yFrac
                    val r = p.radiusDp.dp.toPx()
                    val pulse = 0.55f + 0.45f * sin(driftPhase * 2.4f + p.swayPhase)
                    if (p.variant % 2 == 0) {
                        drawSoftAtmosphericOrb(
                            center = Offset(px, py),
                            radius = r,
                            coreColor = mintCol,
                            haloColor = limeCol,
                            alpha = 0.70f * pulse * fw,
                            drawCrispCore = true
                        )
                    } else {
                        drawOval(
                            color = emeraldCol.copy(alpha = 0.34f * fw),
                            topLeft = Offset(px - r * 1.15f, py - r * 0.55f),
                            size = Size(r * 2.3f, r * 1.1f)
                        )
                    }
                }
            }

            // 4. STAGE 4 — CATACLYSM & VOLCANIC LAVA: Obsidian caldera ridges, rising heat bloom & embers
            if (biomeState.volcanicWeight > 0.01f) {
                val vw = biomeState.volcanicWeight
                val lavaDepth = biomeState.lavaSubmersion
                val magmaCol = if (isDarkTheme) VolcanicNightMagma else VolcanicDayMagma
                val goldCol = if (isDarkTheme) VolcanicNightGold else VolcanicDayOrange

                // Rising incandescent magma horizon glow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            magmaCol.copy(alpha = (0.09f + 0.25f * lavaDepth) * vw),
                            goldCol.copy(alpha = (0.14f + 0.30f * lavaDepth) * vw)
                        ),
                        startY = h * (0.56f - 0.24f * lavaDepth),
                        endY = h
                    )
                )

                // Layered obsidian caldera silhouettes backlit by crater glow
                val calderaH = h * (0.125f + 0.095f * lavaDepth)
                buildAlpineRidgePath(
                    path = farRidgePath,
                    width = w,
                    height = h,
                    peakHeight = calderaH,
                    crests = floatArrayOf(0.58f, 0.96f, 0.78f, 0.52f, 1.05f, 0.70f, 0.60f)
                )
                drawPath(
                    path = farRidgePath,
                    brush = Brush.verticalGradient(
                        listOf(
                            magmaCol.copy(alpha = 0.22f * vw),
                            Color(0xFF1A0402).copy(alpha = (if (isDarkTheme) 0.52f else 0.22f) * vw)
                        ),
                        startY = h - calderaH,
                        endY = h
                    )
                )

                // Rising hot volcanic embers & fine ash bokeh (zero shader allocation)
                particles.forEach { p ->
                    val riseFrac = (((p.yOffset - normalizedCycle * (p.speed * 1.05f)) % 1f) + 1f) % 1f
                    val px = w * p.xFrac + sin(driftPhase * 1.9f + p.swayPhase) * (p.swayAmpDp * 1.15f).dp.toPx()
                    val py = h * riseFrac
                    val r = p.radiusDp.dp.toPx() * (0.85f + 0.45f * lavaDepth)
                    val emberAlpha = (0.42f + 0.50f * lavaDepth) * vw
                    drawSoftAtmosphericOrb(
                        center = Offset(px, py),
                        radius = r,
                        coreColor = if (p.variant % 2 == 0) goldCol else Color(0xFFFFEDD5),
                        haloColor = magmaCol,
                        alpha = emberAlpha,
                        drawCrispCore = p.depthLayer != 2
                    )
                }
            }
        }
    }
}

/**
 * Foreground non-blocking viewport atmospheric immersion overlay:
 * Soft photographic edge framing, smooth frosted ice periphery at Level 1, canyon warmth in Stage 2,
 * emerald mist in Stage 3, and progressive molten magma horizon swell at Level 700.
 * Zero jagged edge feathers or twig spikes for crisp realism and high smoothness.
 */
@Composable
fun RoadmapBiomeSubmersionOverlay(
    biomeState: RoadmapBiomeState,
    driftPhase: Float,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val pulse = 0.88f + 0.12f * sin(driftPhase * 1.8f)
    val magmaPoolPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("roadmap_biome_submersion_overlay")
    ) {
        val w = size.width
        val h = size.height

        // 1. GLACIAL SUBMERSION (maximum 1.0f at Level 1) — Clean frosted atmospheric haze on perimeter
        if (biomeState.iceSubmersion > 0.01f) {
            val d = biomeState.iceSubmersion
            val rimeEdge = if (isDarkTheme) GlacialNightIce else Color.White
            val rimeGlow = if (isDarkTheme) GlacialNightCyan else GlacialDayCyan
            drawEdgeVignettes(w, h, rimeEdge, rimeGlow, d * 0.88f, pulse)
        }

        // 2. ROCKY / STONE FRAME
        if (biomeState.rockIntensity > 0.01f) {
            val r = biomeState.rockIntensity
            val stoneEdge = if (isDarkTheme) Color(0xFF3F3A36) else Color(0xFFD6C3A5)
            val amberGlow = if (isDarkTheme) RockyNightAmber else RockyDayAmber
            drawEdgeVignettes(w, h, stoneEdge, amberGlow, r * 0.36f, pulse)
        }

        // 3. FOREST / EMERALD MIST FRAME
        if (biomeState.forestIntensity > 0.01f) {
            val f = biomeState.forestIntensity
            val mossEdge = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFA7F3D0)
            val vineGlow = if (isDarkTheme) ForestNightEmerald else ForestDayEmerald
            drawEdgeVignettes(w, h, mossEdge, vineGlow, f * 0.42f, pulse)
        }

        // 4. VOLCANIC LAVA SUBMERSION (ramps up from Cataclysm 526 -> 1.0f deep magma at 700)
        if (biomeState.lavaSubmersion > 0.01f) {
            val v = biomeState.lavaSubmersion
            val lavaCore = if (isDarkTheme) VolcanicNightGold else VolcanicDayOrange
            val lavaRim = if (isDarkTheme) VolcanicNightMagma else VolcanicDayMagma
            drawEdgeVignettes(w, h, lavaCore, lavaRim, v * 0.92f, pulse)

            // Smooth undulating molten magma swell along the bottom rim
            val poolHeight = (18f + 46f * v).dp.toPx()
            val waveShift = sin(driftPhase * 1.8f) * 5.dp.toPx()
            magmaPoolPath.reset()
            magmaPoolPath.moveTo(0f, h)
            magmaPoolPath.lineTo(0f, h - poolHeight * 0.65f)
            magmaPoolPath.cubicTo(
                w * 0.33f, h - poolHeight * (1.02f + 0.07f * sin(driftPhase)),
                w * 0.67f, h - poolHeight * 0.58f + waveShift,
                w, h - poolHeight * 0.74f
            )
            magmaPoolPath.lineTo(w, h)
            magmaPoolPath.close()
            drawPath(
                path = magmaPoolPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lavaRim.copy(alpha = 0.24f * v * pulse),
                        lavaCore.copy(alpha = 0.44f * v * pulse)
                    ),
                    startY = h - poolHeight,
                    endY = h
                )
            )
        }
    }
}

/**
 * Draws a soft realistic bokeh / particle orb using 2 concentric solid-alpha circles
 * instead of per-particle Shader/radialGradient allocations.
 */
private fun DrawScope.drawSoftAtmosphericOrb(
    center: Offset,
    radius: Float,
    coreColor: Color,
    haloColor: Color,
    alpha: Float,
    drawCrispCore: Boolean
) {
    val safeAlpha = alpha.coerceIn(0f, 1f)
    if (safeAlpha <= 0.01f) return
    // Outer soft atmospheric bokeh glow
    drawCircle(
        color = haloColor.copy(alpha = safeAlpha * 0.16f),
        radius = radius * 2.5f,
        center = center
    )
    // Mid diffusion halo
    drawCircle(
        color = coreColor.copy(alpha = safeAlpha * 0.34f),
        radius = radius * 1.45f,
        center = center
    )
    // Crisp specular center on near/mid particles
    if (drawCrispCore) {
        drawCircle(
            color = coreColor.copy(alpha = (safeAlpha * 0.86f).coerceAtMost(0.95f)),
            radius = radius * 0.72f,
            center = center
        )
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
    val topRimH = (14f + 40f * strength).dp.toPx()
    val botRimH = (18f + 54f * strength).dp.toPx()
    val sideRimW = (8f + 24f * strength).dp.toPx()
    val rimAlpha = (0.09f + 0.24f * strength) * pulse

    drawRect(
        brush = Brush.verticalGradient(
            listOf(outerCol.copy(alpha = rimAlpha * 0.58f), glowCol.copy(alpha = rimAlpha * 0.24f), Color.Transparent),
            startY = 0f,
            endY = topRimH
        ),
        topLeft = Offset.Zero,
        size = Size(w, topRimH)
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, glowCol.copy(alpha = rimAlpha * 0.32f), outerCol.copy(alpha = rimAlpha * 0.72f)),
            startY = h - botRimH,
            endY = h
        ),
        topLeft = Offset(0f, h - botRimH),
        size = Size(w, botRimH)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(outerCol.copy(alpha = rimAlpha * 0.42f), glowCol.copy(alpha = rimAlpha * 0.14f), Color.Transparent),
            startX = 0f,
            endX = sideRimW
        ),
        topLeft = Offset.Zero,
        size = Size(sideRimW, h)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, glowCol.copy(alpha = rimAlpha * 0.14f), outerCol.copy(alpha = rimAlpha * 0.42f)),
            startX = w - sideRimW,
            endX = w
        ),
        topLeft = Offset(w - sideRimW, 0f),
        size = Size(sideRimW, h)
    )
}

private fun buildAlpineRidgePath(
    path: Path,
    width: Float,
    height: Float,
    peakHeight: Float,
    crests: FloatArray
) {
    path.reset()
    path.moveTo(0f, height)
    val count = crests.size.coerceAtLeast(2)
    val segW = width / (count - 1).toFloat()
    path.lineTo(0f, height - peakHeight * crests[0])
    for (i in 1 until count) {
        val prevX = (i - 1) * segW
        val prevY = height - peakHeight * crests[i - 1]
        val curX = i * segW
        val curY = height - peakHeight * crests[i]
        val midX = (prevX + curX) * 0.5f
        path.quadraticTo(prevX + segW * 0.35f, prevY, midX, (prevY + curY) * 0.5f)
        path.quadraticTo(curX - segW * 0.35f, curY, curX, curY)
    }
    path.lineTo(width, height)
    path.close()
}

private fun buildTerracedMesaPath(
    path: Path,
    width: Float,
    height: Float,
    canyonH: Float,
    shiftX: Float
) {
    path.reset()
    path.moveTo(0f, height)
    path.lineTo(0f, height - canyonH * 0.52f)
    path.lineTo((width * 0.15f + shiftX).coerceAtMost(width), height - canyonH * 0.52f)
    path.lineTo((width * 0.21f + shiftX).coerceAtMost(width), height - canyonH * 0.88f)
    path.lineTo((width * 0.39f + shiftX).coerceAtMost(width), height - canyonH * 0.88f)
    path.lineTo((width * 0.47f + shiftX).coerceAtMost(width), height - canyonH * 0.44f)
    path.lineTo((width * 0.65f + shiftX).coerceAtMost(width), height - canyonH * 0.76f)
    path.lineTo((width * 0.83f + shiftX).coerceAtMost(width), height - canyonH * 0.76f)
    path.lineTo(width, height - canyonH * 0.48f)
    path.lineTo(width, height)
    path.close()
}

private fun buildCanopyRidgePath(
    path: Path,
    width: Float,
    height: Float,
    canopyH: Float
) {
    path.reset()
    path.moveTo(0f, height)
    path.lineTo(0f, height - canopyH * 0.54f)
    path.cubicTo(
        width * 0.18f, height - canopyH * 1.02f,
        width * 0.35f, height - canopyH * 0.36f,
        width * 0.52f, height - canopyH * 0.84f
    )
    path.cubicTo(
        width * 0.72f, height - canopyH * 1.10f,
        width * 0.88f, height - canopyH * 0.46f,
        width, height - canopyH * 0.70f
    )
    path.lineTo(width, height)
    path.close()
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
            val bg = if (isDarkTheme) listOf(Color(0xD6072540), Color(0xD60C395E)) else listOf(Color(0xF5E0F2FE), Color(0xF5BAE6FD))
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
            val bg = if (isDarkTheme) listOf(Color(0xD6221C24), Color(0xD631251B)) else listOf(Color(0xF5FAF6F0), Color(0xF5EFE2CE))
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
            val bg = if (isDarkTheme) listOf(Color(0xD6072217), Color(0xD60C3523)) else listOf(Color(0xF5ECFDF5), Color(0xF5D1FAE5))
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
            val bg = if (isDarkTheme) listOf(Color(0xE6380B06), Color(0xE6541108)) else listOf(Color(0xF5FFE4D6), Color(0xF5FFC2A8))
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
            val bg = if (isDarkTheme) listOf(Color(0xD62A0A08), Color(0xD6421109)) else listOf(Color(0xF5FFF1EB), Color(0xF5FFD8C7))
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
            .border(
                1.1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x88FFFFFF), accent.copy(alpha = 0.78f))
                ),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 11.dp, vertical = 3.5.dp)
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
