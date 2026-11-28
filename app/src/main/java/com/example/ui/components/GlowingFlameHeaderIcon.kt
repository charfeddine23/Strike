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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
 * Frameless, enlarged, ultra-high-precision & silky-smooth volumetric 3D Flame hero for HomeScreen:
 * - Zero box, zero border, zero circular frame (pure atmospheric radial bloom)
 * - Zero per-frame recompositions: all animation phases read strictly inside graphicsLayer & DrawScope
 * - Zero per-frame Path allocations: pre-allocated reusable Paths for stutter-free 60/120fps motion
 * - Pure C-infinity sinusoidal harmonics & travelling vertical phase wave (eliminates triangle-wave jerks)
 * - 5-layer anatomical plasma rendering + 32 velocity-trailed micro-embers
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
    // Continuous 0..2π phase cycle (no RepeatMode.Reverse sharp velocity reversals)
    val phaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flame_phase"
    )
    // Secondary incommensurate ember/shimmer cycle for natural organic non-repetitive flame life
    val emberPhaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flame_ember_phase"
    )

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }

    val pressScaleState = animateFloatAsState(
        targetValue = if (isHolding) 1.13f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "flame_press_scale"
    )

    // Pre-allocated Path objects to prevent per-frame GC allocation stutter
    val mantlePath = remember { Path() }
    val mainFlamePath = remember { Path() }
    val midPath = remember { Path() }
    val innerPath = remember { Path() }

    // 32 precision rising embers with precomputed speed, horizontal anchor, and turbulence phase
    val microSparks = remember {
        List(32) { i ->
            val xRatio = (i * 0.033f + 0.05f) % 1f
            val speed = 1 + (i % 3)
            val isAltColor = i % 2 == 0
            val phaseOffset = i * 0.37f
            floatArrayOf(xRatio, speed.toFloat(), if (isAltColor) 1f else 0f, phaseOffset)
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
            .graphicsLayer {
                val breath = if (stage == FlameVisualStage.FROZEN_STATIC) {
                    1f + 0.014f * sin(phaseState.value)
                } else {
                    1f + 0.024f * sin(phaseState.value * 2f)
                }
                val s = pressScaleState.value * breath
                scaleX = s
                scaleY = s
                if (stage != FlameVisualStage.FROZEN_STATIC) {
                    translationY = -1.6.dp.toPx() * sin(phaseState.value)
                }
            }
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

            val t = phaseState.value
            val tEmber = emberPhaseState.value
            val motion = if (stage == FlameVisualStage.FROZEN_STATIC) 0f else 1f

            // Pure smooth sinusoidal breathing (zero velocity discontinuities)
            val pulseGlow = 1f + 0.12f * sin(t * 2f) + 0.04f * cos(t * 3f)

            // Travelling fluid wave along flame height (lower belly leads, mid twists, tip follows smoothly)
            val bellyWave = sin(t * 2f) * (w * 0.015f) * motion
            val midWave = (sin(t * 2f - 0.75f) * 0.70f + cos(t * 3f - 0.4f) * 0.30f) * (w * 0.024f) * motion
            val tipWave = (sin(t * 2f - 1.50f) * 0.65f + sin(t * 4f - 0.9f) * 0.35f) * (w * 0.036f) * motion
            val lickWave = cos(t * 3f - 1.1f) * (w * 0.020f) * motion
            val heightBreath = 1f + 0.028f * sin(t * 3f) * motion

            // 1. Frameless multi-stop volumetric atmospheric aura
            val auraPrimary = when (stage) {
                FlameVisualStage.FROZEN_STATIC -> Color(0x5238BDF8)
                FlameVisualStage.ICE_CRACKING -> Color(0x5C00E5FF)
                FlameVisualStage.COLOR_GRADIENT -> colors.second.copy(alpha = (if (isDarkTheme) 0.44f else 0.30f) * pulseGlow)
                FlameVisualStage.FULLY_IGNITED -> Color(0xFFF97316).copy(alpha = (if (isDarkTheme) 0.48f else 0.32f) * pulseGlow)
                FlameVisualStage.SUPERNOVA_OVERLOAD -> Color(0xFFC084FC).copy(alpha = (if (isDarkTheme) 0.54f else 0.36f) * pulseGlow)
                FlameVisualStage.OBSIDIAN_PLASMA -> Color(0xFFE879F9).copy(alpha = (if (isDarkTheme) 0.58f else 0.38f) * pulseGlow)
            }
            val auraRadius = minOf(w, h) * 0.485f
            val auraCenter = Offset(cx + bellyWave * 0.3f, cy - h * 0.03f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to auraPrimary,
                    0.36f to colors.second.copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
                    0.68f to colors.third.copy(alpha = if (isDarkTheme) 0.09f else 0.05f),
                    1.0f to Color.Transparent,
                    center = auraCenter,
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = auraCenter
            )

            val baseY = cy + h * 0.31f
            val baseSpan = h * 0.72f * heightBreath
            val tipY = baseY - baseSpan
            val flameHeight = baseY - tipY
            val flameWidth = w * 0.48f
            val tipX = cx + tipWave

            // Soft ground thermal reflection pool
            drawOval(
                brush = Brush.radialGradient(
                    0.0f to colors.first.copy(alpha = if (isDarkTheme) 0.34f else 0.22f),
                    0.55f to colors.second.copy(alpha = if (isDarkTheme) 0.18f else 0.11f),
                    1.0f to Color.Transparent,
                    center = Offset(cx + bellyWave * 0.4f, baseY + h * 0.015f),
                    radius = flameWidth * 0.68f
                ),
                topLeft = Offset(cx - flameWidth * 0.56f + bellyWave * 0.4f, baseY - h * 0.035f),
                size = Size(flameWidth * 1.12f, h * 0.105f)
            )

            // 2. Outer translucent plasma veil (pre-allocated Path)
            val mantleScale = 1.12f + 0.035f * sin(t * 2f - 0.5f)
            val mantleWidth = flameWidth * mantleScale
            val mantleTipY = tipY - flameHeight * 0.075f
            val mantleBaseY = baseY + flameHeight * 0.025f
            mantlePath.reset()
            mantlePath.moveTo(cx, mantleBaseY)
            mantlePath.cubicTo(
                cx - mantleWidth * 0.65f + bellyWave, cy + flameHeight * 0.18f,
                cx - mantleWidth * 0.46f + midWave * 0.7f, cy - flameHeight * 0.24f,
                tipX - lickWave * 0.35f, mantleTipY
            )
            mantlePath.cubicTo(
                cx + mantleWidth * 0.46f + midWave * 0.7f, cy - flameHeight * 0.24f,
                cx + mantleWidth * 0.65f + bellyWave, cy + flameHeight * 0.18f,
                cx, mantleBaseY
            )
            mantlePath.close()
            drawPath(
                path = mantlePath,
                brush = Brush.verticalGradient(
                    0.0f to colors.first.copy(alpha = 0.21f),
                    0.48f to colors.second.copy(alpha = 0.25f),
                    1.0f to colors.third.copy(alpha = 0.11f),
                    startY = mantleTipY,
                    endY = mantleBaseY
                )
            )

            // 3. Sculpted multi-tongue high-precision flame silhouette
            mainFlamePath.reset()
            mainFlamePath.moveTo(cx, baseY)
            mainFlamePath.cubicTo(
                cx - flameWidth * 0.58f + bellyWave, cy + flameHeight * 0.17f,
                cx - flameWidth * 0.52f + midWave * 0.75f, cy - flameHeight * 0.06f,
                cx - flameWidth * 0.28f + lickWave, cy - flameHeight * 0.24f
            )
            mainFlamePath.cubicTo(
                cx - flameWidth * 0.17f + midWave * 0.6f, cy - flameHeight * 0.14f,
                cx - flameWidth * 0.12f + tipWave * 0.65f, cy - flameHeight * 0.34f,
                tipX, tipY
            )
            mainFlamePath.cubicTo(
                cx + flameWidth * 0.18f + tipWave * 0.60f, cy - flameHeight * 0.30f,
                cx + flameWidth * 0.26f - lickWave * 0.45f, cy - flameHeight * 0.16f,
                cx + flameWidth * 0.34f - lickWave * 0.35f, cy - flameHeight * 0.20f
            )
            mainFlamePath.cubicTo(
                cx + flameWidth * 0.54f + midWave * 0.65f, cy - flameHeight * 0.02f,
                cx + flameWidth * 0.56f + bellyWave, cy + flameHeight * 0.18f,
                cx, baseY
            )
            mainFlamePath.close()

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

            // Dual-pass luminous rim contour
            drawPath(
                path = mainFlamePath,
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.28f),
                    0.55f to colors.first.copy(alpha = 0.20f),
                    1.0f to Color.Transparent,
                    startY = tipY,
                    endY = baseY
                ),
                style = Stroke(width = w * 0.017f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = mainFlamePath,
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.74f),
                    0.45f to colors.first.copy(alpha = 0.44f),
                    1.0f to Color.Transparent,
                    startY = tipY,
                    endY = baseY
                ),
                style = Stroke(width = w * 0.0065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 4. Mid-plasma smooth tonal tongue
            val midTipY = cy - flameHeight * 0.28f
            val midBaseY = baseY - flameHeight * 0.03f
            val midWidth = flameWidth * 0.68f
            val midTipX = cx + midWave * 0.95f
            midPath.reset()
            midPath.moveTo(cx, midBaseY)
            midPath.cubicTo(
                cx - midWidth * 0.58f + bellyWave * 0.6f, cy + flameHeight * 0.13f,
                cx - midWidth * 0.38f + midWave * 0.7f, cy - flameHeight * 0.12f,
                midTipX, midTipY
            )
            midPath.cubicTo(
                cx + midWidth * 0.38f + midWave * 0.7f, cy - flameHeight * 0.12f,
                cx + midWidth * 0.58f + bellyWave * 0.6f, cy + flameHeight * 0.13f,
                cx, midBaseY
            )
            midPath.close()
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
            val innerTipX = cx + bellyWave * 0.85f + midWave * 0.35f
            innerPath.reset()
            innerPath.moveTo(cx, innerBaseY)
            innerPath.cubicTo(
                cx - innerWidth * 0.56f, cy + flameHeight * 0.09f,
                cx - innerWidth * 0.28f + bellyWave * 0.5f, cy - flameHeight * 0.08f,
                innerTipX, innerTipY
            )
            innerPath.cubicTo(
                cx + innerWidth * 0.28f + bellyWave * 0.5f, cy - flameHeight * 0.08f,
                cx + innerWidth * 0.56f, cy + flameHeight * 0.09f,
                cx, innerBaseY
            )
            innerPath.close()
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
                    color = Color(0xFF22D3EE).copy(alpha = 0.34f * pulseGlow),
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
                val starR = w * 0.056f
                for (a in 0 until 3) {
                    val ang = a * (PI.toFloat() / 3f)
                    val dx = cos(ang) * starR
                    val dy = sin(ang) * starR
                    drawLine(
                        color = Color.White.copy(alpha = 0.95f),
                        start = Offset(coreCenter.x - dx, coreCenter.y - dy),
                        end = Offset(coreCenter.x + dx, coreCenter.y + dy),
                        strokeWidth = w * 0.0085f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    color = Color(0xFFE0F2FE),
                    radius = w * 0.022f,
                    center = coreCenter
                )
            } else {
                val coreGlowR = w * (0.092f + 0.012f * sin(t * 3f))
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.98f),
                        0.45f to Color.White.copy(alpha = 0.55f),
                        1.0f to Color.Transparent,
                        center = coreCenter,
                        radius = coreGlowR
                    ),
                    radius = coreGlowR,
                    center = coreCenter
                )
                val gleamR = w * (0.058f + 0.009f * cos(t * 2f))
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.88f), Color.Transparent),
                        startX = coreCenter.x - gleamR,
                        endX = coreCenter.x + gleamR
                    ),
                    start = Offset(coreCenter.x - gleamR, coreCenter.y),
                    end = Offset(coreCenter.x + gleamR, coreCenter.y),
                    strokeWidth = w * 0.0065f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.88f), Color.Transparent),
                        startY = coreCenter.y - gleamR * 1.25f,
                        endY = coreCenter.y + gleamR * 1.25f
                    ),
                    start = Offset(coreCenter.x, coreCenter.y - gleamR * 1.25f),
                    end = Offset(coreCenter.x, coreCenter.y + gleamR * 1.25f),
                    strokeWidth = w * 0.0065f,
                    cap = StrokeCap.Round
                )
            }

            // 7. 32 silky velocity-trailed rising embers (strictly periodic 2π flutter)
            if (stage != FlameVisualStage.FROZEN_STATIC) {
                val twoPi = 2f * PI.toFloat()
                val startY = baseY - flameHeight * 0.08f
                val endY = tipY - h * 0.135f
                val riseSpan = startY - endY

                for (idx in microSparks.indices) {
                    val spark = microSparks[idx]
                    val xRatio = spark[0]
                    val speed = spark[1]
                    val isAltColor = spark[2] > 0.5f
                    val phaseOff = spark[3]

                    val rawProg = ((tEmber * speed + phaseOff) % twoPi) / twoPi
                    // Gentle ease-out ascent for lifelike buoyant spark deceleration
                    val easedProg = 1f - (1f - rawProg) * (1f - rawProg) * 0.45f - (1f - rawProg) * 0.55f
                    val py = startY - easedProg * riseSpan
                    val flutterAngle = rawProg * twoPi + phaseOff
                    val px = cx + (xRatio - 0.5f) * flameWidth * (1.05f + easedProg * 0.24f) +
                        sin(flutterAngle) * (w * 0.032f)
                    val prevPy = (py + h * 0.024f * (1.15f - easedProg * 0.5f)).coerceAtMost(startY)
                    val prevPx = px - cos(flutterAngle) * (w * 0.007f)

                    val alpha = (sin(rawProg * PI.toFloat())).coerceIn(0f, 1f)
                    val baseSparkColor = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) {
                        if (isAltColor) Color(0xFFE879F9) else Color(0xFF22D3EE)
                    } else if (isAltColor) {
                        Color(0xFFFB7185)
                    } else {
                        Color(0xFFF97316)
                    }

                    // Taper ember radius subtly near peak altitude
                    val taper = 1f - easedProg * 0.35f
                    val dotRadius = w * (0.005f + (idx % 4) * 0.0024f) * taper
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                baseSparkColor.copy(alpha = alpha * 0.72f),
                                Color.Transparent
                            ),
                            startY = py,
                            endY = prevPy
                        ),
                        start = Offset(px, py),
                        end = Offset(prevPx, prevPy),
                        strokeWidth = dotRadius * 1.15f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = baseSparkColor.copy(alpha = alpha * 0.32f),
                        radius = dotRadius * 2.3f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.78f),
                        radius = dotRadius * 0.62f,
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
