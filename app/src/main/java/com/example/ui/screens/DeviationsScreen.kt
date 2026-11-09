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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * AAA-Quality Cinematic Jetpack Compose Implementation of "Deviations" in StrikePulse:
 * - 3D Volumetric Neon DNA Structure: Thick glowing volumetric 3D tubes rendered via multi-pass bloom,
 *   with an internal particle system of flowing energy dots and hydrogen-bond orbital rings.
 * - 60-90 FPS Drag-to-rotate with velocity spring physics and haptic milestones.
 * - Dynamic Cyber-Shadow Entity (The "Devil/Shadow" Overhaul): Menacing biomechanical claws physically
 *   wrapping around and squeezing the DNA based on "Shadow Grip" percentage, with glitch scanlines,
 *   blood-red ambient glow, and Baphomet horned demon silhouette at score 0.
 * - Dynamic Cyber-Angel Entity: Majestic luminous archangel wings, dual rotating sacred halos,
 *   and divine diamond lightning transmuting the DNA to pure white at score 10,000 (Gabriel).
 * - Pulsing Cyber Warning Bar: Intense alert bar reflecting real-time threat or celestial resonance.
 * - Dynamic Biological Energy Gauge & 50-Tier Biological Rank System.
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

    // Spring-reactive rotation angle for 3D DNA with velocity
    val manualAngleOffset = remember { Animatable(0f) }
    var lastHapticAngle by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "deviations_ambient")

    // Continuous 3D rotation
    val ambientRot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_rot"
    )

    // Breathing pulse for halos and hazard glow
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    // Flowing phase for the internal particle system inside DNA tubes
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dna_particles"
    )

    // Glitch flicker phase for dark track
    val glitchPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitch_phase"
    )

    // Warning bar strobe pulse
    val warningStrobe by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_strobe"
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

        // 2. PULSING CYBER WARNING BAR
        item {
            PulsingCyberWarningBar(
                isDarkTrack = isDarkTrack,
                flameScore = flameScore,
                darkRankTitle = darkRank.title,
                lightRankTitle = lightRank.title,
                gripProgress = darkRank.gripProgress,
                approachProgress = lightRank.approachProgress,
                strobe = warningStrobe
            )
        }

        // 3. SIMULATOR CONTROLS (IF EXPANDED)
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

        // 4. CENTRAL 3D VOLUMETRIC DNA CANVAS & MENACING CYBER-SHADOW / ANGEL HERO CARD
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
                    // Track Identification Badge with Dynamic Glow
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkTrack) Color(0x38EF4444) else Color(0x3838BDF8))
                            .border(1.2.dp, if (isDarkTrack) NeonRose else NeonCyan, RoundedCornerShape(12.dp))
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

                    // THE MAJESTIC 3D VOLUMETRIC CANVAS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (isDarkTrack) Color(0xFF1A060C) else Color(0xFF071228),
                                        if (isDarkTrack) Color(0xFF0A0205) else Color(0xFF040A18)
                                    )
                                )
                            )
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { /* Started drag */ },
                                    onDragEnd = {
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
                                    onDragCancel = { },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val delta = dragAmount.x * 0.016f
                                        val newAngle = manualAngleOffset.value + delta
                                        coroutineScope.launch {
                                            manualAngleOffset.snapTo(newAngle)
                                        }
                                        // Haptic tick on rotational milestone
                                        if (abs(newAngle - lastHapticAngle) > 0.6f) {
                                            lastHapticAngle = newAngle
                                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
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

                            // Background Cyber-Grid
                            drawCyberGrid(w, h, isDarkTrack, haloPulse)

                            // 1. RENDER BACKGROUND ENTITY (Demonic Claws & Wings OR Angelic Ethereal Wings)
                            if (currentScore <= 1000.0) {
                                // CYBER-SHADOW ENTITY & CLAWS WRAPPING DNA
                                val grip = darkRank.gripProgress
                                drawCyberShadowClawsAndDemon(
                                    grip = grip,
                                    centerX = centerX,
                                    centerY = centerY,
                                    width = w,
                                    height = h,
                                    pulse = haloPulse,
                                    glitch = glitchPhase
                                )
                            } else {
                                // CYBER-ANGEL ENTITY WITH RADIANT WINGS & HALO
                                val approach = lightRank.approachProgress
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

                            // 2. RENDER 3D VOLUMETRIC NEON DNA STRUCTURE
                            val isPureWhite = currentScore >= 9990.0
                            val isShadowGripped = currentScore <= 50.0
                            val gripProgress = if (currentScore <= 1000.0) darkRank.gripProgress else 0f

                            drawVolumetricNeonDnaStructure(
                                rotationAngle = totalDnaAngle,
                                centerX = centerX,
                                centerY = centerY,
                                width = w,
                                height = h,
                                isPureWhite = isPureWhite,
                                isShadowGripped = isShadowGripped,
                                isDarkTrack = isDarkTrack,
                                haloPulse = haloPulse,
                                particlePhase = particlePhase,
                                gripProgress = gripProgress,
                                glitchPhase = if (isDarkTrack) glitchPhase else 0f
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
                                color = if (isDarkTrack) NeonRose else NeonCyan,
                                fontFamily = PlusJakartaSans
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isDarkTrack) {
                                    stringResource(R.string.deviations_grip_status, (darkRank.gripProgress * 100).toInt())
                                } else {
                                    stringResource(R.string.deviations_angel_status, (lightRank.approachProgress * 100).toInt())
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTrack) NeonRose else Color.White
                            )

                            // Small Grip Progress Bar
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x331E293B))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (isDarkTrack) darkRank.gripProgress else lightRank.approachProgress)
                                        .height(4.dp)
                                        .background(if (isDarkTrack) NeonRose else NeonCyan)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. DYNAMIC BIOLOGICAL ENERGY GAUGE (ESTROGEN VS TESTOSTERONE)
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مؤشر التوازن البيولوجي (الطاقة الحيوية)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.deviations_tier_label, bioTier.index),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val tPercent = bioTier.testosteronePercent
                    val ePercent = bioTier.estrogenPercent

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.2.dp, Color(0x4038BDF8), RoundedCornerShape(14.dp))
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
                                .height(28.dp)
                                .background(Color.White.copy(alpha = 0.7f))
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
                                .size(26.dp)
                                .scale(haloPulse)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.5.dp, Color(0xFF0F172A), CircleShape)
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

        // 6. 50-TIER BIOLOGICAL PROGRESSION LUXURY CYBER-FRAME
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
                                        .background(if (isCurrent) NeonCyan else Color(0x22FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$tierNum",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isCurrent) Color.Black else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
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
                                        .background(Color(0x3338BDF8))
                                        .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "المستوى الحالي",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonCyan
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
 * Pulsing Cyber Warning Bar: Reflects immediate system hazard or celestial resonance
 */
@Composable
fun PulsingCyberWarningBar(
    isDarkTrack: Boolean,
    flameScore: Double,
    darkRankTitle: String,
    lightRankTitle: String,
    gripProgress: Float,
    approachProgress: Float,
    strobe: Float
) {
    val barColor = if (isDarkTrack) NeonRose else NeonCyan
    val barBg = if (isDarkTrack) {
        Color(0x33EF4444).copy(alpha = 0.25f + gripProgress * 0.2f)
    } else {
        Color(0x2238BDF8).copy(alpha = 0.25f + approachProgress * 0.15f)
    }

    val warningText = when {
        flameScore <= 0.0 -> stringResource(R.string.deviations_baphomet_overload)
        isDarkTrack -> stringResource(
            R.string.deviations_warning_shadow_hazard,
            (gripProgress * 100).toInt(),
            darkRankTitle
        )
        flameScore >= 9990.0 -> stringResource(R.string.deviations_gabriel_ascension)
        else -> stringResource(
            R.string.deviations_harmonic_resonance,
            (approachProgress * 100).toInt(),
            lightRankTitle
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(barBg)
            .border(
                width = if (isDarkTrack && gripProgress > 0.8f) 1.8.dp else 1.2.dp,
                color = barColor.copy(alpha = strobe),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(barColor.copy(alpha = 0.25f * strobe))
                    .border(1.dp, barColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDarkTrack) Icons.Default.Warning else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = warningText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTrack) Color.White else Color(0xFFF0FDF4),
                letterSpacing = 0.2.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Draws Cyber-Grid and ambient background lighting
 */
fun DrawScope.drawCyberGrid(width: Float, height: Float, isDarkTrack: Boolean, pulse: Float) {
    val gridColor = if (isDarkTrack) Color(0x18EF4444) else Color(0x1438BDF8)
    val horizonY = height * 0.85f

    // Horizontal grid lines
    var y = horizonY
    var step = 8f
    while (y < height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += step
        step *= 1.35f
    }

    // Perspective lines
    val vanishingX = width / 2f
    val vanishingY = horizonY - 40f
    val lineCount = 9
    for (i in 0..lineCount) {
        val bottomX = (width / lineCount) * i
        drawLine(
            color = gridColor,
            start = Offset(vanishingX, vanishingY),
            end = Offset(bottomX, height),
            strokeWidth = 1f
        )
    }

    // Dynamic ambient glow sphere
    val glowColor = if (isDarkTrack) Color(0x35EF4444) else Color(0x2838BDF8)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = Offset(width / 2f, height / 2f),
            radius = width * 0.55f * pulse
        ),
        radius = width * 0.55f * pulse
    )
}

/**
 * Draws the Menacing Cyber-Shadow Entity (Score 0 to 1000):
 * - Bio-mechanical demonic claws reaching from flanks and wrapping tightly around DNA rungs
 * - Menacing horned silhouette with glowing red eyes and cybernetic wings
 * - Glitch lines & chromatic distortion as grip approaches 1.0 (Baphomet at 0)
 */
fun DrawScope.drawCyberShadowClawsAndDemon(
    grip: Float, // 0.0 at 1000 to 1.0 at 0
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    pulse: Float,
    glitch: Float
) {
    val scale = 0.65f + grip * 0.55f
    val entityAlpha = (0.25f + grip * 0.72f).coerceIn(0.15f, 0.98f)

    // Dark void background core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x99450A0A).copy(alpha = entityAlpha * 0.65f),
                Color(0x66180505).copy(alpha = entityAlpha * 0.45f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY - 20f),
            radius = width * (0.60f * scale * pulse)
        ),
        radius = width * (0.60f * scale * pulse)
    )

    // Menacing Demonic Wings
    val wingSpan = width * (0.42f + grip * 0.22f)
    val wingHeight = height * (0.35f + grip * 0.15f)

    val leftWing = Path().apply {
        moveTo(centerX - 10f, centerY - 40f)
        cubicTo(
            centerX - wingSpan * 0.5f, centerY - wingHeight * 0.8f,
            centerX - wingSpan, centerY - wingHeight * 0.4f,
            centerX - wingSpan * 0.95f, centerY + 20f
        )
        cubicTo(
            centerX - wingSpan * 0.6f, centerY + 80f,
            centerX - 40f, centerY + 20f,
            centerX - 10f, centerY - 20f
        )
        close()
    }

    val rightWing = Path().apply {
        moveTo(centerX + 10f, centerY - 40f)
        cubicTo(
            centerX + wingSpan * 0.5f, centerY - wingHeight * 0.8f,
            centerX + wingSpan, centerY - wingHeight * 0.4f,
            centerX + wingSpan * 0.95f, centerY + 20f
        )
        cubicTo(
            centerX + wingSpan * 0.6f, centerY + 80f,
            centerX + 40f, centerY + 20f,
            centerX + 10f, centerY - 20f
        )
        close()
    }

    drawPath(leftWing, color = Color(0x331E050B).copy(alpha = entityAlpha))
    drawPath(leftWing, color = Color(0xFFEF4444).copy(alpha = entityAlpha * 0.7f), style = Stroke(width = 2.2f * scale))
    drawPath(rightWing, color = Color(0x331E050B).copy(alpha = entityAlpha))
    drawPath(rightWing, color = Color(0xFFEF4444).copy(alpha = entityAlpha * 0.7f), style = Stroke(width = 2.2f * scale))

    // Horned Demonic Head (Baphomet Silhouette)
    val headPath = Path().apply {
        moveTo(centerX - 35f * scale, centerY - 65f * scale)
        cubicTo(
            centerX - 75f * scale, centerY - 120f * scale,
            centerX - 65f * scale, centerY - 170f * scale,
            centerX - 20f * scale, centerY - 135f * scale
        )
        lineTo(centerX, centerY - 95f * scale)
        lineTo(centerX + 20f * scale, centerY - 135f * scale)
        cubicTo(
            centerX + 65f * scale, centerY - 170f * scale,
            centerX + 75f * scale, centerY - 120f * scale,
            centerX + 35f * scale, centerY - 65f * scale
        )
        lineTo(centerX + 18f * scale, centerY - 30f * scale)
        lineTo(centerX, centerY - 18f * scale)
        lineTo(centerX - 18f * scale, centerY - 30f * scale)
        close()
    }

    drawPath(headPath, color = Color(0xFF140306).copy(alpha = entityAlpha))
    drawPath(headPath, color = Color(0xFFEF4444).copy(alpha = entityAlpha * 0.9f), style = Stroke(width = 2.5f * scale))

    // Forehead Runic Pentagram Node
    drawCircle(
        color = Color(0xFFEF4444).copy(alpha = entityAlpha),
        radius = 4f * scale * pulse,
        center = Offset(centerX, centerY - 80f * scale)
    )

    // Piercing Crimson Eyes with Laser Trails
    val eyeY = centerY - 60f * scale
    val eyeGlowRadius = 5.5f * scale * pulse
    drawCircle(color = Color(0xFFFF0055), radius = eyeGlowRadius, center = Offset(centerX - 15f * scale, eyeY))
    drawCircle(color = Color.White, radius = eyeGlowRadius * 0.45f, center = Offset(centerX - 15f * scale, eyeY))
    drawCircle(color = Color(0xFFFF0055), radius = eyeGlowRadius, center = Offset(centerX + 15f * scale, eyeY))
    drawCircle(color = Color.White, radius = eyeGlowRadius * 0.45f, center = Offset(centerX + 15f * scale, eyeY))

    // BIOMECHANICAL CLAWS WRAPPING TIGHTLY AROUND THE DNA
    val clawReach = grip * (width * 0.42f)
    if (grip > 0.08f) {
        val clawCenterY = centerY + 15f
        // Left Robotic Arm & Claws reaching inward
        val leftArmStart = Offset(centerX - width * 0.48f, clawCenterY - 30f)
        val leftJoint = Offset(centerX - width * 0.48f + clawReach * 0.6f, clawCenterY - 15f)
        val leftPalm = Offset(centerX - width * 0.48f + clawReach, clawCenterY)

        // Draw arm segments
        drawLine(color = Color(0xFFEF4444).copy(alpha = entityAlpha), start = leftArmStart, end = leftJoint, strokeWidth = 5f * scale, cap = StrokeCap.Round)
        drawLine(color = Color(0xFFEF4444).copy(alpha = entityAlpha), start = leftJoint, end = leftPalm, strokeWidth = 4f * scale, cap = StrokeCap.Round)
        drawCircle(color = Color.White, radius = 3.5f * scale, center = leftJoint)

        // 3 curved talons gripping the DNA
        for (f in -1..1) {
            val talonTip = Offset(leftPalm.x + 24f * grip, leftPalm.y + f * 14f * scale + sin(pulse) * 4f)
            val talonPath = Path().apply {
                moveTo(leftPalm.x, leftPalm.y + f * 8f)
                cubicTo(leftPalm.x + 12f, leftPalm.y + f * 16f, leftPalm.x + 20f, leftPalm.y + f * 12f, talonTip.x, talonTip.y)
            }
            drawPath(talonPath, color = Color(0xFFEF4444).copy(alpha = entityAlpha), style = Stroke(width = 3f * scale, cap = StrokeCap.Round))
            // Electric clench sparks when tightly gripping
            if (grip > 0.75f) {
                drawCircle(color = Color.White, radius = 2.5f * pulse, center = talonTip)
            }
        }

        // Right Robotic Arm & Claws reaching inward
        val rightArmStart = Offset(centerX + width * 0.48f, clawCenterY + 30f)
        val rightJoint = Offset(centerX + width * 0.48f - clawReach * 0.6f, clawCenterY + 15f)
        val rightPalm = Offset(centerX + width * 0.48f - clawReach, clawCenterY)

        drawLine(color = Color(0xFFEF4444).copy(alpha = entityAlpha), start = rightArmStart, end = rightJoint, strokeWidth = 5f * scale, cap = StrokeCap.Round)
        drawLine(color = Color(0xFFEF4444).copy(alpha = entityAlpha), start = rightJoint, end = rightPalm, strokeWidth = 4f * scale, cap = StrokeCap.Round)
        drawCircle(color = Color.White, radius = 3.5f * scale, center = rightJoint)

        for (f in -1..1) {
            val talonTip = Offset(rightPalm.x - 24f * grip, rightPalm.y + f * 14f * scale - sin(pulse) * 4f)
            val talonPath = Path().apply {
                moveTo(rightPalm.x, rightPalm.y + f * 8f)
                cubicTo(rightPalm.x - 12f, rightPalm.y + f * 16f, rightPalm.x - 20f, rightPalm.y + f * 12f, talonTip.x, talonTip.y)
            }
            drawPath(talonPath, color = Color(0xFFEF4444).copy(alpha = entityAlpha), style = Stroke(width = 3f * scale, cap = StrokeCap.Round))
            if (grip > 0.75f) {
                drawCircle(color = Color.White, radius = 2.5f * pulse, center = talonTip)
            }
        }
    }

    // Glitch Scanlines & Distortion at High Grip
    if (grip > 0.65f) {
        val glitchOffset = sin(glitch * PI.toFloat() * 6f) * 12f
        drawLine(
            color = Color(0xFFFF0055).copy(alpha = 0.4f),
            start = Offset(centerX - width * 0.35f + glitchOffset, centerY - 40f),
            end = Offset(centerX + width * 0.35f + glitchOffset, centerY - 40f),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFF38BDF8).copy(alpha = 0.3f),
            start = Offset(centerX - width * 0.4f - glitchOffset, centerY + 30f),
            end = Offset(centerX + width * 0.4f - glitchOffset, centerY + 30f),
            strokeWidth = 1.5f
        )
    }
}

