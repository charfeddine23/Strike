package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans

/**
 * Unified iPhone-style Liquid Glass 3-segment Command Dock connecting:
 *  - Physical LEFT  (activeSlot = 0): Tasks Ledger
 *  - Physical CENTER (activeSlot = 1): Level X/700 & Roadmap Progress
 *  - Physical RIGHT (activeSlot = 2): Xp Logs
 *
 * Layout order stays physically Left-to-Right in both Arabic (RTL) and English/French (LTR),
 * while restoring the app's LayoutDirection inside each individual segment for clean typography/icons.
 */
@Composable
fun RoadmapCommandDock(
    activeSlot: Int, // 0 = Tasks Ledger, 1 = LVL / Roadmap, 2 = Xp Logs
    safeStationId: Int,
    totalStationsCount: Int,
    stageProgressFraction: Float,
    biomeState: RoadmapBiomeState,
    currentLang: String,
    onOpenTasksLedger: () -> Unit,
    onOpenProgressOrRoadmap: () -> Unit,
    onOpenXpLogs: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val appDirection = LocalLayoutDirection.current
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val ledgerAccent = if (isDarkTheme) ElectricMint else DayEmerald
    val centerAccent = biomePrimaryAccent(biomeState, isDarkTheme)
    val xpLogsAccent = if (isDarkTheme) NeonViolet else DayPurple

    val biomeIcon = when (biomeState.dominantBiome) {
        RoadmapBiome.GLACIAL -> Icons.Default.AcUnit
        RoadmapBiome.ROCKY -> Icons.Default.Landscape
        RoadmapBiome.FOREST -> Icons.Default.Park
        RoadmapBiome.VOLCANIC -> Icons.Default.LocalFireDepartment
    }

    val activeAccent = when (activeSlot) {
        0 -> ledgerAccent
        2 -> xpLogsAccent
        else -> centerAccent
    }

    val outerTrayShape = RoundedCornerShape(24.dp)
    val trayFillBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(Color(0x5A0A1324), Color(0x74070E1B), Color(0x660B1528))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xD8FFFFFF), Color(0xB8F1F5F9), Color(0xCCFFFFFF))
        )
    }
    val trayBorderBrush = Brush.linearGradient(
        listOf(
            if (isDarkTheme) Color(0x70FFFFFF) else Color(0xEEFFFFFF),
            activeAccent.copy(alpha = if (isDarkTheme) 0.55f else 0.45f),
            if (isDarkTheme) Color(0x28FFFFFF) else Color(0xFFCBD5E1)
        )
    )

    val ledgerLabel = when {
        isAr -> "دفتر المهام"
        isFr -> "Registre"
        else -> "Tasks Ledger"
    }
    val levelLabel = if (isFr) "Niv $safeStationId/$totalStationsCount" else "LVL $safeStationId/$totalStationsCount"
    val xpLogsLabel = "Xp Logs"

    val clampedFraction by animateFloatAsState(
        targetValue = stageProgressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "dock_level_progress"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDarkTheme) 10.dp else 12.dp,
                    shape = outerTrayShape,
                    clip = false,
                    ambientColor = Color(0x44000000),
                    spotColor = Color(0x55000000)
                )
                .clip(outerTrayShape)
                .background(trayFillBrush)
                .drawBehind {
                    val w = size.width
                    val inset = 22.dp.toPx().coerceAtMost(w * 0.25f)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x66FFFFFF),
                                if (isDarkTheme) Color(0xB8FFFFFF) else Color(0xFFFFFFFF),
                                Color(0x66FFFFFF),
                                Color.Transparent
                            )
                        ),
                        start = Offset(inset, 1.dp.toPx()),
                        end = Offset(w - inset, 1.dp.toPx()),
                        strokeWidth = 1.15.dp.toPx()
                    )
                }
                .border(1.1.dp, trayBorderBrush, outerTrayShape)
                .padding(horizontal = 5.dp, vertical = 4.dp)
                .testTag("roadmap_command_dock")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SLOT: Tasks Ledger
                DockSegmentItem(
                    label = ledgerLabel,
                    icon = Icons.Default.MenuBook,
                    contentDescription = if (isAr) "دفتر المهام" else "Tasks Ledger",
                    selected = activeSlot == 0,
                    accentColor = ledgerAccent,
                    progressFraction = null,
                    appDirection = appDirection,
                    isDarkTheme = isDarkTheme,
                    onClick = onOpenTasksLedger,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("roadmap_header_ledger_btn")
                )

                // CENTER SLOT: LVL X/700 + Roadmap Progress Drawer
                DockSegmentItem(
                    label = levelLabel,
                    icon = biomeIcon,
                    contentDescription = when {
                        isAr -> "تقدم المسار"
                        isFr -> "Progression"
                        else -> "Roadmap Progress"
                    },
                    selected = activeSlot == 1,
                    accentColor = centerAccent,
                    progressFraction = clampedFraction,
                    appDirection = appDirection,
                    isDarkTheme = isDarkTheme,
                    onClick = onOpenProgressOrRoadmap,
                    modifier = Modifier
                        .weight(1.06f)
                        .testTag("roadmap_header_progress_btn")
                )

                // RIGHT SLOT: Xp Logs
                DockSegmentItem(
                    label = xpLogsLabel,
                    icon = Icons.Default.History,
                    contentDescription = if (isAr) "سجل XP" else "Xp Logs",
                    selected = activeSlot == 2,
                    accentColor = xpLogsAccent,
                    progressFraction = null,
                    appDirection = appDirection,
                    isDarkTheme = isDarkTheme,
                    onClick = onOpenXpLogs,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("roadmap_header_xp_logs_btn")
                )
            }
        }
    }
}

