package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.profile.SkipKeyType
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.RoadmapStation
import com.example.data.roadmap.TaskItem
import com.example.data.roadmap.TaskPeriod
import com.example.data.roadmap.TaskPools
import com.example.data.roadmap.TaskStatus
import com.example.ui.components.ElectricMint
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NeonAqua
import com.example.ui.components.RoadmapAmbientBackground
import com.example.ui.components.RoadmapProgressHeader
import com.example.ui.components.WavySnakeStationRow
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 500-Station Wavy Roadmap & Dynamic Randomized Task Engine:
 * - Continuous scrollable Bezier/sine-wave snake path connecting 500 progressive stations
 * - Deep OLED dark background (#0B0F19 -> #121826) with ambient floating glow dots
 * - Frosted glassmorphism HUD & station cards with glowing cyan (#00F0FF), electric mint (#00FF88),
 *   and warm fire orange (#FF8800) milestone accents
 * - Dynamic Task Engine (Daily, Weekly, Monthly, Annual) using Inventory Skip-Keys
 */
@Composable
fun RoadmapScreen(
    viewModel: IndividualViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    initialMainTab: Int = 0,
    onMainTabChanged: (Int) -> Unit = {},
    onOpenTasksLedger: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val claimedStations by viewModel.claimedStations.collectAsStateWithLifecycle()

    val dailyTasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val weeklyTasks by viewModel.weeklyTasks.collectAsStateWithLifecycle()
    val monthlyTasks by viewModel.monthlyTasks.collectAsStateWithLifecycle()
    val yearlyTasks by viewModel.yearlyTasks.collectAsStateWithLifecycle()

    val woodenKeys by viewModel.woodenKeys.collectAsStateWithLifecycle()
    val bronzeKeys by viewModel.bronzeKeys.collectAsStateWithLifecycle()
    val silverKeys by viewModel.silverKeys.collectAsStateWithLifecycle()

    var selectedMainTab by remember(initialMainTab) { mutableStateOf(initialMainTab) } // 0: Roadmap, 1: Tasks, 2: Elite Missions
    var selectedTaskPeriod by remember { mutableStateOf(TaskPeriod.DAILY) }

    BackHandler(enabled = selectedMainTab != 0) {
        selectedMainTab = 0
        onMainTabChanged(0)
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(selectedMainTab) {
        if (selectedMainTab == 1) {
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
    }

    val stageProgress = remember(totalXp) { RoadmapManager.getStageProgress(totalXp) }
    val currentStationId = stageProgress.unlockedStationId
    val roadmapListState = rememberLazyListState()

    // 500 Stations generation (memoized)
    val stations = remember(claimedStations, totalXp) {
        RoadmapManager.generateStations(totalXp, claimedStations)
    }

    var hasAutoScrolledToFrontier by remember { mutableStateOf(false) }
    LaunchedEffect(selectedMainTab, currentStationId) {
        if (selectedMainTab == 0 && !hasAutoScrolledToFrontier && currentStationId > 2) {
            hasAutoScrolledToFrontier = true
            val targetIdx = (currentStationId - 2).coerceIn(0, stations.lastIndex)
            roadmapListState.scrollToItem(targetIdx)
        }
    }

    val rtlSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("roadmap_screen")
    ) {
        // Deep OLED background & floating subtle glow dots
        RoadmapAmbientBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // TOP HEADER & NAVIGATION PILLS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xBB161E31))
                                .border(1.dp, Color(0x40FFFFFF), CircleShape)
                                .testTag("roadmap_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isAr) "رجوع" else "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

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

                    // Top-Right Frosted Tasks Ledger Button (replaces Shield badge opposite Roadmap title)
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                onOpenTasksLedger()
                            }
                            .testTag("roadmap_header_ledger_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xBB0D1D24))
                                .border(1.2.dp, ElectricMint.copy(alpha = 0.8f), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = if (isAr) "دفتر المهام" else "Tasks Ledger",
                                tint = ElectricMint,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when {
                                    isAr -> "دفتر المهام"
                                    currentLang == "fr" -> "Registre"
                                    else -> "Tasks Ledger"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = ElectricMint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // MAIN SEGMENTED GLASS PILL TABS: Roadmap vs Tasks vs Elite Missions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoadmapPillTabButton(
                        label = stringResource(R.string.roadmap_tab_stations),
                        icon = Icons.Default.Timeline,
                        selected = selectedMainTab == 0,
                        accentColor = NeonAqua,
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            selectedMainTab = 0
                            onMainTabChanged(0)
                        },
                        modifier = Modifier
                            .weight(0.95f)
                            .testTag("roadmap_tab_stations")
                    )
                    RoadmapPillTabButton(
                        label = stringResource(R.string.roadmap_tab_tasks),
                        icon = Icons.Default.Bolt,
                        selected = selectedMainTab == 1,
                        accentColor = NeonAqua,
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            selectedMainTab = 1
                            onMainTabChanged(1)
                        },
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("roadmap_tab_tasks")
                    )
                    RoadmapPillTabButton(
                        label = stringResource(R.string.roadmap_tab_elite_missions),
                        icon = Icons.Default.MilitaryTech,
                        selected = selectedMainTab == 2,
                        accentColor = NeonAmber,
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            selectedMainTab = 2
                            onMainTabChanged(2)
                        },
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("roadmap_tab_elite_missions")
                    )
                }
            }

            // CONTENT SECTION: TAB 0 (ROADMAP) / TAB 1 (TASKS) / TAB 2 (ELITE MISSIONS)
            AnimatedContent(
                targetState = selectedMainTab,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
                transitionSpec = {
                    val dir = (if (targetState > initialState) 1 else -1) * rtlSign
                    val enter = fadeIn(
                        animationSpec = tween(durationMillis = 155, delayMillis = 65, easing = FastOutSlowInEasing)
                    ) + slideInHorizontally(
                        animationSpec = tween(durationMillis = 170, delayMillis = 65, easing = FastOutSlowInEasing)
                    ) { fullWidth -> dir * (fullWidth / 14) }
                    val exit = fadeOut(
                        animationSpec = tween(durationMillis = 65, easing = FastOutLinearInEasing)
                    ) + slideOutHorizontally(
                        animationSpec = tween(durationMillis = 70, easing = FastOutLinearInEasing)
                    ) { fullWidth -> -dir * (fullWidth / 18) }
                    enter.togetherWith(exit).using(SizeTransform(clip = true))
                },
                label = "roadmap_tab_transition"
            ) { activeTab ->
                when (activeTab) {
                    0 -> {
                        RoadmapStationsTabContent(
                            stations = stations,
                            roadmapListState = roadmapListState,
                            currentStationId = currentStationId,
                            totalXp = totalXp,
                            nextStationId = stageProgress.nextStationId,
                            currentStageXp = stageProgress.currentStageXp,
                            isAr = isAr,
                            currentLang = currentLang,
                            onJumpToStation = { targetStation ->
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                coroutineScope.launch {
                                    val targetIdx = (targetStation - 2).coerceIn(0, stations.lastIndex)
                                    roadmapListState.animateScrollToItem(targetIdx)
                                }
                            },
                            onClaimStation = { stationId ->
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                viewModel.claimStation(stationId)
                            }
                        )
                    }

                    1 -> {
                // ==========================================
                // TAB 1: DYNAMIC TASK ENGINE & TASKS LEDGER
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    var everlastingFilter by remember { mutableStateOf(0) } // 0: All active, 1: Ready to Collect, 2: Pending

                    // Period Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeriodFilterChip(
                            label = if (isAr) "يومية (5)" else "Daily (5)",
                            isSelected = selectedTaskPeriod == TaskPeriod.DAILY,
                            onClick = {
                                selectedTaskPeriod = TaskPeriod.DAILY
                            }
                        )
                        PeriodFilterChip(
                            label = if (isAr) "أسبوعية (7)" else "Weekly (7)",
                            isSelected = selectedTaskPeriod == TaskPeriod.WEEKLY,
                            onClick = {
                                selectedTaskPeriod = TaskPeriod.WEEKLY
                            }
                        )
                        PeriodFilterChip(
                            label = if (isAr) "شهرية (10)" else "Monthly (10)",
                            isSelected = selectedTaskPeriod == TaskPeriod.MONTHLY,
                            onClick = {
                                selectedTaskPeriod = TaskPeriod.MONTHLY
                            }
                        )
                        PeriodFilterChip(
                            label = if (isAr) "الأبدية (${yearlyTasks.size})" else "Eternal (${yearlyTasks.size})",
                            isSelected = selectedTaskPeriod == TaskPeriod.YEARLY,
                            onClick = {
                                selectedTaskPeriod = TaskPeriod.YEARLY
                            }
                        )
                    }

                    // Eternal Master Goals Header Banner (Schedule countdown lives directly inside locked task containers)
                    if (selectedTaskPeriod == TaskPeriod.YEARLY) {
                        // Eternal Master Goals Header Banner
                        val readyEternalCount = yearlyTasks.count { it.isReadyToClaim }
                        val claimedEternalCount = (TaskPools.ALL_EVERLASTING_TASKS.size - yearlyTasks.size).coerceAtLeast(0)
                        val totalEternal = TaskPools.ALL_EVERLASTING_TASKS.size.coerceAtLeast(1)
                        val progressFraction = (claimedEternalCount.toFloat() / totalEternal.toFloat()).coerceIn(0f, 1f)

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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isAr) "مهام الأبدية (${yearlyTasks.size} متبقية)" else "Eternal Goals (${yearlyTasks.size} Remaining)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = PlusJakartaSans,
                                            color = NeonAmber
                                        )
                                        Text(
                                            text = if (isAr) {
                                                "عند جمع المكافأة تُنقل المهمة مباشرةً إلى دفتر المهام المنجزة • دون مفاتيح تجاوز"
                                            } else {
                                                "Collecting a completed goal transfers it directly to the Completed Tasks Ledger"
                                            },
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x33F59E0B))
                                            .border(1.dp, NeonAmber, RoundedCornerShape(8.dp))
                                            .clickable {
                                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                                onMainTabChanged(1)
                                                onOpenTasksLedger()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isAr) "في الدفتر: $claimedEternalCount/$totalEternal" else "Ledger: $claimedEternalCount/$totalEternal",
                                            fontSize = 10.sp,
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

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChipTiny(
                                        label = if (isAr) "الكل (${yearlyTasks.size})" else "All (${yearlyTasks.size})",
                                        selected = everlastingFilter == 0,
                                        onClick = { everlastingFilter = 0 }
                                    )
                                    FilterChipTiny(
                                        label = if (isAr) "جاهزة للجمع ($readyEternalCount)" else "Collect ($readyEternalCount)",
                                        selected = everlastingFilter == 1,
                                        onClick = { everlastingFilter = 1 }
                                    )
                                    FilterChipTiny(
                                        label = if (isAr) "قيد الإنجاز (${yearlyTasks.size - readyEternalCount})" else "In Progress (${yearlyTasks.size - readyEternalCount})",
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
                        TaskPeriod.YEARLY, TaskPeriod.EVERLASTING -> when (everlastingFilter) {
                            1 -> yearlyTasks.filter { it.isReadyToClaim }
                            2 -> yearlyTasks.filter { !it.isReadyToClaim }
                            else -> yearlyTasks
                        }
                    }

                    // Inventory Skip-Keys Summary Strip (for Daily, Weekly, Monthly)
                    if (selectedTaskPeriod != TaskPeriod.YEARLY) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KeyInventoryChip(
                                name = SkipKeyType.WOODEN.getLocalizedName(currentLang),
                                count = woodenKeys,
                                color = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                            KeyInventoryChip(
                                name = SkipKeyType.BRONZE.getLocalizedName(currentLang),
                                count = bronzeKeys,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            KeyInventoryChip(
                                name = SkipKeyType.SILVER.getLocalizedName(currentLang),
                                count = silverKeys,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.weight(1f)
                            )
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
                                nowMillis = nowMillis,
                                currentLang = currentLang,
                                availableKeysForType = when (task.requiredSkipKey) {
                                    SkipKeyType.WOODEN -> woodenKeys
                                    SkipKeyType.BRONZE -> bronzeKeys
                                    SkipKeyType.SILVER -> silverKeys
                                },
                                onClaimReward = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    viewModel.claimTaskReward(task.id)
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

                    else -> {
                        EliteMissionsSoonPage(
                            currentLang = currentLang
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapStationsTabContent(
    stations: List<RoadmapStation>,
    roadmapListState: LazyListState,
    currentStationId: Int,
    totalXp: Long,
    nextStationId: Int,
    currentStageXp: Long,
    isAr: Boolean,
    currentLang: String,
    onJumpToStation: (Int) -> Unit,
    onClaimStation: (Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "roadmap_pulse")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow_phase"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_progress"
    )
    val badgeBobDp by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_bob"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "egg_pulse"
    )
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            RoadmapProgressHeader(
                currentStationId = currentStationId,
                totalXp = totalXp,
                isAr = isAr,
                currentLang = currentLang,
                glowPulse = glowPulse,
                onJumpToStation = onJumpToStation
            )
        }

        LazyColumn(
            state = roadmapListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(stations, key = { _, station -> station.id }) { index, station ->
                val prevState = stations.getOrNull(index - 1)?.state
                val nextState = stations.getOrNull(index + 1)?.state
                val isFrontierStation = station.id == currentStationId.coerceAtLeast(1)

                WavySnakeStationRow(
                    station = station,
                    prevStationState = prevState,
                    nextStationState = nextState,
                    isCurrentMilestone = isFrontierStation,
                    currentLang = currentLang,
                    pulseScale = pulseScale,
                    orbitRotation = orbitRotation,
                    flowPhase = flowPhase,
                    glowPulse = glowPulse,
                    rippleProgress = rippleProgress,
                    badgeBobDp = badgeBobDp,
                    nextStationId = nextStationId,
                    currentStageXp = currentStageXp,
                    onClaim = { onClaimStation(station.id) }
                )
            }
        }
    }
}

@Composable
private fun EliteMissionsSoonPage(
    currentLang: String
) {
    val goldAccent = NeonAmber
    val violetAccent = NeonViolet

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("elite_missions_page"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xEB15182B),
                            Color(0xEB0E1222),
                            Color(0xEB090D18)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(goldAccent.copy(alpha = 0.8f), violetAccent.copy(alpha = 0.7f))),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(goldAccent.copy(alpha = 0.28f), violetAccent.copy(alpha = 0.14f), Color.Transparent)
                        )
                    )
                    .border(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(listOf(goldAccent, violetAccent)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = stringResource(R.string.roadmap_tab_elite_missions),
                    tint = goldAccent,
                    modifier = Modifier.size(38.dp)
                )
            }

            Text(
                text = stringResource(R.string.roadmap_tab_elite_missions),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(goldAccent.copy(alpha = 0.18f))
                    .border(1.2.dp, goldAccent.copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.elite_missions_soon),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                    color = goldAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("elite_missions_soon_badge")
                )
            }

            Text(
                text = when (currentLang) {
                    "ar" -> "تحديات ومهام نخبوية عالية المستوى قيد التطوير حالياً."
                    "fr" -> "Des missions d'élite à haut niveau de défi arrivent bientôt."
                    else -> "High-stakes elite operations and prestige missions are arriving soon."
                },
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RoadmapPillTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color = NeonAqua,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.16f) else Color(0xAA131A29),
        animationSpec = tween(150),
        label = "pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.85f) else Color(0x26FFFFFF),
        animationSpec = tween(150),
        label = "pill_border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else Color(0xFFCBD5E1),
        animationSpec = tween(150),
        label = "pill_content"
    )

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (selected) 1.2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Task Item Card in the Dynamic Task Engine
 */