/**
 * Draws the Majestic Cyber-Angel Entity (Score 1001 to 10000):
 * - Multi-layered ethereal luminous wings
 * - Sacred geometric halo rings
 * - Celestial hands touching DNA at score 10,000 (Gabriel)
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
    val scale = 0.70f + approach * 0.45f
    val angelAlpha = (0.30f + approach * 0.68f).coerceIn(0.2f, 0.98f)

    val primaryAngelColor = if (isPureWhite) Color.White else Color(0xFF38BDF8)
    val haloColor = if (isPureWhite) Color(0xFFFFFFFF) else Color(0xFFFCD34D) // Gold or Diamond White

    // Celestial Starlight Radial Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryAngelColor.copy(alpha = angelAlpha * 0.50f),
                haloColor.copy(alpha = angelAlpha * 0.25f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY - 50f),
            radius = width * (0.65f * scale * pulse)
        ),
        radius = width * (0.65f * scale * pulse)
    )

    // Sacred Concentric Geometric Halo Rings
    drawCircle(
        color = haloColor.copy(alpha = angelAlpha * 0.85f),
        radius = 42f * scale * pulse,
        center = Offset(centerX, centerY - 105f * scale),
        style = Stroke(width = 2.4f)
    )
    drawCircle(
        color = primaryAngelColor.copy(alpha = angelAlpha * 0.55f),
        radius = 52f * scale * pulse,
        center = Offset(centerX, centerY - 105f * scale),
        style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    )

    // Multi-Layered Luminous Archangel Wings
    val wingSpan = width * (0.42f + approach * 0.18f)
    val wingHeight = height * (0.40f + approach * 0.15f)

    val leftWingOuter = Path().apply {
        moveTo(centerX, centerY - 65f * scale)
        cubicTo(
            centerX - wingSpan * 0.55f, centerY - wingHeight * 0.85f,
            centerX - wingSpan, centerY - wingHeight * 0.45f,
            centerX - wingSpan * 0.85f, centerY + 10f
        )
        cubicTo(
            centerX - wingSpan * 0.5f, centerY + 50f * scale,
            centerX - 25f * scale, centerY - 20f * scale,
            centerX, centerY - 35f * scale
        )
        close()
    }

    val rightWingOuter = Path().apply {
        moveTo(centerX, centerY - 65f * scale)
        cubicTo(
            centerX + wingSpan * 0.55f, centerY - wingHeight * 0.85f,
            centerX + wingSpan, centerY - wingHeight * 0.45f,
            centerX + wingSpan * 0.85f, centerY + 10f
        )
        cubicTo(
            centerX + wingSpan * 0.5f, centerY + 50f * scale,
            centerX + 25f * scale, centerY - 20f * scale,
            centerX, centerY - 35f * scale
        )
        close()
    }

    drawPath(leftWingOuter, color = primaryAngelColor.copy(alpha = angelAlpha * 0.30f))
    drawPath(leftWingOuter, color = haloColor.copy(alpha = angelAlpha * 0.85f), style = Stroke(width = 2.2f))
    drawPath(rightWingOuter, color = primaryAngelColor.copy(alpha = angelAlpha * 0.30f))
    drawPath(rightWingOuter, color = haloColor.copy(alpha = angelAlpha * 0.85f), style = Stroke(width = 2.2f))

    // Celestial Hands Touching DNA at 10000
    if (approach > 0.82f) {
        val touchReach = ((approach - 0.82f) / 0.18f).coerceIn(0f, 1f)
        val handY = centerY + 25f * scale

        // Pure white divine plasma bursts
        drawCircle(color = Color.White.copy(alpha = touchReach), radius = 8f * pulse, center = Offset(centerX - 42f * (1f - touchReach * 0.25f), handY))
        drawCircle(color = Color.White.copy(alpha = touchReach), radius = 8f * pulse, center = Offset(centerX + 42f * (1f - touchReach * 0.25f), handY))

        // Lightning arc connecting hands to DNA
        drawLine(
            color = Color.White.copy(alpha = touchReach * 0.9f),
            start = Offset(centerX - 35f, handY),
            end = Offset(centerX + 35f, handY),
            strokeWidth = 3f * touchReach,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 3D Volumetric Neon DNA Structure Rendered via Multi-Pass Bloom & Internal Particle System:
 * - 28 Rungs with 3D Z-sorting
 * - Continuous connected volumetric neon tubes for Strand A & Strand B
 * - Internal flowing energy dots (particle system)
 * - Hydrogen bond orbital particle rings in central rungs
 * - Pure white diamond lightning transformation at score 10,000
 */
