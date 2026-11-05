package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PlusJakartaSans
import kotlin.math.cos
import kotlin.math.sin

/**
 * Striker status based on last recorded strike date:
 * - ACTIVE: Recorded a strike within the last 2 years (<= 2 years) -> Green
 * - STAND_BY: Has NOT recorded a strike for MORE than 2 years (<= 3 years) -> Orange
 * - RETIRED: Has NOT recorded a strike for MORE than 3 years -> Red
 */
enum class StrikerStatus(val stringRes: Int) {
    ACTIVE(R.string.status_active),
    STAND_BY(R.string.status_stand_by),
    RETIRED(R.string.status_retired)
}

object StrikerStatusEvaluator {
    private const val ONE_YEAR_MILLIS = (365.25 * 24 * 60 * 60 * 1000).toLong()
    val TWO_YEARS_MILLIS = 2 * ONE_YEAR_MILLIS
    val THREE_YEARS_MILLIS = 3 * ONE_YEAR_MILLIS

    /**
     * Determines the status based on last recorded strike timestamp.
     * Evaluates retirement priority (> 3 years) before stand by (> 2 years).
     */
    fun evaluate(
        lastStrikeTimestamp: Long?,
        strikeCount: Int = 1,
        updatedAt: Long = 0L
    ): StrikerStatus {
        val now = System.currentTimeMillis()
        val effectiveTime = lastStrikeTimestamp
            ?: if (strikeCount > 0 && updatedAt > 0) updatedAt else null

        if (effectiveTime == null) {
            return StrikerStatus.RETIRED
        }

        val elapsed = now - effectiveTime

        return when {
            elapsed > THREE_YEARS_MILLIS -> StrikerStatus.RETIRED // > 3 years inactive
            elapsed > TWO_YEARS_MILLIS -> StrikerStatus.STAND_BY  // > 2 years inactive (and <= 3 years)
            else -> StrikerStatus.ACTIVE                         // Within last 2 years (<= 2 years)
        }
    }
}

/**
 * Status colors configuration
 */
data class StatusColorPalette(
    val primary: Color,
    val secondary: Color,
    val backgroundTint: Color,
    val borderTint: Color
)

fun getStatusPalette(status: StrikerStatus, darkTheme: Boolean): StatusColorPalette {
    return when (status) {
        StrikerStatus.ACTIVE -> StatusColorPalette(
            primary = if (darkTheme) Color(0xFF10B981) else Color(0xFF059669), // Emerald
            secondary = if (darkTheme) Color(0xFF00E5FF) else Color(0xFF0284C7),
            backgroundTint = if (darkTheme) Color(0x2210B981) else Color(0x1810B981),
            borderTint = if (darkTheme) Color(0x6610B981) else Color(0x4D059669)
        )
        StrikerStatus.STAND_BY -> StatusColorPalette(
            primary = if (darkTheme) Color(0xFFF59E0B) else Color(0xFFD97706), // Warm Orange
            secondary = if (darkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
            backgroundTint = if (darkTheme) Color(0x22F59E0B) else Color(0x18F59E0B),
            borderTint = if (darkTheme) Color(0x66F59E0B) else Color(0x4DD97706)
        )
        StrikerStatus.RETIRED -> StatusColorPalette(
            primary = if (darkTheme) Color(0xFFF43F5E) else Color(0xFFE11D48), // Rose Red
            secondary = if (darkTheme) Color(0xFFFB7185) else Color(0xFFBE123C),
            backgroundTint = if (darkTheme) Color(0x22F43F5E) else Color(0x18F43F5E),
            borderTint = if (darkTheme) Color(0x66F43F5E) else Color(0x4DE11D48)
        )
    }
}

/**
 * Dynamic Status Badge with an animated glowing beacon dot
 */
@Composable
fun StrikerStatusBadge(
    status: StrikerStatus,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val palette = remember(status, darkTheme) { getStatusPalette(status, darkTheme) }

    val infiniteTransition = rememberInfiniteTransition(label = "status_beacon")
    val beaconPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )
    val beaconPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.backgroundTint)
            .border(1.dp, palette.borderTint, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
            .testTag("status_badge_${status.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Beacon Indicator Dot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(8.dp)
            ) {
                // Expanding glowing halo
                Box(
                    modifier = Modifier
                        .size(8.dp * beaconPulseScale)
                        .clip(CircleShape)
                        .background(palette.primary.copy(alpha = beaconPulseAlpha * 0.4f))
                )
                // Solid core center
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(palette.primary)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = stringResource(status.stringRes),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = palette.primary,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * Dynamic Animated Frame & Background Card Component
 * Wraps the person's profile with:
 * 1. A subtle rotating animated gradient border/frame tuned to their status color
 * 2. An organic breathing background gradient effect that makes the card feel alive
 */
@Composable
fun DynamicAnimatedProfileCard(
    status: StrikerStatus,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    borderWidth: Dp = 1.25.dp,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = remember(status, darkTheme) { getStatusPalette(status, darkTheme) }

    val infiniteTransition = rememberInfiniteTransition(label = "profile_card_anim")

    // Smooth rotating angle for the border frame
    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_angle"
    )

    // Breathing pulse for the background dynamic effect
    val bgPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_pulse"
    )

    // Compute animated dynamic border brush
    val angleRad = Math.toRadians(borderAngle.toDouble())
    val dirX = cos(angleRad).toFloat()
    val dirY = sin(angleRad).toFloat()

    val animatedBorderBrush = Brush.linearGradient(
        colors = listOf(
            palette.primary.copy(alpha = if (darkTheme) 0.85f else 0.7f),
            palette.secondary.copy(alpha = if (darkTheme) 0.35f else 0.25f),
            Color.White.copy(alpha = if (darkTheme) 0.15f else 0.4f),
            palette.primary.copy(alpha = if (darkTheme) 0.75f else 0.6f)
        ),
        start = Offset(250f * (1f - dirX), 250f * (1f - dirY)),
        end = Offset(250f * (1f + dirX), 250f * (1f + dirY))
    )

    val shadowElevation = if (darkTheme) 5.dp else 8.dp
    val shadowAmbient = if (darkTheme) Color.Transparent else Color(0x140284C7)
    val shadowSpot = if (darkTheme) Color.Transparent else Color(0x180F172A)

    val baseBgColor = if (darkTheme) Color(0xDD0D1629) else Color(0xF2FFFFFF)

    val baseModifier = modifier
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = shadowAmbient,
            spotColor = shadowSpot
        )
        .clip(shape)
        .background(baseBgColor)
        .drawBehind {
            // Dynamic breathing background effect (living radial aura from status badge origin)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = bgPulseAlpha),
                        palette.secondary.copy(alpha = bgPulseAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.2f),
                    radius = size.width * 0.8f
                )
            )

            // Specular top-light refraction edge
            drawLine(
                color = if (darkTheme) Color(0x2AFFFFFF) else Color(0x80FFFFFF),
                start = Offset(14.dp.toPx(), 1.dp.toPx()),
                end = Offset(size.width - 14.dp.toPx(), 1.dp.toPx()),
                strokeWidth = 1.2.dp.toPx()
            )
        }
        .border(borderWidth, animatedBorderBrush, shape)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier.padding(16.dp),
        content = content
    )
}
