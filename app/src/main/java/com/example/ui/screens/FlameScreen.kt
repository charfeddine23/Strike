package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.DeflameAdjustmentDialog
import com.example.ui.components.FLAME_MAX_SCALE_SCORE
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
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

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FlameScreen(
    viewModel: IndividualViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

    val flameScore by viewModel.flameScore.collectAsStateWithLifecycle()
    val flameLogs by viewModel.flameLogs.collectAsStateWithLifecycle()
    val isStreakShieldActive by viewModel.isStreakShieldActive.collectAsStateWithLifecycle()
    val peakScore by viewModel.peakFlameScore.collectAsStateWithLifecycle()
    val totalGained by viewModel.totalPointsGained.collectAsStateWithLifecycle()
    val totalExtinguished by viewModel.totalPointsExtinguished.collectAsStateWithLifecycle()
    val girlDeductionPoints by viewModel.girlDeductionPoints.collectAsStateWithLifecycle()
    val pulseElixirUsesLeft by viewModel.pulseElixirUsesLeft.collectAsStateWithLifecycle()

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
    var showStartDateDialog by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHoldingFlame by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf(FlameLogFilter.ALL) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showGirlAdjustmentDialog by remember { mutableStateOf(false) }

    val animatedScore by animateFloatAsState(
        targetValue = flameScore.toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "animated_score"
    )

    val stageSub = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> stringResource(R.string.flame_stage_frozen_sub)
        FlameVisualStage.ICE_CRACKING -> stringResource(R.string.flame_stage_cracking_sub)
        FlameVisualStage.COLOR_GRADIENT -> stringResource(R.string.flame_stage_gradient_sub)
        FlameVisualStage.FULLY_IGNITED -> stringResource(R.string.flame_stage_ignited_sub)
        FlameVisualStage.SUPERNOVA_OVERLOAD -> stringResource(R.string.flame_stage_supernova_sub)
        FlameVisualStage.OBSIDIAN_PLASMA -> stringResource(R.string.flame_stage_obsidian_sub)
    }

    val stageBadgeColor = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> Color(0xFF38BDF8)
        FlameVisualStage.ICE_CRACKING -> Color(0xFF00E5FF)
        FlameVisualStage.COLOR_GRADIENT -> colors.second
        FlameVisualStage.FULLY_IGNITED -> Color(0xFFEF4444)
        FlameVisualStage.SUPERNOVA_OVERLOAD -> Color(0xFFC084FC)
        FlameVisualStage.OBSIDIAN_PLASMA -> Color(0xFFE879F9)
    }

    // Filtered flame logs
    val filteredLogs = remember(flameLogs, selectedFilter) {
        when (selectedFilter) {
            FlameLogFilter.ALL -> flameLogs
            FlameLogFilter.STRIKES -> flameLogs.filter {
                it.reason in listOf(
                    "STRIKE_DEDUCTION",
                    "RETROACTIVE",
                    "STRIKE_DELETED_REFUND",
                    "STRIKE_RESTORED_UNDO",
                    "DOUBLE_STRIKE",
                    "MULTI_STRIKE_COMBO",
                    "EXTREME_OVERLOAD"
                )
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
            currentLang = currentLang,
            onDismiss = { showSettingsModal = false },
            onClearLogs = { showClearConfirmDialog = true }
        )
    }

    // Streak Start Date Dialog (Launched by 2-second hold on the flame canvas)
    if (showStartDateDialog) {
        com.example.ui.components.StreakStartDateDialog(
            viewModel = viewModel,
            onDismiss = { showStartDateDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    // Girl Points Adjustment Dialog (Opened on long press of the Girl button; saves points for subsequent taps)
    if (showGirlAdjustmentDialog) {
        DeflameAdjustmentDialog(
            currentFlameScore = flameScore,
            onDismiss = { showGirlAdjustmentDialog = false },
            onApply = { penaltyPoints ->
                viewModel.setGirlDeductionPoints(penaltyPoints)
            },
            isHapticEnabled = isHapticEnabled,
            isGirlMode = true,
            currentLang = currentLang,
            initialDeduction = girlDeductionPoints
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("flame_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Flame Stage Card (Placed at the very top of the Flame Screen!)
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
                                        showStartDateDialog = true
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
                        // Smooth dynamic energy aura & scaling effect during long press gesture (replaces circular ring)
                        if (isHoldingFlame && holdProgress > 0f) {
                            val auraPulse = (sin(holdProgress * 18f) + 1f) / 2f
                            val auraScale = 1f + holdProgress * 0.18f + auraPulse * 0.04f
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .scale(auraScale)
                                    .drawBehind {
                                        val radius = (size.minDimension / 2f) * (0.82f + holdProgress * 0.38f)
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    NeonCyan.copy(alpha = 0.50f * holdProgress),
                                                    Color(0xFF8B5CF6).copy(alpha = 0.40f * holdProgress),
                                                    Color(0xFF38BDF8).copy(alpha = 0.20f * holdProgress),
                                                    Color.Transparent
                                                ),
                                                center = center,
                                                radius = radius
                                            ),
                                            radius = radius
                                        )
                                    }
                            )
                        }

                        FlameVisualizer(
                            score = animatedScore.toDouble(),
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(if (isHoldingFlame) 1f + holdProgress * 0.12f else 1f)
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

                    // Dynamic SeekBar Scale (0 Ice -> 30,000 Obsidian Black Plasma Max Power)
                    FlameLevelSeekBar(
                        currentScore = flameScore,
                        animatedScore = animatedScore.toDouble(),
                        stageBadgeColor = stageBadgeColor,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Manual Instant Control Buttons ("Deflame" & "Girl" - placed directly below the Flame!)
        item {
            val pulseBonus = if (pulseElixirUsesLeft > 0) 5.0 else 0.0
            val effectiveDeflamePoints = 3.0 + pulseBonus
            val effectiveGirlPoints = girlDeductionPoints.coerceAtLeast(0.1)

            fun formatControlPts(value: Double): String {
                return if (value % 1.0 == 0.0 && value <= Long.MAX_VALUE.toDouble()) {
                    "${value.toLong()}"
                } else {
                    String.format(Locale.US, "%.1f", value)
                }
            }

            val deflameFormatted = formatControlPts(effectiveDeflamePoints)
            val girlFormatted = formatControlPts(effectiveGirlPoints)

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
                    // "Deflame" Button (Displays true effective points including active Pulse Elixir +5 bonus)
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                viewModel.deflame(3.0)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (pulseBonus > 0.0) Color(0x3300E5FF) else Color(0x2238BDF8)
                                        )
                                        .border(
                                            width = if (pulseBonus > 0.0) 1.dp else 0.dp,
                                            color = if (pulseBonus > 0.0) NeonCyan.copy(alpha = 0.65f) else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "-$deflameFormatted نقطة" else "-$deflameFormatted PTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (pulseBonus > 0.0) NeonCyan else Color(0xFF38BDF8),
                                        modifier = Modifier.testTag("deflame_points_badge")
                                    )
                                }
                                if (pulseBonus > 0.0) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = if (isAr) "معزز بإكسير النبض" else "Pulse Elixir Boosted",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // "Girl" Button (Normal tap: deducts configured points; Long press: configure points for next taps)
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .combinedClickable(
                                onClick = {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    viewModel.girl()
                                },
                                onLongClick = {
                                    HapticHelper.triggerStageTransitionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    showGirlAdjustmentDialog = true
                                }
                            )
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x22FB7185))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "-$girlFormatted نقطة" else "-$girlFormatted PTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFB7185),
                                        modifier = Modifier.testTag("girl_points_badge")
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = if (isAr) "تعديل النقاط (ضغط مطوّل)" else "Adjust points (Hold)",
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(12.dp)
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

                        // Dynamic state badge across 0..30,000 scale
                        val statusText = when {
                            flameScore > 24_000.0 -> stringResource(R.string.flame_status_obsidian)
                            flameScore > 12_000.0 -> stringResource(R.string.flame_status_overload)
                            flameScore > 3_000.0 -> stringResource(R.string.flame_status_critical)
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
                            text = if (isAr) "معدل السيطرة ${(extRatio * 100).toInt()}%" else "${(extRatio * 100).toInt()}% Control Rate",
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

        // 5. Calculation & Event Logic Rules Summary
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
                        ),
                        Triple(
                            Icons.Default.Bolt,
                            stringResource(R.string.rule_double_strike_title),
                            stringResource(R.string.rule_double_strike_desc)
                        ),
                        Triple(
                            Icons.Default.LocalFireDepartment,
                            stringResource(R.string.rule_multi_strike_title),
                            stringResource(R.string.rule_multi_strike_desc)
                        ),
                        Triple(
                            Icons.Default.Warning,
                            stringResource(R.string.rule_extreme_overload_title),
                            stringResource(R.string.rule_extreme_overload_desc)
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
                                    text = if (isAr) "${filteredLogs.size} من ${flameLogs.size} حدث" else "${filteredLogs.size} of ${flameLogs.size} events",
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
                                FlameAuditLogItemCard(entry, currentLang = currentLang)
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
    currentLang: String = "ar",
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit
) {
    val isAr = currentLang == "ar"
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
                            text = if (isAr) {
                                "${String.format(Locale.US, "%.1f", currentScore)} نقطة"
                            } else {
                                "${String.format(Locale.US, "%.1f", currentScore)} pts"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = stageBadgeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct Numeric Input (Unlimited up to Infinity)
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
                                    text = "0 - ∞",
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
                                    if (num != null && num.isFinite() && num >= 0.0) {
                                        viewModel.setFlameScore(num)
                                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        focusManager.clearFocus()
                                        inputError = null
                                    } else {
                                        inputError = ">= 0"
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
                                if (num != null && num.isFinite() && num >= 0.0) {
                                    viewModel.setFlameScore(num)
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    focusManager.clearFocus()
                                    inputError = null
                                } else {
                                    inputError = ">= 0"
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
                            text = if (isAr) "أدخل قيمة موجبة (0 فأكثر)" else "Enter any non-negative value (>= 0)",
                            fontSize = 11.sp,
                            color = NeonRose,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Adaptive Continuous Slider (0..30,000 primary scale, expands dynamically beyond)
                    val sliderMax = remember(currentScore) {
                        val safe = currentScore.coerceIn(0.0, 1_000_000_000.0)
                        if (safe <= FLAME_MAX_SCALE_SCORE) FLAME_MAX_SCALE_SCORE.toFloat()
                        else (((safe / 10000.0).toLong() + 1L) * 10000f).coerceAtMost(1_000_000_000f)
                    }
                    Slider(
                        value = currentScore.toFloat().coerceIn(0f, sliderMax),
                        onValueChange = {
                            viewModel.setFlameScore(it.toDouble())
                            customScoreInput = String.format(Locale.US, "%.0f", it)
                        },
                        valueRange = 0f..sliderMax,
                        colors = SliderDefaults.colors(
                            thumbColor = stageBadgeColor,
                            activeTrackColor = stageBadgeColor,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.testTag("sheet_flame_score_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Presets spanning Ice 0 through Obsidian Plasma Max Power 30,000+
                    val presets = listOf(
                        Pair("0", 0.0),
                        Pair("45", 45.0),
                        Pair("1,500", 1500.0),
                        Pair("7,500", 7500.0),
                        Pair("18,000", 18000.0),
                        Pair("27,500", 27500.0),
                        Pair(if (isAr) "30,000 (الطاقة القصوى)" else "30,000 (Max Plasma)", 30000.0),
                        Pair("50,000", 50000.0),
                        Pair("100,000", 100000.0)
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
                        text = if (isAr) "تفضيلات النظام" else "System Preferences",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeonCyan,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )



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
                                    text = if (isAr) "الاهتزاز اللمسي التفاعلي" else "Tactile Haptic Feedback",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAr) "اهتزازات خفيفة عند الضغط على الأزرار وتبدّل المراحل" else "Vibrations on buttons and state transitions",
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
 * Luxury Continuous Endless-Loop Flame Level SeekBar (0 Ice -> 30,000 Obsidian Black Plasma Max Power).
 * Smoothly fills as Flame rises and empties as Flame lowers, with Ice crystal stasis at 0
 * and Obsidian Plasma cosmic lightning energy waves at 30,000.
 */
@Composable
private fun FlameLevelSeekBar(
    currentScore: Double,
    animatedScore: Double,
    stageBadgeColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val stage = getFlameStage(currentScore)
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    // Single seamless endless-loop driver: integer harmonics of [0..2π] wrap with zero jump.
    val infiniteTransition = rememberInfiniteTransition(label = "flame_seekbar_endless_loop")
    val loopProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "seekbar_loop_progress"
    )

    val progress = (animatedScore / FLAME_MAX_SCALE_SCORE).toFloat().coerceIn(0f, 1f)
    val clampedDisplayScore = currentScore.coerceAtLeast(0.0)
    val pct = ((clampedDisplayScore / FLAME_MAX_SCALE_SCORE) * 100.0).coerceIn(0.0, 100.0).roundToInt()

    val stageLabelRes = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> R.string.flame_stage_frozen
        FlameVisualStage.ICE_CRACKING -> R.string.flame_stage_cracking
        FlameVisualStage.COLOR_GRADIENT -> R.string.flame_stage_gradient
        FlameVisualStage.FULLY_IGNITED -> R.string.flame_stage_ignited
        FlameVisualStage.SUPERNOVA_OVERLOAD -> R.string.flame_stage_supernova
        FlameVisualStage.OBSIDIAN_PLASMA -> R.string.flame_stage_obsidian
    }

    val formattedScore = remember(clampedDisplayScore) {
        if (clampedDisplayScore % 1.0 == 0.0) {
            String.format(Locale.US, "%,.0f", clampedDisplayScore)
        } else {
            String.format(Locale.US, "%,.1f", clampedDisplayScore)
        }
    }

    val wavePath = remember { Path() }

    Column(modifier = modifier) {
        // Header Row: Stage Badge + Numeric 0..30,000 Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(stageBadgeColor.copy(alpha = 0.16f))
                    .border(1.dp, stageBadgeColor.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(stageBadgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(stageLabelRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        color = stageBadgeColor,
                        modifier = Modifier.testTag("flame_seekbar_stage_badge")
                    )
                }
            }

            Text(
                text = "$formattedScore / 30,000 ($pct%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("flame_seekbar_progress_text")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SeekBar Track + Endless-Loop Energy Waves + Plasma/Ice Thumb Orb
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .testTag("flame_level_seekbar")
        ) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val thumbRadius = 11.dp.toPx()
            val trackHeight = 14.dp.toPx()
            val trackRadius = trackHeight / 2f
            val trackLeft = thumbRadius
            val trackRight = w - thumbRadius
            val trackWidth = (trackRight - trackLeft).coerceAtLeast(1f)
            val trackTop = centerY - trackHeight / 2f

            val startX = if (isRtl) trackRight else trackLeft
            val endX = if (isRtl) trackLeft else trackRight
            val thumbX = if (isRtl) trackRight - progress * trackWidth else trackLeft + progress * trackWidth

            val loopRad = loopProgress * 2f * PI.toFloat()

            // 1. Recessed Obsidian Glass Track
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF060913), Color(0xFF10182C))
                    } else {
                        listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC))
                    }
                ),
                topLeft = Offset(trackLeft, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackRadius, trackRadius)
            )

            // Subtle frost glint sweeping gently along empty track in endless loop
            val frostSweepCenter = if (isRtl) {
                trackRight - ((loopProgress + 0.5f) % 1f) * trackWidth
            } else {
                trackLeft + ((loopProgress + 0.5f) % 1f) * trackWidth
            }
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF7DD3FC).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    startX = frostSweepCenter - trackWidth * 0.22f,
                    endX = frostSweepCenter + trackWidth * 0.22f
                ),
                topLeft = Offset(trackLeft, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackRadius, trackRadius)
            )

            // Quarter tick notches (25%, 50%, 75%)
            for (q in 1..3) {
                val frac = q * 0.25f
                val tx = if (isRtl) trackRight - frac * trackWidth else trackLeft + frac * trackWidth
                drawLine(
                    color = Color.White.copy(alpha = if (progress >= frac) 0.38f else 0.16f),
                    start = Offset(tx, trackTop + 2.5.dp.toPx()),
                    end = Offset(tx, trackTop + trackHeight - 2.5.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 2. Active Filled Spectrum Segment (Ice 0 -> Cyan -> Orange -> Violet -> Obsidian Black Plasma 30,000)
            val fillSpan = kotlin.math.abs(thumbX - startX)
            if (fillSpan > 1f) {
                val clipLeft = kotlin.math.min(startX, thumbX)
                val clipRight = kotlin.math.max(startX, thumbX)

                // Soft underglow beneath active portion of the SeekBar
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.30f),
                            Color(0xFFF97316).copy(alpha = 0.35f),
                            stageBadgeColor.copy(alpha = 0.45f)
                        ),
                        startX = startX,
                        endX = thumbX
                    ),
                    topLeft = Offset(clipLeft, trackTop - 3.dp.toPx()),
                    size = Size(fillSpan, trackHeight + 6.dp.toPx()),
                    cornerRadius = CornerRadius(trackRadius + 3.dp.toPx(), trackRadius + 3.dp.toPx())
                )

                clipRect(
                    left = clipLeft,
                    top = trackTop - 2.dp.toPx(),
                    right = clipRight,
                    bottom = trackTop + trackHeight + 2.dp.toPx()
                ) {
                    // Full 0..30k gradient mapped to track extremes so color intensifies continuously as fill grows
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            0.00f to Color(0xFFBAE6FD), // 0: Glacial Ice
                            0.12f to Color(0xFF00E5FF), // Arctic Cyan
                            0.28f to Color(0xFFA855F7), // Plasma Violet transition
                            0.50f to Color(0xFFF97316), // Blazing Furnace Orange
                            0.72f to Color(0xFFE11D48), // Crimson Core
                            0.86f to Color(0xFF9333EA), // Ultraviolet Supernova
                            0.94f to Color(0xFF2E1065), // Abyssal Violet
                            1.00f to Color(0xFF090514), // 30,000: Obsidian Black Plasma Core
                            startX = startX,
                            endX = endX
                        ),
                        topLeft = Offset(trackLeft, trackTop),
                        size = Size(trackWidth, trackHeight),
                        cornerRadius = CornerRadius(trackRadius, trackRadius)
                    )

                    // Seamless endless-loop sinusoidal plasma current flowing along the filled SeekBar
                    wavePath.reset()
                    val waveSteps = 32
                    for (i in 0..waveSteps) {
                        val t = i.toFloat() / waveSteps
                        val wx = trackLeft + t * trackWidth
                        val directionalPhase = if (isRtl) t else (1f - t)
                        val dy = sin(directionalPhase * 6f * PI.toFloat() + loopRad * 2f) * (trackHeight * 0.24f)
                        val wy = centerY + dy
                        if (i == 0) wavePath.moveTo(wx, wy) else wavePath.lineTo(wx, wy)
                    }
                    drawPath(
                        path = wavePath,
                        color = if (progress > 0.78f) Color(0xFFE879F9).copy(alpha = 0.75f)
                        else Color.White.copy(alpha = 0.55f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Second counter-harmonic cyan ribbon for Obsidian Plasma richness
                    if (progress > 0.35f) {
                        wavePath.reset()
                        for (i in 0..waveSteps) {
                            val t = i.toFloat() / waveSteps
                            val wx = trackLeft + t * trackWidth
                            val directionalPhase = if (isRtl) t else (1f - t)
                            val dy = cos(directionalPhase * 8f * PI.toFloat() + loopRad * 3f) * (trackHeight * 0.20f)
                            val wy = centerY + dy
                            if (i == 0) wavePath.moveTo(wx, wy) else wavePath.lineTo(wx, wy)
                        }
                        drawPath(
                            path = wavePath,
                            color = Color(0xFF22D3EE).copy(alpha = (progress * 0.65f).coerceIn(0.2f, 0.75f)),
                            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Endless sliding luminous energy pulse travelling from start -> thumb
                    val pulseX = if (isRtl) {
                        startX - loopProgress * fillSpan
                    } else {
                        startX + loopProgress * fillSpan
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f),
                                stageBadgeColor.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(pulseX, centerY),
                            radius = trackHeight * 1.4f
                        ),
                        radius = trackHeight * 1.4f,
                        center = Offset(pulseX, centerY)
                    )
                }
            }

            // Track crystal border
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF7DD3FC).copy(alpha = 0.55f),
                        stageBadgeColor.copy(alpha = 0.65f),
                        Color(0xFFE879F9).copy(alpha = if (progress > 0.8f) 0.90f else 0.35f)
                    ),
                    startX = startX,
                    endX = endX
                ),
                topLeft = Offset(trackLeft, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackRadius, trackRadius),
                style = Stroke(width = 1.1.dp.toPx())
            )

            // Specular upper glass reflection across filled/unfilled track
            drawLine(
                color = Color.White.copy(alpha = 0.28f),
                start = Offset(trackLeft + trackRadius, trackTop + 2.dp.toPx()),
                end = Offset(trackRight - trackRadius, trackTop + 2.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 3. SeekBar Thumb Orb (Ice Crystal at 0 -> Inferno -> Obsidian Black-Hole Singularity at 30,000)
            val thumbCenter = Offset(thumbX, centerY)
            val breath = 1f + sin(loopRad * 2f) * 0.10f

            // Outer ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stageBadgeColor.copy(alpha = 0.70f),
                        if (stage == FlameVisualStage.OBSIDIAN_PLASMA) Color(0xFF22D3EE).copy(alpha = 0.40f)
                        else Color(0xFF7DD3FC).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = thumbCenter,
                    radius = thumbRadius * 1.85f * breath
                ),
                radius = thumbRadius * 1.85f * breath,
                center = thumbCenter
            )

            // Rotating endless dual-orbit corona ring
            val orbitSize = Size(thumbRadius * 2.15f, thumbRadius * 2.15f)
            val orbitTopLeft = Offset(thumbCenter.x - orbitSize.width / 2f, thumbCenter.y - orbitSize.height / 2f)
            val spinDeg = loopProgress * 360f
            drawArc(
                color = if (stage == FlameVisualStage.OBSIDIAN_PLASMA) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.92f),
                startAngle = spinDeg,
                sweepAngle = 115f,
                useCenter = false,
                topLeft = orbitTopLeft,
                size = orbitSize,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = stageBadgeColor,
                startAngle = spinDeg + 180f,
                sweepAngle = 115f,
                useCenter = false,
                topLeft = orbitTopLeft,
                size = orbitSize,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Core thumb sphere
            val isObsidianPhase = stage == FlameVisualStage.OBSIDIAN_PLASMA
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isObsidianPhase) {
                        listOf(
                            Color(0xFF030208), // Pitch-black obsidian void center
                            Color(0xFF1A0733),
                            Color(0xFFE879F9)
                        )
                    } else if (stage <= FlameVisualStage.ICE_CRACKING) {
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFF7DD3FC),
                            Color(0xFF0284C7)
                        )
                    } else {
                        listOf(
                            Color(0xFFFFFFFF),
                            stageBadgeColor,
                            Color(0xFF0F172A)
                        )
                    },
                    center = thumbCenter,
                    radius = thumbRadius * 0.88f
                ),
                radius = thumbRadius * 0.88f,
                center = thumbCenter
            )

            // Crisp rim + 3D specular glint
            drawCircle(
                color = if (isObsidianPhase) Color(0xFFFDE047) else Color.White.copy(alpha = 0.92f),
                radius = thumbRadius * 0.88f,
                center = thumbCenter,
                style = Stroke(width = 1.6.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = if (isObsidianPhase) 0.55f else 0.9f),
                radius = thumbRadius * 0.24f,
                center = Offset(thumbCenter.x - thumbRadius * 0.30f, thumbCenter.y - thumbRadius * 0.30f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scale Milestones Row (0 .. 7.5K .. 15K .. 22.5K .. 30,000)
        val milestones = listOf(
            0.0 to "0",
            7_500.0 to "7.5K",
            15_000.0 to "15K",
            22_500.0 to "22.5K",
            30_000.0 to "30,000"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            milestones.forEach { (threshold, label) ->
                val reached = currentScore >= threshold
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = if (reached) FontWeight.ExtraBold else FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                    color = when {
                        threshold == 0.0 -> Color(0xFF7DD3FC)
                        threshold == 30_000.0 && reached -> Color(0xFFE879F9)
                        reached -> stageBadgeColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Ice (0) <-> Obsidian Black Plasma Max Power (30,000) Poles Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = Color(0xFF7DD3FC),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.flame_seekbar_ice_pole),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7DD3FC)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.flame_seekbar_obsidian_pole),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE879F9)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFE879F9),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Liquid Glass audit card for each recorded Flame event
 */
