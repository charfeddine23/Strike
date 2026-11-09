package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.deviations.DeviationsManager
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
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Production-ready, cinematic Jetpack Compose UI implementation for the "Deviations" screen
 * in StrikePulse:
 * - 3D Double-Helix DNA structure rendered via Canvas with spring physics and continuous rotation
 * - Dynamic Cyber-Shadow Entity & 20 Dark Ranks (Score 0..1000, fully gripping DNA at 0)
 * - Dynamic Cyber-Angel Entity & 20 Light Ranks (Score 1001..10000, transforming DNA to pure white at 10000)
 * - Dynamic Biological Energy Gauge (Femininity / Estrogen vs Masculinity / Testosterone)
 * - 50-Tier Biological / Energy progression ranking cyber-frame
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviationsScreen(
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

    var showInfoDialog by remember { mutableStateOf(false) }
    var showAllTiersSheet by remember { mutableStateOf(false) }
    var showSimulatorControls by remember { mutableStateOf(false) }

    // Spring-reactive rotation angle for 3D DNA
    val manualAngleOffset = remember { Animatable(0f) }
    var isDraggingDna by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "deviations_ambient")
    val ambientRot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_rot"
    )

    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    // Derived Deviations Models
    val isDarkTrack = DeviationsManager.isDarkTrack(flameScore)
    val darkRank = remember(flameScore) { DeviationsManager.getDarkRank(flameScore) }
    val lightRank = remember(flameScore) { DeviationsManager.getLightRank(flameScore) }
    val bioTier = remember(flameScore) { DeviationsManager.getBiologicalTier(flameScore) }

    // Smooth animated flame score for fluid gauges and entities
    val animatedScore by animateFloatAsState(
        targetValue = flameScore.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "anim_score"
    )

    val totalDnaAngle = ambientRot + manualAngleOffset.value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("deviations_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER ROW
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                            .testTag("deviations_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.deviations_title),
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.deviations_subtitle),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Simulator Toggle Button
                    IconButton(
                        onClick = {
                            showSimulatorControls = !showSimulatorControls
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (showSimulatorControls) Color(0x3338BDF8) else Color(0x221E293B))
                            .border(1.dp, if (showSimulatorControls) NeonCyan else Color(0x3338BDF8), CircleShape)
                            .testTag("btn_toggle_deviations_sim")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Simulator",
                            tint = if (showSimulatorControls) NeonCyan else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Info Dialog Button
                    IconButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            showInfoDialog = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x221E293B))
                            .border(1.dp, Color(0x3338BDF8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. SIMULATOR CONTROLS (IF EXPANDED)
        item {
            AnimatedVisibility(
                visible = showSimulatorControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Flame Simulator (0 - 10,000 PTS)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeonCyan
                            )
                            Text(
                                text = "${flameScore.toInt()} PTS",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = if (isDarkTrack) NeonRose else NeonCyan
                            )
                        }

                        Slider(
                            value = flameScore.toFloat(),
                            onValueChange = { newVal ->
                                viewModel.setFlameScore(newVal.toDouble())
                            },
                            valueRange = 0f..10000f,
                            colors = SliderDefaults.colors(
                                thumbColor = if (isDarkTrack) NeonRose else Color.White,
                                activeTrackColor = if (isDarkTrack) NeonRose else NeonCyan
                            ),
                            modifier = Modifier.testTag("deviations_score_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetScoreChip(label = "0 (بافوميت)", score = 0.0, onClick = { viewModel.setFlameScore(0.0) })
                            PresetScoreChip(label = "500 (ظلال)", score = 500.0, onClick = { viewModel.setFlameScore(500.0) })
                            PresetScoreChip(label = "4000 (تعادل)", score = 4000.0, onClick = { viewModel.setFlameScore(4000.0) })
                            PresetScoreChip(label = "10K (جبريل)", score = 10000.0, onClick = { viewModel.setFlameScore(10000.0) })
                        }
                    }
                }
            }
        }

        // 3. CENTRAL 3D DNA CANVAS & DYNAMIC CYBER-ENTITY HERO CARD
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dna_hero_card"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Track Identification Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkTrack) Color(0x33EF4444) else Color(0x3338BDF8))
                            .border(1.dp, if (isDarkTrack) NeonRose else NeonCyan, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkTrack) Icons.Default.Warning else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isDarkTrack) NeonRose else NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDarkTrack) {
                                    "${stringResource(R.string.deviations_dark_track)} • ${darkRank.index}/20"
                                } else {
                                    "${stringResource(R.string.deviations_light_track)} • ${lightRank.index}/20"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = if (isDarkTrack) NeonRose else NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // THE MAJESTIC 3D CANVAS: Entities in background + 3D Double-Helix DNA in center!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF070D1E))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDraggingDna = true },
                                    onDragEnd = {
                                        isDraggingDna = false
                                        coroutineScope.launch {
                                            manualAngleOffset.animateTo(
                                                targetValue = manualAngleOffset.value,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    },
                                    onDragCancel = { isDraggingDna = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            manualAngleOffset.snapTo(manualAngleOffset.value + dragAmount.x * 0.015f)
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val centerX = w / 2f
                            val centerY = h / 2f

                            val currentScore = animatedScore.toDouble()

                            // A. RENDER DYNAMIC BACKGROUND ENTITY
                            if (currentScore <= 1000.0) {
                                // CYBER-SHADOW ENTITY CREEPING CLOSER
                                val grip = darkRank.gripProgress // 0.0 at 1000 to 1.0 at 0
                                drawCyberShadowEntity(
                                    grip = grip,
                                    centerX = centerX,
                                    centerY = centerY,
                                    width = w,
                                    height = h,
                                    pulse = haloPulse
                                )
                            } else {
                                // CYBER-ANGEL ENTITY APPROACHING
                                val approach = lightRank.approachProgress // 0.0 at 1001 to 1.0 at 10000
                                val isPureWhiteTouch = currentScore >= 9990.0
                                drawCyberAngelEntity(
                                    approach = approach,
                                    isPureWhite = isPureWhiteTouch,
                                    centerX = centerX,
                                    centerY = centerY,
                                    width = w,
                                    height = h,
                                    pulse = haloPulse
                                )
                            }

                            // B. RENDER MAJESTIC 3D DOUBLE-HELIX DNA
                            val isPureWhite = currentScore >= 9990.0
                            val isShadowGripped = currentScore <= 50.0
                            draw3dDoubleHelixDna(
                                rotationAngle = totalDnaAngle,
                                centerX = centerX,
                                centerY = centerY,
                                width = w,
                                height = h,
                                isPureWhite = isPureWhite,
                                isShadowGripped = isShadowGripped,
                                isDarkTrack = isDarkTrack,
                                haloPulse = haloPulse
                            )
                        }

                        // Hint overlay at bottom of canvas
                        Text(
                            text = stringResource(R.string.deviations_dna_rotate_hint),
                            fontSize = 10.sp,
                            color = Color(0x9994A3B8),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ENTITY & RANK STATUS BADGE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x221E293B))
                            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isDarkTrack) "الرتبة المظلمة الحالية:" else "الرتبة الملائكية الحالية:",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = if (isDarkTrack) darkRank.title else lightRank.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = CaveatHandwriting,
                                color = if (isDarkTrack) NeonRose else if (flameScore >= 9990.0) Color.White else NeonCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDarkTrack) Color(0x33EF4444) else Color(0x3310B981))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isDarkTrack) {
                                    stringResource(R.string.deviations_grip_status, (darkRank.gripProgress * 100).toInt())
                                } else {
                                    stringResource(R.string.deviations_angel_status, (lightRank.approachProgress * 100).toInt())
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isDarkTrack) NeonRose else NeonEmerald
                            )
                        }
                    }
                }
            }
        }

        // 4. BOTTOM BIOLOGICAL GAUGE (ESTROGEN VS TESTOSTERONE)
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biological_gauge_card"),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Gauge Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.deviations_estrogen),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFB7185)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.deviations_testosterone),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biological Energy Bar with Oscillating Needle
                    val tPercent = bioTier.testosteronePercent
                    val ePercent = bioTier.estrogenPercent

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.2.dp, Color(0x4038BDF8), RoundedCornerShape(13.dp))
                    ) {
                        // Background Dual-Pole Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFF43F5E), // Estrogen Pink
                                            Color(0xFF8B5CF6), // Purple Equilibrium
                                            Color(0xFF06B6D4), // Cyan
                                            Color(0xFFF59E0B)  // Testosterone Amber
                                        )
                                    )
                                )
                        )

                        // Center Equilibrium Line
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(2.dp)
                                .height(26.dp)
                                .background(Color.White.copy(alpha = 0.6f))
                        )

                        // Animated Laser Needle / Marker
                        val animatedFraction by animateFloatAsState(
                            targetValue = tPercent,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                            label = "needle_pos"
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = (animatedFraction * 310).dp.coerceAtMost(310.dp))
                                .size(24.dp)
                                .scale(haloPulse)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Color(0xFF0F172A), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Percentage Readout Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(ePercent * 100).toInt()}% Estrogen",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFB7185)
                        )
                        Text(
                            text = "نقطة التوازن: 50% / 50%",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${(tPercent * 100).toInt()}% Testosterone",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // 5. 50-TIER BIOLOGICAL PROGRESSION LUXURY CYBER-FRAME
        item {
            LuxuryCyberRankFrame(
                tier = bioTier,
                currentScore = flameScore,
                onInspectAllTiers = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    showAllTiersSheet = true
                }
            )
        }
    }

    // ALL 50 TIERS INSPECTION MODAL BOTTOM SHEET
    if (showAllTiersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAllTiersSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                    Column {
                        Text(
                            text = stringResource(R.string.deviations_inspect_tiers),
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "تدرج المستويات البيولوجية من الأنثى المطلقة (1) إلى هرقل (50)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    IconButton(onClick = { showAllTiersSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val listState = rememberLazyListState(initialFirstVisibleItemIndex = (bioTier.index - 3).coerceAtLeast(0))
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(DeviationsManager.BIOLOGICAL_50_TIERS) { idx, name ->
                        val tierNum = idx + 1
                        val isCurrent = tierNum == bioTier.index
                        val step = 10000.0 / 50.0
                        val minSc = (idx * step).toInt()
                        val maxSc = ((idx + 1) * step).toInt()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) Color(0x3338BDF8) else Color(0x221E293B))
                                .border(
                                    1.dp,
                                    if (isCurrent) NeonCyan else Color(0x15FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) NeonCyan else Color(0x2264748B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$tierNum",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = if (isCurrent) Color(0xFF0F172A) else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = name,
                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isCurrent) NeonCyan else Color.White
                                    )
                                    Text(
                                        text = "$minSc - $maxSc PTS",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonCyan)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "مستواك الحالي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // INFO EXPLANATION DIALOG
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.deviations_title),
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "نظام الانحرافات والشيفرة الجينية المزدوجة:",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "1. مسار الظلال (0 - 1000 نقطة): يقترب كيان الظل السيبراني كلما انخفضت النقاط نحو الصفر حتى يمسك بالـ DNA بالكامل عند الصفر مع 20 رتبة مظلمة تنتهي بـ 'بافوميت'.",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "2. مسار النور الملائكي (1001 - 10000 نقطة): يقترب الملاك السيبراني بأجنحته النورانية ويتحول الـ DNA إلى الأبيض الخالص عند بلوغ 10,000 نقطة مع 20 رتبة نورانية تنتهي بـ 'جبريل'.",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "3. مقياس الطاقة البيولوجية و50 مستوى: يتأرجح بين الأنوثة (إستروجين) والرجولة (تيستوستيرون) عبر 50 رتبة من 'الأنثى المطلقة' إلى 'هرقل'.",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("حسناً", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0F172A),
            tonalElevation = 8.dp
        )
    }
}