fun DrawScope.drawVolumetricNeonDnaStructure(
    rotationAngle: Float,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    isPureWhite: Boolean,
    isShadowGripped: Boolean,
    isDarkTrack: Boolean,
    haloPulse: Float,
    particlePhase: Float,
    gripProgress: Float,
    glitchPhase: Float
) {
    val rungsCount = 28
    val radiusX = width * 0.27f
    val dnaHeight = height * 0.84f
    val topY = centerY - dnaHeight / 2f

    // Dynamic Strand Colors
    val strandAColor = when {
        isPureWhite -> Color.White
        isShadowGripped -> Color(0xFFFF0055)
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
        isShadowGripped -> Color(0xFFFF0055)
        isDarkTrack -> Color(0xFFF87171)
        else -> Color(0xFFFCD34D) // Radiant Gold
    }

    val strandANodes = mutableListOf<DnaNode3D>()
    val strandBNodes = mutableListOf<DnaNode3D>()

    for (i in 0 until rungsCount) {
        val y = topY + (i.toFloat() / (rungsCount - 1)) * dnaHeight
        val phase = rotationAngle + (i * 0.38f)

        val sinA = sin(phase)
        val cosA = cos(phase)

        // Subtle glitch offset when dark track has high grip
        val glitchX = if (isDarkTrack && gripProgress > 0.6f && (i % 5 == 0)) {
            sin(glitchPhase * PI.toFloat() * 4f + i) * (gripProgress * 8f)
        } else 0f

        val x1 = centerX + radiusX * cosA + glitchX
        val x2 = centerX - radiusX * cosA - glitchX

        val z1 = sinA // -1 (back) to +1 (front)
        val z2 = -sinA

        val depth1 = (z1 + 1f) / 2f // 0f (back) to 1f (front)
        val depth2 = (z2 + 1f) / 2f

        strandANodes.add(DnaNode3D(x1, y, z1, depth1))
        strandBNodes.add(DnaNode3D(x2, y, z2, depth2))
    }

    // 1. DRAW REAR VOLUMETRIC TUBE SEGMENTS & NODES (Z < 0)
    drawTubeSegments(strandANodes, strandAColor, isPureWhite, onlyRear = true)
    drawTubeSegments(strandBNodes, strandBColor, isPureWhite, onlyRear = true)

    // 2. DRAW BASE PAIR RUNGS & HYDROGEN BOND ORBITALS
    for (i in 0 until rungsCount) {
        val nA = strandANodes[i]
        val nB = strandBNodes[i]

        val rungAlpha = (0.28f + (1f - abs(nA.z)) * 0.68f).coerceIn(0.18f, 1f)
        val rungWidth = if (isPureWhite) 3.5f else 2.2f

        // Outer glow on rung
        drawLine(
            color = rungBaseColor.copy(alpha = rungAlpha * 0.35f),
            start = Offset(nA.x, nA.y),
            end = Offset(nB.x, nB.y),
            strokeWidth = rungWidth * 3.2f,
            cap = StrokeCap.Round
        )

        // Core solid rung
        drawLine(
            color = rungBaseColor.copy(alpha = rungAlpha * 0.95f),
            start = Offset(nA.x, nA.y),
            end = Offset(nB.x, nB.y),
            strokeWidth = rungWidth,
            cap = StrokeCap.Round
        )

        // Center Hydrogen Bond orbital nodes
        val bondAlpha = if (isPureWhite) 1f else 0.85f
        drawCircle(
            color = Color.White.copy(alpha = bondAlpha),
            radius = if (isPureWhite) 3.8f else 2.4f,
            center = Offset(centerX, nA.y)
        )

        // Orbital particle rings on every 3rd rung
        if (i % 3 == 0) {
            val ringRadius = (12f * (1f - abs(nA.z) * 0.5f)).coerceAtLeast(4f)
            drawCircle(
                color = rungBaseColor.copy(alpha = rungAlpha * 0.6f),
                radius = ringRadius,
                center = Offset(centerX, nA.y),
                style = Stroke(width = 1.2f)
            )
        }
    }

    // 3. DRAW FRONT VOLUMETRIC TUBE SEGMENTS (Z >= 0)
    drawTubeSegments(strandANodes, strandAColor, isPureWhite, onlyRear = false)
    drawTubeSegments(strandBNodes, strandBColor, isPureWhite, onlyRear = false)

    // 4. DRAW 3D SPHERICAL NODES WITH MULTI-PASS SPECULAR BLOOM
    for (i in 0 until rungsCount) {
        val nA = strandANodes[i]
        val nB = strandBNodes[i]

        drawVolumetricNode(nA.x, nA.y, nA.depth, strandAColor, isPureWhite)
        drawVolumetricNode(nB.x, nB.y, nB.depth, strandBColor, isPureWhite)
    }

    // 5. INTERNAL ENERGY PARTICLE SYSTEM FLOWING INSIDE THE DNA
    val particleCount = 32
    for (p in 0 until particleCount) {
        val t = ((particlePhase + (p.toFloat() / particleCount)) % 1f)
        val continuousIdx = t * (rungsCount - 1)
        val idxFloor = continuousIdx.toInt().coerceIn(0, rungsCount - 2)
        val fraction = continuousIdx - idxFloor

        // Alternate particle between Strand A and Strand B
        val nodes = if (p % 2 == 0) strandANodes else strandBNodes
        val color = if (p % 2 == 0) strandAColor else strandBColor

        val n1 = nodes[idxFloor]
        val n2 = nodes[idxFloor + 1]

        val px = n1.x + (n2.x - n1.x) * fraction
        val py = n1.y + (n2.y - n1.y) * fraction
        val pDepth = n1.depth + (n2.depth - n1.depth) * fraction

        val pRadius = (2f + pDepth * 3.5f) * (if (isPureWhite) 1.3f else 1f)
        val pAlpha = (0.45f + pDepth * 0.55f).coerceIn(0.2f, 1f)

        // Outer glow
        drawCircle(
            color = color.copy(alpha = pAlpha * 0.5f),
            radius = pRadius * 2.2f,
            center = Offset(px, py)
        )
        // White-hot core
        drawCircle(
            color = Color.White.copy(alpha = pAlpha * 0.95f),
            radius = pRadius,
            center = Offset(px, py)
        )
    }
}

