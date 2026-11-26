package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.TopBottomEdgeFadeBlurOverlay
import com.example.ui.components.edgeFadeInBlur
import com.example.data.flame.ActiveShieldType
import com.example.data.flame.FlameManager
import com.example.data.update.AppUpdateState
import com.example.ui.components.AppUpdateCardDialog
import com.example.ui.components.DnaHeaderIcon
import com.example.ui.components.DynamicStreakShieldHeaderButton
import com.example.ui.components.FullscreenFingerprintUnlockScreen
import com.example.ui.components.GlowingFlameHeaderIcon
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LuxurySettingsMenuButton
import com.example.ui.components.RecordsDashboardExpandablePanel
import com.example.ui.components.SettingsSideDrawer
import com.example.ui.map.isRobolectricRuntime
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviationsScreen
import com.example.ui.screens.FirstStrikeHeatmapScreen
import com.example.ui.screens.FlameScreen
import com.example.ui.screens.HeatmapMapController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.RoadmapScreen
import com.example.ui.screens.StrikeScreen
import com.example.ui.screens.TasksLedgerScreen
import com.example.ui.screens.XpLogsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CosmicDarkSurface
import com.example.ui.theme.DarkNavBg
import com.example.ui.theme.DarkNavBorder
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LightNavBg
import com.example.ui.theme.LightNavBorder
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalScaffoldContentPadding
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StrikePulseTheme
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.CallNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

enum class AppScreen {
    HOME,
    DASHBOARD,
    INVENTORY,
    STRIKE,
    COMPARISON,
    FLAME,
    DEVIATIONS,
    ROADMAP,
    TASKS_LEDGER,
    XP_LOGS,
    PROFILE
}

class MainActivity : FragmentActivity() {
    private val viewModel: IndividualViewModel by viewModels {
        IndividualViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallNotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }
            val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
            val isScreenshotBlockingEnabled = viewModel.isScreenshotBlockingEnabled.value
            val isScreenSecureRequired = isScreenshotBlockingEnabled || !isAppUnlocked

