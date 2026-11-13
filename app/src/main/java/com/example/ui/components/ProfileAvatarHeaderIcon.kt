package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.profile.AvatarItem
import com.example.data.profile.TitleItem
import com.example.data.profile.TitleRarity
import com.example.ui.theme.NeonRose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic Circular Profile Avatar Icon with fluid cyberpunk neon border/halo.
 * Intensity, brightness, and visual complexity dynamically scale up based on the
 * rarity tier of the user's currently equipped title (Common -> Uncommon -> Rare -> Legendary / Ultimate / Milestone Peak).
 * Supports optional 5-second (5000ms) long-press for hard resetting all task completion states.
 */
@Composable
fun ProfileAvatarHeaderIcon(
    avatar: AvatarItem,
    title: TitleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    onLongClick5s: (() -> Unit)? = null
) {
    val rarity = title.rarity
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }

    val holdProgress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = if (isHolding) {
            tween(durationMillis = 5000, easing = LinearEasing)
        } else {
            tween(durationMillis = 150)
        },
        label = "profile_hold_5s_progress"
    )

    // Infinite transition for fluid rotating neon halo
    val infiniteTransition = rememberInfiniteTransition(label = "halo_transition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (rarity) {
                    TitleRarity.COMMON -> 6000
                    TitleRarity.UNCOMMON -> 4000
                    TitleRarity.RARE -> 2500
                    TitleRarity.LEGENDARY, TitleRarity.ULTIMATE, TitleRarity.MILESTONE_PEAK -> 1600
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = when (rarity) {
            TitleRarity.COMMON -> 1.02f
            TitleRarity.UNCOMMON -> 1.05f
            TitleRarity.RARE -> 1.08f
            TitleRarity.LEGENDARY, TitleRarity.ULTIMATE, TitleRarity.MILESTONE_PEAK -> 1.12f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val haloColor1 = rarity.primaryColor
    val haloColor2 = rarity.secondaryColor
    val glowColor = rarity.glowColor

    val gestureModifier = if (onLongClick5s != null) {
        Modifier.pointerInput(onLongClick5s, onClick) {
            detectTapGestures(
                onPress = {
                    isHolding = true
                    var triggered = false
                    val job = coroutineScope.launch {
                        delay(5000L)
                        triggered = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick5s.invoke()
                    }
                    val released = tryAwaitRelease()
                    isHolding = false
                    job.cancel()
                    if (released && !triggered) {
                        onClick()
                    }
                }
            )
        }
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(gestureModifier)
            .testTag("profile_avatar_header_btn"),
        contentAlignment = Alignment.Center
    ) {
        // Multi-ring fluid halo Canvas
        Canvas(modifier = Modifier.size(size)) {
            val center = this.center
            val radius = size.toPx() / 2f - 2f

            // Outer glow ambient circle
            drawCircle(
                color = glowColor.copy(alpha = if (rarity.borderComplexity >= 3) 0.45f else 0.25f),
                radius = radius + (pulseScale - 1f) * 12f
            )

            // Inner complexity rings
            if (rarity.borderComplexity >= 2) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(haloColor1, haloColor2, Color.Transparent, haloColor1),
                        center = center
                    ),
                    radius = radius - 1.5f,
                    style = Stroke(width = if (rarity.borderComplexity >= 3) 2.5f else 1.8f)
                )
            }

            // Orbital particle sparks for Legendary / Ultimate / Milestone Peak
            if (rarity.borderComplexity >= 4) {
                val rad = Math.toRadians(rotationAngle.toDouble())
                val sparkX = center.x + (radius + 2f) * cos(rad).toFloat()
                val sparkY = center.y + (radius + 2f) * sin(rad).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = androidx.compose.ui.geometry.Offset(sparkX, sparkY)
                )

                val oppRad = rad + Math.PI
                val oppX = center.x + (radius + 2f) * cos(oppRad).toFloat()
                val oppY = center.y + (radius + 2f) * sin(oppRad).toFloat()
                drawCircle(
                    color = haloColor1,
                    radius = 2f,
                    center = androidx.compose.ui.geometry.Offset(oppX, oppY)
                )
            }

            // 5-second hard task reset hold progress ring
            if (holdProgress > 0.01f) {
                drawArc(
                    color = NeonRose,
                    startAngle = -90f,
                    sweepAngle = holdProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Inner Avatar Container
        Box(
            modifier = Modifier
                .size(size - 6.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .border(1.dp, if (isHolding) NeonRose else haloColor1.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val icon = getAvatarVector(avatar.badgeIconName)
            Icon(
                imageVector = icon,
                contentDescription = avatar.nameAr,
                tint = if (isHolding) NeonRose else haloColor1,
                modifier = Modifier
                    .size(size * 0.52f)
                    .scale(pulseScale)
            )
        }
    }
}

fun getAvatarVector(iconName: String): ImageVector {
    return when (iconName) {
        "hub" -> Icons.Default.Hub
        "memory" -> Icons.Default.Bolt
        "diamond" -> Icons.Default.Diamond
        "code" -> Icons.Default.AutoAwesome
        "shield" -> Icons.Default.Shield
        "visibility" -> Icons.Default.Visibility
        "dna" -> Icons.Default.AllInclusive
        "ac_unit" -> Icons.Default.Shield
        "security" -> Icons.Default.Shield
        "pan_tool" -> Icons.Default.Speed
        "biotech" -> Icons.Default.AllInclusive
        "speed" -> Icons.Default.Speed
        "electric_bolt" -> Icons.Default.ElectricBolt
        "flutter_dash" -> Icons.Default.AutoAwesome
        "coronavirus" -> Icons.Default.Coronavirus
        "military_tech" -> Icons.Default.MilitaryTech
        "all_inclusive" -> Icons.Default.AllInclusive
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        else -> Icons.Default.Person
    }
}
