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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.RoadmapStation
import com.example.data.roadmap.TaskPools
import com.example.ui.components.NeonAqua
import com.example.ui.components.RoadmapAmbientBackground
import com.example.ui.components.RoadmapBiome
import com.example.ui.components.RoadmapBiomeBackdrop
import com.example.ui.components.RoadmapBiomeState
import com.example.ui.components.RoadmapBiomeSubmersionOverlay
import com.example.ui.components.TopFrostedLiquidGlassVeil
import com.example.ui.components.WavySnakeStationRow
import com.example.ui.components.biomePrimaryAccent
import com.example.ui.components.biomeSecondaryAccent
import com.example.ui.components.edgeFadeInBlur
import com.example.ui.components.roadmapBiomeStateForStation
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalScaffoldContentPadding
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.launch
import kotlin.math.PI

/**
 * 700-Station Wavy Biome Roadmap:
 * - Clean focused top tabs: Roadmap / Elite Missions (no Level badge, no Tasks/Ledger/XpLogs)
 * - Dynamic biome atmosphere from Glacial Ice (1) -> Rocky (176) -> Forest (351) -> Molten Volcanic (700)
 * - Manual-only reward claiming and accurate live frontier progression
 */
@Composable
fun RoadmapScreen(
    viewModel: IndividualViewModel,
    @Suppress("UNUSED_PARAMETER") onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    initialMainTab: Int = 0,
    onMainTabChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val appLayoutDirection = LocalLayoutDirection.current

    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val claimedStations by viewModel.claimedStations.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var selectedMainTab by remember(initialMainTab) { mutableIntStateOf(initialMainTab.coerceIn(0, 1)) } // 0: Roadmap, 1: Elite Missions
    var isBiomeMenuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedMainTab != 0) {
        selectedMainTab = 0
        onMainTabChanged(0)
    }
    BackHandler(enabled = isBiomeMenuExpanded) {
        isBiomeMenuExpanded = false
    }

    val stageProgress = remember(totalXp) { RoadmapManager.getStageProgress(totalXp) }
    val currentStationId = stageProgress.unlockedStationId.coerceIn(0, TaskPools.TOTAL_STATIONS_COUNT)
    val scrollAnchorStationId = currentStationId.coerceAtLeast(1)
    val roadmapListState = rememberLazyListState()

    val stations = remember(claimedStations, totalXp) {
        RoadmapManager.generateStations(totalXp, claimedStations)
    }

    var lastAutoScrolledLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedMainTab, currentStationId) {
        if (selectedMainTab == 0 && currentStationId > 2 && lastAutoScrolledLevel != currentStationId) {
            lastAutoScrolledLevel = currentStationId
            val targetIdx = (currentStationId - 2).coerceIn(0, stations.lastIndex)
            roadmapListState.scrollToItem(targetIdx)
        }
    }

    val rawContinuousStation by remember(selectedMainTab, scrollAnchorStationId, stations.size) {
        derivedStateOf {
            if (selectedMainTab != 0) {
                scrollAnchorStationId.toFloat().coerceIn(1f, TaskPools.TOTAL_STATIONS_COUNT.toFloat())
            } else {
                val layoutInfo = roadmapListState.layoutInfo
                val visible = layoutInfo.visibleItemsInfo
                if (visible.isEmpty()) {
                    scrollAnchorStationId.toFloat().coerceIn(1f, TaskPools.TOTAL_STATIONS_COUNT.toFloat())
                } else {
                    val first = visible.first()
                    val last = visible.last()
                    val maxStationF = TaskPools.TOTAL_STATIONS_COUNT.toFloat()
                    when {
                        first.index == 0 && first.offset >= -12 -> 1f
                        last.index == stations.lastIndex && last.offset + last.size <= layoutInfo.viewportEndOffset + 24 -> maxStationF
                        else -> {
                            val midY = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) * 0.5f
                            val centerItem = visible.minByOrNull { item ->
                                kotlin.math.abs((item.offset + item.size * 0.5f) - midY)
                            } ?: first
                            val frac = if (centerItem.size > 0) {
                                ((midY - centerItem.offset) / centerItem.size.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }
                            (centerItem.index + 1 + (frac - 0.5f)).coerceIn(1f, maxStationF)
                        }
                    }
                }
            }
        }
    }
    val animatedStationPos by animateFloatAsState(
        targetValue = rawContinuousStation,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "biome_continuous_station"
    )
    val viewportBiomeState = roadmapBiomeStateForStation(animatedStationPos)
    val glacialTransition = rememberInfiniteTransition(label = "glacial_atmosphere")
    val glacialDriftPhase by glacialTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glacial_drift_phase"
    )

    val rtlSign = if (appLayoutDirection == LayoutDirection.Rtl) -1 else 1
    val tabAquaAccent = if (isDarkTheme) NeonAqua else DayCyan
    val tabEliteAccent = if (isDarkTheme) NeonAmber else DayOrange

    val scaffoldPadding = LocalScaffoldContentPadding.current
    val density = LocalDensity.current
    var topHeaderHeightPx by remember { mutableIntStateOf(0) }
    val topHeaderClearanceDp = if (topHeaderHeightPx > 0) {
        with(density) { topHeaderHeightPx.toDp() }
    } else {
        scaffoldPadding.calculateTopPadding() + 70.dp
    }
    val roadmapTopFadeHeightDp = topHeaderClearanceDp + 18.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("roadmap_screen")
    ) {
        if (selectedMainTab == 0) {
            RoadmapBiomeBackdrop(
                biomeState = viewportBiomeState,
                driftPhase = glacialDriftPhase,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            RoadmapAmbientBackground(
                modifier = Modifier.fillMaxSize(),
                darkTheme = isDarkTheme
            )
        }

        AnimatedContent(
            targetState = selectedMainTab,
            modifier = Modifier
                .fillMaxSize()
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .edgeFadeInBlur(
                                    topHeight = roadmapTopFadeHeightDp,
                                    bottomHeight = 0.dp,
                                    blurRadius = 22.dp,
                                    maxEdgeFraction = 0.48f
                                )
                        ) {
                            RoadmapStationsTabContent(
                                stations = stations,
                                roadmapListState = roadmapListState,
                                nextStationId = stageProgress.nextStationId,
                                currentStageXp = stageProgress.currentStageXp,
                                unlockedStationId = currentStationId,
                                currentLang = currentLang,
                                isDarkTheme = isDarkTheme,
                                topInset = topHeaderClearanceDp,
                                bottomInset = scaffoldPadding.calculateBottomPadding(),
                                onClaimStation = { stationId ->
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    viewModel.claimStation(stationId)
                                }
                            )
                        }
                        TopFrostedLiquidGlassVeil(
                            height = roadmapTopFadeHeightDp,
                            isDarkTheme = isDarkTheme,
                            testTag = "roadmap_expanded_fade_veil"
                        )
                    }
                }

                else -> {
                    EliteMissionsSoonPage(
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme,
                        topInset = topHeaderClearanceDp,
                        bottomInset = scaffoldPadding.calculateBottomPadding()
                    )
                }
            }
        }

        if (selectedMainTab == 0) {
            RoadmapBiomeSubmersionOverlay(
                biomeState = viewportBiomeState,
                driftPhase = glacialDriftPhase,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        }

        // FLOATING TOP HEADER: Unified [ Roadmap · Glacial Peaks | Elite Missions ] tabs directly below Top Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(
                    top = scaffoldPadding.calculateTopPadding() + 6.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { topHeaderHeightPx = it.height }
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoadmapMergedTabButton(
                    label = stringResource(R.string.roadmap_tab_stations),
                    biomeState = viewportBiomeState,
                    currentLang = currentLang,
                    selected = selectedMainTab == 0,
                    expanded = isBiomeMenuExpanded && selectedMainTab == 0,
                    accentColor = tabAquaAccent,
                    isDarkTheme = isDarkTheme,
                    onSelectTab = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        if (selectedMainTab != 0) {
                            isBiomeMenuExpanded = false
                            selectedMainTab = 0
                            onMainTabChanged(0)
                        } else {
                            isBiomeMenuExpanded = !isBiomeMenuExpanded
                        }
                    },
                    onToggleBiomeMenu = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        if (selectedMainTab != 0) {
                            selectedMainTab = 0
                            onMainTabChanged(0)
                            isBiomeMenuExpanded = true
                        } else {
                            isBiomeMenuExpanded = !isBiomeMenuExpanded
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                RoadmapPillTabButton(
                    label = stringResource(R.string.roadmap_tab_elite_missions),
                    icon = Icons.Default.MilitaryTech,
                    selected = selectedMainTab == 1,
                    accentColor = tabEliteAccent,
                    isDarkTheme = isDarkTheme,
                    onClick = {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        isBiomeMenuExpanded = false
                        selectedMainTab = 1
                        onMainTabChanged(1)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("roadmap_tab_elite_missions")
                )
            }

            AnimatedVisibility(
                visible = isBiomeMenuExpanded && selectedMainTab == 0,
                enter = fadeIn(tween(150)) + expandVertically(tween(180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(150))
            ) {
                RoadmapBiomeQuickJumpMenu(
                    activeBiome = viewportBiomeState.dominantBiome,
                    currentAnchorStationId = scrollAnchorStationId,
                    currentLang = currentLang,
                    isDarkTheme = isDarkTheme,
                    onJumpToStation = { targetStationId ->
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        isBiomeMenuExpanded = false
                        selectedMainTab = 0
                        onMainTabChanged(0)
                        val targetIdx = (targetStationId - 1).coerceIn(0, stations.lastIndex)
                        coroutineScope.launch {
                            roadmapListState.animateScrollToItem(targetIdx)
                        }
                    }
                )
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
    unlockedStationId: Int,
    currentLang: String,
    isDarkTheme: Boolean,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
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

    LazyColumn(
        state = roadmapListState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("roadmap_stations_list"),
        contentPadding = PaddingValues(top = topInset + 6.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(stations, key = { _, station -> station.id }) { index, station ->
            val prevState = stations.getOrNull(index - 1)?.state
            val nextState = stations.getOrNull(index + 1)?.state

            WavySnakeStationRow(
                station = station,
                prevStationState = prevState,
                nextStationState = nextState,
                unlockedStationId = unlockedStationId,
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

@Composable
private fun EliteMissionsSoonPage(
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
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
            .padding(
                top = topInset + 12.dp,
                bottom = bottomInset + 16.dp,
                start = 24.dp,
                end = 24.dp
            )
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
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color = NeonAqua,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseSurface = if (isDarkTheme) Color(0xBC0D1628) else Color(0xCEFFFFFF)
    val unselectedBg = baseSurface
    val selectedBg = accentColor.copy(alpha = if (isDarkTheme) 0.24f else 0.16f).compositeOver(baseSurface)
    val unselectedBorder = if (isDarkTheme) Color(0x44FFFFFF) else Color(0xCCFFFFFF)
    val unselectedText = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextSecondary

    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedBg else unselectedBg,
        animationSpec = tween(150),
        label = "pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.88f) else unselectedBorder,
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
            .height(52.dp)
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

@Composable
private fun RoadmapMergedTabButton(
    label: String,
    biomeState: RoadmapBiomeState,
    currentLang: String,
    selected: Boolean,
    expanded: Boolean,
    accentColor: Color,
    isDarkTheme: Boolean,
    onSelectTab: () -> Unit,
    onToggleBiomeMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val biomeAccent = biomePrimaryAccent(biomeState, isDarkTheme)
    val baseSurface = if (isDarkTheme) Color(0xBC0D1628) else Color(0xCEFFFFFF)
    val selectedBg = accentColor.copy(alpha = if (isDarkTheme) 0.24f else 0.16f).compositeOver(baseSurface)
    val unselectedBorder = if (isDarkTheme) Color(0x44FFFFFF) else Color(0xCCFFFFFF)
    val unselectedText = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextSecondary

    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedBg else baseSurface,
        animationSpec = tween(150),
        label = "merged_pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) biomeAccent.copy(alpha = 0.88f) else unselectedBorder,
        animationSpec = tween(150),
        label = "merged_pill_border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else unselectedText,
        animationSpec = tween(150),
        label = "merged_pill_content"
    )

    val biomeLabel = biomeDisplayLabel(biomeState.dominantBiome, currentLang)
    val biomeIcon = biomeIconFor(biomeState.dominantBiome)

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (selected) 1.2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSelectTab)
                .testTag("roadmap_tab_stations"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(biomeAccent.copy(alpha = if (isDarkTheme) (if (expanded) 0.24f else 0.14f) else (if (expanded) 0.16f else 0.09f)))
                .clickable(onClick = onToggleBiomeMenu)
                .padding(horizontal = 6.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = biomeLabel
                }
                .testTag("roadmap_biome_glacial_btn"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = biomeIcon,
                contentDescription = null,
                tint = biomeAccent,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = biomeLabel,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.92f) else DayTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = biomeAccent,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private fun biomeDisplayLabel(biome: RoadmapBiome, currentLang: String): String = when (biome) {
    RoadmapBiome.GLACIAL -> when (currentLang) {
        "ar" -> "القمم الجليدية"
        "fr" -> "Sommets Glaciaux"
        else -> "Glacial Peaks"
    }
    RoadmapBiome.ROCKY -> when (currentLang) {
        "ar" -> "المرتفعات الصخرية"
        "fr" -> "Crêtes Rocheuses"
        else -> "Rocky Ridge"
    }
    RoadmapBiome.FOREST -> when (currentLang) {
        "ar" -> "الغابة الكثيفة"
        "fr" -> "Forêt Dense"
        else -> "Emerald Forest"
    }
    RoadmapBiome.VOLCANIC -> when (currentLang) {
        "ar" -> "القلب البركاني"
        "fr" -> "Cœur Volcanique"
        else -> "Volcanic Core"
    }
}

private fun biomeIconFor(biome: RoadmapBiome): ImageVector = when (biome) {
    RoadmapBiome.GLACIAL -> Icons.Default.AcUnit
    RoadmapBiome.ROCKY -> Icons.Default.Landscape
    RoadmapBiome.FOREST -> Icons.Default.Park
    RoadmapBiome.VOLCANIC -> Icons.Default.LocalFireDepartment
}


@Composable
private fun RoadmapBiomeQuickJumpMenu(
    activeBiome: RoadmapBiome,
    currentAnchorStationId: Int,
    currentLang: String,
    isDarkTheme: Boolean,
    onJumpToStation: (Int) -> Unit
) {
    val panelBg = if (isDarkTheme) Color(0xEC091323) else Color(0xF6FFFFFF)
    val panelBorder = if (isDarkTheme) Color(0x4038BDF8) else Color(0x400284C7)
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    val entries = listOf(
        Triple(RoadmapBiome.GLACIAL, 1, "1 – 175"),
        Triple(RoadmapBiome.ROCKY, 176, "176 – 350"),
        Triple(RoadmapBiome.FOREST, 351, "351 – 525"),
        Triple(RoadmapBiome.VOLCANIC, 526, "526 – 700")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(panelBg)
            .border(1.2.dp, panelBorder, RoundedCornerShape(18.dp))
            .padding(10.dp)
            .testTag("roadmap_biome_jump_menu"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEach { (biome, startStation, rangeLabel) ->
            val biomeAccent = biomePrimaryAccent(roadmapBiomeStateForStation(startStation.toFloat()), isDarkTheme)
            val isSelected = biome == activeBiome
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) biomeAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) biomeAccent.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onJumpToStation(startStation) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("roadmap_biome_jump_$startStation"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = biomeIconFor(biome),
                        contentDescription = null,
                        tint = biomeAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = biomeDisplayLabel(biome, currentLang),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        color = titleColor
                    )
                }
                Text(
                    text = rangeLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                    color = if (isSelected) biomeAccent else mutedColor
                )
            }
        }

        val currentLabel = when (currentLang) {
            "ar" -> "الانتقال لموقعي"
            "fr" -> "Ma position"
            else -> "My Position"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonAqua.copy(alpha = if (isDarkTheme) 0.12f else 0.08f))
                .clickable { onJumpToStation(currentAnchorStationId) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("roadmap_biome_jump_current"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = if (isDarkTheme) NeonAqua else DayCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = currentLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = titleColor
                )
            }
            Text(
                text = "#$currentAnchorStationId",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = if (isDarkTheme) NeonAqua else DayCyan
            )
        }
    }
}