/**
 * Luxury Cyber-Frame HUD for 50-Tier Biological System
 */
@Composable
fun LuxuryCyberRankFrame(
    tier: com.example.data.deviations.BiologicalTier,
    currentScore: Double,
    onInspectAllTiers: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xD90B132B))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF818CF8),
                        Color(0xFFC084FC),
                        Color(0xFF38BDF8)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(18.dp)
            .testTag("cyber_rank_frame")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3338BDF8))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "TIER ${tier.index} / 50",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                }

                Button(
                    onClick = onInspectAllTiers,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x331E293B)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "كافة المستويات", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Majestic Arabic Rank Title
            Text(
                text = tier.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = CaveatHandwriting,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Score Range
            val nextScore = tier.maxScore
            val ptsToNext = (nextScore - currentScore).coerceAtLeast(0.0)
            Text(
                text = if (ptsToNext > 0) "المستوى التالي بعد ${ptsToNext.toInt()} نقطة" else "وصلت إلى القمة المطلقة!",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tier Progress Bar
            val tierProgress = ((currentScore - tier.minScore) / (tier.maxScore - tier.minScore)).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x331E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(tierProgress)
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(listOf(NeonCyan, Color(0xFFC084FC)))
                        )
                )
            }
        }
    }
}

/**
 * Draws the Cyber-Shadow Entity (Score 0 to 1000):
 * Horned demonic silhouette with glowing crimson eyes, dark shadows, and creeping claw tendrils
 * gripping the DNA at score 0.
 */
