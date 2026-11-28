package com.example.ui.components

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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Frameless, enlarged luxury 3D animated Flame hero icon for HomeScreen:
 * - Zero box, zero border, zero circular frame (pure atmospheric volumetric bloom)
 * - Sculpted multi-layer plasma mantle + luminous inner tongue + hot core singularity
 * - Rising orange/magenta/cyan embers + smooth continuous breathing & sway
 * - Supports 2-second hold detector to launch Streak Start Date Dialog
 */
@Composable
fun GlowingFlameHeaderIcon(
    flameScore: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onLongClick2s: (() -> Unit)? = null,
    iconSize: Dp = 86.dp
) {
    val stage = getFlameStage(flameScore)
    val colors = getFlameColors(flameScore)

    val infiniteTransition = rememberInfiniteTransition(label = "header_flame_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "header_time"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "header_pulse"
    )

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isHolding) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "flame_press_scale"
    )

    // Rising embers precomputed with integer speed multipliers for seamless infinite loop
    val microSparks = remember {
        List(18) { i ->
            Triple(
                (i * 0.059f) % 1f,
                1 + (i % 3),
                i % 2 == 0
            )
        }
    }

    val gestureModifier = Modifier.pointerInput(onLongClick2s, onClick) {
        detectTapGestures(
            onPress = {
                isHolding = true
                var triggered = false
                val job = if (onLongClick2s != null) {
                    coroutineScope.launch {
                        delay(2000L)
                        triggered = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick2s.invoke()
                    }
                } else null
                val released = tryAwaitRelease()
                isHolding = false
                job?.cancel()
                if (released && !triggered) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            }
        )
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(iconSize)
            .scale(pressScale)
            .semantics {
                role = Role.Button
                contentDescription = "Flame System"
            }
            .then(gestureModifier)
            .testTag("header_flame_btn"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.54f

            val motion = if (stage == FlameVisualStage.FROZEN_STATIC) 0f else 1f
            val swayPrimary = sin(time * 2f) * (w * 0.042f) * motion
            val swaySecondary = cos(time * 3f) * (w * 0.024f) * motion

            // 1. Frameless volumetric atmospheric aura (fades cleanly to transparent before edges)
            val auraPrimary = when (stage) {
                FlameVisualStage.FROZEN_STATIC -> Color(0x4D38BDF8)
                FlameVisualStage.ICE_CRACKING -> Color(0x5500E5FF)
                FlameVisualStage.COLOR_GRADIENT -> colors.second.copy(alpha = (if (isDarkTheme) 0.42f else 0.30f) * pulseGlow)
                FlameVisualStage.FULLY_IGNITED -> Color(0xFFF97316).copy(alpha = (if (isDarkTheme) 0.46f else 0.32f) * pulseGlow)
                FlameVisualStage.SUPERNOVA_OVERLOAD -> Color(0xFFC084FC).copy(alpha = (if (isDarkTheme) 0.52f else 0.36f) * pulseGlow)
                FlameVisualStage.OBSIDIAN_PLASMA -> Color(0xFFE879F9).copy(alpha = (if (isDarkTheme) 0.56f else 0.38f) * pulseGlow)
            }
            val auraRadius = minOf(w, h) * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to auraPrimary,
                    0.48f to colors.third.copy(alpha = if (isDarkTheme) 0.16f else 0.10f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(cx, cy)
            )

            // Subtle heat base pool shimmer (no hard border)
            val baseY = cy + h * 0.30f
            val tipY = cy - h * 0.38f
            val flameHeight = baseY - tipY
            val flameWidth = w * 0.50f
            val tipX = cx + swayPrimary

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.second.copy(alpha = if (isDarkTheme) 0.36f else 0.24f),
                        Color.Transparent
                    ),
                    center = Offset(cx, baseY + h * 0.01f),
                    radius = flameWidth * 0.65f
                ),
                topLeft = Offset(cx - flameWidth * 0.52f, baseY - h * 0.04f),
                size = androidx.compose.ui.geometry.Size(flameWidth * 1.04f, h * 0.11f)
            )

            // 2. Outer translucent plasma mantle
            val mantleScale = 1.12f + (pulseGlow - 1f) * 0.14f
            val mantleWidth = flameWidth * mantleScale
            val mantleTipY = tipY - flameHeight * 0.08f
            val mantleBaseY = baseY + flameHeight * 0.03f
            val mantlePath = Path().apply {
                moveTo(cx, mantleBaseY)
                cubicTo(
                    cx - mantleWidth * 0.64f, cy + flameHeight * 0.16f,
                    cx - mantleWidth * 0.44f, cy - flameHeight * 0.24f,
                    tipX - swaySecondary * 0.5f, mantleTipY
                )
                cubicTo(
                    cx + mantleWidth * 0.44f, cy - flameHeight * 0.24f,
                    cx + mantleWidth * 0.64f, cy + flameHeight * 0.16f,
                    cx, mantleBaseY
                )
                close()
            }
            drawPath(
                path = mantlePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.first.copy(alpha = 0.22f),
                        colors.second.copy(alpha = 0.28f),
                        colors.third.copy(alpha = 0.14f)
                    ),
                    startY = mantleTipY,
                    endY = mantleBaseY
                )
            )

            // 3. Sculpted asymmetric main flame body
            val mainFlamePath = Path().apply {
                moveTo(cx, baseY)
                cubicTo(
                    cx - flameWidth * 0.60f, cy + flameHeight * 0.16f,
                    cx - flameWidth * 0.46f, cy - flameHeight * 0.12f,
                    cx - flameWidth * 0.24f + swaySecondary, cy - flameHeight * 0.26f
                )
                cubicTo(
                    cx - flameWidth * 0.14f, cy - flameHeight * 0.14f,
                    cx - flameWidth * 0.08f, cy - flameHeight * 0.34f,
                    tipX, tipY
                )
                cubicTo(
                    cx + flameWidth * 0.20f, cy - flameHeight * 0.28f,
                    cx + flameWidth * 0.44f - swaySecondary * 0.6f, cy - flameHeight * 0.06f,
                    cx + flameWidth * 0.56f, cy + flameHeight * 0.16f
                )
                cubicTo(
                    cx + flameWidth * 0.42f, baseY - flameHeight * 0.02f,
                    cx + flameWidth * 0.18f, baseY + flameHeight * 0.02f,
                    cx, baseY
                )
                close()
            }
            drawPath(
                path = mainFlamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(colors.first, colors.second, colors.third),
                    startY = tipY,
                    endY = baseY
                )
            )

            // Delicate luminous rim highlight along flame edge
            drawPath(
                path = mainFlamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        colors.first.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    startY = tipY,
                    endY = baseY
                ),
                style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
            )

            // 4. Inner bright luminous heart tongue
            val innerTipY = cy - flameHeight * 0.20f
            val innerBaseY = baseY - flameHeight * 0.05f
            val innerWidth = flameWidth * 0.46f
            val innerTipX = cx + swayPrimary * 0.55f
            val innerPath = Path().apply {
                moveTo(cx, innerBaseY)
                cubicTo(
                    cx - innerWidth * 0.58f, cy + flameHeight * 0.10f,
                    cx - innerWidth * 0.32f, cy - flameHeight * 0.12f,
                    innerTipX, innerTipY
                )
                cubicTo(
                    cx + innerWidth * 0.32f, cy - flameHeight * 0.12f,
                    cx + innerWidth * 0.58f, cy + flameHeight * 0.10f,
                    cx, innerBaseY
                )
                close()
            }
            drawPath(
                path = innerPath,
                brush = Brush.verticalGradient(
                    colors = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
                        listOf(Color(0xFFE879F9), Color(0xFF22D3EE), Color(0xFF090612))
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.96f),
                            colors.first.copy(alpha = 0.92f),
                            colors.second.copy(alpha = 0.72f)
                        )
                    },
                    startY = innerTipY,
                    endY = innerBaseY
                )
            )

            // 5. Inner Core Singularity / Frost Crystal
            val coreCenter = Offset(innerTipX, cy + flameHeight * 0.06f)
            if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
                drawCircle(
                    color = Color(0xFF06040D),
                    radius = w * 0.068f,
                    center = coreCenter
                )
                drawCircle(
                    color = Color(0xFF22D3EE).copy(alpha = 0.92f),
                    radius = w * 0.036f,
                    center = coreCenter
                )
            } else if (stage == FlameVisualStage.FROZEN_STATIC) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = w * 0.050f,
                    center = coreCenter
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.95f), Color.Transparent),
                        center = coreCenter,
                        radius = w * 0.10f
                    ),
                    radius = w * 0.10f,
                    center = coreCenter
                )
            }

            // 6. Floating luminous sparks & embers
            if (stage != FlameVisualStage.FROZEN_STATIC) {
                for ((idx, spark) in microSparks.withIndex()) {
                    val (xRatio, speed, isPink) = spark
                    val sparkProgress = ((time * speed + idx * 0.39f) % (2f * PI.toFloat())) / (2f * PI.toFloat())

                    val startY = baseY - flameHeight * 0.06f
                    val endY = tipY - h * 0.12f
                    val py = startY - sparkProgress * (startY - endY)
                    val px = cx + (xRatio - 0.5f) * flameWidth * 1.15f + sin(sparkProgress * 6f + idx) * (w * 0.045f)

                    val alpha = (sin(sparkProgress * PI.toFloat())).coerceIn(0f, 1f)
                    val baseSparkColor = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
                        if (isPink) Color(0xFFE879F9) else Color(0xFF22D3EE)
                    } else if (isPink) {
                        Color(0xFFFB7185)
                    } else {
                        Color(0xFFF97316)
                    }
                    val dotRadius = w * (0.015f + (idx % 3) * 0.007f)
                    drawCircle(
                        color = baseSparkColor.copy(alpha = alpha * 0.35f),
                        radius = dotRadius * 2.2f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = baseSparkColor.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
