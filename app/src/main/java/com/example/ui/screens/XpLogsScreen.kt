package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.XpHistoryEntry
import com.example.data.roadmap.XpSourceCategory
import com.example.ui.components.ElectricMint
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.MilestoneGold
import com.example.ui.components.NeonAqua
import com.example.ui.components.ObsidianRoadmapHubSwitcher
import com.example.ui.components.RoadmapAmbientBackground
import com.example.ui.components.RoadmapHubDestination
import com.example.ui.components.WarmFireOrange
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayGlassSurface
import com.example.ui.theme.DayGlassSurfaceElevated
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayRose
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun XpLogsScreen(
    viewModel: IndividualViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTasksLedger: () -> Unit = {},
    onOpenRoadmap: () -> Unit = onNavigateBack,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val xpHistory by viewModel.xpHistory.collectAsStateWithLifecycle()
    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val stageProgress = remember(totalXp) { RoadmapManager.getStageProgress(totalXp) }

    var selectedCategory by remember { mutableStateOf<XpSourceCategory?>(null) }

    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    val filteredLogs = remember(xpHistory, selectedCategory) {
        if (selectedCategory == null) {
            xpHistory
        } else {
            xpHistory.filter { it.category == selectedCategory }
        }
    }

    val cyanTone = if (isDarkTheme) NeonCyan else DayCyan
    val violetTone = if (isDarkTheme) NeonViolet else DayPurple
    val emeraldTone = if (isDarkTheme) ElectricMint else DayEmerald
    val orangeTone = if (isDarkTheme) WarmFireOrange else DayOrange
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val descColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("xp_logs_screen")
    ) {
        RoadmapAmbientBackground(
            modifier = Modifier.fillMaxSize(),
            darkTheme = isDarkTheme
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // 1. TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color(0xBB161E31) else DayGlassSurface)
                            .border(
                                1.dp,
                                if (isDarkTheme) Color(0x40FFFFFF) else Color(0xFFCBD5E1),
                                CircleShape
                            )
                            .testTag("xp_logs_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isAr) "رجوع" else if (isFr) "Retour" else "Back",
                            tint = titleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = violetTone,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = when {
                                isAr -> "سجل نقاط XP (Xp Logs)"
                                isFr -> "Journaux XP"
                                else -> "Xp Logs"
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("xp_logs_title")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(emeraldTone.copy(alpha = 0.14f))
                        .border(1.dp, emeraldTone.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${nf.format(totalXp.coerceAtLeast(0L))} XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        color = emeraldTone
                    )
                }
            }

            // Unified Hub Switcher (Tasks Ledger <-> LVL X/700 <-> Xp Logs)
            ObsidianRoadmapHubSwitcher(
                activeDestination = RoadmapHubDestination.XP_LOGS,
                unlockedStationId = stageProgress.unlockedStationId,
                progressFraction = stageProgress.progressFraction,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                roadmapAccent = cyanTone,
                onTasksLedgerClick = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onOpenTasksLedger()
                },
                onProgressClick = {
                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onOpenRoadmap()
                },
                onXpLogsClick = {},
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // 2. SUMMARY COUNTERS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                XpLogSummaryCard(
                    value = nf.format(totalXp.coerceAtLeast(0L)),
                    label = when {
                        isAr -> "إجمالي XP المكتسب"
                        isFr -> "Total XP gagné"
                        else -> "Total XP Earned"
                    },
                    accentColor = emeraldTone,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("xp_logs_stat_total")
                )
                XpLogSummaryCard(
                    value = "${xpHistory.size}",
                    label = when {
                        isAr -> "عدد عمليات التحصل"
                        isFr -> "Événements XP"
                        else -> "XP Events Logged"
                    },
                    accentColor = cyanTone,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("xp_logs_stat_count")
                )
            }

            // 3. CATEGORY FILTER CHIPS
            val filters = listOf(
                null to (if (isAr) "الكل" else if (isFr) "Tout" else "All"),
                XpSourceCategory.TASK to (if (isAr) "المهام" else if (isFr) "Missions" else "Tasks"),
                XpSourceCategory.STRIKE to (if (isAr) "السترايك" else "Strikes"),
                XpSourceCategory.PERSON to (if (isAr) "الأفراد" else if (isFr) "Individus" else "People"),
                XpSourceCategory.DEFLAME to "Deflame",
                XpSourceCategory.GIRL to "Girl",
                XpSourceCategory.SKIP_KEY to (if (isAr) "مفاتيح التخطي" else if (isFr) "Clés" else "Skip Keys"),
                XpSourceCategory.RETROACTIVE to (if (isAr) "المزامنة" else if (isFr) "Synchro" else "Sync")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                filters.forEach { (cat, label) ->
                    val isSelected = selectedCategory == cat
                    val chipAccent = when (cat) {
                        XpSourceCategory.TASK -> emeraldTone
                        XpSourceCategory.STRIKE -> orangeTone
                        XpSourceCategory.PERSON -> violetTone
                        XpSourceCategory.DEFLAME -> cyanTone
                        XpSourceCategory.GIRL -> Color(0xFFEC4899)
                        XpSourceCategory.SKIP_KEY -> if (isDarkTheme) MilestoneGold else DayOrange
                        XpSourceCategory.RETROACTIVE -> cyanTone
                        else -> violetTone
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) chipAccent.copy(alpha = if (isDarkTheme) 0.22f else 0.14f)
                                else if (isDarkTheme) Color(0x33162033) else DayGlassSurface
                            )
                            .border(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                color = if (isSelected) chipAccent else if (isDarkTheme) Color(0x26FFFFFF) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                selectedCategory = cat
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) chipAccent else descColor,
                            maxLines = 1
                        )
                    }
                }
            }

            // 4. XP LOG LIST
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredLogs.isEmpty()) {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        darkTheme = isDarkTheme
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = descColor,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when {
                                    isAr -> "لا توجد عمليات XP مسجلة هنا بعد. سيظهر كل XP تحصلت عليه مع سببه هنا."
                                    isFr -> "Aucun journal XP enregistré ici pour l'instant."
                                    else -> "No XP gains logged here yet. Every XP earned and its reason will appear here."
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = descColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("xp_logs_list"),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { entry ->
                            XpLogDetailCard(
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
    }
}

@Composable
private fun XpLogSummaryCard(
    value: String,
    label: String,
    accentColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isDarkTheme) {
        Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.20f), Color(0xCC0D1525)))
    } else {
        Brush.verticalGradient(listOf(Color.White, DayGlassSurfaceElevated))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .border(1.dp, accentColor.copy(alpha = if (isDarkTheme) 0.45f else 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            fontFamily = PlusJakartaSans,
            color = accentColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color(0xFFE2E8F0) else DayTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun XpLogDetailCard(
    entry: XpHistoryEntry,
    formattedTime: String,
    currentLang: String,
    isDarkTheme: Boolean
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val emeraldTone = if (isDarkTheme) ElectricMint else DayEmerald
    val cyanTone = if (isDarkTheme) NeonAqua else DayCyan
    val violetTone = if (isDarkTheme) NeonViolet else DayPurple
    val orangeTone = if (isDarkTheme) WarmFireOrange else DayOrange
    val goldTone = if (isDarkTheme) MilestoneGold else DayOrange
    val negColor = if (isDarkTheme) Color(0xFFF87171) else DayRose

    val (icon, accent, categoryLabel) = when (entry.category) {
        XpSourceCategory.TASK -> Triple(
            Icons.Default.TaskAlt,
            emeraldTone,
            if (isAr) "مهمة" else if (isFr) "Mission" else "Task"
        )
        XpSourceCategory.SKIP_KEY -> Triple(
            Icons.Default.VpnKey,
            goldTone,
            if (isAr) "مفتاح تخطي" else if (isFr) "Clé" else "Skip Key"
        )
        XpSourceCategory.DEFLAME -> Triple(
            Icons.Default.AcUnit,
            cyanTone,
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
            if (isAr) "سترايك" else "Strike"
        )
        XpSourceCategory.PERSON -> Triple(
            Icons.Default.PersonAdd,
            violetTone,
            if (isAr) "فرد جديد" else if (isFr) "Individu" else "Person"
        )
        XpSourceCategory.RETROACTIVE -> Triple(
            Icons.Default.History,
            if (isDarkTheme) NeonCyan else DayCyan,
            if (isAr) "مزامنة" else if (isFr) "Synchro" else "Sync"
        )
        XpSourceCategory.SYSTEM -> Triple(
            Icons.Default.AutoAwesome,
            if (isDarkTheme) Color(0xFFCBD5E1) else DayTextSecondary,
            if (isAr) "النظام" else if (isFr) "Système" else "System"
        )
    }

    val amountPrefix = if (entry.amount >= 0L) "+" else ""
    val amountColor = if (entry.amount >= 0L) accent else negColor
    val sourceText = entry.getLocalizedConcernedParty(currentLang)
    val reasonText = entry.getLocalizedDetail(currentLang)

    val cardBg = if (isDarkTheme) Color(0xAA131C30) else DayGlassSurface
    val titleColor = if (isDarkTheme) Color(0xFFF8FAFC) else DayTextPrimary
    val descColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, accent.copy(alpha = if (isDarkTheme) 0.32f else 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = if (isDarkTheme) 0.16f else 0.12f))
                .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = categoryLabel,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = categoryLabel,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Text(
                    text = formattedTime,
                    fontSize = 9.5.sp,
                    color = descColor,
                    maxLines = 1
                )
            }

            Text(
                text = sourceText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (reasonText.isNotBlank()) {
                Text(
                    text = when {
                        isAr -> "السبب: $reasonText"
                        isFr -> "Raison : $reasonText"
                        else -> "Reason: $reasonText"
                    },
                    fontSize = 11.sp,
                    color = descColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(amountColor.copy(alpha = if (isDarkTheme) 0.16f else 0.12f))
                .border(1.dp, amountColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$amountPrefix${entry.amount} XP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = PlusJakartaSans,
                color = amountColor,
                maxLines = 1
            )
        }
    }
}
