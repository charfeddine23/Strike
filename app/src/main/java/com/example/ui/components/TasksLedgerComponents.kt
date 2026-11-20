package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.roadmap.TaskItem
import com.example.data.roadmap.TaskLedgerEntry
import com.example.data.roadmap.TaskPeriod
import com.example.data.roadmap.TaskPools
import com.example.data.roadmap.TaskStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TASK_TOGGLE_HOLD_DURATION_MS = 10_000L
private const val TASK_HOLD_ACTIVATION_DELAY_MS = 180L

/**
 * Displays the active cycle grant day rule and live countdown timer for Daily, Weekly (Sunday),
 * and Monthly (1st of month) tasks.
 */
@Composable
fun CycleScheduleBanner(
    period: TaskPeriod,
    nowMillis: Long,
    currentLang: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    if (period == TaskPeriod.YEARLY || period == TaskPeriod.EVERLASTING) return

    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val nextResetMillis = remember(period, nowMillis / 1000L) {
        TaskPools.getNextCycleResetMillis(period, nowMillis)
    }
    val countdown = TaskPools.formatRemainingCountdown(nextResetMillis, nowMillis, currentLang)

    val (title, subtitle, accentColor) = when (period) {
        TaskPeriod.DAILY -> Triple(
            when {
                isAr -> "منح وتجديد اليومي • الساعة 12:00 ليلاً"
                isFr -> "Attribution quotidienne • Minuit (00h00)"
                else -> "Daily Grant & Unlock • Midnight (12:00 AM)"
            },
            when {
                isAr -> "المهام البديلة تفتح في اليوم الجديد بعد: $countdown"
                isFr -> "Nouvelles permissions à minuit dans : $countdown"
                else -> "Replacement slots unlock at midnight in: $countdown"
            },
            if (isDarkTheme) NeonCyan else DayCyan
        )
        TaskPeriod.WEEKLY -> Triple(
            when {
                isAr -> "يوم منح مهام 7 أيام • كل يوم أحد (12:00 ليلاً)"
                isFr -> "Jour d'attribution (7 jours) • Chaque Dimanche"
                else -> "7-Day Weekly Grant Day • Every Sunday (12:00 AM)"
            },
            when {
                isAr -> "تُمنح مهام الأسبوع كل أحد • المتبقي: $countdown"
                isFr -> "Ouverture chaque dimanche dans : $countdown"
                else -> "Weekly missions grant every Sunday • Next: $countdown"
            },
            if (isDarkTheme) NeonViolet else DayPurple
        )
        TaskPeriod.MONTHLY -> Triple(
            when {
                isAr -> "يوم منح مهام الشهر • يوم 1 من كل شهر (12:00 ليلاً)"
                isFr -> "Jour d'attribution mensuel • 1er du mois"
                else -> "Monthly Grant Day • 1st of Every Month (12:00 AM)"
            },
            when {
                isAr -> "تُمنح المهام الشهرية في 1 من الشهر • المتبقي: $countdown"
                isFr -> "Ouverture le 1er du mois dans : $countdown"
                else -> "Monthly missions grant on the 1st • Next: $countdown"
            },
            if (isDarkTheme) WarmFireOrange else DayOrange
        )
        else -> Triple("", "", if (isDarkTheme) NeonCyan else DayCyan)
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("cycle_schedule_banner_${period.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        darkTheme = isDarkTheme
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = accentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isDarkTheme) Color(0xFFCBD5E1) else DayTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Full 1,000-Task Logbook ("دفتر المهام المنجزة وغير المنجزة"):
 * Shows EVERY task across Daily, Weekly, Monthly, and Eternal pools along with live progress bars,
 * percentages, completion dates, and instant Claim ("جمع") actions for ready tasks.
 */
@Composable
fun TasksLedgerSection(
    entries: List<TaskLedgerEntry>,
    nowMillis: Long,
    currentLang: String,
    onClaimTask: (String) -> Unit,
    onToggleTaskCompletion: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val mintTone = if (isDarkTheme) ElectricMint else DayEmerald
    val emeraldTone = if (isDarkTheme) NeonEmerald else DayEmerald
    val cyanTone = if (isDarkTheme) NeonCyan else DayCyan
    val aquaTone = if (isDarkTheme) NeonAqua else DayCyan
    val violetTone = if (isDarkTheme) NeonViolet else DayPurple
    val orangeTone = if (isDarkTheme) WarmFireOrange else DayOrange
    val amberTone = if (isDarkTheme) NeonAmber else DayOrange
    val descColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    var searchQuery by remember { mutableStateOf("") }
    // 0 = All, 1 = Completed, 2 = Uncompleted, 3 = Ready To Claim
    var statusFilter by remember { mutableIntStateOf(0) }
    // null = All periods, or specific TaskPeriod filter
    var periodFilter by remember { mutableStateOf<TaskPeriod?>(null) }

    val completedCount = remember(entries) { entries.count { it.task.isCompleted } }
    val readyCount = remember(entries) { entries.count { it.task.isReadyToClaim } }
    val uncompletedCount = entries.size - completedCount
    val completionRatio = (completedCount.toFloat() / entries.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val completionPercent = (completionRatio * 100f).toInt()

    val filteredEntries = remember(entries, searchQuery, statusFilter, periodFilter) {
        val q = searchQuery.trim().lowercase()
        entries.filter { entry ->
            val task = entry.task
            val matchesStatus = when (statusFilter) {
                1 -> task.isCompleted
                2 -> !task.isCompleted
                3 -> task.isReadyToClaim
                else -> true
            }
            val matchesPeriod = when (periodFilter) {
                null -> true
                TaskPeriod.YEARLY, TaskPeriod.EVERLASTING ->
                    task.period == TaskPeriod.YEARLY || task.period == TaskPeriod.EVERLASTING
                else -> task.period == periodFilter
            }
            val matchesSearch = q.isEmpty() ||
                task.taskCode.lowercase().contains(q) ||
                task.title.lowercase().contains(q) ||
                task.titleEn.lowercase().contains(q) ||
                task.description.lowercase().contains(q) ||
                task.descriptionEn.lowercase().contains(q)
            matchesStatus && matchesPeriod && matchesSearch
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

    Column(modifier = modifier.fillMaxSize()) {
        // Overview Card
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .testTag("tasks_ledger_summary_card"),
            shape = RoundedCornerShape(16.dp),
            darkTheme = isDarkTheme
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = mintTone,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = when {
                                    isAr -> "دفتر المهام الشامل (المنجزة وغير المنجزة)"
                                    isFr -> "Registre complet des missions"
                                    else -> "Complete Tasks Ledger (All 1,000 Goals)"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = mintTone
                            )
                            Text(
                                text = when {
                                    isAr -> "يعرض كل المهام المنجزة والمتبقية ونسبة التقدم لكل مهمة"
                                    isFr -> "Progression exacte de toutes les missions terminées et restantes"
                                    else -> "Live progress tracking across every completed & pending mission"
                                },
                                fontSize = 10.sp,
                                color = descColor
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(mintTone.copy(alpha = 0.16f))
                            .border(1.dp, mintTone, RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$completionPercent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = mintTone
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Overall Gradient Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDarkTheme) Color(0x331E293B) else DayGlassSurfaceElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(completionRatio)
                            .height(7.dp)
                            .background(Brush.horizontalGradient(listOf(aquaTone, mintTone)))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // KPI Summary Pills Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LedgerStatMiniChip(
                        label = if (isAr) "المنجزة" else if (isFr) "Terminées" else "Completed",
                        value = "$completedCount",
                        accent = emeraldTone,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    LedgerStatMiniChip(
                        label = if (isAr) "غير المنجزة" else if (isFr) "Restantes" else "Uncompleted",
                        value = "$uncompletedCount",
                        accent = cyanTone,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    LedgerStatMiniChip(
                        label = if (isAr) "جاهزة للجمع" else if (isFr) "À collecter" else "To Collect",
                        value = "$readyCount",
                        accent = amberTone,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isAr) "ابحث في 1000 مهمة..." else "Search across all 1,000 tasks...",
                            fontSize = 12.sp,
                            color = descColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = cyanTone,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isAr) "مسح البحث" else "Clear search",
                                    tint = descColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cyanTone,
                        unfocusedBorderColor = if (isDarkTheme) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                        focusedTextColor = if (isDarkTheme) Color.White else DayTextPrimary,
                        unfocusedTextColor = if (isDarkTheme) Color.White else DayTextPrimary,
                        focusedContainerColor = if (isDarkTheme) Color(0x330B1220) else DayGlassSurface,
                        unfocusedContainerColor = if (isDarkTheme) Color(0x330B1220) else DayGlassSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LedgerFilterPill(
                        label = if (isAr) "الكل (${entries.size})" else "All (${entries.size})",
                        selected = statusFilter == 0,
                        accent = aquaTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { statusFilter = 0 }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "المنجزة ($completedCount)" else "Completed ($completedCount)",
                        selected = statusFilter == 1,
                        accent = emeraldTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { statusFilter = 1 }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "غير المنجزة ($uncompletedCount)" else "Uncompleted ($uncompletedCount)",
                        selected = statusFilter == 2,
                        accent = cyanTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { statusFilter = 2 }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "جاهزة للجمع ($readyCount)" else "Ready ($readyCount)",
                        selected = statusFilter == 3,
                        accent = amberTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { statusFilter = 3 }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Period Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LedgerFilterPill(
                        label = if (isAr) "كل الفئات" else "All Periods",
                        selected = periodFilter == null,
                        accent = mintTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { periodFilter = null }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "يومية" else "Daily",
                        selected = periodFilter == TaskPeriod.DAILY,
                        accent = cyanTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { periodFilter = TaskPeriod.DAILY }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "أسبوعية" else "Weekly",
                        selected = periodFilter == TaskPeriod.WEEKLY,
                        accent = violetTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { periodFilter = TaskPeriod.WEEKLY }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "شهرية" else "Monthly",
                        selected = periodFilter == TaskPeriod.MONTHLY,
                        accent = orangeTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { periodFilter = TaskPeriod.MONTHLY }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "الأبدية" else "Eternal",
                        selected = periodFilter == TaskPeriod.YEARLY,
                        accent = amberTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { periodFilter = TaskPeriod.YEARLY }
                    )
                }
            }
        }

        // Entries List
        if (filteredEntries.isEmpty()) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                darkTheme = isDarkTheme
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = descColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "لا توجد مهام مطابقة للتصفية الحالية" else "No tasks match the current filter",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = descColor
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tasks_ledger_list"),
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEntries, key = { it.task.id }) { entry ->
                    LedgerTaskCard(
                        entry = entry,
                        nowMillis = nowMillis,
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        onClaimTask = { onClaimTask(entry.task.id) },
                        onLongPressToggle = { onToggleTaskCompletion(entry.task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerTaskCard(
    entry: TaskLedgerEntry,
    nowMillis: Long,
    currentLang: String,
    onClaimTask: () -> Unit,
    onLongPressToggle: (() -> Unit)? = null,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val task = entry.task
    val isLocked = task.isLocked(nowMillis)
    val percent = (task.progressFraction * 100f).toInt().coerceIn(0, 100)

    val mintTone = if (isDarkTheme) ElectricMint else DayEmerald
    val emeraldTone = if (isDarkTheme) NeonEmerald else DayEmerald
    val cyanTone = if (isDarkTheme) NeonCyan else DayCyan
    val violetTone = if (isDarkTheme) NeonViolet else DayPurple
    val orangeTone = if (isDarkTheme) WarmFireOrange else DayOrange
    val amberTone = if (isDarkTheme) NeonAmber else DayOrange

    val (periodLabel, periodColor) = when (task.period) {
        TaskPeriod.DAILY -> (if (isAr) "يومية" else if (isFr) "Quotidienne" else "Daily") to cyanTone
        TaskPeriod.WEEKLY -> (if (isAr) "أسبوعية" else if (isFr) "Hebdo" else "Weekly") to violetTone
        TaskPeriod.MONTHLY -> (if (isAr) "شهرية" else if (isFr) "Mensuelle" else "Monthly") to orangeTone
        TaskPeriod.YEARLY, TaskPeriod.EVERLASTING -> (if (isAr) "الأبدية" else if (isFr) "Éternelle" else "Eternal") to amberTone
    }

    var holdProgress by remember(task.id) { mutableFloatStateOf(0f) }
    var isHolding by remember(task.id) { mutableStateOf(false) }
    val latestOnToggle by rememberUpdatedState(onLongPressToggle)
    val holdHighlightColor = if (task.isCompleted) amberTone else mintTone
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val descColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary
    val countColor = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextPrimary
    val mutedColor = if (isDarkTheme) Color(0xFF64748B) else DayTextSecondary

    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (isHolding && holdProgress > 0f) {
                Modifier.border(1.6.dp, holdHighlightColor, RoundedCornerShape(12.dp))
            } else {
                Modifier
            }
        )
        .then(
            if (onLongPressToggle != null) {
                Modifier.pointerInput(task.id, task.isCompleted) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val pointerId = down.id
                        val startPos = down.position
                        val startTime = System.currentTimeMillis()
                        val touchSlop = viewConfiguration.touchSlop
                        isHolding = true
                        holdProgress = 0f

                        try {
                            var triggered = false
                            while (!triggered) {
                                val elapsed = System.currentTimeMillis() - startTime
                                if (elapsed >= TASK_TOGGLE_HOLD_DURATION_MS) {
                                    holdProgress = 1f
                                    triggered = true
                                    latestOnToggle?.invoke()
                                    break
                                } else if (elapsed >= TASK_HOLD_ACTIVATION_DELAY_MS) {
                                    holdProgress = ((elapsed - TASK_HOLD_ACTIVATION_DELAY_MS).toFloat() /
                                        (TASK_TOGGLE_HOLD_DURATION_MS - TASK_HOLD_ACTIVATION_DELAY_MS).toFloat())
                                        .coerceIn(0f, 1f)
                                }

                                val event = withTimeoutOrNull(45L) { awaitPointerEvent() }
                                if (event != null) {
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                        ?: event.changes.firstOrNull()
                                    if (change == null || !change.pressed ||
                                        (change.position - startPos).getDistance() > touchSlop
                                    ) {
                                        break
                                    }
                                }
                            }
                        } finally {
                            isHolding = false
                            holdProgress = 0f
                        }
                    }
                }
            } else {
                Modifier
            }
        )
        .testTag("ledger_card_${task.id}")

    LiquidGlassCard(
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        darkTheme = isDarkTheme
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(periodColor.copy(alpha = if (isDarkTheme) 0.16f else 0.12f))
                                .border(0.8.dp, periodColor.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = periodLabel,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = periodColor
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isDarkTheme) Color(0x331E293B) else DayGlassSurfaceElevated)
                                .border(
                                    0.8.dp,
                                    if (isDarkTheme) Color(0x40FFFFFF) else Color(0xFFCBD5E1),
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                .testTag("ledger_task_id_badge_${task.id}")
                        ) {
                            Text(
                                text = task.taskCode,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextPrimary
                            )
                        }
                        if (task.isCompleted) {
                            Text(
                                text = if (isAr) "منجزة ✓" else if (isFr) "Terminée ✓" else "Completed ✓",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = emeraldTone
                            )
                        } else if (task.isReadyToClaim) {
                            Text(
                                text = if (isAr) "جاهزة للجمع ✨" else if (isFr) "Prête ✨" else "Ready ✨",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = mintTone
                            )
                        } else if (isLocked) {
                            val cd = TaskPools.formatRemainingCountdown(task.unlockAtMillis, nowMillis, currentLang)
                            Text(
                                text = if (isAr) "تفتح بعد $cd" else "Unlocks in $cd",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = amberTone
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = task.getLocalizedTitle(currentLang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.getLocalizedDescription(currentLang),
                        fontSize = 10.sp,
                        color = descColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(emeraldTone.copy(alpha = if (isDarkTheme) 0.20f else 0.14f))
                            .border(0.8.dp, emeraldTone, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${task.xpBounty} XP",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = emeraldTone
                        )
                    }
                    Text(
                        text = "$percent%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (task.isCompleted || task.isReadyToClaim) mintTone else descColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar + Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDarkTheme) Color(0x331E293B) else DayGlassSurfaceElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(task.progressFraction)
                            .height(5.dp)
                            .background(
                                when {
                                    task.isCompleted -> emeraldTone
                                    task.isReadyToClaim -> mintTone
                                    else -> periodColor
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${task.currentCount} / ${task.targetCount}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = countColor
                )
            }

            if (task.isReadyToClaim) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onClaimTask,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = mintTone,
                        contentColor = if (isDarkTheme) Color(0xFF041418) else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("btn_ledger_claim_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isAr) "جمع المكافأة (+${task.xpBounty} XP)" else "Collect (+${task.xpBounty} XP)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else if (task.isCompleted && entry.completedAtMillis != null) {
                val formattedDate = remember(entry.completedAtMillis) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(entry.completedAtMillis))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = emeraldTone,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isAr) "نُقلت إلى السجل في $formattedDate" else "Recorded in ledger at $formattedDate",
                        fontSize = 9.5.sp,
                        color = descColor
                    )
                }
            }

            if (isHolding && holdProgress > 0f) {
                val heldSeconds = (holdProgress * 10f).coerceIn(0f, 10f)
                val formattedHeld = String.format(Locale.US, "%.1f", heldSeconds)
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(holdHighlightColor.copy(alpha = 0.12f))
                        .border(0.8.dp, holdHighlightColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("ledger_hold_progress_${task.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isAr -> if (task.isCompleted) {
                                    "استمر بالضغط لإرجاعها غير منجزة (XP محفوظ)"
                                } else {
                                    "استمر بالضغط لإنجازها (XP محفوظ)"
                                }
                                isFr -> if (task.isCompleted) {
                                    "Maintenez pour réactiver (XP gardé)"
                                } else {
                                    "Maintenez pour terminer (XP gardé)"
                                }
                                else -> if (task.isCompleted) {
                                    "Holding to mark uncompleted (XP kept)"
                                } else {
                                    "Holding to mark completed (XP kept)"
                                }
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = holdHighlightColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${formattedHeld}s / 10s",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            color = holdHighlightColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDarkTheme) Color(0x401E293B) else DayGlassSurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(holdProgress)
                                .height(5.dp)
                                .background(holdHighlightColor)
                        )
                    }
                }
            } else if (onLongPressToggle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = mutedColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = when {
                            isAr -> "ضغط مطوّل 10ث: تبديل الحالة دون مساس بـ XP"
                            isFr -> "Appui long 10s : basculer l'état (XP conservé)"
                            else -> "Hold 10s to toggle status (earned XP intact)"
                        },
                        fontSize = 9.sp,
                        color = mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerStatMiniChip(
    label: String,
    value: String,
    accent: Color,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDarkTheme) Color(0x281E293B) else DayGlassSurfaceElevated)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(vertical = 7.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = accent
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isDarkTheme) Color(0xFFCBD5E1) else DayTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LedgerFilterPill(
    label: String,
    selected: Boolean,
    accent: Color,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) accent.copy(alpha = if (isDarkTheme) 0.22f else 0.14f)
                else if (isDarkTheme) Color(0x261E293B) else DayGlassSurface
            )
            .border(
                1.dp,
                if (selected) accent else if (isDarkTheme) Color(0x22FFFFFF) else Color(0xFFCBD5E1),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary
        )
    }
}
