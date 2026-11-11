package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.roadmap.KeyRarity
import com.example.data.roadmap.RewardType
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.RoadmapStation
import com.example.data.roadmap.StationReward
import com.example.data.roadmap.StationState
import com.example.data.roadmap.TaskItem
import com.example.data.roadmap.TaskPeriod
import com.example.data.roadmap.TaskPools
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.CaveatHandwriting
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * 500-Station Wavy Roadmap & Dynamic Randomized Task Engine:
 * - Continuous scrollable sine-wave path connecting 500 progressive stations
 * - Station States: Locked, Unlocked (pulsing egg reward), Claimed (golden neon)
 * - Dynamic Task Engine (Daily, Weekly, Monthly, Yearly)
 * - Cyber Arsenal Modal (Power Hammers, Skip Keys, Shields)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    viewModel: IndividualViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled

    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val claimedStations by viewModel.claimedStations.collectAsStateWithLifecycle()

    val dailyTasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val weeklyTasks by viewModel.weeklyTasks.collectAsStateWithLifecycle()
    val monthlyTasks by viewModel.monthlyTasks.collectAsStateWithLifecycle()
    val yearlyTasks by viewModel.yearlyTasks.collectAsStateWithLifecycle()

    val commonKeys by viewModel.commonKeys.collectAsStateWithLifecycle()
    val normalKeys by viewModel.normalKeys.collectAsStateWithLifecycle()
    val rareKeys by viewModel.rareKeys.collectAsStateWithLifecycle()
    val legendaryKeys by viewModel.legendaryKeys.collectAsStateWithLifecycle()

    val girlBoostHammerExpiresAt by viewModel.girlBoostHammerExpiresAt.collectAsStateWithLifecycle()
    val deflameBoostHammerExpiresAt by viewModel.deflameBoostHammerExpiresAt.collectAsStateWithLifecycle()
    val thorHammerExpiresAt by viewModel.thorHammerExpiresAt.collectAsStateWithLifecycle()

    var selectedMainTab by remember { mutableStateOf(0) } // 0: Wavy Roadmap, 1: Dynamic Tasks
    var selectedTaskPeriod by remember { mutableStateOf(TaskPeriod.DAILY) }
    var showArsenalSheet by remember { mutableStateOf(false) }

    val currentStationId = remember(totalXp) { RoadmapManager.calculateUnlockedStationId(totalXp) }
    val roadmapListState = rememberLazyListState()

    // 500 Stations generation (memoized)
    val stations = remember(claimedStations, totalXp) {
        (1..500).map { id ->
            val reward = TaskPools.getRewardForStation(id)
            val reqXp = TaskPools.getRequiredXpForStation(id)
            val state = RoadmapManager.getStationState(id)
            RoadmapStation(id = id, requiredTotalXp = reqXp, reward = reward, state = state)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "roadmap_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "egg_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("roadmap_screen")
    ) {
        // TOP HEADER & HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x221E293B))
                            .border(1.dp, Color(0x3338BDF8), CircleShape)
                            .testTag("roadmap_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.roadmap_title),
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.roadmap_subtitle),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cyber Arsenal Button
                    IconButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            showArsenalSheet = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x33F59E0B))
                            .border(1.dp, NeonAmber, CircleShape)
                            .testTag("btn_open_arsenal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Arsenal",
                            tint = NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TOP HUD STATS CARD: Station #, Total XP & Next Station Progress
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "المحطة الحالية: $currentStationId / 500",
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                fontSize = 15.sp,
                                color = NeonCyan
                            )
                            val reqCurrent = TaskPools.getRequiredXpForStation(currentStationId)
                            val reqNext = TaskPools.getRequiredXpForStation((currentStationId + 1).coerceAtMost(500))
                            Text(
                                text = "المجموع التراكمي: $totalXp / ${TaskPools.TOTAL_XP_POOL} XP (المحطة التالية: $reqNext XP)",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        // Quick Jump to Current Station
                        Button(
                            onClick = {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                coroutineScope.launch {
                                    val targetIdx = (currentStationId - 2).coerceAtLeast(0)
                                    roadmapListState.animateScrollToItem(targetIdx)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x330284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(text = "قفزة لمحطتي", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val prevStationXp = TaskPools.getRequiredXpForStation(currentStationId)
                    val nextStationXp = TaskPools.getRequiredXpForStation((currentStationId + 1).coerceAtMost(500))
                    val xpInLevel = (totalXp - prevStationXp).coerceAtLeast(0L)
                    val levelRange = (nextStationXp - prevStationXp).coerceAtLeast(1L)
                    val progressFraction = (xpInLevel.toFloat() / levelRange.toFloat()).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x331E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(6.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(NeonCyan, NeonEmerald))
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MAIN TABS: Roadmap vs Dynamic Tasks
            TabRow(
                selectedTabIndex = selectedMainTab,
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                        color = NeonCyan,
                        height = 2.5.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedMainTab == 0,
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        selectedMainTab = 0
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.roadmap_tab_stations),
                                fontWeight = if (selectedMainTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedMainTab == 1,
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        selectedMainTab = 1
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.roadmap_tab_tasks),
                                fontWeight = if (selectedMainTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
            }
        }

        // CONTENT SECTION: TAB 0 (ROADMAP) OR TAB 1 (TASKS)
        if (selectedMainTab == 0) {
            // ==========================================
            // TAB 0: 500-STATION WAVY ROADMAP
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                LazyColumn(
                    state = roadmapListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    items(stations, key = { it.id }) { station ->
                        WavyStationItem(
                            station = station,
                            pulseScale = pulseScale,
                            onClaim = {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                viewModel.claimStation(station.id)
                            }
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // TAB 1: DYNAMIC TASK ENGINE
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                var everlastingFilter by remember { mutableStateOf(0) } // 0: All, 1: Pending, 2: Completed

                // Period Selector Chips (Daily, Weekly, Monthly, Everlasting)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodFilterChip(
                        label = "يومية (5)",
                        isSelected = selectedTaskPeriod == TaskPeriod.DAILY,
                        onClick = { selectedTaskPeriod = TaskPeriod.DAILY }
                    )
                    PeriodFilterChip(
                        label = "أسبوعية (7)",
                        isSelected = selectedTaskPeriod == TaskPeriod.WEEKLY,
                        onClick = { selectedTaskPeriod = TaskPeriod.WEEKLY }
                    )
                    PeriodFilterChip(
                        label = "شهرية (10)",
                        isSelected = selectedTaskPeriod == TaskPeriod.MONTHLY,
                        onClick = { selectedTaskPeriod = TaskPeriod.MONTHLY }
                    )
                    PeriodFilterChip(
                        label = "أهداف أبدية (250)",
                        isSelected = selectedTaskPeriod == TaskPeriod.YEARLY,
                        onClick = { selectedTaskPeriod = TaskPeriod.YEARLY }
                    )
                }

                // Master Goals Header Banner (Displayed when Everlasting tab is active)
                if (selectedTaskPeriod == TaskPeriod.YEARLY) {
                    val completedCount = yearlyTasks.count { it.isCompleted }
                    val totalEverlastingXpEarned = yearlyTasks.filter { it.isCompleted }.sumOf { it.xpBounty }
                    val progressFraction = (completedCount.toFloat() / yearlyTasks.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "مصفوفة الأهداف الأبدية (Master Goals)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = PlusJakartaSans,
                                        color = NeonAmber
                                    )
                                    Text(
                                        text = "250 هدفاً ملحمياً معروضة بالكامل | الإجمالي: 245,350 XP",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x33F59E0B))
                                        .border(1.dp, NeonAmber, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$completedCount / ${yearlyTasks.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonAmber
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0x331E293B))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .height(6.dp)
                                        .background(Brush.horizontalGradient(listOf(NeonAmber, Color(0xFFF97316))))
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Sub-filter chips for 250 master goals
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChipTiny(
                                    label = "الكل (250)",
                                    selected = everlastingFilter == 0,
                                    onClick = { everlastingFilter = 0 }
                                )
                                FilterChipTiny(
                                    label = "قيد الإنجاز (${yearlyTasks.count { !it.isCompleted }})",
                                    selected = everlastingFilter == 1,
                                    onClick = { everlastingFilter = 1 }
                                )
                                FilterChipTiny(
                                    label = "المكتملة ($completedCount)",
                                    selected = everlastingFilter == 2,
                                    onClick = { everlastingFilter = 2 }
                                )
                            }
                        }
                    }
                }

                val currentTasks = when (selectedTaskPeriod) {
                    TaskPeriod.DAILY -> dailyTasks
                    TaskPeriod.WEEKLY -> weeklyTasks
                    TaskPeriod.MONTHLY -> monthlyTasks
                    TaskPeriod.YEARLY -> when (everlastingFilter) {
                        1 -> yearlyTasks.filter { !it.isCompleted }
                        2 -> yearlyTasks.filter { it.isCompleted }
                        else -> yearlyTasks
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            availableKeysForRarity = when (task.requiredKeyRarity) {
                                KeyRarity.COMMON -> commonKeys
                                KeyRarity.NORMAL -> normalKeys
                                KeyRarity.RARE -> rareKeys
                                KeyRarity.LEGENDARY -> legendaryKeys
                            },
                            onSkipWithKey = {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                viewModel.useSkipKeyForTask(task.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // CYBER ARSENAL MODAL SHEET
    if (showArsenalSheet) {
        CyberArsenalModalSheet(
            girlHammerExpires = girlBoostHammerExpiresAt,
            deflameHammerExpires = deflameBoostHammerExpiresAt,
            thorHammerExpires = thorHammerExpiresAt,
            commonKeys = commonKeys,
            normalKeys = normalKeys,
            rareKeys = rareKeys,
            legendaryKeys = legendaryKeys,
            onActivateHammer = { hammerType ->
                viewModel.activateHammer(hammerType)
            },
            onDismiss = { showArsenalSheet = false }
        )
    }
}

/**
 * Wavy Station Item along the vertical sine-wave trajectory
 */
@Composable
fun WavyStationItem(
    station: RoadmapStation,
    pulseScale: Float,
    onClaim: () -> Unit
) {
    // Horizontal Sine-Wave oscillation: -60.dp to +60.dp
    val phase = (station.id * 0.42f)
    val horizontalOffsetDp = (sin(phase) * 60f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Connecting Cyber Conduit to next station along the sine wave
        if (station.id < 500) {
            val nextPhase = ((station.id + 1) * 0.42f)
            val nextOffsetDp = (sin(nextPhase) * 60f).dp
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val startX = size.width / 2f + horizontalOffsetDp.toPx()
                val endX = size.width / 2f + nextOffsetDp.toPx()
                val conduitColor = when (station.state) {
                    StationState.CLAIMED -> Color(0xFFF59E0B).copy(alpha = 0.65f)
                    StationState.UNLOCKED -> Color(0xFF10B981).copy(alpha = 0.85f)
                    StationState.LOCKED -> Color(0x3364748B)
                }
                drawLine(
                    color = conduitColor,
                    start = Offset(startX, 0f),
                    end = Offset(endX, size.height),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(horizontalOffsetDp.roundToPx(), 0) }
                .width(260.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("station_${station.id}"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Station ID Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (station.state) {
                                        StationState.CLAIMED -> Color(0x33F59E0B)
                                        StationState.UNLOCKED -> Color(0x3310B981)
                                        StationState.LOCKED -> Color(0x221E293B)
                                    }
                                )
                                .border(
                                    1.dp,
                                    when (station.state) {
                                        StationState.CLAIMED -> NeonAmber
                                        StationState.UNLOCKED -> NeonEmerald
                                        StationState.LOCKED -> Color(0x3364748B)
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "محطة #${station.id}",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = when (station.state) {
                                    StationState.CLAIMED -> NeonAmber
                                    StationState.UNLOCKED -> NeonEmerald
                                    StationState.LOCKED -> Color(0xFF94A3B8)
                                }
                            )
                        }

                        // Status Icon (Egg, Check, Lock)
                        when (station.state) {
                            StationState.CLAIMED -> {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33F59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                                }
                            }
                            StationState.UNLOCKED -> {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0x3310B981))
                                        .border(1.5.dp, NeonEmerald, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Egg, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                                }
                            }
                            StationState.LOCKED -> {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x2264748B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reward Title & Description
                    Text(
                        text = station.reward.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = station.reward.description,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Button depending on state
                    when (station.state) {
                        StationState.UNLOCKED -> {
                            Button(
                                onClick = onClaim,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .scale(pulseScale)
                                    .testTag("btn_claim_station_${station.id}")
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "جني المكافأة السيبرانية", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Black)
                            }
                        }
                        StationState.CLAIMED -> {
                            Text(
                                text = "تم جني المكافأة بنجاح ✓",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        StationState.LOCKED -> {
                            Text(
                                text = "مقفلة • تتطلب ${station.requiredTotalXp} XP",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Task Item Card in the Dynamic Task Engine
 */
@Composable
fun TaskItemCard(
    task: TaskItem,
    availableKeysForRarity: Int,
    onSkipWithKey: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = task.description,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // XP Bounty Badge
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                            .background(if (task.isCompleted) NeonEmerald else NeonCyan)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${task.currentCount} / ${task.targetCount}",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            if (task.isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مكتملة (+${task.xpBounty} XP حُصدت بنجاح)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تُنجز بالنشاط الفعلي أو بالتجاوز",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedButton(
                        onClick = onSkipWithKey,
                        enabled = availableKeysForRarity > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_skip_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (availableKeysForRarity > 0) NeonAmber else Color.Gray,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تجاوز بمفتاح ($availableKeysForRarity)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (availableKeysForRarity > 0) NeonAmber else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cyber Arsenal & Active Power Hammers Modal Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberArsenalModalSheet(
    girlHammerExpires: Long?,
    deflameHammerExpires: Long?,
    thorHammerExpires: Long?,
    commonKeys: Int,
    normalKeys: Int,
    rareKeys: Int,
    legendaryKeys: Int,
    onActivateHammer: (RewardType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun formatRemaining(expiresAt: Long?): String {
        if (expiresAt == null) return "غير نشطة"
        val remaining = expiresAt - System.currentTimeMillis()
        if (remaining <= 0) return "منتهية الصلاحية"
        val days = remaining / (24 * 3600 * 1000L)
        val hours = (remaining % (24 * 3600 * 1000L)) / (3600 * 1000L)
        val mins = (remaining % (3600 * 1000L)) / (60 * 1000L)
        return "${days}d ${hours}h ${mins}m"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        tonalElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33F59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Gavel, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.roadmap_arsenal_title),
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "المطارق الخارقة ومخزون مفاتيح التجاوز والدروع",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: POWER HAMMERS
            Text(
                text = "المطارق الخارقة (Power Hammers):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Girl Boost Hammer
            HammerCard(
                title = "مطرقة تعزيز Girl (7 أيام)",
                description = "زيادة +50 نقطة لكل ضغطة زر Girl",
                status = formatRemaining(girlHammerExpires),
                isActive = (girlHammerExpires != null && girlHammerExpires > System.currentTimeMillis()),
                badgeColor = Color(0xFFF472B6),
                onActivate = { onActivateHammer(RewardType.GIRL_BOOST_HAMMER_7D) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Deflame Boost Hammer
            HammerCard(
                title = "مطرقة تعزيز Deflame (7 أيام)",
                description = "زيادة +50 نقطة لكل ضغطة زر Deflame",
                status = formatRemaining(deflameHammerExpires),
                isActive = (deflameHammerExpires != null && deflameHammerExpires > System.currentTimeMillis()),
                badgeColor = NeonCyan,
                onActivate = { onActivateHammer(RewardType.DEFLAME_BOOST_HAMMER_7D) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Thor's Ultimate Hammer
            HammerCard(
                title = "مطرقة ثور الأسطورية (30 يوماً)",
                description = "تخفيض 60% من أثر أي ضربة قادمة",
                status = formatRemaining(thorHammerExpires),
                isActive = (thorHammerExpires != null && thorHammerExpires > System.currentTimeMillis()),
                badgeColor = NeonAmber,
                onActivate = { onActivateHammer(RewardType.THOR_HAMMER_30D) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: SKIPPING KEYS INVENTORY
            Text(
                text = stringResource(R.string.roadmap_keys_inventory),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeyInventoryChip(name = "شائع", count = commonKeys, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                KeyInventoryChip(name = "عادي", count = normalKeys, color = NeonCyan, modifier = Modifier.weight(1f))
                KeyInventoryChip(name = "نادر", count = rareKeys, color = Color(0xFFC084FC), modifier = Modifier.weight(1f))
                KeyInventoryChip(name = "أسطوري", count = legendaryKeys, color = NeonAmber, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HammerCard(
    title: String,
    description: String,
    status: String,
    isActive: Boolean,
    badgeColor: Color,
    onActivate: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Text(text = description, fontSize = 10.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isActive) "نشطة (المتبقي: $status)" else "غير نشطة",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) NeonEmerald else Color(0xFF64748B)
                )
            }

            Button(
                onClick = onActivate,
                colors = ButtonDefaults.buttonColors(containerColor = if (isActive) Color(0x3310B981) else Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = if (isActive) "+ تمديد" else "تفعيل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun KeyInventoryChip(name: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x221E293B))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = name, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
            Text(text = "$count", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
fun PeriodFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0x3338BDF8) else Color(0x221E293B))
            .border(1.dp, if (isSelected) NeonCyan else Color(0x20FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) NeonCyan else Color(0xFF94A3B8)
        )
    }
}

@Composable
fun FilterChipTiny(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0x33F59E0B) else Color(0x221E293B))
            .border(1.dp, if (selected) NeonAmber else Color(0x20FFFFFF), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) NeonAmber else Color(0xFF94A3B8)
        )
    }
}

