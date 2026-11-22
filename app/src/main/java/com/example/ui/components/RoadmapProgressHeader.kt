package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.TaskPools
import com.example.data.roadmap.XpHistoryEntry
import com.example.data.roadmap.XpSourceCategory
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayGlassSheet
import com.example.ui.theme.DayGlassSurface
import com.example.ui.theme.DayGlassSurfaceElevated
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayRose
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top Frosted Liquid Glass Roadmap Progress Header Card ("وعاء تقدم المسار").
 * Features:
 * - Displays `"تقدم المسار: المحطة X / 700"` and XP progress up to 20,000,000 XP at Level 700.
 * - Adapts with icy crystal frost borders & snow indicator when viewing/unlocking Frozen Levels (501..700).
 * - Tapping the glowing info button opens a Liquid Glass XP Guide modal dialog.
 */
@Composable
fun RoadmapProgressHeader(
    currentStationId: Int,
    totalXp: Long,
    isAr: Boolean,
    onJumpToStation: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glowPulse: Float = 0.85f,
    biomeState: RoadmapBiomeState = roadmapBiomeStateForStation(currentStationId.toFloat().coerceAtLeast(1f)),
    currentLang: String = if (isAr) "ar" else "en",
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    var showXpGuideDialog by remember { mutableStateOf(false) }

    val totalStations = TaskPools.TOTAL_STATIONS_COUNT
    val stageProgress = remember(totalXp) { RoadmapManager.getStageProgress(totalXp) }
    val safeStation = stageProgress.unlockedStationId.coerceIn(0, totalStations)
    val stagesLeft = (totalStations - safeStation).coerceAtLeast(0)
    val progressFraction = stageProgress.progressFraction.coerceIn(0f, 1f)
    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }
    val xpFormatted = remember(totalXp) { nf.format(totalXp.coerceAtLeast(0L)) }
    val stageXpFormatted = remember(stageProgress) { nf.format(stageProgress.currentStageXp) }
    val stageReqFormatted = remember(stageProgress) { nf.format(stageProgress.nextStageRequiredXp) }

    val effectiveLayoutDirection = if (isAr || currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val aquaTone = biomePrimaryAccent(biomeState, isDarkTheme)
    val mintTone = biomeSecondaryAccent(biomeState, isDarkTheme)
    val goldTone = if (isDarkTheme) MilestoneGold else DayOrange
    val headerIcon = when (biomeState.dominantBiome) {
        RoadmapBiome.GLACIAL -> Icons.Default.AcUnit
        RoadmapBiome.ROCKY -> Icons.Default.Landscape
        RoadmapBiome.FOREST -> Icons.Default.Park
        RoadmapBiome.VOLCANIC -> Icons.Default.LocalFireDepartment
    }
    val cardBgBrush = if (isDarkTheme) {
        val top = blendBiomeColor(Color(0xE0092138), Color(0xE01F191C), Color(0xE0082319), Color(0xE02D0A06), biomeState)
        val bot = blendBiomeColor(Color(0xD8051526), Color(0xD8151216), Color(0xD8051710), Color(0xD81C0604), biomeState)
        Brush.verticalGradient(colors = listOf(top, bot))
    } else {
        val bot = blendBiomeColor(Color(0xF5D9F0FE), Color(0xF5F1E5D5), Color(0xF5DCFCE7), Color(0xF5FFE2D6), biomeState)
        Brush.verticalGradient(colors = listOf(Color(0xFAFFFFFF), bot))
    }
    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(
            aquaTone.copy(alpha = if (isDarkTheme) 0.65f else 0.72f),
            mintTone.copy(alpha = if (isDarkTheme) 0.45f else 0.55f),
            goldTone.copy(alpha = 0.36f)
        )
    )
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val subtitleColor = if (isDarkTheme) Color(0xFFCBD5E1) else DayTextSecondary

    CompositionLocalProvider(LocalLayoutDirection provides effectiveLayoutDirection) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cardBgBrush)
                .border(
                    width = 1.3.dp,
                    brush = cardBorderBrush,
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start emblem (Physical Right in Arabic RTL, Left in LTR)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(aquaTone.copy(alpha = 0.16f))
                            .border(1.4.dp, aquaTone.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = aquaTone,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (currentLang) {
                                "ar" -> "تقدم المسار: المحطة $safeStation / $totalStations"
                                "fr" -> "Progression : Station $safeStation / $totalStations"
                                else -> "Roadmap Progress: LVL $safeStation / $totalStations"
                            },
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 15.sp,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("roadmap_header_progress_title")
                        )
                        Text(
                            text = if (safeStation >= totalStations) {
                                when (currentLang) {
                                    "ar" -> "اكتملت $totalStations محطة! • الإجمالي: $xpFormatted XP"
                                    "fr" -> "$totalStations stations débloquées ! • Total : $xpFormatted XP"
                                    else -> "All $totalStations stations unlocked! • Total: $xpFormatted XP"
                                }
                            } else {
                                when (currentLang) {
                                    "ar" -> "المحطة ${stageProgress.nextStationId}: $stageXpFormatted / $stageReqFormatted XP • متبقي $stagesLeft"
                                    "fr" -> "Station ${stageProgress.nextStationId} : $stageXpFormatted / $stageReqFormatted XP • $stagesLeft rest."
                                    else -> "Station ${stageProgress.nextStationId}: $stageXpFormatted / $stageReqFormatted XP • $stagesLeft left"
                                }
                            },
                            fontSize = 11.sp,
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // End slot -> Physical LEFT edge in Arabic RTL!
                    // Glowing cybernetic Liquid Glass Info button (48dp touch target)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .testTag("btn_xp_guide_info")
                            .clickable { showXpGuideDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer soft ambient cyan halo
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .scale(0.95f + 0.08f * glowPulse)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            aquaTone.copy(alpha = 0.28f * glowPulse),
                                            mintTone.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        // Translucent Liquid Glass circle with glowing cyan rim
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            aquaTone.copy(alpha = 0.20f),
                                            if (isDarkTheme) Color(0xAA0E182B) else Color.White
                                        )
                                    )
                                )
                                .border(
                                    width = 1.3.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            aquaTone.copy(alpha = 0.92f),
                                            mintTone.copy(alpha = 0.65f)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = when (currentLang) {
                                    "ar" -> "دليل طرق كسب نقاط XP"
                                    "fr" -> "Guide des sources d'XP"
                                    else -> "XP Earning Guide"
                                },
                                tint = aquaTone,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sleek horizontal glowing progress track with endpoint glow dots
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isDarkTheme) Color(0xFF1E293B) else DayGlassSurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceAtLeast(0.02f))
                                .height(4.dp)
                                .background(Brush.horizontalGradient(listOf(aquaTone, mintTone)))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(0.92f + 0.16f * glowPulse)
                                .clip(CircleShape)
                                .background(aquaTone)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(0.92f + 0.16f * glowPulse)
                                .clip(CircleShape)
                                .background(
                                    if (progressFraction >= 0.99f) mintTone
                                    else aquaTone.copy(alpha = 0.72f * glowPulse)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick-Jump Milestones Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iceAccent = if (isDarkTheme) GlacialNightCyan else GlacialDayDeepBlue
                    val rockAccent = if (isDarkTheme) RockyNightAmber else RockyDayAmber
                    val forestAccent = if (isDarkTheme) ForestNightEmerald else ForestDayEmerald
                    val lavaAccent = if (isDarkTheme) VolcanicNightMagma else VolcanicDayMagma

                    HeaderQuickJumpChip(
                        label = when (currentLang) {
                            "ar" -> "موقعي ($safeStation)"
                            "fr" -> "Moi ($safeStation)"
                            else -> "Me ($safeStation)"
                        },
                        icon = Icons.Default.MyLocation,
                        isHighlighted = true,
                        accentColor = aquaTone,
                        isDarkTheme = isDarkTheme,
                        onClick = { onJumpToStation(safeStation.coerceAtLeast(1)) },
                        modifier = Modifier.testTag("btn_jump_me")
                    )
                    HeaderQuickJumpChip(
                        label = when (currentLang) {
                            "ar" -> "جليد 1"
                            "fr" -> "Glace 1"
                            else -> "1 Ice"
                        },
                        icon = Icons.Default.AcUnit,
                        isHighlighted = biomeState.dominantBiome == RoadmapBiome.GLACIAL,
                        accentColor = iceAccent,
                        isDarkTheme = isDarkTheme,
                        onClick = { onJumpToStation(1) },
                        modifier = Modifier.testTag("btn_jump_1")
                    )
                    HeaderQuickJumpChip(
                        label = when (currentLang) {
                            "ar" -> "صخر 176"
                            "fr" -> "Roche 176"
                            else -> "176 Rock"
                        },
                        icon = Icons.Default.Landscape,
                        isHighlighted = biomeState.dominantBiome == RoadmapBiome.ROCKY,
                        accentColor = rockAccent,
                        isDarkTheme = isDarkTheme,
                        onClick = { onJumpToStation(176) },
                        modifier = Modifier.testTag("btn_jump_176")
                    )
                    HeaderQuickJumpChip(
                        label = when (currentLang) {
                            "ar" -> "غابة 351"
                            "fr" -> "Forêt 351"
                            else -> "351 Forest"
                        },
                        icon = Icons.Default.Park,
                        isHighlighted = biomeState.dominantBiome == RoadmapBiome.FOREST,
                        accentColor = forestAccent,
                        isDarkTheme = isDarkTheme,
                        onClick = { onJumpToStation(351) },
                        modifier = Modifier.testTag("btn_jump_351")
                    )
                    HeaderQuickJumpChip(
                        label = "500",
                        icon = Icons.Default.EmojiEvents,
                        isHighlighted = false,
                        accentColor = goldTone,
                        isCrown = true,
                        isDarkTheme = isDarkTheme,
                        onClick = { onJumpToStation(500) },
                        modifier = Modifier.testTag("btn_jump_500")
                    )
                    HeaderQuickJumpChip(
                        label = when (currentLang) {
                            "ar" -> "كارثة 526"
                            "fr" -> "Volcan 526"
                            else -> "526 Lava"
                        },
                        icon = Icons.Default.LocalFireDepartment,
                        isHighlighted = biomeState.dominantBiome == RoadmapBiome.VOLCANIC && biomeState.lavaSubmersion < 0.85f,
                        accentColor = lavaAccent,
                        isDarkTheme = isDarkTheme,
                        onClick = { onJumpToStation(526) },
                        modifier = Modifier.testTag("btn_jump_526")
                    )
                    HeaderQuickJumpChip(
                        label = "700",
                        icon = Icons.Default.LocalFireDepartment,
                        isHighlighted = biomeState.dominantBiome == RoadmapBiome.VOLCANIC && biomeState.lavaSubmersion >= 0.85f,
                        accentColor = lavaAccent,
                        isCrown = true,
                        isDarkTheme = isDarkTheme,
                        onClick = { onJumpToStation(700) },
                        modifier = Modifier.testTag("btn_jump_700")
                    )
                }
            }
        }
    }

    if (showXpGuideDialog) {
        XpEarningGuideDialog(
            isAr = isAr || currentLang == "ar",
            currentLang = currentLang,
            isDarkTheme = isDarkTheme,
            onDismiss = { showXpGuideDialog = false }
        )
    }
}

