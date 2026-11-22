package com.example.ui.screens

import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.ui.components.DynamicAnimatedProfileCard
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SmartMetricComparisonBar
import com.example.ui.components.StrikerStatus
import com.example.ui.components.StrikerStatusBadge
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.DayTextSecondary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.util.HapticHelper
import com.example.util.SmartSearchMatcher

@Composable
fun ComparisonScreen(
    viewModel: IndividualViewModel,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val allList by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val personA by viewModel.personA.collectAsStateWithLifecycle()
    val personB by viewModel.personB.collectAsStateWithLifecycle()
    val lastStrikeMap by viewModel.individualLastStrikeMap.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isHapticEnabled by viewModel.isHapticFeedbackEnabled
    val context = LocalContext.current
    val view = LocalView.current
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentViolet = if (isDarkTheme) NeonViolet else DayPurple
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val accentEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val selectableList = allList

    // null = closed, 1 = Person A picker, 2 = Person B picker
    var pickerTargetSlot by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Person Selectors & Matchup Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector Person A
                Box(modifier = Modifier.weight(1f)) {
                    PersonSelectorCard(
                        label = stringResource(R.string.select_person_a),
                        person = personA,
                        status = personA?.let { viewModel.getStatusForIndividual(it, lastStrikeMap) },
                        accentColor = accentCyan,
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            pickerTargetSlot = 1
                        },
                        testTag = "selector_person_a",
                        isAr = isAr,
                        isDarkTheme = isDarkTheme
                    )
                }

                // Swap Button (Raw arrow icon without circular background or border)
                IconButton(
                    onClick = { viewModel.swapComparisonPersons() },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(44.dp)
                        .testTag("swap_persons_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.swap),
                        tint = accentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Selector Person B
                Box(modifier = Modifier.weight(1f)) {
                    PersonSelectorCard(
                        label = stringResource(R.string.select_person_b),
                        person = personB,
                        status = personB?.let { viewModel.getStatusForIndividual(it, lastStrikeMap) },
                        accentColor = accentViolet,
                        onClick = {
                            HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                            pickerTargetSlot = 2
                        },
                        testTag = "selector_person_b",
                        isAr = isAr,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }

        if (personA != null && personB != null) {
            val a = personA!!
            val b = personB!!

            // Overall Summary Card
            item {
                val aWins = (if (a.totalScore > b.totalScore) 1 else 0) +
                        (if (a.boost > b.boost) 1 else 0) +
                        (if (a.size > b.size) 1 else 0) +
                        (if (a.strikeCount > b.strikeCount) 1 else 0) +
                        (if (a.sScore > b.sScore) 1 else 0) +
                        (if (a.fScore > b.fScore) 1 else 0) +
                        (if (a.cScore > b.cScore) 1 else 0)

                val bWins = (if (b.totalScore > a.totalScore) 1 else 0) +
                        (if (b.boost > a.boost) 1 else 0) +
                        (if (b.size > a.size) 1 else 0) +
                        (if (b.strikeCount > a.strikeCount) 1 else 0) +
                        (if (b.sScore > a.sScore) 1 else 0) +
                        (if (b.fScore > a.fScore) 1 else 0) +
                        (if (b.cScore > a.cScore) 1 else 0)

                val leadsWithStr = stringResource(R.string.leads_with)
                val categoriesStr = stringResource(R.string.categories)
                val perfectBalanceStr = stringResource(R.string.perfect_balance)

                val leaderText = if (aWins > bWins) {
                    "${a.fullName} $leadsWithStr $aWins $categoriesStr"
                } else if (bWins > aWins) {
                    "${b.fullName} $leadsWithStr $bWins $categoriesStr"
                } else {
                    "$perfectBalanceStr ($aWins — $bWins)"
                }

                val leaderColor = if (aWins > bWins) accentCyan else if (bWins > aWins) accentViolet else accentAmber

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    darkTheme = isDarkTheme
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = leaderColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.analytical_verdict),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = leaderText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = leaderColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accentCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${a.fullName} ($aWins)",
                                    fontSize = 12.sp,
                                    color = accentCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accentViolet)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${b.fullName} ($bWins)",
                                    fontSize = 12.sp,
                                    color = accentViolet,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Analytical Metric Comparisons List
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    darkTheme = isDarkTheme
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isAr) "تفصيل المؤشرات والمقاييس" else "METRIC BREAKDOWN",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        // 0. TOTAL SCORE (BOOST × Strikes)
                        SmartMetricComparisonBar(
                            title = if (isAr) "النتيجة الإجمالية (التعزيز × الضربات)" else "TOTAL SCORE (BOOST × Strikes)",
                            valueA = a.totalScore,
                            valueB = b.totalScore,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8),
                            darkTheme = isDarkTheme
                        )

                        // 1. BOOST
                        SmartMetricComparisonBar(
                            title = if (isAr) "التعزيز (المعادلة الديناميكية)" else "BOOST (Dynamic Formula)",
                            valueA = a.boost,
                            valueB = b.boost,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8),
                            darkTheme = isDarkTheme
                        )

                        // 2. Size (Length * Width)
                        SmartMetricComparisonBar(
                            title = if (isAr) "الحجم (الطول × العرض)" else "Size (Length × Width)",
                            valueA = a.size,
                            valueB = b.size,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8),
                            darkTheme = isDarkTheme
                        )

                        // 3. Strike Count
                        SmartMetricComparisonBar(
                            title = if (isAr) "عدد الضربات" else "Strike Count",
                            valueA = a.strikeCount.toDouble(),
                            valueB = b.strikeCount.toDouble(),
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8),
                            darkTheme = isDarkTheme
                        )

                        // 4. S SCORE
                        SmartMetricComparisonBar(
                            title = if (isAr) "نقاط S" else "S SCORE",
                            valueA = a.sScore,
                            valueB = b.sScore,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8),
                            darkTheme = isDarkTheme
                        )

                        // 5. F SCORE
                        SmartMetricComparisonBar(
                            title = if (isAr) "نقاط F" else "F SCORE",
                            valueA = a.fScore,
                            valueB = b.fScore,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8),
                            darkTheme = isDarkTheme
                        )

                        // 6. C SCORE
                        SmartMetricComparisonBar(
                            title = if (isAr) "نقاط C" else "C SCORE",
                            valueA = a.cScore,
                            valueB = b.cScore,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8),
                            darkTheme = isDarkTheme
                        )
                    }
                }
            }

            // Side-by-Side Quick Strike Action Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Strike A
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.incrementStrikeForId(a.id, 1) },
                        shape = RoundedCornerShape(16.dp),
                        darkTheme = isDarkTheme
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isAr) "ضربة ${a.fullName.take(8)}" else "STRIKE ${a.fullName.take(8)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = accentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+1",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentCyan
                                )
                            }
                        }
                    }

                    // Strike B
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.incrementStrikeForId(b.id, 1) },
                        shape = RoundedCornerShape(16.dp),
                        darkTheme = isDarkTheme
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isAr) "ضربة ${b.fullName.take(8)}" else "STRIKE ${b.fullName.take(8)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentViolet
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = accentViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+1",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentViolet
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    darkTheme = isDarkTheme
                ) {
                    Text(
                        text = if (isAr) {
                            "يرجى إضافة شخصين على الأقل لإجراء المقارنة التحليلية."
                        } else {
                            "Please add at least two individuals to perform analytical comparison."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    val activeSlot = pickerTargetSlot
    if (activeSlot != null) {
        LuxuryComparisonPersonPickerDialog(
            slot = activeSlot,
            candidates = selectableList,
            selectedPersonA = personA,
            selectedPersonB = personB,
            accentCyan = accentCyan,
            accentViolet = accentViolet,
            accentEmerald = accentEmerald,
            isAr = isAr,
            isFr = isFr,
            isDarkTheme = isDarkTheme,
            onSelect = { chosen ->
                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                view.playSoundEffect(SoundEffectConstants.CLICK)
                if (activeSlot == 1) {
                    viewModel.selectPersonA(chosen.id)
                } else {
                    viewModel.selectPersonB(chosen.id)
                }
                pickerTargetSlot = null
            },
            onDismiss = { pickerTargetSlot = null }
        )
    }
}

@Composable
fun PersonSelectorCard(
    label: String,
    person: Individual?,
    status: StrikerStatus?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    isAr: Boolean = true,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val boostTextColor = if (isDarkTheme) NeonEmerald else DayEmerald
    if (person != null && status != null) {
        DynamicAnimatedProfileCard(
            status = status,
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = RoundedCornerShape(16.dp),
            darkTheme = isDarkTheme
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = person.fullName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StrikerStatusBadge(status = status, darkTheme = isDarkTheme)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassTypeBadge(type = person.type, darkTheme = isDarkTheme)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAr) "التعزيز ${person.boost}" else "BOOST ${person.boost}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = boostTextColor
                    )
                }
            }
        }
    } else {
        LiquidGlassCard(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = RoundedCornerShape(16.dp),
            darkTheme = isDarkTheme
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAr) "اختر…" else "Select…",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LuxuryComparisonPersonPickerDialog(
    slot: Int,
    candidates: List<Individual>,
    selectedPersonA: Individual?,
    selectedPersonB: Individual?,
    accentCyan: Color,
    accentViolet: Color,
    accentEmerald: Color,
    isAr: Boolean,
    isFr: Boolean,
    isDarkTheme: Boolean,
    onSelect: (Individual) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember(slot) { mutableStateOf("") }
    val slotAccent = if (slot == 1) accentCyan else accentViolet
    val targetSelectedId = if (slot == 1) selectedPersonA?.id else selectedPersonB?.id
    val otherSelectedId = if (slot == 1) selectedPersonB?.id else selectedPersonA?.id

    val filteredList = remember(candidates, searchQuery) {
        if (searchQuery.isBlank()) {
            candidates
        } else {
            SmartSearchMatcher.filterAndRank(candidates, searchQuery).map { it.individual }
        }
    }

    val surfaceFill = if (isDarkTheme) {
        Brush.verticalGradient(listOf(Color(0xF00B1222), Color(0xEC070C17)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFCFFFFFF), Color(0xF6F1F5F9)))
    }
    val borderBrush = Brush.linearGradient(
        listOf(
            if (isDarkTheme) Color(0x80FFFFFF) else Color(0xFFFFFFFF),
            slotAccent.copy(alpha = 0.72f),
            if (isDarkTheme) Color(0x2AFFFFFF) else Color(0xFFCBD5E1)
        )
    )
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xA6000000))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 18.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .heightIn(max = 560.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(surfaceFill)
                    .drawBehind {
                        val w = size.width
                        val inset = 24.dp.toPx()
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isDarkTheme) 0.55f else 0.95f),
                                    Color.Transparent
                                )
                            ),
                            start = Offset(inset, 1.dp.toPx()),
                            end = Offset(w - inset, 1.dp.toPx()),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                    .border(1.2.dp, borderBrush, RoundedCornerShape(26.dp))
                    .clickable(enabled = false, onClick = {})
                    .padding(18.dp)
                    .testTag("comparison_person_picker_dialog")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Clean Minimal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(slotAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.14f))
                                    .border(1.dp, slotAccent.copy(alpha = 0.7f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (slot == 1) stringResource(R.string.select_person_a) else stringResource(R.string.select_person_b),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = slotAccent
                                )
                            }
                            Text(
                                text = when {
                                    isAr -> "اختر شخصاً"
                                    isFr -> "Choisir un profil"
                                    else -> "Choose Profile"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDarkTheme) Color(0x1EFFFFFF) else Color(0xFFE2E8F0))
                                .testTag("comparison_picker_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = mutedColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Search Input Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comparison_picker_search_input"),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_hint),
                                fontSize = 13.sp,
                                color = mutedColor.copy(alpha = 0.75f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = slotAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("comparison_picker_clear_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isAr) "مسح البحث" else "Clear search",
                                        tint = mutedColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = slotAccent,
                            unfocusedBorderColor = slotAccent.copy(alpha = if (isDarkTheme) 0.35f else 0.4f),
                            focusedContainerColor = if (isDarkTheme) Color(0x201E293B) else Color(0xFFFFFFFF),
                            unfocusedContainerColor = if (isDarkTheme) Color(0x141E293B) else Color(0xFFF8FAFC),
                            cursorColor = slotAccent
                        )
                    )

                    // Candidate List or Empty Search State
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = mutedColor.copy(alpha = 0.55f),
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = when {
                                        isAr -> "لا توجد نتائج مطابقة"
                                        isFr -> "Aucun profil correspondant"
                                        else -> "No matching profiles"
                                    },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = mutedColor
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(filteredList, key = { it.id }) { item ->
                                val isSelectedForTarget = item.id == targetSelectedId
                                val isSelectedInOtherSlot = item.id == otherSelectedId
                                val itemBg = when {
                                    isSelectedForTarget -> Brush.horizontalGradient(
                                        listOf(
                                            slotAccent.copy(alpha = if (isDarkTheme) 0.24f else 0.16f),
                                            slotAccent.copy(alpha = if (isDarkTheme) 0.09f else 0.05f)
                                        )
                                    )
                                    isDarkTheme -> Brush.horizontalGradient(listOf(Color(0x18FFFFFF), Color(0x0DFFFFFF)))
                                    else -> Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
                                }
                                val itemBorder = if (isSelectedForTarget) {
                                    slotAccent.copy(alpha = 0.85f)
                                } else if (isDarkTheme) {
                                    Color(0x26FFFFFF)
                                } else {
                                    Color(0xFFCBD5E1)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(itemBg)
                                        .border(if (isSelectedForTarget) 1.3.dp else 1.dp, itemBorder, RoundedCornerShape(16.dp))
                                        .clickable { onSelect(item) }
                                        .padding(horizontal = 12.dp, vertical = 11.dp)
                                        .testTag("comparison_picker_item_${item.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        GlassTypeBadge(type = item.type, darkTheme = isDarkTheme)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.fullName,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelectedForTarget) FontWeight.Black else FontWeight.Bold,
                                                fontFamily = PlusJakartaSans,
                                                color = if (isSelectedForTarget) slotAccent else titleColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isAr) "التعزيز ${item.boost}" else "BOOST ${item.boost}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = accentEmerald
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 11.sp,
                                                    color = mutedColor
                                                )
                                                Text(
                                                    text = if (isAr) "${item.strikeCount} ضربة" else "${item.strikeCount} strikes",
                                                    fontSize = 11.sp,
                                                    color = mutedColor
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isSelectedInOtherSlot) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(mutedColor.copy(alpha = 0.14f))
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (slot == 1) stringResource(R.string.select_person_b) else stringResource(R.string.select_person_a),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = mutedColor
                                                )
                                            }
                                        }

                                        if (isSelectedForTarget) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = slotAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

