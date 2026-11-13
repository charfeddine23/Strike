package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FormatListNumbered
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.flame.ActiveShieldType
import com.example.data.flame.FlameManager
import com.example.data.update.AppUpdateState
import com.example.ui.components.AppUpdateCardDialog
import com.example.ui.components.DnaHeaderIcon
import com.example.ui.components.DynamicStreakShieldHeaderButton
import com.example.ui.components.FlameEnergyBanner
import com.example.ui.components.FullscreenPatternUnlockScreen
import com.example.ui.components.GlowingFlameHeaderIcon
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviationsScreen
import com.example.ui.screens.FlameScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.RoadmapScreen
import com.example.ui.screens.StrikeScreen
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
    DASHBOARD,
    INVENTORY,
    STRIKE,
    COMPARISON,
    FLAME,
    DEVIATIONS,
    ROADMAP,
    PROFILE
}

class MainActivity : ComponentActivity() {
    private val viewModel: IndividualViewModel by viewModels {
        IndividualViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallNotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()

        setContent {
            // Theme automatically follows system theme
            val isDarkTheme = isSystemInDarkTheme()
            val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()

            val locale = when (currentLang) {
                "ar" -> Locale("ar")
                "fr" -> Locale("fr")
                else -> Locale("en")
            }
            // Keep layout direction strictly LTR across all languages to prevent UI mirroring
            val layoutDirection = LayoutDirection.Ltr

            val currentContext = LocalContext.current
            val localizedContext = remember(currentLang) {
                val config = Configuration(currentContext.resources.configuration)
                config.setLocale(locale)
                config.setLayoutDirection(Locale.ENGLISH)
                currentContext.createConfigurationContext(config)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                StrikePulseTheme(darkTheme = isDarkTheme) {
                    LiquidGlassBackground(darkTheme = isDarkTheme) {
                        if (!isAppUnlocked) {
                            FullscreenPatternUnlockScreen(
                                onUnlockSuccess = { /* state handled in viewmodel */ },
                                onVerifyPattern = { pattern ->
                                    viewModel.verifyAndUnlock(pattern)
                                }
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
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showShieldDropdown by remember { mutableStateOf(false) }
    var showStartDateDialog by remember { mutableStateOf(false) }
    var startedUpdateFromModal by remember { mutableStateOf(false) }
    var updateDialogDismissed by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check for updates automatically every single time the user enters or returns to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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
        while (true) {
            currentShieldRemainingMillis = FlameManager.getActiveShieldRemainingMillis()
            delay(1000L)
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

    // Hardware/gesture BackHandler: if not on DASHBOARD, return to DASHBOARD
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val triggerThresholdPx = 42.dp.toPx()
                val bailoutThresholdPx = 28.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val pointerId = down.id
                        val startX = down.position.x
                        val startY = down.position.y
                        val isInLeftHalf = startX <= size.width * 0.5f

                        if (isInLeftHalf && !showSettingsDialog) {
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) break

                                val dx = change.position.x - startX
                                val dy = change.position.y - startY

                                // Abort early if vertical scrolling or swiping left
                                if (dx < -bailoutThresholdPx) break
                                if (kotlin.math.abs(dy) > bailoutThresholdPx &&
                                    kotlin.math.abs(dy) > kotlin.math.abs(dx)
                                ) {
                                    break
                                }

                                // Trigger Settings when swiping left-to-right across left half of screen
                                if (dx >= triggerThresholdPx && dx > kotlin.math.abs(dy) * 1.2f) {
                                    showSettingsDialog = true
                                    change.consume()
                                    break
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                        onOpenSettings = { currentScreen = AppScreen.PROFILE },
                        onLongPressProfile5s = { viewModel.hardResetAllTaskCompletionStates() },
                        onOpenFlame = { currentScreen = AppScreen.FLAME },
                        onOpenDeviations = { currentScreen = AppScreen.DEVIATIONS },
                        onLongPressFlame = { showStartDateDialog = true },
                        isDarkTheme = isDarkTheme
                    )
                    if (currentScreen != AppScreen.FLAME && currentScreen != AppScreen.DEVIATIONS && currentScreen != AppScreen.ROADMAP && currentScreen != AppScreen.PROFILE) {
                        FlameEnergyBanner(
                            flameScore = flameScore,
                            onClick = { currentScreen = AppScreen.FLAME },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            },
            bottomBar = {
                LiquidGlassBottomBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { screen ->
                        currentScreen = screen
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 68.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width / 4 else -width / 4 })
                        .togetherWith(fadeOut() + slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width / 4 else width / 4 })
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToStrike = { id ->
                            viewModel.selectStrikeIndividual(id)
                            currentScreen = AppScreen.STRIKE
                        },
                        onNavigateToCompare = { id ->
                            viewModel.selectPersonA(id)
                            currentScreen = AppScreen.COMPARISON
                        }
                    )

                    AppScreen.INVENTORY -> InventoryScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme
                    )

                    AppScreen.STRIKE -> StrikeScreen(
                        viewModel = viewModel
                    )

                    AppScreen.COMPARISON -> ComparisonScreen(
                        viewModel = viewModel
                    )

                    AppScreen.FLAME -> FlameScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                    )

                    AppScreen.DEVIATIONS -> DeviationsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                    )

                    AppScreen.ROADMAP -> RoadmapScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                    )

                    AppScreen.PROFILE -> com.example.ui.screens.ProfileScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
            }
        }
    }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false },
            onLockAppNow = { viewModel.lockApp() }
        )
    }

    // Streak Start Date Dialog (Launched by 2-second hold on the flame icon)
    if (showStartDateDialog) {
        com.example.ui.components.StreakStartDateDialog(
            viewModel = viewModel,
            onDismiss = { showStartDateDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    // Optional In-App Update Card Dialog (user can update immediately or choose "ربما لاحقا")
    val shouldShowUpdateModal = !updateDialogDismissed && !showSettingsDialog && when (appUpdateState) {
        is AppUpdateState.UpdateAvailable,
        is AppUpdateState.Downloading,
        is AppUpdateState.ReadyToInstall -> true
        is AppUpdateState.Error -> startedUpdateFromModal
        else -> false
    }

    if (shouldShowUpdateModal) {
        AppUpdateCardDialog(
            state = appUpdateState,
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
            onDismiss = {
                updateDialogDismissed = true
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
    onOpenSettings: () -> Unit,
    onLongPressProfile5s: () -> Unit = {},
    onOpenFlame: () -> Unit,
    onOpenDeviations: () -> Unit,
    onLongPressFlame: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val title = when (currentScreen) {
        AppScreen.DASHBOARD -> stringResource(R.string.nav_dashboard)
        AppScreen.INVENTORY -> stringResource(R.string.nav_inventory)
        AppScreen.STRIKE -> stringResource(R.string.nav_strike)
        AppScreen.COMPARISON -> stringResource(R.string.nav_compare)
        AppScreen.FLAME -> stringResource(R.string.flame_system_title)
        AppScreen.DEVIATIONS -> stringResource(R.string.deviations_title)
        AppScreen.ROADMAP -> stringResource(R.string.roadmap_title)
        AppScreen.PROFILE -> "الملف الشخصي"
    }

    val cyanColor = if (isDarkTheme) NeonCyan else com.example.ui.theme.DayCyan
    val iconBg = if (isDarkTheme) Color(0xFF0F1E36) else Color(0x2000B4D8)
    val iconBorder = if (isDarkTheme) NeonCyan else com.example.ui.theme.DayCyan.copy(alpha = 0.5f)

    val equippedAvatar by com.example.data.profile.ProfileManager.equippedAvatar.collectAsStateWithLifecycle()
    val equippedTitle by com.example.data.profile.ProfileManager.equippedTitle.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg)
                    .border(1.dp, iconBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = cyanColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = com.example.ui.theme.PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (currentScreen == AppScreen.PROFILE) "الملف الشخصي" else title,
                    style = MaterialTheme.typography.labelSmall,
                    color = cyanColor,
                    fontFamily = com.example.ui.theme.CaveatHandwriting,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sleek, vertical, 3D-styled DNA double helix icon on the left of main flame icon
            DnaHeaderIcon(
                onClick = onOpenDeviations,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Adaptive Glowing Animated "Fire" Header Icon (with 2-second hold detector)
            GlowingFlameHeaderIcon(
                flameScore = flameScore,
                onClick = onOpenFlame,
                onLongClick2s = onLongPressFlame,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Dynamic Streak Shield Element + Strictly Read-Only Monitoring Dropdown
            Box {
                DynamicStreakShieldHeaderButton(
                    isShieldActive = isStreakShieldActive,
                    onClick = onOpenShieldDropdown,
                    isDarkTheme = isDarkTheme
                )

                DropdownMenu(
                    expanded = showShieldDropdown,
                    onDismissRequest = onDismissShieldDropdown,
                    offset = DpOffset(x = (-12).dp, y = 8.dp),
                    modifier = Modifier
                        .width(250.dp)
                        .background(Color(0xF20B1221))
                        .border(
                            1.2.dp,
                            if (isStreakShieldActive) NeonEmerald.copy(alpha = 0.7f) else NeonAmber.copy(alpha = 0.6f),
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
                                    tint = if (isStreakShieldActive) NeonEmerald else NeonAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isStreakShieldActive) activeShieldType.labelAr else "لا يوجد درع نشط",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isStreakShieldActive) NeonEmerald else NeonAmber
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x201E293B))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "الوقت المتبقي",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
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
                                color = if (isStreakShieldActive) NeonCyan else Color(0xFF64748B)
                            )
                        }

                        HorizontalDivider(color = Color(0x2A38BDF8))

                        // Read-only Available Inventory Counts for Each Shield Type
                        ShieldReadOnlyCountRow(
                            label = "درع الـ 24 ساعة",
                            valueText = "$available24hShields",
                            accent = NeonCyan
                        )
                        ShieldReadOnlyCountRow(
                            label = "درع الـ 48 ساعة",
                            valueText = "$available48hShields",
                            accent = NeonEmerald
                        )
                        ShieldReadOnlyCountRow(
                            label = "درع الـ 72 ساعة",
                            valueText = "$available72hShields",
                            accent = NeonAmber
                        )
                        ShieldReadOnlyCountRow(
                            label = "درع الـ 7 أيام (تلقائي)",
                            valueText = if (isStreakShieldActive && activeShieldType == ActiveShieldType.SHIELD_7D_AUTO) {
                                "نشط (1)"
                            } else {
                                "$sevenDayAutoShieldCount"
                            },
                            accent = Color(0xFFFFD700)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dynamic Circular Profile Avatar Icon (Short tap -> Profile, 5s hold -> Hard Task Reset)
            com.example.ui.components.ProfileAvatarHeaderIcon(
                avatar = equippedAvatar,
                title = equippedTitle,
                onClick = onOpenSettings,
                onLongClick5s = onLongPressProfile5s,
                size = 38.dp
            )
        }
    }
}

@Composable
private fun ShieldReadOnlyCountRow(
    label: String,
    valueText: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFE2E8F0),
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
    onScreenSelected: (AppScreen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(com.example.ui.theme.DarkNavBg)
                .border(1.dp, com.example.ui.theme.DarkNavBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.FormatListNumbered,
                label = stringResource(R.string.nav_dashboard),
                isSelected = currentScreen == AppScreen.DASHBOARD,
                activeColor = NeonCyan,
                testTag = "nav_tab_dashboard",
                onClick = { onScreenSelected(AppScreen.DASHBOARD) }
            )

            BottomNavItem(
                icon = Icons.Default.Inventory2,
                label = stringResource(R.string.nav_inventory),
                isSelected = currentScreen == AppScreen.INVENTORY,
                activeColor = NeonEmerald,
                testTag = "nav_tab_inventory",
                onClick = { onScreenSelected(AppScreen.INVENTORY) }
            )

            BottomNavItem(
                icon = Icons.Default.Bolt,
                label = stringResource(R.string.nav_strike),
                isSelected = currentScreen == AppScreen.STRIKE,
                activeColor = NeonAmber,
                testTag = "nav_tab_strike",
                onClick = { onScreenSelected(AppScreen.STRIKE) }
            )

            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                label = stringResource(R.string.nav_compare),
                isSelected = currentScreen == AppScreen.COMPARISON,
                activeColor = NeonViolet,
                testTag = "nav_tab_compare",
                onClick = { onScreenSelected(AppScreen.COMPARISON) }
            )

            BottomNavItem(
                icon = Icons.Default.Timeline,
                label = stringResource(R.string.roadmap_nav_title),
                isSelected = currentScreen == AppScreen.ROADMAP,
                activeColor = NeonAmber,
                testTag = "nav_tab_roadmap",
                onClick = { onScreenSelected(AppScreen.ROADMAP) }
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
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) activeColor.copy(alpha = 0.8f) else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = activeColor,
                    fontFamily = com.example.ui.theme.PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
