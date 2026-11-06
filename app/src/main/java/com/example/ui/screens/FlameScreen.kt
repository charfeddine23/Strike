package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.flame.FlameLogEntry
import com.example.ui.components.FlameVisualStage
import com.example.ui.components.FlameVisualizer
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.getFlameColors
import com.example.ui.components.getFlameStage
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FlameLogFilter {
    ALL,
    STRIKES,
    CALLS,
    INACTIVITY,
    CONTROLS,
    SIMULATOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlameScreen(
    viewModel: IndividualViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled

    val flameScore by viewModel.flameScore.collectAsStateWithLifecycle()
    val flameLogs by viewModel.flameLogs.collectAsStateWithLifecycle()
    val isStreakShieldActive by viewModel.isStreakShieldActive.collectAsStateWithLifecycle()
    val peakScore by viewModel.peakFlameScore.collectAsStateWithLifecycle()
    val totalGained by viewModel.totalPointsGained.collectAsStateWithLifecycle()
    val totalExtinguished by viewModel.totalPointsExtinguished.collectAsStateWithLifecycle()

    val stage = getFlameStage(flameScore)
    val colors = getFlameColors(flameScore)

    // Haptic state transition detection
    var previousStage by remember { mutableStateOf(stage) }
    LaunchedEffect(stage) {
        if (stage != previousStage) {
            HapticHelper.triggerStageTransitionHaptic(context, isHapticEnabled)
            previousStage = stage
        }
    }

    // Modal state for Settings & Interactive Simulator (opened on 2-second hold)
    var showSettingsModal by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHoldingFlame by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf(FlameLogFilter.ALL) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val animatedScore by animateFloatAsState(
        targetValue = flameScore.toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "animated_score"
    )

    val stageLabel = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> stringResource(R.string.flame_stage_frozen)
        FlameVisualStage.ICE_CRACKING -> stringResource(R.string.flame_stage_cracking)
        FlameVisualStage.COLOR_GRADIENT -> stringResource(R.string.flame_stage_gradient)
        FlameVisualStage.FULLY_IGNITED -> stringResource(R.string.flame_stage_ignited)
        FlameVisualStage.SUPERNOVA_OVERLOAD -> stringResource(R.string.flame_stage_supernova)
    }

    val stageSub = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> stringResource(R.string.flame_stage_frozen_sub)
        FlameVisualStage.ICE_CRACKING -> stringResource(R.string.flame_stage_cracking_sub)
        FlameVisualStage.COLOR_GRADIENT -> stringResource(R.string.flame_stage_gradient_sub)
        FlameVisualStage.FULLY_IGNITED -> stringResource(R.string.flame_stage_ignited_sub)
        FlameVisualStage.SUPERNOVA_OVERLOAD -> stringResource(R.string.flame_stage_supernova_sub)
    }

    val stageBadgeColor = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> Color(0xFF38BDF8)
        FlameVisualStage.ICE_CRACKING -> Color(0xFF00E5FF)
        FlameVisualStage.COLOR_GRADIENT -> colors.second
        FlameVisualStage.FULLY_IGNITED -> Color(0xFFEF4444)
        FlameVisualStage.SUPERNOVA_OVERLOAD -> Color(0xFFC084FC) // Neon Violet
    }

    // Filtered flame logs
    val filteredLogs = remember(flameLogs, selectedFilter) {
        when (selectedFilter) {
            FlameLogFilter.ALL -> flameLogs
            FlameLogFilter.STRIKES -> flameLogs.filter {
                it.reason in listOf("STRIKE_DEDUCTION", "RETROACTIVE", "STRIKE_DELETED_REFUND", "STRIKE_RESTORED_UNDO")
            }
            FlameLogFilter.CALLS -> flameLogs.filter { it.reason == "CONTACT_CALL" }
            FlameLogFilter.INACTIVITY -> flameLogs.filter { it.reason in listOf("INACTIVITY", "STREAK_SHIELD") }
            FlameLogFilter.CONTROLS -> flameLogs.filter { it.reason in listOf("DEFLAME", "GIRL") }
            FlameLogFilter.SIMULATOR -> flameLogs.filter { it.reason == "SIMULATION" }
        }
    }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.flame_clear_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.flame_clear_confirm_msg),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearFlameLogs()
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    modifier = Modifier.testTag("flame_clear_confirm_btn")
                ) {
                    Text(
                        text = stringResource(R.string.flame_clear_action),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = stringResource(R.string.flame_cancel_action))
                }
            },
            containerColor = Color(0xFF0F172A),
            tonalElevation = 8.dp
        )
    }

    // Flame Settings & Simulator Modal BottomSheet (opened by 2-second hold on the flame)
    if (showSettingsModal) {
        FlameSettingsAndSimulatorSheet(
            viewModel = viewModel,
            currentScore = flameScore,
            stageBadgeColor = stageBadgeColor,
            isStreakShieldActive = isStreakShieldActive,
            isHapticEnabled = isHapticEnabled,
            onDismiss = { showSettingsModal = false },
            onClearLogs = { showClearConfirmDialog = true }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Navigation Header (Clean, minimalist)
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x221E293B))
                                .border(1.dp, Color(0x3338BDF8), CircleShape)
                                .testTag("flame_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.flame_system_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.flame_system_subtitle),
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    // Discreet settings shortcut icon as a convenience
                    IconButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            showSettingsModal = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x15FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .testTag("flame_settings_shortcut_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.flame_settings_title),
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Hero Flame Stage Card (Placed at the very top as requested!)
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flame_hero_card"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Stage Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(stageBadgeColor.copy(alpha = 0.18f))
                            .border(1.dp, stageBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (stage) {
                                    FlameVisualStage.FROZEN_STATIC -> Icons.Default.AcUnit
                                    FlameVisualStage.SUPERNOVA_OVERLOAD -> Icons.Default.Bolt
                                    else -> Icons.Default.LocalFireDepartment
                                },
                                contentDescription = null,
                                tint = stageBadgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stageLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = stageBadgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Flame Canvas with 2-second hold detector to open settings
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var completed = false
                                    val holdJob = coroutineScope.launch {
                                        isHoldingFlame = true
                                        val totalDurationMs = 2000L
                                        val interval = 25L
                                        var elapsed = 0L
                                        while (elapsed < totalDurationMs) {
                                            delay(interval)
                                            elapsed += interval
                                            holdProgress = (elapsed.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                                        }
                                        completed = true
                                        holdProgress = 1f
                                        HapticHelper.triggerStageTransitionHaptic(context, isHapticEnabled)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        showSettingsModal = true
                                        isHoldingFlame = false
                                        holdProgress = 0f
                                    }

                                    do {
                                        val event = awaitPointerEvent()
                                        val released = event.changes.any { !it.pressed }
                                        if (released) {
                                            holdJob.cancel()
                                            isHoldingFlame = false
                                            holdProgress = 0f
                                            break
                                        }
                                    } while (event.changes.any { it.pressed })

                                    if (!completed) {
                                        holdJob.cancel()
                                        isHoldingFlame = false
                                        holdProgress = 0f
                                    }
                                }
                            }
                            .testTag("flame_hero_canvas"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dynamic circular charging ring when holding for 2 seconds
                        if (isHoldingFlame && holdProgress > 0f) {
                            CircularProgressIndicator(
                                progress = { holdProgress },
                                modifier = Modifier.size(244.dp),
                                color = NeonCyan,
                                trackColor = Color(0x2238BDF8),
                                strokeWidth = 3.5.dp
                            )
                        }

                        FlameVisualizer(
                            score = flameScore,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Score Display
                    Text(
                        text = String.format(Locale.US, "%.1f", animatedScore),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )

                    Text(
                        text = stringResource(R.string.flame_total_points),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stageSub,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = stageBadgeColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Segmented Range Tier Bar (0-30, 31-60, 61-300, 301-600, >600)
                    FlameTierBar(
                        currentScore = flameScore,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Long press hint under the flame
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isHoldingFlame) NeonCyan.copy(alpha = 0.15f) else Color(0x10FFFFFF))
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                showSettingsModal = true
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isHoldingFlame) NeonCyan else Color(0x99FFFFFF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHoldingFlame) {
                                "${(holdProgress * 100).toInt()}% • " + stringResource(R.string.flame_long_press_hint)
                            } else {
                                stringResource(R.string.flame_long_press_hint)
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isHoldingFlame) FontWeight.Bold else FontWeight.Medium,
                            color = if (isHoldingFlame) NeonCyan else Color(0x99FFFFFF)
                        )
                    }
                }
            }
        }

        // 3. Manual Instant Control Buttons ("Deflame" & "Girl" - placed directly below the Flame!)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.manual_controls_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // "Deflame" Button (-2 points)
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                viewModel.deflame()
                            }
                            .testTag("btn_deflame"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x330284C7))
                                    .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = "Deflame",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.btn_deflame_title),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x2238BDF8))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "-2 PTS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    // "Girl" Button (-3 points)
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                viewModel.girl()
                            }
                            .testTag("btn_girl"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33E11D48))
                                    .border(1.5.dp, Color(0xFFFB7185), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Girl",
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.btn_girl_title),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x22FB7185))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "-3 PTS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFB7185)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Flame Analytics Summary Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flame_analytics_summary_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.flame_analytics_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Dynamic state badge
                        val statusText = when {
                            flameScore > 600.0 -> stringResource(R.string.flame_status_overload)
                            flameScore > 300.0 -> stringResource(R.string.flame_status_critical)
                            flameScore > 60.0 -> stringResource(R.string.flame_status_heating)
                            else -> stringResource(R.string.flame_status_stable)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(stageBadgeColor.copy(alpha = 0.18f))
                                .border(1.dp, stageBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = stageBadgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Stats Grid (Peak Flame, Total Accrued, Total Extinguished)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Peak Flame Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x181E293B))
                                .border(1.dp, Color(0x25F59E0B), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = NeonAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.flame_analytics_peak),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", peakScore),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = NeonAmber
                                )
                            }
                        }

                        // Total Accrued Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x181E293B))
                                .border(1.dp, Color(0x25EF4444), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = NeonRose,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.flame_analytics_gained),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", totalGained)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = NeonRose
                                )
                            }
                        }

                        // Total Extinguished Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x181E293B))
                                .border(1.dp, Color(0x2538BDF8), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.flame_analytics_extinguished),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "-${String.format(Locale.US, "%.1f", totalExtinguished)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Extinguish Efficiency Bar
                    val totalActivity = (totalGained + totalExtinguished).coerceAtLeast(1.0)
                    val extRatio = (totalExtinguished / totalActivity).toFloat().coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.flame_analytics_efficiency),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(extRatio * 100).toInt()}% Control Rate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x20FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(extRatio.coerceAtLeast(0.02f))
                                .height(6.dp)
                                .background(Color(0xFF38BDF8))
                        )
                        Box(
                            modifier = Modifier
                                .weight((1f - extRatio).coerceAtLeast(0.02f))
                                .height(6.dp)
                                .background(NeonRose)
                        )
                    }
                }
            }
        }

        // 5. "Streak Shield" Card (Quick status)
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("streak_shield_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isStreakShieldActive) NeonEmerald.copy(alpha = 0.22f)
                                    else Color(0x2064748B)
                                )
                                .border(
                                    1.5.dp,
                                    if (isStreakShieldActive) NeonEmerald else Color(0x4094A3B8),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isStreakShieldActive) NeonEmerald else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.streak_shield_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = PlusJakartaSans,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isStreakShieldActive) NeonEmerald.copy(alpha = 0.2f)
                                            else Color(0x15FFFFFF)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isStreakShieldActive) {
                                            stringResource(R.string.streak_shield_active_badge)
                                        } else {
                                            stringResource(R.string.streak_shield_inactive_badge)
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isStreakShieldActive) NeonEmerald else Color.Gray
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.streak_shield_desc),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Switch(
                        checked = isStreakShieldActive,
                        onCheckedChange = { active ->
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            viewModel.setStreakShieldActive(active)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonEmerald,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0x221E293B)
                        ),
                        modifier = Modifier.testTag("streak_shield_switch")
                    )
                }
            }
        }

        // 6. Calculation & Event Logic Rules Summary
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.flame_rules_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val rules = listOf(
                        Triple(
                            Icons.Default.Shield,
                            stringResource(R.string.rule_shield_title),
                            stringResource(R.string.rule_shield_desc)
                        ),
                        Triple(
                            Icons.Default.Schedule,
                            stringResource(R.string.rule_inactivity_title),
                            stringResource(R.string.rule_inactivity_desc)
                        ),
                        Triple(
                            Icons.Default.Call,
                            stringResource(R.string.rule_call_title),
                            stringResource(R.string.rule_call_desc)
                        ),
                        Triple(
                            Icons.Default.AutoAwesome,
                            stringResource(R.string.rule_retroactive_title),
                            stringResource(R.string.rule_retroactive_desc)
                        ),
                        Triple(
                            Icons.Default.LocalFireDepartment,
                            stringResource(R.string.rule_strike_title),
                            stringResource(R.string.rule_strike_desc)
                        )
                    )

                    rules.forEach { (icon, title, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2038BDF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Dedicated Flame Activity Log & Audit History Section
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flame_audit_log_section"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = NeonAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.flame_activity_log_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${filteredLogs.size} of ${flameLogs.size} events",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (flameLogs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    showClearConfirmDialog = true
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x15FFFFFF))
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                    .testTag("flame_clear_logs_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.flame_clear_history),
                                    tint = NeonRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FlameLogFilter.values().forEach { filter ->
                            val isSelected = selectedFilter == filter
                            val label = when (filter) {
                                FlameLogFilter.ALL -> stringResource(R.string.flame_filter_all)
                                FlameLogFilter.STRIKES -> stringResource(R.string.flame_filter_strikes)
                                FlameLogFilter.CALLS -> stringResource(R.string.flame_filter_calls)
                                FlameLogFilter.INACTIVITY -> stringResource(R.string.flame_filter_inactivity)
                                FlameLogFilter.CONTROLS -> stringResource(R.string.flame_filter_controls)
                                FlameLogFilter.SIMULATOR -> stringResource(R.string.flame_filter_simulator)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) NeonCyan.copy(alpha = 0.25f)
                                        else Color(0x15FFFFFF)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else Color(0x25FFFFFF),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        selectedFilter = filter
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x10FFFFFF))
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.flame_activity_empty),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredLogs.forEach { entry ->
                                FlameAuditLogItemCard(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Flame System Settings & Interactive Simulator Modal Bottom Sheet.
 * Opened whenever the user long-presses the Flame for 2 seconds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlameSettingsAndSimulatorSheet(
    viewModel: IndividualViewModel,
    currentScore: Double,
    stageBadgeColor: Color,
    isStreakShieldActive: Boolean,
    isHapticEnabled: Boolean,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customScoreInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B1324),
        scrimColor = Color.Black.copy(alpha = 0.70f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x55FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("flame_settings_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.flame_settings_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.flame_settings_subtitle),
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x15FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: Interactive Score Simulator (Moved here as requested!)
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.flame_simulator_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeonCyan
                            )
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.1f", currentScore)} pts",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = stageBadgeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct Numeric Input up to 10,000
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customScoreInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                    customScoreInput = input
                                    inputError = null
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.flame_simulator_input_label),
                                    fontSize = 11.sp
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "0 - 10000",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val num = customScoreInput.toDoubleOrNull()
                                    if (num != null && num in 0.0..10000.0) {
                                        viewModel.setFlameScore(num)
                                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        focusManager.clearFocus()
                                        inputError = null
                                    } else {
                                        inputError = "0 - 10000"
                                    }
                                }
                            ),
                            isError = inputError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x4038BDF8),
                                focusedLabelColor = NeonCyan,
                                cursorColor = NeonCyan
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sheet_flame_score_input")
                        )

                        Button(
                            onClick = {
                                val num = customScoreInput.toDoubleOrNull()
                                if (num != null && num in 0.0..10000.0) {
                                    viewModel.setFlameScore(num)
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    focusManager.clearFocus()
                                    inputError = null
                                } else {
                                    inputError = "0 - 10000"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("sheet_apply_score_btn")
                        ) {
                            Text(
                                text = stringResource(R.string.flame_simulator_apply),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    if (inputError != null) {
                        Text(
                            text = stringResource(R.string.flame_simulator_range_error),
                            fontSize = 11.sp,
                            color = NeonRose,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Continuous Slider up to 10,000 pts
                    Slider(
                        value = currentScore.toFloat().coerceIn(0f, 10000f),
                        onValueChange = {
                            viewModel.setFlameScore(it.toDouble())
                            customScoreInput = String.format(Locale.US, "%.0f", it)
                        },
                        valueRange = 0f..10000f,
                        colors = SliderDefaults.colors(
                            thumbColor = stageBadgeColor,
                            activeTrackColor = stageBadgeColor,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.testTag("sheet_flame_score_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Presets
                    val presets = listOf(
                        Pair("0", 0.0),
                        Pair("45", 45.0),
                        Pair("180", 180.0),
                        Pair("350", 350.0),
                        Pair("750 (Supernova)", 750.0),
                        Pair("2,000", 2000.0),
                        Pair("10,000", 10000.0)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, value) ->
                            val isSelected = currentScore == value
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x221E293B))
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else Color(0x4038BDF8),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.setFlameScore(value)
                                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        customScoreInput = value.toInt().toString()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NeonCyan else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: Flame System Config (Streak Shield, Haptics, Logs)
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "System Preferences",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeonCyan,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Streak Shield Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isStreakShieldActive) NeonEmerald else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.streak_shield_title),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isStreakShieldActive) "Inactivity paused (+3/day off)" else "Normal accrual active",
                                    fontSize = 10.sp,
                                    color = if (isStreakShieldActive) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isStreakShieldActive,
                            onCheckedChange = { active ->
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                viewModel.setStreakShieldActive(active)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonEmerald
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Haptics Feedback Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tactile Haptic Feedback",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vibrations on buttons and state transitions",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isHapticEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setHapticFeedbackEnabled(enabled)
                                HapticHelper.triggerActionHaptic(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clear Activity Log Action Button
                    Button(
                        onClick = onClearLogs,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x25EF4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = NeonRose,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.flame_clear_history),
                            color = NeonRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * 5-Segment Tier Bar supporting Supernova (>600)
 */
@Composable
private fun FlameTierBar(
    currentScore: Double,
    modifier: Modifier = Modifier
) {
    val tiers = listOf(
        Pair("0 - 30", Color(0xFF38BDF8)),
        Pair("31 - 60", Color(0xFF00E5FF)),
        Pair("61 - 300", Color(0xFFE11D48)),
        Pair("301 - 600", Color(0xFFF97316)),
        Pair("> 600", Color(0xFFC084FC))
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tiers.forEachIndexed { idx, (_, tierColor) ->
                val isCurrent = when (idx) {
                    0 -> currentScore <= 30.0
                    1 -> currentScore in 30.001..60.0
                    2 -> currentScore in 60.001..300.0
                    3 -> currentScore in 300.001..600.0
                    else -> currentScore > 600.0
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isCurrent) tierColor else tierColor.copy(alpha = 0.25f))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tiers.forEachIndexed { idx, (label, tierColor) ->
                val isCurrent = when (idx) {
                    0 -> currentScore <= 30.0
                    1 -> currentScore in 30.001..60.0
                    2 -> currentScore in 60.001..300.0
                    3 -> currentScore in 300.001..600.0
                    else -> currentScore > 600.0
                }
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                    color = if (isCurrent) tierColor else Color(0x80FFFFFF)
                )
            }
        }
    }
}

/**
 * Liquid Glass audit card for each recorded Flame event
 */
@Composable
private fun FlameAuditLogItemCard(entry: FlameLogEntry) {
    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd • HH:mm:ss", Locale.getDefault()) }
    val timeStr = remember(entry.timestamp) { timeFormat.format(Date(entry.timestamp)) }

    val icon: ImageVector
    val iconColor: Color
    val iconBgColor: Color

    when (entry.reason) {
        "INACTIVITY" -> {
            icon = Icons.Default.Schedule
            iconColor = NeonCyan
            iconBgColor = NeonCyan.copy(alpha = 0.18f)
        }
        "STREAK_SHIELD" -> {
            icon = Icons.Default.Shield
            iconColor = NeonEmerald
            iconBgColor = NeonEmerald.copy(alpha = 0.18f)
        }
        "CONTACT_CALL" -> {
            icon = Icons.Default.Call
            iconColor = NeonEmerald
            iconBgColor = NeonEmerald.copy(alpha = 0.18f)
        }
        "STRIKE_DEDUCTION" -> {
            icon = Icons.Default.LocalFireDepartment
            iconColor = NeonRose
            iconBgColor = NeonRose.copy(alpha = 0.18f)
        }
        "RETROACTIVE" -> {
            icon = Icons.Default.Shield
            iconColor = Color(0xFFA855F7)
            iconBgColor = Color(0x28A855F7)
        }
        "DEFLAME" -> {
            icon = Icons.Default.AcUnit
            iconColor = Color(0xFF38BDF8)
            iconBgColor = Color(0x2838BDF8)
        }
        "GIRL" -> {
            icon = Icons.Default.Favorite
            iconColor = Color(0xFFFB7185)
            iconBgColor = Color(0x28FB7185)
        }
        "STRIKE_DELETED_REFUND", "STRIKE_RESTORED_UNDO" -> {
            icon = Icons.Default.Undo
            iconColor = NeonAmber
            iconBgColor = NeonAmber.copy(alpha = 0.18f)
        }
        else -> {
            icon = Icons.Default.Tune
            iconColor = NeonCyan
            iconBgColor = NeonCyan.copy(alpha = 0.18f)
        }
    }

    val isPositive = entry.change > 0
    val isZero = entry.change == 0.0
    val deltaColor = if (isPositive) NeonAmber else if (entry.change < 0) Color(0xFF38BDF8) else Color(0xFFA855F7)
    val deltaSign = if (isPositive) "+" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x181E293B))
            .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(iconBgColor)
                            .border(1.dp, iconColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = entry.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Variation Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(deltaColor.copy(alpha = 0.16f))
                        .border(1.dp, deltaColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isZero) "0.0 PROTECTED" else "$deltaSign${String.format(Locale.US, "%.1f", entry.change)} PTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = deltaColor
                    )
                }
            }

            // Secondary Row: Details + Resulting Score Pill
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (entry.details.isNotBlank()) {
                    Text(
                        text = entry.details,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Resulting Total Score badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x221E293B))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Total: ${String.format(Locale.US, "%.1f", entry.resultingScore)} pts",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}
