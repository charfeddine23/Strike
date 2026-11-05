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
import androidx.compose.foundation.layout.size
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
import kotlin.math.sin

/**
 * Visual state definition for the Flame System based on the exact user-specified score ranges:
 * - 0 to 30: Completely frozen / static
 * - 31 to 60: Ice begins to crack smoothly with subtle particle/crack effects
 * - 61 to 300: Flame dynamically transitions its color gradient from icy blue to deep dark red
 * - > 300: Fully ignited with intense blazing particles and glow
 */
enum class FlameVisualStage {
    FROZEN_STATIC,   // 0 - 30
    ICE_CRACKING,    // 31 - 60
    COLOR_GRADIENT,  // 61 - 300
    FULLY_IGNITED    // > 300
}

fun getFlameStage(score: Double): FlameVisualStage {
    return when {
        score <= 30.0 -> FlameVisualStage.FROZEN_STATIC
        score <= 60.0 -> FlameVisualStage.ICE_CRACKING
        score <= 300.0 -> FlameVisualStage.COLOR_GRADIENT
        else -> FlameVisualStage.FULLY_IGNITED
    }
}

/**
 * Interpolates flame colors smoothly based on the exact score ranges:
 * - Under 30: Glacial white & icy blue
 * - 31 - 60: Thawing ice cyan
 * - 61 - 300: Continuous smooth transition from icy blue to deep dark red
 * - > 300: Blazing incandescent gold-to-dark red inferno
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
                lerp(Color(0xFF7DD3FC), Color(0xFF38BDF8), t),
                lerp(Color(0xFF0369A1), Color(0xFF0284C7), t)
            )
        }
        score <= 300.0 -> {
            val t = ((score - 61.0) / (300.0 - 61.0)).toFloat().coerceIn(0f, 1f)
            // Interpolate across spectrum:
            // 0.0 (61) -> Icy Electric Blue
            // 0.35 (145) -> Electric Violet / Purple
            // 0.70 (228) -> Radiant Crimson / Orange
            // 1.0 (300) -> Deep Dark Red
            val apex = when {
                t < 0.35f -> lerp(Color(0xFF38BDF8), Color(0xFFC084FC), t / 0.35f)
                t < 0.70f -> lerp(Color(0xFFC084FC), Color(0xFFFB7185), (t - 0.35f) / 0.35f)
                else -> lerp(Color(0xFFFB7185), Color(0xFFEF4444), (t - 0.70f) / 0.30f)
            }
            val body = when {
                t < 0.35f -> lerp(Color(0xFF0284C7), Color(0xFF9333EA), t / 0.35f)
                t < 0.70f -> lerp(Color(0xFF9333EA), Color(0xFFE11D48), (t - 0.35f) / 0.35f)
                else -> lerp(Color(0xFFE11D48), Color(0xFFB91C1C), (t - 0.70f) / 0.30f)
            }
            val base = when {
                t < 0.35f -> lerp(Color(0xFF0C4A6E), Color(0xFF581C87), t / 0.35f)
                t < 0.70f -> lerp(Color(0xFF581C87), Color(0xFF881337), (t - 0.35f) / 0.35f)
                else -> lerp(Color(0xFF881337), Color(0xFF450A0A), (t - 0.70f) / 0.30f)
            }
            Triple(apex, body, base)
        }
        else -> {
            // Score > 300: Fully Ignited Blazing Inferno
            Triple(
                Color(0xFFFFFBEB), // Incandescent white-gold core
                Color(0xFFF97316), // Blazing neon orange
                Color(0xFF7F1D1D)  // Deep ember furnace red
            )
        }
    }
}

private data class Particle(
    val initialXRatio: Float,
    val speed: Float,
    val sizeRatio: Float,
    val phase: Float,
    val isPink: Boolean
)

@Composable
fun FlameVisualizer(
    score: Double,
    modifier: Modifier = Modifier
) {
    val stage = getFlameStage(score)
    val colors = getFlameColors(score)

    // Infinite animation loop for smooth 60fps physics
    val infiniteTransition = rememberInfiniteTransition(label = "flame_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flame_time"
    )

    // Precomputed particle seeds for allocation-free drawing
    val particles = remember {
        List(60) { index ->
            Particle(
                initialXRatio = (index * 0.0165f) % 1f,
                speed = 0.5f + ((index * 7) % 10) * 0.12f,
                sizeRatio = 0.6f + ((index * 13) % 10) * 0.10f,
                phase = (index * 0.105f * 2f * PI.toFloat()),
                isPink = (index % 3 == 0)
            )
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Animation motion factor: completely 0 for frozen 0-30, up to 1.8 for >300
            val motionFactor = when (stage) {
                FlameVisualStage.FROZEN_STATIC -> 0f
                FlameVisualStage.ICE_CRACKING -> ((score - 30.0) / 30.0).toFloat().coerceIn(0f, 1f) * 0.35f
                FlameVisualStage.COLOR_GRADIENT -> 0.35f + ((score - 61.0) / 239.0).toFloat().coerceIn(0f, 1f) * 0.65f
                FlameVisualStage.FULLY_IGNITED -> 1.0f + min(0.8f, ((score - 300.0) / 200.0).toFloat() * 0.4f)
            }

            // 1. Ambient Background Glow Aura
            drawAuraGlow(cx, cy, w, h, colors, stage, time, motionFactor)

            // 2. Base & Outer Flame Vector Silhouette
            drawFlameBody(cx, cy, w, h, colors, time, motionFactor, stage)

            // 3. Inner Core Flame
            drawInnerCoreFlame(cx, cy, w, h, colors, time, motionFactor, stage)

            // 4. Score-Specific Overlays (Ice Crystal / Cracks / Supercharged Inferno Sparks)
            when (stage) {
                FlameVisualStage.FROZEN_STATIC -> {
                    drawFrozenIceShield(cx, cy, w, h)
                }
                FlameVisualStage.ICE_CRACKING -> {
                    val crackProgress = ((score - 30.0) / 30.0).toFloat().coerceIn(0f, 1f)
                    drawCrackingIce(cx, cy, w, h, crackProgress, particles, time)
                }
                FlameVisualStage.COLOR_GRADIENT -> {
                    drawRisingEmbers(cx, cy, w, h, colors, particles, time, count = 28, stage = stage)
                }
                FlameVisualStage.FULLY_IGNITED -> {
                    drawSuperchargedInferno(cx, cy, w, h, colors, particles, time)
                }
            }
        }
    }
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
    val pulse = if (motionFactor > 0f) 1f + sin(time * 2f) * 0.08f * motionFactor else 1f
    val auraRadius = (min(w, h) * 0.46f) * pulse

    val glowAlpha = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> 0.16f
        FlameVisualStage.ICE_CRACKING -> 0.22f
        FlameVisualStage.COLOR_GRADIENT -> 0.32f
        FlameVisualStage.FULLY_IGNITED -> 0.52f
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                colors.second.copy(alpha = glowAlpha),
                colors.third.copy(alpha = glowAlpha * 0.4f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = auraRadius
        ),
        radius = auraRadius,
        center = Offset(cx, cy)
    )
}

/**
 * Draws the primary dynamic flame body using smooth cubic Bézier curves.
 */
