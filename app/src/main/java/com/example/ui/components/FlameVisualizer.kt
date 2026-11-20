package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

const val FLAME_MAX_SCALE_SCORE = 30_000.0

/**
 * Visual state definition for the Flame System (0 to 30,000+):
 * - 0 to 30: Frozen / Static crystal ice stasis
 * - 31 to 60: Thawing ice fissures breaking open with emerging cyan wisps
 * - 61 to 3,000: Dynamic Thermal Gradient shifting from arctic cyan -> violet -> crimson
 * - 3,001 to 12,000: Blazing Inferno with multi-tongue flickering fire & rising golden sparks
 * - 12,001 to 24,000: Supernova Storm — incandescent white-violet plasma vortex & electric arcs
 * - 24,001 to 30,000+: Obsidian Black Plasma — abyssal dark-matter plasma core with neon corona & lightning
 */
enum class FlameVisualStage {
    FROZEN_STATIC,       // 0 - 30
    ICE_CRACKING,        // 31 - 60
    COLOR_GRADIENT,      // 61 - 3,000
    FULLY_IGNITED,       // 3,001 - 12,000
    SUPERNOVA_OVERLOAD,  // 12,001 - 24,000
    OBSIDIAN_PLASMA      // 24,001 - 30,000+
}

fun getFlameStage(score: Double): FlameVisualStage {
    return when {
        score <= 30.0 -> FlameVisualStage.FROZEN_STATIC
        score <= 60.0 -> FlameVisualStage.ICE_CRACKING
        score <= 3_000.0 -> FlameVisualStage.COLOR_GRADIENT
        score <= 12_000.0 -> FlameVisualStage.FULLY_IGNITED
        score <= 24_000.0 -> FlameVisualStage.SUPERNOVA_OVERLOAD
        else -> FlameVisualStage.OBSIDIAN_PLASMA
    }
}

/**
 * Interpolates flame colors (apex, body, base) smoothly across the full 0..30,000+ spectrum.
 */
fun getFlameColors(score: Double): Triple<Color, Color, Color> {
    return when {
        score <= 30.0 -> {
            Triple(
                Color(0xFFE0F2FE), // Glacial white-blue apex
                Color(0xFF7DD3FC), // Icy cyan body
                Color(0xFF0369A1)  // Deep frost base
            )
        }
        score <= 60.0 -> {
            val t = ((score - 30.0) / 30.0).toFloat().coerceIn(0f, 1f)
            Triple(
                lerp(Color(0xFFE0F2FE), Color(0xFFBAE6FD), t),
                lerp(Color(0xFF7DD3FC), Color(0xFF00E5FF), t),
                lerp(Color(0xFF0369A1), Color(0xFF0284C7), t)
            )
        }
        score <= 3_000.0 -> {
            val t = ((score - 61.0) / (3_000.0 - 61.0)).toFloat().coerceIn(0f, 1f)
            val apex = when {
                t < 0.30f -> lerp(Color(0xFF38BDF8), Color(0xFFFDE047), t / 0.30f)
                t < 0.70f -> lerp(Color(0xFFFDE047), Color(0xFFFB923C), (t - 0.30f) / 0.40f)
                else -> lerp(Color(0xFFFB923C), Color(0xFFFEF08A), (t - 0.70f) / 0.30f)
            }
            val body = when {
                t < 0.30f -> lerp(Color(0xFF00E5FF), Color(0xFFA855F7), t / 0.30f)
                t < 0.70f -> lerp(Color(0xFFA855F7), Color(0xFFE11D48), (t - 0.30f) / 0.40f)
                else -> lerp(Color(0xFFE11D48), Color(0xFFEA580C), (t - 0.70f) / 0.30f)
            }
            val base = when {
                t < 0.30f -> lerp(Color(0xFF0C4A6E), Color(0xFF581C87), t / 0.30f)
                t < 0.70f -> lerp(Color(0xFF581C87), Color(0xFF881337), (t - 0.30f) / 0.40f)
                else -> lerp(Color(0xFF881337), Color(0xFF7F1D1D), (t - 0.70f) / 0.30f)
            }
            Triple(apex, body, base)
        }
        score <= 12_000.0 -> {
            val t = ((score - 3_001.0) / (12_000.0 - 3_001.0)).toFloat().coerceIn(0f, 1f)
            Triple(
                lerp(Color(0xFFFFFBEB), Color(0xFFFDE047), t), // White-gold apex
                lerp(Color(0xFFF97316), Color(0xFFEF4444), t), // Blazing furnace orange-crimson
                lerp(Color(0xFF991B1B), Color(0xFF7F1D1D), t)  // Deep ember base
            )
        }
        score <= 24_000.0 -> {
            val t = ((score - 12_001.0) / (24_000.0 - 12_001.0)).toFloat().coerceIn(0f, 1f)
            Triple(
                Color(0xFFFFFFFF),                             // Blinding white apex
                lerp(Color(0xFFFB7185), Color(0xFFC084FC), t), // Solar rose -> neon ultraviolet
                lerp(Color(0xFF7C2D12), Color(0xFF4C1D95), t)  // Furnace -> cosmic ultraviolet base
            )
        }
        else -> {
            // Obsidian Black Plasma (24,001 .. 30,000+)
            val t = ((score - 24_001.0) / (30_000.0 - 24_001.0)).toFloat().coerceIn(0f, 1f)
            Triple(
                lerp(Color(0xFFF5D0FE), Color(0xFFFFFFFF), t), // White-hot plasma tip
                lerp(Color(0xFF9333EA), Color(0xFF1E0836), t), // Deepening into Obsidian Violet-Black body
                lerp(Color(0xFF2E1065), Color(0xFF06050C), t)  // Abyssal Obsidian Black base
            )
        }
    }
}

