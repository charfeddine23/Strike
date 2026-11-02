package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.CaveatHandwriting
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

enum class PatternStatus {
    IDLE,
    DRAWING,
    SUCCESS,
    ERROR
}

@Composable
fun PatternLockCanvas(
    modifier: Modifier = Modifier,
    status: PatternStatus = PatternStatus.IDLE,
    onPatternEntered: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }
    val isDarkTheme = isSystemInDarkTheme()

    fun triggerTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (_: Exception) {}
    }

    val canvasBg = if (isDarkTheme) Color(0x330F172A) else Color(0x80FFFFFF)
    val successColor = if (isDarkTheme) NeonEmerald else DayEmerald
    val errorColor = if (isDarkTheme) NeonRose else Color(0xFFDC2626)
    val idleBorder = if (isDarkTheme) Color(0x4038BDF8) else Color(0x6000B4D8)

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(canvasBg)
            .border(1.5.dp, when (status) {
                PatternStatus.SUCCESS -> successColor
                PatternStatus.ERROR -> errorColor
                else -> idleBorder
            }, RoundedCornerShape(24.dp))
            .pointerInput(status) {
                if (status == PatternStatus.SUCCESS || status == PatternStatus.ERROR) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        selectedNodes.clear()
                        currentTouchPosition = offset
                        val node = findNodeAt(offset, size.width.toFloat(), size.height.toFloat())
                        if (node != null) {
                            selectedNodes.add(node)
                            triggerTick()
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchPosition = change.position
                        val node = findNodeAt(change.position, size.width.toFloat(), size.height.toFloat())
                        if (node != null && !selectedNodes.contains(node)) {
                            selectedNodes.add(node)
                            triggerTick()
                        }
                    },
                    onDragEnd = {
                        val patternStr = selectedNodes.joinToString("-")
                        currentTouchPosition = null
                        if (patternStr.isNotEmpty()) {
                            onPatternEntered(patternStr)
                        }
                    },
                    onDragCancel = {
                        selectedNodes.clear()
                        currentTouchPosition = null
                    }
                )
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val nodePositions = remember(width, height) {
            calculateNodePositions(width, height)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = when (status) {
                PatternStatus.SUCCESS -> NeonEmerald
                PatternStatus.ERROR -> NeonRose
                else -> NeonCyan
            }

            // Draw lines between connected nodes
            if (selectedNodes.size > 1) {
                for (i in 0 until selectedNodes.size - 1) {
                    val start = nodePositions[selectedNodes[i]]
                    val end = nodePositions[selectedNodes[i + 1]]
                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(
                                lineColor.copy(alpha = 0.85f),
                                lineColor
                            ),
                            start = start,
                            end = end
                        ),
                        start = start,
                        end = end,
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw line to current touch pointer
            if (currentTouchPosition != null && selectedNodes.isNotEmpty()) {
                val lastNodePos = nodePositions[selectedNodes.last()]
                drawLine(
                    color = lineColor.copy(alpha = 0.6f),
                    start = lastNodePos,
                    end = currentTouchPosition!!,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // Draw 9 nodes
            for (i in 0..8) {
                val pos = nodePositions[i]
                val isSelected = selectedNodes.contains(i)

                val nodeColor = if (isSelected) lineColor else Color(0x8094A3B8)
                val outerRadius = if (isSelected) 32f else 18f
                val innerRadius = if (isSelected) 14f else 8f

                // Outer halo/ring
                if (isSelected) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.25f),
                        radius = outerRadius + 14f,
                        center = pos
                    )
                    drawCircle(
                        color = lineColor.copy(alpha = 0.6f),
                        radius = outerRadius,
                        center = pos,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                } else {
                    drawCircle(
                        color = Color(0x3338BDF8),
                        radius = outerRadius,
                        center = pos,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }

                // Inner core dot
                drawCircle(
                    color = nodeColor,
                    radius = innerRadius,
                    center = pos
                )
            }
        }
    }
}

private fun calculateNodePositions(width: Float, height: Float): List<Offset> {
    val positions = mutableListOf<Offset>()
    val stepX = width / 4f
    val stepY = height / 4f

    for (row in 1..3) {
        for (col in 1..3) {
            positions.add(Offset(col * stepX, row * stepY))
        }
    }
    return positions
}

private fun findNodeAt(touch: Offset, width: Float, height: Float, hitRadius: Float = 65f): Int? {
    val positions = calculateNodePositions(width, height)
    for (i in positions.indices) {
        val node = positions[i]
        val distance = sqrt((touch.x - node.x).pow(2) + (touch.y - node.y).pow(2))
        if (distance <= hitRadius) {
            return i
        }
    }
    return null
}

/**
 * Fullscreen / Overlay Pattern Unlock Screen
 */
@Composable
fun FullscreenPatternUnlockScreen(
    onUnlockSuccess: () -> Unit,
    onVerifyPattern: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var status by remember { mutableStateOf(PatternStatus.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    val overlayBg = if (isDarkTheme) Color(0xE6050B14) else Color(0xF5F1F5F9)
    val primaryColor = if (isDarkTheme) NeonCyan else DayCyan
    val iconBorder = if (status == PatternStatus.ERROR) {
        if (isDarkTheme) NeonRose else Color(0xFFDC2626)
    } else {
        primaryColor
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Lock Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.16f))
                    .border(1.5.dp, iconBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (status) {
                        PatternStatus.SUCCESS -> Icons.Default.CheckCircle
                        PatternStatus.ERROR -> Icons.Default.Error
                        else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = when (status) {
                        PatternStatus.SUCCESS -> if (isDarkTheme) NeonEmerald else DayEmerald
                        PatternStatus.ERROR -> if (isDarkTheme) NeonRose else Color(0xFFDC2626)
                        else -> primaryColor
                    },
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 26.sp,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = errorMessage ?: stringResource(R.string.draw_pattern_to_unlock),
                fontSize = 15.sp,
                fontFamily = CaveatHandwriting,
                color = if (errorMessage != null) (if (isDarkTheme) NeonRose else Color(0xFFDC2626)) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = if (errorMessage != null) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(28.dp))

            PatternLockCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("pattern_lock_canvas"),
                status = status,
                onPatternEntered = { pattern ->
                    val isValid = onVerifyPattern(pattern)
                    if (isValid) {
                        status = PatternStatus.SUCCESS
                        errorMessage = null
                        scope.launch {
                            delay(350)
                            onUnlockSuccess()
                        }
                    } else {
                        status = PatternStatus.ERROR
                        errorMessage = "نقش غير صحيح / Schéma incorrect"
                        scope.launch {
                            delay(900)
                            status = PatternStatus.IDLE
                            errorMessage = null
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.pattern_lock_desc),
                fontSize = 11.sp,
                color = Color(0x8094A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}
