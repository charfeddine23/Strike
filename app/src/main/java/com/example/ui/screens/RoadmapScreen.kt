package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.components.GlacialDayCyan
import com.example.ui.components.GlacialNightCyan
import com.example.ui.components.GlacialSubmersionOverlay
import com.example.ui.components.GlacialViewportBackdrop
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NeonAqua
import com.example.ui.components.RoadmapAmbientBackground
import com.example.ui.components.RoadmapProgressHeader
import com.example.ui.components.WavySnakeStationRow
import com.example.ui.components.glacialDepthForStation
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayGlassSurface
import com.example.ui.theme.DayGlassSurfaceElevated
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.DayTextTertiary
import com.example.ui.theme.LocalAppDarkTheme
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
 * - Top quick-action bar: Left "Tasks Ledger", Right "Xp Logs"
 * - 3 Main Tabs: Roadmap / Tasks / Elite Missions
 * - Every task displays a unique compact Task ID code (e.g. D23E)
 * - Skip action shows a custom key silhouette matching key type (Wooden / Bronze / Silver) beside "Skip"
 */
@Composable
fun RoadmapScreen(
    viewModel: IndividualViewModel,
    @Suppress("UNUSED_PARAMETER") onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    initialMainTab: Int = 0,
    onMainTabChanged: (Int) -> Unit = {},
    onOpenTasksLedger: () -> Unit = {},
    onOpenXpLogs: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    val appLayoutDirection = LocalLayoutDirection.current

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
    var showRoadmapProgressDialog by remember { mutableStateOf(false) }

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

    // 700 Stations generation (memoized)
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

    // Live scroll-driven glacial immersion depth (0f for Stations 1..500 -> 1f at Station 700)
    val rawGlacialDepth by remember(selectedMainTab, currentStationId) {
        derivedStateOf {
            if (selectedMainTab != 0) {
                0f
            } else {
                val visible = roadmapListState.layoutInfo.visibleItemsInfo
                val deepestVisible = if (visible.isNotEmpty()) {
                    visible.maxOf { it.index + 1 }
                } else {
                    currentStationId
                }
                if (deepestVisible > TaskPools.BASE_STATIONS_COUNT) {
                    glacialDepthForStation(deepestVisible).coerceAtLeast(0.10f)
                } else {
                    0f
                }
            }
        }
    }
    val animatedGlacialDepth by animateFloatAsState(
        targetValue = rawGlacialDepth,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "glacial_submersion_depth"
    )
    val glacialTransition = rememberInfiniteTransition(label = "glacial_atmosphere")
    val glacialDriftPhase by glacialTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glacial_drift_phase"
    )

    val rtlSign = if (appLayoutDirection == LayoutDirection.Rtl) -1 else 1
    val ledgerAccent = if (isDarkTheme) ElectricMint else DayEmerald
    val xpLogsAccent = if (isDarkTheme) NeonViolet else DayPurple
    val pillBgLedger = if (isDarkTheme) Color(0xBB0D1D24) else DayGlassSurface
    val pillBgXpLogs = if (isDarkTheme) Color(0xBB191229) else DayGlassSurface
    val tabAquaAccent = if (isDarkTheme) NeonAqua else DayCyan
    val tabEliteAccent = if (isDarkTheme) NeonAmber else DayOrange

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("roadmap_screen")
    ) {
        RoadmapAmbientBackground(
            modifier = Modifier.fillMaxSize(),
            darkTheme = isDarkTheme
        )

        GlacialViewportBackdrop(
            glacialDepth = animatedGlacialDepth,
            driftPhase = glacialDriftPhase,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // TOP QUICK-ACTION ROW & NAVIGATION PILLS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Physical LTR layout so "Tasks Ledger" sits strictly on the physical LEFT,
                // the Roadmap Progress floating-menu trigger sits centered between them,
                // and "Xp Logs" sits strictly on the physical RIGHT.
                val progressFrostView = animatedGlacialDepth > 0.05f || currentStationId > TaskPools.BASE_STATIONS_COUNT
                val progressBtnAccent = when {
                    progressFrostView && isDarkTheme -> GlacialNightCyan
                    progressFrostView && !isDarkTheme -> GlacialDayCyan
                    else -> tabAquaAccent
                }
                val pillBgProgress = if (isDarkTheme) {
                    if (progressFrostView) Color(0xCC0A2238) else Color(0xBB0C1A2E)
                } else {
                    DayGlassSurface
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: Tasks Ledger button
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
                            CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(pillBgLedger)
                                        .border(1.2.dp, ledgerAccent.copy(alpha = 0.8f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = if (isAr) "دفتر المهام" else "Tasks Ledger",
                                        tint = ledgerAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = when {
                                            isAr -> "دفتر المهام"
                                            currentLang == "fr" -> "Registre"
                                            else -> "Tasks Ledger"
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = PlusJakartaSans,
                                        color = ledgerAccent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // CENTER: Roadmap Progress Floating Menu Trigger
                        val safeHeaderStation = currentStationId.coerceIn(0, TaskPools.TOTAL_STATIONS_COUNT)
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    showRoadmapProgressDialog = true
                                }
                                .testTag("roadmap_header_progress_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(pillBgProgress)
                                        .border(1.3.dp, progressBtnAccent.copy(alpha = 0.85f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (progressFrostView) Icons.Default.AcUnit else Icons.Default.MilitaryTech,
                                        contentDescription = when (currentLang) {
                                            "ar" -> "تقدم المسار"
                                            "fr" -> "Progression"
                                            else -> "Roadmap Progress"
                                        },
                                        tint = progressBtnAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = when (currentLang) {
                                            "ar" -> "LVL $safeHeaderStation/${TaskPools.TOTAL_STATIONS_COUNT}"
                                            "fr" -> "Niv $safeHeaderStation/${TaskPools.TOTAL_STATIONS_COUNT}"
                                            else -> "LVL $safeHeaderStation/${TaskPools.TOTAL_STATIONS_COUNT}"
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = PlusJakartaSans,
                                        color = progressBtnAccent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // RIGHT: Xp Logs button
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    onOpenXpLogs()
                                }
                                .testTag("roadmap_header_xp_logs_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(pillBgXpLogs)
                                        .border(1.2.dp, xpLogsAccent.copy(alpha = 0.8f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = if (isAr) "سجل XP" else "Xp Logs",
                                        tint = xpLogsAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Xp Logs",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = PlusJakartaSans,
                                        color = xpLogsAccent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // MAIN SEGMENTED GLASS PILL TABS: Roadmap vs Tasks vs Elite Missions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoadmapPillTabButton(
                        label = stringResource(R.string.roadmap_tab_stations),
                        icon = Icons.Default.Timeline,
                        selected = selectedMainTab == 0,
                        accentColor = tabAquaAccent,
                        isDarkTheme = isDarkTheme,
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
                        accentColor = tabAquaAccent,
                        isDarkTheme = isDarkTheme,
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
                        accentColor = tabEliteAccent,
                        isDarkTheme = isDarkTheme,
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
                            nextStationId = stageProgress.nextStationId,
                            currentStageXp = stageProgress.currentStageXp,
                            currentStationId = currentStationId,
                            currentLang = currentLang,
                            isDarkTheme = isDarkTheme,
                            onClaimStation = { stationId ->
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                viewModel.claimStation(stationId)
                            }
                        )
                    }

                    1 -> {
                        // ==========================================
                        // TAB 1: DYNAMIC TASK ENGINE
                        // ==========================================
                        val cyanAccent = if (isDarkTheme) NeonCyan else DayCyan
                        val amberAccent = if (isDarkTheme) NeonAmber else DayOrange
                        val descColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            var everlastingFilter by remember { mutableStateOf(0) } // 0: All active, 1: Collect, 2: Pending
                            var showEternalGoalsHeader by remember { mutableStateOf(true) }
                            val tasksListState = rememberLazyListState()

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

                            LaunchedEffect(selectedTaskPeriod) {
                                showEternalGoalsHeader = true
                                tasksListState.scrollToItem(0)
                            }

                            LaunchedEffect(tasksListState, selectedTaskPeriod) {
                                if (selectedTaskPeriod == TaskPeriod.YEARLY || selectedTaskPeriod == TaskPeriod.EVERLASTING) {
                                    snapshotFlow {
                                        tasksListState.firstVisibleItemIndex to tasksListState.firstVisibleItemScrollOffset
                                    }.collect { (index, offset) ->
                                        if ((index > 0 || offset > 24) && showEternalGoalsHeader) {
                                            showEternalGoalsHeader = false
                                        }
                                    }
                                }
                            }

                            LaunchedEffect(currentTasks.isEmpty(), selectedTaskPeriod) {
                                if ((selectedTaskPeriod == TaskPeriod.YEARLY || selectedTaskPeriod == TaskPeriod.EVERLASTING) &&
                                    currentTasks.isEmpty()
                                ) {
                                    showEternalGoalsHeader = true
                                }
                            }

                            val eternalHeaderNestedScroll = remember(selectedTaskPeriod) {
                                object : NestedScrollConnection {
                                    private var upwardSwipeDelta = 0f
                                    private var downwardPullAtTopDelta = 0f

                                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                        if (selectedTaskPeriod != TaskPeriod.YEARLY && selectedTaskPeriod != TaskPeriod.EVERLASTING) {
                                            return Offset.Zero
                                        }
                                        if (source == NestedScrollSource.UserInput) {
                                            val atFirstTask = tasksListState.firstVisibleItemIndex == 0 &&
                                                tasksListState.firstVisibleItemScrollOffset == 0
                                            if (available.y < 0f && showEternalGoalsHeader) {
                                                downwardPullAtTopDelta = 0f
                                                upwardSwipeDelta += -available.y
                                                if (upwardSwipeDelta >= 12f) {
                                                    upwardSwipeDelta = 0f
                                                    showEternalGoalsHeader = false
                                                }
                                            } else if (available.y > 0f) {
                                                upwardSwipeDelta = 0f
                                                if (atFirstTask && !showEternalGoalsHeader) {
                                                    downwardPullAtTopDelta += available.y
                                                    if (downwardPullAtTopDelta >= 14f) {
                                                        downwardPullAtTopDelta = 0f
                                                        showEternalGoalsHeader = true
                                                    }
                                                }
                                            }
                                        }
                                        return Offset.Zero
                                    }

                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource
                                    ): Offset {
                                        if (selectedTaskPeriod != TaskPeriod.YEARLY && selectedTaskPeriod != TaskPeriod.EVERLASTING) {
                                            return Offset.Zero
                                        }
                                        val atFirstTask = tasksListState.firstVisibleItemIndex == 0 &&
                                            tasksListState.firstVisibleItemScrollOffset == 0
                                        if (!atFirstTask) {
                                            downwardPullAtTopDelta = 0f
                                            return Offset.Zero
                                        }
                                        if (source == NestedScrollSource.UserInput && available.y > 0f && !showEternalGoalsHeader) {
                                            downwardPullAtTopDelta += available.y
                                            if (downwardPullAtTopDelta >= 14f) {
                                                downwardPullAtTopDelta = 0f
                                                showEternalGoalsHeader = true
                                            }
                                        }
                                        return Offset.Zero
                                    }
                                }
                            }

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
                                    accentColor = cyanAccent,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { selectedTaskPeriod = TaskPeriod.DAILY }
                                )
                                PeriodFilterChip(
                                    label = if (isAr) "أسبوعية (7)" else "Weekly (7)",
                                    isSelected = selectedTaskPeriod == TaskPeriod.WEEKLY,
                                    accentColor = cyanAccent,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { selectedTaskPeriod = TaskPeriod.WEEKLY }
                                )
                                PeriodFilterChip(
                                    label = if (isAr) "شهرية (10)" else "Monthly (10)",
                                    isSelected = selectedTaskPeriod == TaskPeriod.MONTHLY,
                                    accentColor = cyanAccent,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { selectedTaskPeriod = TaskPeriod.MONTHLY }
                                )
                                PeriodFilterChip(
                                    label = if (isAr) "الأبدية (${yearlyTasks.size})" else "Eternal (${yearlyTasks.size})",
                                    isSelected = selectedTaskPeriod == TaskPeriod.YEARLY,
                                    accentColor = cyanAccent,
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier.testTag("period_chip_eternal"),
                                    onClick = { selectedTaskPeriod = TaskPeriod.YEARLY }
                                )
                            }

                            // Eternal Master Goals Header Banner (smoothly hides on upward scroll, returns on pull-down at first task)
                            AnimatedVisibility(
                                visible = (selectedTaskPeriod == TaskPeriod.YEARLY || selectedTaskPeriod == TaskPeriod.EVERLASTING) &&
                                    showEternalGoalsHeader,
                                enter = expandVertically(
                                    expandFrom = Alignment.Top,
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                                exit = shrinkVertically(
                                    shrinkTowards = Alignment.Top,
                                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing)),
                                label = "eternal_goals_header_visibility"
                            ) {
                                val readyEternalCount = yearlyTasks.count { it.isReadyToClaim }
                                val claimedEternalCount = (TaskPools.ALL_EVERLASTING_TASKS.size - yearlyTasks.size).coerceAtLeast(0)
                                val totalEternal = TaskPools.ALL_EVERLASTING_TASKS.size.coerceAtLeast(1)
                                val progressFraction = (claimedEternalCount.toFloat() / totalEternal.toFloat()).coerceIn(0f, 1f)

                                LiquidGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .testTag("eternal_goals_header"),
                                    shape = RoundedCornerShape(14.dp),
                                    darkTheme = isDarkTheme
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
                                                    color = amberAccent
                                                )
                                                Text(
                                                    text = if (isAr) {
                                                        "عند جمع المكافأة تُنقل المهمة مباشرةً إلى دفتر المهام المنجزة • دون مفاتيح تجاوز"
                                                    } else {
                                                        "Collecting a completed goal transfers it directly to the Completed Tasks Ledger"
                                                    },
                                                    fontSize = 10.sp,
                                                    color = descColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(amberAccent.copy(alpha = if (isDarkTheme) 0.2f else 0.14f))
                                                    .border(1.dp, amberAccent, RoundedCornerShape(8.dp))
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
                                                    color = amberAccent
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isDarkTheme) Color(0x331E293B) else DayGlassSurfaceElevated)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(progressFraction)
                                                    .height(6.dp)
                                                    .background(Brush.horizontalGradient(listOf(amberAccent, Color(0xFFF97316))))
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChipTiny(
                                                label = if (isAr) "الكل (${yearlyTasks.size})" else "All (${yearlyTasks.size})",
                                                selected = everlastingFilter == 0,
                                                isDarkTheme = isDarkTheme,
                                                onClick = { everlastingFilter = 0 }
                                            )
                                            FilterChipTiny(
                                                label = if (isAr) "جاهزة للجمع ($readyEternalCount)" else "Collect ($readyEternalCount)",
                                                selected = everlastingFilter == 1,
                                                isDarkTheme = isDarkTheme,
                                                onClick = { everlastingFilter = 1 }
                                            )
                                            FilterChipTiny(
                                                label = if (isAr) "قيد الإنجاز (${yearlyTasks.size - readyEternalCount})" else "In Progress (${yearlyTasks.size - readyEternalCount})",
                                                selected = everlastingFilter == 2,
                                                isDarkTheme = isDarkTheme,
                                                onClick = { everlastingFilter = 2 }
                                            )
                                        }
                                    }
                                }
                            }

                            LazyColumn(
                                state = tasksListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(eternalHeaderNestedScroll)
                                    .testTag("tasks_lazy_list"),
                                contentPadding = PaddingValues(bottom = 48.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(currentTasks, key = { it.id }) { task ->
                                    TaskItemCard(
                                        task = task,
                                        nowMillis = nowMillis,
                                        currentLang = currentLang,
                                        isDarkTheme = isDarkTheme,
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
                            currentLang = currentLang,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
        // Non-blocking foreground edge-creeping frost & branching ice dendrites when above Station 500
        GlacialSubmersionOverlay(
            glacialDepth = animatedGlacialDepth,
            driftPhase = glacialDriftPhase,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.fillMaxSize()
        )

        // Floating Roadmap Progress Popup triggered from the top-center pill button
        if (showRoadmapProgressDialog) {
            Dialog(
                onDismissRequest = { showRoadmapProgressDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("roadmap_progress_popup"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoadmapProgressHeader(
                        currentStationId = currentStationId,
                        totalXp = totalXp,
                        isAr = isAr,
                        currentLang = currentLang,
                        glacialDepth = animatedGlacialDepth,
                        isDarkTheme = isDarkTheme,
                        onJumpToStation = { targetStation ->
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            showRoadmapProgressDialog = false
                            if (selectedMainTab != 0) {
                                selectedMainTab = 0
                                onMainTabChanged(0)
                            }
                            coroutineScope.launch {
                                val targetIdx = (targetStation - 2).coerceIn(0, stations.lastIndex)
                                roadmapListState.animateScrollToItem(targetIdx)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 42.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDarkTheme) Color(0xEA0F172A) else DayGlassSurface)
                            .border(
                                1.dp,
                                tabAquaAccent.copy(alpha = 0.55f),
                                RoundedCornerShape(50)
                            )
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                showRoadmapProgressDialog = false
                            }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                            .testTag("btn_close_roadmap_progress_popup"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = tabAquaAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (currentLang) {
                                "ar" -> "إغلاق"
                                "fr" -> "Fermer"
                                else -> "Close"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else DayTextPrimary
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
    nextStationId: Int,
    currentStageXp: Long,
    currentStationId: Int,
    currentLang: String,
    isDarkTheme: Boolean,
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
                    isDarkTheme = isDarkTheme,
                    onClaim = { onClaimStation(station.id) }
                )
            }
        }
    }
}

@Composable
private fun EliteMissionsSoonPage(
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val goldAccent = if (isDarkTheme) NeonAmber else DayOrange
    val violetAccent = if (isDarkTheme) NeonViolet else DayPurple
    val cardBrush = if (isDarkTheme) {
        Brush.verticalGradient(listOf(Color(0xEB15182B), Color(0xEB0E1222), Color(0xEB090D18)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFAFFFFFF), Color(0xFFF3F7FC)))
    }
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val descColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

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
                .background(cardBrush)
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
                color = titleColor,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(goldAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.14f))
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
                color = descColor,
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
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unselectedBg = if (isDarkTheme) Color(0xAA131A29) else DayGlassSurface
    val unselectedBorder = if (isDarkTheme) Color(0x26FFFFFF) else Color(0xFFCBD5E1)
    val unselectedText = if (isDarkTheme) Color(0xFFCBD5E1) else DayTextSecondary

    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = if (isDarkTheme) 0.16f else 0.14f) else unselectedBg,
        animationSpec = tween(150),
        label = "pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.85f) else unselectedBorder,
        animationSpec = tween(150),
        label = "pill_border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else unselectedText,
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
 * Custom key icon drawn cleanly with Canvas so WOODEN, BRONZE, and SILVER keys
 * have unmistakably distinct silhouettes & metallic/wood accents matching their type.
 */
@Composable
fun SkipKeyShapeIcon(
    keyType: SkipKeyType,
    enabled: Boolean,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (!enabled) {
        Color(0xFF94A3B8)
    } else when (keyType) {
        SkipKeyType.WOODEN -> if (isDarkTheme) Color(0xFFD97706) else Color(0xFF92400E)
        SkipKeyType.BRONZE -> if (isDarkTheme) Color(0xFFF59E0B) else Color(0xFFB45309)
        SkipKeyType.SILVER -> if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569)
    }
    val highlightColor = if (!enabled) {
        Color(0xFFCBD5E1)
    } else when (keyType) {
        SkipKeyType.WOODEN -> if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        SkipKeyType.BRONZE -> if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFF59E0B)
        SkipKeyType.SILVER -> if (isDarkTheme) Color.White else Color(0xFF94A3B8)
    }

    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val strokeThin = 1.3.dp.toPx()

        when (keyType) {
            SkipKeyType.WOODEN -> {
                // Wooden Key: warm chunky rounded-plank oval bow + rustic grain line + 2 wide carved square teeth
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.05f, h * 0.24f),
                    size = Size(w * 0.34f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.12f, h * 0.18f),
                    style = Stroke(width = strokeThin * 1.35f)
                )
                drawLine(
                    color = highlightColor,
                    start = Offset(w * 0.14f, h * 0.44f),
                    end = Offset(w * 0.30f, h * 0.44f),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Thick wooden shaft
                drawLine(
                    color = primaryColor,
                    start = Offset(w * 0.39f, h * 0.50f),
                    end = Offset(w * 0.93f, h * 0.50f),
                    strokeWidth = strokeThin * 1.4f,
                    cap = StrokeCap.Round
                )
                // Twin square carved wooden teeth
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.64f, h * 0.50f),
                    size = Size(w * 0.11f, h * 0.24f)
                )
                drawRect(
                    color = highlightColor,
                    topLeft = Offset(w * 0.81f, h * 0.50f),
                    size = Size(w * 0.11f, h * 0.28f)
                )
            }

            SkipKeyType.BRONZE -> {
                // Bronze Key: antique rotated diamond bow + decorative collar ring + dual stepped castle bit
                val diamond = Path().apply {
                    moveTo(w * 0.23f, h * 0.18f)
                    lineTo(w * 0.40f, h * 0.50f)
                    lineTo(w * 0.23f, h * 0.82f)
                    lineTo(w * 0.06f, h * 0.50f)
                    close()
                }
                drawPath(
                    path = diamond,
                    color = primaryColor,
                    style = Stroke(width = strokeThin * 1.25f, join = StrokeJoin.Round)
                )
                drawCircle(
                    color = highlightColor,
                    radius = w * 0.06f,
                    center = Offset(w * 0.23f, h * 0.50f)
                )
                // Collar notch
                drawLine(
                    color = highlightColor,
                    start = Offset(w * 0.48f, h * 0.38f),
                    end = Offset(w * 0.48f, h * 0.62f),
                    strokeWidth = strokeThin,
                    cap = StrokeCap.Round
                )
                // Shaft
                drawLine(
                    color = primaryColor,
                    start = Offset(w * 0.40f, h * 0.50f),
                    end = Offset(w * 0.94f, h * 0.50f),
                    strokeWidth = strokeThin * 1.2f,
                    cap = StrokeCap.Round
                )
                // Stepped antique castle teeth
                val bitPath = Path().apply {
                    moveTo(w * 0.62f, h * 0.50f)
                    lineTo(w * 0.62f, h * 0.78f)
                    lineTo(w * 0.72f, h * 0.78f)
                    lineTo(w * 0.72f, h * 0.65f)
                    lineTo(w * 0.82f, h * 0.65f)
                    lineTo(w * 0.82f, h * 0.80f)
                    lineTo(w * 0.92f, h * 0.80f)
                    lineTo(w * 0.92f, h * 0.50f)
                }
                drawPath(
                    path = bitPath,
                    color = primaryColor,
                    style = Stroke(width = strokeThin, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            SkipKeyType.SILVER -> {
                // Silver Key: royal hexagonal faceted crown bow + twin collar rings + 3 precision warded teeth
                val hex = Path().apply {
                    moveTo(w * 0.14f, h * 0.22f)
                    lineTo(w * 0.31f, h * 0.22f)
                    lineTo(w * 0.40f, h * 0.50f)
                    lineTo(w * 0.31f, h * 0.78f)
                    lineTo(w * 0.14f, h * 0.78f)
                    lineTo(w * 0.05f, h * 0.50f)
                    close()
                }
                drawPath(
                    path = hex,
                    color = primaryColor,
                    style = Stroke(width = strokeThin * 1.25f, join = StrokeJoin.Round)
                )
                // Inner crown gem glint
                drawLine(
                    color = highlightColor,
                    start = Offset(w * 0.17f, h * 0.38f),
                    end = Offset(w * 0.28f, h * 0.38f),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Double collar guard
                drawLine(
                    color = highlightColor,
                    start = Offset(w * 0.46f, h * 0.35f),
                    end = Offset(w * 0.46f, h * 0.65f),
                    strokeWidth = 1.1.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(w * 0.40f, h * 0.50f),
                    end = Offset(w * 0.95f, h * 0.50f),
                    strokeWidth = strokeThin * 1.15f,
                    cap = StrokeCap.Round
                )
                // Triple royal warded teeth
                listOf(0.60f to 0.70f, 0.75f to 0.80f, 0.90f to 0.74f).forEachIndexed { idx, (tx, ty) ->
                    drawLine(
                        color = if (idx == 1) highlightColor else primaryColor,
                        start = Offset(w * tx, h * 0.50f),
                        end = Offset(w * tx, h * ty),
                        strokeWidth = strokeThin * 1.1f,
                        cap = StrokeCap.Round
                    )
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
    nowMillis: Long = System.currentTimeMillis(),
    currentLang: String = "ar",
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    availableKeysForType: Int,
    onClaimReward: () -> Unit = {},
    onSkipWithKey: () -> Unit
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val isLocked = task.isLocked(nowMillis)
    val isReady = task.status == TaskStatus.READY_TO_CLAIM

    val cyanAccent = if (isDarkTheme) NeonCyan else DayCyan
    val emeraldAccent = if (isDarkTheme) NeonEmerald else DayEmerald
    val mintAccent = if (isDarkTheme) ElectricMint else DayEmerald
    val amberAccent = if (isDarkTheme) NeonAmber else DayOrange
    val violetAccent = if (isDarkTheme) NeonViolet else DayPurple
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val descColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary
    val mutedColor = if (isDarkTheme) Color(0xFF64748B) else DayTextTertiary
    val idBadgeBg = if (isDarkTheme) Color(0x2838BDF8) else Color(0x180284C7)

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    isReady -> Modifier.border(1.4.dp, mintAccent, RoundedCornerShape(16.dp))
                    isLocked -> Modifier.border(1.1.dp, amberAccent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    else -> Modifier
                }
            )
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        darkTheme = isDarkTheme
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(idBadgeBg)
                                .border(0.9.dp, cyanAccent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("task_id_badge_${task.id}")
                        ) {
                            Text(
                                text = task.taskCode,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = cyanAccent
                            )
                        }

                        // Difficulty Tier Indicator Pill
                        val (tierLabel, tierColor) = when (task.difficultyTier) {
                            1 -> (if (isAr) "سهل" else "Easy") to cyanAccent
                            2 -> (if (isAr) "متوسط" else "Medium") to amberAccent
                            3 -> (if (isAr) "متقدم" else "Hard") to if (isDarkTheme) Color(0xFFF97316) else Color(0xFFC2410C)
                            else -> (if (isAr) "نخبوي" else "Elite") to violetAccent
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.getLocalizedTitle(currentLang),
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        color = if (isLocked) descColor else titleColor
                    )
                    Text(
                        text = task.getLocalizedDescription(currentLang),
                        fontSize = 11.sp,
                        color = descColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Top-End Badges: optional "can't skip" beside XP Bounty Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!task.canBeSkipped) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(amberAccent.copy(alpha = if (isDarkTheme) 0.16f else 0.12f))
                                .border(1.dp, amberAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("task_cant_skip_badge_${task.id}")
                        ) {
                            Text(
                                text = "can't skip",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = amberAccent,
                                maxLines = 1
                            )
                        }
                    }

                    // XP Bounty Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isReady) mintAccent.copy(alpha = if (isDarkTheme) 0.22f else 0.15f)
                                else emeraldAccent.copy(alpha = if (isDarkTheme) 0.2f else 0.14f)
                            )
                            .border(1.dp, if (isReady) mintAccent else emeraldAccent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+${task.xpBounty} XP",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (isReady) mintAccent else emeraldAccent,
                            maxLines = 1
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
                        .background(if (isDarkTheme) Color(0x331E293B) else DayGlassSurfaceElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(task.progressFraction)
                            .height(6.dp)
                            .background(
                                when {
                                    isReady -> mintAccent
                                    task.isCompleted -> emeraldAccent
                                    isLocked -> mutedColor
                                    else -> cyanAccent
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${task.currentCount} / ${task.targetCount}",
                    fontSize = 10.sp,
                    color = if (isReady) mintAccent else if (isDarkTheme) Color(0xFFCBD5E1) else DayTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isReady || isLocked || task.isCompleted || task.canBeSkipped) {
                Spacer(modifier = Modifier.height(10.dp))
            }

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
                            color = mintAccent
                        )
                        Button(
                            onClick = onClaimReward,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = mintAccent,
                                contentColor = if (isDarkTheme) Color(0xFF041418) else Color.White
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
                            .background(amberAccent.copy(alpha = if (isDarkTheme) 0.14f else 0.10f))
                            .border(1.dp, amberAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
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
                                tint = amberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = grantTitle,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = amberAccent
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
                                    tint = if (isDarkTheme) Color(0xFFFDE68A) else amberAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = grantSubtitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDarkTheme) Color(0xCC0B1324) else DayGlassSurface)
                                    .border(1.dp, amberAccent, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = countdown,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = amberAccent
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
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = emeraldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "مكتملة (+${task.xpBounty} XP حُصدت بنجاح)" else "Completed (+${task.xpBounty} XP Claimed)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = emeraldAccent
                        )
                    }
                }
                !task.canBeSkipped -> Unit
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "تُنجز بالنشاط الفعلي أو بالتجاوز" else "Complete via activity or skip-key",
                            fontSize = 10.sp,
                            color = mutedColor,
                            fontWeight = FontWeight.Medium
                        )

                        val keyColor = when (task.requiredSkipKey) {
                            SkipKeyType.WOODEN -> if (isDarkTheme) Color(0xFFD97706) else Color(0xFF92400E)
                            SkipKeyType.BRONZE -> if (isDarkTheme) Color(0xFFF59E0B) else Color(0xFFB45309)
                            SkipKeyType.SILVER -> if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569)
                        }
                        val hasKeyAvailable = availableKeysForType > 0

                        OutlinedButton(
                            onClick = onSkipWithKey,
                            enabled = hasKeyAvailable,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.1.dp,
                                if (hasKeyAvailable) keyColor.copy(alpha = 0.8f) else mutedColor.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_skip_task_${task.id}")
                        ) {
                            SkipKeyShapeIcon(
                                keyType = task.requiredSkipKey,
                                enabled = hasKeyAvailable,
                                isDarkTheme = isDarkTheme,
                                modifier = Modifier.testTag("skip_key_shape_${task.requiredSkipKey.name.lowercase()}_${task.id}")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isAr -> "تجاوز"
                                    isFr -> "Passer"
                                    else -> "Skip"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (hasKeyAvailable) keyColor else mutedColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodFilterChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color = NeonCyan,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> accentColor.copy(alpha = if (isDarkTheme) 0.2f else 0.14f)
        isDarkTheme -> Color(0x221E293B)
        else -> DayGlassSurface
    }
    val borderColor = when {
        isSelected -> accentColor
        isDarkTheme -> Color(0x20FFFFFF)
        else -> Color(0xFFCBD5E1)
    }
    val textColor = when {
        isSelected -> accentColor
        isDarkTheme -> Color(0xFF94A3B8)
        else -> DayTextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun FilterChipTiny(
    label: String,
    selected: Boolean,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onClick: () -> Unit
) {
    val amberAccent = if (isDarkTheme) NeonAmber else DayOrange
    val bgColor = when {
        selected -> amberAccent.copy(alpha = if (isDarkTheme) 0.2f else 0.14f)
        isDarkTheme -> Color(0x221E293B)
        else -> DayGlassSurface
    }
    val borderColor = when {
        selected -> amberAccent
        isDarkTheme -> Color(0x20FFFFFF)
        else -> Color(0xFFCBD5E1)
    }
    val textColor = when {
        selected -> amberAccent
        isDarkTheme -> Color(0xFF94A3B8)
        else -> DayTextSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}


