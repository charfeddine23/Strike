package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.util.HapticHelper
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassInnerPanel
import com.example.ui.components.StrikerStatus
import com.example.ui.components.StrikerStatusBadge
import com.example.ui.components.DynamicAnimatedProfileCard
import com.example.ui.theme.CaveatHandwriting
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayRose
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonSky
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.ui.viewmodel.SortOrder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: IndividualViewModel,
    onNavigateToStrike: (Long) -> Unit,
    onNavigateToCompare: (Long) -> Unit,
    modifier: Modifier = Modifier,
    hostDataEntryExternally: Boolean = false,
    isDarkTheme: Boolean = LocalAppDarkTheme.current
) {
    val individuals by viewModel.filteredIndividuals.collectAsStateWithLifecycle()
    val allList by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val lastStrikeMap by viewModel.individualLastStrikeMap.collectAsStateWithLifecycle()
    val isDataEntryOpen by viewModel.isDataEntryOpen.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isHapticFeedbackEnabled by viewModel.isHapticFeedbackEnabled
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val accentAmber = if (isDarkTheme) NeonAmber else DayOrange
    val accentRose = if (isDarkTheme) NeonRose else DayRose

    var individualToDelete by remember { mutableStateOf<Individual?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var expandedCardIds by remember { mutableStateOf(setOf<Long>()) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Search Bar & Sort Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input"),
                        placeholder = {
                            Text(
                                stringResource(R.string.search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = accentCyan
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearSearch() },
                                    modifier = Modifier.testTag("clear_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentCyan,
                            unfocusedBorderColor = if (isDarkTheme) Color(0x3338BDF8) else Color(0x660284C7),
                            focusedContainerColor = if (isDarkTheme) Color(0x1A1E293B) else Color(0xF5FFFFFF),
                            unfocusedContainerColor = if (isDarkTheme) Color(0x101E293B) else Color(0xCCFFFFFF),
                            cursorColor = accentCyan
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(contentAlignment = Alignment.TopEnd) {
                        val sortAccent = if (isDarkTheme) NeonCyan else DayCyan
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            sortAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.14f),
                                            NeonViolet.copy(alpha = if (isDarkTheme) 0.12f else 0.08f)
                                        )
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    sortAccent.copy(alpha = if (showSortMenu) 0.9f else 0.45f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    HapticHelper.triggerActionHaptic(context, isHapticFeedbackEnabled)
                                    showSortMenu = !showSortMenu
                                }
                                .testTag("sort_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = sortAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 6.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isDarkTheme) NeonAmber else DayOrange)
                        )

                        // Luxury Sort Selector anchored directly to & emerging from the Sort button
                        LuxurySortButtonPopup(
                            visible = showSortMenu,
                            currentSortOrder = sortOrder,
                            currentLang = currentLang,
                            onSelectSortOrder = { selected ->
                                HapticHelper.triggerActionHaptic(context, isHapticFeedbackEnabled)
                                viewModel.setSortOrder(selected)
                                showSortMenu = false
                            },
                            onDismiss = { showSortMenu = false },
                            darkTheme = isDarkTheme
                        )
                    }
                }
            }

            // Type Filter Chips Bar (ALL, M, G, TS, FG) with live count badges
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "تصفية حسب النوع" else "FILTER BY TYPE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filterOptions = listOf(
                            Triple("ALL", allList.size, if (isDarkTheme) NeonCyan else DayCyan),
                            Triple("M", allList.count { it.type == "M" }, if (isDarkTheme) NeonCyan else DayCyan),
                            Triple("G", allList.count { it.type == "G" }, if (isDarkTheme) NeonViolet else DayPurple),
                            Triple("TS", allList.count { it.type == "TS" }, if (isDarkTheme) NeonEmerald else DayEmerald),
                            Triple("FG", allList.count { it.type == "FG" }, if (isDarkTheme) NeonAmber else DayOrange)
                        )

                        items(filterOptions) { (opt, count, accentColor) ->
                            val isSelected = filterType == opt
                            val chipBg = if (isSelected) {
                                accentColor.copy(alpha = if (isDarkTheme) 0.25f else 0.16f)
                            } else {
                                if (isDarkTheme) Color(0x14FFFFFF) else Color(0xB3FFFFFF)
                            }
                            val chipBorder = if (isSelected) {
                                accentColor
                            } else {
                                if (isDarkTheme) Color(0x2538BDF8) else Color(0x45CBD5E1)
                            }
                            val textColor = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.5.dp, chipBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSelected && opt != "ALL") {
                                            viewModel.setFilterType("ALL")
                                        } else {
                                            viewModel.setFilterType(opt)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("filter_chip_$opt"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (opt == "ALL") stringResource(R.string.filter_all) else opt,
                                        color = textColor,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected && isDarkTheme -> accentColor.copy(alpha = 0.35f)
                                                    isSelected -> accentColor
                                                    isDarkTheme -> Color(0x181E293B)
                                                    else -> Color(0x33CBD5E1)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 10.sp,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // List of Records or Empty State
            if (allList.isEmpty()) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                            .testTag("empty_records_card"),
                        shape = RoundedCornerShape(20.dp),
                        darkTheme = isDarkTheme
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.empty_records_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.empty_records_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else if (individuals.isEmpty()) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        darkTheme = isDarkTheme
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = accentAmber,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isAr) "لم يتم العثور على نتائج" else "No Individuals Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isAr) {
                                    "لا توجد سجلات تطابق بحثك \"$searchQuery\" أو التصفية ($filterType)."
                                } else {
                                    "No records matched your search \"$searchQuery\" or selected filter ($filterType)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentCyan.copy(alpha = if (isDarkTheme) 0.2f else 0.14f))
                                    .border(1.dp, accentCyan, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.resetFilters() }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                                    .testTag("reset_search_mismatch_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) "إعادة ضبط التصفية والبحث" else "Reset Filters & Search",
                                    color = accentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(individuals, key = { it.id }) { item ->
                    val status = viewModel.getStatusForIndividual(item, lastStrikeMap)
                    val isExpanded = item.id in expandedCardIds
                    IndividualRecordCard(
                        individual = item,
                        status = status,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedCardIds = if (isExpanded) {
                                expandedCardIds - item.id
                            } else {
                                expandedCardIds + item.id
                            }
                        },
                        onGoToStrike = { onNavigateToStrike(item.id) },
                        onCompare = { onNavigateToCompare(item.id) },
                        onEdit = {
                            viewModel.openDataEntryForEdit(item)
                        },
                        onDelete = { individualToDelete = item },
                        darkTheme = isDarkTheme,
                        isHapticFeedbackEnabled = isHapticFeedbackEnabled,
                        isAr = isAr
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button to Add New
        FloatingActionButton(
            onClick = {
                viewModel.openDataEntryForNew()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_record_fab"),
            containerColor = accentCyan,
            contentColor = if (isDarkTheme) Color(0xFF03141F) else Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.nav_entry),
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Integrated Data Entry Modal Overlay (+ button & Edit action) when hosted standalone
    if (!hostDataEntryExternally && isDataEntryOpen) {
        com.example.ui.components.DataEntryDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeDataEntry() },
            isDarkTheme = isDarkTheme
        )
    }

    // Delete confirmation dialog
    if (individualToDelete != null) {
        val target = individualToDelete!!
        AlertDialog(
            onDismissRequest = { individualToDelete = null },
            containerColor = if (isDarkTheme) Color(0xFF0E1729) else Color(0xFFF8FAFC),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text("${stringResource(R.string.confirm_delete_msg)}\n(${target.fullName})") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIndividual(target)
                        individualToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text(stringResource(R.string.delete), color = accentRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { individualToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun IndividualRecordCard(
    individual: Individual,
    status: StrikerStatus,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onGoToStrike: () -> Unit,
    onCompare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = LocalAppDarkTheme.current,
    isHapticFeedbackEnabled: Boolean = true,
    isAr: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnEdit by rememberUpdatedState(onEdit)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "record_card_chevron_rot"
    )

    val boostBg = if (darkTheme) {
        Brush.linearGradient(listOf(NeonEmerald.copy(alpha = 0.25f), NeonCyan.copy(alpha = 0.2f)))
    } else {
        Brush.linearGradient(listOf(DayEmerald.copy(alpha = 0.18f), DayCyan.copy(alpha = 0.14f)))
    }
    val boostColor = if (darkTheme) NeonEmerald else DayEmerald

    DynamicAnimatedProfileCard(
        status = status,
        modifier = modifier
            .fillMaxWidth()
            .testTag("record_card_${individual.id}"),
        shape = RoundedCornerShape(18.dp),
        darkTheme = darkTheme
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                .pointerInput(individual.id) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val downPos = down.position
                        var triggered = false
                        val holdJob = coroutineScope.launch {
                            delay(500L)
                            triggered = true
                            HapticHelper.triggerActionHaptic(context, isHapticFeedbackEnabled)
                            currentOnEdit()
                        }
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (triggered) {
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) {
                                        break
                                    }
                                } else {
                                    val movedTooFar = event.changes.any { change ->
                                        (change.position - downPos).getDistance() > touchSlop
                                    }
                                    if (movedTooFar || event.changes.none { it.pressed }) {
                                        holdJob.cancel()
                                        break
                                    }
                                }
                            }
                        } finally {
                            holdJob.cancel()
                        }
                    }
                }
        ) {
            // Always-Visible Collapsed Summary Area (Name, Status, Phone, BOOST at bottom + Expand/Collapse)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
            ) {
                // Top Row: Name + Status Badge + Expand/Collapse Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = individual.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isExpanded) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StrikerStatusBadge(status = status, darkTheme = darkTheme)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (darkTheme) Color(0x2238BDF8) else Color(0x140284C7))
                            .border(
                                1.dp,
                                if (darkTheme) Color(0x4538BDF8) else Color(0x450284C7),
                                CircleShape
                            )
                            .clickable(onClick = onToggleExpand)
                            .testTag("record_expand_btn_${individual.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(
                                if (isExpanded) R.string.collapse_card else R.string.expand_card
                            ),
                            tint = if (darkTheme) NeonCyan else DayCyan,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Row of Collapsed View: Phone Number & BOOST Badge moved to bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (individual.phoneNumber.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (darkTheme) NeonCyan else DayCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = individual.phoneNumber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (darkTheme) NeonCyan else DayCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "—",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // BOOST Badge repositioned to bottom
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(boostBg)
                            .border(1.dp, boostColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAr) "التعزيز: " else "BOOST: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${individual.boost}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                color = boostColor
                            )
                        }
                    }
                }
            }

            // Expandable Detailed Telemetry & Actions
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Metrics Row: Type, Dimensions & Size (Length x Width) in a frosted inner tier
                    LiquidGlassInnerPanel(
                        modifier = Modifier.fillMaxWidth(),
                        darkTheme = darkTheme
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassTypeBadge(type = individual.type, darkTheme = darkTheme)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${stringResource(R.string.length)} × ${stringResource(R.string.width)}",
                                        fontSize = 10.sp,
                                        fontFamily = PlusJakartaSans,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${individual.length} × ${individual.width}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = PlusJakartaSans,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.size),
                                    fontSize = 10.sp,
                                    fontFamily = PlusJakartaSans,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${individual.size}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                    color = if (darkTheme) NeonCyan else DayCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

            // Scores breakdown pills (S, F, C)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScorePill(
                    label = "S",
                    value = individual.sScore,
                    color = NeonSky,
                    modifier = Modifier.weight(1f),
                    darkTheme = darkTheme
                )
                ScorePill(
                    label = "F",
                    value = individual.fScore,
                    color = NeonViolet,
                    modifier = Modifier.weight(1f),
                    darkTheme = darkTheme
                )
                ScorePill(
                    label = "C",
                    value = individual.cScore,
                    color = NeonAmber,
                    modifier = Modifier.weight(1f),
                    darkTheme = darkTheme
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calculated Score Banner: BOOST × Strikes (Score = boost * strikeCount)
            val scoreBannerBg = if (darkTheme) {
                Brush.horizontalGradient(listOf(NeonAmber.copy(alpha = 0.22f), NeonCyan.copy(alpha = 0.18f)))
            } else {
                Brush.horizontalGradient(listOf(DayOrange.copy(alpha = 0.15f), DayCyan.copy(alpha = 0.12f)))
            }
            val scoreColor = if (darkTheme) NeonAmber else DayOrange

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scoreBannerBg)
                    .border(1.dp, scoreColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = scoreColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isAr) {
                                "النتيجة الإجمالية (التعزيز × الضربات)"
                            } else {
                                "TOTAL SCORE (BOOST × STRIKES)"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            color = scoreColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${individual.boost} × ${individual.strikeCount}",
                            fontSize = 10.sp,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "= ${individual.totalScore}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                    color = scoreColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Actions: Read-only Strike Count + Navigation / Compare & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Read-only Strike Count badge
                val strikeBtnColor = if (darkTheme) NeonAmber else DayOrange
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkTheme) Color(0x20F59E0B) else Color(0x24F59E0B))
                        .border(1.dp, strikeBtnColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("record_strike_count_${individual.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = strikeBtnColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${individual.strikeCount}",
                        fontWeight = FontWeight.Black,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        color = strikeBtnColor
                    )
                }

                // Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onGoToStrike,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = stringResource(R.string.nav_strike),
                            tint = if (darkTheme) NeonCyan else DayCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onCompare,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = stringResource(R.string.nav_compare),
                            tint = if (darkTheme) NeonViolet else DayPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = if (darkTheme) NeonRose.copy(alpha = 0.8f) else DayRose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
                }
            }
        }
    }
}