private data class Particle(
    val initialXRatio: Float,
    val speedMult: Int,
    val sizeRatio: Float,
    val phase: Float,
    val variant: Int
)

private class FlamePathPool {
    val mainBody = Path()
    val innerCore = Path()
    val obsidianVoidCore = Path()
    val leftTongue = Path()
    val rightTongue = Path()
    val crownTongue = Path()
    val wispPath = Path()
    val auxPath = Path()
}

@Composable
fun FlameVisualizer(
    score: Double,
    modifier: Modifier = Modifier
) {
    val stage = getFlameStage(score)
    val colors = getFlameColors(score)

    // Constant seamless endless-loop driver: integer harmonics of `time` ensure t=2π == t=0 identically.
    val infiniteTransition = rememberInfiniteTransition(label = "flame_endless_loop")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flame_time_phase"
    )

    // Allocation-free precomputed seeds (integer speed multipliers guarantee zero loop seams)
    val particles = remember {
        List(64) { index ->
            Particle(
                initialXRatio = ((index * 37) % 100) / 100f,
                speedMult = 1 + (index % 3),
                sizeRatio = 0.55f + ((index * 13) % 10) * 0.09f,
                phase = (index * 0.137f * 2f * PI.toFloat()),
                variant = index % 4
            )
        }
    }

    val pathPool = remember { FlamePathPool() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Continuous energy intensity across 0..30,000+ (smoothly drives tongue amplitude, height & turbulence)
            val normalized30k = (score / FLAME_MAX_SCALE_SCORE).toFloat().coerceIn(0f, 1f)
            val motionFactor = when (stage) {
                FlameVisualStage.FROZEN_STATIC -> 0f
                FlameVisualStage.ICE_CRACKING -> ((score - 30.0) / 30.0).toFloat().coerceIn(0f, 1f) * 0.40f
                FlameVisualStage.COLOR_GRADIENT -> 0.45f + ((score - 61.0) / 2_939.0).toFloat().coerceIn(0f, 1f) * 0.45f
                FlameVisualStage.FULLY_IGNITED -> 0.90f + ((score - 3_001.0) / 8_999.0).toFloat().coerceIn(0f, 1f) * 0.35f
                FlameVisualStage.SUPERNOVA_OVERLOAD -> 1.25f + ((score - 12_001.0) / 11_999.0).toFloat().coerceIn(0f, 1f) * 0.35f
                FlameVisualStage.OBSIDIAN_PLASMA -> 1.60f + normalized30k * 0.35f
            }

            // 1. Ambient Background Convection Aura
            drawAuraGlow(cx, cy, w, h, colors, stage, time, motionFactor)

            // 2. Outer Flanking Flame Tongues (realistic asymmetric fire licks)
            if (stage != FlameVisualStage.FROZEN_STATIC) {
                drawSideFlameTongues(cx, cy, w, h, colors, time, motionFactor, stage, pathPool)
            }

            // 3. Primary Traveling-Wave Flame Column
            drawFlameBody(cx, cy, w, h, colors, time, motionFactor, stage, pathPool)

            // 4. Detached Rising Flame Wisps (pinched off flamelets)
            if (motionFactor > 0.25f) {
                drawDetachedFlameWisps(cx, cy, w, h, colors, time, motionFactor, stage, pathPool.wispPath)
            }

            // 5. Incandescent Inner Core (or Obsidian Void Core at 24k-30k+)
            drawInnerCoreFlame(cx, cy, w, h, colors, time, motionFactor, stage, pathPool)

            // 6. Score-Specific Overlays (Ice Crystal / Cracks / Embers / Inferno Sparks / Plasma Arcs)
            when (stage) {
                FlameVisualStage.FROZEN_STATIC -> {
                    drawFrozenIceShield(cx, cy, w, h, time, pathPool.auxPath)
                }
                FlameVisualStage.ICE_CRACKING -> {
                    val crackProgress = ((score - 30.0) / 30.0).toFloat().coerceIn(0f, 1f)
                    drawCrackingIce(cx, cy, w, h, crackProgress, particles, time, pathPool.auxPath)
                }
                FlameVisualStage.COLOR_GRADIENT -> {
                    drawRisingEmbers(cx, cy, w, h, colors, particles, time, count = 30)
                }
                FlameVisualStage.FULLY_IGNITED -> {
                    drawSuperchargedInferno(cx, cy, w, h, particles, time)
                }
                FlameVisualStage.SUPERNOVA_OVERLOAD -> {
                    drawSupernovaOverload(cx, cy, w, h, particles, time, pathPool.auxPath, isObsidian = false)
                }
                FlameVisualStage.OBSIDIAN_PLASMA -> {
                    drawSupernovaOverload(cx, cy, w, h, particles, time, pathPool.auxPath, isObsidian = true)
                }
            }
        }
    }
}

