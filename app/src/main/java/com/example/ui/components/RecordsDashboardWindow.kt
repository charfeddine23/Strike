package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.ui.screens.SwipeableStrikeLogItem
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.ui.viewmodel.YearlyStrikeStat
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sleek Liquid Glass "Dashboard" banner replacing the Flame Energy banner on the Records page
 * to save vertical space for the records list while giving one-tap access to key metrics.
 */
@Composable
fun RecordsDashboardBanner(
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "records_dash_banner_anim")
    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_border_angle"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dash_glow_pulse"
    )

    val angleRad = Math.toRadians(borderAngle.toDouble())
    val dirX = cos(angleRad).toFloat()
    val dirY = sin(angleRad).toFloat()

    val primaryAccent = if (isDarkTheme) NeonCyan else DayCyan
    val secondaryAccent = if (isDarkTheme) NeonViolet else DayPurple

    val dynamicBorderBrush = Brush.linearGradient(
        colors = listOf(
            primaryAccent.copy(alpha = glowPulse * 0.9f),
            NeonEmerald.copy(alpha = 0.65f),
            secondaryAccent.copy(alpha = glowPulse * 0.85f),
            primaryAccent.copy(alpha = 0.7f)
        ),
        start = Offset(200f * (1f - dirX), 60f * (1f - dirY)),
        end = Offset(200f * (1f + dirX), 60f * (1f + dirY))
    )

    val shape = RoundedCornerShape(16.dp)
    val cardBgColor = if (isDarkTheme) Color(0xD90B1324) else Color(0xF2FFFFFF)
    val shadowColor = if (isDarkTheme) primaryAccent.copy(alpha = 0.22f * glowPulse) else Color(0x180284C7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = if (isDarkTheme) 6.dp else 4.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(cardBgColor)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = 0.16f * glowPulse),
                            secondaryAccent.copy(alpha = 0.08f * glowPulse),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.5f),
                        radius = size.width * 0.65f
                    )
                )
                drawLine(
                    color = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x80FFFFFF),
                    start = Offset(16.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 16.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            .border(1.2.dp, dynamicBorderBrush, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("records_dashboard_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    primaryAccent.copy(alpha = 0.24f),
                                    secondaryAccent.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(1.dp, primaryAccent.copy(alpha = 0.65f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        tint = primaryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = stringResource(R.string.records_dashboard_window_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.records_dashboard_banner_subtitle),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(primaryAccent.copy(alpha = 0.16f))
                        .border(1.dp, primaryAccent.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$totalCount • ${stringResource(R.string.records_dashboard_open)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = primaryAccent
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = primaryAccent.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Dashboard Modal Window containing Total Records, Top Striker, Highest BOOST,
 * Avg Size, and the complete Yearly Strikes Dashboard (Yearly Analytics moved from StrikeScreen).
 */
@Composable
fun RecordsDashboardDialog(
    viewModel: IndividualViewModel,
    allList: List<Individual>,
    currentLang: String,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val isAr = currentLang == "ar"
    val totalCount = allList.size
    val topStriker = allList.maxByOrNull { it.strikeCount }
    val highestBoost = allList.maxByOrNull { it.boost }
    val avgSize = if (allList.isNotEmpty()) {
        (allList.map { it.size }.average() * 10.0).toLong() / 10.0
    } else 0.0

    val yearlyStats by viewModel.yearlyStrikeStats.collectAsStateWithLifecycle()
    val allTimeStrikes by viewModel.allTimeStrikesCount.collectAsStateWithLifecycle()
    val allStrikeEvents by viewModel.allStrikeEvents.collectAsStateWithLifecycle()

    var showYearlyAnalytics by remember { mutableStateOf(false) }

    val dialogBg = if (isDarkTheme) Color(0xF50B1324) else Color(0xFFF8FAFC)
    val dialogBorder = if (isDarkTheme) NeonCyan.copy(alpha = 0.45f) else Color(0xFFCBD5E1)
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val accentViolet = if (isDarkTheme) NeonViolet else DayPurple

    Dialog(
        onDismissRequest = {
            if (showYearlyAnalytics) {
                showYearlyAnalytics = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(dialogBg)
                .border(1.5.dp, dialogBorder, RoundedCornerShape(24.dp))
                .padding(18.dp)
                .testTag("records_dashboard_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (showYearlyAnalytics) {
                            IconButton(
                                onClick = { showYearlyAnalytics = false },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(accentEmerald.copy(alpha = 0.16f))
                                    .border(1.dp, accentEmerald.copy(alpha = 0.55f), CircleShape)
                                    .testTag("dashboard_yearly_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = accentEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentCyan.copy(alpha = 0.16f))
                                    .border(1.dp, accentCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null,
                                    tint = accentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (showYearlyAnalytics) {
                                    stringResource(R.string.yearly_dashboard_title)
                                } else {
                                    stringResource(R.string.records_dashboard_window_title)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (showYearlyAnalytics) {
                                    stringResource(R.string.yearly_analytics)
                                } else {
                                    stringResource(R.string.records_dashboard_banner_subtitle)
                                },
                                fontSize = 11.sp,
                                color = if (showYearlyAnalytics) accentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color(0x1E38BDF8) else Color(0x140284C7))
                            .testTag("close_dashboard_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AnimatedContent(
                    targetState = showYearlyAnalytics,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "dashboard_modal_page_switch"
                ) { showingYearly ->
                    if (!showingYearly) {
                        // Dashboard Overview Page
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Telemetry Row 1: Total Records & Top Striker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassStatSummaryCard(
                                    title = stringResource(R.string.total_records),
                                    value = "$totalCount",
                                    icon = Icons.Default.People,
                                    accentColor = accentCyan,
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_total_records"
                                )
                                GlassStatSummaryCard(
                                    title = stringResource(R.string.top_striker),
                                    value = topStriker?.let { "${it.strikeCount} (${it.fullName.take(6)})" } ?: "—",
                                    icon = Icons.Default.Bolt,
                                    accentColor = accentAmber,
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_top_striker"
                                )
                            }

                            // Telemetry Row 2: Highest BOOST & Avg Size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassStatSummaryCard(
                                    title = stringResource(R.string.highest_boost),
                                    value = highestBoost?.let { "${it.boost}" } ?: "—",
                                    icon = Icons.Default.Speed,
                                    accentColor = accentEmerald,
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_highest_boost"
                                )
                                GlassStatSummaryCard(
                                    title = stringResource(R.string.avg_size),
                                    value = "$avgSize",
                                    icon = Icons.Default.FitnessCenter,
                                    accentColor = accentViolet,
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_avg_size"
                                )
                            }

                            // Yearly Strikes Dashboard Card -> opens complete Yearly Analytics inside Dashboard
                            LiquidGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showYearlyAnalytics = true }
                                    .testTag("dashboard_yearly_strikes_card"),
                                shape = RoundedCornerShape(18.dp),
                                darkTheme = isDarkTheme
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(accentEmerald.copy(alpha = 0.2f))
                                                .border(1.dp, accentEmerald, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Leaderboard,
                                                contentDescription = null,
                                                tint = accentEmerald,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.yearly_dashboard_title),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) {
                                                    "إحصائيات السنوات ($allTimeStrikes ضربة) • اضغط للعرض"
                                                } else {
                                                    "Yearly Analytics ($allTimeStrikes strikes) • Tap to view"
                                                },
                                                fontSize = 11.sp,
                                                color = accentEmerald
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accentCyan.copy(alpha = 0.2f))
                                            .border(1.dp, accentCyan, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (isAr) "عرض اللوحة" else "View Board",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentCyan
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Complete Yearly Strikes Dashboard (Moved Yearly Analytics from StrikeScreen)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 560.dp)
                                .testTag("dashboard_yearly_analytics_list"),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Summary Banner Card
                            item {
                                LiquidGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    darkTheme = isDarkTheme
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(accentEmerald.copy(alpha = 0.2f))
                                                    .border(1.dp, accentEmerald, RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = accentEmerald,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.yearly_analytics),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (isAr) {
                                                        "إحصائيات الضربات مجمّعة حسب كل سنة"
                                                    } else {
                                                        "Strike statistics grouped by year"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = accentEmerald
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$allTimeStrikes",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = accentEmerald
                                            )
                                            Text(
                                                text = stringResource(R.string.all_time_strikes),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Yearly Breakdown Cards
                            if (yearlyStats.isEmpty()) {
                                item {
                                    LiquidGlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        darkTheme = isDarkTheme
                                    ) {
                                        Text(
                                            text = if (isAr) {
                                                "لا توجد ضربات مسجّلة بعد في قاعدة البيانات."
                                            } else {
                                                "No strikes recorded yet in the database."
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                items(yearlyStats, key = { "year_${it.year}" }) { stat ->
                                    YearlyStatCard(stat = stat, darkTheme = isDarkTheme, isAr = isAr)
                                }
                            }

                            // Detailed Historical Strike Log List
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isAr) "سجل الضربات التاريخي المفصّل" else "Detailed Historical Strike Log",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.swipe_to_delete_hint),
                                        fontSize = 11.sp,
                                        color = NeonRose.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            items(allStrikeEvents, key = { "yearly_hist_${it.id}" }) { event ->
                                SwipeableStrikeLogItem(
                                    event = event,
                                    onDelete = { viewModel.deleteStrikeEvent(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearlyStatCard(
    stat: YearlyStrikeStat,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAr: Boolean = true
) {
    val yearBadgeBg = if (darkTheme) Color(0xFF0F2537) else Color(0x2200B4D8)
    val yearBadgeBorder = if (darkTheme) NeonCyan else DayCyan
    val yearTextColor = if (darkTheme) NeonCyan else DayCyan

    val topBadgeBg = if (darkTheme) NeonAmber.copy(alpha = 0.2f) else Color(0x24F59E0B)
    val topBadgeBorder = if (darkTheme) NeonAmber else DayOrange.copy(alpha = 0.5f)
    val topBadgeColor = if (darkTheme) NeonAmber else DayOrange

    val trackColor = if (darkTheme) Color(0x331E293B) else Color(0x35CBD5E1)
    val progressBrush = if (darkTheme) {
        Brush.horizontalGradient(listOf(NeonCyan, NeonEmerald))
    } else {
        Brush.horizontalGradient(listOf(DayCyan, DayEmerald))
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        darkTheme = darkTheme
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(yearBadgeBg)
                            .border(1.5.dp, yearBadgeBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${stat.year}",
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 16.sp,
                            color = yearTextColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAr) "${stat.totalStrikes} ضربة" else "${stat.totalStrikes} strikes",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAr) "${stat.eventCount} عملية تسجيل في السنة" else "${stat.eventCount} recorded events this year",
                            fontSize = 11.sp,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(topBadgeBg)
                        .border(1.dp, topBadgeBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = topBadgeColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stat.topIndividualName,
                            fontSize = 11.sp,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            color = topBadgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Glowing Progress Bar for the year proportion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stat.percentageOfAllTime)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(progressBrush)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isAr) {
                        "الحصة من الإجمالي: ${(stat.percentageOfAllTime * 100).toInt()}%"
                    } else {
                        "Share of total: ${(stat.percentageOfAllTime * 100).toInt()}%"
                    },
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isAr) {
                        "المتصدر: ${stat.topIndividualStrikes} ضربة"
                    } else {
                        "Leader: ${stat.topIndividualStrikes} strikes"
                    },
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                    color = if (darkTheme) NeonEmerald else DayEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
