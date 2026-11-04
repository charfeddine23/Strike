package com.example.ui.screens

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.util.HapticHelper
import com.example.ui.components.GlassStatSummaryCard
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassInnerPanel
import com.example.ui.theme.CaveatHandwriting
import com.example.ui.theme.DayCyan
import com.example.ui.theme.DayEmerald
import com.example.ui.theme.DayOrange
import com.example.ui.theme.DayPurple
import com.example.ui.theme.DayTextPrimary
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonSky
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.viewmodel.IndividualViewModel
import com.example.ui.viewmodel.SortOrder

@Composable
fun DashboardScreen(
    viewModel: IndividualViewModel,
    onNavigateToEntry: () -> Unit,
    onNavigateToStrike: (Long) -> Unit,
    onNavigateToCompare: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val individuals by viewModel.filteredIndividuals.collectAsStateWithLifecycle()
    val allList by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val isHapticFeedbackEnabled by viewModel.isHapticFeedbackEnabled

    var individualToDelete by remember { mutableStateOf<Individual?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Summary calculations
    val totalCount = allList.size
    val topStriker = allList.maxByOrNull { it.strikeCount }
    val highestBoost = allList.maxByOrNull { it.boost }
    val avgSize = if (allList.isNotEmpty()) {
        (allList.map { it.size }.average() * 10.0).toLong() / 10.0
    } else 0.0

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Telemetry Stat Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassStatSummaryCard(
                        title = stringResource(R.string.total_records),
                        value = "$totalCount",
                        icon = Icons.Default.People,
                        accentColor = NeonCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "stat_total_records"
                    )
                    GlassStatSummaryCard(
                        title = stringResource(R.string.top_striker),
                        value = topStriker?.let { "${it.strikeCount} (${it.fullName.take(6)})" } ?: "—",
                        icon = Icons.Default.Bolt,
                        accentColor = NeonAmber,
                        modifier = Modifier.weight(1f),
                        testTag = "stat_top_striker"
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassStatSummaryCard(
                        title = stringResource(R.string.highest_boost),
                        value = highestBoost?.let { "${it.boost}" } ?: "—",
                        icon = Icons.Default.Speed,
                        accentColor = NeonEmerald,
                        modifier = Modifier.weight(1f),
                        testTag = "stat_highest_boost"
                    )
                    GlassStatSummaryCard(
                        title = stringResource(R.string.avg_size),
                        value = "$avgSize",
                        icon = Icons.Default.FitnessCenter,
                        accentColor = NeonViolet,
                        modifier = Modifier.weight(1f),
                        testTag = "stat_avg_size"
                    )
                }
            }

            // Yearly Strikes Analytics Quick Banner
            item {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToStrike(allList.firstOrNull()?.id ?: 1L) },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonEmerald.copy(alpha = 0.2f))
                                    .border(1.dp, NeonEmerald, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.yearly_dashboard_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تتبع إجمالي الضربات لكل سنة • اضغط للعرض",
                                    fontSize = 11.sp,
                                    color = NeonEmerald
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "عرض الداش بورد",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }

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
                                tint = NeonCyan
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
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x3338BDF8),
                            focusedContainerColor = Color(0x1A1E293B),
                            unfocusedContainerColor = Color(0x101E293B)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x221E293B))
                                .border(1.dp, Color(0x4038BDF8), RoundedCornerShape(16.dp))
                                .testTag("sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = NeonCyan
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_score)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.SCORE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_boost)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.BOOST_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_strikes)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.STRIKES_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_size)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.SIZE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_name)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.NAME_ASC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Type Filter Chips Bar (ALL, M, G, TS, FG) with live count badges
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FILTER BY TYPE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (filterType != "ALL" || searchQuery.isNotEmpty()) {
                            Text(
                                text = "Clear All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonRose,
                                modifier = Modifier
                                    .clickable { viewModel.resetFilters() }
                                    .padding(4.dp)
                                    .testTag("reset_all_filters_btn")
                            )
                        }
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
                                            .background(if (isSelected) accentColor.copy(alpha = 0.35f) else if (isDarkTheme) Color(0x181E293B) else Color(0x25CBD5E1))
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

            // Active Filter Result Feedback Bar
            if (searchQuery.isNotBlank() || filterType != "ALL") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x181E293B))
                            .border(1.dp, Color(0x3038BDF8), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Found ${individuals.size} of $totalCount individuals" +
                                    (if (filterType != "ALL") " • Type: $filterType" else "") +
                                    (if (searchQuery.isNotBlank()) " • Query: \"$searchQuery\"" else ""),
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.resetFilters() }
                        )
                    }
                }
            }

            // List of Records or Empty State
            if (allList.isEmpty()) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(20.dp)
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
                                tint = NeonCyan,
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
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(NeonCyan, NeonViolet)
                                        )
                                    )
                                    .clickable { viewModel.seedSampleData() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .testTag("seed_sample_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.seed_sample_data),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else if (individuals.isEmpty()) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(20.dp)
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
                                tint = NeonAmber,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Individuals Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No records matched your search \"$searchQuery\" or selected filter ($filterType).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.resetFilters() }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                                    .testTag("reset_search_mismatch_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Reset Filters & Search",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(individuals, key = { it.id }) { item ->
                    IndividualRecordCard(
                        individual = item,
                        onStrike = {
                            HapticHelper.triggerStrikeHaptic(context, isHapticFeedbackEnabled)
                            viewModel.incrementStrikeForId(item.id, 1)
                        },
                        onGoToStrike = { onNavigateToStrike(item.id) },
                        onCompare = { onNavigateToCompare(item.id) },
                        onEdit = {
                            viewModel.initFormForEdit(item)
                            onNavigateToEntry()
                        },
                        onDelete = { individualToDelete = item }
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
                viewModel.initFormForNew()
                onNavigateToEntry()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_record_fab"),
            containerColor = NeonCyan,
            contentColor = Color(0xFF03141F),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.nav_entry),
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Delete confirmation dialog
    if (individualToDelete != null) {
        val target = individualToDelete!!
        AlertDialog(
            onDismissRequest = { individualToDelete = null },
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
                    Text(stringResource(R.string.delete), color = NeonRose, fontWeight = FontWeight.Bold)
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
    onStrike: () -> Unit,
    onGoToStrike: () -> Unit,
    onCompare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("record_card_${individual.id}"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Name, Type, and BOOST Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    GlassTypeBadge(type = individual.type)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = individual.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (individual.phoneNumber.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = individual.phoneNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }

                // BOOST Badge
                val boostBg = if (darkTheme) {
                    Brush.linearGradient(listOf(NeonEmerald.copy(alpha = 0.25f), NeonCyan.copy(alpha = 0.2f)))
                } else {
                    Brush.linearGradient(listOf(DayEmerald.copy(alpha = 0.18f), DayCyan.copy(alpha = 0.14f)))
                }
                val boostColor = if (darkTheme) NeonEmerald else DayEmerald

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(boostBg)
                        .border(1.dp, boostColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BOOST: ",
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

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Row: Dimensions & Size (Length x Width) in a frosted inner tier
            LiquidGlassInnerPanel(
                modifier = Modifier.fillMaxWidth(),
                darkTheme = darkTheme
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            text = "TOTAL SCORE (BOOST × STRIKES)",
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

            // Bottom Actions: Strike Count + Quick +1 Strike Button + Compare & Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Strike info with Quick +1 Action
                val strikeBtnColor = if (darkTheme) NeonAmber else DayOrange
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkTheme) Color(0x20F59E0B) else Color(0x24F59E0B))
                        .border(1.dp, strikeBtnColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(strikeBtnColor)
                            .clickable { onStrike() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("quick_strike_${individual.id}")
                    ) {
                        Text(
                            text = "+1",
                            color = Color.White,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
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
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = if (darkTheme) NeonRose.copy(alpha = 0.8f) else Color(0xFFBE123C),
                            modifier = Modifier.size(18.dp)
                        )
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
    darkTheme: Boolean = isSystemInDarkTheme()
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
