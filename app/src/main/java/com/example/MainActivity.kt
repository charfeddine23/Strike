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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.flame.FlameManager
import com.example.ui.components.DnaHeaderIcon
import com.example.ui.components.DynamicStreakShieldHeaderButton
import com.example.ui.components.FlameEnergyBanner
import com.example.ui.components.FullscreenPatternUnlockScreen
import com.example.ui.components.GlowingFlameHeaderIcon
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StreakShieldModalSheet
import androidx.compose.material.icons.filled.Timeline
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataEntryScreen
import com.example.ui.screens.DeviationsScreen
import com.example.ui.screens.FlameScreen
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
    DATA_ENTRY,
    STRIKE,
    COMPARISON,
    FLAME,
    DEVIATIONS,
    ROADMAP
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
    var showShieldModal by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val flameScore by viewModel.flameScore.collectAsStateWithLifecycle()
    val isStreakShieldActive by viewModel.isStreakShieldActive.collectAsStateWithLifecycle()
    val activeShieldExpiresAt by viewModel.activeShieldExpiresAt.collectAsStateWithLifecycle()
    val availableManualShields by viewModel.availableManualShields.collectAsStateWithLifecycle()
    val available1DayShields by viewModel.available1DayShields.collectAsStateWithLifecycle()
    var currentShieldRemainingMillis by remember { mutableLongStateOf(FlameManager.getActiveShieldRemainingMillis()) }

    LaunchedEffect(isStreakShieldActive, activeShieldExpiresAt) {
        while (true) {
            currentShieldRemainingMillis = FlameManager.getActiveShieldRemainingMillis()
            delay(1000L)
        }
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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LiquidGlassTopBar(
                    currentScreen = currentScreen,
                    flameScore = flameScore,
                    isStreakShieldActive = isStreakShieldActive,
                    onOpenShieldModal = { showShieldModal = true },
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenFlame = { currentScreen = AppScreen.FLAME },
                    onOpenDeviations = { currentScreen = AppScreen.DEVIATIONS },
                    isDarkTheme = isDarkTheme
                )
                if (currentScreen != AppScreen.FLAME && currentScreen != AppScreen.DEVIATIONS && currentScreen != AppScreen.ROADMAP) {
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
                    if (screen == AppScreen.DATA_ENTRY && currentScreen != AppScreen.DATA_ENTRY) {
                        viewModel.initFormForNew()
                    }
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
                        onNavigateToEntry = { currentScreen = AppScreen.DATA_ENTRY },
                        onNavigateToStrike = { id ->
                            viewModel.selectStrikeIndividual(id)
                            currentScreen = AppScreen.STRIKE
                        },
                        onNavigateToCompare = { id ->
                            viewModel.selectPersonA(id)
                            currentScreen = AppScreen.COMPARISON
                        }
                    )

                    AppScreen.DATA_ENTRY -> DataEntryScreen(
                        viewModel = viewModel,
                        onFinished = { currentScreen = AppScreen.DASHBOARD }
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

    // Streak Shield Modal Sheet (Settings, Inventory, 1-Day shield, and Rules)
    if (showShieldModal) {
        StreakShieldModalSheet(
            isShieldActive = isStreakShieldActive && (activeShieldExpiresAt == null || currentShieldRemainingMillis > 0L),
            activeShieldExpiresAt = activeShieldExpiresAt,
            remainingMillis = currentShieldRemainingMillis,
            availableManualShields = availableManualShields,
            available1DayShields = available1DayShields,
            onActivateManualShield = { viewModel.activateManualShield() },
            onActivate1DayShield = { viewModel.activate1DayShield() },
            onGrant1DayShield = { viewModel.grant1DayShield() },
            onToggleShieldActive = { active -> viewModel.setStreakShieldActive(active) },
            onDismiss = { showShieldModal = false }
        )
    }
}

@Composable
fun LiquidGlassTopBar(
    currentScreen: AppScreen,
    flameScore: Double,
    isStreakShieldActive: Boolean = false,
    onOpenShieldModal: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenFlame: () -> Unit,
    onOpenDeviations: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val title = when (currentScreen) {
        AppScreen.DASHBOARD -> stringResource(R.string.nav_dashboard)
        AppScreen.DATA_ENTRY -> stringResource(R.string.nav_entry)
        AppScreen.STRIKE -> stringResource(R.string.nav_strike)
        AppScreen.COMPARISON -> stringResource(R.string.nav_compare)
        AppScreen.FLAME -> stringResource(R.string.flame_system_title)
        AppScreen.DEVIATIONS -> stringResource(R.string.deviations_title)
        AppScreen.ROADMAP -> stringResource(R.string.roadmap_title)
    }

    val cyanColor = if (isDarkTheme) NeonCyan else com.example.ui.theme.DayCyan
    val iconBg = if (isDarkTheme) Color(0xFF0F1E36) else Color(0x2000B4D8)
    val iconBorder = if (isDarkTheme) NeonCyan else com.example.ui.theme.DayCyan.copy(alpha = 0.5f)
    val buttonBg = if (isDarkTheme) Color(0x221E293B) else Color(0xCCFFFFFF)
    val buttonBorder = if (isDarkTheme) Color(0x4038BDF8) else Color(0x5000B4D8)

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
                    text = title,
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

            // Adaptive Glowing Animated "Fire" Header Icon
            GlowingFlameHeaderIcon(
                flameScore = flameScore,
                onClick = onOpenFlame,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Dynamic Streak Shield Element next to the flame with plus (+) action button
            DynamicStreakShieldHeaderButton(
                isShieldActive = isStreakShieldActive,
                onClick = onOpenShieldModal,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(buttonBg)
                    .border(1.dp, buttonBorder, CircleShape)
                    .testTag("settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = cyanColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
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
                icon = Icons.Default.AddCircle,
                label = stringResource(R.string.nav_entry),
                isSelected = currentScreen == AppScreen.DATA_ENTRY,
                activeColor = NeonEmerald,
                testTag = "nav_tab_entry",
                onClick = { onScreenSelected(AppScreen.DATA_ENTRY) }
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