@Composable
private fun HeaderQuickJumpChip(
    label: String,
    icon: ImageVector? = null,
    isHighlighted: Boolean = false,
    accentColor: Color = Color(0xFF94A3B8),
    isCrown: Boolean = false,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crownAccent = if (isDarkTheme) MilestoneGold else DayOrange
    val highlightAccent = if (isDarkTheme) NeonAqua else DayCyan
    val bgColor = when {
        isHighlighted -> accentColor.copy(alpha = if (isDarkTheme) 0.16f else 0.12f)
        isCrown -> crownAccent.copy(alpha = 0.14f)
        else -> if (isDarkTheme) Color(0x99161E31) else Color(0xFFF8FAFC)
    }
    val borderColor = when {
        isHighlighted -> accentColor.copy(alpha = 0.85f)
        isCrown -> crownAccent.copy(alpha = 0.55f)
        else -> if (isDarkTheme) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    }
    val textColor = when {
        isHighlighted -> highlightAccent
        isCrown -> crownAccent
        else -> if (isDarkTheme) Color(0xFFCBD5E1) else DayTextSecondary
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isHighlighted || isCrown) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

/**
 * Sleek dark-mode Liquid Glass Modal Dialog explaining every source of XP
 * including Dynamic Tasks, Deflame, Girl, Strike, Person Addition, Size Bonus,
 * and retroactive calculation starting from `trackingStartDate`.
 */
@Composable
fun XpEarningGuideDialog(
    isAr: Boolean,
    currentLang: String = if (isAr) "ar" else "en",
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    onDismiss: () -> Unit
) {
    val effectiveDir = if (isAr || currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val xpHistory by RoadmapManager.xpHistory.collectAsStateWithLifecycle()
    val totalXp by RoadmapManager.totalXp.collectAsStateWithLifecycle()
    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }

    val aquaTone = if (isDarkTheme) NeonAqua else DayCyan
    val mintTone = if (isDarkTheme) ElectricMint else DayEmerald
    val orangeTone = if (isDarkTheme) WarmFireOrange else DayOrange
    val goldTone = if (isDarkTheme) MilestoneGold else DayOrange
    val violetTone = if (isDarkTheme) NeonViolet else DayPurple
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val subColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary
    val dialogSurfaceColor = if (isDarkTheme) Color(0xF00B1222) else DayGlassSheet
    val dialogGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF131D34).copy(alpha = 0.94f),
                Color(0xFF0A101E).copy(alpha = 0.96f)
            )
        )
    } else {
        Brush.verticalGradient(colors = listOf(Color.White, DayGlassBgColor()))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides effectiveDir) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("xp_guide_dialog"),
                shape = RoundedCornerShape(26.dp),
                color = dialogSurfaceColor,
                tonalElevation = 12.dp,
                shadowElevation = 24.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(dialogGradient)
                        .border(
                            width = 1.3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    aquaTone.copy(alpha = 0.72f),
                                    if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color(0xFFCBD5E1),
                                    mintTone.copy(alpha = 0.58f)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Modal Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(aquaTone.copy(alpha = 0.16f))
                                        .border(1.2.dp, aquaTone.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = aquaTone,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = when (currentLang) {
                                            "ar" -> "دليل كسب نقاط الخبرة (XP)"
                                            "fr" -> "Guide des gains d'XP"
                                            else -> "XP Earning Guide"
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontFamily = PlusJakartaSans,
                                        fontSize = 16.sp,
                                        color = titleColor
                                    )
                                    Text(
                                        text = when (currentLang) {
                                            "ar" -> "جميع المصادر الفعالة للتقدم في مسار ${TaskPools.TOTAL_STATIONS_COUNT} محطة (حتى 20M XP)"
                                            "fr" -> "Toutes les sources pour progresser sur ${TaskPools.TOTAL_STATIONS_COUNT} stations (Max 20M XP)"
                                            else -> "All active sources to unlock ${TaskPools.TOTAL_STATIONS_COUNT} stations (Max 20M XP)"
                                        },
                                        fontSize = 11.sp,
                                        color = subColor
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_close_xp_guide_top")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = when (currentLang) {
                                        "ar" -> "إغلاق"
                                        "fr" -> "Fermer"
                                        else -> "Close"
                                    },
                                    tint = if (isDarkTheme) Color(0xFFCBD5E1) else DayTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // XP Source Rows
                        XpRuleItemRow(
                            icon = Icons.Default.TaskAlt,
                            accentColor = mintTone,
                            isDarkTheme = isDarkTheme,
                            title = when (currentLang) {
                                "ar" -> "المهام اليومية والأسبوعية والشهرية والسنوية"
                                "fr" -> "Missions quotidiennes, hebdo, mensuelles et éternelles"
                                else -> "Daily, Weekly, Monthly & Annual Tasks"
                            },
                            badgeText = when (currentLang) {
                                "ar" -> "مكافأة المهمة"
                                "fr" -> "Prime mission"
                                else -> "Task Bounty"
                            }
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.AcUnit,
                            accentColor = aquaTone,
                            isDarkTheme = isDarkTheme,
                            title = when (currentLang) {
                                "ar" -> "كل ضغطة تبريد (Deflame)"
                                "fr" -> "Chaque appui Refroidir (Deflame)"
                                else -> "Each Deflame Button Tap"
                            },
                            badgeText = "+20 XP"
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.Female,
                            accentColor = if (isDarkTheme) Color(0xFFFF66B2) else Color(0xFFDB2777),
                            isDarkTheme = isDarkTheme,
                            title = when (currentLang) {
                                "ar" -> "كل تفعيل (girl)"
                                "fr" -> "Chaque activation (girl)"
                                else -> "Each Girl Activation"
                            },
                            badgeText = "+50 XP"
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.Bolt,
                            accentColor = orangeTone,
                            isDarkTheme = isDarkTheme,
                            title = when (currentLang) {
                                "ar" -> "كل سترايك واحد (Strike)"
                                "fr" -> "Chaque Strike enregistré"
                                else -> "Each Single Strike Recorded"
                            },
                            badgeText = "+100 XP"
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.PersonAdd,
                            accentColor = violetTone,
                            isDarkTheme = isDarkTheme,
                            title = when (currentLang) {
                                "ar" -> "كل إضافة فرد جديد"
                                "fr" -> "Chaque nouvel individu ajouté"
                                else -> "Each New Person Added"
                            },
                            badgeText = "+500 XP"
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.Straighten,
                            accentColor = goldTone,
                            isDarkTheme = isDarkTheme,
                            title = when (currentLang) {
                                "ar" -> "مكافأة الحجم الإضافية (عند إضافة فرد مجموع حجمه Size أكثر من 200)"
                                "fr" -> "Bonus de taille (individu ajouté avec Size > 200)"
                                else -> "Extra Size Bonus (New Person with Total Size > 200)"
                            },
                            badgeText = when (currentLang) {
                                "ar" -> "+1000 XP إضافية"
                                "fr" -> "+1000 XP bonus"
                                else -> "+1000 XP Extra"
                            }
                        )

                        // Retroactive Tracking Start Date Note Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(aquaTone.copy(alpha = 0.10f))
                                .border(1.dp, aquaTone.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = aquaTone,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = when (currentLang) {
                                    "ar" -> "تُحتسب جميع التسجيلات والنشاطات السابقة بداية من تاريخ بداية العد، وتبدأ كل محطة جديدة جمع الـ XP الخاص بها من الصفر."
                                    "fr" -> "Toutes les actions depuis la date de début sont comptabilisées rétroactivement, et chaque station collecte son propre XP à partir de 0."
                                    else -> "All prior records from your tracking start date are credited retroactively, and each stage collects its XP requirement starting from 0."
                                },
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Dismiss CTA
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_dismiss_xp_guide"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) NeonAqua.copy(alpha = 0.22f) else DayCyan,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.2.dp,
                                Brush.horizontalGradient(listOf(aquaTone, mintTone))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDarkTheme) ElectricMint else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (currentLang) {
                                    "ar" -> "حسناً، فهمت"
                                    "fr" -> "Compris"
                                    else -> "Got It"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // XP HISTORY SECTION AT THE BOTTOM OF XP EARNING GUIDE
                        XpHistoryLedgerSection(
                            xpHistory = xpHistory,
                            totalXpFormatted = nf.format(totalXp.coerceAtLeast(0L)),
                            currentLang = currentLang,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayGlassBgColor(): Color = DayGlassSurfaceElevated

@Composable
private fun XpHistoryLedgerSection(
    xpHistory: List<XpHistoryEntry>,
    totalXpFormatted: String,
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val violetTone = if (isDarkTheme) NeonViolet else DayPurple
    val cyanTone = if (isDarkTheme) NeonCyan else DayCyan
    val mintTone = if (isDarkTheme) ElectricMint else DayEmerald
    val sectionBg = if (isDarkTheme) Color(0xA60E172A) else Color(0xFFF1F5F9)
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val subColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(sectionBg)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(violetTone.copy(alpha = 0.45f), cyanTone.copy(alpha = 0.35f))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
            .testTag("xp_history_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(violetTone.copy(alpha = 0.18f))
                        .border(1.dp, violetTone.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = violetTone,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = when (currentLang) {
                            "ar" -> "سجل نقاط الخبرة (XP History)"
                            "fr" -> "Historique XP (XP History)"
                            else -> "XP History"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (currentLang) {
                            "ar" -> "توثيق كل XP مكتسب في اللعبة والجهة المعنية (${xpHistory.size})"
                            "fr" -> "Chaque XP gagné & entité concernée (${xpHistory.size})"
                            else -> "Every XP earned & concerned source (${xpHistory.size})"
                        },
                        fontSize = 10.sp,
                        color = subColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(mintTone.copy(alpha = 0.15f))
                    .border(1.dp, mintTone.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$totalXpFormatted XP",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                    color = mintTone
                )
            }
        }

        if (xpHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkTheme) Color(0x66162033) else DayGlassSurface)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (currentLang) {
                        "ar" -> "لا توجد عمليات XP مسجلة بعد. سيظهر هنا كل XP تجنيه مع الجهة المعنية."
                        "fr" -> "Aucun XP enregistré pour l'instant."
                        else -> "No XP earned recorded yet. Every XP gain and its source will appear here."
                    },
                    fontSize = 11.5.sp,
                    color = subColor
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 250.dp)
                    .testTag("xp_history_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(xpHistory, key = { it.id }) { entry ->
                    XpHistoryEntryRow(
                        entry = entry,
                        formattedTime = dateFmt.format(Date(entry.timestamp)),
                        currentLang = currentLang,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun XpHistoryEntryRow(
    entry: XpHistoryEntry,
    formattedTime: String,
    currentLang: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val mintTone = if (isDarkTheme) ElectricMint else DayEmerald
    val goldTone = if (isDarkTheme) MilestoneGold else DayOrange
    val aquaTone = if (isDarkTheme) NeonAqua else DayCyan
    val orangeTone = if (isDarkTheme) WarmFireOrange else DayOrange
    val violetTone = if (isDarkTheme) NeonViolet else DayPurple
    val cyanTone = if (isDarkTheme) NeonCyan else DayCyan

    val (icon, accent, categoryLabel) = when (entry.category) {
        XpSourceCategory.TASK -> Triple(
            Icons.Default.TaskAlt,
            mintTone,
            when (currentLang) {
                "ar" -> "مهمة"
                "fr" -> "Mission"
                else -> "Task"
            }
        )
        XpSourceCategory.SKIP_KEY -> Triple(
            Icons.Default.VpnKey,
            goldTone,
            when (currentLang) {
                "ar" -> "مفتاح تخطي"
                "fr" -> "Clé"
                else -> "Skip Key"
            }
        )
        XpSourceCategory.DEFLAME -> Triple(
            Icons.Default.AcUnit,
            aquaTone,
            "Deflame"
        )
        XpSourceCategory.GIRL -> Triple(
            Icons.Default.Female,
            if (isDarkTheme) Color(0xFFFF66B2) else Color(0xFFDB2777),
            "Girl"
        )
        XpSourceCategory.STRIKE -> Triple(
            Icons.Default.Bolt,
            orangeTone,
            when (currentLang) {
                "ar" -> "سترايك"
                else -> "Strike"
            }
        )
        XpSourceCategory.PERSON -> Triple(
            Icons.Default.PersonAdd,
            violetTone,
            when (currentLang) {
                "ar" -> "فرد جديد"
                "fr" -> "Individu"
                else -> "Person"
            }
        )
        XpSourceCategory.RETROACTIVE -> Triple(
            Icons.Default.History,
            cyanTone,
            when (currentLang) {
                "ar" -> "مزامنة"
                "fr" -> "Synchro"
                else -> "Sync"
            }
        )
        XpSourceCategory.SYSTEM -> Triple(
            Icons.Default.AutoAwesome,
            if (isDarkTheme) Color(0xFFCBD5E1) else DayTextSecondary,
            when (currentLang) {
                "ar" -> "النظام"
                "fr" -> "Système"
                else -> "System"
            }
        )
    }

    val amountPrefix = if (entry.amount >= 0L) "+" else ""
    val negTone = if (isDarkTheme) Color(0xFFF87171) else DayRose
    val amountColor = if (entry.amount >= 0L) accent else negTone
    val concernedPartyText = entry.getLocalizedConcernedParty(currentLang)
    val detailText = entry.getLocalizedDetail(currentLang)
    val rowBg = if (isDarkTheme) Color(0x88161E31) else DayGlassSurface
    val primaryText = if (isDarkTheme) Color(0xFFF1F5F9) else DayTextPrimary
    val mutedText = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = categoryLabel,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = categoryLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Text(
                    text = formattedTime,
                    fontSize = 9.sp,
                    color = mutedText,
                    maxLines = 1
                )
            }

            Text(
                text = when (currentLang) {
                    "ar" -> "الجهة المعنية: $concernedPartyText"
                    "fr" -> "Entité : $concernedPartyText"
                    else -> "Source: $concernedPartyText"
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (detailText.isNotBlank()) {
                Text(
                    text = detailText,
                    fontSize = 10.sp,
                    color = mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(amountColor.copy(alpha = 0.16f))
                .border(1.dp, amountColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$amountPrefix${entry.amount} XP",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = amountColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun XpRuleItemRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    badgeText: String,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val rowBg = if (isDarkTheme) Color(0x99161E31) else DayGlassSurface
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else DayTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.52f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = title,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accentColor.copy(alpha = 0.18f))
                .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
