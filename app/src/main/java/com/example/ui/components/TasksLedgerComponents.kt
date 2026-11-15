package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays the active cycle grant day rule and live countdown timer for Daily, Weekly (Sunday),
 * and Monthly (1st of month) tasks.
 */
@Composable
fun CycleScheduleBanner(
    period: TaskPeriod,
    nowMillis: Long,
    currentLang: String,
    modifier: Modifier = Modifier
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
            NeonCyan
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
            NeonViolet
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
            WarmFireOrange
        )
        else -> Triple("", "", NeonCyan)
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("cycle_schedule_banner_${period.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp)
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
                    color = Color(0xFFCBD5E1),
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
    modifier: Modifier = Modifier
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

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
            shape = RoundedCornerShape(16.dp)
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
                            tint = ElectricMint,
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
                                color = ElectricMint
                            )
                            Text(
                                text = when {
                                    isAr -> "يعرض كل المهام المنجزة والمتبقية ونسبة التقدم لكل مهمة"
                                    isFr -> "Progression exacte de toutes les missions terminées et restantes"
                                    else -> "Live progress tracking across every completed & pending mission"
                                },
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricMint.copy(alpha = 0.16f))
                            .border(1.dp, ElectricMint, RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$completionPercent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricMint
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
                        .background(Color(0x331E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(completionRatio)
                            .height(7.dp)
                            .background(Brush.horizontalGradient(listOf(NeonAqua, ElectricMint)))
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
                        accent = NeonEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    LedgerStatMiniChip(
                        label = if (isAr) "غير المنجزة" else if (isFr) "Restantes" else "Uncompleted",
                        value = "$uncompletedCount",
                        accent = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    LedgerStatMiniChip(
                        label = if (isAr) "جاهزة للجمع" else if (isFr) "À collecter" else "To Collect",
                        value = "$readyCount",
                        accent = NeonAmber,
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
                            color = Color(0xFF64748B)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isAr) "مسح البحث" else "Clear search",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x330B1220),
                        unfocusedContainerColor = Color(0x330B1220)
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
                        accent = NeonAqua,
                        onClick = { statusFilter = 0 }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "المنجزة ($completedCount)" else "Completed ($completedCount)",
                        selected = statusFilter == 1,
                        accent = NeonEmerald,
                        onClick = { statusFilter = 1 }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "غير المنجزة ($uncompletedCount)" else "Uncompleted ($uncompletedCount)",
                        selected = statusFilter == 2,
                        accent = NeonCyan,
                        onClick = { statusFilter = 2 }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "جاهزة للجمع ($readyCount)" else "Ready ($readyCount)",
                        selected = statusFilter == 3,
                        accent = NeonAmber,
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
                        accent = ElectricMint,
                        onClick = { periodFilter = null }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "يومية" else "Daily",
                        selected = periodFilter == TaskPeriod.DAILY,
                        accent = NeonCyan,
                        onClick = { periodFilter = TaskPeriod.DAILY }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "أسبوعية" else "Weekly",
                        selected = periodFilter == TaskPeriod.WEEKLY,
                        accent = NeonViolet,
                        onClick = { periodFilter = TaskPeriod.WEEKLY }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "شهرية" else "Monthly",
                        selected = periodFilter == TaskPeriod.MONTHLY,
                        accent = WarmFireOrange,
                        onClick = { periodFilter = TaskPeriod.MONTHLY }
                    )
                    LedgerFilterPill(
                        label = if (isAr) "الأبدية" else "Eternal",
                        selected = periodFilter == TaskPeriod.YEARLY,
                        accent = NeonAmber,
                        onClick = { periodFilter = TaskPeriod.YEARLY }
                    )
                }
            }
        }

        // Entries List
        if (filteredEntries.isEmpty()) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
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
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "لا توجد مهام مطابقة للتصفية الحالية" else "No tasks match the current filter",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
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
                        onClaimTask = { onClaimTask(entry.task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerTaskCard(
    entry: TaskLedgerEntry,
    nowMillis: Long,
    currentLang: String,
    onClaimTask: () -> Unit
) {
    val isAr = currentLang == "ar"
    val task = entry.task
    val isLocked = task.isLocked(nowMillis)
    val percent = (task.progressFraction * 100f).toInt().coerceIn(0, 100)

    val (periodLabel, periodColor) = when (task.period) {
        TaskPeriod.DAILY -> (if (isAr) "يومية" else "Daily") to NeonCyan
        TaskPeriod.WEEKLY -> (if (isAr) "أسبوعية" else "Weekly") to NeonViolet
        TaskPeriod.MONTHLY -> (if (isAr) "شهرية" else "Monthly") to WarmFireOrange
        TaskPeriod.YEARLY, TaskPeriod.EVERLASTING -> (if (isAr) "الأبدية" else "Eternal") to NeonAmber
    }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_card_${task.id}"),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(periodColor.copy(alpha = 0.16f))
                                .border(0.8.dp, periodColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = periodLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = periodColor
                            )
                        }
                        if (task.isCompleted) {
                            Text(
                                text = if (isAr) "منجزة ✓" else "Completed ✓",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonEmerald
                            )
                        } else if (task.isReadyToClaim) {
                            Text(
                                text = if (isAr) "جاهزة للجمع ✨" else "Ready to Collect ✨",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricMint
                            )
                        } else if (isLocked) {
                            val cd = TaskPools.formatRemainingCountdown(task.unlockAtMillis, nowMillis, currentLang)
                            Text(
                                text = if (isAr) "تفتح بعد $cd" else "Unlocks in $cd",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = task.getLocalizedTitle(currentLang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.getLocalizedDescription(currentLang),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3310B981))
                            .border(1.dp, NeonEmerald, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "+${task.xpBounty} XP",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = NeonEmerald
                        )
                    }
                    Text(
                        text = "$percent%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (task.isCompleted || task.isReadyToClaim) ElectricMint else Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            // Progress Bar + Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x331E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(task.progressFraction)
                            .height(6.dp)
                            .background(
                                when {
                                    task.isCompleted -> NeonEmerald
                                    task.isReadyToClaim -> ElectricMint
                                    else -> periodColor
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${task.currentCount} / ${task.targetCount}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E8F0)
                )
            }

            if (task.isReadyToClaim) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onClaimTask,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricMint,
                        contentColor = Color(0xFF041418)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_ledger_claim_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAr) "جمع المكافأة (+${task.xpBounty} XP)" else "Collect Reward (+${task.xpBounty} XP)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else if (task.isCompleted && entry.completedAtMillis != null) {
                val formattedDate = remember(entry.completedAtMillis) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(entry.completedAtMillis))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isAr) "نُقلت إلى السجل في $formattedDate" else "Recorded in ledger at $formattedDate",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x281E293B))
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
            color = Color(0xFFCBD5E1),
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color(0x261E293B))
            .border(1.dp, if (selected) accent else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else Color(0xFF94A3B8)
        )
    }
}