            val rootView = LocalView.current
            if (!rootView.isInEditMode) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, rootView)
                    controller.isAppearanceLightStatusBars = !isDarkTheme
                    controller.isAppearanceLightNavigationBars = !isDarkTheme

                    if (isScreenSecureRequired) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }

            val locale = when (currentLang) {
                "ar" -> Locale("ar")
                "fr" -> Locale("fr")
                else -> Locale("en")
            }
            // Mirror layout automatically: RTL for Arabic, LTR for English/French
            val layoutDirection = if (currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            val currentContext = LocalContext.current
            val localizedContext = remember(currentLang, currentContext) {
                val config = Configuration(currentContext.resources.configuration)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                val resContext = currentContext.createConfigurationContext(config)
                object : ContextWrapper(this@MainActivity) {
                    override fun getResources(): Resources = resContext.resources
                    override fun getAssets(): AssetManager = resContext.assets
                    override fun createConfigurationContext(
                        overrideConfiguration: Configuration
                    ): Context = resContext.createConfigurationContext(overrideConfiguration)
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                StrikePulseTheme(darkTheme = isDarkTheme) {
                    LiquidGlassBackground(darkTheme = isDarkTheme) {
                        if (!isAppUnlocked) {
                            val isHapticEnabled by viewModel.isHapticFeedbackEnabled
                            FullscreenFingerprintUnlockScreen(
                                onUnlockSuccess = { viewModel.unlockWithFingerprint() },
                                isHapticEnabled = isHapticEnabled
                            )
                        } else {
                            LiquidGlassApp(
                                viewModel = viewModel,
                                isDarkTheme = isDarkTheme,
                                currentLang = currentLang,
                                onSelectLanguage = { viewModel.setLanguage(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassApp(
    viewModel: IndividualViewModel,
    isDarkTheme: Boolean,
    currentLang: String,
    onSelectLanguage: (String) -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val heatmapMapController = remember {
        if (isRobolectricRuntime()) null else HeatmapMapController(appContext)
    }
    DisposableEffect(Unit) {
        onDispose {
            heatmapMapController?.destroy()
        }
    }

    val curtainScope = rememberCoroutineScope()
    // 0f = Full-screen First-Strike Heatmap (sharp focus), 1f = Main Application Pages (blurred-in focus)
    val heatmapCurtainProgress = remember { Animatable(0f) }
    val showHeatmapEntry = heatmapCurtainProgress.value < 0.5f
    fun animateCurtainTo(target: Float, initialVelocityProgress: Float = 0f) {
        curtainScope.launch {
            heatmapCurtainProgress.animateTo(
                targetValue = target.coerceIn(0f, 1f),
                initialVelocity = initialVelocityProgress,
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var roadmapMainTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showShieldDropdown by remember { mutableStateOf(false) }
    var showStartDateDialog by remember { mutableStateOf(false) }
    var showRecordsDashboardDialog by remember { mutableStateOf(false) }
    var startedUpdateFromModal by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val allIndividuals by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val isDataEntryOpen by viewModel.isDataEntryOpen.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Automatically check for updates when entering the app (including after fingerprint unlock)
    LaunchedEffect(Unit) {
        viewModel.refreshTaskCycles()
        viewModel.checkForAppUpdate(isAutoCheck = true)
    }

    // Check for updates & refresh dynamic task cycles every time the user returns to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTaskCycles()
                viewModel.checkForAppUpdate(isAutoCheck = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val flameScore by viewModel.flameScore.collectAsStateWithLifecycle()
    val isStreakShieldActive by viewModel.isStreakShieldActive.collectAsStateWithLifecycle()
    val activeShieldExpiresAt by viewModel.activeShieldExpiresAt.collectAsStateWithLifecycle()
    val activeShieldType by viewModel.activeShieldType.collectAsStateWithLifecycle()
    val availableManualShields by viewModel.availableManualShields.collectAsStateWithLifecycle()
    val available48hShields by viewModel.available48hShields.collectAsStateWithLifecycle()
    val available1DayShields by viewModel.available1DayShields.collectAsStateWithLifecycle()
    val sevenDayAutoShieldCount by viewModel.sevenDayAutoShieldCount.collectAsStateWithLifecycle()
    var currentShieldRemainingMillis by remember { mutableLongStateOf(FlameManager.getActiveShieldRemainingMillis()) }

    LaunchedEffect(isStreakShieldActive, activeShieldExpiresAt) {
        if (isStreakShieldActive) {
            while (true) {
                currentShieldRemainingMillis = FlameManager.getActiveShieldRemainingMillis()
                if (activeShieldExpiresAt != null && currentShieldRemainingMillis <= 0L) break
                delay(1000L)
            }
        } else {
            currentShieldRemainingMillis = 0L
        }
    }

    val shieldActuallyActive = isStreakShieldActive && (activeShieldExpiresAt == null || currentShieldRemainingMillis > 0L)
    val effectiveActiveShieldType = if (shieldActuallyActive) {
        activeShieldType.takeIf { it != ActiveShieldType.NONE } ?: ActiveShieldType.SHIELD_72H
    } else {
        ActiveShieldType.NONE
    }

    // Listen to ViewModel events (including Swipe-to-delete with Undo)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is com.example.ui.viewmodel.UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is com.example.ui.viewmodel.UiEvent.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteStrikeEvent(event.strikeEvent)
                    }
                }
            }
        }
    }

    val isHapticEnabled by viewModel.isHapticFeedbackEnabled

    val rtlSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1

    LaunchedEffect(currentScreen) {
        showShieldDropdown = false
        if (currentScreen != AppScreen.DASHBOARD) {
            showRecordsDashboardDialog = false
        }
    }

    // Hardware/gesture BackHandler: return from TASKS_LEDGER or XP_LOGS to ROADMAP, otherwise return to HOME
    BackHandler(enabled = !showHeatmapEntry && currentScreen != AppScreen.HOME) {
        currentScreen = if (currentScreen == AppScreen.TASKS_LEDGER || currentScreen == AppScreen.XP_LOGS) {
            AppScreen.ROADMAP
        } else {
            AppScreen.HOME
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fullHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        val dragSpanPx = (fullHeightPx * 0.62f).coerceAtLeast(1f)
        val maxHeatmapBlurPx = with(density) { 26.dp.toPx() }
        val maxPagesBlurPx = with(density) { 24.dp.toPx() }
        val curtainProgress = heatmapCurtainProgress.value.coerceIn(0f, 1f)
        val showMainPages = curtainProgress > 0.001f
        val showHeatmapOverlay = curtainProgress < 0.999f
        val isTransitioning = curtainProgress in 0.001f..0.999f
        val transitionArc = sin(curtainProgress * PI).toFloat().coerceIn(0f, 1f)
        val canUseRenderBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isRobolectricRuntime()

        if (showMainPages) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .graphicsLayer {
                    // Blur In: soft optical bokeh (24dp) sharpens into razor focus (0dp) as pages arrive
                    val blurPx = (1f - curtainProgress) * maxPagesBlurPx
                    renderEffect = if (canUseRenderBlur && blurPx > 0.5f) {
                        RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP).asComposeRenderEffect()
                    } else {
                        null
                    }
                    val scale = 0.92f + (0.08f * curtainProgress)
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - curtainProgress) * fullHeightPx * 0.08f
                    alpha = (curtainProgress / 0.72f).coerceIn(0f, 1f)
                }
        ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                LiquidGlassTopBar(
                    currentScreen = currentScreen,
                    flameScore = flameScore,
                    isStreakShieldActive = shieldActuallyActive,
                    activeShieldType = effectiveActiveShieldType,
                    shieldRemainingMillis = currentShieldRemainingMillis,
                    available24hShields = available1DayShields,
                    available48hShields = available48hShields,
                    available72hShields = availableManualShields,
                    sevenDayAutoShieldCount = sevenDayAutoShieldCount,
                    showShieldDropdown = showShieldDropdown,
                    onOpenShieldDropdown = { showShieldDropdown = true },
                    onDismissShieldDropdown = { showShieldDropdown = false },
                    onOpenProfile = { currentScreen = AppScreen.PROFILE },
                    onOpenSettings = { showSettingsDialog = true },
                    onLongPressProfile5s = { viewModel.hardResetAllTaskCompletionStates() },
                    onOpenFlame = { currentScreen = AppScreen.FLAME },
                    onOpenDeviations = { currentScreen = AppScreen.DEVIATIONS },
                    onLongPressFlame = { showStartDateDialog = true },
                    onPullDownHeatmapDelta = { deltaRootY ->
                        curtainScope.launch {
                            heatmapCurtainProgress.snapTo(
                                (heatmapCurtainProgress.value - (deltaRootY / dragSpanPx)).coerceIn(0f, 1f)
                            )
                        }
                    },
                    onPullDownHeatmapEnd = { velRootY ->
                        val velProgress = -velRootY / dragSpanPx
                        if (heatmapCurtainProgress.value <= 0.78f || velRootY >= 420f) {
                            animateCurtainTo(0f, velProgress)
                        } else {
                            animateCurtainTo(1f, velProgress)
                        }
                    },
                    currentLang = currentLang,
                    isDarkTheme = isDarkTheme,
                    isHapticEnabled = isHapticEnabled
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = currentScreen != AppScreen.TASKS_LEDGER && currentScreen != AppScreen.XP_LOGS,
                    enter = expandVertically(tween(170, easing = FastOutSlowInEasing)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(140, easing = FastOutLinearInEasing)) + fadeOut(tween(100))
                ) {
                    LiquidGlassBottomBar(
                        currentScreen = currentScreen,
                        isDarkTheme = isDarkTheme,
                        onScreenSelected = { screen ->
                            if (screen != AppScreen.DASHBOARD) {
                                showRecordsDashboardDialog = false
                            }
                            currentScreen = screen
                        }
                    )
                }
            },
            snackbarHost = {
                val bottomBarHidden = currentScreen == AppScreen.TASKS_LEDGER || currentScreen == AppScreen.XP_LOGS
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (bottomBarHidden) {
                        Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 12.dp)
                    } else {
                        Modifier.padding(bottom = 8.dp)
                    }
                )
            }
        ) { innerPadding ->
            CompositionLocalProvider(LocalScaffoldContentPadding provides innerPadding) {
                val topBlurHeight = innerPadding.calculateTopPadding() + 20.dp
                val bottomBlurHeight = innerPadding.calculateBottomPadding().coerceAtLeast(32.dp) + 22.dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .edgeFadeInBlur(
                                topHeight = topBlurHeight,
                                bottomHeight = bottomBlurHeight
                            )
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
                transitionSpec = {
                    val hubIndex = mapOf(
                        AppScreen.TASKS_LEDGER to -1,
                        AppScreen.ROADMAP to 0,
                        AppScreen.XP_LOGS to 1
                    )
                    val initialHub = hubIndex[initialState]
                    val targetHub = hubIndex[targetState]
                    val dir = if (initialHub != null && targetHub != null) {
                        if (targetHub >= initialHub) 1 else -1
                    } else {
                        (if (targetState.ordinal >= initialState.ordinal) 1 else -1) * rtlSign
                    }
                    val exitAnim = fadeOut(
                        animationSpec = tween(durationMillis = 65, easing = FastOutLinearInEasing)
                    ) + slideOutHorizontally(
                        animationSpec = tween(durationMillis = 70, easing = FastOutLinearInEasing)
                    ) { fullWidth -> -dir * (fullWidth / 16) }
                    val enterAnim = fadeIn(
                        animationSpec = tween(durationMillis = 165, delayMillis = 65, easing = FastOutSlowInEasing)
                    ) + slideInHorizontally(
                        animationSpec = tween(durationMillis = 180, delayMillis = 65, easing = FastOutSlowInEasing)
                    ) { fullWidth -> dir * (fullWidth / 12) }
                    enterAnim.togetherWith(exitAnim).using(SizeTransform(clip = true))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onOpenHeatmap = { animateCurtainTo(0f) },
                        onOpenRecords = { currentScreen = AppScreen.DASHBOARD },
                        onOpenAddStrike = { currentScreen = AppScreen.STRIKE },
                        onOpenRoadmap = { currentScreen = AppScreen.ROADMAP },
                        isDarkTheme = isDarkTheme
                    )

                    AppScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToStrike = { id ->
                            viewModel.selectStrikeIndividual(id)
                            currentScreen = AppScreen.STRIKE
                        },
                        onNavigateToCompare = { id ->
                            viewModel.selectPersonA(id)
                            currentScreen = AppScreen.COMPARISON
                        },
                        hostDataEntryExternally = true,
                        isDarkTheme = isDarkTheme,
                        isDashboardOpen = showRecordsDashboardDialog,
                        onOpenDashboard = { showRecordsDashboardDialog = !showRecordsDashboardDialog },
                        onNavigateBack = { currentScreen = AppScreen.HOME }
                    )

                    AppScreen.INVENTORY -> InventoryScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme
                    )

                    AppScreen.STRIKE -> StrikeScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onNavigateBack = { currentScreen = AppScreen.HOME }
                    )

                    AppScreen.COMPARISON -> ComparisonScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme
                    )

                    AppScreen.FLAME -> FlameScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.HOME },
                        isDarkTheme = isDarkTheme
                    )

                    AppScreen.DEVIATIONS -> DeviationsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.HOME },
                        isDarkTheme = isDarkTheme
                    )

                    AppScreen.ROADMAP -> RoadmapScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.HOME },
                        isDarkTheme = isDarkTheme,
                        initialMainTab = roadmapMainTab,
                        onMainTabChanged = { roadmapMainTab = it },
                        onOpenTasksLedger = {
                            roadmapMainTab = 1
                            currentScreen = AppScreen.TASKS_LEDGER
                        },
                        onOpenXpLogs = {
                            currentScreen = AppScreen.XP_LOGS
                        }
                    )

                    AppScreen.TASKS_LEDGER -> TasksLedgerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.ROADMAP },
                        onOpenRoadmap = { currentScreen = AppScreen.ROADMAP },
                        onOpenXpLogs = { currentScreen = AppScreen.XP_LOGS },
                        isDarkTheme = isDarkTheme
                    )

                    AppScreen.XP_LOGS -> XpLogsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.ROADMAP },
                        onOpenTasksLedger = { currentScreen = AppScreen.TASKS_LEDGER },
                        onOpenRoadmap = { currentScreen = AppScreen.ROADMAP },
                        isDarkTheme = isDarkTheme
                    )

                    AppScreen.PROFILE -> com.example.ui.screens.ProfileScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.HOME },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
                    }

                    TopBottomEdgeFadeBlurOverlay(
                        topHeight = topBlurHeight,
                        bottomHeight = bottomBlurHeight,
                        isDarkTheme = isDarkTheme
                    )

            // Downward-expanding Dashboard Panel opened from the Dashboard button next to Filter/Sort
            RecordsDashboardExpandablePanel(
                visible = currentScreen == AppScreen.DASHBOARD && showRecordsDashboardDialog,
                viewModel = viewModel,
                allList = allIndividuals,
                currentLang = currentLang,
                onDismiss = { showRecordsDashboardDialog = false },
                isDarkTheme = isDarkTheme
            )
                }
            }
        }

        // Right-Side Partial Slide-Out Settings Drawer (overlays Scaffold edge-to-edge)
        SettingsSideDrawer(
            visible = showSettingsDialog,
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false },
            onLockAppNow = { viewModel.lockApp() },
            onOpenHeatmap = {
                showSettingsDialog = false
                animateCurtainTo(0f)
            }
        )

        // Full-Screen In-Tree Data Entry Overlay (overlays Scaffold & bars cleanly without Dialog window leaks)
        if (isDataEntryOpen) {
            com.example.ui.components.DataEntryDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeDataEntry() },
                isDarkTheme = isDarkTheme
            )
        }
        }
        }

        if (isTransitioning) {
            // Atmospheric spatial mist veil between blurred-out map and blurred-in pages
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.5f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                (if (isDarkTheme) Color(0xFF040914) else Color(0xFFEFF6FF))
                                    .copy(alpha = 0.42f * transitionArc),
                                (if (isDarkTheme) NeonCyan else DayCyan)
                                    .copy(alpha = 0.14f * transitionArc),
                                (if (isDarkTheme) Color(0xFF020611) else Color(0xFFF8FAFC))
                                    .copy(alpha = 0.48f * transitionArc)
                            )
                        )
                    )
            )
        }

        if (showHeatmapOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .graphicsLayer {
                        // Blur Out: map recedes softly backward into deep bokeh (0dp -> 26dp)
                        val blurPx = curtainProgress * maxHeatmapBlurPx
                        renderEffect = if (canUseRenderBlur && blurPx > 0.5f) {
                            RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        } else {
                            null
                        }
                        val scale = 1.0f - (0.09f * curtainProgress)
                        scaleX = scale
                        scaleY = scale
                        translationY = -curtainProgress * fullHeightPx * 0.12f
                        alpha = (1f - ((curtainProgress - 0.22f).coerceAtLeast(0f) / 0.78f)).coerceIn(0f, 1f)
                    }
            ) {
                FirstStrikeHeatmapScreen(
                    viewModel = viewModel,
                    currentLang = currentLang,
                    isAppDarkTheme = isDarkTheme,
                    mapController = heatmapMapController,
                    onEnterApp = { animateCurtainTo(1f) },
                    onNavigateToPersonProfile = { personId ->
                        viewModel.resetFilters()
                        viewModel.focusIndividualProfile(personId)
                        currentScreen = AppScreen.DASHBOARD
                        animateCurtainTo(1f)
                    },
                    onCurtainDragDelta = { deltaRootY ->
                        curtainScope.launch {
                            heatmapCurtainProgress.snapTo(
                                (heatmapCurtainProgress.value - (deltaRootY / dragSpanPx)).coerceIn(0f, 1f)
                            )
                        }
                    },
                    onCurtainDragEnd = { velRootY ->
                        val velProgress = -velRootY / dragSpanPx
                        if (heatmapCurtainProgress.value >= 0.22f || velRootY <= -420f) {
                            animateCurtainTo(1f, velProgress)
                        } else {
                            animateCurtainTo(0f, velProgress)
                        }
                    }
                )

                // Subtle luminous cyan optical edge sweeping across mid-transition
                if (isTransitioning) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .graphicsLayer {
                                alpha = transitionArc
                            }
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        NeonCyan.copy(alpha = 0.85f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 124.dp)
                )
            }
        }
    }

    // Streak Start Date Dialog (Launched by 2-second hold on the flame icon)
    if (showStartDateDialog) {
        com.example.ui.components.StreakStartDateDialog(
            viewModel = viewModel,
            onDismiss = { showStartDateDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    // Optional In-App Update Card Dialog (auto-triggered on app entry when a newer version is found)
    val shouldShowUpdateModal = when (appUpdateState) {
        is AppUpdateState.UpdateAvailable,
        is AppUpdateState.Downloading,
        is AppUpdateState.ReadyToInstall -> true
        is AppUpdateState.Error -> startedUpdateFromModal
        else -> false
    }

    LaunchedEffect(shouldShowUpdateModal) {
        if (shouldShowUpdateModal && showSettingsDialog) {
            showSettingsDialog = false
        }
    }

    if (shouldShowUpdateModal) {
        AppUpdateCardDialog(
            state = appUpdateState,
            currentLang = currentLang,
            isMandatory = false,
            onUpdateClick = { release ->
                startedUpdateFromModal = true
                viewModel.downloadAppUpdate(release)
            },
            onInstallClick = { apkFile ->
                viewModel.installAppUpdate(apkFile)
            },
            onRetryClick = {
                viewModel.checkForAppUpdate(isAutoCheck = false)
            },
            onCancelDownload = {
                viewModel.cancelAppUpdateDownload()
            },
            onDismiss = {
                startedUpdateFromModal = false
                viewModel.dismissUpdatePrompt()
            }
        )
    }
}

@Composable
fun LiquidGlassTopBar(
    currentScreen: AppScreen,
    flameScore: Double,
    isStreakShieldActive: Boolean = false,
    activeShieldType: ActiveShieldType = ActiveShieldType.NONE,
    shieldRemainingMillis: Long = 0L,
    available24hShields: Int = 0,
    available48hShields: Int = 0,
    available72hShields: Int = 0,
    sevenDayAutoShieldCount: Int = 0,
    showShieldDropdown: Boolean = false,
    onOpenShieldDropdown: () -> Unit = {},
    onDismissShieldDropdown: () -> Unit = {},
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onLongPressProfile5s: () -> Unit = {},
    onOpenFlame: () -> Unit,
    onOpenDeviations: () -> Unit,
    onLongPressFlame: () -> Unit = {},
    onPullDownHeatmapDelta: ((Float) -> Unit)? = null,
    onPullDownHeatmapEnd: ((Float) -> Unit)? = null,
    currentLang: String = "ar",
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    isHapticEnabled: Boolean = true
) {
    val title = when (currentScreen) {
        AppScreen.HOME -> stringResource(R.string.nav_home)
        AppScreen.DASHBOARD -> stringResource(R.string.nav_dashboard)
        AppScreen.INVENTORY -> stringResource(R.string.nav_inventory)
        AppScreen.STRIKE -> stringResource(R.string.nav_strike)
        AppScreen.COMPARISON -> stringResource(R.string.nav_compare)
        AppScreen.FLAME -> stringResource(R.string.flame_system_title)
        AppScreen.DEVIATIONS -> stringResource(R.string.deviations_title)
        AppScreen.ROADMAP -> stringResource(R.string.roadmap_title)
        AppScreen.TASKS_LEDGER -> when (currentLang) {
            "ar" -> "دفتر المهام"
            "fr" -> "Registre des missions"
            else -> "Tasks Ledger"
        }
        AppScreen.XP_LOGS -> when (currentLang) {
            "ar" -> "سجل نقاط XP"
            "fr" -> "Journaux XP"
            else -> "Xp Logs"
        }
        AppScreen.PROFILE -> when (currentLang) {
            "ar" -> "الملف الشخصي"
            "fr" -> "Profil"
            else -> "Profile"
        }
    }

    val cyanColor = if (isDarkTheme) NeonCyan else com.example.ui.theme.DayCyan

    val equippedAvatar by com.example.data.profile.ProfileManager.equippedAvatar.collectAsStateWithLifecycle()
    val equippedTitle by com.example.data.profile.ProfileManager.equippedTitle.collectAsStateWithLifecycle()

    val topGlassFill = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xAE0D1424),
                Color(0x9C070C17),
                Color(0xAD040811)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xE8FFFFFF),
                Color(0xD2F1F6FC)
            )
        )
    }

    val topGlassBorder = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x62FFFFFF),
                Color(0x22FFFFFF),
                Color(0x3238BDF8)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0x520284C7),
                Color(0x66CBD5E1)
            )
        )
    }

    var topBarCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val latestPullDelta by rememberUpdatedState(onPullDownHeatmapDelta)
    val latestPullEnd by rememberUpdatedState(onPullDownHeatmapEnd)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("top_bar_outer_container")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { topBarCoords = it }
                .clip(RoundedCornerShape(24.dp))
                .background(topGlassFill)
                .border(0.9.dp, topGlassBorder, RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val rootDown = topBarCoords?.localToRoot(down.position) ?: down.position
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, rootDown)
                        var draggingDown = false
                        var lastRootY = rootDown.y
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (draggingDown) {
                                    latestPullEnd?.invoke(tracker.calculateVelocity().y)
                                }
                                break
                            }
                            val rootPos = topBarCoords?.localToRoot(change.position) ?: change.position
                            tracker.addPosition(change.uptimeMillis, rootPos)
                            val totalDy = rootPos.y - rootDown.y
                            val totalDx = rootPos.x - rootDown.x
                            if (!draggingDown) {
                                if (totalDy > slop && totalDy > abs(totalDx) * 1.1f) {
                                    draggingDown = true
                                    val deltaY = rootPos.y - lastRootY
                                    if (deltaY != 0f) {
                                        latestPullDelta?.invoke(deltaY)
                                    }
                                    change.consume()
                                } else if (abs(totalDx) > slop && abs(totalDx) > abs(totalDy)) {
                                    break
                                }
                            } else {
                                val deltaY = rootPos.y - lastRootY
                                if (deltaY != 0f) {
                                    latestPullDelta?.invoke(deltaY)
                                }
                                change.consume()
                            }
                            lastRootY = rootPos.y
                        }
                    }
                }
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile Avatar replaces the old Bolt app icon next to App Name & Screen Title
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.components.ProfileAvatarHeaderIcon(
                    avatar = equippedAvatar,
                    title = equippedTitle,
                    onClick = onOpenProfile,
                    onLongClick5s = onLongPressProfile5s,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = com.example.ui.theme.PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = {
                            fadeIn(tween(130, delayMillis = 40))
                                .togetherWith(fadeOut(tween(40)))
                        },
                        label = "topbar_title_transition"
                    ) { animatedTitle ->
                        Text(
                            text = animatedTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = cyanColor,
                            fontFamily = com.example.ui.theme.CaveatHandwriting,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right/End Cluster: Borderless Shield, Flame, DNA + Luxury 3-Line Settings Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                // Dynamic Streak Shield Element + Strictly Read-Only Monitoring Dropdown
                Box {
                    DynamicStreakShieldHeaderButton(
                        isShieldActive = isStreakShieldActive,
                        onClick = onOpenShieldDropdown,
                        isDarkTheme = isDarkTheme
                    )

                val dropdownBg = if (isDarkTheme) Color(0xF20B1221) else Color(0xFAFFFFFF)
                val dropdownSurfaceInner = if (isDarkTheme) Color(0x201E293B) else Color(0xFFF1F5F9)
                val emeraldTone = if (isDarkTheme) NeonEmerald else DayEmerald
                val amberTone = if (isDarkTheme) NeonAmber else DayOrange
                val cyanTone = if (isDarkTheme) NeonCyan else DayCyan
                val goldTone = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFFB45309)
                val secondaryTextTone = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

                DropdownMenu(
                    expanded = showShieldDropdown,
                    onDismissRequest = onDismissShieldDropdown,
                    offset = DpOffset(x = (-12).dp, y = 8.dp),
                    modifier = Modifier
                        .width(250.dp)
                        .background(dropdownBg)
                        .border(
                            1.2.dp,
                            if (isStreakShieldActive) emeraldTone.copy(alpha = 0.7f) else amberTone.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("top_bar_shield_readonly_dropdown")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Active Shield & Remaining Countdown (Strictly Read-Only)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isStreakShieldActive) emeraldTone else amberTone,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isStreakShieldActive) {
                                        if (currentLang == "ar") activeShieldType.labelAr else activeShieldType.labelEn
                                    } else {
                                        if (currentLang == "ar") "لا يوجد درع نشط" else "No Active Shield"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isStreakShieldActive) emeraldTone else amberTone
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(dropdownSurfaceInner)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = cyanTone,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (currentLang == "ar") "الوقت المتبقي" else "Time Remaining",
                                    fontSize = 11.sp,
                                    color = secondaryTextTone
                                )
                            }
                            Text(
                                text = if (isStreakShieldActive && shieldRemainingMillis > 0L) {
                                    FlameManager.formatRemaining(shieldRemainingMillis)
                                } else {
                                    "0s"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isStreakShieldActive) cyanTone else secondaryTextTone
                            )
                        }

                        HorizontalDivider(
                            color = if (isDarkTheme) Color(0x2A38BDF8) else Color(0xFFCBD5E1)
                        )

                        // Read-only Available Inventory Counts for Each Shield Type
                        ShieldReadOnlyCountRow(
                            label = if (currentLang == "ar") "درع الـ 24 ساعة" else "24h Shield",
                            valueText = "$available24hShields",
                            accent = cyanTone,
                            isDarkTheme = isDarkTheme
                        )
                        ShieldReadOnlyCountRow(
                            label = if (currentLang == "ar") "درع الـ 48 ساعة" else "48h Shield",
                            valueText = "$available48hShields",
                            accent = emeraldTone,
                            isDarkTheme = isDarkTheme
                        )
                        ShieldReadOnlyCountRow(
                            label = if (currentLang == "ar") "درع الـ 72 ساعة" else "72h Shield",
                            valueText = "$available72hShields",
                            accent = amberTone,
                            isDarkTheme = isDarkTheme
                        )
                        ShieldReadOnlyCountRow(
                            label = if (currentLang == "ar") "درع الـ 7 أيام (تلقائي)" else "7-Day Shield (Auto)",
                            valueText = if (isStreakShieldActive && activeShieldType == ActiveShieldType.SHIELD_7D_AUTO) {
                                if (currentLang == "ar") "نشط (1)" else "Active (1)"
                            } else {
                                "$sevenDayAutoShieldCount"
                            },
                            accent = goldTone,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }

            // Adaptive Glowing Animated "Fire" Header Icon (centered between Shield & DNA)
            GlowingFlameHeaderIcon(
                flameScore = flameScore,
                onClick = onOpenFlame,
                onLongClick2s = onLongPressFlame,
                isDarkTheme = isDarkTheme
            )

            // Sleek, vertical, 3D-styled DNA double helix icon
            DnaHeaderIcon(
                onClick = onOpenDeviations,
                isDarkTheme = isDarkTheme
            )
        }

                // Luxury 3-stacked-lines Settings Menu Button at the outer right edge
                LuxurySettingsMenuButton(
                    onClick = onOpenSettings,
                    isDarkTheme = isDarkTheme,
                    isHapticEnabled = isHapticEnabled
                )
            }
        }
        }
    }
}

