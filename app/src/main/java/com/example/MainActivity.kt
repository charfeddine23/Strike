package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FullscreenPatternUnlockScreen
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataEntryScreen
import com.example.ui.screens.StrikeScreen
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StrikePulseTheme
import com.example.ui.viewmodel.IndividualViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

enum class AppScreen {
    DASHBOARD,
    DATA_ENTRY,
    STRIKE,
    COMPARISON
}

class MainActivity : ComponentActivity() {
    private val viewModel: IndividualViewModel by viewModels {
        IndividualViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val layoutDirection = if (currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            val currentContext = LocalContext.current
            val localizedContext = remember(currentLang) {
                val config = Configuration(currentContext.resources.configuration)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                currentContext.createConfigurationContext(config)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
            LiquidGlassTopBar(
                currentScreen = currentScreen,
                currentLang = currentLang,
                onOpenLanguageDialog = { showLanguageDialog = true },
                onOpenSettings = { showSettingsDialog = true }
            )
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

    // Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val languages = listOf(
                        Triple("en", "English", "EN"),
                        Triple("ar", "العربية (Arabic - RTL)", "AR"),
                        Triple("fr", "Français (French)", "FR")
                    )

                    languages.forEach { (code, title, tag) ->
                        val isSelected = currentLang == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x101E293B))
                                .border(1.dp, if (isSelected) NeonCyan else Color(0x3038BDF8), RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun LiquidGlassTopBar(
    currentScreen: AppScreen,
    currentLang: String,
    onOpenLanguageDialog: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val title = when (currentScreen) {
        AppScreen.DASHBOARD -> stringResource(R.string.nav_dashboard)
        AppScreen.DATA_ENTRY -> stringResource(R.string.nav_entry)
        AppScreen.STRIKE -> stringResource(R.string.nav_strike)
        AppScreen.COMPARISON -> stringResource(R.string.nav_compare)
    }

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
                    .background(Color(0xFF0F1E36))
                    .border(1.dp, NeonCyan, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontSize = 11.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Language selector button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x221E293B))
                    .border(1.dp, Color(0x4038BDF8), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenLanguageDialog)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("language_switch_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentLang.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Settings button (Replaced Dark/Light Theme button)
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x221E293B))
                    .border(1.dp, Color(0x3538BDF8), CircleShape)
                    .testTag("settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = NeonCyan,
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
                .background(Color(0xCC0F172A))
                .border(1.dp, Color(0x4038BDF8), RoundedCornerShape(22.dp))
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
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = activeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