private fun DrawScope.drawFlameBody(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    time: Float,
    motionFactor: Float,
    stage: FlameVisualStage
) {
    val flameWidth = w * 0.52f
    val flameHeight = h * 0.70f

    // Dynamic wave offsets for flicker
    val tipSway = if (motionFactor > 0f) sin(time * 3f) * 14f * motionFactor else 0f
    val waistSwayL = if (motionFactor > 0f) sin(time * 2.4f) * 10f * motionFactor else 0f
    val waistSwayR = if (motionFactor > 0f) cos(time * 2.8f) * 10f * motionFactor else 0f

    val tipX = cx + tipSway
    val tipY = cy - flameHeight * 0.48f

    val baseLeftX = cx - flameWidth * 0.38f + waistSwayL
    val baseRightX = cx + flameWidth * 0.38f + waistSwayR
    val baseY = cy + flameHeight * 0.44f

    val flamePath = Path().apply {
        moveTo(cx, baseY)

        // Left flame belly curve up to flame tip
        cubicTo(
            baseLeftX - flameWidth * 0.30f, baseY - flameHeight * 0.30f,
            cx - flameWidth * 0.45f + waistSwayL, cy - flameHeight * 0.22f,
            cx - flameWidth * 0.20f + waistSwayL * 0.5f, cy - flameHeight * 0.35f
        )
        cubicTo(
            cx - flameWidth * 0.10f, cy - flameHeight * 0.40f,
            tipX - 8f, tipY + 12f,
            tipX, tipY
        )

        // Right flame curve down from tip to base
        cubicTo(
            tipX + 8f, tipY + 14f,
            cx + flameWidth * 0.16f, cy - flameHeight * 0.38f,
            cx + flameWidth * 0.25f + waistSwayR * 0.5f, cy - flameHeight * 0.25f
        )
        cubicTo(
            cx + flameWidth * 0.52f + waistSwayR, cy - flameHeight * 0.10f,
            baseRightX + flameWidth * 0.30f, baseY - flameHeight * 0.30f,
            cx, baseY
        )
        close()
    }

    val flameBrush = Brush.verticalGradient(
        colors = listOf(
            colors.first,
            colors.second,
            colors.third
        ),
        startY = tipY,
        endY = baseY
    )

    drawPath(path = flamePath, brush = flameBrush)

    // Outer subtle contour stroke for glass depth
    val strokeColor = if (stage == FlameVisualStage.FROZEN_STATIC) {
        Color.White.copy(alpha = 0.65f)
    } else {
        colors.first.copy(alpha = 0.55f)
    }

    drawPath(
        path = flamePath,
        color = strokeColor,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Draws the high-intensity inner core flame.
 */
private fun DrawScope.drawInnerCoreFlame(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    time: Float,
    motionFactor: Float,
    stage: FlameVisualStage
) {
    val coreWidth = w * 0.28f
    val coreHeight = h * 0.42f

    val coreTipSway = if (motionFactor > 0f) sin(time * 3.8f + 1f) * 8f * motionFactor else 0f
    val tipX = cx + coreTipSway
    val tipY = cy - coreHeight * 0.40f
    val baseY = cy + coreHeight * 0.52f

    val corePath = Path().apply {
        moveTo(cx, baseY)
        cubicTo(
            cx - coreWidth * 0.6f, baseY - coreHeight * 0.35f,
            cx - coreWidth * 0.4f, cy - coreHeight * 0.15f,
            tipX, tipY
        )
        cubicTo(
            cx + coreWidth * 0.4f, cy - coreHeight * 0.15f,
            cx + coreWidth * 0.6f, baseY - coreHeight * 0.35f,
            cx, baseY
        )
        close()
    }

    val coreColors = if (stage == FlameVisualStage.FULLY_IGNITED) {
        listOf(Color(0xFFFFFFFF), Color(0xFFFDE047), Color(0xFFF97316))
    } else {
        listOf(
            Color.White.copy(alpha = 0.9f),
            colors.first.copy(alpha = 0.75f),
            colors.second.copy(alpha = 0.5f)
        )
    }

    drawPath(
        path = corePath,
        brush = Brush.verticalGradient(
            colors = coreColors,
            startY = tipY,
            endY = baseY
        )
    )
}

/**
 * Draws the frozen hexagonal ice shield and sharp geometric frost crystals (Score 0-30).
 */
private fun DrawScope.drawFrozenIceShield(cx: Float, cy: Float, w: Float, h: Float) {
    val shieldRadius = min(w, h) * 0.36f

    // Draw frosted hexagonal perimeter
    val hexPath = Path()
    for (i in 0 until 6) {
        val angle = (i * 60f - 30f) * (PI.toFloat() / 180f)
        val x = cx + shieldRadius * cos(angle)
        val y = cy + shieldRadius * sin(angle)
        if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
    }
    hexPath.close()

    // Semi-transparent icy glass facet fill
    drawPath(
        path = hexPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color(0x33BAE6FD),
                Color(0x1A0284C7)
            ),
            start = Offset(cx - shieldRadius, cy - shieldRadius),
            end = Offset(cx + shieldRadius, cy + shieldRadius)
        )
    )

    // Frost facet lines
    drawPath(
        path = hexPath,
        color = Color.White.copy(alpha = 0.85f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Sharp crystalline internal frost vectors
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

    // Star frost sparkles at crystal intersections
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
    time: Float
) {
    val crackAlpha = (1f - progress * 0.4f).coerceIn(0.2f, 1f)
    val maxLen = min(w, h) * 0.38f * progress

    // Draw main central fractures splitting outwards
    val crackColor = Color.White.copy(alpha = crackAlpha)
    val branchColor = Color(0xFFBAE6FD).copy(alpha = crackAlpha * 0.85f)
    val strokeW = (2.2f * (1f + progress * 0.6f)).dp.toPx()

    // 4 major fracture branches with jagged lightning-like segments
    val angles = listOf(45f, 135f, 220f, 310f, 85f, 275f)
    for ((idx, baseDeg) in angles.withIndex()) {
        val rad = baseDeg * (PI.toFloat() / 180f)
        val len = maxLen * (0.7f + (idx % 3) * 0.15f)

        var currX = cx
        var currY = cy
        val steps = 4
        val stepLen = len / steps

        val crackPath = Path().apply {
            moveTo(currX, currY)
            for (s in 1..steps) {
                val jiggleAngle = rad + sin(s * 2.3f + idx) * 0.25f
                currX += stepLen * cos(jiggleAngle)
                currY += stepLen * sin(jiggleAngle)
                lineTo(currX, currY)
            }
        }

        drawPath(
            path = crackPath,
            color = if (idx % 2 == 0) crackColor else branchColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Subtle flaking ice shard particles drifting off
    val shardCount = (progress * 24).toInt()
    for (i in 0 until shardCount) {
        val p = particles[i]
        val pTime = (time * p.speed + p.phase) % (2f * PI.toFloat())
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
 * Draws floating thermal embers and rising sparks (Score 61-300).
 */
private fun DrawScope.drawRisingEmbers(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    particles: List<Particle>,
    time: Float,
    count: Int,
    stage: FlameVisualStage
) {
    val flameHeight = h * 0.65f
    val flameWidth = w * 0.45f

    for (i in 0 until count) {
        val p = particles[i]
        val pProgress = ((time * (0.8f + p.speed * 0.4f) + p.phase) % (2f * PI.toFloat())) / (2f * PI.toFloat())

        // Embers rise from flame body upward
        val startY = cy + flameHeight * 0.30f
        val endY = cy - flameHeight * 0.65f
        val py = startY - pProgress * (startY - endY)

        val spread = flameWidth * 0.6f * (1f - pProgress * 0.4f)
        val px = cx + (p.initialXRatio - 0.5f) * spread + sin(pProgress * 6f + p.phase) * 8f

        val pAlpha = (sin(pProgress * PI.toFloat())).coerceIn(0f, 1f) * 0.9f
        val emberColor = if (p.isPink) {
            Color(0xFFF43F5E).copy(alpha = pAlpha)
        } else {
            colors.first.copy(alpha = pAlpha)
        }

        val emberRadius = (1.8f + p.sizeRatio * 1.8f).dp.toPx()

        drawCircle(
            color = emberColor,
            radius = emberRadius,
            center = Offset(px, py)
        )
    }
}

/**
 * Draws the blazing supercharged inferno with intense rising glowing orange and pink sparks (> 300).
 */
private fun DrawScope.drawSuperchargedInferno(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    colors: Triple<Color, Color, Color>,
    particles: List<Particle>,
    time: Float
) {
    val flameHeight = h * 0.72f
    val flameWidth = w * 0.55f

    // 1. Double pulsing heat aura
    val outerPulse = 1f + sin(time * 4f) * 0.12f
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

    // 2. High-speed rising glowing orange, gold, and pink sparks (55+ particles)
    for (i in 0 until 55) {
        val p = particles[i]
        val pProgress = ((time * (1.6f + p.speed * 0.8f) + p.phase) % (2f * PI.toFloat())) / (2f * PI.toFloat())

        val startY = cy + flameHeight * 0.40f
        val endY = cy - flameHeight * 0.85f
        val py = startY - pProgress * (startY - endY)

        val spread = flameWidth * 0.85f * (0.8f + pProgress * 0.5f)
        val px = cx + (p.initialXRatio - 0.5f) * spread + sin(pProgress * 8f + p.phase) * 14f

        val pAlpha = (sin(pProgress * PI.toFloat())).coerceIn(0f, 1f)

        val sparkColor = when {
            p.isPink -> Color(0xFFFB7185).copy(alpha = pAlpha)
            i % 4 == 0 -> Color(0xFFFDE047).copy(alpha = pAlpha)
            i % 2 == 0 -> Color(0xFFF97316).copy(alpha = pAlpha)
            else -> Color(0xFFFFFFFF).copy(alpha = pAlpha)
        }

        val sparkRadius = (2.2f + p.sizeRatio * 2.5f).dp.toPx()

        // Glow halo
        drawCircle(
            color = sparkColor.copy(alpha = pAlpha * 0.35f),
            radius = sparkRadius * 2.2f,
            center = Offset(px, py)
        )
        // Core spark
        drawCircle(
            color = sparkColor,
            radius = sparkRadius,
            center = Offset(px, py)
        )
    }
}
