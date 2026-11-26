package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.TaskPools
import com.example.ui.components.RoadmapBiome
import com.example.ui.components.biomePrimaryAccent
import com.example.ui.components.biomeSecondaryAccent
import com.example.ui.components.blendBiomeColor
import com.example.ui.components.roadmapBiomeStateForStation
import com.example.ui.map.MapStyleHelper
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayRose
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalScaffoldContentPadding
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: IndividualViewModel,
    onOpenHeatmap: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenAddStrike: () -> Unit,
    onOpenRoadmap: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val context = LocalContext.current
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val allIndividuals by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val allStrikeEvents by viewModel.allStrikeEvents.collectAsStateWithLifecycle()
    val allTimeStrikesCount by viewModel.allTimeStrikesCount.collectAsStateWithLifecycle()

    val totalAllTimeStrikes = maxOf(allTimeStrikesCount, allIndividuals.sumOf { it.strikeCount })
    val mappedPointsCount = remember(allIndividuals, allStrikeEvents) {
        MapStyleHelper.collectAllMappedPoints(allIndividuals, allStrikeEvents).size
    }

    val stageProgress = remember(totalXp) { RoadmapManager.getStageProgress(totalXp) }
    val totalStations = TaskPools.TOTAL_STATIONS_COUNT
    val safeStation = stageProgress.unlockedStationId.coerceIn(0, totalStations)
    val biomeState = remember(safeStation) {
        roadmapBiomeStateForStation(safeStation.toFloat().coerceAtLeast(1f))
    }
    val animatedProgress by animateFloatAsState(
        targetValue = stageProgress.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "home_roadmap_xp_progress"
    )

    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }
    val xpFormatted = remember(totalXp) { nf.format(totalXp.coerceAtLeast(0L)) }
    val stageXpFormatted = remember(stageProgress) { nf.format(stageProgress.currentStageXp) }
    val stageReqFormatted = remember(stageProgress) { nf.format(stageProgress.nextStageRequiredXp) }

    val biomePrimary = biomePrimaryAccent(biomeState, isDarkTheme)
    val biomeSecondary = biomeSecondaryAccent(biomeState, isDarkTheme)
    val cyanAccent = if (isDarkTheme) NeonCyan else DayCyan
    val emeraldAccent = if (isDarkTheme) NeonEmerald else DayEmerald
    val amberAccent = if (isDarkTheme) NeonAmber else DayOrange
    val roseAccent = if (isDarkTheme) NeonRose else DayRose

    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val secondaryColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    val biomeIcon = when (biomeState.dominantBiome) {
        RoadmapBiome.GLACIAL -> Icons.Default.AcUnit
        RoadmapBiome.ROCKY -> Icons.Default.Landscape
        RoadmapBiome.FOREST -> Icons.Default.Park
        RoadmapBiome.VOLCANIC -> Icons.Default.LocalFireDepartment
    }
    val biomeName = when (biomeState.dominantBiome) {
        RoadmapBiome.GLACIAL -> when {
            isAr -> "القمم الجليدية"
            isFr -> "Sommets Glaciaux"
            else -> "Glacial Peaks"
        }
        RoadmapBiome.ROCKY -> when {
            isAr -> "المرتفعات الصخرية"
            isFr -> "Crêtes Rocheuses"
            else -> "Rocky Ridge"
        }
        RoadmapBiome.FOREST -> when {
            isAr -> "الغابة الكثيفة"
            isFr -> "Forêt Dense"
            else -> "Emerald Forest"
        }
        RoadmapBiome.VOLCANIC -> when {
            isAr -> "القلب البركاني"
            isFr -> "Cœur Volcanique"
            else -> "Volcanic Core"
        }
    }

    val scaffoldPadding = LocalScaffoldContentPadding.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = scaffoldPadding.calculateTopPadding() + 14.dp,
                bottom = scaffoldPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ROADMAP LEVEL CARD (Directly below Top Bar)
            item {
                val roadmapBg = if (isDarkTheme) {
                    val top = blendBiomeColor(Color(0xE80A2239), Color(0xE821191D), Color(0xE809251A), Color(0xE82D0B07), biomeState)
                    val bot = blendBiomeColor(Color(0xDC051424), Color(0xDC141015), Color(0xDC051610), Color(0xDC1A0604), biomeState)
                    Brush.verticalGradient(listOf(top, bot))
                } else {
                    val bot = blendBiomeColor(Color(0xF5D9F0FE), Color(0xF5F1E5D5), Color(0xF5DCFCE7), Color(0xF5FFE2D6), biomeState)
                    Brush.verticalGradient(listOf(Color(0xFCFFFFFF), bot))
                }
                val roadmapBorder = Brush.linearGradient(
                    listOf(
                        biomePrimary.copy(alpha = if (isDarkTheme) 0.72f else 0.80f),
                        biomeSecondary.copy(alpha = if (isDarkTheme) 0.45f else 0.55f),
                        amberAccent.copy(alpha = 0.38f)
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(roadmapBg)
                        .border(1.3.dp, roadmapBorder, RoundedCornerShape(24.dp))
                        .clickable {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            onOpenRoadmap()
                        }
                        .testTag("home_roadmap_level_card")
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    biomePrimary.copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.88f, size.height * 0.18f),
                                radius = size.width * 0.48f
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.18f),
                            radius = size.width * 0.48f
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(biomePrimary.copy(alpha = if (isDarkTheme) 0.20f else 0.14f))
                                        .border(1.4.dp, biomePrimary.copy(alpha = 0.85f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = biomeIcon,
                                        contentDescription = null,
                                        tint = biomePrimary,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = when {
                                            isAr -> "مستوى الـ Roadmap"
                                            isFr -> "Niveau Roadmap"
                                            else -> "Roadmap Level"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "LVL $safeStation / $totalStations",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = PlusJakartaSans,
                                        color = titleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("home_roadmap_level_badge")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(biomePrimary.copy(alpha = if (isDarkTheme) 0.18f else 0.13f))
                                    .border(1.dp, biomePrimary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = biomePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = biomeName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = biomePrimary,
                                    maxLines = 1
                                )
                            }
                        }

                        // Glowing XP Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isDarkTheme) Color(0x451E293B) else Color(0xFFE2E8F0)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress.coerceAtLeast(0.03f))
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(biomePrimary, biomeSecondary, amberAccent)
                                            )
                                        )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (safeStation >= totalStations) {
                                        when {
                                            isAr -> "اكتملت كل المحطات"
                                            isFr -> "Toutes les stations débloquées"
                                            else -> "All Stations Unlocked"
                                        }
                                    } else {
                                        when {
                                            isAr -> "التالي ${stageProgress.nextStationId}: $stageXpFormatted / $stageReqFormatted XP"
                                            isFr -> "Suivante ${stageProgress.nextStationId} : $stageXpFormatted / $stageReqFormatted XP"
                                            else -> "Next LVL ${stageProgress.nextStationId}: $stageXpFormatted / $stageReqFormatted XP"
                                        }
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$xpFormatted XP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = PlusJakartaSans,
                                    color = biomePrimary
                                )
                            }
                        }
                    }
                }
            }

            // 2. HEATMAP CARD + RECORDS BUTTON SIDE BY SIDE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // HEATMAP TILE -> opens the Heatmap map directly
                    val heatmapBg = if (isDarkTheme) {
                        Brush.verticalGradient(listOf(Color(0xE80A192F), Color(0xE0060D1D)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFCFFFFFF), Color(0xF5E0F2FE)))
                    }
                    val heatmapBorder = Brush.linearGradient(
                        listOf(
                            cyanAccent.copy(alpha = 0.80f),
                            roseAccent.copy(alpha = 0.50f)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(178.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(heatmapBg)
                            .border(1.3.dp, heatmapBorder, RoundedCornerShape(24.dp))
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                onOpenHeatmap()
                            }
                            .testTag("home_heatmap_card")
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val radarCenter = Offset(size.width * 0.78f, size.height * 0.24f)
                            drawCircle(
                                color = cyanAccent.copy(alpha = if (isDarkTheme) 0.14f else 0.12f),
                                radius = size.minDimension * 0.34f,
                                center = radarCenter,
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                            drawCircle(
                                color = roseAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.14f),
                                radius = size.minDimension * 0.20f,
                                center = radarCenter,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        roseAccent.copy(alpha = if (isDarkTheme) 0.34f else 0.22f),
                                        Color.Transparent
                                    ),
                                    center = radarCenter,
                                    radius = size.minDimension * 0.36f
                                ),
                                radius = size.minDimension * 0.36f,
                                center = radarCenter
                            )
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 4.dp.toPx(),
                                center = radarCenter
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(15.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cyanAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.13f))
                                    .border(1.dp, cyanAccent.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Heatmap",
                                    tint = cyanAccent,
                                    modifier = Modifier.size(23.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "Heatmap",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isAr -> "الخريطة الحرارية"
                                        isFr -> "Carte thermique"
                                        else -> "Thermal Strike Map"
                                    },
                                    fontSize = 11.5.sp,
                                    color = secondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cyanAccent.copy(alpha = if (isDarkTheme) 0.14f else 0.11f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = roseAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = when {
                                            isAr -> "$mappedPointsCount موقع"
                                            isFr -> "$mappedPointsCount points"
                                            else -> "$mappedPointsCount mapped"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cyanAccent,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = cyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // RECORDS BUTTON -> opens moved old Records (DashboardScreen) page
                    val recordsBg = if (isDarkTheme) {
                        Brush.verticalGradient(listOf(Color(0xE80A1F24), Color(0xE0061018)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFCFFFFFF), Color(0xF5DCFCE7)))
                    }
                    val recordsBorder = Brush.linearGradient(
                        listOf(
                            emeraldAccent.copy(alpha = 0.80f),
                            cyanAccent.copy(alpha = 0.50f)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(178.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(recordsBg)
                            .border(1.3.dp, recordsBorder, RoundedCornerShape(24.dp))
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                onOpenRecords()
                            }
                            .testTag("home_records_btn")
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val startX = size.width * 0.64f
                            val baseY = size.height * 0.34f
                            listOf(12f, 22f, 17f).forEachIndexed { idx, h ->
                                drawLine(
                                    color = emeraldAccent.copy(alpha = if (isDarkTheme) 0.24f else 0.18f),
                                    start = Offset(startX + (idx * 11.dp.toPx()), baseY),
                                    end = Offset(startX + (idx * 11.dp.toPx()), baseY - h.dp.toPx()),
                                    strokeWidth = 4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(15.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(emeraldAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.13f))
                                    .border(1.dp, emeraldAccent.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListNumbered,
                                    contentDescription = "Records",
                                    tint = emeraldAccent,
                                    modifier = Modifier.size(23.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "Records",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isAr -> "سجلات الأفراد"
                                        isFr -> "Registres complets"
                                        else -> "Individuals & Logs"
                                    },
                                    fontSize = 11.5.sp,
                                    color = secondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(emeraldAccent.copy(alpha = if (isDarkTheme) 0.14f else 0.11f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when {
                                        isAr -> "${allIndividuals.size} سجل"
                                        isFr -> "${allIndividuals.size} fiches"
                                        else -> "${allIndividuals.size} records"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = emeraldAccent,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = emeraldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. ADD STRIKE BUTTON (Directly below Heatmap + Records -> opens moved StrikeScreen)
            item {
                val strikeBg = if (isDarkTheme) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xEC2B0E17),
                            Color(0xEC1B1020),
                            Color(0xEC24120B)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFCFFFFFF),
                            Color(0xF5FFE4E6),
                            Color(0xF5FFEDD5)
                        )
                    )
                }
                val strikeBorder = Brush.linearGradient(
                    listOf(
                        roseAccent.copy(alpha = if (isDarkTheme) 0.85f else 0.78f),
                        amberAccent.copy(alpha = if (isDarkTheme) 0.80f else 0.72f)
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(strikeBg)
                        .border(1.4.dp, strikeBorder, RoundedCornerShape(24.dp))
                        .clickable {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            onOpenAddStrike()
                        }
                        .testTag("home_add_strike_btn")
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    amberAccent.copy(alpha = if (isDarkTheme) 0.24f else 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.14f, size.height * 0.5f),
                                radius = size.height * 0.95f
                            ),
                            radius = size.height * 0.95f,
                            center = Offset(size.width * 0.14f, size.height * 0.5f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                amberAccent.copy(alpha = if (isDarkTheme) 0.26f else 0.18f),
                                                roseAccent.copy(alpha = if (isDarkTheme) 0.24f else 0.16f)
                                            )
                                        )
                                    )
                                    .border(1.5.dp, amberAccent.copy(alpha = 0.9f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Add Strike",
                                    tint = amberAccent,
                                    modifier = Modifier.size(27.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "Add Strike",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isAr -> "تسجيل وإدارة الضربات • ${nf.format(totalAllTimeStrikes)} إجمالي الضربات"
                                        isFr -> "Console Strike • ${nf.format(totalAllTimeStrikes)} strikes"
                                        else -> "Open Strike Console • ${nf.format(totalAllTimeStrikes)} total strikes"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = secondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(amberAccent.copy(alpha = if (isDarkTheme) 0.20f else 0.14f))
                                .border(1.dp, amberAccent.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = amberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