/**
 * Helper to represent 3D coordinates for depth sorting
 */
data class DnaNode3D(val x: Float, val y: Float, val z: Float, val depth: Float)

/**
 * Draws continuous volumetric cylindrical tubes between consecutive 3D nodes
 */
private fun DrawScope.drawTubeSegments(
    nodes: List<DnaNode3D>,
    color: Color,
    isPureWhite: Boolean,
    onlyRear: Boolean
) {
    for (i in 0 until nodes.size - 1) {
        val n1 = nodes[i]
        val n2 = nodes[i + 1]
        val avgZ = (n1.z + n2.z) / 2f

        if (onlyRear && avgZ >= 0f) continue
        if (!onlyRear && avgZ < 0f) continue

        val avgDepth = (n1.depth + n2.depth) / 2f
        val tubeAlpha = (0.35f + avgDepth * 0.65f).coerceIn(0.25f, 1f)

        val baseWidth = (4.5f + avgDepth * 4.5f) * (if (isPureWhite) 1.25f else 1f)

        // 1. Diffuse Glow Pass
        drawLine(
            color = color.copy(alpha = tubeAlpha * 0.30f),
            start = Offset(n1.x, n1.y),
            end = Offset(n2.x, n2.y),
            strokeWidth = baseWidth * 2.8f,
            cap = StrokeCap.Round
        )

        // 2. Volumetric Tube Core
        drawLine(
            color = color.copy(alpha = tubeAlpha * 0.90f),
            start = Offset(n1.x, n1.y),
            end = Offset(n2.x, n2.y),
            strokeWidth = baseWidth,
            cap = StrokeCap.Round
        )

        // 3. Specular Highlight Core
        if (avgDepth > 0.45f || isPureWhite) {
            drawLine(
                color = Color.White.copy(alpha = tubeAlpha * 0.85f),
                start = Offset(n1.x, n1.y),
                end = Offset(n2.x, n2.y),
                strokeWidth = baseWidth * 0.38f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Draws a 3D spherical volumetric node with multi-pass specular glint
 */
private fun DrawScope.drawVolumetricNode(
    x: Float,
    y: Float,
    depth: Float,
    color: Color,
    isPureWhite: Boolean
) {
    val radius = (4.5f + depth * 5f) * (if (isPureWhite) 1.25f else 1f)
    val alpha = (0.40f + depth * 0.60f).coerceIn(0.25f, 1f)

    // Outer diffuse halo
    drawCircle(
        color = color.copy(alpha = alpha * 0.35f),
        radius = radius * 2.4f,
        center = Offset(x, y)
    )

    // Saturated neon body
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = Offset(x, y)
    )

    // Specular highlight gleam
    if (depth > 0.4f || isPureWhite) {
        val glintOffset = Offset(x - radius * 0.28f, y - radius * 0.28f)
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.95f),
            radius = radius * 0.42f,
            center = glintOffset
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