@Composable
private fun ShieldReadOnlyCountRow(
    label: String,
    valueText: String,
    accent: Color,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = valueText,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accent
        )
    }
}

@Composable
fun LiquidGlassBottomBar(
    currentScreen: AppScreen,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onScreenSelected: (AppScreen) -> Unit
) {
    val dashColor = if (isDarkTheme) NeonCyan else DayCyan
    val invColor = if (isDarkTheme) NeonEmerald else DayEmerald
    val compareColor = if (isDarkTheme) NeonViolet else DayPurple
    val roadmapColor = if (isDarkTheme) NeonAmber else DayOrange

    val navGlassFill = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xB20D1526),
                Color(0xA6080E1B),
                Color(0xBC040811)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xEBFFFFFF),
                Color(0xD6F1F6FC)
            )
        )
    }

    val navGlassBorder = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x68FFFFFF),
                Color(0x22FFFFFF),
                Color(0x3C38BDF8)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0x4D0284C7),
                Color(0x6ECBD5E1)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("bottom_bar_outer_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(navGlassFill)
                .border(0.95.dp, navGlassBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                isSelected = currentScreen == AppScreen.HOME,
                activeColor = dashColor,
                isDarkTheme = isDarkTheme,
                testTag = "nav_tab_home",
                onClick = { onScreenSelected(AppScreen.HOME) },
                modifier = Modifier.weight(1f)
            )

            BottomNavItem(
                icon = Icons.Default.Inventory2,
                label = stringResource(R.string.nav_inventory),
                isSelected = currentScreen == AppScreen.INVENTORY,
                activeColor = invColor,
                isDarkTheme = isDarkTheme,
                testTag = "nav_tab_inventory",
                onClick = { onScreenSelected(AppScreen.INVENTORY) },
                modifier = Modifier.weight(1f)
            )

            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                label = stringResource(R.string.nav_compare),
                isSelected = currentScreen == AppScreen.COMPARISON,
                activeColor = compareColor,
                isDarkTheme = isDarkTheme,
                testTag = "nav_tab_compare",
                onClick = { onScreenSelected(AppScreen.COMPARISON) },
                modifier = Modifier.weight(1f)
            )

            BottomNavItem(
                icon = Icons.Default.Timeline,
                label = stringResource(R.string.roadmap_nav_title),
                isSelected = currentScreen == AppScreen.ROADMAP,
                activeColor = roadmapColor,
                isDarkTheme = isDarkTheme,
                testTag = "nav_tab_roadmap",
                onClick = { onScreenSelected(AppScreen.ROADMAP) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            activeColor.copy(alpha = if (isDarkTheme) 0.20f else 0.16f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(160),
        label = "nav_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            activeColor.copy(alpha = if (isDarkTheme) 0.72f else 0.65f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(160),
        label = "nav_border"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            activeColor
        } else if (isDarkTheme) {
            Color(0xFFB6C2D4)
        } else {
            Color(0xFF475569)
        },
        animationSpec = tween(160),
        label = "nav_tint"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(0.9.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(23.dp)
        )
    }
}
