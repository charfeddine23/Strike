package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.TaskLedgerEntry
import com.example.data.roadmap.TaskPeriod
import com.example.data.roadmap.TaskPools
import com.example.ui.components.ElectricMint
import com.example.ui.components.LedgerTaskCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NeonAqua
import com.example.ui.components.RoadmapBiomeBackdrop
import com.example.ui.components.RoadmapCommandDock
import com.example.ui.components.WarmFireOrange
import com.example.ui.components.roadmapBiomeStateForStation
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayGlassSurface
import com.example.ui.theme.DayGlassSurfaceElevated
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.delay

/**
 * Full-screen dedicated Tasks Ledger ("دفتر المهام"):
 * 1. Top bar: Title "دفتر المهام" with Back action & overall badge
 * 2. Two stat containers: Completed Tasks count & Uncompleted Tasks count
 * 3. Status filter row: منجزة | غير منجزة | الكل
 * 4. Period filter row: اليومية | الأسبوعية | الشهرية | الأبدية | الكل
 * 5. Expanded task list occupying the remaining vertical space
 */
@Composable
fun TasksLedgerScreen(
    viewModel: IndividualViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    hostedWithFullBleedBiome: Boolean = false,
    onOpenXpLogs: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val dailyTasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val weeklyTasks by viewModel.weeklyTasks.collectAsStateWithLifecycle()
    val monthlyTasks by viewModel.monthlyTasks.collectAsStateWithLifecycle()
    val yearlyTasks by viewModel.yearlyTasks.collectAsStateWithLifecycle()
    val completedTaskIds by viewModel.completedTaskIds.collectAsStateWithLifecycle()
    val completedTaskTimestamps by viewModel.completedTaskTimestamps.collectAsStateWithLifecycle()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        viewModel.refreshTaskCycles()
        while (true) {
            val tick = System.currentTimeMillis()
            nowMillis = tick
            val expired = (viewModel.dailyTasks.value + viewModel.weeklyTasks.value + viewModel.monthlyTasks.value)
                .any { it.unlockAtMillis in 1L..tick }
            if (expired) {
                viewModel.refreshTaskCycles()
            }
            delay(1000L)
        }
    }

    val entries = remember(
        dailyTasks,
        weeklyTasks,
        monthlyTasks,
        yearlyTasks,
        completedTaskIds,
        completedTaskTimestamps
    ) {
        RoadmapManager.getTasksLedgerSnapshot()
    }

    val completedCount = remember(entries) { entries.count { it.task.isCompleted } }
    val uncompletedCount = entries.size - completedCount

    // 0 = الكل (All), 1 = منجزة (Completed), 2 = غير منجزة (Uncompleted)
    var statusFilter by remember { mutableIntStateOf(0) }
    // null = الكل (All), or specific TaskPeriod
    var periodFilter by remember { mutableStateOf<TaskPeriod?>(null) }

    val filteredEntries = remember(entries, statusFilter, periodFilter) {
        entries.filter { entry ->
            val task = entry.task
            val matchesStatus = when (statusFilter) {
                1 -> task.isCompleted
                2 -> !task.isCompleted
                else -> true
            }
            val matchesPeriod = when (periodFilter) {
                null -> true
                TaskPeriod.YEARLY, TaskPeriod.EVERLASTING ->
                    task.period == TaskPeriod.YEARLY || task.period == TaskPeriod.EVERLASTING
                else -> task.period == periodFilter
            }
            matchesStatus && matchesPeriod
        }.sortedWith(
            compareBy<TaskLedgerEntry> { !it.task.isReadyToClaim }
                .thenBy { !it.task.isCompleted }
                .thenByDescending { it.completedAtMillis ?: 0L }
                .thenByDescending { it.task.currentCount }
                .thenBy { it.task.period.ordinal }
                .thenBy { it.task.difficultyTier }
                .thenBy { it.task.xpBounty }
        )
    }

    val emeraldTone = if (isDarkTheme) NeonEmerald else DayEmerald
    val mintTone = if (isDarkTheme) ElectricMint else DayEmerald
    val cyanTone = if (isDarkTheme) NeonCyan else DayCyan
    val violetTone = if (isDarkTheme) NeonViolet else DayPurple
    val orangeTone = if (isDarkTheme) WarmFireOrange else DayOrange
    val amberTone = if (isDarkTheme) NeonAmber else DayOrange
    val aquaTone = if (isDarkTheme) NeonAqua else DayCyan
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val stageProgress = remember(totalXp) { RoadmapManager.getStageProgress(totalXp) }
    val safeStationId = stageProgress.unlockedStationId.coerceIn(0, TaskPools.TOTAL_STATIONS_COUNT)
    val stageFraction = (stageProgress.currentStageXp.toFloat() / stageProgress.xpRequiredForNextStage.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val biomeState = remember(safeStationId) { roadmapBiomeStateForStation(safeStationId.toFloat().coerceAtLeast(1f)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tasks_ledger_screen")
    ) {
        if (!hostedWithFullBleedBiome) {
            RoadmapBiomeBackdrop(
                biomeState = biomeState,
                driftPhase = 0f,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0x40060B14) else Color(0x2EFFFFFF))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            RoadmapCommandDock(
                activeSlot = 0,
                safeStationId = safeStationId,
                totalStationsCount = TaskPools.TOTAL_STATIONS_COUNT,
                stageProgressFraction = stageFraction,
                biomeState = biomeState,
                currentLang = currentLang,
                onOpenTasksLedger = {},
                onOpenProgressOrRoadmap = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onNavigateBack()
                },
                onOpenXpLogs = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onOpenXpLogs()
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // 1. TOP TITLE BAR: Back Button + "دفتر المهام" + Count Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color(0xBB161E31) else DayGlassSurface)
                            .border(1.dp, if (isDarkTheme) Color(0x40FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                            .testTag("ledger_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isAr) "رجوع" else if (isFr) "Retour" else "Back",
                            tint = titleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = mintTone,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = when {
                                isAr -> "دفتر المهام"
                                isFr -> "Registre des missions"
                                else -> "Tasks Ledger"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("ledger_screen_title")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(mintTone.copy(alpha = 0.14f))
                        .border(1.dp, mintTone.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$completedCount / ${entries.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = mintTone
                    )
                }
            }

            // 2. TWO SUMMARY CONTAINERS: Completed Tasks & Uncompleted Tasks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LedgerCountContainer(
                    count = completedCount,
                    label = when {
                        isAr -> "المهام المنجزة"
                        isFr -> "Missions terminées"
                        else -> "Completed Tasks"
                    },
                    icon = Icons.Default.CheckCircle,
                    accentColor = emeraldTone,
                    selected = statusFilter == 1,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_stat_completed",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        statusFilter = if (statusFilter == 1) 0 else 1
                    },
                    modifier = Modifier.weight(1f)
                )

                LedgerCountContainer(
                    count = uncompletedCount,
                    label = when {
                        isAr -> "المهام غير المنجزة"
                        isFr -> "Missions restantes"
                        else -> "Uncompleted Tasks"
                    },
                    icon = Icons.Default.HourglassTop,
                    accentColor = cyanTone,
                    selected = statusFilter == 2,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_stat_uncompleted",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        statusFilter = if (statusFilter == 2) 0 else 2
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. STATUS FILTER ROW: منجزة | غير منجزة | الكل
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedgerFilterSegment(
                    label = when {
                        isAr -> "منجزة"
                        isFr -> "Terminées"
                        else -> "Completed"
                    },
                    selected = statusFilter == 1,
                    accentColor = emeraldTone,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_filter_completed",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        statusFilter = 1
                    },
                    modifier = Modifier.weight(1f)
                )
                LedgerFilterSegment(
                    label = when {
                        isAr -> "غير منجزة"
                        isFr -> "Restantes"
                        else -> "Uncompleted"
                    },
                    selected = statusFilter == 2,
                    accentColor = cyanTone,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_filter_uncompleted",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        statusFilter = 2
                    },
                    modifier = Modifier.weight(1f)
                )
                LedgerFilterSegment(
                    label = when {
                        isAr -> "الكل"
                        isFr -> "Tout"
                        else -> "All"
                    },
                    selected = statusFilter == 0,
                    accentColor = mintTone,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_filter_all",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        statusFilter = 0
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. PERIOD FILTER ROW: اليومية | الأسبوعية | الشهرية | الأبدية | الكل
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LedgerPeriodChip(
                    label = when {
                        isAr -> "اليومية"
                        isFr -> "Quotidiennes"
                        else -> "Daily"
                    },
                    selected = periodFilter == TaskPeriod.DAILY,
                    accentColor = cyanTone,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_period_daily",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        periodFilter = TaskPeriod.DAILY
                    }
                )
                LedgerPeriodChip(
                    label = when {
                        isAr -> "الأسبوعية"
                        isFr -> "Hebdo"
                        else -> "Weekly"
                    },
                    selected = periodFilter == TaskPeriod.WEEKLY,
                    accentColor = violetTone,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_period_weekly",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        periodFilter = TaskPeriod.WEEKLY
                    }
                )
                LedgerPeriodChip(
                    label = when {
                        isAr -> "الشهرية"
                        isFr -> "Mensuelles"
                        else -> "Monthly"
                    },
                    selected = periodFilter == TaskPeriod.MONTHLY,
                    accentColor = orangeTone,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_period_monthly",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        periodFilter = TaskPeriod.MONTHLY
                    }
                )
                LedgerPeriodChip(
                    label = when {
                        isAr -> "الأبدية"
                        isFr -> "Éternelles"
                        else -> "Eternal"
                    },
                    selected = periodFilter == TaskPeriod.YEARLY,
                    accentColor = amberTone,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_period_eternal",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        periodFilter = TaskPeriod.YEARLY
                    }
                )
                LedgerPeriodChip(
                    label = when {
                        isAr -> "الكل"
                        isFr -> "Toutes"
                        else -> "All"
                    },
                    selected = periodFilter == null,
                    accentColor = aquaTone,
                    isDarkTheme = isDarkTheme,
                    testTag = "ledger_period_all",
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        periodFilter = null
                    }
                )
            }

            // 5. EXPANDED TASKS DISPLAY AREA (Occupies maximum screen space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredEntries.isEmpty()) {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        darkTheme = isDarkTheme
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = mutedColor,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when {
                                    isAr -> "لا توجد مهام مطابقة للتصفية الحالية"
                                    isFr -> "Aucune mission ne correspond au filtre"
                                    else -> "No tasks match the active filter"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = mutedColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("tasks_ledger_list"),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filteredEntries, key = { it.task.id }) { entry ->
                            LedgerTaskCard(
                                entry = entry,
                                nowMillis = nowMillis,
                                currentLang = currentLang,
                                isDarkTheme = isDarkTheme,
                                onClaimTask = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    viewModel.claimTaskReward(entry.task.id)
                                },
                                onLongPressToggle = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    viewModel.toggleTaskCompletionLedger(entry.task.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerCountContainer(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    selected: Boolean,
    isDarkTheme: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = if (selected) 0.24f else 0.12f),
                Color(0xCC0D1525)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                if (selected) accentColor.copy(alpha = 0.14f) else DayGlassSurface,
                if (selected) accentColor.copy(alpha = 0.08f) else DayGlassSurfaceElevated
            )
        )
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accentColor else accentColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = accentColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LedgerFilterSegment(
    label: String,
    selected: Boolean,
    accentColor: Color,
    isDarkTheme: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unselectedBg = if (isDarkTheme) Color(0x33162033) else DayGlassSurface
    val unselectedBorder = if (isDarkTheme) Color(0x28FFFFFF) else Color(0xFFCBD5E1)
    val unselectedText = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.14f) else unselectedBg)
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = if (selected) accentColor else unselectedBorder,
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (selected) accentColor else unselectedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LedgerPeriodChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    isDarkTheme: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val unselectedBg = if (isDarkTheme) Color(0x33162033) else DayGlassSurface
    val unselectedBorder = if (isDarkTheme) Color(0x26FFFFFF) else Color(0xFFCBD5E1)
    val unselectedText = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.14f) else unselectedBg)
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = if (selected) accentColor else unselectedBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = if (selected) accentColor else unselectedText,
            maxLines = 1
        )
    }
}