/**
 * Builds an organic tapered flame tongue silhouette using upward-traveling thermal sine harmonics.
 * Because `yFrac` moves smoothly from 0 (fuel base) to 1 (tip) and time multipliers are integers,
 * the contour ripples continuously upward and wraps with zero seam at t = 2π.
 */
private fun buildOrganicFlamePath(
    path: Path,
    baseCenterX: Float,
    baseY: Float,
    maxHalfWidth: Float,
    height: Float,
    time: Float,
    motionFactor: Float,
    leanX: Float,
    phaseOffset: Float,
    primarySpeed: Int,
    secondarySpeed: Int,
    steps: Int = 20
): Offset {
    path.reset()

    fun spineX(yFrac: Float): Float {
        if (motionFactor <= 0f) return baseCenterX + leanX * yFrac
        val envelope = yFrac.pow(1.25f)
        val wave1 = sin(yFrac * 6.2f - time * primarySpeed + phaseOffset)
        val wave2 = sin(yFrac * 11.4f - time * secondarySpeed + phaseOffset * 1.6f) * 0.45f
        val wave3 = cos(yFrac * 16.0f - time * (secondarySpeed + 1) + phaseOffset * 2.1f) * 0.20f
        return baseCenterX + leanX * yFrac + (wave1 + wave2 + wave3) * (maxHalfWidth * 0.34f * motionFactor * envelope)
    }

    fun halfW(yFrac: Float): Float {
        // Teardrop bulb at lower 28%, smooth inward taper toward a sharp whipping tip
        val baseBell = sin((yFrac.coerceIn(0f, 1f)).pow(0.62f) * PI.toFloat())
        val pinch = 1f - yFrac.pow(1.55f) * 0.88f
        val flicker = if (motionFactor > 0f) {
            1f + sin(yFrac * 8.5f - time * secondarySpeed + phaseOffset) * 0.14f * motionFactor * yFrac
        } else 1f
        return (maxHalfWidth * (0.28f + 0.72f * baseBell) * pinch * flicker).coerceAtLeast(0.5f)
    }

    val tipY = baseY - height
    val tipX = spineX(1f)

    // Rounded fuel base bottom
    path.moveTo(baseCenterX, baseY + maxHalfWidth * 0.18f)

    // Left ascending edge (base -> tip)
    for (i in 1 until steps) {
        val yFrac = i.toFloat() / steps
        val y = baseY - height * yFrac
        val cx = spineX(yFrac)
        val hw = halfW(yFrac)
        val edgeWave = if (motionFactor > 0f) {
            sin(yFrac * 10.5f - time * (primarySpeed + 1) + phaseOffset) * maxHalfWidth * 0.10f * motionFactor * yFrac
        } else 0f
        path.lineTo(cx - hw + edgeWave, y)
    }

    // Sharp apex tip
    path.lineTo(tipX, tipY)

    // Right descending edge (tip -> base)
    for (i in (steps - 1) downTo 1) {
        val yFrac = i.toFloat() / steps
        val y = baseY - height * yFrac
        val cx = spineX(yFrac)
        val hw = halfW(yFrac)
        val edgeWave = if (motionFactor > 0f) {
            cos(yFrac * 9.8f - time * (primarySpeed + 1) - phaseOffset) * maxHalfWidth * 0.10f * motionFactor * yFrac
        } else 0f
        path.lineTo(cx + hw + edgeWave, y)
    }

    path.close()
    return Offset(tipX, tipY)
}

/**
 * Draws the soft radial ambient glow behind the flame.
 */
private fun DrawScope.drawAuraGlow(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    stage: FlameVisualStage,
    time: Float,
    motionFactor: Float
) {
    val pulseFreq = if (stage >= FlameVisualStage.SUPERNOVA_OVERLOAD) 3 else 2
    val pulse = if (motionFactor > 0f) 1f + sin(time * pulseFreq) * 0.08f * motionFactor.coerceAtMost(1.5f) else 1f
    val auraRadius = (min(w, h) * (if (stage >= FlameVisualStage.SUPERNOVA_OVERLOAD) 0.54f else 0.47f)) * pulse

    val glowAlpha = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> 0.18f
        FlameVisualStage.ICE_CRACKING -> 0.24f
        FlameVisualStage.COLOR_GRADIENT -> 0.34f
        FlameVisualStage.FULLY_IGNITED -> 0.50f
        FlameVisualStage.SUPERNOVA_OVERLOAD -> 0.68f
        FlameVisualStage.OBSIDIAN_PLASMA -> 0.78f
    }

    val auraInner = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) Color(0xFFE879F9) else colors.second
    val auraOuter = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) Color(0xFF22D3EE) else colors.third

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                auraInner.copy(alpha = glowAlpha),
                auraOuter.copy(alpha = glowAlpha * 0.42f),
                Color.Transparent
            ),
            center = Offset(cx, cy + h * 0.04f),
            radius = auraRadius
        ),
        radius = auraRadius,
        center = Offset(cx, cy + h * 0.04f)
    )

    if (stage == FlameVisualStage.SUPERNOVA_OVERLOAD || stage == FlameVisualStage.OBSIDIAN_PLASMA) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) 0.45f else 0.52f),
                    Color(0xFFC084FC).copy(alpha = 0.34f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = auraRadius * 0.56f
            ),
            radius = auraRadius * 0.56f,
            center = Offset(cx, cy)
        )
    }
}

