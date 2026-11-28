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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
 * Frameless, enlarged, ultra-high-precision volumetric 3D animated Flame hero for HomeScreen:
 * - Zero box, zero border, zero circular frame (pure atmospheric radial bloom)
 * - 5-layer anatomical flame gradient (mantle -> outer body -> mid plasma -> incandescent core -> singularity)
 * - Razor-thin luminous rim filament, crystal/star flare detail, and 26 velocity-trailed micro-embers
 * - Supports 2-second hold detector to launch Streak Start Date Dialog
 */
@Composable
fun GlowingFlameHeaderIcon(
    flameScore: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onLongClick2s: (() -> Unit)? = null,
    iconSize: Dp = 148.dp
) {
    val stage = getFlameStage(flameScore)
    val colors = getFlameColors(flameScore)

    val infiniteTransition = rememberInfiniteTransition(label = "header_flame_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "header_time"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "header_pulse"
    )

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isHolding) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "flame_press_scale"
    )

    // 26 precision micro-embers with speed multiplier, horizontal jitter phase, and color flag
    val microSparks = remember {
        List(26) { i ->
            Triple(
                (i * 0.041f + 0.07f) % 1f,
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
            val cy = h * 0.55f

            val motion = if (stage == FlameVisualStage.FROZEN_STATIC) 0f else 1f
            val swayPrimary = sin(time * 2f) * (w * 0.036f) * motion
            val swaySecondary = cos(time * 3f) * (w * 0.022f) * motion
            val swayTertiary = sin(time * 4f + 1.1f) * (w * 0.014f) * motion

            // 1. Frameless multi-stop volumetric atmospheric aura (fades seamlessly to transparent before edge)
            val auraPrimary = when (stage) {
                FlameVisualStage.FROZEN_STATIC -> Color(0x5238BDF8)
                FlameVisualStage.ICE_CRACKING -> Color(0x5C00E5FF)
                FlameVisualStage.COLOR_GRADIENT -> colors.second.copy(alpha = (if (isDarkTheme) 0.44f else 0.30f) * pulseGlow)
                FlameVisualStage.FULLY_IGNITED -> Color(0xFFF97316).copy(alpha = (if (isDarkTheme) 0.48f else 0.32f) * pulseGlow)
                FlameVisualStage.SUPERNOVA_OVERLOAD -> Color(0xFFC084FC).copy(alpha = (if (isDarkTheme) 0.54f else 0.36f) * pulseGlow)
                FlameVisualStage.OBSIDIAN_PLASMA -> Color(0xFFE879F9).copy(alpha = (if (isDarkTheme) 0.58f else 0.38f) * pulseGlow)
            }
            val auraRadius = minOf(w, h) * 0.485f
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to auraPrimary,
                    0.38f to colors.second.copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
                    0.70f to colors.third.copy(alpha = if (isDarkTheme) 0.09f else 0.05f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy - h * 0.03f),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(cx, cy - h * 0.03f)
            )

            val baseY = cy + h * 0.31f
            val tipY = cy - h * 0.41f
            val flameHeight = baseY - tipY
            val flameWidth = w * 0.48f
            val tipX = cx + swayPrimary

            // Soft ground thermal reflection pool (no box or border)
            drawOval(
                brush = Brush.radialGradient(
                    0.0f to colors.first.copy(alpha = if (isDarkTheme) 0.34f else 0.22f),
                    0.55f to colors.second.copy(alpha = if (isDarkTheme) 0.18f else 0.11f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, baseY + h * 0.015f),
                    radius = flameWidth * 0.68f
                ),
                topLeft = Offset(cx - flameWidth * 0.56f, baseY - h * 0.035f),
                size = Size(flameWidth * 1.12f, h * 0.105f)
            )

            // 2. Outer translucent plasma veil
            val mantleScale = 1.13f + (pulseGlow - 1f) * 0.12f
            val mantleWidth = flameWidth * mantleScale
            val mantleTipY = tipY - flameHeight * 0.075f
            val mantleBaseY = baseY + flameHeight * 0.025f
            val mantlePath = Path().apply {
                moveTo(cx, mantleBaseY)
                cubicTo(
                    cx - mantleWidth * 0.65f, cy + flameHeight * 0.18f,
                    cx - mantleWidth * 0.46f, cy - flameHeight * 0.24f,
                    tipX - swaySecondary * 0.45f, mantleTipY
                )
                cubicTo(
                    cx + mantleWidth * 0.46f, cy - flameHeight * 0.24f,
                    cx + mantleWidth * 0.65f, cy + flameHeight * 0.18f,
                    cx, mantleBaseY
                )
                close()
            }
            drawPath(
                path = mantlePath,
                brush = Brush.verticalGradient(
                    0.0f to colors.first.copy(alpha = 0.22f),
                    0.48f to colors.second.copy(alpha = 0.26f),
                    1.0f to colors.third.copy(alpha = 0.12f),
                    startY = mantleTipY,
                    endY = mantleBaseY
                )
            )

            // 3. Sculpted multi-tongue high-precision flame silhouette
            val mainFlamePath = Path().apply {
                moveTo(cx, baseY)
                // Left lower curve into fine secondary left lick
                cubicTo(
                    cx - flameWidth * 0.58f, cy + flameHeight * 0.17f,
                    cx - flameWidth * 0.52f, cy - flameHeight * 0.06f,
                    cx - flameWidth * 0.28f + swaySecondary, cy - flameHeight * 0.24f
                )
                // Dip between left lick & tall central crest
                cubicTo(
                    cx - flameWidth * 0.17f, cy - flameHeight * 0.14f,
                    cx - flameWidth * 0.13f + swayTertiary, cy - flameHeight * 0.34f,
                    tipX, tipY
                )
                // Central crest down into fine right shoulder curl
                cubicTo(
                    cx + flameWidth * 0.18f + swayTertiary * 0.6f, cy - flameHeight * 0.30f,
                    cx + flameWidth * 0.26f - swaySecondary * 0.5f, cy - flameHeight * 0.16f,
                    cx + flameWidth * 0.34f - swaySecondary * 0.4f, cy - flameHeight * 0.20f
                )
                cubicTo(
                    cx + flameWidth * 0.54f, cy - flameHeight * 0.02f,
                    cx + flameWidth * 0.56f, cy + flameHeight * 0.18f,
                    cx, baseY
                )
                close()
            }
            drawPath(
                path = mainFlamePath,
                brush = Brush.verticalGradient(
                    0.0f to colors.first,
                    0.35f to colors.first.copy(alpha = 0.95f),
                    0.68f to colors.second,
                    1.0f to colors.third,
                    startY = tipY,
                    endY = baseY
                )
            )

            // Precision dual-pass luminous rim contour
            drawPath(
                path = mainFlamePath,
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.30f),
                    0.55f to colors.first.copy(alpha = 0.22f),
                    1.0f to Color.Transparent,
                    startY = tipY,
                    endY = baseY
                ),
                style = Stroke(width = w * 0.018f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = mainFlamePath,
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.72f),
                    0.45f to colors.first.copy(alpha = 0.45f),
                    1.0f to Color.Transparent,
                    startY = tipY,
                    endY = baseY
                ),
                style = Stroke(width = w * 0.007f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 4. Mid-plasma smooth tonal tongue
            val midTipY = cy - flameHeight * 0.28f
            val midBaseY = baseY - flameHeight * 0.03f
            val midWidth = flameWidth * 0.68f
            val midTipX = cx + swayPrimary * 0.70f
            val midPath = Path().apply {
                moveTo(cx, midBaseY)
                cubicTo(
                    cx - midWidth * 0.58f, cy + flameHeight * 0.13f,
                    cx - midWidth * 0.38f, cy - flameHeight * 0.12f,
                    midTipX, midTipY
                )
                cubicTo(
                    cx + midWidth * 0.38f, cy - flameHeight * 0.12f,
                    cx + midWidth * 0.58f, cy + flameHeight * 0.13f,
                    cx, midBaseY
                )
                close()
            }
            drawPath(
                path = midPath,
                brush = Brush.verticalGradient(
                    0.0f to colors.first.copy(alpha = 0.94f),
                    0.62f to colors.second.copy(alpha = 0.85f),
                    1.0f to colors.third.copy(alpha = 0.50f),
                    startY = midTipY,
                    endY = midBaseY
                )
            )

            // 5. Sharp incandescent inner heart tongue
            val innerTipY = cy - flameHeight * 0.16f
            val innerBaseY = baseY - flameHeight * 0.06f
            val innerWidth = flameWidth * 0.42f
            val innerTipX = cx + swayPrimary * 0.45f
            val innerPath = Path().apply {
                moveTo(cx, innerBaseY)
                cubicTo(
                    cx - innerWidth * 0.56f, cy + flameHeight * 0.09f,
                    cx - innerWidth * 0.28f, cy - flameHeight * 0.08f,
                    innerTipX, innerTipY
                )
                cubicTo(
                    cx + innerWidth * 0.28f, cy - flameHeight * 0.08f,
                    cx + innerWidth * 0.56f, cy + flameHeight * 0.09f,
                    cx, innerBaseY
                )
                close()
            }
            drawPath(
                path = innerPath,
                brush = Brush.verticalGradient(
                    colors = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
                        listOf(Color(0xFFF5D0FE), Color(0xFF22D3EE), Color(0xFF070410))
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.98f),
                            colors.first.copy(alpha = 0.94f),
                            colors.second.copy(alpha = 0.68f)
                        )
                    },
                    startY = innerTipY,
                    endY = innerBaseY
                )
            )

            // 6. High-precision core singularity / frost crystal
            val coreCenter = Offset(innerTipX, cy + flameHeight * 0.07f)
            if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
                drawCircle(
                    color = Color(0xFF22D3EE).copy(alpha = 0.35f),
                    radius = w * 0.082f,
                    center = coreCenter
                )
                drawCircle(
                    color = Color(0xFF05030B),
                    radius = w * 0.060f,
                    center = coreCenter
                )
                drawCircle(
                    color = Color(0xFF22D3EE).copy(alpha = 0.95f),
                    radius = w * 0.060f,
                    center = coreCenter,
                    style = Stroke(width = w * 0.008f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = w * 0.016f,
                    center = Offset(coreCenter.x - w * 0.018f, coreCenter.y - w * 0.018f)
                )
            } else if (stage == FlameVisualStage.FROZEN_STATIC) {
                // Crisp 6-pointed frost star crystal inside frozen flame
                val starR = w * 0.056f
                for (a in 0 until 3) {
                    val ang = a * (PI.toFloat() / 3f)
                    val dx = cos(ang) * starR
                    val dy = sin(ang) * starR
                    drawLine(
                        color = Color.White.copy(alpha = 0.95f),
                        start = Offset(coreCenter.x - dx, coreCenter.y - dy),
                        end = Offset(coreCenter.x + dx, coreCenter.y + dy),
                        strokeWidth = w * 0.009f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    color = Color(0xFFE0F2FE),
                    radius = w * 0.022f,
                    center = coreCenter
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.98f),
                        0.45f to Color.White.copy(alpha = 0.55f),
                        1.0f to Color.Transparent,
                        center = coreCenter,
                        radius = w * 0.095f
                    ),
                    radius = w * 0.095f,
                    center = coreCenter
                )
                // Delicate 4-point hot gleam
                val gleamR = w * 0.060f
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                        startX = coreCenter.x - gleamR,
                        endX = coreCenter.x + gleamR
                    ),
                    start = Offset(coreCenter.x - gleamR, coreCenter.y),
                    end = Offset(coreCenter.x + gleamR, coreCenter.y),
                    strokeWidth = w * 0.007f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                        startY = coreCenter.y - gleamR * 1.25f,
                        endY = coreCenter.y + gleamR * 1.25f
                    ),
                    start = Offset(coreCenter.x, coreCenter.y - gleamR * 1.25f),
                    end = Offset(coreCenter.x, coreCenter.y + gleamR * 1.25f),
                    strokeWidth = w * 0.007f,
                    cap = StrokeCap.Round
                )
            }

            // 7. 26 ultra-fine velocity-trailed rising embers & sparks
            if (stage != FlameVisualStage.FROZEN_STATIC) {
                for ((idx, spark) in microSparks.withIndex()) {
                    val (xRatio, speed, isPink) = spark
                    val sparkProgress = ((time * speed + idx * 0.29f) % (2f * PI.toFloat())) / (2f * PI.toFloat())

                    val startY = baseY - flameHeight * 0.08f
                    val endY = tipY - h * 0.13f
                    val py = startY - sparkProgress * (startY - endY)
                    val px = cx + (xRatio - 0.5f) * flameWidth * 1.18f + sin(sparkProgress * 6.5f + idx) * (w * 0.036f)
                    val prevPy = (py + h * 0.022f).coerceAtMost(startY)
                    val prevPx = px - cos(sparkProgress * 6.5f + idx) * (w * 0.008f)

                    val alpha = (sin(sparkProgress * PI.toFloat())).coerceIn(0f, 1f)
                    val baseSparkColor = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
                        if (isPink) Color(0xFFE879F9) else Color(0xFF22D3EE)
                    } else if (isPink) {
                        Color(0xFFFB7185)
                    } else {
                        Color(0xFFF97316)
                    }

                    val dotRadius = w * (0.0055f + (idx % 4) * 0.0028f)
                    // Micro trail streak
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                baseSparkColor.copy(alpha = alpha * 0.75f),
                                Color.Transparent
                            ),
                            startY = py,
                            endY = prevPy
                        ),
                        start = Offset(px, py),
                        end = Offset(prevPx, prevPy),
                        strokeWidth = dotRadius * 1.2f,
                        cap = StrokeCap.Round
                    )
                    // Soft ember bloom
                    drawCircle(
                        color = baseSparkColor.copy(alpha = alpha * 0.34f),
                        radius = dotRadius * 2.4f,
                        center = Offset(px, py)
                    )
                    // Razor-sharp bright ember core
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.72f),
                        radius = dotRadius * 0.65f,
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
