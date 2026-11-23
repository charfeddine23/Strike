package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SmartMetricComparisonBar
import com.example.ui.components.StrikerStatus
import com.example.ui.components.StrikerStatusBadge
import com.example.ui.components.DynamicAnimatedProfileCard
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayGlassSheet
import com.example.ui.theme.DayGlassSurface
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
    val isAr = currentLang == "ar"
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentViolet = if (isDarkTheme) NeonViolet else DayPurple
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val accentEmerald = if (isDarkTheme) NeonEmerald else DayEmerald
    val selectableList = allList

    // 0 = Person A picker, 1 = Person B picker
    var activePickerSlot by remember { mutableStateOf<Int?>(null) }

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
                        onClick = { activePickerSlot = 0 },
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
                        .size(48.dp)
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
                        onClick = { activePickerSlot = 1 },
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

    activePickerSlot?.let { slot ->
        val isSlotA = slot == 0
        LuxuryComparisonPersonPickerDialog(
            title = if (isSlotA) stringResource(R.string.select_person_a) else stringResource(R.string.select_person_b),
            slotBadge = if (isSlotA) "A" else "B",
            accentColor = if (isSlotA) accentCyan else accentViolet,
            individuals = selectableList,
            selectedId = if (isSlotA) personA?.id else personB?.id,
            otherSlotId = if (isSlotA) personB?.id else personA?.id,
            otherSlotBadge = if (isSlotA) "B" else "A",
            getStatus = { viewModel.getStatusForIndividual(it, lastStrikeMap) },
            currentLang = currentLang,
            isDarkTheme = isDarkTheme,
            onSelect = { chosen ->
                if (isSlotA) {
                    viewModel.selectPersonA(chosen.id)
                } else {
                    viewModel.selectPersonB(chosen.id)
                }
                activePickerSlot = null
            },
            onDismiss = { activePickerSlot = null }
        )
    }
}

@Composable
private fun LuxuryComparisonPersonPickerDialog(
    title: String,
    slotBadge: String,
    accentColor: Color,
    individuals: List<Individual>,
    selectedId: Long?,
    otherSlotId: Long?,
    otherSlotBadge: String,
    getStatus: (Individual) -> StrikerStatus,
    currentLang: String,
    isDarkTheme: Boolean,
    onSelect: (Individual) -> Unit,
    onDismiss: () -> Unit
) {
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val layoutDir = if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr

    var searchQuery by remember { mutableStateOf("") }
    val filteredResults = remember(individuals, searchQuery) {
        SmartSearchMatcher.filterAndRank(individuals, searchQuery)
    }

    val sheetGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(Color(0xF60C121E), Color(0xF6060911))
        )
    } else {
        Brush.verticalGradient(listOf(Color.White, DayGlassSheet))
    }
    val borderBrush = Brush.linearGradient(
        listOf(
            accentColor.copy(alpha = 0.72f),
            if (isDarkTheme) Color.White.copy(alpha = 0.16f) else Color(0xFFCBD5E1),
            accentColor.copy(alpha = 0.32f)
        )
    )
    val titleColor = if (isDarkTheme) Color.White else DayTextPrimary
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else DayTextSecondary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 24.dp)
                    .testTag("comparison_person_picker_dialog"),
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sheetGradient, RoundedCornerShape(26.dp))
                        .border(1.2.dp, borderBrush, RoundedCornerShape(26.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
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
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f))
                                    .border(1.2.dp, accentColor.copy(alpha = 0.8f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slotBadge,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = accentColor
                                )
                            }
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = PlusJakartaSans,
                                    color = titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isAr -> "${filteredResults.size} من ${individuals.size}"
                                        isFr -> "${filteredResults.size} sur ${individuals.size}"
                                        else -> "${filteredResults.size} of ${individuals.size}"
                                    },
                                    fontSize = 11.sp,
                                    color = mutedColor
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("comparison_picker_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isAr) "إغلاق" else if (isFr) "Fermer" else "Close",
                                tint = mutedColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comparison_picker_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("comparison_picker_clear_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isAr) "مسح" else "Clear",
                                        tint = mutedColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        placeholder = {
                            Text(
                                text = when {
                                    isAr -> "ابحث بالاسم أو الفئة…"
                                    isFr -> "Rechercher par nom ou catégorie…"
                                    else -> "Search by name or category…"
                                },
                                fontSize = 13.sp,
                                color = mutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor.copy(alpha = 0.85f),
                            unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.14f) else Color(0xFFCBD5E1),
                            focusedContainerColor = if (isDarkTheme) Color(0x880E1626) else DayGlassSurface,
                            unfocusedContainerColor = if (isDarkTheme) Color(0x660B111E) else DayGlassSurface,
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            cursorColor = accentColor
                        )
                    )

                    HorizontalDivider(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                    )

                    // Filtered People List
                    if (filteredResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    isAr -> "لا توجد نتائج مطابقة"
                                    isFr -> "Aucun résultat correspondant"
                                    else -> "No matching people found"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = mutedColor
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .testTag("comparison_picker_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredResults, key = { it.individual.id }) { match ->
                                val item = match.individual
                                val isSelected = item.id == selectedId
                                val isOtherSlot = item.id == otherSlotId
                                val status = getStatus(item)

                                val rowBg = when {
                                    isSelected -> accentColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
                                    isDarkTheme -> Color(0x700F172A)
                                    else -> DayGlassSurface
                                }
                                val rowBorder = when {
                                    isSelected -> accentColor.copy(alpha = 0.85f)
                                    isDarkTheme -> Color.White.copy(alpha = 0.10f)
                                    else -> Color(0xFFE2E8F0)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(rowBg)
                                        .border(if (isSelected) 1.3.dp else 1.dp, rowBorder, RoundedCornerShape(16.dp))
                                        .clickable { onSelect(item) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("comparison_picker_item_${item.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassTypeBadge(type = item.type, darkTheme = isDarkTheme)

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.fullName,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                color = titleColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            StrikerStatusBadge(status = status, darkTheme = isDarkTheme)
                                        }
                                        Text(
                                            text = if (isAr) {
                                                "التعزيز ${item.boost} • ${item.strikeCount} ضربة"
                                            } else {
                                                "BOOST ${item.boost} • ${item.strikeCount} strikes"
                                            },
                                            fontSize = 11.sp,
                                            color = mutedColor,
                                            maxLines = 1
                                        )
                                    }

                                    when {
                                        isSelected -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        isOtherSlot -> {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(mutedColor.copy(alpha = 0.16f))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = otherSlotBadge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = mutedColor
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