/**
 * Draws independent left, right, and crown secondary flame tongues that dance around the main column.
 */
private fun DrawScope.drawSideFlameTongues(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    time: Float,
    motionFactor: Float,
    stage: FlameVisualStage,
    pool: FlamePathPool
) {
    val heightScale = 0.90f + motionFactor.coerceAtMost(1.8f) * 0.14f
    val baseY = cy + h * 0.32f
    val sideHalfW = w * 0.14f * (0.85f + motionFactor.coerceAtMost(1.5f) * 0.18f)
    val sideHeight = h * 0.46f * heightScale

    // Left tongue
    val leftBaseX = cx - w * 0.13f + sin(time * 2f) * 4f * motionFactor
    val leftTip = buildOrganicFlamePath(
        path = pool.leftTongue,
        baseCenterX = leftBaseX,
        baseY = baseY,
        maxHalfWidth = sideHalfW,
        height = sideHeight * (0.92f + sin(time * 3f) * 0.10f),
        time = time,
        motionFactor = motionFactor,
        leanX = -w * 0.05f + sin(time * 2f) * 8f,
        phaseOffset = 0.8f,
        primarySpeed = 2,
        secondarySpeed = 3,
        steps = 16
    )

    val tongueAlpha = when {
        stage == FlameVisualStage.ICE_CRACKING -> 0.52f
        stage >= FlameVisualStage.SUPERNOVA_OVERLOAD -> 0.88f
        else -> 0.76f
    }

    val leftTipColor = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) Color(0xFF22D3EE) else colors.first
    val rightTipColor = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) Color(0xFFFDE047) else colors.first

    drawPath(
        path = pool.leftTongue,
        brush = Brush.verticalGradient(
            colors = listOf(
                leftTipColor.copy(alpha = tongueAlpha),
                colors.second.copy(alpha = tongueAlpha * 0.85f),
                colors.third.copy(alpha = tongueAlpha * 0.45f)
            ),
            startY = leftTip.y,
            endY = baseY
        )
    )

    // Right tongue
    val rightBaseX = cx + w * 0.13f + cos(time * 2f) * 4f * motionFactor
    val rightTip = buildOrganicFlamePath(
        path = pool.rightTongue,
        baseCenterX = rightBaseX,
        baseY = baseY,
        maxHalfWidth = sideHalfW * 0.96f,
        height = sideHeight * (0.88f + cos(time * 3f + 0.5f) * 0.12f),
        time = time,
        motionFactor = motionFactor,
        leanX = w * 0.05f + cos(time * 2f) * 8f,
        phaseOffset = 2.4f,
        primarySpeed = 2,
        secondarySpeed = 4,
        steps = 16
    )

    drawPath(
        path = pool.rightTongue,
        brush = Brush.verticalGradient(
            colors = listOf(
                rightTipColor.copy(alpha = tongueAlpha),
                colors.second.copy(alpha = tongueAlpha * 0.85f),
                colors.third.copy(alpha = tongueAlpha * 0.45f)
            ),
            startY = rightTip.y,
            endY = baseY
        )
    )

    // High-energy 3rd upper crown tongue for blazing inferno / plasma levels
    if (stage >= FlameVisualStage.FULLY_IGNITED) {
        val crownTip = buildOrganicFlamePath(
            path = pool.crownTongue,
            baseCenterX = cx + sin(time * 1f) * 5f,
            baseY = baseY - h * 0.06f,
            maxHalfWidth = w * 0.11f,
            height = h * 0.72f * (0.95f + sin(time * 4f) * 0.08f),
            time = time,
            motionFactor = motionFactor * 1.15f,
            leanX = sin(time * 2f + 1.2f) * 12f,
            phaseOffset = 4.1f,
            primarySpeed = 3,
            secondarySpeed = 5,
            steps = 16
        )
        drawPath(
            path = pool.crownTongue,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    if (stage == FlameVisualStage.OBSIDIAN_PLASMA) Color(0xFFE879F9).copy(alpha = 0.75f)
                    else colors.first.copy(alpha = 0.70f),
                    Color.Transparent
                ),
                startY = crownTip.y,
                endY = baseY - h * 0.06f
            )
        )
    }
}

/**
 * Draws the primary dynamic volumetric flame column using upward traveling thermal waves.
 */