fun DrawScope.drawCyberShadowEntity(
    grip: Float, // 0.0 at 1000 to 1.0 at 0
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    pulse: Float
) {
    val scale = 0.6f + grip * 0.5f // grows larger as score drops
    val entityAlpha = (0.20f + grip * 0.75f).coerceIn(0.1f, 0.95f)

    // Dark radial void aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x99450A0A).copy(alpha = entityAlpha * 0.6f),
                Color(0x66180505).copy(alpha = entityAlpha * 0.4f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = width * (0.55f * scale * pulse)
        ),
        radius = width * (0.55f * scale * pulse)
    )

    // Dark Horned Silhouette / Cyber Baphomet Head Outline
    val headPath = Path().apply {
        // Left horn
        moveTo(centerX - 40f * scale, centerY - 60f * scale)
        cubicTo(
            centerX - 70f * scale, centerY - 110f * scale,
            centerX - 60f * scale, centerY - 150f * scale,
            centerX - 25f * scale, centerY - 120f * scale
        )
        // Center skull brow
        lineTo(centerX, centerY - 80f * scale)
        // Right horn
        lineTo(centerX + 25f * scale, centerY - 120f * scale)
        cubicTo(
            centerX + 60f * scale, centerY - 150f * scale,
            centerX + 70f * scale, centerY - 110f * scale,
            centerX + 40f * scale, centerY - 60f * scale
        )
        // Jawline
        lineTo(centerX + 20f * scale, centerY - 20f * scale)
        lineTo(centerX, centerY - 10f * scale)
        lineTo(centerX - 20f * scale, centerY - 20f * scale)
        close()
    }

    drawPath(
        path = headPath,
        color = Color(0xFF1C070C).copy(alpha = entityAlpha),
        style = androidx.compose.ui.graphics.drawscope.Fill
    )

    drawPath(
        path = headPath,
        color = Color(0xFFEF4444).copy(alpha = entityAlpha * 0.8f),
        style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
    )

    // Glowing Crimson Eyes
    val eyeGlow = 4f * scale * pulse
    drawCircle(
        color = Color(0xFFEF4444),
        radius = eyeGlow,
        center = Offset(centerX - 16f * scale, centerY - 55f * scale)
    )
    drawCircle(
        color = Color(0xFFEF4444),
        radius = eyeGlow,
        center = Offset(centerX + 16f * scale, centerY - 55f * scale)
    )

    // Creeping Claws / Tendrils reaching toward DNA
    val tendrilReach = grip * (width * 0.38f)
    if (grip > 0.15f) {
        // Left claw reaching inward
        drawLine(
            color = Color(0xFFEF4444).copy(alpha = entityAlpha * 0.7f),
            start = Offset(centerX - width * 0.45f, centerY),
            end = Offset(centerX - width * 0.45f + tendrilReach, centerY + sin(pulse) * 10f),
            strokeWidth = 3f * grip,
            cap = StrokeCap.Round
        )
        // Right claw reaching inward
        drawLine(
            color = Color(0xFFEF4444).copy(alpha = entityAlpha * 0.7f),
            start = Offset(centerX + width * 0.45f, centerY),
            end = Offset(centerX + width * 0.45f - tendrilReach, centerY - sin(pulse) * 10f),
            strokeWidth = 3f * grip,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Draws the Majestic Cyber-Angel Entity (Score 1001 to 10000):
 * Radiant wings, golden/cyan halos, celestial light rays, touching the DNA at 10000.
 */
fun DrawScope.drawCyberAngelEntity(
    approach: Float, // 0.0 at 1001 to 1.0 at 10000
    isPureWhite: Boolean,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    pulse: Float
) {
    val scale = 0.65f + approach * 0.45f
    val angelAlpha = (0.25f + approach * 0.70f).coerceIn(0.15f, 0.98f)

    val primaryAngelColor = if (isPureWhite) Color.White else Color(0xFF38BDF8)
    val haloColor = if (isPureWhite) Color(0xFFFFFFFF) else Color(0xFFFCD34D) // Gold or Diamond White

    // Celestial Radial Starlight Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryAngelColor.copy(alpha = angelAlpha * 0.45f),
                haloColor.copy(alpha = angelAlpha * 0.20f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY - 40f),
            radius = width * (0.60f * scale * pulse)
        ),
        radius = width * (0.60f * scale * pulse)
    )

    // Sacred Geometric Halo
    drawCircle(
        color = haloColor.copy(alpha = angelAlpha * 0.75f),
        radius = (38f * scale * pulse),
        center = Offset(centerX, centerY - 100f * scale),
        style = Stroke(width = 2.2f)
    )

    // Radiant Wings
    val wingSpan = width * (0.35f + approach * 0.15f)
    val leftWing = Path().apply {
        moveTo(centerX, centerY - 60f * scale)
        cubicTo(
            centerX - wingSpan * 0.5f, centerY - 140f * scale,
            centerX - wingSpan, centerY - 90f * scale,
            centerX - wingSpan * 0.8f, centerY
        )
        cubicTo(
            centerX - wingSpan * 0.5f, centerY + 40f * scale,
            centerX - 20f * scale, centerY - 20f * scale,
            centerX, centerY - 30f * scale
        )
        close()
    }

    val rightWing = Path().apply {
        moveTo(centerX, centerY - 60f * scale)
        cubicTo(
            centerX + wingSpan * 0.5f, centerY - 140f * scale,
            centerX + wingSpan, centerY - 90f * scale,
            centerX + wingSpan * 0.8f, centerY
        )
        cubicTo(
            centerX + wingSpan * 0.5f, centerY + 40f * scale,
            centerX + 20f * scale, centerY - 20f * scale,
            centerX, centerY - 30f * scale
        )
        close()
    }

    drawPath(leftWing, color = primaryAngelColor.copy(alpha = angelAlpha * 0.35f))
    drawPath(leftWing, color = haloColor.copy(alpha = angelAlpha * 0.8f), style = Stroke(width = 1.8f))
    drawPath(rightWing, color = primaryAngelColor.copy(alpha = angelAlpha * 0.35f))
    drawPath(rightWing, color = haloColor.copy(alpha = angelAlpha * 0.8f), style = Stroke(width = 1.8f))

    // Celestial Hands reaching to touch DNA at score 10000
    if (approach > 0.80f) {
        val touchReach = (approach - 0.80f) / 0.20f
        val handY = centerY + 30f * scale
        // Left hand touching DNA
        drawCircle(
            color = Color.White.copy(alpha = touchReach),
            radius = 6f * pulse,
            center = Offset(centerX - 35f * (1f - touchReach * 0.3f), handY)
        )
        // Right hand touching DNA
        drawCircle(
            color = Color.White.copy(alpha = touchReach),
            radius = 6f * pulse,
            center = Offset(centerX + 35f * (1f - touchReach * 0.3f), handY)
        )
    }
}

