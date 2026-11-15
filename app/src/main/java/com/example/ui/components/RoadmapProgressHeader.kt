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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TaskAlt
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
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import java.text.NumberFormat
import java.util.Locale

/**
 * Top Frosted Liquid Glass Roadmap Progress Header Card ("وعاء تقدم المسار").
 * Features:
 * - Arabic RTL layout when [isAr] is true, placing the glowing cyan Liquid Glass `?` / Info icon
 *   at the physical left edge (`Alignment.CenterEnd` in RTL / `Alignment.CenterStart` visual guide slot).
 * - Displays `"تقدم المسار: المحطة X / 500"` and XP progress toward 1,000,000 XP.
 * - Tapping the glowing info button opens a dark-mode Liquid Glass XP Guide modal dialog.
 */
@Composable
fun RoadmapProgressHeader(
    currentStationId: Int,
    totalXp: Long,
    isAr: Boolean,
    onJumpToStation: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glowPulse: Float = 0.85f,
    currentLang: String = if (isAr) "ar" else "en"
) {
    var showXpGuideDialog by remember { mutableStateOf(false) }

    val safeStation = currentStationId.coerceIn(0, 500)
    val stagesLeft = (500 - safeStation).coerceAtLeast(0)
    val progressFraction = (safeStation / 500f).coerceIn(0f, 1f)
    val xpFormatted = remember(totalXp) {
        NumberFormat.getNumberInstance(Locale.US).format(totalXp.coerceAtLeast(0L))
    }

    val effectiveLayoutDirection = if (isAr || currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides effectiveLayoutDirection) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xDD141C2E), Color(0xCC0C1220))
                    )
                )
                .border(
                    width = 1.1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NeonAqua.copy(alpha = 0.48f),
                            Color.White.copy(alpha = 0.16f),
                            ElectricMint.copy(alpha = 0.36f)
                        )
                    ),
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
                            .background(NeonAqua.copy(alpha = 0.15f))
                            .border(1.4.dp, NeonAqua.copy(alpha = 0.82f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = NeonAqua,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (currentLang) {
                                "ar" -> "تقدم المسار: المحطة $safeStation / 500"
                                "fr" -> "Progression : Station $safeStation / 500"
                                else -> "Roadmap Progress: LVL $safeStation / 500"
                            },
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (currentLang) {
                                "ar" -> "XP الإجمالي: $xpFormatted / 1,000,000 • متبقي $stagesLeft محطة"
                                "fr" -> "XP Total : $xpFormatted / 1,000,000 • $stagesLeft restantes"
                                else -> "Total XP: $xpFormatted / 1,000,000 • $stagesLeft stages left"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
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
                                            NeonAqua.copy(alpha = 0.28f * glowPulse),
                                            ElectricMint.copy(alpha = 0.08f),
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
                                            NeonAqua.copy(alpha = 0.22f),
                                            Color(0xAA0E182B)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.3.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            NeonAqua.copy(alpha = 0.92f),
                                            ElectricMint.copy(alpha = 0.65f)
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
                                tint = NeonAqua,
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
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceAtLeast(0.02f))
                                .height(4.dp)
                                .background(Brush.horizontalGradient(listOf(NeonAqua, ElectricMint)))
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
                                .background(NeonAqua)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(0.92f + 0.16f * glowPulse)
                                .clip(CircleShape)
                                .background(
                                    if (progressFraction >= 0.99f) ElectricMint
                                    else NeonAqua.copy(alpha = 0.72f * glowPulse)
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
                    HeaderQuickJumpChip(
                        label = when (currentLang) {
                            "ar" -> "موقعي ($safeStation)"
                            "fr" -> "Moi ($safeStation)"
                            else -> "Me ($safeStation)"
                        },
                        icon = Icons.Default.MyLocation,
                        isHighlighted = true,
                        accentColor = NeonAqua,
                        onClick = { onJumpToStation(safeStation.coerceAtLeast(1)) },
                        modifier = Modifier.testTag("btn_jump_me")
                    )
                    HeaderQuickJumpChip(
                        label = if (currentLang == "ar") "م 1" else "LVL 1",
                        onClick = { onJumpToStation(1) }
                    )
                    HeaderQuickJumpChip(
                        label = if (currentLang == "ar") "م 100" else "LVL 100",
                        onClick = { onJumpToStation(100) }
                    )
                    HeaderQuickJumpChip(
                        label = if (currentLang == "ar") "م 250" else "LVL 250",
                        onClick = { onJumpToStation(250) }
                    )
                    HeaderQuickJumpChip(
                        label = if (currentLang == "ar") "م 400" else "LVL 400",
                        onClick = { onJumpToStation(400) }
                    )
                    HeaderQuickJumpChip(
                        label = "500",
                        icon = Icons.Default.EmojiEvents,
                        isHighlighted = false,
                        accentColor = MilestoneGold,
                        isCrown = true,
                        onClick = { onJumpToStation(500) }
                    )
                }
            }
        }
    }

    if (showXpGuideDialog) {
        XpEarningGuideDialog(
            isAr = isAr || currentLang == "ar",
            currentLang = currentLang,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isHighlighted -> accentColor.copy(alpha = 0.16f)
        isCrown -> MilestoneGold.copy(alpha = 0.14f)
        else -> Color(0x99161E31)
    }
    val borderColor = when {
        isHighlighted -> accentColor.copy(alpha = 0.85f)
        isCrown -> MilestoneGold.copy(alpha = 0.55f)
        else -> Color(0x33FFFFFF)
    }
    val textColor = when {
        isHighlighted -> NeonAqua
        isCrown -> MilestoneGold
        else -> Color(0xFFCBD5E1)
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
    onDismiss: () -> Unit
) {
    val effectiveDir = if (isAr || currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

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
                color = Color(0xF00B1222),
                tonalElevation = 12.dp,
                shadowElevation = 24.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF131D34).copy(alpha = 0.94f),
                                    Color(0xFF0A101E).copy(alpha = 0.96f)
                                )
                            )
                        )
                        .border(
                            width = 1.3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    NeonAqua.copy(alpha = 0.72f),
                                    Color.White.copy(alpha = 0.18f),
                                    ElectricMint.copy(alpha = 0.58f)
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
                                        .background(NeonAqua.copy(alpha = 0.16f))
                                        .border(1.2.dp, NeonAqua.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = NeonAqua,
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
                                        color = Color.White
                                    )
                                    Text(
                                        text = when (currentLang) {
                                            "ar" -> "جميع المصادر الفعالة للتقدم في مسار 500 محطة"
                                            "fr" -> "Toutes les sources pour progresser sur 500 stations"
                                            else -> "All active sources to unlock 500 stations"
                                        },
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
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
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // XP Source Rows
                        XpRuleItemRow(
                            icon = Icons.Default.TaskAlt,
                            accentColor = ElectricMint,
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
                            accentColor = NeonAqua,
                            title = when (currentLang) {
                                "ar" -> "كل ضغطة تبريد (Deflame)"
                                "fr" -> "Chaque appui Refroidir (Deflame)"
                                else -> "Each Deflame Button Tap"
                            },
                            badgeText = "+20 XP"
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.Female,
                            accentColor = Color(0xFFFF66B2),
                            title = when (currentLang) {
                                "ar" -> "كل تفعيل (girl)"
                                "fr" -> "Chaque activation (girl)"
                                else -> "Each Girl Activation"
                            },
                            badgeText = "+50 XP"
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.Bolt,
                            accentColor = WarmFireOrange,
                            title = when (currentLang) {
                                "ar" -> "كل سترايك واحد (Strike)"
                                "fr" -> "Chaque Strike enregistré"
                                else -> "Each Single Strike Recorded"
                            },
                            badgeText = "+100 XP"
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.PersonAdd,
                            accentColor = NeonViolet,
                            title = when (currentLang) {
                                "ar" -> "كل إضافة فرد جديد"
                                "fr" -> "Chaque nouvel individu ajouté"
                                else -> "Each New Person Added"
                            },
                            badgeText = "+500 XP"
                        )

                        XpRuleItemRow(
                            icon = Icons.Default.Straighten,
                            accentColor = MilestoneGold,
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
                                .background(NeonAqua.copy(alpha = 0.10f))
                                .border(1.dp, NeonAqua.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = NeonAqua,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = when (currentLang) {
                                    "ar" -> "تُحتسب جميع التسجيلات والنشاطات السابقة بداية من تاريخ بداية العد."
                                    "fr" -> "Tous les enregistrements et actions passés depuis la date de début sont comptabilisés rétroactivement."
                                    else -> "All prior logs and records starting strictly from your tracking start date are credited retroactively."
                                },
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = Color(0xFFE2E8F0),
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
                                containerColor = NeonAqua.copy(alpha = 0.22f),
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.2.dp,
                                Brush.horizontalGradient(listOf(NeonAqua, ElectricMint))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ElectricMint,
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
                    }
                }
            }
        }
    }
}

@Composable
private fun XpRuleItemRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    badgeText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x99161E31))
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
            color = Color(0xFFF1F5F9),
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
