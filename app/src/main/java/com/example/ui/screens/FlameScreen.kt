package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FlameScreen(
    viewModel: IndividualViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val flameScore by viewModel.flameScore.collectAsStateWithLifecycle()
    val flameLogs by viewModel.flameLogs.collectAsStateWithLifecycle()
    val stage = getFlameStage(flameScore)
    val colors = getFlameColors(flameScore)

    var showTestControls by remember { mutableStateOf(false) }

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
    }

    val stageSub = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> stringResource(R.string.flame_stage_frozen_sub)
        FlameVisualStage.ICE_CRACKING -> stringResource(R.string.flame_stage_cracking_sub)
        FlameVisualStage.COLOR_GRADIENT -> stringResource(R.string.flame_stage_gradient_sub)
        FlameVisualStage.FULLY_IGNITED -> stringResource(R.string.flame_stage_ignited_sub)
    }

    val stageBadgeColor = when (stage) {
        FlameVisualStage.FROZEN_STATIC -> Color(0xFF38BDF8)
        FlameVisualStage.ICE_CRACKING -> Color(0xFF00E5FF)
        FlameVisualStage.COLOR_GRADIENT -> colors.second
        FlameVisualStage.FULLY_IGNITED -> Color(0xFFEF4444)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Navigation Header
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
                            onClick = onNavigateBack,
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

                    // Test mode toggle
                    IconButton(
                        onClick = { showTestControls = !showTestControls },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (showTestControls) NeonCyan.copy(alpha = 0.2f) else Color(0x15FFFFFF))
                            .border(1.dp, if (showTestControls) NeonCyan else Color(0x33FFFFFF), CircleShape)
                            .testTag("flame_test_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Test controls",
                            tint = if (showTestControls) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Test Controls Drawer (for interactive grading and verification)
        if (showTestControls) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Interactive Score Simulator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeonCyan
                            )
                            Text(
                                text = "${flameScore.toInt()} pts",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = stageBadgeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = flameScore.toFloat().coerceIn(0f, 400f),
                            onValueChange = { viewModel.setFlameScore(it.toDouble()) },
                            valueRange = 0f..400f,
                            colors = SliderDefaults.colors(
                                thumbColor = stageBadgeColor,
                                activeTrackColor = stageBadgeColor,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.testTag("flame_score_slider")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Pair("0 (Frozen)", 0.0),
                                Pair("45 (Cracking)", 45.0),
                                Pair("180 (Gradient)", 180.0),
                                Pair("350 (Inferno)", 350.0)
                            ).forEach { (label, value) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x221E293B))
                                        .border(1.dp, Color(0x4038BDF8), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setFlameScore(value) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (flameScore == value) NeonCyan else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Hero Flame Stage Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
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
                                imageVector = if (stage == FlameVisualStage.FROZEN_STATIC) Icons.Default.AcUnit else Icons.Default.LocalFireDepartment,
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

                    // Animated Flame Canvas
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .testTag("flame_hero_canvas"),
                        contentAlignment = Alignment.Center
                    ) {
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // Segmented Range Tier Bar (0-30, 31-60, 61-300, >300)
                    FlameTierBar(
                        currentScore = flameScore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Manual Control Buttons ("Deflame" & "Girl")
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
                            .clickable { viewModel.deflame() }
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
                            .clickable { viewModel.girl() }
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

        // 4. Calculation & Event Logic Rules Summary
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
                            Icons.Default.Shield,
                            stringResource(R.string.rule_retroactive_title),
                            stringResource(R.string.rule_retroactive_desc)
                        ),
                        Triple(
                            Icons.Default.LocalFireDepartment,
                            stringResource(R.string.rule_strike_title),
                            stringResource(R.string.rule_strike_desc)
                        )
                    )

                    rules.forEachIndexed { index, (icon, title, desc) ->
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

        // 5. Activity Log / Event Timeline
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.flame_activity_log_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${flameLogs.size} events",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (flameLogs.isEmpty()) {
                        Text(
                            text = stringResource(R.string.flame_activity_empty),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        flameLogs.take(15).forEach { entry ->
                            FlameLogItemRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlameTierBar(
    currentScore: Double,
    modifier: Modifier = Modifier
) {
    val tiers = listOf(
        Pair("0 - 30", Color(0xFF38BDF8)),
        Pair("31 - 60", Color(0xFF00E5FF)),
        Pair("61 - 300", Color(0xFFE11D48)),
        Pair("> 300", Color(0xFFF97316))
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tiers.forEachIndexed { idx, (label, tierColor) ->
                val isCurrent = when (idx) {
                    0 -> currentScore <= 30.0
                    1 -> currentScore in 30.001..60.0
                    2 -> currentScore in 60.001..300.0
                    else -> currentScore > 300.0
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
                    else -> currentScore > 300.0
                }
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                    color = if (isCurrent) tierColor else Color(0x80FFFFFF)
                )
            }
        }
    }
}

@Composable
private fun FlameLogItemRow(entry: FlameLogEntry) {
    val timeStr = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
    }

    val isPositive = entry.change > 0
    val changeColor = if (isPositive) Color(0xFFF97316) else if (entry.change < 0) Color(0xFF38BDF8) else Color.Gray
    val sign = if (isPositive) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = timeStr,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(changeColor.copy(alpha = 0.16f))
                .border(1.dp, changeColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$sign${String.format(Locale.US, "%.1f", entry.change)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = changeColor
            )
        }
    }
}