@Composable
fun TaskItemCard(
    task: TaskItem,
    nowMillis: Long = System.currentTimeMillis(),
    currentLang: String = "ar",
    availableKeysForType: Int,
    onClaimReward: () -> Unit = {},
    onSkipWithKey: () -> Unit
) {
    val isAr = currentLang == "ar"
    val isLocked = task.isLocked(nowMillis)
    val isReady = task.status == TaskStatus.READY_TO_CLAIM

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    isReady -> Modifier.border(1.4.dp, ElectricMint, RoundedCornerShape(16.dp))
                    isLocked -> Modifier.border(1.1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    else -> Modifier
                }
            )
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
                        text = task.getLocalizedTitle(currentLang),
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        color = if (isLocked) Color(0xFF94A3B8) else Color.White
                    )
                    Text(
                        text = task.getLocalizedDescription(currentLang),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // XP Bounty Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isReady) ElectricMint.copy(alpha = 0.22f) else Color(0x3310B981))
                            .border(1.dp, if (isReady) ElectricMint else NeonEmerald, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "+${task.xpBounty} XP",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (isReady) ElectricMint else NeonEmerald
                        )
                    }

                    // Difficulty Tier Indicator Pill
                    val (tierLabel, tierColor) = when (task.difficultyTier) {
                        1 -> (if (isAr) "سهل" else "Easy") to NeonCyan
                        2 -> (if (isAr) "متوسط" else "Medium") to NeonAmber
                        3 -> (if (isAr) "متقدم" else "Hard") to Color(0xFFF97316)
                        else -> (if (isAr) "نخبوي" else "Elite") to NeonViolet
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tierColor.copy(alpha = 0.16f))
                            .border(0.8.dp, tierColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tierLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor
                        )
                    }
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
                            .background(
                                when {
                                    isReady -> ElectricMint
                                    task.isCompleted -> NeonEmerald
                                    isLocked -> Color(0xFF64748B)
                                    else -> NeonCyan
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${task.currentCount} / ${task.targetCount}",
                    fontSize = 10.sp,
                    color = if (isReady) ElectricMint else Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action / Status Area
            when {
                isReady -> {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = when (task.period) {
                                TaskPeriod.DAILY -> if (isAr) {
                                    "اكتمل الهدف! اضغط «جمع» لاستلام المكافأة واستبدالها بمهمة جديدة تفتح 12:00 ليلاً"
                                } else {
                                    "Goal met! Tap Collect to claim XP and queue a replacement for midnight"
                                }
                                TaskPeriod.WEEKLY -> if (isAr) {
                                    "اكتمل الهدف! اضغط «جمع» لاستلام المكافأة وتجهيز مهمة جديدة ليوم الأحد"
                                } else {
                                    "Goal met! Tap Collect to claim XP and queue a replacement for Sunday"
                                }
                                TaskPeriod.MONTHLY -> if (isAr) {
                                    "اكتمل الهدف! اضغط «جمع» لاستلام المكافأة وتجهيز مهمة جديدة ليوم 1 من الشهر"
                                } else {
                                    "Goal met! Tap Collect to claim XP and queue a replacement for the 1st of month"
                                }
                                TaskPeriod.YEARLY, TaskPeriod.EVERLASTING -> if (isAr) {
                                    "أنجزت مهمة أبدية! اضغط «جمع» لنقلها مباشرة إلى دفتر المهام المنجزة"
                                } else {
                                    "Eternal milestone reached! Tap Collect to move it to the Completed Tasks Ledger"
                                }
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricMint
                        )
                        Button(
                            onClick = onClaimReward,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricMint,
                                contentColor = Color(0xFF041418)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("btn_claim_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAr) "جمع (+${task.xpBounty} XP)" else "Collect (+${task.xpBounty} XP)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                isLocked -> {
                    val isFr = currentLang == "fr"
                    val countdown = TaskPools.formatRemainingCountdown(task.unlockAtMillis, nowMillis, currentLang)
                    val (grantTitle, grantSubtitle) = when (task.period) {
                        TaskPeriod.DAILY -> Pair(
                            when {
                                isAr -> "منح وتجديد اليومي • الساعة 12:00 ليلاً"
                                isFr -> "Attribution quotidienne • Minuit (00h00)"
                                else -> "Daily Grant & Unlock • 12:00 AM Midnight"
                            },
                            when {
                                isAr -> "تفتح المهمة البديلة في اليوم الجديد بعد:"
                                isFr -> "Nouvelle permission à minuit dans :"
                                else -> "Replacement task unlocks in:"
                            }
                        )
                        TaskPeriod.WEEKLY -> Pair(
                            when {
                                isAr -> "يوم منح مهام 7 أيام • كل يوم أحد (12:00 ليلاً)"
                                isFr -> "Attribution hebdo • Chaque Dimanche"
                                else -> "Weekly Grant Day • Sunday (12:00 AM)"
                            },
                            when {
                                isAr -> "تُمنح صلاحية الإنجاز بعد:"
                                isFr -> "Ouverture dans :"
                                else -> "Permission unlocks in:"
                            }
                        )
                        TaskPeriod.MONTHLY -> Pair(
                            when {
                                isAr -> "يوم منح مهام الشهر • 1 من كل شهر (12:00 ليلاً)"
                                isFr -> "Attribution mensuelle • 1er du mois"
                                else -> "Monthly Grant Day • 1st of Month (12:00 AM)"
                            },
                            when {
                                isAr -> "تُمنح صلاحية الإنجاز بعد:"
                                isFr -> "Ouverture dans :"
                                else -> "Permission unlocks in:"
                            }
                        )
                        else -> "" to ""
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22F59E0B))
                            .border(1.dp, NeonAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                            .testTag("task_cooldown_timer_${task.id}"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = grantTitle,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonAmber
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = grantSubtitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xCC0B1324))
                                    .border(1.dp, NeonAmber, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = countdown,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = NeonAmber
                                )
                            }
                        }
                    }
                }
                task.isCompleted -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "مكتملة (+${task.xpBounty} XP حُصدت بنجاح)" else "Completed (+${task.xpBounty} XP Claimed)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                    }
                }
                !task.canBeSkipped -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22F59E0B))
                            .border(1.dp, Color(0x55F59E0B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) {
                                "مهمة أبدية (غير قابلة للتجاوز بالمفاتيح - تتطلب إنجازاً فعلياً)"
                            } else {
                                "Eternal Milestone (Cannot be skipped with keys — genuine completion required)"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "تُنجز بالنشاط الفعلي أو بالتجاوز" else "Complete via activity or skip-key",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )

                        val keyColor = when (task.requiredSkipKey) {
                            SkipKeyType.WOODEN -> Color(0xFFD97706)
                            SkipKeyType.BRONZE -> Color(0xFFF59E0B)
                            SkipKeyType.SILVER -> Color(0xFFE2E8F0)
                        }
                        val hasKeyAvailable = availableKeysForType > 0

                        OutlinedButton(
                            onClick = onSkipWithKey,
                            enabled = hasKeyAvailable,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (hasKeyAvailable) keyColor.copy(alpha = 0.75f) else Color(0x3364748B)
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_skip_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = if (hasKeyAvailable) keyColor else Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isAr) {
                                    "تجاوز بـ ${task.requiredSkipKey.getLocalizedName(currentLang)} ($availableKeysForType)"
                                } else {
                                    "Skip w/ ${task.requiredSkipKey.getLocalizedName(currentLang)} ($availableKeysForType)"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasKeyAvailable) keyColor else Color.Gray
                            )
                        }
                    }
                }
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
    accentColor: Color = NeonCyan,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0x221E293B))
            .border(1.dp, if (isSelected) accentColor else Color(0x20FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) accentColor else Color(0xFF94A3B8)
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

