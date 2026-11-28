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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
 * Frameless, enlarged, ultra-high-precision 3D Quantum DNA Double Helix hero for HomeScreen:
 * - Zero box, zero border, zero circular frame (pure holographic radial aura)
 * - True depth-interwoven 120-segment backbone ribbons (back strand -> rungs -> front strand + specular filament)
 * - 13 precision hydrogen-bonded base-pair rungs with dual-tone gradients & center bond notch
 * - 26 depth-sorted 3D specular crystal spheres + 10 orbiting photonic motes
 */
@Composable
fun DnaHeaderIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    isHapticEnabled: Boolean = true,
    iconSize: Dp = 124.dp
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "dna_press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "dna_header_rot")
    val rotationRad by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dna_rot"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dna_pulse"
    )

    val cyanAccent = if (isDarkTheme) NeonCyan else DayCyan
    val violetAccent = if (isDarkTheme) NeonViolet else DayPurple
    val emeraldAccent = NeonEmerald
    val magentaAccent = Color(0xFFEC4899)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(iconSize)
            .scale(pulse * pressScale)
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

            // 1. Frameless ethereal multi-stop holographic aura
            val auraRadius = minOf(w, h) * 0.485f
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to cyanAccent.copy(alpha = if (isDarkTheme) 0.34f else 0.21f),
                    0.38f to violetAccent.copy(alpha = if (isDarkTheme) 0.19f else 0.12f),
                    0.72f to emeraldAccent.copy(alpha = if (isDarkTheme) 0.08f else 0.04f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(cx, cy)
            )

            val radiusX = w * 0.25f
            val topY = h * 0.09f
            val botY = h * 0.91f
            val spanY = botY - topY
            val twistCycles = 2.75f * PI.toFloat()

            // Helper to blend colors smoothly along vertical progress t in [0..1]
            fun strandAColor(t: Float): Color {
                val r = cyanAccent.red + (emeraldAccent.red - cyanAccent.red) * t
                val g = cyanAccent.green + (emeraldAccent.green - cyanAccent.green) * t
                val b = cyanAccent.blue + (emeraldAccent.blue - cyanAccent.blue) * t
                return Color(r, g, b, 1f)
            }

            fun strandBColor(t: Float): Color {
                val r = violetAccent.red + (magentaAccent.red - violetAccent.red) * t
                val g = violetAccent.green + (magentaAccent.green - violetAccent.green) * t
                val b = violetAccent.blue + (magentaAccent.blue - violetAccent.blue) * t
                return Color(r, g, b, 1f)
            }

            // 2. High-density 120-segment parametric curves for depth-sorted ribbon drawing
            val samples = 120
            fun drawRibbonPass(frontOnly: Boolean) {
                for (s in 0 until samples) {
                    val t0 = s.toFloat() / samples
                    val t1 = (s + 1).toFloat() / samples
                    val y0 = topY + t0 * spanY
                    val y1 = topY + t1 * spanY
                    val p0 = rotationRad + t0 * twistCycles
                    val p1 = rotationRad + t1 * twistCycles
                    val pMid = (p0 + p1) * 0.5f

                    val zA = sin(pMid)
                    val zB = -zA

                    // Strand A segment
                    if ((frontOnly && zA > 0f) || (!frontOnly && zA <= 0f)) {
                        val xA0 = cx + radiusX * cos(p0)
                        val xA1 = cx + radiusX * cos(p1)
                        val depth = (zA + 1f) * 0.5f
                        val cA = strandAColor((t0 + t1) * 0.5f)
                        val coreAlpha = 0.36f + depth * 0.64f
                        val coreW = w * (0.014f + depth * 0.015f)
                        // Outer atmospheric ribbon bloom
                        drawLine(
                            color = cA.copy(alpha = coreAlpha * 0.30f),
                            start = Offset(xA0, y0),
                            end = Offset(xA1, y1),
                            strokeWidth = coreW * 2.4f,
                            cap = StrokeCap.Round
                        )
                        // Sharp ribbon core
                        drawLine(
                            color = cA.copy(alpha = coreAlpha),
                            start = Offset(xA0, y0),
                            end = Offset(xA1, y1),
                            strokeWidth = coreW,
                            cap = StrokeCap.Round
                        )
                        // Crisp specular filament along front ribbon crest
                        if (frontOnly && depth > 0.55f) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.52f * depth),
                                start = Offset(xA0, y0),
                                end = Offset(xA1, y1),
                                strokeWidth = w * 0.006f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Strand B segment
                    if ((frontOnly && zB > 0f) || (!frontOnly && zB <= 0f)) {
                        val xB0 = cx - radiusX * cos(p0)
                        val xB1 = cx - radiusX * cos(p1)
                        val depth = (zB + 1f) * 0.5f
                        val cB = strandBColor((t0 + t1) * 0.5f)
                        val coreAlpha = 0.36f + depth * 0.64f
                        val coreW = w * (0.014f + depth * 0.015f)
                        drawLine(
                            color = cB.copy(alpha = coreAlpha * 0.30f),
                            start = Offset(xB0, y0),
                            end = Offset(xB1, y1),
                            strokeWidth = coreW * 2.4f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = cB.copy(alpha = coreAlpha),
                            start = Offset(xB0, y0),
                            end = Offset(xB1, y1),
                            strokeWidth = coreW,
                            cap = StrokeCap.Round
                        )
                        if (frontOnly && depth > 0.55f) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.52f * depth),
                                start = Offset(xB0, y0),
                                end = Offset(xB1, y1),
                                strokeWidth = w * 0.006f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // Pass 1: Back backbone ribbons
            drawRibbonPass(frontOnly = false)

            // Pass 2: 13 precision hydrogen-bonded base-pair rungs
            val rungs = 13
            val rungBaseColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
            for (i in 0 until rungs) {
                val t = i.toFloat() / (rungs - 1)
                val y = topY + t * spanY
                val phase = rotationRad + t * twistCycles
                val sinA = sin(phase)
                val cosA = cos(phase)
                val x1 = cx + radiusX * cosA
                val x2 = cx - radiusX * cosA
                val cA = strandAColor(t)
                val cB = strandBColor(t)

                val rungAlpha = (0.30f + (1f - abs(sinA)) * 0.62f).coerceIn(0.18f, 0.94f)
                val startX = minOf(x1, x2)
                val endX = maxOf(x1, x2).coerceAtLeast(startX + 1f)
                val leftColor = if (x1 <= x2) cA else cB
                val rightColor = if (x1 <= x2) cB else cA

                // Outer glow bridge
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            leftColor.copy(alpha = rungAlpha * 0.28f),
                            rungBaseColor.copy(alpha = rungAlpha * 0.22f),
                            rightColor.copy(alpha = rungAlpha * 0.28f)
                        ),
                        startX = startX,
                        endX = endX
                    ),
                    start = Offset(x1, y),
                    end = Offset(x2, y),
                    strokeWidth = w * 0.024f,
                    cap = StrokeCap.Round
                )

                // Razor-crisp core rung
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            leftColor.copy(alpha = rungAlpha * 0.90f),
                            rungBaseColor.copy(alpha = rungAlpha * 0.72f),
                            rightColor.copy(alpha = rungAlpha * 0.90f)
                        ),
                        startX = startX,
                        endX = endX
                    ),
                    start = Offset(x1, y),
                    end = Offset(x2, y),
                    strokeWidth = w * 0.011f,
                    cap = StrokeCap.Round
                )

                // Micro hydrogen-bond junction node in the middle when wide enough
                if (abs(x1 - x2) > w * 0.14f) {
                    drawCircle(
                        color = Color.White.copy(alpha = rungAlpha * 0.68f),
                        radius = w * 0.009f,
                        center = Offset(cx, y)
                    )
                }
            }

            // Pass 3: Front backbone ribbons crossing cleanly over back strands & rungs
            drawRibbonPass(frontOnly = true)

            // Pass 4: 26 depth-sorted 3D crystal nucleotide spheres
            for (i in 0 until rungs) {
                val t = i.toFloat() / (rungs - 1)
                val y = topY + t * spanY
                val phase = rotationRad + t * twistCycles
                val sinA = sin(phase)
                val cosA = cos(phase)
                val x1 = cx + radiusX * cosA
                val x2 = cx - radiusX * cosA
                val z1 = sinA
                val z2 = -sinA
                val cA = strandAColor(t)
                val cB = strandBColor(t)

                val drawCrystalNode: (Float, Float, Color) -> Unit = { nx, nz, baseColor ->
                    val depthNorm = (nz + 1f) * 0.5f
                    val nodeR = w * (0.021f + depthNorm * 0.018f)
                    val nodeAlpha = 0.45f + depthNorm * 0.55f

                    // Soft neon glow aura
                    drawCircle(
                        color = baseColor.copy(alpha = nodeAlpha * 0.34f),
                        radius = nodeR * 2.0f,
                        center = Offset(nx, y)
                    )
                    // Subtle dark shadow edge for spherical depth
                    drawCircle(
                        color = Color(0xFF030712).copy(alpha = 0.35f * depthNorm),
                        radius = nodeR * 1.05f,
                        center = Offset(nx + nodeR * 0.10f, y + nodeR * 0.12f)
                    )
                    // 3D radial sphere fill
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to Color.White.copy(alpha = nodeAlpha * 0.90f),
                            0.38f to baseColor.copy(alpha = nodeAlpha),
                            1.0f to baseColor.copy(alpha = nodeAlpha * 0.72f),
                            center = Offset(nx - nodeR * 0.24f, y - nodeR * 0.24f),
                            radius = nodeR * 1.15f
                        ),
                        radius = nodeR,
                        center = Offset(nx, y)
                    )
                    // Crisp specular top-left pinpoint highlight
                    if (depthNorm > 0.35f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.92f * depthNorm),
                            radius = nodeR * 0.30f,
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

            // 5. 10 ultra-fine orbiting photonic quantum motes
            for (p in 0 until 10) {
                val pPhase = rotationRad * (if (p % 2 == 0) 1f else -1f) + p * 0.628f
                val px = cx + cos(pPhase) * (w * (0.30f + (p % 3) * 0.045f))
                val py = cy + sin(pPhase * 0.8f + p * 0.7f) * (h * 0.37f)
                val pAlpha = (0.30f + 0.55f * abs(sin(pPhase))).coerceIn(0f, 0.90f)
                val pColor = when (p % 3) {
                    0 -> cyanAccent
                    1 -> violetAccent
                    else -> emeraldAccent
                }
                val r = w * (0.006f + (p % 3) * 0.0025f)
                drawCircle(
                    color = pColor.copy(alpha = pAlpha * 0.35f),
                    radius = r * 2.2f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White.copy(alpha = pAlpha * 0.80f),
                    radius = r * 0.65f,
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
