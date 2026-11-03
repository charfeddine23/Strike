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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.Individual
import com.example.ui.components.GlassTypeBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SmartMetricComparisonBar
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.viewmodel.IndividualViewModel

@Composable
fun ComparisonScreen(
    viewModel: IndividualViewModel,
    modifier: Modifier = Modifier
) {
    val allList by viewModel.allIndividuals.collectAsStateWithLifecycle()
    val personA by viewModel.personA.collectAsStateWithLifecycle()
    val personB by viewModel.personB.collectAsStateWithLifecycle()

    var showMenuA by remember { mutableStateOf(false) }
    var showMenuB by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
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
                    Column {
                        Text(
                            text = stringResource(R.string.comparison_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Head-to-head metrics analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeonViolet.copy(alpha = 0.2f))
                            .border(1.dp, NeonViolet, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

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
                        accentColor = NeonCyan,
                        onClick = { showMenuA = true },
                        testTag = "selector_person_a"
                    )

                    DropdownMenu(
                        expanded = showMenuA,
                        onDismissRequest = { showMenuA = false }
                    ) {
                        allList.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.fullName} (${item.type})") },
                                onClick = {
                                    viewModel.selectPersonA(item.id)
                                    showMenuA = false
                                }
                            )
                        }
                    }
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
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Selector Person B
                Box(modifier = Modifier.weight(1f)) {
                    PersonSelectorCard(
                        label = stringResource(R.string.select_person_b),
                        person = personB,
                        accentColor = NeonViolet,
                        onClick = { showMenuB = true },
                        testTag = "selector_person_b"
                    )

                    DropdownMenu(
                        expanded = showMenuB,
                        onDismissRequest = { showMenuB = false }
                    ) {
                        allList.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.fullName} (${item.type})") },
                                onClick = {
                                    viewModel.selectPersonB(item.id)
                                    showMenuB = false
                                }
                            )
                        }
                    }
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

                val leaderColor = if (aWins > bWins) NeonCyan else if (bWins > aWins) NeonViolet else NeonAmber

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
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
                                        .background(NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${a.fullName} ($aWins)",
                                    fontSize = 12.sp,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(NeonViolet)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${b.fullName} ($bWins)",
                                    fontSize = 12.sp,
                                    color = NeonViolet,
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
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "METRIC BREAKDOWN",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        // 0. TOTAL SCORE (BOOST × Strikes)
                        SmartMetricComparisonBar(
                            title = "TOTAL SCORE (BOOST × Strikes)",
                            valueA = a.totalScore,
                            valueB = b.totalScore,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8)
                        )

                        // 1. BOOST
                        SmartMetricComparisonBar(
                            title = "BOOST (Dynamic Formula)",
                            valueA = a.boost,
                            valueB = b.boost,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8)
                        )

                        // 2. Size (Length * Width)
                        SmartMetricComparisonBar(
                            title = "Size (Length × Width)",
                            valueA = a.size,
                            valueB = b.size,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8)
                        )

                        // 3. Strike Count
                        SmartMetricComparisonBar(
                            title = "Strike Count (عدد المرات)",
                            valueA = a.strikeCount.toDouble(),
                            valueB = b.strikeCount.toDouble(),
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8)
                        )

                        // 4. S SCORE
                        SmartMetricComparisonBar(
                            title = "S SCORE",
                            valueA = a.sScore,
                            valueB = b.sScore,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8)
                        )

                        // 5. F SCORE
                        SmartMetricComparisonBar(
                            title = "F SCORE",
                            valueA = a.fScore,
                            valueB = b.fScore,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8)
                        )

                        // 6. C SCORE
                        SmartMetricComparisonBar(
                            title = "C SCORE",
                            valueA = a.cScore,
                            valueB = b.cScore,
                            labelA = a.fullName.take(8),
                            labelB = b.fullName.take(8)
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
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "STRIKE ${a.fullName.take(8)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+1",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonCyan
                                )
                            }
                        }
                    }

                    // Strike B
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.incrementStrikeForId(b.id, 1) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "STRIKE ${b.fullName.take(8)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonViolet
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NeonViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+1",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonViolet
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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Please add at least two individuals to perform analytical comparison.",
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
}

@Composable
fun PersonSelectorCard(
    label: String,
    person: Individual?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(16.dp)
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
            if (person != null) {
                Text(
                    text = person.fullName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassTypeBadge(type = person.type)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BOOST ${person.boost}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonEmerald
                    )
                }
            } else {
                Text(
                    text = "Select…",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
