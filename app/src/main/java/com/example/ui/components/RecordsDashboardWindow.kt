package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.ui.screens.SwipeableStrikeLogItem
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.ui.viewmodel.YearlyStrikeStat

/**
 * Downward-expanding Liquid Glass Dashboard panel triggered from the Dashboard button next to Filter/Sort.
 * Expands downward smoothly from the top without popping up from the bottom of the screen.
 */
@Composable
fun RecordsDashboardExpandablePanel(
    visible: Boolean,
    viewModel: IndividualViewModel,
    allList: List<Individual>,
    currentLang: String,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
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

    var showYearlyAnalytics by remember { mutableStateOf(false) }
    var pendingDeleteEvent by remember { mutableStateOf<com.example.data.model.StrikeEvent?>(null) }

    LaunchedEffect(visible) {
        if (!visible) {
            showYearlyAnalytics = false
            pendingDeleteEvent = null
        }
    }

    BackHandler(enabled = visible) {
        if (showYearlyAnalytics) {
            showYearlyAnalytics = false
        } else {
            onDismiss()
        }
    }

    val dialogBg = if (isDarkTheme) Color(0xF50B1324) else Color(0xFFF8FAFC)
    val dialogBorder = if (isDarkTheme) NeonCyan.copy(alpha = 0.45f) else Color(0xFFCBD5E1)
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val accentViolet = if (isDarkTheme) NeonViolet else DayPurple

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle scrim below the banner; tapping outside collapses the panel upward
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(160)),
            label = "dashboard_scrim_anim"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isDarkTheme) 0.42f else 0.22f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Panel expanding downwards directly below the Dashboard button
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(180)) + expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.align(Alignment.TopCenter),
            label = "dashboard_dropdown_expand"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .widthIn(max = 560.dp)
                    .shadow(
                        elevation = if (isDarkTheme) 18.dp else 12.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = accentCyan.copy(alpha = 0.22f),
                        spotColor = accentViolet.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(dialogBg)
                    .border(1.3.dp, dialogBorder, RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(16.dp)
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
                        val allStrikeEvents by viewModel.allStrikeEvents.collectAsStateWithLifecycle()
                        val nf = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.US) }

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
                                items(
                                    items = yearlyStats,
                                    key = { "year_${it.year}" },
                                    contentType = { "yearly_stat_card" }
                                ) { stat ->
                                    YearlyStatCard(stat = stat, darkTheme = isDarkTheme, isAr = isAr)
                                }
                            }

                            // Detailed Historical Strike Log List (up to 10,000 strikes)
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (isAr) "سجل الضربات التاريخي المفصّل" else "Detailed Historical Strike Log",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "(${nf.format(allStrikeEvents.size)} / 10,000)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentCyan
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.swipe_to_delete_hint),
                                        fontSize = 11.sp,
                                        color = NeonRose.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            items(
                                items = allStrikeEvents,
                                key = { "yearly_hist_${it.id}" },
                                contentType = { "historical_strike_log" }
                            ) { event ->
                                SwipeableStrikeLogItem(
                                    event = event,
                                    onRequestDelete = { target -> pendingDeleteEvent = target },
                                    darkTheme = isDarkTheme
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    pendingDeleteEvent?.let { targetEvent ->
        com.example.ui.screens.BiometricDeleteStrikeDialog(
            event = targetEvent,
            currentLang = currentLang,
            isDarkTheme = isDarkTheme,
            onDismiss = { pendingDeleteEvent = null },
            onConfirmDelete = { confirmedEvent ->
                viewModel.deleteStrikeEvent(confirmedEvent)
                pendingDeleteEvent = null
            }
        )
    }
}

/**
 * Formats a yearly winner name so long names are capped and suffixed with exactly two dots "..".
 */
fun formatYearlyWinnerName(rawName: String, maxChars: Int = 12): String {
    val trimmed = rawName.trim()
    if (trimmed.length <= maxChars) return trimmed
    return trimmed.take((maxChars - 2).coerceAtLeast(1)).trimEnd() + ".."
}

@Composable
fun YearlyStatCard(
    stat: YearlyStrikeStat,
    darkTheme: Boolean = LocalAppDarkTheme.current,
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
                    Text(
                        text = if (isAr) "${stat.totalStrikes} ضربة" else "${stat.totalStrikes} strikes",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Fixed-size winner container: never changes size; truncates long names with ".."
                var winnerCharLimit by remember(stat.topIndividualName) { mutableIntStateOf(12) }
                val winnerDisplayText = remember(stat.topIndividualName, winnerCharLimit) {
                    formatYearlyWinnerName(stat.topIndividualName, maxChars = winnerCharLimit)
                }

                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(topBadgeBg)
                        .border(1.dp, topBadgeBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = topBadgeColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = winnerDisplayText,
                            fontSize = 11.sp,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            color = topBadgeColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            onTextLayout = { layoutResult ->
                                if (layoutResult.hasVisualOverflow && winnerCharLimit > 4) {
                                    winnerCharLimit -= 1
                                }
                            },
                            modifier = Modifier.weight(1f)
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
