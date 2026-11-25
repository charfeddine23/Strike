package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.map.HeatmapDisplayMode
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PlusJakartaSans

/**
 * Sleek translucent Liquid Glass summary & visualization switcher bar shown below the elevated
 * "Swipe up to hide map" pill. Intentionally avoids duplicating "Hottest" (shown at top-left).
 */
@Composable
fun HeatmapSummaryAndToggleBar(
    registeredPeopleCount: Int,
    totalPinnedStrikesCount: Int,
    displayMode: HeatmapDisplayMode,
    accentCyan: Color,
    isMapDark: Boolean,
    currentLang: String,
    onSelectDisplayMode: (HeatmapDisplayMode) -> Unit,
    onCounterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val barBg = if (isMapDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xD80D172B),
                Color(0xE8070E1C)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xEEFFFFFF),
                Color(0xE6EEF4FC)
            )
        )
    }
    val borderColor = if (isMapDark) {
        accentCyan.copy(alpha = 0.34f)
    } else {
        Color(0x550284C7)
    }
    val textPrimary = if (isMapDark) Color(0xFFF8FAFC) else DayTextPrimary
    val textSecondary = if (isMapDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val isHeatmapActive = displayMode != HeatmapDisplayMode.CLUSTER_PINS
    val isPinsActive = displayMode == HeatmapDisplayMode.CLUSTER_PINS

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(barBg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("heatmap_summary_toggle_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Total Mapped Counter (distinct registered individuals mapped on map)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onCounterClick)
                .padding(horizontal = 4.dp, vertical = 3.dp)
                .testTag("heatmap_total_mapped_counter"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accentCyan.copy(alpha = 0.28f),
                                NeonEmerald.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(1.dp, accentCyan.copy(alpha = 0.52f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = when {
                        isAr -> "$registeredPeopleCount مسجل على الخريطة"
                        isFr -> "$registeredPeopleCount inscrits"
                        else -> "$registeredPeopleCount Mapped"
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        isAr -> "$totalPinnedStrikesCount نقطة ضربة نشطة"
                        isFr -> "$totalPinnedStrikesCount repères actifs"
                        else -> "$totalPinnedStrikesCount active pin(s)"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Instant Visualization Switcher (Heatmap <-> Pins)
        val capsuleBg = if (isMapDark) Color(0x66060B16) else Color(0xFFE2E8F0)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(15.dp))
                .background(capsuleBg)
                .border(0.9.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(15.dp))
                .padding(3.dp)
                .testTag("heatmap_hud_view_switcher"),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudModeSegmentPill(
                label = when {
                    isAr -> "حراري"
                    isFr -> "Chaleur"
                    else -> "Heatmap"
                },
                icon = Icons.Default.Whatshot,
                selected = isHeatmapActive,
                accentColor = NeonAmber,
                isMapDark = isMapDark,
                onClick = { onSelectDisplayMode(HeatmapDisplayMode.HEATMAP_GLOW) },
                modifier = Modifier.testTag("heatmap_hud_mode_heatmap_btn")
            )
            HudModeSegmentPill(
                label = when {
                    isAr -> "دبابيس"
                    isFr -> "Points"
                    else -> "Pins"
                },
                icon = Icons.Default.LocationOn,
                selected = isPinsActive,
                accentColor = accentCyan,
                isMapDark = isMapDark,
                onClick = { onSelectDisplayMode(HeatmapDisplayMode.CLUSTER_PINS) },
                modifier = Modifier.testTag("heatmap_hud_mode_pins_btn")
            )
        }
    }
}

@Composable
private fun HudModeSegmentPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color,
    isMapDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            accentColor.copy(alpha = if (isMapDark) 0.24f else 0.20f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hud_mode_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            accentColor
        } else if (isMapDark) {
            Color(0xFF94A3B8)
        } else {
            Color(0xFF475569)
        },
        animationSpec = tween(durationMillis = 160),
        label = "hud_mode_content"
    )
    val borderColor = if (selected) accentColor.copy(alpha = 0.72f) else Color.Transparent

    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Bottom Dual-Action Bar pairing Know My Location (GPS animateCamera) and Enter (reveal main app + TopAppBar).
 */
@Composable
fun HeatmapDualActionBar(
    accentCyan: Color,
    isMapDark: Boolean,
    currentLang: String,
    onKnowLocationClick: () -> Unit,
    onEnterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val locationBg = if (isMapDark) {
        Brush.verticalGradient(listOf(Color(0xE8101C33), Color(0xF208101F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFAFFFFFF), Color(0xF0ECF3FC)))
    }
    val locationText = if (isMapDark) Color(0xFFF1F5F9) else DayTextPrimary

    val enterBg = Brush.horizontalGradient(
        listOf(
            Color(0xFF0EA5E9),
            Color(0xFF06B6D4),
            Color(0xFF10B981)
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .testTag("heatmap_bottom_bar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Know My Location
        Row(
            modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .heightIn(min = 50.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(locationBg)
                .border(1.1.dp, accentCyan.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .clickable(onClick = onKnowLocationClick)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("heatmap_know_location_btn"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = accentCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = when {
                    isAr -> "موقعي الآن"
                    isFr -> "Ma position"
                    else -> "Know My Location"
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                color = locationText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Enter main Obsidian app & reveal TopAppBar
        Row(
            modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .heightIn(min = 50.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(enterBg)
                .border(
                    1.1.dp,
                    Color.White.copy(alpha = 0.42f),
                    RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onEnterClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("heatmap_enter_btn"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    isAr -> "دخول"
                    isFr -> "Entrer"
                    else -> "Enter"
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = Color(0xFF031521),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(7.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF031521),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Elevated Liquid Glass "Swipe up to hide map" pill floating above the lower HUD stack.
 */
@Composable
fun ElevatedSwipeUpBeaconPill(
    label: String,
    accentCyan: Color,
    textPrimary: Color,
    isMapDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "elevated_swipe_up_beacon")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "swipe_wave_progress"
    )
    val floatProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipe_float_progress"
    )
    val shimmerSweep by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "swipe_shimmer_sweep"
    )

    val pillBg = if (isMapDark) {
        Brush.verticalGradient(listOf(Color(0xE8111E36), Color(0xF2070E1C)))
    } else {
        Brush.verticalGradient(listOf(Color(0xF8FFFFFF), Color(0xEEEDF3FC)))
    }

    val borderSweep = Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f to accentCyan.copy(alpha = 0.28f),
            (shimmerSweep - 0.15f).coerceIn(0f, 1f) to accentCyan.copy(alpha = 0.45f),
            shimmerSweep.coerceIn(0f, 1f) to Color.White.copy(alpha = if (isMapDark) 0.90f else 0.95f),
            (shimmerSweep + 0.15f).coerceIn(0f, 1f) to NeonEmerald.copy(alpha = 0.65f),
            1.0f to accentCyan.copy(alpha = 0.28f)
        )
    )

    val orbArrowShift = (waveProgress * 2f).let { p ->
        if (p <= 1f) -p * 3.0f else -(2f - p) * 3.0f
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = ((floatProgress - 0.5f) * -4.dp.toPx())
            }
            .clip(RoundedCornerShape(50))
            .background(pillBg)
            .border(1.25.dp, borderSweep, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 18.dp, vertical = 9.dp)
            .testTag("heatmap_swipe_up_hide_btn"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accentCyan.copy(alpha = 0.34f + 0.18f * floatProgress),
                                accentCyan.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .border(1.dp, accentCyan.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            translationY = orbArrowShift.dp.toPx()
                        }
                )
            }

            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = accentCyan.copy(alpha = 0.7f + 0.3f * floatProgress),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        translationY = orbArrowShift.dp.toPx()
                    }
            )
        }
    }
}