@Composable
private fun FlameAuditLogItemCard(
    entry: FlameLogEntry,
    currentLang: String = "ar"
) {
    val isAr = currentLang == "ar"
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
        "DOUBLE_STRIKE" -> {
            icon = Icons.Default.Bolt
            iconColor = NeonAmber
            iconBgColor = NeonAmber.copy(alpha = 0.18f)
        }
        "MULTI_STRIKE_COMBO" -> {
            icon = Icons.Default.LocalFireDepartment
            iconColor = Color(0xFFF472B6)
            iconBgColor = Color(0x33EC4899)
        }
        "EXTREME_OVERLOAD" -> {
            icon = Icons.Default.Warning
            iconColor = Color(0xFFEF4444)
            iconBgColor = Color(0x33EF4444)
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
                        text = if (isZero) {
                            if (isAr) "0.0 محمي" else "0.0 PROTECTED"
                        } else {
                            if (isAr) "$deltaSign${String.format(Locale.US, "%.1f", entry.change)} نقطة"
                            else "$deltaSign${String.format(Locale.US, "%.1f", entry.change)} PTS"
                        },
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
                        text = if (isAr) {
                            "الإجمالي: ${String.format(Locale.US, "%.1f", entry.resultingScore)} نقطة"
                        } else {
                            "Total: ${String.format(Locale.US, "%.1f", entry.resultingScore)} pts"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}