/**
 * Renders the 3D Double-Helix DNA Structure via Canvas:
 * Strand A and Strand B rotate vertically in 3D perspective with depth scaling,
 * connected by base pair rungs, transforming to pure white at score 10000.
 */
fun DrawScope.draw3dDoubleHelixDna(
    rotationAngle: Float,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    isPureWhite: Boolean,
    isShadowGripped: Boolean,
    isDarkTrack: Boolean,
    haloPulse: Float
) {
    val rungsCount = 22
    val radiusX = width * 0.28f
    val dnaHeight = height * 0.82f
    val topY = centerY - dnaHeight / 2f

    // Color definitions
    val strandAColor = when {
        isPureWhite -> Color.White
        isShadowGripped -> Color(0xFFEF4444)
        isDarkTrack -> Color(0xFFF43F5E)
        else -> NeonCyan
    }

    val strandBColor = when {
        isPureWhite -> Color(0xFFF0F9FF)
        isShadowGripped -> Color(0xFF991B1B)
        isDarkTrack -> Color(0xFFC084FC)
        else -> Color(0xFF38BDF8)
    }

    val rungBaseColor = when {
        isPureWhite -> Color.White
        isShadowGripped -> Color(0xFFEF4444)
        isDarkTrack -> Color(0xFFF87171)
        else -> Color(0xFFFCD34D) // Gold neon
    }

    for (i in 0 until rungsCount) {
        val y = topY + (i.toFloat() / (rungsCount - 1)) * dnaHeight
        // Vertical helical phase
        val phase = rotationAngle + (i * 0.42f)

        val sinA = sin(phase)
        val cosA = cos(phase)

        val x1 = centerX + radiusX * cosA
        val x2 = centerX - radiusX * cosA

        val z1 = sinA // -1 (back) to +1 (front)
        val z2 = -sinA

        // Depth perspective calculation
        val depth1 = (z1 + 1f) / 2f // 0f (back) to 1f (front)
        val depth2 = (z2 + 1f) / 2f

        // Draw connecting Base Pair Rung
        val rungAlpha = (0.25f + (1f - abs(sinA)) * 0.65f).coerceIn(0.15f, 1f)
        val rungWidth = if (isPureWhite) 3f else 1.8f
        drawLine(
            color = rungBaseColor.copy(alpha = rungAlpha * (if (isPureWhite) 0.95f else 0.70f)),
            start = Offset(x1, y),
            end = Offset(x2, y),
            strokeWidth = rungWidth,
            cap = StrokeCap.Round
        )

        // Hydrogen bond center node
        val bondAlpha = if (isPureWhite) 1f else 0.8f
        drawCircle(
            color = Color.White.copy(alpha = bondAlpha),
            radius = if (isPureWhite) 3.5f else 2.2f,
            center = Offset(centerX, y)
        )

        // Draw Strand A Node
        val radius1 = (3.5f + depth1 * 3.5f) * (if (isPureWhite) 1.25f else 1f)
        val alpha1 = (0.35f + depth1 * 0.65f).coerceIn(0.2f, 1f)
        drawCircle(
            color = strandAColor.copy(alpha = alpha1),
            radius = radius1,
            center = Offset(x1, y)
        )
        if (depth1 > 0.6f || isPureWhite) {
            // Shiny highlight core
            drawCircle(
                color = Color.White.copy(alpha = alpha1 * 0.9f),
                radius = radius1 * 0.45f,
                center = Offset(x1, y)
            )
        }

        // Draw Strand B Node
        val radius2 = (3.5f + depth2 * 3.5f) * (if (isPureWhite) 1.25f else 1f)
        val alpha2 = (0.35f + depth2 * 0.65f).coerceIn(0.2f, 1f)
        drawCircle(
            color = strandBColor.copy(alpha = alpha2),
            radius = radius2,
            center = Offset(x2, y)
        )
        if (depth2 > 0.6f || isPureWhite) {
            drawCircle(
                color = Color.White.copy(alpha = alpha2 * 0.9f),
                radius = radius2 * 0.45f,
                center = Offset(x2, y)
            )
        }
    }
}

@Composable
fun PresetScoreChip(label: String, score: Double, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x221E293B))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
    }
}