@Composable
private fun DockSegmentItem(
    label: String,
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    accentColor: Color,
    progressFraction: Float?,
    appDirection: LayoutDirection,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val segShape = RoundedCornerShape(19.dp)

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            accentColor
        } else if (isDarkTheme) {
            accentColor.copy(alpha = 0.88f)
        } else {
            DayTextPrimary
        },
        animationSpec = tween(180),
        label = "dock_seg_fg"
    )
    val bgTopColor by animateColorAsState(
        targetValue = when {
            selected && isDarkTheme -> accentColor.copy(alpha = 0.28f)
            selected && !isDarkTheme -> Color.White.copy(alpha = 0.94f)
            !selected && isDarkTheme -> Color(0x16FFFFFF)
            else -> Color(0x66FFFFFF)
        },
        animationSpec = tween(180),
        label = "dock_seg_bg_top"
    )
    val bgBotColor by animateColorAsState(
        targetValue = when {
            selected && isDarkTheme -> accentColor.copy(alpha = 0.12f)
            selected && !isDarkTheme -> accentColor.copy(alpha = 0.18f)
            !selected && isDarkTheme -> Color(0x0AFFFFFF)
            else -> Color(0x3DF8FAFC)
        },
        animationSpec = tween(180),
        label = "dock_seg_bg_bot"
    )
    val borderCol by animateColorAsState(
        targetValue = if (selected) {
            accentColor.copy(alpha = if (isDarkTheme) 0.90f else 0.78f)
        } else {
            accentColor.copy(alpha = if (isDarkTheme) 0.34f else 0.24f)
        },
        animationSpec = tween(180),
        label = "dock_seg_border"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(segShape)
            .background(Brush.verticalGradient(listOf(bgTopColor, bgBotColor)))
            .drawBehind {
                if (selected) {
                    val w = size.width
                    val inset = 10.dp.toPx().coerceAtMost(w * 0.25f)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isDarkTheme) 0.55f else 0.95f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(inset, 1.dp.toPx()),
                        end = Offset(w - inset, 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .border(if (selected) 1.25.dp else 0.85.dp, borderCol, segShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides appDirection) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = if (selected) contentColor else accentColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = if (selected) contentColor else if (isDarkTheme) Color(0xFFE2E8F0) else DayTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (progressFraction != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.76f)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDarkTheme) Color(0x33FFFFFF) else Color(0x280F172A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceAtLeast(0.05f))
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accentColor, if (isDarkTheme) Color.White else DayCyan)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
