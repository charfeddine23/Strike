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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Frameless, enlarged luxury 3D floating DNA Double Helix hero icon for HomeScreen:
 * - Zero box, zero border, zero circular frame (pure holographic volumetric aura)
 * - Smooth continuous 3D backbone ribbons, depth-sorted specular crystal nodes & glowing rungs
 * - Orbiting quantum shimmer particles and tactile bounce press navigating to DeviationsScreen
 */
@Composable
fun DnaHeaderIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    isHapticEnabled: Boolean = true,
    iconSize: Dp = 86.dp
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "dna_press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "dna_header_rot")
    val rotationRad by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dna_rot"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dna_pulse"
    )

    val cyanAccent = if (isDarkTheme) NeonCyan else DayCyan
    val violetAccent = if (isDarkTheme) NeonViolet else DayPurple
    val emeraldAccent = NeonEmerald

    val scope = rememberCoroutineScope()

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

            // 1. Frameless ethereal holographic aura (fades softly to transparent before bounds)
            val auraRadius = minOf(w, h) * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to cyanAccent.copy(alpha = if (isDarkTheme) 0.34f else 0.22f),
                    0.46f to violetAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.11f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(cx, cy)
            )

            val radiusX = w * 0.26f
            val topY = h * 0.11f
            val botY = h * 0.89f
            val spanY = botY - topY

            // Smooth continuous backbone ribbons
            val spineSamples = 36
            val strandAPath = Path()
            val strandBPath = Path()
            for (s in 0..spineSamples) {
                val t = s.toFloat() / spineSamples
                val y = topY + t * spanY
                val phase = rotationRad + t * (2.6f * PI.toFloat())
                val cosA = cos(phase)
                val xA = cx + radiusX * cosA
                val xB = cx - radiusX * cosA
                if (s == 0) {
                    strandAPath.moveTo(xA, y)
                    strandBPath.moveTo(xB, y)
                } else {
                    strandAPath.lineTo(xA, y)
                    strandBPath.lineTo(xB, y)
                }
            }

            // Outer volumetric ribbon bloom
            drawPath(
                path = strandAPath,
                brush = Brush.verticalGradient(listOf(cyanAccent.copy(alpha = 0.32f), emeraldAccent.copy(alpha = 0.28f))),
                style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
            )
            drawPath(
                path = strandBPath,
                brush = Brush.verticalGradient(listOf(violetAccent.copy(alpha = 0.32f), Color(0xFFEC4899).copy(alpha = 0.28f))),
                style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
            )

            // Crisp neon inner strands
            drawPath(
                path = strandAPath,
                brush = Brush.verticalGradient(listOf(cyanAccent, emeraldAccent)),
                style = Stroke(width = w * 0.028f, cap = StrokeCap.Round)
            )
            drawPath(
                path = strandBPath,
                brush = Brush.verticalGradient(listOf(violetAccent, Color(0xFFEC4899))),
                style = Stroke(width = w * 0.028f, cap = StrokeCap.Round)
            )

            // 2. Rungs and depth-sorted 3D crystal orbs
            val rungs = 9
            val rungBaseColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
            for (i in 0 until rungs) {
                val t = i.toFloat() / (rungs - 1)
                val y = topY + t * spanY
                val phase = rotationRad + t * (2.6f * PI.toFloat())

                val sinA = sin(phase)
                val cosA = cos(phase)

                val x1 = cx + radiusX * cosA
                val x2 = cx - radiusX * cosA
                val z1 = sinA
                val z2 = -sinA

                // Rung connector with cyan-to-violet luminous bridge
                val rungAlpha = (0.34f + (1f - abs(sinA)) * 0.56f).coerceIn(0.20f, 0.92f)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            cyanAccent.copy(alpha = rungAlpha * 0.85f),
                            rungBaseColor.copy(alpha = rungAlpha * 0.65f),
                            violetAccent.copy(alpha = rungAlpha * 0.85f)
                        ),
                        startX = minOf(x1, x2),
                        endX = maxOf(x1, x2).coerceAtLeast(minOf(x1, x2) + 1f)
                    ),
                    start = Offset(x1, y),
                    end = Offset(x2, y),
                    strokeWidth = w * 0.020f,
                    cap = StrokeCap.Round
                )

                // Depth-sorted 3D nodes (draw back node first, front node second)
                val drawNode: (Float, Float, Color) -> Unit = { nx, nz, baseColor ->
                    val depthNorm = (nz + 1f) * 0.5f // 0 = far, 1 = near
                    val nodeR = w * (0.028f + depthNorm * 0.022f)
                    val nodeAlpha = 0.42f + depthNorm * 0.58f

                    // Soft glow halo
                    drawCircle(
                        color = baseColor.copy(alpha = nodeAlpha * 0.35f),
                        radius = nodeR * 1.95f,
                        center = Offset(nx, y)
                    )
                    // Specular orb core
                    drawCircle(
                        color = baseColor.copy(alpha = nodeAlpha),
                        radius = nodeR,
                        center = Offset(nx, y)
                    )
                    // Crisp top-left highlight glint on foreground orbs
                    if (depthNorm > 0.45f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f * depthNorm),
                            radius = nodeR * 0.36f,
                            center = Offset(nx - nodeR * 0.28f, y - nodeR * 0.28f)
                        )
                    }
                }

                if (z1 <= z2) {
                    drawNode(x1, z1, cyanAccent)
                    drawNode(x2, z2, violetAccent)
                } else {
                    drawNode(x2, z2, violetAccent)
                    drawNode(x1, z1, cyanAccent)
                }
            }

            // 3. Subtle orbiting photonic sparkles around the helix
            for (p in 0 until 6) {
                val pPhase = rotationRad * (if (p % 2 == 0) 1f else -1f) + p * 1.05f
                val px = cx + cos(pPhase) * (w * (0.31f + (p % 2) * 0.06f))
                val py = cy + sin(pPhase * 0.7f + p) * (h * 0.34f)
                val pAlpha = (0.35f + 0.45f * abs(sin(pPhase))).coerceIn(0f, 0.85f)
                val pColor = if (p % 2 == 0) cyanAccent else violetAccent
                drawCircle(
                    color = pColor.copy(alpha = pAlpha),
                    radius = w * 0.014f,
                    center = Offset(px, py)
                )
            }
        }
    }
}