@Composable
fun ScorePill(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = LocalAppDarkTheme.current
) {
    val effectiveColor = if (darkTheme) color else when (color) {
        NeonSky, NeonCyan -> DayCyan
        NeonViolet -> DayPurple
        NeonAmber -> DayOrange
        NeonEmerald -> DayEmerald
        else -> color
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(effectiveColor.copy(alpha = if (darkTheme) 0.12f else 0.16f))
            .border(1.dp, effectiveColor.copy(alpha = if (darkTheme) 0.35f else 0.45f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = PlusJakartaSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$value",
                color = effectiveColor,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class LuxurySortOptionSpec(
    val order: SortOrder,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun LuxurySortButtonPopup(
    visible: Boolean,
    currentSortOrder: SortOrder,
    currentLang: String,
    onSelectSortOrder: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
    darkTheme: Boolean = LocalAppDarkTheme.current
) {
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) {
        transitionState.targetState = visible
    }

    if (!visible && !transitionState.currentState && !transitionState.targetState) {
        return
    }

    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"

    val headerTitle = when {
        isAr -> "ترتيب السجلات حسب"
        isFr -> "Trier les fiches par"
        else -> "Sort Records By"
    }
    val headerSubtitle = when {
        isAr -> "اختر معيار الترتيب المفضل لقائمة الأسماء"
        isFr -> "Choisissez le critère d'affichage"
        else -> "Select your preferred ranking criterion"
    }

    val options = listOf(
        LuxurySortOptionSpec(
            order = SortOrder.SCORE_DESC,
            title = stringResource(R.string.sort_score),
            subtitle = when {
                isAr -> "أعلى نتيجة إجمالية أولاً (التنازلي)"
                isFr -> "Score total le plus élevé en premier"
                else -> "Highest combined score first"
            },
            icon = Icons.Default.EmojiEvents,
            accent = if (darkTheme) NeonAmber else DayOrange
        ),
        LuxurySortOptionSpec(
            order = SortOrder.BOOST_DESC,
            title = stringResource(R.string.sort_boost),
            subtitle = when {
                isAr -> "الأقوى في قيمة التعزيز BOOST"
                isFr -> "Valeur BOOST maximale en premier"
                else -> "Strongest BOOST multiplier first"
            },
            icon = Icons.Default.Speed,
            accent = if (darkTheme) NeonEmerald else DayEmerald
        ),
        LuxurySortOptionSpec(
            order = SortOrder.STRIKES_DESC,
            title = stringResource(R.string.sort_strikes),
            subtitle = when {
                isAr -> "الأكثر عدداً في الضربات المسجلة"
                isFr -> "Nombre de strikes le plus élevé"
                else -> "Most recorded strikes first"
            },
            icon = Icons.Default.Bolt,
            accent = if (darkTheme) NeonRose else DayRose
        ),
        LuxurySortOptionSpec(
            order = SortOrder.SIZE_DESC,
            title = stringResource(R.string.sort_size),
            subtitle = when {
                isAr -> "الأكبر مساحةً (الطول × العرض)"
                isFr -> "Plus grande taille calculée en premier"
                else -> "Largest calculated dimensions first"
            },
            icon = Icons.Default.FitnessCenter,
            accent = if (darkTheme) NeonViolet else DayPurple
        ),
        LuxurySortOptionSpec(
            order = SortOrder.NAME_ASC,
            title = stringResource(R.string.sort_name),
            subtitle = when {
                isAr -> "ترتيب أبجدي تصاعدي للأسماء"
                isFr -> "Ordre alphabétique croissant"
                else -> "Alphabetical A to Z ordering"
            },
            icon = Icons.Default.SortByAlpha,
            accent = if (darkTheme) NeonCyan else DayCyan
        )
    )

    val containerBg = if (darkTheme) Color(0xF50B1324) else Color(0xFFF8FAFC)
    val accentCyan = if (darkTheme) NeonCyan else DayCyan
    val accentViolet = if (darkTheme) NeonViolet else DayPurple
    val borderBrush = Brush.linearGradient(
        listOf(
            accentCyan.copy(alpha = 0.8f),
            accentViolet.copy(alpha = 0.6f),
            if (darkTheme) NeonEmerald.copy(alpha = 0.7f) else DayEmerald.copy(alpha = 0.65f)
        )
    )

    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val yOffsetPx = remember(density) { with(density) { 54.dp.roundToPx() } }
    val scaleOrigin = TransformOrigin(
        pivotFractionX = if (isRtl) 0.08f else 0.92f,
        pivotFractionY = 0.0f
    )
    val popoverShape = RoundedCornerShape(
        topStart = if (isRtl) 6.dp else 22.dp,
        topEnd = if (isRtl) 22.dp else 6.dp,
        bottomStart = 22.dp,
        bottomEnd = 22.dp
    )

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, yOffsetPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(animationSpec = tween(durationMillis = 150)) + scaleIn(
                initialScale = 0.16f,
                transformOrigin = scaleOrigin,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(animationSpec = tween(durationMillis = 130)) + scaleOut(
                targetScale = 0.16f,
                transformOrigin = scaleOrigin,
                animationSpec = tween(durationMillis = 140)
            ),
            label = "luxury_sort_button_emerge"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .widthIn(min = 270.dp, max = 340.dp)
                    .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 12.dp)
                    .shadow(
                        elevation = if (darkTheme) 18.dp else 12.dp,
                        shape = popoverShape,
                        ambientColor = accentCyan.copy(alpha = 0.28f),
                        spotColor = accentViolet.copy(alpha = 0.26f)
                    )
                    .clip(popoverShape)
                    .background(containerBg)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentCyan.copy(alpha = if (darkTheme) 0.18f else 0.11f),
                                    accentViolet.copy(alpha = if (darkTheme) 0.09f else 0.05f),
                                    Color.Transparent
                                ),
                                center = Offset(
                                    x = if (isRtl) size.width * 0.12f else size.width * 0.88f,
                                    y = size.height * 0.08f
                                ),
                                radius = size.width * 0.85f
                            )
                        )
                        drawLine(
                            color = if (darkTheme) Color(0x40FFFFFF) else Color(0xAAFFFFFF),
                            start = Offset(20.dp.toPx(), 1.dp.toPx()),
                            end = Offset(size.width - 20.dp.toPx(), 1.dp.toPx()),
                            strokeWidth = 1.4.dp.toPx()
                        )
                    }
                    .border(1.4.dp, borderBrush, popoverShape)
                    .padding(16.dp)
                    .testTag("luxury_sort_dialog")
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            accentCyan.copy(alpha = 0.24f),
                                            accentViolet.copy(alpha = 0.20f)
                                        )
                                    )
                                )
                                .border(1.dp, accentCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = headerSubtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (darkTheme) Color(0x1E38BDF8) else Color(0x140284C7))
                            .testTag("close_sort_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Luxury Option Cards
                options.forEach { option ->
                    val isSelected = currentSortOrder == option.order
                    val itemBg = if (isSelected) {
                        Brush.horizontalGradient(
                            listOf(
                                option.accent.copy(alpha = if (darkTheme) 0.24f else 0.16f),
                                option.accent.copy(alpha = if (darkTheme) 0.08f else 0.05f)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                if (darkTheme) Color(0x14FFFFFF) else Color(0xFFFFFFFF),
                                if (darkTheme) Color(0x0AFFFFFF) else Color(0xFFF1F5F9)
                            )
                        )
                    }
                    val itemBorderColor = if (isSelected) {
                        option.accent.copy(alpha = 0.85f)
                    } else {
                        if (darkTheme) Color(0x2438BDF8) else Color(0xFFCBD5E1)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(itemBg)
                            .border(if (isSelected) 1.5.dp else 1.dp, itemBorderColor, RoundedCornerShape(16.dp))
                            .clickable { onSelectSortOrder(option.order) }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .testTag("sort_option_${option.order.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        option.accent.copy(
                                            alpha = if (isSelected) 0.24f else if (darkTheme) 0.13f else 0.12f
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        option.accent.copy(alpha = if (isSelected) 0.8f else 0.35f),
                                        RoundedCornerShape(11.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = option.accent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontFamily = PlusJakartaSans,
                                    color = if (isSelected) option.accent else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.subtitle,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = option.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(
                                        1.5.dp,
                                        if (darkTheme) Color(0x4094A3B8) else Color(0xFF94A3B8),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