private fun DrawScope.drawFlameBody(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    time: Float,
    motionFactor: Float,
    stage: FlameVisualStage,
    pool: FlamePathPool
) {
    val heightGrowth = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> 0.58f
        FlameVisualStage.ICE_CRACKING -> 0.62f
        FlameVisualStage.COLOR_GRADIENT -> 0.68f
        FlameVisualStage.FULLY_IGNITED -> 0.73f
        FlameVisualStage.SUPERNOVA_OVERLOAD -> 0.77f
        FlameVisualStage.OBSIDIAN_PLASMA -> 0.80f
    }
    val flameWidth = w * 0.25f
    val flameHeight = h * heightGrowth
    val baseY = cy + h * 0.34f

    val tipOffset = buildOrganicFlamePath(
        path = pool.mainBody,
        baseCenterX = cx,
        baseY = baseY,
        maxHalfWidth = flameWidth,
        height = flameHeight,
        time = time,
        motionFactor = motionFactor,
        leanX = if (motionFactor > 0f) sin(time * 1f) * 7f * motionFactor.coerceAtMost(1.4f) else 0f,
        phaseOffset = 0f,
        primarySpeed = 2,
        secondarySpeed = 3,
        steps = 22
    )

    val flameBrush = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFE879F9),
                Color(0xFF4C1D95),
                Color(0xFF090714)
            ),
            startY = tipOffset.y,
            endY = baseY
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colors.first,
                colors.second,
                colors.third
            ),
            startY = tipOffset.y,
            endY = baseY
        )
    }

    drawPath(path = pool.mainBody, brush = flameBrush)

    val strokeColor = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> Color.White.copy(alpha = 0.65f)
        FlameVisualStage.SUPERNOVA_OVERLOAD -> Color.White.copy(alpha = 0.85f)
        FlameVisualStage.OBSIDIAN_PLASMA -> Color(0xFFFDE047).copy(alpha = 0.90f)
        else -> colors.first.copy(alpha = 0.50f)
    }

    drawPath(
        path = pool.mainBody,
        color = strokeColor,
        style = Stroke(
            width = if (stage >= FlameVisualStage.SUPERNOVA_OVERLOAD) 2.2.dp.toPx() else 1.6.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Draws detached rising flame wisps (pinched-off flamelets) ascending from the upper crown and fading organically.
 */
private fun DrawScope.drawDetachedFlameWisps(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    time: Float,
    motionFactor: Float,
    stage: FlameVisualStage,
    wispPath: Path
) {
    val wispCount = when {
        stage >= FlameVisualStage.OBSIDIAN_PLASMA -> 8
        stage >= FlameVisualStage.SUPERNOVA_OVERLOAD -> 7
        stage >= FlameVisualStage.FULLY_IGNITED -> 6
        else -> 4
    }

    val startBaseY = cy - h * 0.14f
    val riseDist = h * 0.30f

    for (i in 0 until wispCount) {
        val speedInt = 1 + (i % 2)
        val rawPhase = ((time * speedInt + i * (2f * PI.toFloat() / wispCount)) % (2f * PI.toFloat()))
        val p = rawPhase / (2f * PI.toFloat()) // 0..1 seamless cycle

        val lifeAlpha = sin(p * PI.toFloat()).coerceIn(0f, 1f)
        val wispBaseY = startBaseY - p * riseDist
        val spreadX = ((i % 3) - 1) * w * 0.11f
        val driftX = cx + spreadX + sin(p * 2f * PI.toFloat() + i) * 10f * motionFactor.coerceAtMost(1.4f)

        val wispH = (h * 0.13f * (1f - p * 0.55f)).coerceAtLeast(6f)
        val wispHalfW = (w * 0.045f * (1f - p * 0.65f)).coerceAtLeast(2f)

        wispPath.reset()
        wispPath.moveTo(driftX, wispBaseY)
        wispPath.cubicTo(
            driftX - wispHalfW, wispBaseY - wispH * 0.35f,
            driftX - wispHalfW * 0.45f, wispBaseY - wispH * 0.78f,
            driftX + sin(time * 2f + i) * 4f, wispBaseY - wispH
        )
        wispPath.cubicTo(
            driftX + wispHalfW * 0.45f, wispBaseY - wispH * 0.78f,
            driftX + wispHalfW, wispBaseY - wispH * 0.35f,
            driftX, wispBaseY
        )
        wispPath.close()

        val wispColor = when (stage) {
            FlameVisualStage.OBSIDIAN_PLASMA -> if (i % 2 == 0) Color(0xFFE879F9) else Color(0xFF22D3EE)
            FlameVisualStage.SUPERNOVA_OVERLOAD -> if (i % 2 == 0) Color.White else Color(0xFFC084FC)
            else -> if (i % 2 == 0) colors.first else colors.second
        }

        drawPath(
            path = wispPath,
            color = wispColor.copy(alpha = lifeAlpha * 0.72f)
        )
    }
}

/**
 * Draws the luminous inner heart of the flame (and the Abyssal Obsidian Plasma Void Core at 24,001 - 30,000+).
 */
private fun DrawScope.drawInnerCoreFlame(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    time: Float,
    motionFactor: Float,
    stage: FlameVisualStage,
    pool: FlamePathPool
) {
    val coreWidth = w * 0.135f
    val coreHeight = h * 0.42f
    val baseY = cy + h * 0.28f

    val coreTip = buildOrganicFlamePath(
        path = pool.innerCore,
        baseCenterX = cx,
        baseY = baseY,
        maxHalfWidth = coreWidth,
        height = coreHeight,
        time = time,
        motionFactor = motionFactor * 0.9f,
        leanX = if (motionFactor > 0f) cos(time * 2f) * 4f * motionFactor.coerceAtMost(1.3f) else 0f,
        phaseOffset = 1.4f,
        primarySpeed = 3,
        secondarySpeed = 4,
        steps = 16
    )

    val coreColors = when (stage) {
        FlameVisualStage.FULLY_IGNITED -> {
            listOf(Color(0xFFFFFFFF), Color(0xFFFDE047), Color(0xFFF97316))
        }
        FlameVisualStage.SUPERNOVA_OVERLOAD -> {
            listOf(Color(0xFFFFFFFF), Color(0xFFF5D0FE), Color(0xFFC084FC))
        }
        FlameVisualStage.OBSIDIAN_PLASMA -> {
            listOf(Color(0xFFFFFFFF), Color(0xFFFDE047), Color(0xFFE879F9))
        }
        else -> {
            listOf(
                Color.White.copy(alpha = 0.94f),
                colors.first.copy(alpha = 0.80f),
                colors.second.copy(alpha = 0.52f)
            )
        }
    }

    drawPath(
        path = pool.innerCore,
        brush = Brush.verticalGradient(
            colors = coreColors,
            startY = coreTip.y,
            endY = baseY
        )
    )

    // In Obsidian Plasma phase, carve a dramatic pitch-black void singularity core inside the white-gold rim!
    if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
        val voidBaseY = cy + h * 0.24f
        val voidTip = buildOrganicFlamePath(
            path = pool.obsidianVoidCore,
            baseCenterX = cx,
            baseY = voidBaseY,
            maxHalfWidth = coreWidth * 0.56f,
            height = coreHeight * 0.58f,
            time = time,
            motionFactor = motionFactor * 0.7f,
            leanX = 0f,
            phaseOffset = 3.1f,
            primarySpeed = 3,
            secondarySpeed = 5,
            steps = 14
        )
        drawPath(
            path = pool.obsidianVoidCore,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E0B36),
                    Color(0xFF06040D),
                    Color(0xFF020206)
                ),
                startY = voidTip.y,
                endY = voidBaseY
            )
        )
        drawPath(
            path = pool.obsidianVoidCore,
            color = Color(0xFF22D3EE).copy(alpha = 0.85f),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws the frozen hexagonal ice shield and sharp geometric frost crystals (Score 0-30).
 */
private fun DrawScope.drawFrozenIceShield(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    time: Float,
    hexPath: Path
) {
    val shieldRadius = min(w, h) * 0.36f

    hexPath.reset()
    for (i in 0 until 6) {
        val angle = (i * 60f - 30f) * (PI.toFloat() / 180f)
        val x = cx + shieldRadius * cos(angle)
        val y = cy + shieldRadius * sin(angle)
        if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
    }
    hexPath.close()

    val shimmer = 0.82f + sin(time * 1f) * 0.14f

    drawPath(
        path = hexPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f * shimmer),
                Color(0x33BAE6FD),
                Color(0x1A0284C7)
            ),
            start = Offset(cx - shieldRadius, cy - shieldRadius),
            end = Offset(cx + shieldRadius, cy + shieldRadius)
        )
    )

    drawPath(
        path = hexPath,
        color = Color.White.copy(alpha = 0.88f * shimmer),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    drawLine(
        color = Color(0xCCBAE6FD),
        start = Offset(cx, cy - shieldRadius),
        end = Offset(cx, cy + shieldRadius),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = Color(0xCCBAE6FD),
        start = Offset(cx - shieldRadius * 0.86f, cy - shieldRadius * 0.5f),
        end = Offset(cx + shieldRadius * 0.86f, cy + shieldRadius * 0.5f),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = Color(0xCCBAE6FD),
        start = Offset(cx - shieldRadius * 0.86f, cy + shieldRadius * 0.5f),
        end = Offset(cx + shieldRadius * 0.86f, cy - shieldRadius * 0.5f),
        strokeWidth = 1.5.dp.toPx()
    )

    val starPoints = listOf(
        Offset(cx, cy),
        Offset(cx - shieldRadius * 0.45f, cy - shieldRadius * 0.25f),
        Offset(cx + shieldRadius * 0.45f, cy + shieldRadius * 0.25f),
        Offset(cx + shieldRadius * 0.45f, cy - shieldRadius * 0.25f),
        Offset(cx - shieldRadius * 0.45f, cy + shieldRadius * 0.25f)
    )

    for (pt in starPoints) {
        drawCircle(color = Color.White.copy(alpha = 0.95f), radius = 3.dp.toPx(), center = pt)
        drawCircle(color = Color(0x9938BDF8), radius = 6.dp.toPx(), center = pt)
    }
}

