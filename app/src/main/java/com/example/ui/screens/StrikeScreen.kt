package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.components.CosmicStrikeButton
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.StrikerStatus
import com.example.ui.components.StrikerStatusBadge
import com.example.ui.components.DynamicAnimatedProfileCard
import com.example.ui.theme.CaveatHandwriting
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

import com.example.util.HapticHelper

@Composable
fun StrikeScreen(
    viewModel: IndividualViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHapticFeedbackEnabled by viewModel.isHapticFeedbackEnabled
    val currentTarget by viewModel.currentStrikeIndividual.collectAsStateWithLifecycle()
    val allList by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val allStrikeEvents by viewModel.allStrikeEvents.collectAsStateWithLifecycle()
    val yearlyStats by viewModel.yearlyStrikeStats.collectAsStateWithLifecycle()
    val allTimeStrikes by viewModel.allTimeStrikesCount.collectAsStateWithLifecycle()
    val lastStrikeMap by viewModel.individualLastStrikeMap.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

    // Screen Sub-tab: 0 = Arena, 1 = Yearly Dashboard
    var selectedTab by remember { mutableIntStateOf(0) }

    fun triggerHapticFeedback() {
        HapticHelper.triggerStrikeHaptic(context, isHapticFeedbackEnabled)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Switcher: Arena vs Yearly Dashboard
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x221E293B))
                        .border(1.dp, Color(0x3538BDF8), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 0) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (selectedTab == 0) NeonCyan else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp)
                            .testTag("tab_strike_arena"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (selectedTab == 0) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.strike_arena),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 1) NeonEmerald.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (selectedTab == 1) NeonEmerald else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp)
                            .testTag("tab_yearly_dashboard"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = if (selectedTab == 1) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.yearly_analytics),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // ==========================================
                // TAB 0: STRIKE ARENA
                // ==========================================

                // Individual Selector Carousel
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.select_person),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (allList.isEmpty()) {
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_records_desc),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(allList) { individual ->
                                    val isSelected = currentTarget?.id == individual.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x181E293B)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) NeonCyan else Color(0x2838BDF8),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { viewModel.selectStrikeIndividual(individual.id) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            GlassTypeBadge(type = individual.type)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = individual.fullName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = if (isAr) "${individual.strikeCount} ضربة" else "${individual.strikeCount} strikes",
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) NeonAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Target Profile Card
                item {
                    if (currentTarget != null) {
                        val target = currentTarget!!
                        val targetStatus = viewModel.getStatusForIndividual(target, lastStrikeMap)
                        DynamicAnimatedProfileCard(
                            status = targetStatus,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
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
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(NeonCyan.copy(alpha = 0.2f))
                                                .border(1.5.dp, NeonCyan, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = target.fullName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                StrikerStatusBadge(status = targetStatus)
                                            }
                                            Text(
                                                text = if (isAr) "الفئة ${target.type} • الحجم: ${target.size}" else "Type ${target.type} • Size: ${target.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NeonEmerald.copy(alpha = 0.2f))
                                            .border(1.dp, NeonEmerald, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = null,
                                                tint = NeonEmerald,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isAr) "التعزيز ${target.boost}" else "BOOST ${target.boost}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonEmerald
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Live Calculated Score (BOOST * strikes)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x20F59E0B))
                                        .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isAr) "النتيجة: " else "SCORE: ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${target.boost} × ${target.strikeCount}",
                                            fontSize = 11.sp,
                                            color = NeonAmber,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "= ${target.totalScore}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonAmber
                                    )
                                }
                            }
                        }
                    }
                }

                // Hero Tactile STRIKE Button (Auto-saves to database with date, time, year!)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CosmicStrikeButton(
                            onClick = {
                                triggerHapticFeedback()
                                viewModel.triggerStrike(1)
                            },
                            strikeCount = currentTarget?.strikeCount ?: 0,
                            subtitle = stringResource(R.string.strike_action)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Automatic Persistence Notice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2006B6D4))
                                .border(1.dp, Color(0x3506B6D4), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.auto_logged_notice),
                                fontSize = 11.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Recent Auto-Logged Strikes Activity Feed (Infinite Scrollable Log)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.recent_activity),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.swipe_to_delete_hint),
                                    fontSize = 11.sp,
                                    color = NeonRose.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${allStrikeEvents.size})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                if (allStrikeEvents.isEmpty()) {
                    item {
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (isAr) {
                                    "اضغط على زر الضربة لتسجيل أول ضربة تلقائياً بالتاريخ والوقت."
                                } else {
                                    "Tap the Strike button to automatically record your first strike with date & time."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(allStrikeEvents, key = { "recent_${it.id}" }) { event ->
                        SwipeableStrikeLogItem(
                            event = event,
                            onDelete = { viewModel.deleteStrikeEvent(it) }
                        )
                    }
                }

            } else {
                // ==========================================
                // TAB 1: YEARLY STRIKES DASHBOARD
                // ==========================================
                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
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
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NeonEmerald.copy(alpha = 0.2f))
                                            .border(1.dp, NeonEmerald, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = NeonEmerald,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.yearly_dashboard_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "إحصائيات الضربات مجمّعة حسب كل سنة" else "Strike statistics grouped by year",
                                            fontSize = 11.sp,
                                            color = NeonEmerald
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$allTimeStrikes",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonEmerald
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
                }

                // Yearly Breakdown Cards
                if (yearlyStats.isEmpty()) {
                    item {
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
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
                    items(yearlyStats) { stat ->
                        YearlyStatCard(stat = stat, isAr = isAr)
                    }
                }

                // Full Historical Strike Log List
                item {
                    Spacer(modifier = Modifier.height(10.dp))
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

                items(allStrikeEvents, key = { it.id }) { event ->
                    SwipeableStrikeLogItem(
                        event = event,
                        onDelete = { viewModel.deleteStrikeEvent(it) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
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
        shape = RoundedCornerShape(18.dp)
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

@Composable
fun StrikeLogItem(
    event: StrikeEvent,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val cardBg = if (darkTheme) Color(0xE60D1527) else Color(0xF5FFFFFF)
    val cardBorder = if (darkTheme) Color(0x3538BDF8) else Color(0x5500B4D8)
    val boltBg = if (darkTheme) NeonCyan.copy(alpha = 0.2f) else Color(0x2000B4D8)
    val boltColor = if (darkTheme) NeonCyan else DayCyan
    val yearColor = if (darkTheme) NeonAmber else DayOrange
    val countColor = if (darkTheme) NeonEmerald else DayEmerald
    val countBg = if (darkTheme) NeonEmerald.copy(alpha = 0.2f) else Color(0x2010B981)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(boltBg)
                        .border(1.dp, boltColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = boltColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = event.individualName,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${event.date} • ${event.time}",
                            fontSize = 10.sp,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${event.year}]",
                            fontSize = 10.sp,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            color = yearColor
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(countBg)
                    .border(1.dp, countColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+${event.count}",
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    color = countColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableStrikeLogItem(
    event: StrikeEvent,
    onDelete: (StrikeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete(event)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFDC2626).copy(alpha = 0.9f))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.delete),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    ) {
        StrikeLogItem(event = event)
    }
}

