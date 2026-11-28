package com.example.ui.components

import android.view.SoundEffectConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayPurple
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.util.HapticHelper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Frameless, enlarged, ultra-high-precision & silky-smooth 3D Quantum DNA Double Helix for HomeScreen:
 * - Zero box, zero border, zero circular frame (pure holographic radial aura)
 * - Zero per-frame recompositions: animation states read strictly inside graphicsLayer & DrawScope
 * - True 3D perspective foreshortening + pre-allocated contiguous Path ribbons per depth half-turn
 * - 15 precision hydrogen-bonded base-pair rungs with glowing junction nodes
 * - 30 depth-sorted 3D crystal nucleotide spheres + 12 depth-sorted orbiting quantum motes
 */
@Composable
fun DnaHeaderIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    isHapticEnabled: Boolean = true,
    iconSize: Dp = 148.dp
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) 1.13f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "dna_press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "dna_header_rot")
    // Continuous 0..2π rotation cycle
    val rotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dna_rot"
    )
    // Pure C-infinity sinusoidal breathing cycle (no RepeatMode.Reverse sharp turnaround)
    val breathPhaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dna_breath_phase"
    )

    // Pre-allocated Path buffers for contiguous depth-batched anti-aliased ribbons
    val strandBackPaths = remember { List(8) { Path() } }
    val strandFrontPaths = remember { List(8) { Path() } }

    val cyanAccent = if (isDarkTheme) NeonCyan else DayCyan
    val violetAccent = if (isDarkTheme) NeonViolet else DayPurple
    val emeraldAccent = NeonEmerald
    val magentaAccent = Color(0xFFEC4899)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(iconSize)
            .graphicsLayer {
                val breath = 1f + 0.032f * sin(breathPhaseState.value)
                val s = pressScaleState.value * breath
                scaleX = s
                scaleY = s
                // Gentle weightless holographic hover float
                translationY = -2.2.dp.toPx() * sin(breathPhaseState.value + 0.6f)
            }
            .semantics {
                role = Role.Button
                contentDescription = "DNA Track & Deviations"
            }
            .pointerInput(onClick, isHapticEnabled) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onClick()
                        }
                    }
                )
            }
            .testTag("dna_header_icon"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            val rotationRad = rotationState.value
            val breathPhase = breathPhaseState.value
            val auraPulse = 1f + 0.10f * sin(breathPhase)

            // 1. Frameless ethereal multi-stop holographic aura
            val auraRadius = minOf(w, h) * 0.485f
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to cyanAccent.copy(alpha = (if (isDarkTheme) 0.34f else 0.21f) * auraPulse),
                    0.38f to violetAccent.copy(alpha = (if (isDarkTheme) 0.19f else 0.12f) * auraPulse),
                    0.72f to emeraldAccent.copy(alpha = if (isDarkTheme) 0.08f else 0.04f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(cx, cy)
            )

            val radiusX = w * 0.245f
            val topY = h * 0.085f
            val botY = h * 0.915f
            val spanY = botY - topY
            val twistCycles = 2.85f * PI.toFloat()

            fun blendColor(a: Color, b: Color, t: Float): Color = Color(
                red = a.red + (b.red - a.red) * t,
                green = a.green + (b.green - a.green) * t,
                blue = a.blue + (b.blue - a.blue) * t,
                alpha = 1f
            )

            // True 3D perspective projection helper
            fun projectX(cosA: Float, sinA: Float, sign: Float): Float {
                val persp = 1f / (1f - 0.15f * sinA * sign)
                return cx + radiusX * sign * cosA * persp
            }

            // Build & draw contiguous back/front ribbon runs with zero micro-segment beading
            fun drawContinuousStrand(isStrandA: Boolean, frontOnly: Boolean) {
                val sign = if (isStrandA) 1f else -1f
                val topColor = if (isStrandA) cyanAccent else violetAccent
                val botColor = if (isStrandA) emeraldAccent else magentaAccent
                val samples = 160
                val pool = if (frontOnly) strandFrontPaths else strandBackPaths
                var runIndex = -1
                var inRun = false

                for (s in 0..samples) {
                    val t = s.toFloat() / samples
                    val y = topY + t * spanY
                    val phase = rotationRad + t * twistCycles
                    val sinA = sin(phase)
                    val cosA = cos(phase)
                    val z = sinA * sign
                    val x = projectX(cosA, sinA, sign)

                    val matchesPass = if (frontOnly) z >= -0.02f else z <= 0.02f
                    if (matchesPass) {
                        if (!inRun) {
                            runIndex++
                            if (runIndex < pool.size) {
                                pool[runIndex].reset()
                                pool[runIndex].moveTo(x, y)
                                inRun = true
                            }
                        } else if (runIndex < pool.size) {
                            pool[runIndex].lineTo(x, y)
                        }
                    } else {
                        inRun = false
                    }
                }

                val activeRuns = (runIndex + 1).coerceAtMost(pool.size)
                val baseBrush = Brush.verticalGradient(
                    colors = listOf(topColor, botColor),
                    startY = topY,
                    endY = botY
                )
                val alphaMul = if (frontOnly) 1f else 0.38f
                val coreStrokeW = w * (if (frontOnly) 0.024f else 0.014f)

                for (rIdx in 0 until activeRuns) {
                    val p = pool[rIdx]
                    // Soft bloom pass
                    drawPath(
                        path = p,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                topColor.copy(alpha = 0.28f * alphaMul),
                                botColor.copy(alpha = 0.28f * alphaMul)
                            ),
                            startY = topY,
                            endY = botY
                        ),
                        style = Stroke(
                            width = coreStrokeW * 2.5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    // Crisp inner neon ribbon
                    drawPath(
                        path = p,
                        brush = baseBrush,
                        alpha = alphaMul,
                        style = Stroke(
                            width = coreStrokeW,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    // Specular white crest highlight on foreground ribbon
                    if (frontOnly) {
                        drawPath(
                            path = p,
                            color = Color.White.copy(alpha = 0.44f),
                            style = Stroke(
                                width = w * 0.0055f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Orbiting motes behind helix (z <= 0)
            for (p in 0 until 12) {
                val pPhase = rotationRad * (if (p % 2 == 0) 1f else -1f) + p * 0.5236f
                val zOrb = sin(pPhase)
                if (zOrb <= 0f) {
                    val depth = (zOrb + 1f) * 0.5f
                    val px = cx + cos(pPhase) * (w * (0.29f + (p % 3) * 0.045f))
                    val py = cy + sin(pPhase * 0.8f + p * 0.7f) * (h * 0.37f)
                    val pAlpha = (0.18f + 0.38f * depth).coerceIn(0f, 0.65f)
                    val pColor = when (p % 3) {
                        0 -> cyanAccent
                        1 -> violetAccent
                        else -> emeraldAccent
                    }
                    val r = w * (0.005f + (p % 3) * 0.002f) * (0.7f + 0.4f * depth)
                    drawCircle(color = pColor.copy(alpha = pAlpha), radius = r, center = Offset(px, py))
                }
            }

            // Pass 1: Continuous back backbone ribbons
            drawContinuousStrand(isStrandA = true, frontOnly = false)
            drawContinuousStrand(isStrandA = false, frontOnly = false)

            // Pass 2: 15 precision hydrogen-bonded base-pair rungs
            val rungs = 15
            val rungBaseColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
            for (i in 0 until rungs) {
                val t = i.toFloat() / (rungs - 1)
                val y = topY + t * spanY
                val phase = rotationRad + t * twistCycles
                val sinA = sin(phase)
                val cosA = cos(phase)
                val x1 = projectX(cosA, sinA, 1f)
                val x2 = projectX(cosA, sinA, -1f)
                val cA = blendColor(cyanAccent, emeraldAccent, t)
                val cB = blendColor(violetAccent, magentaAccent, t)

                val rungAlpha = (0.28f + (1f - abs(sinA)) * 0.66f).coerceIn(0.18f, 0.95f)
                val startX = minOf(x1, x2)
                val endX = maxOf(x1, x2).coerceAtLeast(startX + 1f)
                val leftColor = if (x1 <= x2) cA else cB
                val rightColor = if (x1 <= x2) cB else cA

                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            leftColor.copy(alpha = rungAlpha * 0.26f),
                            rungBaseColor.copy(alpha = rungAlpha * 0.20f),
                            rightColor.copy(alpha = rungAlpha * 0.26f)
                        ),
                        startX = startX,
                        endX = endX
                    ),
                    start = Offset(x1, y),
                    end = Offset(x2, y),
                    strokeWidth = w * 0.022f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            leftColor.copy(alpha = rungAlpha * 0.92f),
                            rungBaseColor.copy(alpha = rungAlpha * 0.74f),
                            rightColor.copy(alpha = rungAlpha * 0.92f)
                        ),
                        startX = startX,
                        endX = endX
                    ),
                    start = Offset(x1, y),
                    end = Offset(x2, y),
                    strokeWidth = w * 0.010f,
                    cap = StrokeCap.Round
                )

                val spanX = abs(x1 - x2)
                if (spanX > w * 0.14f) {
                    val midX = (x1 + x2) * 0.5f
                    drawCircle(
                        color = Color.White.copy(alpha = rungAlpha * 0.72f),
                        radius = w * 0.008f,
                        center = Offset(midX, y)
                    )
                }
            }

            // Pass 3: Continuous front backbone ribbons cleanly crossing over back strands & rungs
            drawContinuousStrand(isStrandA = true, frontOnly = true)
            drawContinuousStrand(isStrandA = false, frontOnly = true)

            // Pass 4: 30 depth-sorted 3D crystal nucleotide spheres
            for (i in 0 until rungs) {
                val t = i.toFloat() / (rungs - 1)
                val y = topY + t * spanY
                val phase = rotationRad + t * twistCycles
                val sinA = sin(phase)
                val cosA = cos(phase)
                val x1 = projectX(cosA, sinA, 1f)
                val x2 = projectX(cosA, sinA, -1f)
                val z1 = sinA
                val z2 = -sinA
                val cA = blendColor(cyanAccent, emeraldAccent, t)
                val cB = blendColor(violetAccent, magentaAccent, t)

                val drawCrystalNode: (Float, Float, Color) -> Unit = { nx, nz, baseColor ->
                    val depthNorm = (nz + 1f) * 0.5f
                    val nodeR = w * (0.019f + depthNorm * 0.017f)
                    val nodeAlpha = 0.44f + depthNorm * 0.56f

                    drawCircle(
                        color = baseColor.copy(alpha = nodeAlpha * 0.34f),
                        radius = nodeR * 1.95f,
                        center = Offset(nx, y)
                    )
                    drawCircle(
                        color = Color(0xFF030712).copy(alpha = 0.35f * depthNorm),
                        radius = nodeR * 1.05f,
                        center = Offset(nx + nodeR * 0.10f, y + nodeR * 0.12f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to Color.White.copy(alpha = nodeAlpha * 0.92f),
                            0.36f to baseColor.copy(alpha = nodeAlpha),
                            1.0f to baseColor.copy(alpha = nodeAlpha * 0.70f),
                            center = Offset(nx - nodeR * 0.24f, y - nodeR * 0.24f),
                            radius = nodeR * 1.15f
                        ),
                        radius = nodeR,
                        center = Offset(nx, y)
                    )
                    if (depthNorm > 0.32f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.94f * depthNorm),
                            radius = nodeR * 0.28f,
                            center = Offset(nx - nodeR * 0.30f, y - nodeR * 0.30f)
                        )
                    }
                }

                if (z1 <= z2) {
                    drawCrystalNode(x1, z1, cA)
                    drawCrystalNode(x2, z2, cB)
                } else {
                    drawCrystalNode(x2, z2, cB)
                    drawCrystalNode(x1, z1, cA)
                }
            }

            // Pass 5: Foreground orbiting photonic quantum motes (z > 0)
            for (p in 0 until 12) {
                val pPhase = rotationRad * (if (p % 2 == 0) 1f else -1f) + p * 0.5236f
                val zOrb = sin(pPhase)
                if (zOrb > 0f) {
                    val depth = (zOrb + 1f) * 0.5f
                    val px = cx + cos(pPhase) * (w * (0.29f + (p % 3) * 0.045f))
                    val py = cy + sin(pPhase * 0.8f + p * 0.7f) * (h * 0.37f)
                    val pAlpha = (0.45f + 0.50f * depth).coerceIn(0f, 0.95f)
                    val pColor = when (p % 3) {
                        0 -> cyanAccent
                        1 -> violetAccent
                        else -> emeraldAccent
                    }
                    val r = w * (0.0055f + (p % 3) * 0.0022f) * (0.85f + 0.45f * depth)
                    drawCircle(
                        color = pColor.copy(alpha = pAlpha * 0.36f),
                        radius = r * 2.2f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = pAlpha * 0.82f),
                        radius = r * 0.62f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = pColor.copy(alpha = pAlpha),
                        radius = r,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