/**
 * Draws the cracking ice fracture lines and flaking ice dust particles (Score 31-60).
 */
private fun DrawScope.drawCrackingIce(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    progress: Float,
    particles: List<Particle>,
    time: Float,
    crackPath: Path
) {
    val crackAlpha = (1f - progress * 0.4f).coerceIn(0.2f, 1f)
    val maxLen = min(w, h) * 0.38f * progress

    val crackColor = Color.White.copy(alpha = crackAlpha)
    val branchColor = Color(0xFFBAE6FD).copy(alpha = crackAlpha * 0.85f)
    val strokeW = (2.2f * (1f + progress * 0.6f)).dp.toPx()

    val angles = listOf(45f, 135f, 220f, 310f, 85f, 275f)
    for ((idx, baseDeg) in angles.withIndex()) {
        val rad = baseDeg * (PI.toFloat() / 180f)
        val len = maxLen * (0.7f + (idx % 3) * 0.15f)

        var currX = cx
        var currY = cy
        val steps = 4
        val stepLen = len / steps

        crackPath.reset()
        crackPath.moveTo(currX, currY)
        for (s in 1..steps) {
            val jiggleAngle = rad + sin(s * 2.3f + idx) * 0.25f
            currX += stepLen * cos(jiggleAngle)
            currY += stepLen * sin(jiggleAngle)
            crackPath.lineTo(currX, currY)
        }

        drawPath(
            path = crackPath,
            color = if (idx % 2 == 0) crackColor else branchColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    val shardCount = (progress * 24).toInt()
    for (i in 0 until shardCount) {
        val p = particles[i]
        val pTime = (time * p.speedMult + p.phase) % (2f * PI.toFloat())
        val normT = pTime / (2f * PI.toFloat())

        val pDist = min(w, h) * 0.32f * (0.4f + normT * 0.7f)
        val pAngle = p.initialXRatio * 2f * PI.toFloat() + normT * 0.5f

        val px = cx + pDist * cos(pAngle)
        val py = cy + pDist * sin(pAngle) - normT * 20f

        val pAlpha = (1f - normT) * 0.8f
        drawCircle(
            color = Color.White.copy(alpha = pAlpha),
            radius = (1.5f + p.sizeRatio * 1.5f).dp.toPx(),
            center = Offset(px, py)
        )
    }
}

/**
 * Draws floating thermal embers and rising sparks (Score 61-3,000).
 */
private fun DrawScope.drawRisingEmbers(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    particles: List<Particle>,
    time: Float,
    count: Int
) {
    val flameHeight = h * 0.65f
    val flameWidth = w * 0.45f

    for (i in 0 until count) {
        val p = particles[i]
        val pProgress = ((time * p.speedMult + p.phase) % (2f * PI.toFloat())) / (2f * PI.toFloat())

        val startY = cy + flameHeight * 0.32f
        val endY = cy - flameHeight * 0.68f
        val py = startY - pProgress * (startY - endY)

        val spread = flameWidth * 0.65f * (1f - pProgress * 0.35f)
        val px = cx + (p.initialXRatio - 0.5f) * spread + sin(pProgress * 2f * PI.toFloat() * 2f + p.phase) * 8f

        val pAlpha = (sin(pProgress * PI.toFloat())).coerceIn(0f, 1f) * 0.9f
        val emberColor = when (p.variant) {
            0 -> Color(0xFFFDE047).copy(alpha = pAlpha)
            1 -> Color(0xFFF43F5E).copy(alpha = pAlpha)
            else -> colors.first.copy(alpha = pAlpha)
        }

        val emberRadius = (1.6f + p.sizeRatio * 1.6f).dp.toPx()

        drawCircle(
            color = emberColor,
            radius = emberRadius,
            center = Offset(px, py)
        )
    }
}

/**
 * Draws the blazing supercharged inferno with intense rising glowing orange, gold and rose sparks (3,001 - 12,000).
 */
private fun DrawScope.drawSuperchargedInferno(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    particles: List<Particle>,
    time: Float
) {
    val flameHeight = h * 0.72f
    val flameWidth = w * 0.56f

    val outerPulse = 1f + sin(time * 3f) * 0.11f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x55F59E0B),
                Color(0x33F43F5E),
                Color(0x117F1D1D),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = min(w, h) * 0.50f * outerPulse
        ),
        radius = min(w, h) * 0.50f * outerPulse,
        center = Offset(cx, cy)
    )

    for (i in 0 until 52) {
        val p = particles[i]
        val pProgress = ((time * (p.speedMult + 1) + p.phase) % (2f * PI.toFloat())) / (2f * PI.toFloat())

        val startY = cy + flameHeight * 0.40f
        val endY = cy - flameHeight * 0.85f
        val py = startY - pProgress * (startY - endY)

        val spread = flameWidth * 0.85f * (0.8f + pProgress * 0.5f)
        val px = cx + (p.initialXRatio - 0.5f) * spread + sin(pProgress * 2f * PI.toFloat() * 2f + p.phase) * 14f

        val pAlpha = (sin(pProgress * PI.toFloat())).coerceIn(0f, 1f)

        val sparkColor = when (p.variant) {
            0 -> Color(0xFFFB7185).copy(alpha = pAlpha)
            1 -> Color(0xFFFDE047).copy(alpha = pAlpha)
            2 -> Color(0xFFF97316).copy(alpha = pAlpha)
            else -> Color(0xFFFFFFFF).copy(alpha = pAlpha)
        }

        val sparkRadius = (1.9f + p.sizeRatio * 2.2f).dp.toPx()

        drawCircle(
            color = sparkColor.copy(alpha = pAlpha * 0.35f),
            radius = sparkRadius * 2.2f,
            center = Offset(px, py)
        )
        drawCircle(
            color = sparkColor,
            radius = sparkRadius,
            center = Offset(px, py)
        )
    }
}

/**
 * Draws the Supernova Overload (12,001 - 24,000) & Obsidian Black Plasma Max Power (24,001 - 30,000+)
 * with seamless endless shockwaves, electric arcs, and hyper-velocity plasma sparks.
 */
private fun DrawScope.drawSupernovaOverload(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    particles: List<Particle>,
    time: Float,
    arcPath: Path,
    isObsidian: Boolean
) {
    val flameHeight = h * 0.78f
    val flameWidth = w * 0.62f

    val shockwave1 = 1f + sin(time * 4f) * 0.14f
    val shockwave2 = 1f + cos(time * 3f) * 0.18f

    drawCircle(
        brush = Brush.radialGradient(
            colors = if (isObsidian) {
                listOf(
                    Color(0x66FFFFFF),
                    Color(0x55E879F9),
                    Color(0x4422D3EE),
                    Color(0x2205050A),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0x66FFFFFF),
                    Color(0x55C084FC),
                    Color(0x33A855F7),
                    Color(0x153B0764),
                    Color.Transparent
                )
            },
            center = Offset(cx, cy),
            radius = min(w, h) * 0.54f * shockwave1
        ),
        radius = min(w, h) * 0.54f * shockwave1,
        center = Offset(cx, cy)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x44E879F9),
                Color(0x2222D3EE),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = min(w, h) * 0.43f * shockwave2
        ),
        radius = min(w, h) * 0.43f * shockwave2,
        center = Offset(cx, cy)
    )

    // Electric plasma discharge arcs crackling seamlessly around perimeter
    val arcCount = if (isObsidian) 8 else 6
    for (idx in 0 until arcCount) {
        val deg = idx * (360f / arcCount) + 15f
        val baseAngle = deg * (PI.toFloat() / 180f) + sin(time * 3f + idx) * 0.16f
        val arcLen = min(w, h) * (0.34f + sin(time * 4f + idx * 2f) * 0.08f)
        var arcX = cx + min(w, h) * 0.15f * cos(baseAngle)
        var arcY = cy + min(w, h) * 0.15f * sin(baseAngle)

        arcPath.reset()
        arcPath.moveTo(arcX, arcY)
        for (step in 1..4) {
            val stepLen = arcLen / 4f
            val jitter = sin(time * 5f + idx * 3f + step) * 11f
            arcX += stepLen * cos(baseAngle) + jitter * -sin(baseAngle)
            arcY += stepLen * sin(baseAngle) + jitter * cos(baseAngle)
            arcPath.lineTo(arcX, arcY)
        }

        val arcColor = if (isObsidian) {
            when (idx % 3) {
                0 -> Color(0xFFFDE047).copy(alpha = 0.92f)
                1 -> Color(0xFFE879F9).copy(alpha = 0.90f)
                else -> Color(0xFF22D3EE).copy(alpha = 0.88f)
            }
        } else {
            if (idx % 2 == 0) Color.White.copy(alpha = 0.92f) else Color(0xFFE879F9).copy(alpha = 0.85f)
        }

        drawPath(
            path = arcPath,
            color = arcColor,
            style = Stroke(width = if (isObsidian) 2.dp.toPx() else 1.7.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Hyper-velocity plasma sparks
    for (i in 0 until 64) {
        val p = particles[i]
        val pProgress = ((time * (p.speedMult + 1) + p.phase) % (2f * PI.toFloat())) / (2f * PI.toFloat())

        val startY = cy + flameHeight * 0.45f
        val endY = cy - flameHeight * 1.02f
        val py = startY - pProgress * (startY - endY)

        val spread = flameWidth * 0.92f * (0.7f + pProgress * 0.65f)
        val turbulence = sin(pProgress * 2f * PI.toFloat() * 2f + p.phase) * 16f
        val px = cx + (p.initialXRatio - 0.5f) * spread + turbulence

        val pAlpha = (sin(pProgress * PI.toFloat())).coerceIn(0f, 1f)

        val particleColor = if (isObsidian) {
            when (p.variant) {
                0 -> Color(0xFFFDE047).copy(alpha = pAlpha)
                1 -> Color(0xFFE879F9).copy(alpha = pAlpha)
                2 -> Color(0xFF22D3EE).copy(alpha = pAlpha)
                else -> Color(0xFFFFFFFF).copy(alpha = pAlpha)
            }
        } else {
            when (p.variant) {
                0 -> Color(0xFFFFFFFF).copy(alpha = pAlpha)
                1 -> Color(0xFFE879F9).copy(alpha = pAlpha)
                2 -> Color(0xFFC084FC).copy(alpha = pAlpha)
                else -> Color(0xFF22D3EE).copy(alpha = pAlpha)
            }
        }

        val particleRadius = (2.0f + p.sizeRatio * 2.6f).dp.toPx()

        drawCircle(
            color = particleColor.copy(alpha = pAlpha * 0.42f),
            radius = particleRadius * 2.4f,
            center = Offset(px, py)
        )
        drawCircle(
            color = particleColor,
            radius = particleRadius,
            center = Offset(px, py)
        )
    }
}
